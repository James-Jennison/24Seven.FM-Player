package com.codeframe78.twentyfourseven.player.companion

import com.codeframe78.twentyfourseven.player.domain.ExportedStationSession
import com.codeframe78.twentyfourseven.player.domain.SessionCookie
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationSessionExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionSessionHandoffTest {
    private val secret = "SECRETVALUE-9f3a"
    private val keyHash = "cd".repeat(32)
    private val logged = mutableListOf<String>()

    @Before fun captureLog() { CompanionLog.sink = { logged += it } }
    @After fun releaseLog() { CompanionLog.sink = null }

    private val passingVoucher = CommunityVoucher(adultAgeGate = true, termsVersion = "2026-07-18", communityVisible = true)

    private fun cookies(count: Int = 2, value: String = secret) =
        (1..count).map { SessionCookie("c$it", value, if (it == 1) -1L else 3600L, true, "/") }

    private class FakeChannel : CompanionChannel {
        val incoming = Channel<DecodedFrame>(Channel.UNLIMITED)
        val sent = mutableListOf<CompanionFrame>()
        override suspend fun readFrame(): DecodedFrame = incoming.receive()
        override suspend fun writeFrame(frame: CompanionFrame) { sent += frame }
        override fun close() = Unit
    }

    private class Handler : CompanionRequestHandler {
        override suspend fun resolve(station: String, trackRef: String) = CompanionTrack(station, trackRef, "Song", "A", "B")
        override suspend fun approve(track: CompanionTrack) = true
        override suspend fun passesSafetyGate() = true
        override suspend fun submit(track: CompanionTrack) = CompanionSubmitOutcome.Accepted
    }

    private fun qr() =
        """{"v":1,"name":"Living room","host":"192.168.1.20","port":8443,"secret":"${"ab".repeat(16)}","exp":2000,"kh":"$keyHash"}"""

    private fun TestScope.controller(
        channel: FakeChannel,
        exporter: StationSessionExporter?,
        voucher: CommunityVoucher? = passingVoucher,
        blockedReason: String? = null,
    ) = CompanionPairingController(
        scope = backgroundScope,
        clockMillis = { 1_000_000L },
        connect = { channel },
        probe = { _, _ -> ProbedConnection(channel, keyHash) },
        handlerFor = { approve -> object : CompanionRequestHandler by Handler() {
            override suspend fun approve(track: CompanionTrack) = approve(track)
        } },
        keepAliveMillis = 1_000_000L,
        sessionExporter = exporter,
        communityGate = { voucher?.let { CommunityGateCheck(blockedReason == null, it, blockedReason) } },
    )

    private fun exporter(session: ExportedStationSession?) = exporterOf(listOfNotNull(session))

    private fun exporterOf(sessions: List<ExportedStationSession>) = object : StationSessionExporter {
        override suspend fun export(stationId: StationId) = sessions.firstOrNull { it.station == stationId.value }
        override suspend fun exportAll() = sessions
    }

    private suspend fun TestScope.pair(channel: FakeChannel, controller: CompanionPairingController) {
        controller.pairWithQr(qr(), "Razr", "James", "sst")
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.PairOk("s1")))
        runCurrent()
        channel.sent.clear()
    }

    @Test
    fun `the session frame round-trips and its size is bounded`() {
        val frame = CompanionFrame.Session("sst", "James", cookies())
        val bytes = CompanionFrameCodec.encode(frame)
        val decoded = CompanionFrameCodec.decode(bytes.copyOfRange(4, bytes.size)) as DecodedFrame.Frame
        assertEquals(frame, decoded.frame)
        val tooBig = CompanionFrame.Session("sst", "James", cookies(count = 16, value = "x".repeat(4000)))
        val failure = runCatching { CompanionFrameCodec.encode(tooBig) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `session and reply frames never print cookie values`() {
        val frame = CompanionFrame.Session("sst", "James", cookies())
        assertFalse(frame.toString().contains(secret))
        assertFalse(ExportedStationSession("sst", "James", cookies()).toString().contains(secret))
        assertFalse(cookies().first().toString().contains(secret))
        assertEquals(CompanionFrame.SessionOk("sst"), (CompanionFrameCodec.decode(
            CompanionFrameCodec.encode(CompanionFrame.SessionOk("sst")).let { it.copyOfRange(4, it.size) }) as DecodedFrame.Frame).frame)
    }

    @Test
    fun `approving the offer sends one session frame and shows the TV's answer`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())))
        pair(channel, controller)
        assertEquals(PairingUiState.HandoffOffer("Living room", listOf(HandoffAccount("SST", "James"))), controller.uiState.value)
        controller.decideHandoff(true)
        runCurrent()
        val sent = channel.sent.single() as CompanionFrame.Session
        assertEquals(cookies(), sent.cookies)
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.SessionOk("sst")))
        runCurrent()
        val active = controller.uiState.value as PairingUiState.Active
        assertEquals("The TV is signed in to SST.", active.notice)
        assertTrue(active.handedOver)
    }

    @Test
    fun `declining the offer sends nothing and moves on to the trust question`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())))
        pair(channel, controller)
        controller.decideHandoff(false)
        runCurrent()
        assertTrue(channel.sent.isEmpty())
        assertEquals(PairingUiState.TrustChoice("Living room"), controller.uiState.value)
    }

    @Test
    fun `a refusal from the TV is shown and nothing else is sent`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())))
        pair(channel, controller)
        controller.decideHandoff(true)
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.SessionRefused("storage full")))
        runCurrent()
        assertEquals("The TV did not accept the sign-in: storage full", (controller.uiState.value as PairingUiState.Active).notice)
        assertFalse((controller.uiState.value as PairingUiState.Active).handedOver)
        assertEquals(1, channel.sent.size)
    }

    @Test
    fun `no offer when nobody is signed in`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(null))
        pair(channel, controller)
        assertEquals(PairingUiState.TrustChoice("Living room"), controller.uiState.value)
        assertTrue(channel.sent.isEmpty())
    }

    @Test
    fun `an oversized session is refused locally and not sent`() = runTest {
        val channel = FakeChannel()
        val big = ExportedStationSession("sst", "James", cookies(count = 16, value = "x".repeat(4000)))
        val controller = controller(channel, exporter(big))
        pair(channel, controller)
        controller.decideHandoff(true)
        runCurrent()
        assertTrue(channel.sent.isEmpty())
        assertTrue((controller.uiState.value as PairingUiState.Active).notice!!.contains("too large"))
    }

    @Test
    fun `cookie values never reach the log and only names and counts do`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())))
        pair(channel, controller)
        controller.decideHandoff(true)
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.SessionRefused("no")))
        runCurrent()
        assertTrue(logged.isNotEmpty())
        assertTrue(logged.none { it.contains(secret) })
        assertTrue(logged.any { it.contains("2 cookies") && it.contains("c1") })
    }

    @Test
    fun `the handed over session is not revoked when the pairing ends`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())))
        pair(channel, controller)
        controller.decideHandoff(true)
        runCurrent()
        controller.close()
        runCurrent()
        assertTrue(channel.sent.none { it is CompanionFrame.Revoke })
        assertEquals(1, channel.sent.count { it is CompanionFrame.Session })
    }

    @Test
    fun `the frame carries the gate the phone read, nested under gate`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())))
        pair(channel, controller)
        controller.decideHandoff(true)
        runCurrent()
        val sent = channel.sent.single() as CompanionFrame.Session
        assertEquals(passingVoucher, sent.voucher)
        val bytes = CompanionFrameCodec.encode(sent)
        val decoded = (CompanionFrameCodec.decode(bytes.copyOfRange(4, bytes.size)) as DecodedFrame.Frame).frame
        assertEquals(sent, decoded)
        val gate = org.json.JSONObject(String(bytes.copyOfRange(4, bytes.size))).getJSONObject("gate")
        assertEquals(true, gate.getBoolean("adultAgeGate"))
        assertEquals("2026-07-18", gate.getString("termsVersion"))
        assertEquals(true, gate.getBoolean("communityVisible"))
    }

    @Test
    fun `a failing community gate blocks the offer and sends no session frame`() = runTest {
        val channel = FakeChannel()
        val failing = CommunityVoucher(adultAgeGate = true, termsVersion = "", communityVisible = true)
        val controller = controller(
            channel, exporter(ExportedStationSession("sst", "James", cookies())), failing,
            blockedReason = "Accept the community terms on this phone first.",
        )
        pair(channel, controller)
        assertEquals(
            PairingUiState.HandoffBlocked("Living room", "Accept the community terms on this phone first."),
            controller.uiState.value,
        )
        controller.decideHandoff(true)
        runCurrent()
        assertTrue(channel.sent.isEmpty())
        controller.acknowledgeHandoffBlocked()
        assertEquals(PairingUiState.TrustChoice("Living room"), controller.uiState.value)
    }

    @Test
    fun `an unreadable gate is treated as failed`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())), voucher = null)
        pair(channel, controller)
        assertTrue(controller.uiState.value is PairingUiState.HandoffBlocked)
        assertTrue(channel.sent.isEmpty())
    }

    @Test
    fun `the TV's refusal reasons for the gate are explained to the listener`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())))
        pair(channel, controller)
        controller.decideHandoff(true)
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.SessionRefused("terms_out_of_date")))
        runCurrent()
        assertTrue((controller.uiState.value as PairingUiState.Active).notice!!.contains("Update the TV app or this app"))
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.SessionRefused("gate_not_passed")))
        runCurrent()
        assertTrue((controller.uiState.value as PairingUiState.Active).notice!!.contains("community settings"))
    }

    @Test
    fun `the log records only that the gate passed and never the terms version`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporter(ExportedStationSession("sst", "James", cookies())))
        pair(channel, controller)
        controller.decideHandoff(true)
        runCurrent()
        assertTrue(logged.any { it.contains("gate passed") })
        assertTrue(logged.none { it.contains("2026-07-18") })
        assertTrue(logged.none { it.contains(secret) })
    }

    private fun twoStations() = listOf(
        ExportedStationSession("sst", "James", cookies(), "SST"),
        ExportedStationSession("1980s", "Jim", cookies(count = 3, value = secret + "b"), "1980s.FM"),
    )

    @Test
    fun `one approval offers every signed-in station and sends a frame for each`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporterOf(twoStations()))
        pair(channel, controller)
        assertEquals(
            PairingUiState.HandoffOffer("Living room", listOf(HandoffAccount("SST", "James"), HandoffAccount("1980s.FM", "Jim"))),
            controller.uiState.value,
        )
        controller.decideHandoff(true)
        runCurrent()
        val sent = channel.sent.filterIsInstance<CompanionFrame.Session>()
        assertEquals(listOf("sst", "1980s"), sent.map { it.station })
        assertEquals(listOf("James", "Jim"), sent.map { it.account })
        assertEquals(listOf(2, 3), sent.map { it.cookies.size })
        assertTrue(sent.all { it.voucher == passingVoucher })
        assertTrue(logged.none { it.contains(secret) })
    }

    @Test
    fun `each station's answer is shown and a refusal for one does not hide the other`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporterOf(twoStations()))
        pair(channel, controller)
        controller.decideHandoff(true)
        runCurrent()
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.SessionOk("sst")))
        channel.incoming.send(DecodedFrame.Frame(CompanionFrame.SessionRefused("already_handed_over", "1980s")))
        runCurrent()
        val active = controller.uiState.value as PairingUiState.Active
        assertTrue(active.handedOver)
        assertTrue(active.notice!!.contains("The TV is signed in to SST."))
        assertTrue(active.notice!!.contains("sign-in to 1980s.FM"))
        assertTrue(active.notice!!.contains("already has a sign-in"))
    }

    @Test
    fun `declining sends nothing for any station`() = runTest {
        val channel = FakeChannel()
        val controller = controller(channel, exporterOf(twoStations()))
        pair(channel, controller)
        controller.decideHandoff(false)
        runCurrent()
        assertTrue(channel.sent.isEmpty())
    }
}
