package com.codeframe78.twentyfourseven.player.companion

import com.codeframe78.twentyfourseven.player.domain.AuthRepository
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyRepository
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityRepository
import com.codeframe78.twentyfourseven.player.domain.QueueRepository
import com.codeframe78.twentyfourseven.player.domain.RequestConfirmationContext
import com.codeframe78.twentyfourseven.player.domain.RequestSearchTarget
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus
import com.codeframe78.twentyfourseven.player.domain.SongRequestOutcome
import com.codeframe78.twentyfourseven.player.domain.SongRequestRepository
import com.codeframe78.twentyfourseven.player.domain.SongRequestState
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import com.codeframe78.twentyfourseven.player.domain.SUPPORTED_STATION_IDS
import kotlinx.coroutines.flow.first

/** The TV's opaque track reference: `<albumId>:<songId>`, split at the last colon. */
data class CompanionTrackRef(val albumId: String, val songId: String) {
    companion object {
        private val ALBUM_ID = Regex("[A-Za-z0-9._-]{1,64}")
        private val SONG_ID = Regex("[0-9]{1,12}")

        fun parse(ref: String): CompanionTrackRef? {
            val split = ref.lastIndexOf(':')
            if (split <= 0) return null
            val albumId = ref.substring(0, split)
            val songId = ref.substring(split + 1)
            if (!ALBUM_ID.matches(albumId) || !SONG_ID.matches(songId)) return null
            return CompanionTrackRef(albumId, songId)
        }
    }
}

/** Maps the repository's recorded outcome to what the TV is told. Anything unclear is reported as uncertain. */
internal fun SongRequestOutcome?.toCompanionOutcome(): CompanionSubmitOutcome = when (this) {
    SongRequestOutcome.Submitted -> CompanionSubmitOutcome.Accepted
    is SongRequestOutcome.Rejected -> CompanionSubmitOutcome.Refused(message)
    is SongRequestOutcome.NotSent -> CompanionSubmitOutcome.Refused(message)
    SongRequestOutcome.Unconfirmed, null -> CompanionSubmitOutcome.Uncertain
}

/**
 * Runs a TV-proposed request through the same repository path as a request made in the Player. The phone resolves the
 * track from the station's own album page, shows the listener what it read, checks the community safety gate, and
 * submits exactly once.
 *
 * @param approveOnPhone shows the exact track on the phone and returns true only on an explicit approval.
 */
class PlayerCompanionRequestHandler(
    private val stations: StationRepository,
    private val requests: SongRequestRepository,
    private val auth: AuthRepository,
    private val queue: QueueRepository,
    private val listenerActivity: ListenerActivityRepository,
    private val communitySafety: CommunitySafetyRepository,
    private val approveOnPhone: suspend (CompanionTrack) -> Boolean,
) : CompanionRequestHandler {
    private var resolved: RequestableTrack? = null

    override suspend fun resolve(station: String, trackRef: String): CompanionTrack? {
        resolved = null
        val stationId = StationId(station).takeIf { it in SUPPORTED_STATION_IDS } ?: return null
        val ref = CompanionTrackRef.parse(trackRef) ?: return null
        requests.openSearchResult(stationId, RequestSearchTarget.Album(ref.albumId))
        val state: SongRequestState = requests.observeRequests(stationId).first()
        if (state.status != SongRequestLoadStatus.Ready) return null
        val track = state.tracks.firstOrNull { it.songId == ref.songId && it.albumId == ref.albumId && it.eligible }
            ?: return null
        resolved = track
        return CompanionTrack(station, trackRef, track.title, track.artist.orEmpty(), track.albumTitle ?: state.albumTitle.orEmpty())
    }

    override suspend fun approve(track: CompanionTrack): Boolean = approveOnPhone(track)

    override suspend fun passesSafetyGate(): Boolean = communitySafety.observeSafety().first().canContributeCommunityContent

    override suspend fun submit(track: CompanionTrack): CompanionSubmitOutcome {
        val requestable = resolved ?: return CompanionSubmitOutcome.Refused("The track is no longer available.")
        resolved = null
        val stationId = StationId(track.station)
        val authState = auth.observeAuth(stationId).first()
        val account = authState.displayName?.trim().orEmpty()
        if (authState.status != AuthStatus.SignedIn || account.isEmpty()) {
            return CompanionSubmitOutcome.Refused("Sign in to this station on the phone before requesting.")
        }
        val capabilities = stations.availableStations().firstOrNull { it.id == stationId }?.capabilities
        requests.prepareRequest(stationId, requestable, account)
        requests.confirmRequest(
            stationId,
            RequestConfirmationContext(
                auth = authState,
                queue = queue.currentQueue(stationId),
                listenerActivity = listenerActivity.observeActivity(stationId).first(),
                requiresListenerActivity = capabilities?.supportsListenerActivity == true,
            ),
            "",
        )
        queue.refresh(stationId)
        return requests.observeRequests(stationId).first().outcome.toCompanionOutcome()
    }
}
