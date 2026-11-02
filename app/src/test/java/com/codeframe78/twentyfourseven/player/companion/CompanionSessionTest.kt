package com.codeframe78.twentyfourseven.player.companion

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompanionSessionTest {
    private val op = "0123456789abcdef0123456789abcdef"
    private val op2 = "fedcba9876543210fedcba9876543210"
    private val qr = PairingQr("Living room", "192.168.1.20", 8443, "ab".repeat(16), 2000, "cd".repeat(32))

    private class FakeHandler : CompanionRequestHandler {
        var resolves: Boolean = true
        var gate: Boolean = true
        var outcome: CompanionSubmitOutcome = CompanionSubmitOutcome.Accepted
        var approval = CompletableDeferred<Boolean>()
        var submits = 0
        val calls = mutableListOf<String>()

        override suspend fun resolve(station: String, trackRef: String): CompanionTrack? {
            calls += "resolve:$trackRef"
            return if (resolves) CompanionTrack(station, trackRef, "T", "A", "B") else null
        }
        override suspend fun approve(track: CompanionTrack): Boolean {
            calls += "approve"
            return approval.await()
        }
        override suspend fun passesSafetyGate(): Boolean {
            calls += "gate"
            return gate
        }
        override suspend fun submit(track: CompanionTrack): CompanionSubmitOutcome {
            calls += "submit"
            submits++
            return outcome
        }
    }

    private class Rig(scope: TestScope, val handler: FakeHandler = FakeHandler()) {
        var now = 1_000L
        val sent = mutableListOf<CompanionFrame>()
        val session = CompanionSession(
            scope = scope.backgroundScope,
            clockMillis = { now },
            handler = handler,
            send = { sent += it },
            idleMillis = 60_000L,
        )
    }

    private suspend fun Rig.pairedSession(qr: PairingQr, station: String = "sst") {
        session.startPairing(qr, "Razr", "James", station)
        session.onFrame(CompanionFrame.PairOk("s1"))
        sent.clear()
    }

    private fun propose(op: String, station: String = "sst", ref: String = "ref-1") =
        CompanionFrame.Propose(op, station, ref, "T", "A", "B")

    @Test
    fun `pairing sends the HMAC proof and not the secret`() = runTest {
        val rig = Rig(this)
        rig.session.startPairing(qr, "Razr", "James", "sst")
        val pair = rig.sent.single() as CompanionFrame.Pair
        assertEquals(PairingProof.compute(qr.secretHex, qr.keyHashHex), pair.proof)
        assertEquals(CompanionState.Pairing, rig.session.state.value)
        rig.session.onFrame(CompanionFrame.PairOk("s1"))
        assertEquals(CompanionState.Paired("s1"), rig.session.state.value)
    }

    @Test
    fun `a refused pairing closes the session`() = runTest {
        val rig = Rig(this)
        rig.session.startPairing(qr, "Razr", "James", "sst")
        rig.session.onFrame(CompanionFrame.PairRefused(PairRefusal.Expired))
        assertEquals(CompanionState.Closed(CloseReason.Refused), rig.session.state.value)
    }

    @Test
    fun `an approved request goes awaiting, submitting, result in order and submits once`() = runTest {
        val rig = Rig(this)
        rig.pairedSession(qr)
        rig.session.onFrame(propose(op))
        runCurrent()
        assertEquals(listOf<CompanionFrame>(CompanionFrame.Awaiting(op)), rig.sent)
        rig.handler.approval.complete(true)
        runCurrent()
        assertEquals(
            listOf(
                CompanionFrame.Awaiting(op),
                CompanionFrame.Submitting(op),
                CompanionFrame.Result(op, CompanionResultCode.Accepted),
            ),
            rig.sent,
        )
        assertEquals(1, rig.handler.submits)
        assertEquals(listOf("resolve:ref-1", "approve", "gate", "submit"), rig.handler.calls)
        assertEquals(CompanionState.Paired("s1"), rig.session.state.value)
    }

    @Test
    fun `the listener declining sends declined and never submits`() = runTest {
        val rig = Rig(this)
        rig.pairedSession(qr)
        rig.session.onFrame(propose(op))
        rig.handler.approval.complete(false)
        runCurrent()
        assertEquals(CompanionFrame.Declined(op), rig.sent.last())
        assertEquals(0, rig.handler.submits)
    }

    @Test
    fun `a failed safety gate declines without submitting`() = runTest {
        val rig = Rig(this)
        rig.handler.gate = false
        rig.pairedSession(qr)
        rig.session.onFrame(propose(op))
        rig.handler.approval.complete(true)
        runCurrent()
        assertEquals(CompanionFrame.Declined(op), rig.sent.last())
        assertEquals(0, rig.handler.submits)
    }

    @Test
    fun `a track the phone cannot resolve is declined before the listener is asked`() = runTest {
        val rig = Rig(this)
        rig.handler.resolves = false
        rig.pairedSession(qr)
        rig.session.onFrame(propose(op))
        runCurrent()
        assertEquals(listOf<CompanionFrame>(CompanionFrame.Declined(op)), rig.sent)
        assertTrue("approve" !in rig.handler.calls)
    }

    @Test
    fun `a proposal for another station or while unpaired is declined`() = runTest {
        val rig = Rig(this)
        rig.session.onFrame(propose(op))
        assertEquals(CompanionFrame.Declined(op), rig.sent.single())
        rig.sent.clear()
        rig.pairedSession(qr, station = "sst")
        rig.session.onFrame(propose(op2, station = "afm"))
        assertEquals(CompanionFrame.Declined(op2), rig.sent.single())
        assertEquals(0, rig.handler.submits)
    }

    @Test
    fun `a second proposal during an operation is declined`() = runTest {
        val rig = Rig(this)
        rig.pairedSession(qr)
        rig.session.onFrame(propose(op))
        runCurrent()
        rig.session.onFrame(propose(op2))
        assertEquals(CompanionFrame.Declined(op2), rig.sent.last())
        rig.handler.approval.complete(true)
        runCurrent()
        assertEquals(1, rig.handler.submits)
    }

    @Test
    fun `a repeated operation id replays the final frame without a second submit`() = runTest {
        val rig = Rig(this)
        rig.pairedSession(qr)
        rig.session.onFrame(propose(op))
        rig.handler.approval.complete(true)
        runCurrent()
        rig.sent.clear()
        rig.session.onFrame(propose(op))
        assertEquals(listOf<CompanionFrame>(CompanionFrame.Result(op, CompanionResultCode.Accepted)), rig.sent)
        assertEquals(1, rig.handler.submits)
    }

    @Test
    fun `refused and uncertain outcomes are reported and uncertain is not retried`() = runTest {
        val rig = Rig(this)
        rig.handler.outcome = CompanionSubmitOutcome.Refused("Request limit reached")
        rig.pairedSession(qr)
        rig.session.onFrame(propose(op))
        rig.handler.approval.complete(true)
        runCurrent()
        assertEquals(CompanionFrame.Result(op, CompanionResultCode.Refused, "Request limit reached"), rig.sent.last())

        val rig2 = Rig(this)
        rig2.handler.outcome = CompanionSubmitOutcome.Uncertain
        rig2.pairedSession(qr)
        rig2.session.onFrame(propose(op))
        rig2.handler.approval.complete(true)
        runCurrent()
        assertEquals(CompanionFrame.Result(op, CompanionResultCode.Uncertain), rig2.sent.last())
        assertEquals(1, rig2.handler.submits)
    }

    @Test
    fun `a station change while awaiting approval cancels the operation and ends the pairing`() = runTest {
        val rig = Rig(this)
        rig.pairedSession(qr)
        rig.session.onFrame(propose(op))
        runCurrent()
        rig.session.onStationOrAccountChanged("1980s", "James")
        rig.handler.approval.complete(true)
        runCurrent()
        assertEquals(CompanionState.Closed(CloseReason.StationOrAccountChanged), rig.session.state.value)
        assertEquals(0, rig.handler.submits)
        assertTrue(rig.sent.any { it is CompanionFrame.StateChanged })
    }

    @Test
    fun `a station change during submitting still reports the result and then ends the pairing`() = runTest {
        val rig = Rig(this)
        val gate = CompletableDeferred<Unit>()
        val handler = object : CompanionRequestHandler {
            override suspend fun resolve(station: String, trackRef: String) = CompanionTrack(station, trackRef, "T", "A", "B")
            override suspend fun approve(track: CompanionTrack) = true
            override suspend fun passesSafetyGate() = true
            override suspend fun submit(track: CompanionTrack): CompanionSubmitOutcome {
                gate.await()
                return CompanionSubmitOutcome.Accepted
            }
        }
        val sent = mutableListOf<CompanionFrame>()
        val session = CompanionSession(backgroundScope, { 1_000L }, handler, { sent += it })
        session.startPairing(qr, "Razr", "James", "sst")
        session.onFrame(CompanionFrame.PairOk("s1"))
        session.onFrame(propose(op))
        runCurrent()
        assertEquals(CompanionState.Submitting(op), session.state.value)
        session.onStationOrAccountChanged("afm", "James")
        assertEquals(CompanionState.Submitting(op), session.state.value)
        gate.complete(Unit)
        runCurrent()
        assertEquals(CompanionFrame.Result(op, CompanionResultCode.Accepted), sent.last())
        assertEquals(CompanionState.Closed(CloseReason.StationOrAccountChanged), session.state.value)
    }

    @Test
    fun `revoke from either side closes and the TV revoke is not echoed`() = runTest {
        val rig = Rig(this)
        rig.pairedSession(qr)
        rig.session.onFrame(CompanionFrame.Revoke)
        assertEquals(CompanionState.Closed(CloseReason.RevokedByTv), rig.session.state.value)
        assertTrue(rig.sent.isEmpty())

        val rig2 = Rig(this)
        rig2.pairedSession(qr)
        rig2.session.revoke()
        assertEquals(CompanionFrame.Revoke, rig2.sent.single())
        assertEquals(CompanionState.Closed(CloseReason.RevokedByPhone), rig2.session.state.value)
    }

    @Test
    fun `an idle pairing revokes after the idle limit and a request resets the clock`() = runTest {
        val rig = Rig(this)
        rig.pairedSession(qr)
        rig.now += 59_000
        rig.session.checkIdle()
        assertTrue(rig.session.state.value is CompanionState.Paired)
        rig.now += 2_000
        rig.session.checkIdle()
        assertEquals(CompanionState.Closed(CloseReason.IdleExpired), rig.session.state.value)
        assertEquals(CompanionFrame.Revoke, rig.sent.last())
    }

    @Test
    fun `ping is answered with pong`() = runTest {
        val rig = Rig(this)
        rig.pairedSession(qr)
        rig.session.onFrame(CompanionFrame.Ping)
        assertEquals(CompanionFrame.Pong, rig.sent.single())
    }
}
