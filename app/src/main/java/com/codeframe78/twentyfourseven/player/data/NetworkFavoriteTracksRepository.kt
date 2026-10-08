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

    override suspend fun refresh(stationId: StationId): Unit = lock(stationId).withLock {
        update(stationId) { it.copy(status = FavoriteTracksLoadStatus.Loading, errorMessage = null) }
        runCatching { remote.load(stationId) }
            .onSuccess { tracks ->
                update(stationId) {
                    it.copy(
                        status = FavoriteTracksLoadStatus.Ready,
                        tracks = tracks,
                        errorMessage = null,
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
                    ranked.tracks + page.tracks,
                    loadedPages = next,
                    hasMore = page.nextPage != null && page.tracks.isNotEmpty(),
                ),
            )
        }
    }

    override suspend fun changeFavorite(stationId: StationId, songId: String, change: FavoriteChange): Unit =
        lock(stationId).withLock {
            val current = state(stationId).value
            if (current.tracks.none { it.songId == songId } && current.ranked.tracks.none { it.songId == songId }) return@withLock
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
            if (status == FavoriteChangeStatus.SignInRequired) return@withLock
            // A move only changes the ranked order; the full list is sorted by the station and is re-read after a removal.
            if (current.ranked.loadedPages > 0) reloadRanked(stationId, current.ranked.loadedPages)
            if (change == FavoriteChange.Remove && current.status == FavoriteTracksLoadStatus.Ready) {
                runCatching { remote.load(stationId) }.onSuccess { tracks ->
                    update(stationId) { it.copy(status = FavoriteTracksLoadStatus.Ready, tracks = tracks, errorMessage = null) }
                }
            }
            Unit
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
        update(stationId) { it.copy(ranked = RankedFavoritesState(RankedFavoritesStatus.Ready, tracks, loaded, hasMore)) }
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
    }

    private fun update(stationId: StationId, transform: (FavoriteTracksState) -> FavoriteTracksState) {
        state(stationId).value = transform(state(stationId).value)
    }
}

class UnavailableFavoriteTracksRepository : FavoriteTracksRepository {
    override fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState> =
        kotlinx.coroutines.flow.flowOf(FavoriteTracksState(stationId))

    override suspend fun refresh(stationId: StationId) = Unit
    override suspend fun refreshRanked(stationId: StationId) = Unit
    override suspend fun loadMoreRanked(stationId: StationId) = Unit
    override suspend fun changeFavorite(stationId: StationId, songId: String, change: FavoriteChange) = Unit
    override suspend fun clear(stationId: StationId) = Unit
}
