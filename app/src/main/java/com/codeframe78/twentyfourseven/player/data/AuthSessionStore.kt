package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
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
    fun loadDisplayName(stationId: StationId): String?
    fun save(stationId: StationId, expectedDomain: String, cookies: List<HttpCookie>, displayName: String)
    fun updateCookies(
        stationId: StationId,
        expectedDomain: String,
        cookies: List<HttpCookie>,
        refreshedCookies: List<HttpCookie>,
    )
    fun clear(stationId: StationId)
}

private data class StoredAuthCookie(
    val cookie: HttpCookie,
    val expiresAtEpochMillis: Long?,
)

internal class AndroidKeystoreAuthSessionStore(
    context: Context,
    private val epochMillis: () -> Long = System::currentTimeMillis,
) : AuthSessionStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyStationKeys()
    }

    override fun load(stationId: StationId, expectedDomain: String): List<HttpCookie> {
        val canonical = stationId.canonicalized()
        return readStoredCookies(canonical, expectedDomain)?.map { copyCookie(it.cookie) }.orEmpty()
    }

    override fun loadDisplayName(stationId: StationId): String? = preferences
        .getString(identityKey(stationId.canonicalized()), null)
        ?.let { encrypted -> runCatching { decrypt(encrypted) }.getOrNull() }
        ?.takeIf { it.isNotBlank() }

    override fun save(
        stationId: StationId,
        expectedDomain: String,
        cookies: List<HttpCookie>,
        displayName: String,
    ) {
        val now = epochMillis()
        val stored = cookies
            .filter { it.matchesDomain(expectedDomain) && !it.hasExpired() }
            .map { StoredAuthCookie(copyCookie(it), it.expiryFrom(now)) }
            .filterNot { it.isExpiredAt(now) }
        persist(stationId.canonicalized(), expectedDomain, stored, displayName)
    }

    override fun updateCookies(
        stationId: StationId,
        expectedDomain: String,
        cookies: List<HttpCookie>,
        refreshedCookies: List<HttpCookie>,
    ) {
        val canonical = stationId.canonicalized()
        val displayName = loadDisplayName(canonical) ?: return
        val now = epochMillis()
        val previous = readStoredCookies(canonical, expectedDomain).orEmpty()
        val merged = mergeStoredCookies(cookies, refreshedCookies, previous, expectedDomain, now)
        persist(canonical, expectedDomain, merged, displayName)
    }

    override fun clear(stationId: StationId) {
        val canonical = stationId.canonicalized()
        preferences.edit().remove(key(canonical)).remove(identityKey(canonical)).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return listOf(cipher.iv, encrypted).joinToString(".") { Base64.encodeToString(it, Base64.NO_WRAP) }
    }

    private fun decrypt(value: String): String {
        val parts = value.split('.', limit = 2)
        require(parts.size == 2)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
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

    private fun encodeCookies(cookies: List<StoredAuthCookie>, expectedDomain: String): String = JSONArray().apply {
        cookies.forEach { stored ->
            val cookie = stored.cookie
            put(JSONObject().apply {
                put("name", cookie.name)
                put("value", cookie.value)
                put("domain", cookie.domain?.trimStart('.')?.takeIf(String::isNotBlank) ?: expectedDomain)
                put("path", cookie.path ?: "/")
                put("secure", true)
                put("httpOnly", cookie.isHttpOnly)
                put("maxAge", cookie.maxAge)
                stored.expiresAtEpochMillis?.let { put("expiresAtEpochMillis", it) }
            })
        }
    }.toString()

    private fun decodeCookies(json: String, expectedDomain: String, now: Long): List<StoredAuthCookie> {
        val array = JSONArray(json)
        return buildList {
            repeat(array.length()) { index ->
                val value = array.getJSONObject(index)
                val storedMaxAge = value.optLong("maxAge", -1L)
                val expiresAt = if (value.has("expiresAtEpochMillis")) {
                    value.optLong("expiresAtEpochMillis")
                } else {
                    maxAgeExpiry(now, storedMaxAge)
                }
                if (expiresAt != null && expiresAt <= now) return@repeat
                val cookie = HttpCookie(value.getString("name"), value.getString("value")).apply {
                    domain = value.getString("domain")
                    path = value.getString("path")
                    secure = value.getBoolean("secure")
                    isHttpOnly = value.getBoolean("httpOnly")
                    maxAge = expiresAt?.let { remainingSeconds(it, now) } ?: -1L
                }
                if (cookie.matchesDomain(expectedDomain) && !cookie.hasExpired()) {
                    add(StoredAuthCookie(cookie, expiresAt))
                }
            }
        }
    }

    private fun readStoredCookies(stationId: StationId, expectedDomain: String): List<StoredAuthCookie>? {
        val encrypted = preferences.getString(key(stationId), null) ?: return null
        return runCatching {
            decodeCookies(decrypt(encrypted), expectedDomain, epochMillis())
        }.getOrElse {
            clear(stationId)
            null
        }
    }

    private fun persist(
        stationId: StationId,
        expectedDomain: String,
        cookies: List<StoredAuthCookie>,
        displayName: String,
    ) {
        if (cookies.isEmpty()) {
            clear(stationId)
            return
        }
        preferences.edit()
            .putString(key(stationId), encrypt(encodeCookies(cookies, expectedDomain)))
            .putString(identityKey(stationId), encrypt(displayName))
            .apply()
    }

    private fun migrateLegacyStationKeys() {
        val editor = preferences.edit()
        LEGACY_STATION_IDS.forEach { (legacy, canonical) ->
            val legacyStation = StationId(legacy)
            val canonicalStation = StationId(canonical)
            if (!preferences.contains(key(canonicalStation))) {
                preferences.getString(key(legacyStation), null)?.let { editor.putString(key(canonicalStation), it) }
            }
            if (!preferences.contains(identityKey(canonicalStation))) {
                preferences.getString(identityKey(legacyStation), null)
                    ?.let { editor.putString(identityKey(canonicalStation), it) }
            }
            editor.remove(key(legacyStation)).remove(identityKey(legacyStation))
        }
        editor.apply()
    }

    private fun HttpCookie.matchesDomain(expectedDomain: String): Boolean {
        val normalized = domain?.trimStart('.')?.takeIf(String::isNotBlank) ?: expectedDomain
        return normalized.equals(expectedDomain, ignoreCase = true)
    }

    private fun key(stationId: StationId) = "station_${stationId.value}"
    private fun identityKey(stationId: StationId) = "identity_${stationId.value}"

    private companion object {
        const val PREFERENCES_NAME = "protected_auth_sessions"
        const val KEY_ALIAS = "twentyfourseven_auth_sessions"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        val LEGACY_STATION_IDS = mapOf("adagio" to "afm", "death" to "dfm", "entranced" to "efm")
    }
}

