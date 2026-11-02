package com.codeframe78.twentyfourseven.player.companion

import com.codeframe78.twentyfourseven.player.domain.ExportedStationSession
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationSessionExporter
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The phone's community gate: [passes] is `canContributeCommunityContent`; [voucher] is what is attested to the TV. */
data class CommunityGateCheck(val passes: Boolean, val voucher: CommunityVoucher, val blockedReason: String?)

sealed interface PairingUiState {
    data object Idle : PairingUiState
    data object Connecting : PairingUiState
    /** Typed pairing: the listener compares [fingerprint] with the TV screen before anything is sent. */
    data class ConfirmFingerprint(val fingerprint: String) : PairingUiState
    /** The handoff is not offered because the phone's own community gate does not pass; [reason] says what to do. */
    data class HandoffBlocked(val tvName: String, val reason: String) : PairingUiState
    /** Asked once after pairing: hand the phone's station session to the TV so it can search and request by itself? */
    data class HandoffOffer(val tvName: String, val station: String, val account: String) : PairingUiState
    /** Asked once after pairing: may this TV request songs without the listener approving each one? */
    data class TrustChoice(val tvName: String) : PairingUiState
    /** [trusted]: requests from this TV skip the approval screen; [notice] names the song just requested that way. */
    data class Active(
        val tvName: String,
        val station: String,
        val trusted: Boolean = false,
        val notice: String? = null,
        /** The TV accepted the phone's station session and can request songs by itself. */
        val handedOver: Boolean = false,
    ) : PairingUiState
    /** The exact track the TV asked for, waiting for the listener's decision on the phone. */
    data class Approval(val tvName: String, val track: CompanionTrack) : PairingUiState
    data class Message(val text: String) : PairingUiState
}

/**
 * Joins the TV channel and the phone-side [CompanionSession] to what the pairing screen shows. Nothing here runs unless
 * [TvCompanionFeature.ENABLED] is on and the screen is open; closing the screen must call [close].
 */
