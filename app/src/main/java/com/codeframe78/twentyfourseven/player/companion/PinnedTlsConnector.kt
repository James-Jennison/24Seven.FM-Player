package com.codeframe78.twentyfourseven.player.companion

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * Trusts exactly one key: the one whose SHA-256 the TV put in its QR code. The certificate chain and host name
 * are deliberately not used; the pin replaces them, so a LAN attacker without the TV's key cannot connect.
 */
class PinnedTrustManager(private val keyHashHex: String) : X509TrustManager {
    fun matches(publicKeyEncoded: ByteArray): Boolean =
        MessageDigest.isEqual(sha256Hex(publicKeyEncoded).toByteArray(), keyHashHex.lowercase().toByteArray())

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("No TV certificate")
        if (!matches(leaf.publicKey.encoded)) throw CertificateException("TV key does not match the pairing code")
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("The phone does not accept client certificates")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        fun sha256Hex(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

/** One TLS 1.3 connection to the TV carrying length-prefixed JSON frames. */
class CompanionConnection internal constructor(private val socket: SSLSocket) : AutoCloseable {
    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()

    /** Blocking; run off the main thread. */
    fun readFrame(): DecodedFrame = CompanionFrameCodec.decode(CompanionFrameCodec.readPayload(input))

    @Synchronized
    fun writeFrame(frame: CompanionFrame) {
        output.write(CompanionFrameCodec.encode(frame))
        output.flush()
    }

    override fun close() {
        runCatching { socket.close() }
    }
}

class PinnedTlsConnector(private val timeoutMillis: Int = 5_000) {
    /** Connects to a LAN host from a parsed QR payload; never to anything else. */
    fun connect(qr: PairingQr): CompanionConnection {
        require(PairingQr.isLanHost(qr.host)) { "Not a local network address" }
        val context = SSLContext.getInstance("TLSv1.3")
        context.init(null, arrayOf(PinnedTrustManager(qr.keyHashHex)), null)
        val socket = context.socketFactory.createSocket() as SSLSocket
        try {
            socket.enabledProtocols = arrayOf("TLSv1.3")
            socket.soTimeout = READ_TIMEOUT_MILLIS
            socket.connect(InetSocketAddress(qr.host.trim('[', ']'), qr.port), timeoutMillis)
            socket.startHandshake()
        } catch (failure: Exception) {
            runCatching { socket.close() }
            throw failure
        }
        return CompanionConnection(socket)
    }

    private companion object {
        /** Longer than the 15 s keep-alive, so two missed pings surface as a read timeout. */
        const val READ_TIMEOUT_MILLIS = 35_000
    }
}