internal class InMemoryAuthSessionStore(
    private val epochMillis: () -> Long = System::currentTimeMillis,
) : AuthSessionStore {
    private val sessions = mutableMapOf<StationId, List<StoredAuthCookie>>()
    private val identities = mutableMapOf<StationId, String>()
    override fun load(stationId: StationId, expectedDomain: String): List<HttpCookie> {
        val now = epochMillis()
        val canonical = stationId.canonicalized()
        val retained = sessions[canonical].orEmpty().filterNot { it.isExpiredAt(now) }
        if (retained.isEmpty()) {
            clear(canonical)
            return emptyList()
        }
        sessions[canonical] = retained
        return retained.filter { it.cookie.matchesDomain(expectedDomain) }.map { stored ->
            copyCookie(stored.cookie).also { cookie ->
                cookie.maxAge = stored.expiresAtEpochMillis?.let { remainingSeconds(it, now) } ?: -1L
            }
        }
    }

    override fun loadDisplayName(stationId: StationId): String? = identities[stationId.canonicalized()]

    override fun save(
        stationId: StationId,
        expectedDomain: String,
        cookies: List<HttpCookie>,
        displayName: String,
    ) {
        val canonical = stationId.canonicalized()
        val now = epochMillis()
        val stored = cookies
            .filter { it.matchesDomain(expectedDomain) && !it.hasExpired() }
            .map { StoredAuthCookie(copyCookie(it), it.expiryFrom(now)) }
            .filterNot { it.isExpiredAt(now) }
        if (stored.isEmpty()) {
            clear(canonical)
            return
        }
        sessions[canonical] = stored
        identities[canonical] = displayName
    }

    override fun updateCookies(
        stationId: StationId,
        expectedDomain: String,
        cookies: List<HttpCookie>,
        refreshedCookies: List<HttpCookie>,
    ) {
        val canonical = stationId.canonicalized()
        if (identities[canonical] == null) return
        val merged = mergeStoredCookies(
            cookies,
            refreshedCookies,
            sessions[canonical].orEmpty(),
            expectedDomain,
            epochMillis(),
        )
        if (merged.isEmpty()) clear(canonical) else sessions[canonical] = merged
    }

    override fun clear(stationId: StationId) {
        val canonical = stationId.canonicalized()
        sessions.remove(canonical)
        identities.remove(canonical)
    }
}