class CompanionPairingController(
    private val scope: CoroutineScope,
    private val clockMillis: () -> Long,
    private val connect: suspend (PairingQr) -> CompanionChannel,
    private val probe: suspend (host: String, port: Int) -> ProbedConnection,
    private val handlerFor: (approve: suspend (CompanionTrack) -> Boolean) -> CompanionRequestHandler,
    private val keepAliveMillis: Long = 15_000L,
    private val sessionExporter: StationSessionExporter? = null,
    /** The phone's community gate at this moment, from the same state the request safety check uses. */
    private val communityGate: suspend () -> CommunityGateCheck? = { null },
) {
    private val mutableState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = mutableState

    private var session: CompanionSession? = null

    /** In memory only, for the life of this pairing: [close] clears it. Rotation does not, because the controller survives it. */
    private var trusted = false
    private var trustAsked = false
    private var notice: String? = null

    /** The session the listener is being asked to hand over. Never put in UI state, logs or messages. */
    private var offered: ExportedStationSession? = null
    private var handoffAsked = false
    private var handedOver = false
    private var channel: CompanionChannel? = null
    private var jobs = mutableListOf<Job>()
    private var tvName = ""
    private var station = ""
    private var approval: CompletableDeferred<Boolean>? = null
    private var fingerprintDecision: CompletableDeferred<Boolean>? = null

    /** QR path: the TV's key hash from the code pins the connection, so `pair` can be sent at once. */
    fun pairWithQr(text: String, phoneLabel: String, accountLabel: String, stationId: String) {
        if (mutableState.value != PairingUiState.Idle && mutableState.value !is PairingUiState.Message) return
        val qr = PairingQr.parse(text)
        if (qr == null) {
            mutableState.value = PairingUiState.Message("That is not a 24Seven.FM TV pairing code.")
            return
        }
        if (qr.isExpired(clockMillis() / 1000)) {
            mutableState.value = PairingUiState.Message("That pairing code has expired. Show a new one on the TV.")
            return
        }
        mutableState.value = PairingUiState.Connecting
        tvName = qr.tvName
        station = stationId
        launch {
            val live = try {
                connect(qr)
            } catch (failure: Exception) {
                CompanionLog.e("pairWithQr: ${failure.javaClass.name}: ${failure.message}")
                return@launch fail("Could not reach the TV. Check that both devices are on the same network.")
            }
            val newSession = start(live, stationId)
            newSession.startPairing(qr, phoneLabel, accountLabel, stationId)
        }
    }

    /** Typed path: connect, show the fingerprint, and send `pair` only after the listener confirms it. */
    fun pairWithCode(host: String, port: Int, code: String, phoneLabel: String, accountLabel: String, stationId: String) {
        if (mutableState.value != PairingUiState.Idle && mutableState.value !is PairingUiState.Message) return
        if (code.length != 6 || !code.all { it in '0'..'9' } || !PairingQr.isLanHost(host) || port !in 1..65535) {
            mutableState.value = PairingUiState.Message("Enter the address, port and six-digit code shown on the TV.")
            return
        }
        mutableState.value = PairingUiState.Connecting
        tvName = host
        station = stationId
        launch {
            val probed = try {
                probe(host, port)
            } catch (failure: Exception) {
                CompanionLog.e("pairWithCode: ${failure.javaClass.name}: ${failure.message}")
                return@launch fail("Could not reach the TV. Check the address and that both devices are on the same network.")
            }
            val decision = CompletableDeferred<Boolean>().also { fingerprintDecision = it }
            mutableState.value = PairingUiState.ConfirmFingerprint(PairingFingerprint.of(probed.keyHashHex))
            if (!decision.await()) {
                runCatching { probed.connection.close() }
                return@launch fail("Pairing cancelled. The code was not sent.")
            }
            CompanionLog.d("fingerprint confirmed, sending pair")
            val newSession = start(probed.connection, stationId)
            newSession.startTypedPairing(code, probed.keyHashHex, phoneLabel, accountLabel, stationId)
        }
    }

    fun showMessage(text: String) {
        mutableState.value = PairingUiState.Message(text)
    }

    /** Back to the start screen after a [PairingUiState.Message]. */
    fun dismissMessage() {
        if (mutableState.value is PairingUiState.Message) mutableState.value = PairingUiState.Idle
    }

    fun confirmFingerprint(matches: Boolean) {
        fingerprintDecision?.complete(matches)
        fingerprintDecision = null
    }

    /** The listener's answer to "let this TV request songs without asking each time?". Asked once per pairing. */
    fun setTrust(trustTv: Boolean) {
        if (mutableState.value !is PairingUiState.TrustChoice) return
        trusted = trustTv
        trustAsked = true
        CompanionLog.d("trust decision: ${if (trustTv) "trusted for this pairing" else "ask each time"}")
        mutableState.value = PairingUiState.Active(tvName, station, trusted)
    }

    /** The listener's answer to "sign this TV in to <station> as <account>?". Decline sends nothing. */
    fun decideHandoff(approve: Boolean) {
        if (mutableState.value !is PairingUiState.HandoffOffer) return
        val exported = offered
        offered = null
        val live = channel
        if (!approve || exported == null || live == null) {
            CompanionLog.d("session handoff: declined")
            mutableState.value = afterHandoffStep()
            return
        }
        notice = "Sending the sign-in to the TV…"
        mutableState.value = PairingUiState.Active(tvName, station, trusted, notice, handedOver)
        launch {
            val gate = readGate()
            if (gate == null || !gate.passes) {
                CompanionLog.d("session handoff: not sent, community gate failed at approval")
                notice = gate?.blockedReason ?: "The community settings on this phone could not be read."
                mutableState.value = PairingUiState.Active(tvName, station, trusted, notice, handedOver)
                return@launch
            }
            val voucher = gate.voucher
            val frame = CompanionFrame.Session(exported.station, exported.account.take(64), exported.cookies, voucher)
            if (runCatching { CompanionFrameCodec.encode(frame) }.isFailure) {
                CompanionLog.e("session handoff: not sent, frame too large or invalid (${exported.cookies.size} cookies)")
                notice = "The sign-in is too large to send to the TV."
                mutableState.value = PairingUiState.Active(tvName, station, trusted, notice, handedOver)
                return@launch
            }
            CompanionLog.d(
                "session handoff: sending ${exported.cookies.size} cookies ${exported.cookies.map { it.name }}, gate passed",
            )
            try {
                live.writeFrame(frame)
            } catch (failure: IOException) {
                CompanionLog.e("session handoff: send failed ${failure.javaClass.name}")
                notice = "The sign-in could not be sent to the TV."
                mutableState.value = PairingUiState.Active(tvName, station, trusted, notice, handedOver)
            }
        }
    }

    private fun onHandoffResult(frame: CompanionFrame) {
        if (frame is CompanionFrame.SessionOk) handedOver = true
        notice = when (frame) {
            is CompanionFrame.SessionOk -> "The TV is signed in to ${frame.station.uppercase()}."
            is CompanionFrame.SessionRefused -> when (frame.reason) {
                "terms_out_of_date" -> "The TV did not accept the sign-in: the community terms differ. Update the TV app or this app."
                "gate_not_passed" -> "The TV did not accept the sign-in: it does not consider the community settings on this phone sufficient."
                else -> "The TV did not accept the sign-in: ${frame.reason.take(100)}"
            }
            else -> return
        }
        CompanionLog.d("session handoff: TV answered ${frame.javaClass.simpleName}")
        val current = mutableState.value
        if (current is PairingUiState.Active) mutableState.value = current.copy(notice = notice, handedOver = handedOver)
    }

    private suspend fun readGate(): CommunityGateCheck? = try {
        communityGate()
    } catch (failure: Exception) {
        CompanionLog.e("session handoff: community gate unreadable ${failure.javaClass.name}")
        null
    }

    /** The listener has read why the handoff was not offered; carry on to the trust question. */
    fun acknowledgeHandoffBlocked() {
        if (mutableState.value is PairingUiState.HandoffBlocked) mutableState.value = afterHandoffStep()
    }

    private fun afterHandoffStep(): PairingUiState =
        if (trustAsked) PairingUiState.Active(tvName, station, trusted, notice, handedOver) else PairingUiState.TrustChoice(tvName)

    private suspend fun afterPaired(): PairingUiState {
        if (handoffAsked) offered = null
        if (!handoffAsked) {
            handoffAsked = true
            val exported = try {
                sessionExporter?.export(StationId(station))
            } catch (failure: Exception) {
                CompanionLog.e("session handoff: export failed ${failure.javaClass.name}: ${failure.message}")
                null
            }
            if (exported != null && exported.cookies.isNotEmpty() && exported.account.isNotBlank()) {
                val gate = readGate()
                if (gate == null || !gate.passes) {
                    CompanionLog.d("session handoff: not offered, community gate failed")
                    return PairingUiState.HandoffBlocked(
                        tvName,
                        gate?.blockedReason ?: "The community settings on this phone could not be read.",
                    )
                }
                offered = exported
                CompanionLog.d("session handoff: offered, ${exported.cookies.size} cookies ${exported.cookies.map { it.name }}")
                return PairingUiState.HandoffOffer(tvName, station, exported.account.take(64))
            }
            CompanionLog.d("session handoff: not offered (exporter=${if (sessionExporter == null) "none" else "returned nothing"}, station=$station)")
        }
        return afterHandoffStep()
    }

    fun decideRequest(approved: Boolean) {
        approval?.complete(approved)
        approval = null
    }

    fun revoke() {
        val live = session
        scope.launch {
            live?.revoke()
            close()
        }
    }

    fun onStationOrAccountChanged(newStation: String, newAccount: String) {
        val live = session ?: return
        scope.launch { live.onStationOrAccountChanged(newStation, newAccount) }
    }

    /** Must be called when the pairing screen closes or the Player leaves the foreground. */
    fun close() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        approval?.complete(false)
        fingerprintDecision?.complete(false)
        approval = null
        fingerprintDecision = null
        channel?.let { runCatching { it.close() } }
        channel = null
        session = null
        trusted = false
        trustAsked = false
        notice = null
        offered = null
        handoffAsked = false
        handedOver = false
        if (mutableState.value !is PairingUiState.Message) mutableState.value = PairingUiState.Idle
    }

    private fun start(live: CompanionChannel, stationId: String): CompanionSession {
        channel = live
        val newSession = CompanionSession(
            scope = scope,
            clockMillis = clockMillis,
            handler = handlerFor { track -> askListener(track) },
            send = { frame -> live.writeFrame(frame) },
        )
        session = newSession
        jobs += scope.launch { observe(newSession) }
        jobs += scope.launch { readLoop(live, newSession) }
        jobs += scope.launch { keepAlive(newSession) }
        return newSession
    }

    private suspend fun askListener(track: CompanionTrack): Boolean {
        if (trusted) {
            // Resolve and the community safety gate have already run and still decide whether anything is submitted.
            CompanionLog.d("trusted TV: approving without the approval screen")
            val text = "Requesting: ${track.title}"
            notice = text
            mutableState.value = PairingUiState.Active(tvName, station, trusted = true, notice = text, handedOver = handedOver)
            scope.launch {
                delay(NOTICE_MILLIS)
                if (notice == text) {
                    notice = null
                    val current = mutableState.value
                    if (current is PairingUiState.Active && current.notice == text) mutableState.value = current.copy(notice = null)
                }
            }
            return true
        }
        CompanionLog.d("untrusted TV: asking the listener")
        val decision = CompletableDeferred<Boolean>().also { approval = it }
        mutableState.value = PairingUiState.Approval(tvName, track)
        return decision.await()
    }

    private suspend fun observe(live: CompanionSession) {
        live.state.collect { state ->
            when (state) {
                is CompanionState.Paired -> {
                    val current = mutableState.value
                    // A pending offer or an active screen already past the question stays as it is.
                    if (current !is PairingUiState.HandoffOffer) mutableState.value = afterPaired()
                }
                is CompanionState.Closed -> {
                    mutableState.value = PairingUiState.Message(closeText(state.reason))
                    close()
                }
                else -> Unit
            }
        }
    }

    private suspend fun readLoop(live: CompanionChannel, activeSession: CompanionSession) {
        try {
            while (true) {
                when (val decoded = live.readFrame()) {
                    is DecodedFrame.Frame -> when (val frame = decoded.frame) {
                        is CompanionFrame.SessionOk, is CompanionFrame.SessionRefused -> onHandoffResult(frame)
                        is CompanionFrame.Session -> Unit
                        else -> activeSession.onFrame(frame)
                    }
                    DecodedFrame.Unknown -> Unit
                    is DecodedFrame.UnsupportedVersion, DecodedFrame.Malformed -> {
                        activeSession.onProtocolError()
                        return
                    }
                }
            }
        } catch (failure: IOException) {
            CompanionLog.e("read loop ended: ${failure.javaClass.name}: ${failure.message}")
            activeSession.onProtocolError()
        } catch (failure: MalformedFrameException) {
            CompanionLog.e("read loop ended: malformed frame: ${failure.message}")
            activeSession.onProtocolError()
        }
    }

    private suspend fun keepAlive(live: CompanionSession) {
        while (true) {
            delay(keepAliveMillis)
            live.checkIdle()
            runCatching { channel?.writeFrame(CompanionFrame.Ping) }
        }
    }

    private fun launch(block: suspend () -> Unit) {
        jobs += scope.launch { block() }
    }

    private companion object {
        const val NOTICE_MILLIS = 6_000L
    }

    private fun fail(text: String) {
        mutableState.value = PairingUiState.Message(text)
        close()
    }

    private fun closeText(reason: CloseReason): String = when (reason) {
        CloseReason.Refused -> "The TV refused the pairing. Show a new code and try again."
        CloseReason.RevokedByTv -> "The TV ended the pairing."
        CloseReason.RevokedByPhone -> "Pairing ended."
        CloseReason.IdleExpired -> "Pairing ended after 30 minutes without a request."
        CloseReason.StationOrAccountChanged -> "Pairing ended because the station or account changed."
        CloseReason.ProtocolError -> "The connection to the TV was lost."
    }
}
