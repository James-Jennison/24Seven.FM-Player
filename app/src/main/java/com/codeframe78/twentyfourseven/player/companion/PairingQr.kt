package com.codeframe78.twentyfourseven.player.companion

import java.net.InetAddress
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONException
import org.json.JSONObject

/** The pairing payload the TV shows as a QR code. The secret is one-time and the TV enforces [expiresAtSeconds]. */
data class PairingQr(
    val tvName: String,
    val host: String,
    val port: Int,
    val secretHex: String,
    val expiresAtSeconds: Long,
    val keyHashHex: String,
) {
    fun isExpired(nowSeconds: Long): Boolean = nowSeconds >= expiresAtSeconds

    companion object {
        private val HEX32 = Regex("[0-9a-f]{32}")
        private val HEX64 = Regex("[0-9a-f]{64}")

        /** Returns null for anything that is not a well-formed, LAN-only v1 payload. */
        fun parse(text: String): PairingQr? {
            if (text.length > 1024) return null
            val json = try {
                JSONObject(text)
            } catch (_: JSONException) {
                return null
            }
            if (json.optInt("v", -1) != COMPANION_PROTOCOL_VERSION) return null
            val name = json.opt("name") as? String ?: return null
            val host = json.opt("host") as? String ?: return null
            val secret = json.opt("secret") as? String ?: return null
            val keyHash = json.opt("kh") as? String ?: return null
            val port = json.optInt("port", -1)
            val exp = json.optLong("exp", -1L)
            if (name.isEmpty() || name.length > 64) return null
            if (port !in 1..65535 || exp <= 0L) return null
            if (!HEX32.matches(secret) || !HEX64.matches(keyHash)) return null
            if (!isLanHost(host)) return null
            return PairingQr(name, host, port, secret, exp, keyHash)
        }

        /**
         * The phone only connects to private-network literals or `.local` names, so a crafted QR code
         * cannot point it at an internet host.
         */
        fun isLanHost(host: String): Boolean {
            if (host.isEmpty() || host.length > 253) return false
            if (host.endsWith(".local", ignoreCase = true) && host.all { it.isLetterOrDigit() || it == '.' || it == '-' }) {
                return true
            }
            val literal = host.trim('[', ']')
            val ipv4 = Regex("""\d{1,3}(\.\d{1,3}){3}""")
            if (ipv4.matches(literal)) {
                val parts = literal.split('.').map { it.toInt() }
                if (parts.any { it > 255 }) return false
                return parts[0] == 10 ||
                    (parts[0] == 172 && parts[1] in 16..31) ||
                    (parts[0] == 192 && parts[1] == 168) ||
                    (parts[0] == 169 && parts[1] == 254)
            }
            if (':' in literal && literal.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' }) {
                val address = runCatching { InetAddress.getByName(literal) }.getOrNull() ?: return false
                return address.isSiteLocalAddress || address.isLinkLocalAddress ||
                    (address.address.size == 16 && (address.address[0].toInt() and 0xfe) == 0xfc)
            }
            return false
        }
    }
}

object PairingProof {
    /** HMAC-SHA256(secret, TV key hash), lowercase hex. The secret itself is never sent. */
    fun compute(secretHex: String, keyHashHex: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hexBytes(secretHex), "HmacSHA256"))
        return mac.doFinal(hexBytes(keyHashHex)).joinToString("") { "%02x".format(it) }
    }

    private fun hexBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0)
        return ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }
}
