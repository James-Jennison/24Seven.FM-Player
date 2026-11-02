package com.codeframe78.twentyfourseven.player.companion

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CompanionCodecAndQrTest {
    private val op = "0123456789abcdef0123456789abcdef"

    private fun roundTrip(frame: CompanionFrame): CompanionFrame {
        val bytes = CompanionFrameCodec.encode(frame)
        val payload = CompanionFrameCodec.readPayload(ByteArrayInputStream(bytes))
        return (CompanionFrameCodec.decode(payload) as DecodedFrame.Frame).frame
    }

    @Test
    fun `every frame survives a length-prefixed round trip`() {
        val frames = listOf(
            CompanionFrame.Pair("a".repeat(64), "Razr", "James", "sst"),
            CompanionFrame.PairOk("s1"),
            CompanionFrame.PairRefused(PairRefusal.BadProof),
            CompanionFrame.Propose(op, "sst", "ref-1", "Title", "Artist", "Album"),
            CompanionFrame.Awaiting(op),
            CompanionFrame.Declined(op),
            CompanionFrame.Submitting(op),
            CompanionFrame.Result(op, CompanionResultCode.Refused, "Too soon"),
            CompanionFrame.Result(op, CompanionResultCode.Accepted),
            CompanionFrame.StateChanged("1980s", "James"),
            CompanionFrame.Revoke,
            CompanionFrame.Ping,
            CompanionFrame.Pong,
        )
        frames.forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `a different protocol version is reported and a missing version is malformed`() {
        assertEquals(
            DecodedFrame.UnsupportedVersion(2),
            CompanionFrameCodec.decode("""{"v":2,"type":"ping"}""".toByteArray()),
        )
        assertEquals(DecodedFrame.Malformed, CompanionFrameCodec.decode("""{"type":"ping"}""".toByteArray()))
    }

    @Test
    fun `unknown types are ignored but bad fields are malformed`() {
        assertEquals(DecodedFrame.Unknown, CompanionFrameCodec.decode("""{"v":1,"type":"future"}""".toByteArray()))
        assertEquals(DecodedFrame.Malformed, CompanionFrameCodec.decode("""{"v":1,"type":"awaiting","op":"xyz"}""".toByteArray()))
        assertEquals(DecodedFrame.Malformed, CompanionFrameCodec.decode("not json".toByteArray()))
        assertEquals(
            DecodedFrame.Malformed,
            CompanionFrameCodec.decode(
                """{"v":1,"type":"result","op":"$op","code":"maybe"}""".toByteArray(),
            ),
        )
    }

    @Test
    fun `oversized and truncated frames are rejected`() {
        val tooBig = byteArrayOf(0, 1, 0, 0) // 65536 bytes
        try {
            CompanionFrameCodec.readPayload(ByteArrayInputStream(tooBig))
            fail("expected oversize rejection")
        } catch (_: MalformedFrameException) {
        }
        val truncated = byteArrayOf(0, 0, 0, 40) + "{\"v\":1}".toByteArray()
        try {
            CompanionFrameCodec.readPayload(ByteArrayInputStream(truncated))
            fail("expected truncation rejection")
        } catch (_: MalformedFrameException) {
        }
    }

    @Test
    fun `an overlong track title is malformed`() {
        val json = """{"v":1,"type":"propose","op":"$op","station":"sst","trackRef":"r","title":"${"t".repeat(201)}","artist":"a","album":"b"}"""
        assertEquals(DecodedFrame.Malformed, CompanionFrameCodec.decode(json.toByteArray()))
    }

    private fun qr(host: String = "192.168.1.20", extra: String = ""): String =
        """{"v":1,"name":"Living room","host":"$host","port":8443,"secret":"${"ab".repeat(16)}","exp":2000,"kh":"${"cd".repeat(32)}"$extra}"""

    @Test
    fun `a well-formed LAN payload parses and expires on the TV clock`() {
        val parsed = PairingQr.parse(qr())
        assertNotNull(parsed)
        assertFalse(parsed!!.isExpired(1999))
        assertTrue(parsed.isExpired(2000))
    }

    @Test
    fun `only private network hosts are accepted`() {
        listOf("10.0.0.5", "172.16.4.1", "172.31.255.1", "192.168.0.9", "169.254.1.1", "tv.local", "fe80::1", "fd00::5")
            .forEach { assertNotNull(it, PairingQr.parse(qr(it))) }
        listOf("8.8.8.8", "172.32.0.1", "example.com", "192.168.1.999", "", "2001:db8::1")
            .forEach { assertNull(it, PairingQr.parse(qr(it))) }
    }

    @Test
    fun `payloads with bad fields are rejected`() {
        assertNull(PairingQr.parse("nope"))
        assertNull(PairingQr.parse(qr().replace("\"v\":1", "\"v\":2")))
        assertNull(PairingQr.parse(qr().replace("8443", "70000")))
        assertNull(PairingQr.parse(qr().replace("ab".repeat(16), "zz".repeat(16))))
        assertNull(PairingQr.parse("x".repeat(2000)))
    }

    @Test
    fun `the proof is a stable HMAC that does not contain the secret`() {
        val secret = "00".repeat(16)
        val keyHash = "11".repeat(32)
        val proof = PairingProof.compute(secret, keyHash)
        assertEquals(64, proof.length)
        assertEquals(proof, PairingProof.compute(secret, keyHash))
        assertFalse(proof.contains(secret))
        assertFalse(proof == PairingProof.compute("01".repeat(16), keyHash))
    }
}
