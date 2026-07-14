package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.RequestSearchResult
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkSongRequestRepositoryTest {
    @Test
    fun `search and album browsing are user initiated`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)

        assertEquals(0, remote.searchCalls)
        repository.search(stationId, "Example", RequestSearchField.Title)
        assertEquals(1, remote.searchCalls)
        assertEquals("Example track", repository.observeRequests(stationId).first().searchResults.single().trackTitle)

        repository.openAlbum(stationId, "ALBUM_1")
        assertEquals(1, remote.albumCalls)
        val albumState = repository.observeRequests(stationId).first()
        assertEquals("Requestable track", albumState.tracks.single().title)
        assertEquals(emptyList<RequestSearchResult>(), albumState.searchResults)
    }

    @Test
    fun `submission requires preparation and is never retried`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkSongRequestRepository(remote)
        repository.openAlbum(stationId, "ALBUM_1")

        repository.confirmRequest(stationId)
        assertEquals(0, remote.submitCalls)

        repository.prepareRequest(stationId, "12345")
        assertEquals("12345", repository.observeRequests(stationId).first().pendingRequest?.songId)
        repository.confirmRequest(stationId, "For the evening listeners")
        repository.confirmRequest(stationId)

        val state = repository.observeRequests(stationId).first()
        assertEquals(1, remote.submitCalls)
        assertEquals("For the evening listeners", remote.lastMessage)
        assertNull(state.pendingRequest)
        assertFalse(state.tracks.single().eligible)
    }

    @Test
    fun `indeterminate confirmation suppresses retry and directs user to queue`() = runTest {
        val remote = FakeRemote().apply { submitFailure = true }
        val repository = NetworkSongRequestRepository(remote)
        repository.openAlbum(stationId, "ALBUM_1")
        repository.prepareRequest(stationId, "12345")

        repository.confirmRequest(stationId)
        repository.confirmRequest(stationId)

        val state = repository.observeRequests(stationId).first()
        assertEquals(1, remote.submitCalls)
        assertNull(state.pendingRequest)
        assertFalse(state.tracks.single().eligible)
        assertEquals(
            "The station may have received this request, but confirmation could not be read. Check Queue before trying again. Nothing was retried.",
            state.errorMessage,
        )
    }

    private class FakeRemote : SongRequestRemoteDataSource {
        var searchCalls = 0
        var albumCalls = 0
        var submitCalls = 0
        var submitFailure = false
        var lastMessage: String? = null

        override suspend fun search(stationId: StationId, query: String, field: RequestSearchField): List<RequestSearchResult> {
            searchCalls++
            return listOf(RequestSearchResult("ALBUM_1", "Example track", "Example album", "2004"))
        }

        override suspend fun loadAlbum(stationId: StationId, albumId: String): RequestAlbum {
            albumCalls++
            return RequestAlbum(
                "Example album",
                listOf(RequestableTrack(albumId, "12345", "Requestable track", "Composer", "3:21", true)),
            )
        }

        override suspend fun submit(
            stationId: StationId,
            track: RequestableTrack,
            message: String,
        ): RequestSubmissionResult {
            submitCalls++
            lastMessage = message
            if (submitFailure) error("Confirmation read failed")
            return RequestSubmissionResult.Submitted("Request accepted")
        }
    }

    private companion object {
        val stationId = StationId("sst")
    }
}
