package com.codeframe78.twentyfourseven.player.companion

import java.security.KeyPairGenerator
import java.security.cert.CertificateException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PinnedTrustManagerTest {
    private fun keyBytes(): ByteArray =
        KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair().public.encoded

    @Test
    fun `only the pinned public key is accepted`() {
        val pinned = keyBytes()
        val other = keyBytes()
        val manager = PinnedTrustManager(PinnedTrustManager.sha256Hex(pinned))
        assertTrue(manager.matches(pinned))
        assertFalse(manager.matches(other))
    }

    @Test
    fun `the pin comparison ignores hex case`() {
        val pinned = keyBytes()
        assertTrue(PinnedTrustManager(PinnedTrustManager.sha256Hex(pinned).uppercase()).matches(pinned))
    }

    @Test
    fun `client certificates and an empty chain are rejected`() {
        val manager = PinnedTrustManager("00".repeat(32))
        try {
            manager.checkServerTrusted(emptyArray(), "ECDHE")
            fail("expected rejection")
        } catch (_: CertificateException) {
        }
        try {
            manager.checkClientTrusted(emptyArray(), "ECDHE")
            fail("expected rejection")
        } catch (_: CertificateException) {
        }
    }
}
