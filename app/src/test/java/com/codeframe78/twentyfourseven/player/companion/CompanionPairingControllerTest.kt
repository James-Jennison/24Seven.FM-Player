package com.codeframe78.twentyfourseven.player.companion

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionPairingControllerTest {
    private val op = "0123456789abcdef0123456789abcdef"
    private val keyHash = "cd".repeat(32)

    private class FakeChannel : CompanionChannel {
        val incoming = Channel<DecodedFrame>(Channel.UNLIMITED)
        val sent = mutableListOf<CompanionFrame>()
        var closed = false
        override suspend fun readFrame(): DecodedFrame = incoming.receive()
        override suspend fun writeFrame(frame: CompanionFrame) { sent += frame }
        override fun close() { closed = true }
    }

    private class Handler : CompanionRequestHandler {
        var submits = 0
        var approve: suspend (CompanionTrack) -> Boolean = { true }
        override suspend fun resolve(station: String, trackRef: String) = CompanionTrack(station, trackRef, "Song", "Artist", "Album")
        override suspend fun approve(track: CompanionTrack) = approve.invoke(track)
        override suspend fun passesSafetyGate() = true
        override suspend fun submit(track: CompanionTrack): CompanionSubmitOutcome {
            submits++
            return CompanionSubmitOutcome.Accepted
        }
    }

    private fun qrText(host: String = "192.168.1.20", exp: Long = 2000) =
        """{"v":1,"name":"Living room","host":"$host","port":8443,"secret":"${"ab".repeat(16)}","exp":$exp,"kh":"$keyHash"}"""

    private fun kotlinx.coroutines.test.TestScope.controller(
        channel: FakeChannel,
        handler: Handler = Handler(),
        now: Long = 1_000_000L,
    ) = CompanionPairingController(
        scope = backgroundScope,
        clockMillis = { now },
        connect = { channel },
        probe = { _, _ -> ProbedConnection(channel, keyHash) },
        handlerFor = { approve -> object : CompanionRequestHandler by handler {
            override suspend fun approve(track: CompanionTrack) = approve(track)
        } },
        keepAliveMillis = 1_000_000L,
    )

    @Test
    fun `scanning a QR pairs sends the proof and becomes active`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel)
        controller.pairWithQr(qrText(), "Razr", "James", "sst")
        runCurrent()
        assertTrue(channel.sent.single() is CompanionFrame.Pair)
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s1")))
        runCurrent()
        assertEquals(PairingUiState.TrustChoice("Living room"), controller.uiState.value)
        controller.setTrust(false)
        assertEquals(PairingUiState.Active("Living room", "sst"), controller.uiState.value)
    }

    @Test
    fun `an expired or non-LAN code is refused without connecting`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, now = 5_000_000L)
        controller.pairWithQr(qrText(exp = 2000), "Razr", "James", "sst")
        assertTrue((controller.uiState.value as PairingUiState.Message).text.contains("expired"))
        val other = controller(channel)
        other.pairWithQr(qrText(host = "8.8.8.8"), "Razr", "James", "sst")
        assertTrue(other.uiState.value is PairingUiState.Message)
        runCurrent()
        assertTrue(channel.sent.isEmpty())
    }

    @Test
    fun `a TV proposal asks the listener and an approval submits once`() = runTest {
        val channel = FakeChannel()
        val handler = Handler()
        val controller = controller(channel, handler)
        controller.pairWithQr(qrText(), "Razr", "James", "sst")
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s1")))
        runCurrent()
        controller.setTrust(false)
        channel.sent.clear()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.Propose(op, "sst", "B000:12", "Song", "Artist", "Album")))
        runCurrent()
        val approval = controller.uiState.value as PairingUiState.Approval
        assertEquals("Song", approval.track.title)
        controller.decideRequest(true)
        runCurrent()
        assertEquals(1, handler.submits)
        assertEquals(CompanionFrame.Result(op, CompanionResultCode.Accepted), channel.sent.last())
        assertEquals(PairingUiState.Active("Living room", "sst"), controller.uiState.value)
    }

    @Test
    fun `declining on the phone never submits`() = runTest {
        val channel = FakeChannel()
        val handler = Handler()
        val controller = controller(channel, handler)
        controller.pairWithQr(qrText(), "Razr", "James", "sst")
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s1")))
        runCurrent()
        controller.setTrust(false)
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.Propose(op, "sst", "B000:12", "Song", "Artist", "Album")))
        runCurrent()
        controller.decideRequest(false)
        runCurrent()
        assertEquals(0, handler.submits)
        assertEquals(CompanionFrame.Declined(op), channel.sent.last())
    }

    @Test
    fun `typed pairing sends nothing until the fingerprint is confirmed`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel)
        controller.pairWithCode("192.168.1.20", 8443, "123456", "Razr", "James", "sst")
        runCurrent()
        assertEquals(PairingUiState.ConfirmFingerprint(PairingFingerprint.of(keyHash)), controller.uiState.value)
        assertTrue(channel.sent.isEmpty())
        controller.confirmFingerprint(true)
        runCurrent()
        val pair = channel.sent.single() as CompanionFrame.Pair
        assertEquals(PairingProof.computeTyped("123456", keyHash), pair.proof)
    }

    @Test
    fun `a rejected fingerprint closes the connection and sends nothing`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel)
        controller.pairWithCode("192.168.1.20", 8443, "123456", "Razr", "James", "sst")
        runCurrent()
        controller.confirmFingerprint(false)
        runCurrent()
        assertTrue(channel.sent.isEmpty())
        assertTrue(channel.closed)
        assertTrue(controller.uiState.value is PairingUiState.Message)
    }

    @Test
    fun `losing the connection ends the pairing with a message`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel)
        controller.pairWithQr(qrText(), "Razr", "James", "sst")
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s1")))
        runCurrent()
        channel.incoming.send(DecodedFrame.Malformed)
        runCurrent()
        assertTrue((controller.uiState.value as PairingUiState.Message).text.contains("lost"))
        assertTrue(channel.closed)
    }

    @Test
    fun `typed details that are not valid are refused before connecting`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel)
        controller.pairWithCode("8.8.8.8", 8443, "123456", "Razr", "James", "sst")
        assertTrue(controller.uiState.value is PairingUiState.Message)
        controller.close()
        val other = controller(channel)
        other.pairWithCode("192.168.1.20", 8443, "12345", "Razr", "James", "sst")
        assertTrue(other.uiState.value is PairingUiState.Message)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.pairedTrusted(channel: FakeChannel, handler: Handler, trust: Boolean): CompanionPairingController {
        val controller = controller(channel, handler)
        controller.pairWithQr(qrText(), "Razr", "James", "sst")
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s1")))
        runCurrent()
        assertEquals(PairingUiState.TrustChoice("Living room"), controller.uiState.value)
        controller.setTrust(trust)
        channel.sent.clear()
        return controller
    }

    @Test
    fun `a trusted TV submits without the approval screen and shows a notice`() = runTest {
        val channel = FakeChannel()
        val handler = Handler()
        val controller = pairedTrusted(channel, handler, trust = true)
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.Propose(op, "sst", "B000:12", "Song", "Artist", "Album")))
        runCurrent()
        assertEquals(1, handler.submits)
        assertEquals(CompanionFrame.Result(op, CompanionResultCode.Accepted), channel.sent.last())
        assertEquals(PairingUiState.Active("Living room", "sst", trusted = true, notice = "Requesting: Song"), controller.uiState.value)
    }

    @Test
    fun `a trusted TV is still stopped by the community safety gate`() = runTest {
        val channel = FakeChannel()
        val handler = object : CompanionRequestHandler by Handler() {
            var submitted = false
            override suspend fun passesSafetyGate() = false
            override suspend fun submit(track: CompanionTrack): CompanionSubmitOutcome { submitted = true; return CompanionSubmitOutcome.Accepted }
        }
        val controller = CompanionPairingController(
            scope = backgroundScope, clockMillis = { 1_000_000L }, connect = { channel },
            probe = { _, _ -> ProbedConnection(channel, keyHash) },
            handlerFor = { approve -> object : CompanionRequestHandler by handler {
                override suspend fun approve(track: CompanionTrack) = approve(track)
            } },
            keepAliveMillis = 1_000_000L,
        )
        controller.pairWithQr(qrText(), "Razr", "James", "sst")
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s1")))
        runCurrent()
        controller.setTrust(true)
        channel.sent.clear()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.Propose(op, "sst", "B000:12", "Song", "Artist", "Album")))
        runCurrent()
        assertTrue(!handler.submitted)
        assertEquals(CompanionFrame.Declined(op), channel.sent.last())
    }

    @Test
    fun `declining the trust question keeps asking for every request`() = runTest {
        val channel = FakeChannel()
        val handler = Handler()
        val controller = pairedTrusted(channel, handler, trust = false)
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.Propose(op, "sst", "B000:12", "Song", "Artist", "Album")))
        runCurrent()
        assertTrue(controller.uiState.value is PairingUiState.Approval)
        assertEquals(0, handler.submits)
    }

    @Test
    fun `an unanswered trust question never auto-approves`() = runTest {
        val channel = FakeChannel()
        val handler = Handler()
        val controller = controller(channel, handler)
        controller.pairWithQr(qrText(), "Razr", "James", "sst")
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s1")))
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.Propose(op, "sst", "B000:12", "Song", "Artist", "Album")))
        runCurrent()
        assertTrue(controller.uiState.value is PairingUiState.Approval)
        assertEquals(0, handler.submits)
    }

    @Test
    fun `trust ends with the pairing and the next pairing asks again`() = runTest {
        val channel = FakeChannel()
        val handler = Handler()
        val controller = pairedTrusted(channel, handler, trust = true)
        controller.close()
        controller.dismissMessage()
        val second = FakeChannel()
        val again = controller(second, handler)
        again.pairWithQr(qrText(), "Razr", "James", "sst")
        runCurrent()
        second.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s2")))
        runCurrent()
        assertEquals(PairingUiState.TrustChoice("Living room"), again.uiState.value)
    }
}
