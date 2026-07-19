package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class AuthenticatedPage(val html: String, val finalUrl: String)

internal sealed interface RestoredAuthSession {
    data object None : RestoredAuthSession
    data object Expired : RestoredAuthSession
    data class SignedIn(val displayName: String) : RestoredAuthSession
}

internal interface AuthRemoteDataSource {
    fun observeSessionValidity(stationId: StationId): Flow<ProtectedSessionValidity> =
        flowOf(ProtectedSessionValidity.Unknown)
    suspend fun fetchChallenge(stationId: StationId): LoginChallenge
    suspend fun signIn(
        stationId: StationId,
        challenge: LoginChallenge,
        username: String,
        password: String,
        securityCode: String,
    ): AuthenticatedPage
    suspend fun restoredSession(stationId: StationId): RestoredAuthSession
    fun persistSession(stationId: StationId, displayName: String)
    suspend fun signOut(stationId: StationId)
}

internal class StationAuthRemoteDataSource(
    private val parser: AuthLoginPageParser = AuthLoginPageParser(),
    private val resultParser: AuthLoginResultParser = AuthLoginResultParser(),
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val sessions: StationAuthSessionCoordinator = StationAuthSessionCoordinator(sessionStore),
) : AuthRemoteDataSource {
    override fun observeSessionValidity(stationId: StationId): Flow<ProtectedSessionValidity> =
        sessions.observeValidity(stationId)

    override suspend fun fetchChallenge(stationId: StationId): LoginChallenge = withContext(Dispatchers.IO) {
        val origin = origin(stationId)
        parser.parse(request(stationId, URI(origin), method = "GET").html, origin)
    }

    override suspend fun signIn(
        stationId: StationId,
        challenge: LoginChallenge,
        username: String,
        password: String,
        securityCode: String,
    ): AuthenticatedPage = withContext(Dispatchers.IO) {
        val origin = origin(stationId)
        requireSameOrigin(challenge.actionUrl, origin)
        val body = listOf(
            "username" to username,
            "user_password" to password,
            "gfx_check" to securityCode,
            "random_num" to challenge.challengeToken,
            "op" to "login",
        ).joinToString("&") { (name, value) -> "${encode(name)}=${encode(value)}" }
        request(stationId, URI(challenge.actionUrl), method = "POST", body = body)
    }

    override suspend fun signOut(stationId: StationId) = withContext(Dispatchers.IO) {
        try {
            request(
                stationId,
                URI(origin(stationId)).resolve("/modules.php?name=Your_Account&op=logout"),
                method = "GET",
            )
            Unit
        } finally {
            sessions.clear(stationId)
        }
    }

    override suspend fun restoredSession(stationId: StationId): RestoredAuthSession = withContext(Dispatchers.IO) {
        val displayName = sessions.displayName(stationId)
        if (cookieManager(stationId).cookieStore.cookies.isEmpty()) {
            if (displayName != null) {
                sessions.expire(stationId)
                return@withContext RestoredAuthSession.Expired
            }
            return@withContext RestoredAuthSession.None
        }
        if (displayName == null) return@withContext RestoredAuthSession.None
        val origin = origin(stationId)
        val page = runCatching { request(stationId, URI(origin), method = "GET") }
            .getOrNull() ?: return@withContext RestoredAuthSession.SignedIn(displayName)
        runCatching { resultParser.parseSignedInDisplayName(page.html, origin, displayName) }
            .fold(
                onSuccess = RestoredAuthSession::SignedIn,
                onFailure = {
                    sessions.expire(stationId)
                    RestoredAuthSession.Expired
                },
            )
    }

    override fun persistSession(stationId: StationId, displayName: String) {
        sessions.persistAuthenticated(stationId, origin(stationId), displayName)
    }

    private fun request(stationId: StationId, initialUri: URI, method: String, body: String? = null): AuthenticatedPage {
        val cookies = cookieManager(stationId)
        var uri = initialUri
        var requestMethod = method
        var requestBody = body
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            requireSameOrigin(uri.toASCIIString(), origin(stationId))
            val connection = uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.requestMethod = requestMethod
                connection.setRequestProperty("Accept", "text/html")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                cookies.get(uri, emptyMap()).forEach { (name, values) ->
                    connection.setRequestProperty(name, values.joinToString("; "))
                }
                requestBody?.let { encoded ->
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    connection.outputStream.use { it.write(encoded.toByteArray(StandardCharsets.UTF_8)) }
                }
                val status = connection.responseCode
                sessions.captureResponse(stationId, origin(stationId), uri, connection.headerFields)
                if (status in REDIRECT_STATUSES) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("Too many station redirects")
                    val location = connection.getHeaderField("Location") ?: throw IOException("Station redirect was invalid")
                    uri = uri.resolve(location)
                    if (status != 307 && status != 308) {
                        requestMethod = "GET"
                        requestBody = null
                    }
                    return@repeat
                }
                if (status !in 200..299) throw IOException("Station returned HTTP $status")
                val html = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use {
                    it.readBounded(MAX_RESPONSE_CHARACTERS)
                }
                return AuthenticatedPage(html, uri.toASCIIString())
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Station request did not complete")
    }

    private fun requireSameOrigin(url: String, origin: String) {
        val uri = URI(url)
        val expected = URI(origin)
        if (
            uri.scheme != "https" || uri.userInfo != null ||
            !uri.host.equals(expected.host, true) || uri.port != expected.port
        ) {
            throw IOException("Untrusted authentication destination")
        }
    }

    private fun origin(stationId: StationId): String = ORIGINS[stationId.canonicalized()]
        ?: throw IOException("Unsupported station")

    private fun cookieManager(stationId: StationId): CookieManager =
        sessions.cookieManager(stationId, origin(stationId))

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val MAX_RESPONSE_CHARACTERS = 512_000
        const val MAX_REDIRECTS = 5
        val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
        val ORIGINS = mapOf(
            StationId("sst") to "https://streamingsoundtracks.com/",
            StationId("1980s") to "https://1980s.fm/",
            StationId("afm") to "https://adagio.fm/",
            StationId("dfm") to "https://death.fm/",
            StationId("efm") to "https://entranced.fm/",
        )
    }
}
