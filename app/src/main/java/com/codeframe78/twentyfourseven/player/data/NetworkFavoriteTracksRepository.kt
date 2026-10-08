package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteChange
import com.codeframe78.twentyfourseven.player.domain.FavoriteChangeState
import com.codeframe78.twentyfourseven.player.domain.FavoriteChangeStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksLoadStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksRepository
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksState
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

    override suspend fun changeFavorite(stationId: StationId, songId: String, change: FavoriteChange): Unit =
        lock(stationId).withLock {
            if (state(stationId).value.tracks.none { it.songId == songId }) return@withLock
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
                // The change may have reached the station, so the list is read again below either way.
                FavoriteChangeStatus.Failed
            }
            update(stationId) { it.copy(change = working.copy(status = status)) }
            if (status == FavoriteChangeStatus.SignInRequired) return@withLock
            runCatching { remote.load(stationId) }.onSuccess { tracks ->
                update(stationId) { it.copy(status = FavoriteTracksLoadStatus.Ready, tracks = tracks, errorMessage = null) }
            }
            Unit
        }

    override suspend fun clear(stationId: StationId) = lock(stationId).withLock {
        state(stationId).value = FavoriteTracksState(stationId)
    }

    private fun state(stationId: StationId) = states.getOrPut(stationId) {
        MutableStateFlow(FavoriteTracksState(stationId))
    }

    private fun lock(stationId: StationId) = locks.getOrPut(stationId, ::Mutex)

    private fun update(stationId: StationId, transform: (FavoriteTracksState) -> FavoriteTracksState) {
        state(stationId).value = transform(state(stationId).value)
    }
}

class UnavailableFavoriteTracksRepository : FavoriteTracksRepository {
    override fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState> =
        kotlinx.coroutines.flow.flowOf(FavoriteTracksState(stationId))

    override suspend fun refresh(stationId: StationId) = Unit
    override suspend fun changeFavorite(stationId: StationId, songId: String, change: FavoriteChange) = Unit
    override suspend fun clear(stationId: StationId) = Unit
}
