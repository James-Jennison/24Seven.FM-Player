package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class ProtectedSessionValidity {
    Unknown,
    Active,
    Expired,
    SignedOut,
}

/**
 * Owns the one in-memory cookie view for every protected station session. Remote data sources may share this
 * coordinator, but a cookie manager is never shared across station origins.
 */
internal class StationAuthSessionCoordinator(
    private val store: AuthSessionStore,
) {
    private data class Session(
        val origin: URI,
        val manager: CookieManager,
    )

    private val sessions = ConcurrentHashMap<StationId, Session>()
    private val validity = ConcurrentHashMap<StationId, MutableStateFlow<ProtectedSessionValidity>>()

    fun observeValidity(stationId: StationId): Flow<ProtectedSessionValidity> =
        validity(stationId.canonicalized()).asStateFlow()

    fun cookieManager(stationId: StationId, origin: String): CookieManager {
        val canonical = stationId.canonicalized()
        val expectedOrigin = trustedOrigin(origin)
        val session = sessions.compute(canonical) { _, existing ->
            if (existing != null) {
                if (!existing.origin.sameOrigin(expectedOrigin)) {
                    throw IOException("Station session origin changed")
                }
                existing
            } else {
                Session(
                    origin = expectedOrigin,
                    manager = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER).also { manager ->
                        val restoredCookies = store.load(canonical, expectedOrigin.host)
                        restoredCookies.forEach {
                            manager.cookieStore.add(expectedOrigin, it)
                        }
                        when {
                            restoredCookies.isNotEmpty() && store.loadDisplayName(canonical) != null ->
                                validity(canonical).value = ProtectedSessionValidity.Active
                            restoredCookies.isEmpty() && store.loadDisplayName(canonical) != null -> {
                                store.clear(canonical)
                                validity(canonical).value = ProtectedSessionValidity.Expired
                            }
                            else -> Unit
                        }
                    },
                )
            }
        } ?: throw IOException("Station session is unavailable")
        return session.manager
    }

    fun captureResponse(
        stationId: StationId,
        origin: String,
        requestUri: URI,
        headers: Map<String?, List<String>>,
    ) {
        val canonical = stationId.canonicalized()
        val expectedOrigin = trustedOrigin(origin)
        if (!requestUri.sameOrigin(expectedOrigin)) throw IOException("Untrusted session response")
        val manager = cookieManager(canonical, origin)
        synchronized(manager) {
            val wasAuthenticated = store.loadDisplayName(canonical) != null
            manager.put(requestUri, headers)
            store.updateCookies(
                canonical,
                expectedOrigin.host,
                manager.cookieStore.cookies,
                responseCookies(headers, requestUri),
            )
            when {
                wasAuthenticated && manager.cookieStore.cookies.isEmpty() -> {
                    store.clear(canonical)
                    validity(canonical).value = ProtectedSessionValidity.Expired
                }
                store.loadDisplayName(canonical) != null ->
                    validity(canonical).value = ProtectedSessionValidity.Active
            }
        }
    }

    fun persistAuthenticated(stationId: StationId, origin: String, displayName: String) {
        val canonical = stationId.canonicalized()
        val expectedOrigin = trustedOrigin(origin)
        val manager = cookieManager(canonical, origin)
        synchronized(manager) {
            store.save(canonical, expectedOrigin.host, manager.cookieStore.cookies, displayName)
            validity(canonical).value = ProtectedSessionValidity.Active
        }
    }

    fun displayName(stationId: StationId): String? = store.loadDisplayName(stationId.canonicalized())

    fun clear(stationId: StationId) {
        val canonical = stationId.canonicalized()
        sessions.remove(canonical)
        store.clear(canonical)
        validity(canonical).value = ProtectedSessionValidity.SignedOut
    }

    fun expire(stationId: StationId) {
        val canonical = stationId.canonicalized()
        sessions.remove(canonical)
        store.clear(canonical)
        validity(canonical).value = ProtectedSessionValidity.Expired
    }

    private fun trustedOrigin(origin: String): URI {
        val uri = runCatching { URI(origin) }.getOrElse { throw IOException("Invalid station origin") }
        if (
            !uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() ||
            uri.userInfo != null || effectivePort(uri) != 443
        ) {
            throw IOException("Invalid station origin")
        }
        return uri
    }

    private fun responseCookies(headers: Map<String?, List<String>>, requestUri: URI): List<HttpCookie> =
        headers.entries
            .filter { (name, _) -> name.equals("Set-Cookie", true) || name.equals("Set-Cookie2", true) }
            .flatMap { (_, values) -> values }
            .flatMap { value -> runCatching { HttpCookie.parse(value) }.getOrDefault(emptyList()) }
            .onEach { cookie ->
                if (cookie.domain.isNullOrBlank()) cookie.domain = requestUri.host
                if (cookie.path.isNullOrBlank()) cookie.path = "/"
            }

    private fun URI.sameOrigin(other: URI): Boolean =
        scheme.equals("https", ignoreCase = true) &&
            other.scheme.equals("https", ignoreCase = true) &&
            host.equals(other.host, ignoreCase = true) &&
            effectivePort(this) == effectivePort(other) &&
            userInfo == null && other.userInfo == null

    private fun effectivePort(uri: URI): Int = if (uri.port >= 0) uri.port else 443

    private fun validity(stationId: StationId) = validity.getOrPut(stationId) {
        MutableStateFlow(ProtectedSessionValidity.Unknown)
    }
}
