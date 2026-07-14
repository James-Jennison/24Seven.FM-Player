package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.codeframe78.twentyfourseven.player.domain.StationId
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpCookie
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface AuthSessionStore {
    fun load(stationId: StationId, expectedDomain: String): List<HttpCookie>
    fun save(stationId: StationId, expectedDomain: String, cookies: List<HttpCookie>)
    fun clear(stationId: StationId)
}

internal class AndroidKeystoreAuthSessionStore(context: Context) : AuthSessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(stationId: StationId, expectedDomain: String): List<HttpCookie> {
        val encrypted = preferences.getString(key(stationId), null) ?: return emptyList()
        return runCatching {
            val parts = encrypted.split('.', limit = 2)
            require(parts.size == 2)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, Base64.decode(parts[0], Base64.NO_WRAP)),
            )
            val json = String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
            decodeCookies(json, expectedDomain)
        }.getOrElse {
            clear(stationId)
            emptyList()
        }
    }

    override fun save(stationId: StationId, expectedDomain: String, cookies: List<HttpCookie>) {
        val json = encodeCookies(cookies.filter { it.matchesDomain(expectedDomain) })
        if (json == "[]") {
            clear(stationId)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        val value = listOf(cipher.iv, encrypted).joinToString(".") { Base64.encodeToString(it, Base64.NO_WRAP) }
        preferences.edit().putString(key(stationId), value).apply()
    }

    override fun clear(stationId: StationId) {
        preferences.edit().remove(key(stationId)).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private fun encodeCookies(cookies: List<HttpCookie>): String = JSONArray().apply {
        cookies.forEach { cookie ->
            put(JSONObject().apply {
                put("name", cookie.name)
                put("value", cookie.value)
                put("domain", cookie.domain)
                put("path", cookie.path ?: "/")
                put("secure", cookie.secure)
                put("httpOnly", cookie.isHttpOnly)
                put("maxAge", cookie.maxAge)
            })
        }
    }.toString()

    private fun decodeCookies(json: String, expectedDomain: String): List<HttpCookie> {
        val array = JSONArray(json)
        return buildList {
            repeat(array.length()) { index ->
                val value = array.getJSONObject(index)
                val cookie = HttpCookie(value.getString("name"), value.getString("value")).apply {
                    domain = value.getString("domain")
                    path = value.getString("path")
                    secure = value.getBoolean("secure")
                    isHttpOnly = value.getBoolean("httpOnly")
                    maxAge = value.getLong("maxAge")
                }
                if (cookie.matchesDomain(expectedDomain) && !cookie.hasExpired()) add(cookie)
            }
        }
    }

    private fun HttpCookie.matchesDomain(expectedDomain: String): Boolean {
        val normalized = domain?.trimStart('.') ?: return false
        return normalized.equals(expectedDomain, ignoreCase = true) && secure
    }

    private fun key(stationId: StationId) = "station_${stationId.value}"

    private companion object {
        const val PREFERENCES_NAME = "protected_auth_sessions"
        const val KEY_ALIAS = "twentyfourseven_auth_sessions"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
    }
}

internal class InMemoryAuthSessionStore : AuthSessionStore {
    private val sessions = mutableMapOf<StationId, List<HttpCookie>>()
    override fun load(stationId: StationId, expectedDomain: String): List<HttpCookie> =
        sessions[stationId].orEmpty().map(::copyCookie)

    override fun save(stationId: StationId, expectedDomain: String, cookies: List<HttpCookie>) {
        sessions[stationId] = cookies.map(::copyCookie)
    }

    override fun clear(stationId: StationId) {
        sessions.remove(stationId)
    }

    private fun copyCookie(cookie: HttpCookie) = HttpCookie(cookie.name, cookie.value).also {
        it.domain = cookie.domain
        it.path = cookie.path
        it.secure = cookie.secure
        it.isHttpOnly = cookie.isHttpOnly
        it.maxAge = cookie.maxAge
    }
}
