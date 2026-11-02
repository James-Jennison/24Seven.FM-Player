package com.codeframe78.twentyfourseven.player.companion

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A track the phone resolved itself from the TV's opaque reference. */
data class CompanionTrack(
    val station: String,
    val trackRef: String,
    val title: String,
    val artist: String,
    val album: String,
)

sealed interface CompanionSubmitOutcome {
    data object Accepted : CompanionSubmitOutcome
    data class Refused(val reason: String) : CompanionSubmitOutcome
    data object Uncertain : CompanionSubmitOutcome
}

/** Everything that touches station state or the listener stays behind this interface on the phone. */
interface CompanionRequestHandler {
    /** Resolves [trackRef] with the phone's own station code; null if it is not a requestable track of [station]. */
    suspend fun resolve(station: String, trackRef: String): CompanionTrack?

    /** Shows the exact track to the listener on the phone; true only on an explicit approval. */
    suspend fun approve(track: CompanionTrack): Boolean

    /** The community safety gate that guards every request made from the Player. */
    suspend fun passesSafetyGate(): Boolean

    /** Makes the single station request. Must never retry. */
    suspend fun submit(track: CompanionTrack): CompanionSubmitOutcome
}

sealed interface CompanionState {
    data object Unpaired : CompanionState
    data object Pairing : CompanionState
    data class Paired(val tvSession: String) : CompanionState
    data class Approving(val op: String) : CompanionState
    data class Submitting(val op: String) : CompanionState
    data class Closed(val reason: CloseReason) : CompanionState
}

enum class CloseReason { Refused, RevokedByTv, RevokedByPhone, IdleExpired, StationOrAccountChanged, ProtocolError }

/**
 * The phone side of one TV pairing. It has no sockets: frames in through [onFrame], frames out through
 * [send], so the whole protocol is testable against a fake TV. One operation runs at a time.
 */
