package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus
import com.codeframe78.twentyfourseven.player.domain.SongRequestRepository
import com.codeframe78.twentyfourseven.player.domain.SongRequestState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class NetworkSongRequestRepository(
    private val remote: SongRequestRemoteDataSource,
) : SongRequestRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<SongRequestState>>()
    private val locks = ConcurrentHashMap<StationId, Mutex>()

    override fun observeRequests(stationId: StationId): Flow<SongRequestState> = state(stationId).asStateFlow()

    override suspend fun search(stationId: StationId, query: String, field: RequestSearchField) =
        lock(stationId).withLock {
            val normalized = query.trim()
            if (normalized.isBlank()) {
                state(stationId).value = state(stationId).value.copy(errorMessage = "Enter something to search for.")
                return@withLock
            }
            update(stationId) { it.copy(status = SongRequestLoadStatus.Loading, query = normalized, searchField = field, errorMessage = null, notice = null, pendingRequest = null) }
            runCatching { remote.search(stationId, normalized, field) }
                .onSuccess { results ->
                    update(stationId) { it.copy(status = SongRequestLoadStatus.Ready, searchResults = results, tracks = emptyList(), albumTitle = null, errorMessage = null, notice = if (results.isEmpty()) "No matching tracks were found." else null) }
                }
                .onFailure { failure(stationId, "Could not search this station right now.") }
        }

    override suspend fun openAlbum(stationId: StationId, albumId: String): Unit = lock(stationId).withLock {
        val knownAlbumTitle = state(stationId).value.searchResults.firstOrNull { it.albumId == albumId }?.albumTitle
        update(stationId) { it.copy(status = SongRequestLoadStatus.Loading, albumTitle = knownAlbumTitle, errorMessage = null, notice = null, pendingRequest = null) }
        runCatching { remote.loadAlbum(stationId, albumId) }
            .onSuccess { album ->
                update(stationId) { it.copy(status = SongRequestLoadStatus.Ready, searchResults = emptyList(), albumTitle = knownAlbumTitle ?: album.title, tracks = album.tracks, errorMessage = null, notice = if (album.tracks.isEmpty()) "No requestable track listing was found for this album." else null) }
            }
            .onFailure { failure(stationId, "Could not load this album right now.") }
        Unit
    }

    override suspend fun prepareRequest(stationId: StationId, songId: String) = lock(stationId).withLock {
        val track = state(stationId).value.tracks.firstOrNull { it.songId == songId && it.eligible } ?: return@withLock
        update(stationId) { it.copy(pendingRequest = track, notice = null, errorMessage = null) }
    }

    override suspend fun cancelRequest(stationId: StationId) = lock(stationId).withLock {
        update(stationId) { it.copy(pendingRequest = null) }
    }

    override suspend fun confirmRequest(stationId: StationId) = lock(stationId).withLock {
        val pending = state(stationId).value.pendingRequest ?: return@withLock
        update(stationId) { it.copy(status = SongRequestLoadStatus.Submitting, errorMessage = null, notice = null) }
        runCatching { remote.submit(stationId, pending) }
            .onSuccess { result ->
                when (result) {
                    is RequestSubmissionResult.Submitted -> update(stationId) { current ->
                        current.copy(status = SongRequestLoadStatus.Ready, pendingRequest = null, notice = result.message, tracks = current.tracks.map { if (it.songId == pending.songId) it.copy(eligible = false) else it })
                    }
                    is RequestSubmissionResult.Rejected -> update(stationId) { it.copy(status = SongRequestLoadStatus.Ready, pendingRequest = null, errorMessage = result.message) }
                    RequestSubmissionResult.AuthenticationRequired -> update(stationId) { it.copy(status = SongRequestLoadStatus.Ready, pendingRequest = null, errorMessage = "Sign in to this station before requesting a song.") }
                }
            }
            .onFailure {
                update(stationId) { current ->
                    current.copy(
                        status = SongRequestLoadStatus.Ready,
                        pendingRequest = null,
                        tracks = current.tracks.map {
                            if (it.songId == pending.songId) it.copy(eligible = false) else it
                        },
                        errorMessage = "The station may have received this request, but confirmation could not be read. Check Queue before trying again. Nothing was retried.",
                    )
                }
            }
    }

    private fun state(stationId: StationId) = states.getOrPut(stationId) { MutableStateFlow(SongRequestState(stationId)) }
    private fun lock(stationId: StationId) = locks.getOrPut(stationId, ::Mutex)
    private fun update(stationId: StationId, transform: (SongRequestState) -> SongRequestState) {
        state(stationId).value = transform(state(stationId).value)
    }
    private fun failure(stationId: StationId, message: String) = update(stationId) {
        it.copy(status = SongRequestLoadStatus.Error, pendingRequest = null, errorMessage = message)
    }
}
