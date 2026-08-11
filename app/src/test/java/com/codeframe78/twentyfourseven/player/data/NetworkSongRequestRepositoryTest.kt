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
        assertEquals(1, remote.albumCalls)
        assertEquals("For the evening listeners", remote.lastMessage)
        assertNull(state.pendingRequest)
        assertTrue(state.tracks.single().eligible)
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
        assertTrue(state.tracks.single().eligible)
        assertEquals(
                "The station may have received this request, but confirmation could not be read. " +
                "The confirmation page exceeded the safe response limit. " +
                "Check Queue before trying again. Nothing was retried.",
            state.errorMessage,
        )
    }

    @Test
    fun `unconfirmed acknowledgement leaves the station-provided track availability unchanged`() = runTest {
        val remote = FakeRemote().apply {
            submitResult = RequestSubmissionResult.Unconfirmed(
                "The station acknowledged the request, but it is not yet visible in Queue. It was not retried.",
            )
        }
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")

        repository.confirmAsTestListener(stationId, readyQueue)

        val state = repository.observeRequests(stationId).first()
        assertEquals(1, remote.submitCalls)
        assertEquals(1, remote.albumCalls)
        assertTrue(state.tracks.single().eligible)
        assertEquals(TrackRequestStatus.Available, state.tracks.single().availability.status)
        assertTrue(state.transactionBlocks.isEmpty())
        assertEquals(
            "The station acknowledged the request, but it is not yet visible in Queue. It was not retried.",
            state.errorMessage,
        )
    }

    @Test
    fun `cached queue does not override fresh station track requestability`() = runTest {
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
        assertEquals(1, remote.submitCalls)
        assertTrue(state.tracks.single().eligible)
        assertNull(state.errorMessage)
    }

    @Test
    fun `missing queue snapshot does not block a station-authoritative submission`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")

        repository.confirmAsTestListener(stationId, QueueState(stationId, QueueLoadStatus.Error))

        assertEquals(1, remote.submitCalls)
        assertNull(repository.observeRequests(stationId).first().errorMessage)
    }

    @Test
    fun `cancelling a request also clears its transient result message`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")
        repository.confirmAsTestListener(stationId, QueueState(stationId, QueueLoadStatus.Error))

        repository.cancelRequest(stationId)

        assertNull(repository.observeRequests(stationId).first().pendingRequest)
        assertNull(repository.observeRequests(stationId).first().errorMessage)
        assertNull(repository.observeRequests(stationId).first().notice)
    }

    @Test
    fun `request endpoint remains authoritative after the user opens confirmation`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")
        remote.trackAvailability = TrackRequestAvailability(
            TrackRequestStatus.RecentlyPlayed,
            "Requestable again tomorrow.",
        )

        repository.confirmAsTestListener(stationId, readyQueue)

        assertEquals(1, remote.submitCalls)
        assertEquals(TrackRequestStatus.Available, repository.observeRequests(stationId).first().tracks.single().availability.status)
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
    fun `listener timer does not override a fresh station requestability check`() = runTest {
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
        assertEquals(1, remote.submitCalls)
        assertNull(state.pendingRequest)
        assertNull(state.errorMessage)
    }

    @Test
    fun `unknown membership display does not block a fresh station submission`() = runTest {
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

        assertEquals(1, remote.submitCalls)
        assertNull(repository.observeRequests(stationId).first().pendingRequest)
        assertNull(repository.observeRequests(stationId).first().errorMessage)
    }

    @Test
    fun `listener activity failure does not override a fresh station submission`() = runTest {
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
                    status = ListenerActivityLoadStatus.Error,
                    errorMessage = "Sign in to this station to load request activity and membership status.",
                ),
                requiresListenerActivity = true,
            ),
        )

        assertEquals(1, remote.submitCalls)
        assertNull(repository.observeRequests(stationId).first().errorMessage)
    }

    @Test
    fun `indeterminate result permits a new user-initiated prepare when station still lists the track`() = runTest {
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

        assertEquals("12345", repository.observeRequests(stationId).first().pendingRequest?.songId)
        assertEquals(1, remote.submitCalls)
    }

    @Test
    fun `submission does not reload the album before or after sending`() = runTest {
        val remote = FakeRemote().apply { changeTitleAfterFirstLoad = true }
        val repository = NetworkSongRequestRepository(remote)
        repository.openSearchResult(stationId, albumTarget)
        repository.prepareRequest(stationId, "12345", "Test Listener")

        repository.confirmAsTestListener(stationId, readyQueue)

        assertEquals(1, remote.submitCalls)
        assertEquals(1, remote.albumCalls)
        assertEquals("Request accepted", repository.observeRequests(stationId).first().notice)
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
        var availabilityAfterSubmit: TrackRequestAvailability? = null
        var submitResult: RequestSubmissionResult = RequestSubmissionResult.Submitted("Request accepted")
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
                        (availabilityAfterSubmit.takeIf { submitCalls > 0 } ?: trackAvailability).canRequest,
                        albumTitle = "Example album",
                        availability = availabilityAfterSubmit.takeIf { submitCalls > 0 } ?: trackAvailability,
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
            return submitResult
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