class CompanionSession(
    private val scope: CoroutineScope,
    private val clockMillis: () -> Long,
    private val handler: CompanionRequestHandler,
    private val send: suspend (CompanionFrame) -> Unit,
    private val idleMillis: Long = DEFAULT_IDLE_MILLIS,
) {
    private val mutableState = MutableStateFlow<CompanionState>(CompanionState.Unpaired)
    val state: StateFlow<CompanionState> = mutableState

    private var station: String = ""
    private var lastActivityMillis: Long = clockMillis()
    private var operation: Job? = null
    private val finished = LinkedHashMap<String, CompanionFrame>()
    private var tvSession: String = ""
    private var closeAfterOperation: CloseReason? = null

    suspend fun startPairing(qr: PairingQr, phoneLabel: String, accountLabel: String, stationId: String) =
        sendPair(PairingProof.compute(qr.secretHex, qr.keyHashHex), phoneLabel, accountLabel, stationId)

    /**
     * Typed-code pairing. The caller must already have shown [PairingFingerprint] for the key it read from the live
     * connection and received the listener's confirmation; the proof is not sent before that.
     */
    suspend fun startTypedPairing(
        code: String,
        keyHashHex: String,
        phoneLabel: String,
        accountLabel: String,
        stationId: String,
    ) = sendPair(PairingProof.computeTyped(code, keyHashHex), phoneLabel, accountLabel, stationId)

    private suspend fun sendPair(proof: String, phoneLabel: String, accountLabel: String, stationId: String) {
        check(mutableState.value == CompanionState.Unpaired) { "Already pairing" }
        station = stationId
        mutableState.value = CompanionState.Pairing
        touch()
        send(CompanionFrame.Pair(proof = proof, phone = phoneLabel, account = accountLabel, station = stationId))
    }

    suspend fun onFrame(frame: CompanionFrame) {
        when (frame) {
            is CompanionFrame.PairOk -> if (mutableState.value == CompanionState.Pairing) {
                touch()
                tvSession = frame.session
                mutableState.value = CompanionState.Paired(frame.session)
            }
            is CompanionFrame.PairRefused -> if (mutableState.value == CompanionState.Pairing) {
                mutableState.value = CompanionState.Closed(CloseReason.Refused)
            }
            is CompanionFrame.Propose -> onPropose(frame)
            CompanionFrame.Revoke -> close(CloseReason.RevokedByTv)
            CompanionFrame.Ping -> send(CompanionFrame.Pong)
            else -> Unit
        }
    }

    suspend fun revoke() {
        if (isLive()) send(CompanionFrame.Revoke)
        close(CloseReason.RevokedByPhone)
    }

    /** The phone's station or signed-in account changed: any pending approval is dropped and the pairing ends. */
    suspend fun onStationOrAccountChanged(newStation: String, newAccount: String) {
        if (!isLive()) return
        send(CompanionFrame.StateChanged(newStation, newAccount))
        // A request already submitting cannot be taken back; its result is still reported, then the pairing ends.
        if (mutableState.value is CompanionState.Submitting) {
            closeAfterOperation = CloseReason.StationOrAccountChanged
            return
        }
        close(CloseReason.StationOrAccountChanged)
    }

    /** Called periodically while the screen is foreground. */
    suspend fun checkIdle() {
        if (mutableState.value is CompanionState.Paired && clockMillis() - lastActivityMillis >= idleMillis) {
            send(CompanionFrame.Revoke)
            close(CloseReason.IdleExpired)
        }
    }

    suspend fun onProtocolError() = close(CloseReason.ProtocolError)

    private suspend fun onPropose(frame: CompanionFrame.Propose) {
        finished[frame.op]?.let { send(it); return }
        val paired = mutableState.value is CompanionState.Paired
        if (!paired || frame.station != station) {
            send(CompanionFrame.Declined(frame.op))
            return
        }
        touch()
        mutableState.value = CompanionState.Approving(frame.op)
        operation = scope.launch { runOperation(frame) }
    }

    private suspend fun runOperation(frame: CompanionFrame.Propose) {
        val track = handler.resolve(frame.station, frame.trackRef)
        if (track == null) return finish(frame.op, CompanionFrame.Declined(frame.op))
        send(CompanionFrame.Awaiting(frame.op))
        if (!handler.approve(track) || !handler.passesSafetyGate()) {
            return finish(frame.op, CompanionFrame.Declined(frame.op))
        }
        mutableState.value = CompanionState.Submitting(frame.op)
        send(CompanionFrame.Submitting(frame.op))
        val result = when (val outcome = handler.submit(track)) {
            CompanionSubmitOutcome.Accepted -> CompanionFrame.Result(frame.op, CompanionResultCode.Accepted)
            is CompanionSubmitOutcome.Refused -> CompanionFrame.Result(frame.op, CompanionResultCode.Refused, outcome.reason.take(300))
            CompanionSubmitOutcome.Uncertain -> CompanionFrame.Result(frame.op, CompanionResultCode.Uncertain)
        }
        finish(frame.op, result)
    }

    private suspend fun finish(op: String, frame: CompanionFrame) {
        finished[op] = frame
        while (finished.size > MAX_FINISHED) finished.remove(finished.keys.first())
        touch()
        send(frame)
        val pending = closeAfterOperation
        if (pending != null) {
            closeAfterOperation = null
            close(pending)
        } else if (mutableState.value is CompanionState.Approving || mutableState.value is CompanionState.Submitting) {
            mutableState.value = CompanionState.Paired(tvSession)
        }
    }

    private fun close(reason: CloseReason) {
        if (mutableState.value is CompanionState.Approving) operation?.cancel()
        mutableState.value = CompanionState.Closed(reason)
    }

    private fun isLive(): Boolean = mutableState.value.let {
        it is CompanionState.Paired || it is CompanionState.Approving || it is CompanionState.Submitting
    }

    private fun touch() {
        lastActivityMillis = clockMillis()
    }

    companion object {
        const val DEFAULT_IDLE_MILLIS = 30L * 60L * 1000L
        private const val MAX_FINISHED = 16
    }
}
