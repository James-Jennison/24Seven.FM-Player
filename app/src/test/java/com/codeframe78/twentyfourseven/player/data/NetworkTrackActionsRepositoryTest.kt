package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AlbumRatingOption
import com.codeframe78.twentyfourseven.player.domain.AlbumRatingStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteActionStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class NetworkTrackActionsRepositoryTest {
    private val station = StationId("sst")

    @Test
    fun `a repeated favorite tap sends one request and reports its result against the track`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote(favorite = { gate.await(); AddFavoriteOutcome.Added })
        val repository = NetworkTrackActionsRepository(remote)

        launch { repository.addCurrentTrackToFavorites(station, "The Story") }
        runCurrent()
        repository.addCurrentTrackToFavorites(station, "The Story")
        assertEquals(FavoriteActionStatus.Working, repository.observeTrackActions(station).first().favorite.status)
        gate.complete(Unit)
        runCurrent()

        val favorite = repository.observeTrackActions(station).first().favorite
        assertEquals(1, remote.favoriteRequests)
        assertEquals(FavoriteActionStatus.Added, favorite.status)
        assertEquals("The Story", favorite.trackTitle)
    }

    @Test
    fun `a failed favorite is reported as failed`() = runTest {
        val repository = NetworkTrackActionsRepository(FakeRemote(favorite = { throw IOException("offline") }))

        repository.addCurrentTrackToFavorites(station, "The Story")

        assertEquals(FavoriteActionStatus.Failed, repository.observeTrackActions(station).first().favorite.status)
    }

    @Test
    fun `a rating is sent only for an option the station offered`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkTrackActionsRepository(remote)
        repository.openAlbumRating(station, "B0028ERCMU")

        repository.submitAlbumRating(station, "9")
        assertEquals(0, remote.ratingRequests)

        repository.submitAlbumRating(station, "4.5")
        val rating = repository.observeTrackActions(station).first().rating
        assertEquals(1, remote.ratingRequests)
        assertEquals(AlbumRatingStatus.Rated, rating.status)
    }

    @Test
    fun `a rating whose request failed is unconfirmed and cannot be sent again from the same form`() = runTest {
        val remote = FakeRemote(submit = { throw IOException("timeout") })
        val repository = NetworkTrackActionsRepository(remote)
        repository.openAlbumRating(station, "B0028ERCMU")

        repository.submitAlbumRating(station, "4.5")
        repository.submitAlbumRating(station, "4.5")

        assertEquals(AlbumRatingStatus.Unconfirmed, repository.observeTrackActions(station).first().rating.status)
        assertEquals(1, remote.ratingRequests)
    }

    @Test
    fun `closing the rating returns it to idle`() = runTest {
        val repository = NetworkTrackActionsRepository(FakeRemote())
        repository.openAlbumRating(station, "B0028ERCMU")

        repository.closeAlbumRating(station)

        assertEquals(AlbumRatingStatus.Idle, repository.observeTrackActions(station).first().rating.status)
    }

    private class FakeRemote(
        private val favorite: suspend () -> AddFavoriteOutcome = { AddFavoriteOutcome.Added },
        private val submit: suspend () -> AlbumRatingSubmission = { AlbumRatingSubmission.Rated(page(AlbumRatingAccess.AlreadyRated)) },
    ) : TrackActionsRemoteDataSource {
        var favoriteRequests = 0
        var ratingRequests = 0

        override suspend fun addCurrentTrackToFavorites(stationId: StationId, trackTitle: String): AddFavoriteOutcome {
            favoriteRequests++
            return favorite()
        }

        override suspend fun loadAlbumRating(stationId: StationId, albumId: String) = page(AlbumRatingAccess.CanRate)

        override suspend fun submitAlbumRating(stationId: StationId, albumId: String, value: String): AlbumRatingSubmission {
            ratingRequests++
            return submit()
        }
    }

    private companion object {
        fun page(access: AlbumRatingAccess) = AlbumRatingPage(
            access = access,
            albumTitle = "Battlestar Galactica: Season 4",
            artist = "Bear McCreary",
            currentRating = "4.5 out of 5",
            voteCount = "114",
            options = if (access == AlbumRatingAccess.CanRate) {
                listOf(AlbumRatingOption("5", "5.0 - Perfect"), AlbumRatingOption("4.5", "4.5 - Excellent"))
            } else {
                emptyList()
            },
            formFields = emptyList(),
        )
    }
}
