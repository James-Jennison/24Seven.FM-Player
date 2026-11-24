package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteChange
import com.codeframe78.twentyfourseven.player.domain.FavoriteChangeState
import com.codeframe78.twentyfourseven.player.domain.FavoriteChangeStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksLoadStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksRepository
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksState
import com.codeframe78.twentyfourseven.player.domain.RankedFavoritesState
import com.codeframe78.twentyfourseven.player.domain.RankedFavoritesStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class NetworkFavoriteTracksRepository(
    private val remote: FavoriteTracksRemoteDataSource,
) : FavoriteTracksRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<FavoriteTracksState>>()
    private val locks = ConcurrentHashMap<StationId, Mutex>()

    /** Song numbers read from album pages for ranked tracks the feed gave none, by album and title. */
    private val resolved = ConcurrentHashMap<StationId, ConcurrentHashMap<Pair<String, String>, String>>()

    override fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState> =
        state(stationId).asStateFlow()

    override suspend fun refresh(stationId: StationId): Unit = lock(stationId).withLock { readFirstPage(stationId) }

    override suspend fun setRequestableOnly(stationId: StationId, requestableOnly: Boolean): Unit =
        lock(stationId).withLock {
            if (state(stationId).value.requestableOnly == requestableOnly) return@withLock
            update(stationId) { it.copy(requestableOnly = requestableOnly) }
            readFirstPage(stationId)
        }

    private suspend fun readFirstPage(stationId: StationId) {
        update(stationId) { it.copy(status = FavoriteTracksLoadStatus.Loading, errorMessage = null) }
        val requestableOnly = state(stationId).value.requestableOnly
        runCatching { remote.load(stationId, 1, requestableOnly) }
            .onSuccess { page ->
                update(stationId) {
                    it.copy(
                        status = FavoriteTracksLoadStatus.Ready,
                        tracks = page.tracks,
                        errorMessage = null,
                        loadedPages = 1,
                        hasMore = page.nextPage != null && page.tracks.isNotEmpty(),
                        loadingMore = false,
                        totalTracks = page.totalTracks,
                    )
                }
            }
            .onFailure { failure ->
                update(stationId) {
                    it.copy(
                        status = FavoriteTracksLoadStatus.Error,
                        errorMessage = if (failure is FavoritesAuthenticationRequiredException) {
                            "Sign in to this station to load your favorite tracks."
                        } else {
                            "Your favorite tracks could not be loaded right now."
                        },
                    )
                }
            }
        Unit
    }

    override suspend fun loadMore(stationId: StationId): Unit = lock(stationId).withLock {
        val current = state(stationId).value
        if (current.status != FavoriteTracksLoadStatus.Ready || !current.hasMore || current.loadedPages >= MAX_LIST_PAGES) {
            return@withLock
        }
        update(stationId) { it.copy(loadingMore = true) }
        val next = current.loadedPages + 1
        val page = try {
            remote.load(stationId, next, current.requestableOnly)
        } catch (cancellation: CancellationException) {
            update(stationId) { it.copy(loadingMore = false) }
            throw cancellation
        } catch (failure: Exception) {
            // The pages already shown stay; only the next one failed.
            update(stationId) {
                it.copy(
                    loadingMore = false,
                    errorMessage = if (failure is FavoritesAuthenticationRequiredException) {
                        "Sign in to this station to load your favorite tracks."
                    } else {
                        it.errorMessage
                    },
                )
            }
            return@withLock
        }
        update(stationId) {
            it.copy(
                tracks = mergePage(it.tracks, page.tracks),
                loadedPages = next,
                hasMore = page.nextPage != null && page.tracks.isNotEmpty(),
                loadingMore = false,
                totalTracks = page.totalTracks ?: it.totalTracks,
            )
        }
    }

    override suspend fun refreshRanked(stationId: StationId): Unit = lock(stationId).withLock {
        update(stationId) { it.copy(ranked = it.ranked.copy(status = RankedFavoritesStatus.Loading)) }
        reloadRanked(stationId, pages = 1)
    }

    override suspend fun loadMoreRanked(stationId: StationId): Unit = lock(stationId).withLock {
        val ranked = state(stationId).value.ranked
        if (ranked.status != RankedFavoritesStatus.Ready || !ranked.hasMore || ranked.loadedPages >= MAX_RANKED_PAGES) return@withLock
        update(stationId) { it.copy(ranked = ranked.copy(status = RankedFavoritesStatus.Loading)) }
        val next = ranked.loadedPages + 1
        val page = try {
            remote.loadRanked(stationId, next)
        } catch (cancellation: CancellationException) {
            update(stationId) { it.copy(ranked = ranked) }
            throw cancellation
        } catch (_: Exception) {
            // The pages already shown stay; only the next one failed.
            update(stationId) { it.copy(ranked = ranked) }
            return@withLock
        }
        update(stationId) {
            it.copy(
                ranked = RankedFavoritesState(
                    RankedFavoritesStatus.Ready,
                    ranked.tracks + withKnownSongIds(stationId, page.tracks, it.tracks),
                    loadedPages = next,
                    hasMore = page.nextPage != null && page.tracks.isNotEmpty(),
                ),
            )
        }
    }

    override suspend fun changeFavorite(stationId: StationId, songId: String, change: FavoriteChange): Unit =
        lock(stationId).withLock { applyChange(stationId, songId, change) }

    override suspend fun changeRankedFavorite(stationId: StationId, position: Int, change: FavoriteChange): Unit =
        lock(stationId).withLock {
            val track = state(stationId).value.ranked.tracks.firstOrNull { it.position == position } ?: return@withLock
            track.songId?.let { return@withLock applyChange(stationId, it, change) }
            val albumId = track.albumId ?: return@withLock
            update(stationId) { it.copy(change = FavoriteChangeState(FavoriteChangeStatus.Working, change, null)) }
            val status = try {
                val songId = remote.findSongId(stationId, albumId, track.title)
                if (songId != null) {
                    resolved.getOrPut(stationId) { ConcurrentHashMap() }[albumId to track.title] = songId
                    update(stationId) { state ->
                        state.copy(ranked = state.ranked.copy(tracks = withKnownSongIds(stationId, state.ranked.tracks, state.tracks)))
                    }
                    return@withLock applyChange(stationId, songId, change)
                }
                FavoriteChangeStatus.Failed
            } catch (cancellation: CancellationException) {
                update(stationId) { it.copy(change = FavoriteChangeState()) }
                throw cancellation
            } catch (_: FavoritesAuthenticationRequiredException) {
                FavoriteChangeStatus.SignInRequired
            } catch (_: Exception) {
                FavoriteChangeStatus.Failed
            }
            // Nothing was sent to the station, so the lists are left as they are.
            update(stationId) { it.copy(change = FavoriteChangeState(status, change, null)) }
        }

    private suspend fun applyChange(stationId: StationId, songId: String, change: FavoriteChange) {
        run {
            val current = state(stationId).value
            if (current.tracks.none { it.songId == songId } && current.ranked.tracks.none { it.songId == songId }) return
            val working = FavoriteChangeState(FavoriteChangeStatus.Working, change, songId)
            update(stationId) { it.copy(change = working) }
            val status = try {
                remote.change(stationId, songId, change)
                FavoriteChangeStatus.Done
            } catch (cancellation: CancellationException) {
                update(stationId) { it.copy(change = FavoriteChangeState()) }
                throw cancellation
            } catch (_: FavoritesAuthenticationRequiredException) {
                FavoriteChangeStatus.SignInRequired
            } catch (_: Exception) {
                // The change may have reached the station, so the lists are read again below either way.
                FavoriteChangeStatus.Failed
            }
            update(stationId) { it.copy(change = working.copy(status = status)) }
            if (status == FavoriteChangeStatus.SignInRequired) return
            // A move only changes the ranked order; the full list is sorted by the station and is re-read after a removal.
            if (current.ranked.loadedPages > 0) reloadRanked(stationId, current.ranked.loadedPages)
            if (change == FavoriteChange.Remove && current.status == FavoriteTracksLoadStatus.Ready) {
                reloadPages(stationId, current.loadedPages, current.requestableOnly)
            }
            Unit
        }
    }

    /** Reads the first [pages] pages of the full list again, so a removal shows without losing the pages already read. */
    private suspend fun reloadPages(stationId: StationId, pages: Int, requestableOnly: Boolean) {
        var tracks = emptyList<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack>()
        var loaded = 0
        var hasMore = false
        var total: Int? = null
        try {
            for (page in 1..pages.coerceIn(1, MAX_LIST_PAGES)) {
                val result = remote.load(stationId, page, requestableOnly)
                tracks = mergePage(tracks, result.tracks)
                loaded = page
                total = result.totalTracks ?: total
                hasMore = result.nextPage != null && result.tracks.isNotEmpty()
                if (!hasMore) break
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // The list shown stays as it was; the change notice already says what happened.
            return
        }
        update(stationId) {
            it.copy(
                status = FavoriteTracksLoadStatus.Ready,
                tracks = tracks,
                errorMessage = null,
                loadedPages = loaded,
                hasMore = hasMore,
                totalTracks = total,
            )
        }
    }

    /**
     * The redesigned ranked feed names a track's song number only when the track can be requested. The full list names
     * it on every row, so a ranked track takes it from the same album and title there when the full list has it.
     */
    private fun withKnownSongIds(
        stationId: StationId,
        ranked: List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack>,
        full: List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack>,
    ): List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack> {
        if (ranked.all { it.songId != null }) return ranked
        val known = full.filter { it.songId != null && it.albumId != null }
            .groupBy { it.albumId to it.title }
            .filterValues { it.size == 1 }
            .mapValues { it.value.single().songId }
        val read = resolved[stationId].orEmpty()
        return ranked.map { track ->
            if (track.songId != null) {
                track
            } else {
                track.copy(songId = known[track.albumId to track.title] ?: track.albumId?.let { read[it to track.title] })
            }
        }
    }

    /** Adds a page to the tracks already read, skipping any track a shifted page repeats. */
    private fun mergePage(
        shown: List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack>,
        page: List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack>,
    ): List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack> {
        val seen = shown.mapNotNullTo(HashSet()) { it.songId }
        return shown + page.filter { track -> track.songId == null || seen.add(track.songId) }
    }

    /** Reads the first [pages] ranked pages again, in order, so the list shown is the station's current order. */
    private suspend fun reloadRanked(stationId: StationId, pages: Int) {
        val tracks = mutableListOf<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack>()
        var hasMore = false
        var loaded = 0
        try {
            for (page in 1..pages.coerceIn(1, MAX_RANKED_PAGES)) {
                val result = remote.loadRanked(stationId, page)
                tracks += result.tracks
                loaded = page
                hasMore = result.nextPage != null && result.tracks.isNotEmpty()
                if (!hasMore) break
            }
        } catch (cancellation: CancellationException) {
            update(stationId) { it.copy(ranked = it.ranked.copy(status = RankedFavoritesStatus.Idle)) }
            throw cancellation
        } catch (failure: Exception) {
            update(stationId) {
                it.copy(
                    ranked = it.ranked.copy(status = RankedFavoritesStatus.Error),
                    errorMessage = if (failure is FavoritesAuthenticationRequiredException) {
                        "Sign in to this station to load your favorite tracks."
                    } else {
                        it.errorMessage
                    },
                )
            }
            return
        }
        update(stationId) {
            it.copy(ranked = RankedFavoritesState(RankedFavoritesStatus.Ready, withKnownSongIds(stationId, tracks, it.tracks), loaded, hasMore))
        }
    }

    override suspend fun clear(stationId: StationId) = lock(stationId).withLock {
        state(stationId).value = FavoriteTracksState(stationId)
    }

    private fun state(stationId: StationId) = states.getOrPut(stationId) {
        MutableStateFlow(FavoriteTracksState(stationId))
    }

    private fun lock(stationId: StationId) = locks.getOrPut(stationId, ::Mutex)

    private companion object {
        const val MAX_RANKED_PAGES = 40
        const val MAX_LIST_PAGES = 200
    }

    private fun update(stationId: StationId, transform: (FavoriteTracksState) -> FavoriteTracksState) {
        state(stationId).value = transform(state(stationId).value)
    }
}

class UnavailableFavoriteTracksRepository : FavoriteTracksRepository {
    override fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState> =
        kotlinx.coroutines.flow.flowOf(FavoriteTracksState(stationId))

    override suspend fun refresh(stationId: StationId) = Unit
    override suspend fun loadMore(stationId: StationId) = Unit
    override suspend fun setRequestableOnly(stationId: StationId, requestableOnly: Boolean) = Unit
    override suspend fun refreshRanked(stationId: StationId) = Unit
    override suspend fun loadMoreRanked(stationId: StationId) = Unit
    override suspend fun changeFavorite(stationId: StationId, songId: String, change: FavoriteChange) = Unit
    override suspend fun changeRankedFavorite(stationId: StationId, position: Int, change: FavoriteChange) = Unit
    override suspend fun clear(stationId: StationId) = Unit
}
