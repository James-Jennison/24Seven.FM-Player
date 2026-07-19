package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.RequestSearchResult
import com.codeframe78.twentyfourseven.player.domain.RequestSearchTarget
import com.codeframe78.twentyfourseven.player.domain.RequestSuggestionMode
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityState
import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.RequestConfirmationContext
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NetworkSongRequestRepositoryTest {
    @Test
    fun `search and album browsing are user initiated`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)

        assertEquals(0, remote.searchCalls)
        repository.search(stationId, "Example", RequestSearchField.Title)
        assertEquals(1, remote.searchCalls)
        assertEquals("Example track", repository.observeRequests(stationId).first().searchResults.single().title)

        repository.openSearchResult(stationId, albumTarget)
        assertEquals(1, remote.albumCalls)
        val albumState = repository.observeRequests(stationId).first()
        assertEquals("Requestable track", albumState.tracks.single().title)
        assertEquals(emptyList<RequestSearchResult>(), albumState.searchResults)
    }

    @Test
    fun `artist refinement loads albums only after selection`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)

        assertEquals(0, remote.artistAlbumCalls)
        repository.openSearchResult(stationId, RequestSearchTarget.Artist("Example Composer"))

        assertEquals(1, remote.artistAlbumCalls)
        assertEquals("Example Composer", remote.lastArtistName)
        val state = repository.observeRequests(stationId).first()
        assertEquals(albumTarget, state.searchResults.single().target)
        assertEquals(emptyList<RequestableTrack>(), state.tracks)
    }

    @Test
    fun `submission requires preparation and is never retried`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)

        repository.confirmAsTestListener(stationId, readyQueue)
        assertEquals(0, remote.submitCalls)

        repository.prepareRequest(stationId, "12345", "Test Listener")
        assertEquals("12345", repository.observeRequests(stationId).first().pendingRequest?.songId)
        repository.confirmAsTestListener(stationId, readyQueue, "For the evening listeners")
        repository.confirmAsTestListener(stationId, readyQueue)

        val state = repository.observeRequests(stationId).first()
        assertEquals(1, remote.submitCalls)
        assertEquals(2, remote.albumCalls)
        assertEquals("For the evening listeners", remote.lastMessage)
        assertNull(state.pendingRequest)
        assertFalse(state.tracks.single().eligible)
    }

    @Test
    fun `least played suggestion replaces prior browsing results`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.search(stationId, "Example", RequestSearchField.Title)

        repository.suggest(stationId, RequestSuggestionMode.LeastPlayed)

        val state = repository.observeRequests(stationId).first()
        assertEquals(1, remote.suggestCalls)
        assertEquals(RequestSuggestionMode.LeastPlayed, remote.lastSuggestionMode)
        assertEquals(emptyList<RequestSearchResult>(), state.searchResults)
        assertEquals("Suggested track", state.tracks.single().title)
    }

    @Test
    fun `indeterminate confirmation suppresses retry and directs user to queue`() = runTest {
        val remote = FakeRemote().apply { submitFailure = IOException("Station response was too large") }
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")

        repository.confirmAsTestListener(stationId, readyQueue)
        repository.confirmAsTestListener(stationId, readyQueue)

        val state = repository.observeRequests(stationId).first()
        assertEquals(1, remote.submitCalls)
        assertNull(state.pendingRequest)
        assertFalse(state.tracks.single().eligible)
        assertEquals(
            "The station may have received this request, but confirmation could not be read. " +
                "The confirmation page exceeded the safe response limit. " +
                "Check Queue before trying again. Nothing was retried.",
            state.errorMessage,
        )
    }

    @Test
    fun `fresh queue validation blocks a queued track without submitting`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")
        val queue = readyQueue.copy(
            upcoming = listOf(
                QueueTrack(
                    position = 1,
                    displayTitle = "Requestable track",
                    albumId = "ALBUM_1",
                    artistName = "Composer",
                    albumTitle = "Example album",
                ),
            ),
        )

        repository.confirmAsTestListener(stationId, queue)

        val state = repository.observeRequests(stationId).first()
        assertEquals(0, remote.submitCalls)
        assertEquals(TrackRequestStatus.InCurrentQueue, state.tracks.single().availability.status)
        assertTrue(state.errorMessage.orEmpty().startsWith("Track Recently Played"))
    }

    @Test
    fun `missing fresh queue fails closed without submitting`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")

        repository.confirmAsTestListener(stationId, QueueState(stationId, QueueLoadStatus.Error))

        assertEquals(0, remote.submitCalls)
        assertTrue(repository.observeRequests(stationId).first().errorMessage.orEmpty().startsWith("Requests Temporarily Unavailable"))
    }

    @Test
    fun `track leaving queue remains blocked when fresh station data says recently played`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")
        remote.trackAvailability = TrackRequestAvailability(
            TrackRequestStatus.RecentlyPlayed,
            "Requestable again tomorrow.",
        )

        repository.confirmAsTestListener(stationId, readyQueue)

        assertEquals(0, remote.submitCalls)
        assertEquals(
            TrackRequestStatus.RecentlyPlayed,
            repository.observeRequests(stationId).first().tracks.single().availability.status,
        )
    }

    @Test
    fun `account swap or stale queue blocks submission before remote request`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Listener One")

        repository.confirmRequest(
            stationId,
            RequestConfirmationContext(
                auth = AuthState(stationId, AuthStatus.SignedIn, "Listener Two"),
                queue = readyQueue.copy(isStale = true),
            ),
        )

        assertEquals(0, remote.submitCalls)
        assertNull(repository.observeRequests(stationId).first().pendingRequest)
        assertTrue(repository.observeRequests(stationId).first().errorMessage.orEmpty().startsWith("Sign In to Request"))
    }

    @Test
    fun `station reported wait returns the cooldown contract without submitting`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")

        repository.confirmRequest(
            stationId,
            RequestConfirmationContext(
                auth = AuthState(stationId, AuthStatus.SignedIn, "Test Listener"),
                queue = readyQueue,
                listenerActivity = ListenerActivityState(
                    stationId = stationId,
                    status = ListenerActivityLoadStatus.Ready,
                    membershipTier = MembershipTier.Standard,
                    requestReadiness = RequestReadiness.Waiting,
                    waitMinutes = 7,
                ),
                requiresListenerActivity = true,
            ),
        )

        val state = repository.observeRequests(stationId).first()
        assertEquals(0, remote.submitCalls)
        assertNull(state.pendingRequest)
        assertTrue(state.errorMessage.orEmpty().startsWith("Request Cooldown Active"))
        assertTrue(state.errorMessage.orEmpty().contains("7 minutes"))
    }

    @Test
    fun `required listener activity must be fresh and ready before submission`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")

        repository.confirmRequest(
            stationId,
            RequestConfirmationContext(
                auth = AuthState(stationId, AuthStatus.SignedIn, "Test Listener"),
                queue = readyQueue,
                listenerActivity = ListenerActivityState(
                    stationId = stationId,
                    status = ListenerActivityLoadStatus.Ready,
                    membershipTier = MembershipTier.Unknown,
                    requestReadiness = RequestReadiness.Ready,
                ),
                requiresListenerActivity = true,
            ),
        )

        assertEquals(0, remote.submitCalls)
        assertNull(repository.observeRequests(stationId).first().pendingRequest)
        assertTrue(repository.observeRequests(stationId).first().errorMessage.orEmpty().startsWith("Requests Temporarily Unavailable"))
    }

    @Test
    fun `indeterminate result blocks a second prepare from another surface`() = runTest {
        val remote = FakeRemote().apply { submitFailure = IOException("Station response was too large") }
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")
        repository.confirmAsTestListener(stationId, readyQueue)

        repository.prepareRequest(
            stationId,
            repository.observeRequests(stationId).first().tracks.single(),
            "Test Listener",
        )

        assertNull(repository.observeRequests(stationId).first().pendingRequest)
        assertEquals(1, remote.submitCalls)
    }

    @Test
    fun `fresh album metadata change blocks submission`() = runTest {
        val remote = FakeRemote().apply { changeTitleAfterFirstLoad = true }
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")

        repository.confirmAsTestListener(stationId, readyQueue)

        assertEquals(0, remote.submitCalls)
        assertTrue(repository.observeRequests(stationId).first().errorMessage.orEmpty().contains("no longer listed"))
    }

    private class FakeRemote : SongRequestRemoteDataSource {
        var searchCalls = 0
        var albumCalls = 0
        var artistAlbumCalls = 0
        var submitCalls = 0
        var suggestCalls = 0
        var submitFailure: Throwable? = null
        var lastMessage: String? = null
        var lastArtistName: String? = null
        var lastSuggestionMode: RequestSuggestionMode? = null
        var trackAvailability = TrackRequestAvailability.available()
        var changeTitleAfterFirstLoad = false

        override suspend fun search(stationId: StationId, query: String, field: RequestSearchField): List<RequestSearchResult> {
            searchCalls++
            return listOf(
                RequestSearchResult(
                    target = albumTarget,
                    title = "Example track",
                    subtitle = "Example album",
                    year = "2004",
                ),
            )
        }

        override suspend fun loadArtistAlbums(
            stationId: StationId,
            artistName: String,
        ): List<RequestSearchResult> {
            artistAlbumCalls++
            lastArtistName = artistName
            return listOf(
                RequestSearchResult(
                    target = albumTarget,
                    title = "Example album",
                    year = "2004",
                ),
            )
        }

        override suspend fun loadAlbum(stationId: StationId, albumId: String): RequestAlbum {
            albumCalls++
            return RequestAlbum(
                "Example album",
                listOf(
                    RequestableTrack(
                        albumId,
                        "12345",
                        if (changeTitleAfterFirstLoad && albumCalls > 1) "A different track" else "Requestable track",
                        "Composer",
                        "3:21",
                        trackAvailability.canRequest,
                        albumTitle = "Example album",
                        availability = trackAvailability,
                    ),
                ),
            )
        }

        override suspend fun suggest(stationId: StationId, mode: RequestSuggestionMode): RequestAlbum {
            suggestCalls++
            lastSuggestionMode = mode
            return RequestAlbum(
                "Suggested album",
                listOf(RequestableTrack("ALBUM_2", "67890", "Suggested track", "Composer", "1:24", true)),
            )
        }

        override suspend fun submit(
            stationId: StationId,
            track: RequestableTrack,
            message: String,
        ): RequestSubmissionResult {
            submitCalls++
            lastMessage = message
            submitFailure?.let { throw it }
            return RequestSubmissionResult.Submitted("Request accepted")
        }
    }

    private companion object {
        val stationId = StationId("sst")
        val albumTarget = RequestSearchTarget.Album("ALBUM_1")
        val readyQueue = QueueState(stationId, status = QueueLoadStatus.Ready)
    }

    private suspend fun NetworkSongRequestRepository.confirmAsTestListener(
        stationId: StationId,
        queue: QueueState,
        message: String = "",
    ) = confirmRequest(
        stationId,
        RequestConfirmationContext(
            auth = AuthState(stationId, AuthStatus.SignedIn, "Test Listener"),
            queue = queue,
        ),
        message,
    )
}
