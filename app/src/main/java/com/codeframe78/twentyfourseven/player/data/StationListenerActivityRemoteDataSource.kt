package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.MemberProfile
import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.RequestHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class ListenerActivitySnapshot(
    val membershipTier: MembershipTier,
    val requestReadiness: RequestReadiness,
    val waitMinutes: Int?,
    val recentRequests: List<RequestHistoryEntry>,
    val rankTitle: String? = null,
    val queuedRequestWaitSeconds: Int? = null,
)

internal interface ListenerActivityRemoteDataSource {
    suspend fun load(stationId: StationId): ListenerActivitySnapshot
}

internal class StationListenerActivityRemoteDataSource(
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val parser: ListenerActivityPageParser = ListenerActivityPageParser(),
    private val profileParser: StationExtrasParser = StationExtrasParser(),
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
            val (cooldown, membership) = coroutineScope {
                val cooldown = async {
                    // The stations now print the request clock on the page; older pages framed a separate timer.
                    discovery.cooldown
                        ?: discovery.requestTimerUrl
                            ?.let { parser.parseCooldown(request(stationId, URI(it), origin, manager, TIMER_RESPONSE_LIMIT)) }
                        ?: RequestCooldownEvidence(RequestReadiness.Unknown, null)
                }
                val membership = async {
                    ownProfileCard(stationId, origin, manager, displayName)
                        ?.let { card -> parser.membershipTier(card.membership) to card.rankTitle }
                        ?: discovery.memberProfileUrl
                            ?.let { parser.parseMembership(request(stationId, URI(it), origin, manager), origin, displayName) to null }
                        ?: (MembershipTier.Unknown to null)
                }
                cooldown.await() to membership.await()
            }
            ListenerActivitySnapshot(
                membershipTier = membership.first,
                requestReadiness = cooldown.readiness,
                waitMinutes = cooldown.waitMinutes,
                recentRequests = discovery.recentRequests,
                rankTitle = membership.second,
                queuedRequestWaitSeconds = cooldown.queuedRequestWaitSeconds,
            )
        } catch (failure: ListenerActivityAuthenticationRequiredException) {
            sessions.expire(stationId)
            throw failure
        }
    }

    /**
     * The signed-in member's own public profile card, which names their membership and rank. Null when the station
     * has no card under that name or the card could not be read, so membership falls back to the older profile page.
     */
    private fun ownProfileCard(
        stationId: StationId,
        origin: String,
        manager: CookieManager,
        displayName: String,
    ): MemberProfile? = runCatching {
        val path = PROFILE_CARD_PATH + URLEncoder.encode(displayName, StandardCharsets.UTF_8.name())
        profileParser.parseProfile(request(stationId, URI(origin).resolve(path), origin, manager, PROFILE_CARD_LIMIT), origin)
            ?.takeIf { it.username.equals(displayName, ignoreCase = true) }
    }.getOrNull()

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
                val requestHeaders = synchronized(manager) { manager.get(uri, emptyMap()) }
                requestHeaders.forEach { (name, values) ->
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
                val charset = declaredCharset(connection.contentType, StandardCharsets.ISO_8859_1)
                return connection.inputStream.bufferedReader(charset).use {
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
        const val PROFILE_CARD_PATH = "/modules/Your_Profile/hoverCardAJAX.php?username="
        const val PROFILE_CARD_LIMIT = 50_000
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
