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

    override fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState> =
        state(stationId).asStateFlow()

    override suspend fun refresh(stationId: StationId) {
        lock(stationId).withLock { readFirstPage(stationId) }
    }

    override suspend fun setRequestableOnly(stationId: StationId, requestableOnly: Boolean): Unit =
        lock(stationId).withLock {
            val before = state(stationId).value.requestableOnly
            if (before == requestableOnly) return@withLock
            update(stationId) { it.copy(requestableOnly = requestableOnly) }
            // The tracks shown were read with the earlier choice, so a failed read puts the choice back with them.
            if (!readFirstPage(stationId)) update(stationId) { it.copy(requestableOnly = before) }
        }

    /** Reads page one of the full list; false when it could not be read. */
    private suspend fun readFirstPage(stationId: StationId): Boolean {
        update(stationId) { it.copy(status = FavoriteTracksLoadStatus.Loading, errorMessage = null) }
        val requestableOnly = state(stationId).value.requestableOnly
        return runCatching { remote.load(stationId, 1, requestableOnly) }
            .onSuccess { page ->
                update(stationId) {
                    it.copy(
                        status = FavoriteTracksLoadStatus.Ready,
                        tracks = page.tracks,
                        errorMessage = null,
                        loadedPages = 1,
                        hasMore = page.nextPage != null && page.tracks.isNotEmpty(),
                        loadingMore = false,
                        moreFailed = false,
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
            .isSuccess
    }

    override suspend fun loadMore(stationId: StationId): Unit = lock(stationId).withLock {
        val current = state(stationId).value
        if (current.status != FavoriteTracksLoadStatus.Ready || !current.hasMore || current.loadedPages >= MAX_LIST_PAGES) {
            return@withLock
        }
        update(stationId) { it.copy(loadingMore = true, moreFailed = false) }
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
                    moreFailed = failure !is FavoritesAuthenticationRequiredException,
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
            update(stationId) { it.copy(ranked = ranked.copy(moreFailed = true)) }
            return@withLock
        }
        update(stationId) {
            it.copy(
                ranked = RankedFavoritesState(
                    RankedFavoritesStatus.Ready,
                    ranked.tracks + withKnownSongIds(page.tracks, it.tracks),
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
                // Read from the album page each time, so a number is never older than the change it is used for.
                val songId = remote.findSongId(stationId, albumId, track.title)
                if (songId != null) return@withLock applyChange(stationId, songId, change, row = track)
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

    /**
     * [row] is the ranked row a song number was read from an album page for; otherwise [songId] must be on a row
     * shown, so nothing is sent for a number the lists do not name.
     */
    private suspend fun applyChange(
        stationId: StationId,
        songId: String,
        change: FavoriteChange,
        row: com.codeframe78.twentyfourseven.player.domain.FavoriteTrack? = null,
    ) {
        run {
            val current = state(stationId).value
            val shown = row ?: current.tracks.firstOrNull { it.songId == songId }
                ?: current.ranked.tracks.firstOrNull { it.songId == songId } ?: return
            val removed = RemovedTrack(songId, shown.albumId, shown.title)
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
            val rankedRead = current.ranked.loadedPages > 0 &&
                reloadRanked(stationId, current.ranked.loadedPages, afterChange = true)
            val listRead = change == FavoriteChange.Remove && current.status == FavoriteTracksLoadStatus.Ready &&
                reloadPages(stationId, current.loadedPages, current.requestableOnly)
            if (change == FavoriteChange.Remove && status == FavoriteChangeStatus.Done) {
                // The station's answer alone does not show a removal happened. Each list that named the track before
                // and was read back says whether it still does; a list that never named it says nothing.
                val after = state(stationId).value
                val stillNamed = listOfNotNull(
                    after.ranked.tracks.takeIf { rankedRead && current.ranked.tracks.any(removed::isSameTrack) },
                    after.tracks.takeIf { listRead && current.tracks.any(removed::isSameTrack) },
                ).map { it.any(removed::isSameTrack) }
                val verdict = when {
                    stillNamed.isEmpty() -> FavoriteChangeStatus.Unconfirmed
                    stillNamed.none { it } -> FavoriteChangeStatus.Done
                    stillNamed.all { it } -> FavoriteChangeStatus.Failed
                    else -> FavoriteChangeStatus.Unconfirmed
                }
                if (verdict != status) update(stationId) { it.copy(change = working.copy(status = verdict)) }
            }
            Unit
        }
    }

    /**
     * Reads the first [pages] pages of the full list again, so a removal shows without losing the pages already read.
     * False when they could not be read.
     */
    private suspend fun reloadPages(stationId: StationId, pages: Int, requestableOnly: Boolean): Boolean {
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
            return false
        }
        update(stationId) {
            it.copy(
                status = FavoriteTracksLoadStatus.Ready,
                tracks = tracks,
                errorMessage = null,
                loadedPages = loaded,
                hasMore = hasMore,
                // The pages read again are the ones already shown, so a next page that failed is still missing.
                moreFailed = it.moreFailed && hasMore && loaded == it.loadedPages,
                totalTracks = total,
            )
        }
        return true
    }

    /**
     * The redesigned ranked feed names a track's song number only when the track can be requested. The full list names
     * it on every row, so a ranked track takes it from the same album and title there when the full list has it.
     */
    private fun withKnownSongIds(
        ranked: List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack>,
        full: List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack>,
    ): List<com.codeframe78.twentyfourseven.player.domain.FavoriteTrack> {
        if (ranked.all { it.songId != null }) return ranked
        val known = full.filter { it.songId != null && it.albumId != null }
            .groupBy { it.albumId to it.title }
            .filterValues { it.size == 1 }
            .mapValues { it.value.single().songId }
        return ranked.map { track ->
            if (track.songId != null) track else track.copy(songId = known[track.albumId to track.title])
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

    /**
     * Reads the first [pages] ranked pages again, in order, so the list shown is the station's current order. False
     * when they could not be read.
     */
    private suspend fun reloadRanked(stationId: StationId, pages: Int, afterChange: Boolean = false): Boolean {
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
                    // After a change the rows already shown stay usable; only a first read with nothing to show is an error.
                    ranked = it.ranked.copy(
                        status = if (afterChange && it.ranked.tracks.isNotEmpty()) RankedFavoritesStatus.Ready else RankedFavoritesStatus.Error,
                    ),
                    errorMessage = if (failure is FavoritesAuthenticationRequiredException) {
                        "Sign in to this station to load your favorite tracks."
                    } else {
                        it.errorMessage
                    },
                )
            }
            return false
        }
        update(stationId) {
            it.copy(
                ranked = RankedFavoritesState(
                    RankedFavoritesStatus.Ready,
                    withKnownSongIds(tracks, it.tracks),
                    loaded,
                    hasMore,
                    // Reading the same pages again after a change leaves a next page that failed still missing.
                    moreFailed = afterChange && it.ranked.moreFailed && hasMore && loaded == it.ranked.loadedPages,
                ),
            )
        }
        return true
    }

    override suspend fun clear(stationId: StationId) = lock(stationId).withLock {
        state(stationId).value = FavoriteTracksState(stationId)
    }

    private fun state(stationId: StationId) = states.getOrPut(stationId) {
        MutableStateFlow(FavoriteTracksState(stationId))
    }

    private fun lock(stationId: StationId) = locks.getOrPut(stationId, ::Mutex)

    /** What identifies a removed track in a list read back: its song number, or its album and title on a row without one. */
    private data class RemovedTrack(val songId: String, val albumId: String?, val title: String) {
        fun isSameTrack(track: com.codeframe78.twentyfourseven.player.domain.FavoriteTrack): Boolean =
            track.songId == songId || (track.songId == null && albumId != null && track.albumId == albumId && track.title == title)
    }

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
