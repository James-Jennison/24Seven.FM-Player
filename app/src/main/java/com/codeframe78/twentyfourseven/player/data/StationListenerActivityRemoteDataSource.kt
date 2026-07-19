package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.RequestHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

internal data class ListenerActivitySnapshot(
    val membershipTier: MembershipTier,
    val requestReadiness: RequestReadiness,
    val waitMinutes: Int?,
    val recentRequests: List<RequestHistoryEntry>,
)

internal interface ListenerActivityRemoteDataSource {
    suspend fun load(stationId: StationId): ListenerActivitySnapshot
}

internal class StationListenerActivityRemoteDataSource(
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val parser: ListenerActivityPageParser = ListenerActivityPageParser(),
    private val sessions: StationAuthSessionCoordinator = StationAuthSessionCoordinator(sessionStore),
    private val connectionFactory: (URI) -> HttpURLConnection = {
        it.toURL().openConnection() as HttpURLConnection
    },
) : ListenerActivityRemoteDataSource {
    override suspend fun load(stationId: StationId): ListenerActivitySnapshot = withContext(Dispatchers.IO) {
        try {
            val origin = origin(stationId)
            val displayName = sessions.displayName(stationId)
                ?: throw ListenerActivityAuthenticationRequiredException()
            val manager = authenticatedCookieManager(stationId, origin)
            val historyPage = request(stationId, URI(origin).resolve(REQUEST_HISTORY_PATH), origin, manager)
            val discovery = parser.parseDiscovery(historyPage, origin, displayName)
            val cooldown = discovery.requestTimerUrl
                ?.let { parser.parseCooldown(request(stationId, URI(it), origin, manager, TIMER_RESPONSE_LIMIT)) }
                ?: RequestCooldownEvidence(RequestReadiness.Unknown, null)
            val membership = discovery.memberProfileUrl
                ?.let { parser.parseMembership(request(stationId, URI(it), origin, manager), origin, displayName) }
                ?: MembershipTier.Unknown
            ListenerActivitySnapshot(
                membershipTier = membership,
                requestReadiness = cooldown.readiness,
                waitMinutes = cooldown.waitMinutes,
                recentRequests = discovery.recentRequests,
            )
        } catch (failure: ListenerActivityAuthenticationRequiredException) {
            sessions.expire(stationId)
            throw failure
        }
    }

    private fun authenticatedCookieManager(stationId: StationId, origin: String): CookieManager {
        return sessions.cookieManager(stationId, origin).also { manager ->
            if (manager.cookieStore.cookies.isEmpty()) throw ListenerActivityAuthenticationRequiredException()
        }
    }

    private fun request(
        stationId: StationId,
        initialUri: URI,
        origin: String,
        manager: CookieManager,
        responseLimit: Int = PAGE_RESPONSE_LIMIT,
    ): String {
        val expected = URI(origin)
        var uri = initialUri
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            requireSameOrigin(uri, expected)
            val connection = connectionFactory(uri)
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "text/html")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                manager.get(uri, emptyMap()).forEach { (name, values) ->
                    connection.setRequestProperty(name, values.joinToString("; "))
                }
                val status = connection.responseCode
                sessions.captureResponse(stationId, origin, uri, connection.headerFields)
                if (status in REDIRECT_STATUSES) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("Too many listener activity redirects")
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Listener activity redirect was invalid")
                    uri = uri.resolve(location)
                    return@repeat
                }
                if (status !in 200..299) throw IOException("Station returned HTTP $status")
                return connection.inputStream.bufferedReader(StandardCharsets.ISO_8859_1).use {
                    it.readBounded(responseLimit)
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Listener activity request did not complete")
    }

    private fun requireSameOrigin(uri: URI, origin: URI) {
        if (
            uri.scheme != "https" || uri.userInfo != null ||
            !uri.host.equals(origin.host, true) || uri.port != origin.port
        ) {
            throw IOException("Untrusted listener activity destination")
        }
    }

    private fun origin(stationId: StationId): String = VERIFIED_ORIGINS[stationId.canonicalized()]
        ?: throw IOException("Listener activity is not verified for this station")

    private companion object {
        const val REQUEST_HISTORY_PATH = "/modules.php?name=Your_Requests"
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val PAGE_RESPONSE_LIMIT = 1_000_000
        const val TIMER_RESPONSE_LIMIT = 64_000
        const val MAX_REDIRECTS = 5
        val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
        val VERIFIED_ORIGINS = mapOf(
            StationId("sst") to "https://streamingsoundtracks.com/",
        )
    }
}
