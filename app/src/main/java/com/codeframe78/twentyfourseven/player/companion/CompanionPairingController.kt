package com.codeframe78.twentyfourseven.player.companion

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface PairingUiState {
    data object Idle : PairingUiState
    data object Connecting : PairingUiState
    /** Typed pairing: the listener compares [fingerprint] with the TV screen before anything is sent. */
    data class ConfirmFingerprint(val fingerprint: String) : PairingUiState
    data class Active(val tvName: String, val station: String) : PairingUiState
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
) {
    private val mutableState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = mutableState

    private var session: CompanionSession? = null
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
        val decision = CompletableDeferred<Boolean>().also { approval = it }
        mutableState.value = PairingUiState.Approval(tvName, track)
        return decision.await()
    }

    private suspend fun observe(live: CompanionSession) {
        live.state.collect { state ->
            when (state) {
                is CompanionState.Paired -> mutableState.value = PairingUiState.Active(tvName, station)
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
                    is DecodedFrame.Frame -> activeSession.onFrame(decoded.frame)
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