private fun mergeStoredCookies(
    cookies: List<HttpCookie>,
    refreshedCookies: List<HttpCookie>,
    previous: List<StoredAuthCookie>,
    expectedDomain: String,
    now: Long,
): List<StoredAuthCookie> = cookies
    .filter { it.matchesDomain(expectedDomain) && !it.hasExpired() }
    .map { cookie ->
        val refreshed = refreshedCookies.lastOrNull { it.sameIdentity(cookie, expectedDomain) }
        val existing = previous.firstOrNull {
            it.cookie.sameIdentity(cookie, expectedDomain) && it.cookie.value == cookie.value
        }
        StoredAuthCookie(
            cookie = copyCookie(cookie),
            expiresAtEpochMillis = when {
                refreshed != null -> refreshed.expiryFrom(now)
                existing != null -> existing.expiresAtEpochMillis
                else -> cookie.expiryFrom(now)
            },
        )
    }
    .filterNot { it.isExpiredAt(now) }

private fun StoredAuthCookie.isExpiredAt(now: Long) = expiresAtEpochMillis?.let { it <= now } == true

private fun HttpCookie.expiryFrom(now: Long): Long? = maxAgeExpiry(now, maxAge)

private fun maxAgeExpiry(now: Long, maxAgeSeconds: Long): Long? = when {
    maxAgeSeconds < 0L -> null
    maxAgeSeconds == 0L -> now
    maxAgeSeconds > (Long.MAX_VALUE - now) / 1_000L -> Long.MAX_VALUE
    else -> now + maxAgeSeconds * 1_000L
}

private fun remainingSeconds(expiresAt: Long, now: Long): Long =
    ((expiresAt - now).coerceAtLeast(1L) + 999L) / 1_000L

private fun HttpCookie.sameIdentity(other: HttpCookie, expectedDomain: String): Boolean =
    name.equals(other.name, ignoreCase = true) &&
        normalizedDomain(expectedDomain).equals(other.normalizedDomain(expectedDomain), ignoreCase = true) &&
        normalizedPath() == other.normalizedPath()

private fun HttpCookie.normalizedDomain(expectedDomain: String) =
    domain?.trimStart('.')?.takeIf(String::isNotBlank) ?: expectedDomain

private fun HttpCookie.normalizedPath() = path?.takeIf(String::isNotBlank) ?: "/"

private fun HttpCookie.matchesDomain(expectedDomain: String): Boolean =
    normalizedDomain(expectedDomain).equals(expectedDomain, ignoreCase = true)

private fun copyCookie(cookie: HttpCookie) = HttpCookie(cookie.name, cookie.value).also {
    it.domain = cookie.domain
    it.path = cookie.path
    it.secure = cookie.secure
    it.isHttpOnly = cookie.isHttpOnly
    it.maxAge = cookie.maxAge
    it.version = cookie.version
    it.discard = cookie.discard
}
