package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.RequestSearchTarget
import com.codeframe78.twentyfourseven.player.domain.RequestSuggestionMode
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus
import com.codeframe78.twentyfourseven.player.domain.SongRequestRepository
import com.codeframe78.twentyfourseven.player.domain.SongRequestState
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.MAX_REQUEST_MESSAGE_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailabilityResolver
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.PreparedSongRequest
import com.codeframe78.twentyfourseven.player.domain.RequestConfirmationContext
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import com.codeframe78.twentyfourseven.player.domain.RequestTransactionBlock
import com.codeframe78.twentyfourseven.player.domain.classifyStationRequestAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.SocketTimeoutException
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
                    update(stationId) { it.copy(status = SongRequestLoadStatus.Ready, searchResults = results, tracks = emptyList(), albumTitle = null, errorMessage = null, notice = if (results.isEmpty()) "No matching library results were found." else null) }
                }
                .onFailure { failure(stationId, "Could not search this station right now.") }
        }

    override suspend fun openSearchResult(stationId: StationId, target: RequestSearchTarget): Unit =
        lock(stationId).withLock {
            when (target) {
                is RequestSearchTarget.Album -> {
                    val knownAlbumTitle = state(stationId).value.searchResults
                        .firstOrNull { it.target == target }
                        ?.let { it.subtitle ?: it.title }
                    update(stationId) {
                        it.copy(
                            status = SongRequestLoadStatus.Loading,
                            albumTitle = knownAlbumTitle,
                            errorMessage = null,
                            notice = null,
                            pendingRequest = null,
                        )
                    }
                    runCatching { remote.loadAlbum(stationId, target.albumId) }
                        .onSuccess { album ->
                            update(stationId) {
                                it.copy(
                                    status = SongRequestLoadStatus.Ready,
                                    searchResults = emptyList(),
                                    albumTitle = knownAlbumTitle ?: album.title,
                                    tracks = album.tracks,
                                    errorMessage = null,
                                    notice = if (album.tracks.isEmpty()) {
                                        "No requestable track listing was found for this album."
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                        .onFailure { failure(stationId, "Could not load this album right now.") }
                }

                is RequestSearchTarget.Artist -> {
                    update(stationId) {
                        it.copy(
                            status = SongRequestLoadStatus.Loading,
                            searchResults = emptyList(),
                            tracks = emptyList(),
                            albumTitle = null,
                            errorMessage = null,
                            notice = null,
                            pendingRequest = null,
                        )
                    }
                    runCatching { remote.loadArtistAlbums(stationId, target.artistName) }
                        .onSuccess { results ->
                            update(stationId) {
                                it.copy(
                                    status = SongRequestLoadStatus.Ready,
                                    searchResults = results,
                                    errorMessage = null,
                                    notice = if (results.isEmpty()) {
                                        "No albums were found for this artist."
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                        .onFailure { failure(stationId, "Could not load this artist's albums right now.") }
                }
            }
        }

    override suspend fun suggest(stationId: StationId, mode: RequestSuggestionMode): Unit =
        lock(stationId).withLock {
            update(stationId) {
                it.copy(
                    status = SongRequestLoadStatus.Loading,
                    searchResults = emptyList(),
                    tracks = emptyList(),
                    albumTitle = null,
                    errorMessage = null,
                    notice = null,
                    pendingRequest = null,
                )
            }
            runCatching { remote.suggest(stationId, mode) }
                .onSuccess { suggestion ->
                    update(stationId) {
                        it.copy(
                            status = SongRequestLoadStatus.Ready,
                            albumTitle = suggestion.title,
                            tracks = suggestion.tracks,
                            notice = if (suggestion.tracks.isEmpty()) {
                                "The station did not return an available suggestion."
                            } else {
                                null
                            },
                        )
                    }
                }
                .onFailure { failure(stationId, "Could not load a station suggestion right now.") }
        }

    override suspend fun prepareRequest(stationId: StationId, songId: String, accountDisplayName: String) = lock(stationId).withLock {
        val track = state(stationId).value.tracks.firstOrNull { it.songId == songId && it.eligible } ?: return@withLock
        prepare(stationId, track, accountDisplayName)
    }

    override suspend fun prepareRequest(stationId: StationId, track: com.codeframe78.twentyfourseven.player.domain.RequestableTrack, accountDisplayName: String) =
        lock(stationId).withLock {
            prepare(stationId, track, accountDisplayName)
        }

    override suspend fun cancelRequest(stationId: StationId) = lock(stationId).withLock {
        update(stationId) { it.copy(pendingRequest = null) }
    }

    override suspend fun clear(stationId: StationId) = lock(stationId).withLock {
        update(stationId) { it.copy(pendingRequest = null, transactionBlocks = emptyList()) }
    }

    override suspend fun confirmRequest(stationId: StationId, context: RequestConfirmationContext, message: String) = lock(stationId).withLock {
        val pending = state(stationId).value.pendingRequest ?: return@withLock
        val normalizedMessage = message.trim()
        require(normalizedMessage.length <= MAX_REQUEST_MESSAGE_CHARACTERS) { "Request message is too long" }
        val contextRejection = contextRejection(stationId, pending, context)
        if (contextRejection != null) {
            unavailable(stationId, contextRejection.rejectionMessage())
            return@withLock
        }
        update(stationId) { it.copy(status = SongRequestLoadStatus.Submitting, errorMessage = null, notice = null) }
        val currentTrack = runCatching { remote.loadAlbum(stationId, pending.track.albumId) }
            .getOrElse {
                update(stationId) { state ->
                    state.copy(
                        status = SongRequestLoadStatus.Ready,
                        pendingRequest = null,
                        errorMessage = "Requests Temporarily Unavailable. Current track eligibility could not be confirmed.",
                    )
                }
                return@withLock
            }
            .tracks
            .firstOrNull { it.songId == pending.track.songId && sameTrackSnapshot(pending.track, it) }
        if (currentTrack == null) {
            update(stationId) {
                it.copy(
                    status = SongRequestLoadStatus.Ready,
                    pendingRequest = null,
                    errorMessage = "Requests Temporarily Unavailable. This track is no longer listed by the station.",
                )
            }
            return@withLock
        }
        val availability = TrackRequestAvailabilityResolver.resolve(
            stationId,
            currentTrack.identity,
            currentTrack.availability,
            context.queue,
        )
        if (!availability.canRequest) {
            update(stationId) {
                it.copy(
                    status = SongRequestLoadStatus.Ready,
                    pendingRequest = null,
                    tracks = it.tracks.map { track ->
                        if (track.songId == pending.track.songId) {
                            track.copy(eligible = false, availability = availability)
                        } else {
                            track
                        }
                    },
                    errorMessage = availability.rejectionMessage(),
                )
            }
            return@withLock
        }
        runCatching { remote.submit(stationId, currentTrack, normalizedMessage) }
            .onSuccess { result ->
                when (result) {
                    is RequestSubmissionResult.Submitted -> update(stationId) { current ->
                        current.copy(
                            status = SongRequestLoadStatus.Ready,
                            pendingRequest = null,
                            notice = result.message,
                            tracks = current.tracks.map {
                                if (it.songId == pending.track.songId) {
                                    it.copy(
                                        eligible = false,
                                        availability = TrackRequestAvailability(
                                            TrackRequestStatus.RequestsUnavailable,
                                            "The request was submitted; confirm its queue position before requesting again.",
                                        ),
                                    )
                                } else {
                                    it
                                }
                            },
                            transactionBlocks = addBlock(
                                current.transactionBlocks,
                                RequestTransactionBlock(
                                    availability = TrackRequestAvailability(
                                        TrackRequestStatus.RequestsUnavailable,
                                        "The request was submitted; confirm its queue position before requesting again.",
                                    ),
                                    identity = pending.track.identity,
                                ),
                            ),
                        )
                    }
                    is RequestSubmissionResult.Rejected -> update(stationId) { current ->
                        val availability = classifyStationRequestAvailability(result.message)
                        current.copy(
                            status = SongRequestLoadStatus.Ready,
                            pendingRequest = null,
                            errorMessage = result.message,
                            transactionBlocks = addBlock(
                                current.transactionBlocks,
                                RequestTransactionBlock(
                                    availability = availability,
                                    identity = availability.takeIfTargetScoped()?.let { pending.track.identity },
                                ),
                            ),
                        )
                    }
                    RequestSubmissionResult.AuthenticationRequired -> update(stationId) { current ->
                        current.copy(
                            status = SongRequestLoadStatus.Ready,
                            pendingRequest = null,
                            errorMessage = "Sign in to this station before requesting a song.",
                            transactionBlocks = addBlock(current.transactionBlocks, RequestTransactionBlock(TrackRequestAvailability(TrackRequestStatus.AuthenticationRequired))),
                        )
                    }
                }
            }
            .onFailure { failure ->
                update(stationId) { current ->
                    current.copy(
                        status = SongRequestLoadStatus.Ready,
                        pendingRequest = null,
                        tracks = current.tracks.map {
                            if (it.songId == pending.track.songId) {
                                it.copy(
                                    eligible = false,
                                    availability = TrackRequestAvailability(
                                        TrackRequestStatus.RequestsUnavailable,
                                        "The request result could not be confirmed; check Queue before trying again.",
                                    ),
                                )
                            } else {
                                it
                            }
                        },
                        transactionBlocks = addBlock(
                            current.transactionBlocks,
                            RequestTransactionBlock(
                                TrackRequestAvailability(
                                    TrackRequestStatus.RequestsUnavailable,
                                    "The request result could not be confirmed; check Queue before trying again.",
                                ),
                                pending.track.identity,
                            ),
                        ),
                        errorMessage = "The station may have received this request, but confirmation could not be read. " +
                            "${confirmationFailureDetail(failure)} Check Queue before trying again. Nothing was retried.",
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

    private fun prepare(stationId: StationId, track: com.codeframe78.twentyfourseven.player.domain.RequestableTrack, accountDisplayName: String) {
        val normalizedName = accountDisplayName.trim()
        val blocked = state(stationId).value.transactionBlocks.firstOrNull { block ->
            block.identity == null || TrackRequestAvailabilityResolver.matches(track.identity, block.identity)
        }
        if (!track.eligible || normalizedName.isBlank() || blocked != null) return
        update(stationId) {
            it.copy(
                pendingRequest = PreparedSongRequest(stationId, normalizedName, track),
                notice = null,
                errorMessage = null,
            )
        }
    }

    private fun contextRejection(
        stationId: StationId,
        pending: PreparedSongRequest,
        context: RequestConfirmationContext,
    ): TrackRequestAvailability? {
        if (pending.stationId != stationId || context.auth.stationId != stationId || context.queue.stationId != stationId) {
            return TrackRequestAvailability(
                TrackRequestStatus.StationUnavailable,
                "Request identity no longer matches the selected station.",
            )
        }
        if (context.auth.status != AuthStatus.SignedIn) {
            return TrackRequestAvailability(
                TrackRequestStatus.AuthenticationRequired,
                "Sign in to the selected station again before requesting.",
            )
        }
        if (context.auth.displayName?.trim() != pending.accountDisplayName) {
            return TrackRequestAvailability(
                TrackRequestStatus.AuthenticationRequired,
                "The signed-in account changed after confirmation opened.",
            )
        }
        if (context.queue.status != QueueLoadStatus.Ready || context.queue.isStale) {
            return TrackRequestAvailability(
                TrackRequestStatus.RequestsUnavailable,
                "Queue status must be refreshed before this track can be requested.",
            )
        }
        if (!context.requiresListenerActivity) return null
        val activity = context.listenerActivity
            ?: return TrackRequestAvailability(
                TrackRequestStatus.RequestsUnavailable,
                "Request activity must be refreshed before this track can be requested.",
            )
        if (activity.stationId != stationId || activity.status != ListenerActivityLoadStatus.Ready) {
            return TrackRequestAvailability(
                TrackRequestStatus.RequestsUnavailable,
                "Request activity must be refreshed for the selected station.",
            )
        }
        if (activity.membershipTier == MembershipTier.Unknown) {
            return TrackRequestAvailability(
                TrackRequestStatus.RequestsUnavailable,
                "Membership status could not be confirmed.",
            )
        }
        if (activity.requestReadiness == RequestReadiness.Waiting || (activity.waitMinutes ?: 0) > 0) {
            val waitDetail = activity.waitMinutes?.takeIf { it > 0 }?.let { " Try again in approximately $it minutes." }.orEmpty()
            return TrackRequestAvailability(
                TrackRequestStatus.UserCooldown,
                "The station reports that this account is still waiting.$waitDetail",
            )
        }
        if (activity.requestReadiness != RequestReadiness.Ready) {
            return TrackRequestAvailability(
                TrackRequestStatus.RequestsUnavailable,
                "Request readiness could not be confirmed.",
            )
        }
        return null
    }

    private fun sameTrackSnapshot(
        pending: com.codeframe78.twentyfourseven.player.domain.RequestableTrack,
        fresh: com.codeframe78.twentyfourseven.player.domain.RequestableTrack,
    ): Boolean = pending.songId == fresh.songId &&
        pending.albumId == fresh.albumId &&
        pending.title.trim() == fresh.title.trim() &&
        (pending.artist.isNullOrBlank() || fresh.artist.isNullOrBlank() || pending.artist.trim() == fresh.artist.trim()) &&
        (pending.albumTitle.isNullOrBlank() || fresh.albumTitle.isNullOrBlank() || pending.albumTitle.trim() == fresh.albumTitle.trim())

    private fun unavailable(stationId: StationId, message: String) = update(stationId) {
        it.copy(status = SongRequestLoadStatus.Ready, pendingRequest = null, errorMessage = message)
    }

    private fun addBlock(
        blocks: List<RequestTransactionBlock>,
        block: RequestTransactionBlock,
    ): List<RequestTransactionBlock> = (blocks + block).takeLast(20)

    private fun TrackRequestAvailability.takeIfTargetScoped(): TrackRequestAvailability? = when (status) {
        TrackRequestStatus.UserCooldown,
        TrackRequestStatus.RequestLimitReached,
        TrackRequestStatus.MembershipRequired,
        TrackRequestStatus.AuthenticationRequired -> null
        else -> this
    }

    private fun confirmationFailureDetail(failure: Throwable): String = when {
        failure is SocketTimeoutException -> "The confirmation timed out."
        failure is IOException && failure.message == "Station response was too large" ->
            "The confirmation page exceeded the safe response limit."
        failure is IOException && failure.message == "Too many station redirects" ->
            "The station returned too many confirmation redirects."
        failure is IOException && failure.message == "Untrusted request scheme" ->
            "The station redirected confirmation to an unsupported protocol."
        failure is IOException && failure.message == "Untrusted request host" ->
            "The station redirected confirmation to an unverified host."
        failure is IOException && failure.message == "Untrusted request port" ->
            "The station redirected confirmation to an unverified port."
        failure is IOException && failure.message == "Unrecognized station request confirmation" ->
            "The station response did not explicitly confirm the request."
        failure is IOException -> "The confirmation connection failed."
        else -> "The confirmation could not be processed."
    }

    private fun TrackRequestAvailability.rejectionMessage(): String = when (status) {
        TrackRequestStatus.InCurrentQueue, TrackRequestStatus.RecentlyPlayed ->
            "Track Recently Played. ${detail.orEmpty()}".trim()
        TrackRequestStatus.AuthenticationRequired -> "Sign In to Request. ${detail.orEmpty()}".trim()
        TrackRequestStatus.UserCooldown -> "Request Cooldown Active. ${detail.orEmpty()}".trim()
        TrackRequestStatus.MembershipRequired -> "VIP Membership Required. ${detail.orEmpty()}".trim()
        TrackRequestStatus.RequestLimitReached -> "Request Limit Reached. ${detail.orEmpty()}".trim()
        TrackRequestStatus.StationUnavailable -> "Station Unavailable. ${detail.orEmpty()}".trim()
        else -> "Requests Temporarily Unavailable. ${detail.orEmpty()}".trim()
    }
}
