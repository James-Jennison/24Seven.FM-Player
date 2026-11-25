package com.codeframe78.twentyfourseven.player.companion

import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/** A frame channel to the TV; the TLS socket in production, an in-memory pair in tests. */
interface CompanionChannel : AutoCloseable {
    /** Suspends until the next frame; throws [java.io.IOException] or [MalformedFrameException] when the link is lost. */
    suspend fun readFrame(): DecodedFrame
    suspend fun writeFrame(frame: CompanionFrame)
}

/** One TLS 1.3 connection to the TV carrying length-prefixed JSON frames. */
class CompanionConnection internal constructor(private val socket: SSLSocket) : CompanionChannel {
    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()
    private val writeLock = Any()

    private val openedAt = System.nanoTime()
    private fun elapsedMs() = (System.nanoTime() - openedAt) / 1_000_000

    override suspend fun readFrame(): DecodedFrame = withContext(Dispatchers.IO) {
        try {
            CompanionFrameCodec.decode(CompanionFrameCodec.readPayload(input)).also {
                CompanionLog.d("read +${elapsedMs()}ms ${describe(it)}")
            }
        } catch (failure: Exception) {
            CompanionLog.e("read failed +${elapsedMs()}ms closed=${socket.isClosed} ${failure.javaClass.name}: ${failure.message}", failure)
            throw failure
        }
    }

    override suspend fun writeFrame(frame: CompanionFrame) = withContext(Dispatchers.IO) {
        try {
            synchronized(writeLock) {
                output.write(CompanionFrameCodec.encode(frame))
                output.flush()
            }
            CompanionLog.d("wrote +${elapsedMs()}ms ${frame.javaClass.simpleName}")
        } catch (failure: Exception) {
            CompanionLog.e("write failed +${elapsedMs()}ms ${frame.javaClass.simpleName} ${failure.javaClass.name}: ${failure.message}", failure)
            throw failure
        }
    }

    override fun close() {
        CompanionLog.d("close +${elapsedMs()}ms closed=${socket.isClosed} from ${Throwable().stackTrace.drop(1).take(4).joinToString(" < ") { "${it.className.substringAfterLast('.')}.${it.methodName}" }}")
        runCatching { socket.close() }
    }

    private fun describe(decoded: DecodedFrame): String = when (decoded) {
        is DecodedFrame.Frame -> decoded.frame.javaClass.simpleName
        else -> decoded.toString()
    }
}

/** Accepts any certificate but records its key hash, for the typed path where the listener confirms the fingerprint. */
class CapturingTrustManager : X509TrustManager {
    @Volatile var keyHashHex: String? = null
        private set

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("No TV certificate")
        keyHashHex = PinnedTrustManager.sha256Hex(leaf.publicKey.encoded)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("The phone does not accept client certificates")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

/** A connection whose TV key is not yet trusted: nothing may be sent until the listener confirms [keyHashHex]. */
class ProbedConnection internal constructor(val connection: CompanionChannel, val keyHashHex: String)

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
            CompanionLog.e("connect failed ${failure.javaClass.name}: ${failure.message}", failure)
            runCatching { socket.close() }
            throw failure
        }
        CompanionLog.d("connected ${socket.session.protocol} ${socket.session.cipherSuite}")
        return CompanionConnection(socket)
    }

    /** Typed path: connect to a LAN host, read the key the TV presents, send nothing. */
    fun probe(host: String, port: Int): ProbedConnection {
        require(PairingQr.isLanHost(host)) { "Not a local network address" }
        require(port in 1..65535)
        val capture = CapturingTrustManager()
        val context = SSLContext.getInstance("TLSv1.3")
        context.init(null, arrayOf(capture), null)
        val socket = context.socketFactory.createSocket() as SSLSocket
        try {
            socket.enabledProtocols = arrayOf("TLSv1.3")
            socket.soTimeout = READ_TIMEOUT_MILLIS
            socket.connect(InetSocketAddress(host.trim('[', ']'), port), timeoutMillis)
            socket.startHandshake()
        } catch (failure: Exception) {
            CompanionLog.e("probe failed ${failure.javaClass.name}: ${failure.message}", failure)
            runCatching { socket.close() }
            throw failure
        }
        CompanionLog.d("probe handshake ok ${socket.session.protocol} ${socket.session.cipherSuite}")
        val hash = capture.keyHashHex
        if (hash == null) {
            runCatching { socket.close() }
            throw CertificateException("The TV presented no key")
        }
        return ProbedConnection(CompanionConnection(socket), hash)
    }

    private companion object {
        /** Longer than the 15 s keep-alive, so two missed pings surface as a read timeout. */
        const val READ_TIMEOUT_MILLIS = 35_000
    }
}
