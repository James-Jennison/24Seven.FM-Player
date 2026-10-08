package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AlbumRatingState
import com.codeframe78.twentyfourseven.player.domain.AlbumRatingStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteActionState
import com.codeframe78.twentyfourseven.player.domain.FavoriteActionStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.TrackActionsRepository
import com.codeframe78.twentyfourseven.player.domain.TrackActionsState
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class NetworkTrackActionsRepository(
    private val remote: TrackActionsRemoteDataSource,
) : TrackActionsRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<TrackActionsState>>()
    private val ratingLocks = ConcurrentHashMap<StationId, Mutex>()

    override fun observeTrackActions(stationId: StationId): Flow<TrackActionsState> =
        state(stationId.canonicalized()).asStateFlow()

    override suspend fun addCurrentTrackToFavorites(stationId: StationId, trackTitle: String) {
        val canonical = stationId.canonicalized()
        if (trackTitle.isBlank()) return
        val working = FavoriteActionState(FavoriteActionStatus.Working, trackTitle)
        // A second tap while the first is still running must not send the request twice.
        val previous = state(canonical).getAndUpdate { current ->
            if (current.favorite.status == FavoriteActionStatus.Working) current else current.copy(favorite = working)
        }
        if (previous.favorite.status == FavoriteActionStatus.Working) return
        val status = try {
            when (remote.addCurrentTrackToFavorites(canonical, trackTitle)) {
                AddFavoriteOutcome.Added -> FavoriteActionStatus.Added
                AddFavoriteOutcome.AlreadyFavorite -> FavoriteActionStatus.AlreadyFavorite
                AddFavoriteOutcome.TrackChanged -> FavoriteActionStatus.TrackChanged
                AddFavoriteOutcome.SignInRequired -> FavoriteActionStatus.SignInRequired
            }
        } catch (cancellation: CancellationException) {
            state(canonical).update { it.copy(favorite = FavoriteActionState()) }
            throw cancellation
        } catch (_: Exception) {
            FavoriteActionStatus.Failed
        }
        state(canonical).update { it.copy(favorite = FavoriteActionState(status, trackTitle)) }
    }

    override suspend fun openAlbumRating(stationId: StationId, albumId: String) {
        val canonical = stationId.canonicalized()
        ratingLock(canonical).withLock {
            state(canonical).update {
                it.copy(rating = AlbumRatingState(AlbumRatingStatus.Loading, albumId = albumId))
            }
            val rating = runCatching { remote.loadAlbumRating(canonical, albumId).toState(albumId) }
                .getOrElse { failure ->
                    if (failure is CancellationException) throw failure
                    AlbumRatingState(AlbumRatingStatus.Error, albumId = albumId)
                }
            state(canonical).update { it.copy(rating = rating) }
        }
    }

    override suspend fun submitAlbumRating(stationId: StationId, value: String) {
        val canonical = stationId.canonicalized()
        ratingLock(canonical).withLock {
            val current = state(canonical).value.rating
            val albumId = current.albumId ?: return
            if (current.status != AlbumRatingStatus.Ready || current.options.none { it.value == value }) return
            state(canonical).update { it.copy(rating = current.copy(status = AlbumRatingStatus.Submitting)) }
            val rating = try {
                when (val result = remote.submitAlbumRating(canonical, albumId, value)) {
                    is AlbumRatingSubmission.Rated ->
                        result.page.toState(albumId).copy(status = AlbumRatingStatus.Rated, options = emptyList())
                    is AlbumRatingSubmission.NotAccepted -> result.page.toState(albumId)
                    AlbumRatingSubmission.Unconfirmed -> current.copy(
                        status = AlbumRatingStatus.Unconfirmed,
                        options = emptyList(),
                    )
                }
            } catch (cancellation: CancellationException) {
                state(canonical).update {
                    it.copy(rating = current.copy(status = AlbumRatingStatus.Unconfirmed, options = emptyList()))
                }
                throw cancellation
            } catch (_: Exception) {
                // The request may have reached the station, so the outcome is unknown rather than failed.
                current.copy(status = AlbumRatingStatus.Unconfirmed, options = emptyList())
            }
            state(canonical).update { it.copy(rating = rating) }
        }
    }

    override suspend fun closeAlbumRating(stationId: StationId) {
        state(stationId.canonicalized()).update { current ->
            // A rating that is on its way keeps its state, so its result is still reported.
            if (current.rating.status == AlbumRatingStatus.Submitting) current else current.copy(rating = AlbumRatingState())
        }
    }

    override suspend fun clear(stationId: StationId) {
        state(stationId.canonicalized()).value = TrackActionsState(stationId.canonicalized())
    }

    private fun AlbumRatingPage.toState(albumId: String) = AlbumRatingState(
        status = when (access) {
            AlbumRatingAccess.CanRate -> AlbumRatingStatus.Ready
            AlbumRatingAccess.AlreadyRated -> AlbumRatingStatus.AlreadyRated
            AlbumRatingAccess.SignInRequired -> AlbumRatingStatus.SignInRequired
            AlbumRatingAccess.Unknown -> AlbumRatingStatus.Error
        },
        albumId = albumId,
        albumTitle = albumTitle,
        artist = artist,
        currentRating = currentRating,
        voteCount = voteCount,
        options = options,
    )

    private fun state(stationId: StationId) = states.getOrPut(stationId) {
        MutableStateFlow(TrackActionsState(stationId))
    }

    private fun ratingLock(stationId: StationId) = ratingLocks.getOrPut(stationId, ::Mutex)
}
