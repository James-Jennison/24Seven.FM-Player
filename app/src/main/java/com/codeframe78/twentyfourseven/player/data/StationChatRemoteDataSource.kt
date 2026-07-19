package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal interface ChatRemoteDataSource {
    suspend fun fetch(stationId: StationId): List<ChatMessage>
    suspend fun send(stationId: StationId, message: String): List<ChatMessage>
}

internal class StationChatRemoteDataSource(
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val responseParser: ChatResponseParser = ChatResponseParser(),
    private val postFormParser: ChatPostFormParser = ChatPostFormParser(),
    private val sessions: StationAuthSessionCoordinator = StationAuthSessionCoordinator(sessionStore),
) : ChatRemoteDataSource {
    override suspend fun fetch(stationId: StationId): List<ChatMessage> = withContext(Dispatchers.IO) {
        val origin = origin(stationId)
        val viewUri = URI(origin).resolve(VIEW_PATH)
        responseParser.parse(
            request(stationId, viewUri, cookieManager = null),
            viewUri.toString(),
        )
    }

    override suspend fun send(stationId: StationId, message: String): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            require(message.isNotBlank() && message.length <= MAX_MESSAGE_CHARACTERS)
            require(StandardCharsets.ISO_8859_1.newEncoder().canEncode(message))
            val origin = origin(stationId)
            val manager = authenticatedCookieManager(stationId, origin)
            val credentials = postFormParser.parse(
                request(stationId, URI(origin), manager),
                origin,
            )
            val query = listOf(
                "comment" to message,
                "submit" to "Say",
                "username" to credentials.username,
                "userpw" to credentials.token,
            ).joinToString("&") { (name, value) -> "${encode(name)}=${encode(value)}" }
            request(
                stationId,
                URI(origin).resolve("${ChatPostFormParser.POST_PATH}?$query"),
                manager,
            )
            val messages = responseParser.parse(
                request(stationId, URI(origin).resolve(VIEW_PATH), cookieManager = null),
                URI(origin).resolve(VIEW_PATH).toString(),
            )
            if (messages.none {
                    it.authorDisplayName.equals(credentials.username, ignoreCase = true) &&
                        it.messageText == message.toSubmittedChatVisibleText()
                }
            ) {
                throw IOException("Station did not confirm the message")
            }
            messages
        }

    private fun authenticatedCookieManager(stationId: StationId, origin: String): CookieManager {
        return sessions.cookieManager(stationId, origin).also { manager ->
            if (manager.cookieStore.cookies.isEmpty()) throw IOException("Station sign-in is required")
        }
    }

    private fun request(stationId: StationId, uri: URI, cookieManager: CookieManager?): String {
        requireSameOrigin(stationId, uri)
        val connection = uri.toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = REQUEST_TIMEOUT_MILLIS
            connection.readTimeout = REQUEST_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "text/html")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            cookieManager?.get(uri, emptyMap())?.forEach { (name, values) ->
                connection.setRequestProperty(name, values.joinToString("; "))
            }
            val status = connection.responseCode
            if (cookieManager != null) {
                sessions.captureResponse(stationId, origin(stationId), uri, connection.headerFields)
            }
            if (status in 300..399) throw IOException("Unexpected station redirect")
            if (status !in 200..299) throw IOException("Station returned HTTP $status")
            connection.inputStream.bufferedReader(StandardCharsets.ISO_8859_1).use { reader ->
                reader.readBounded(MAX_RESPONSE_CHARACTERS)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun requireSameOrigin(stationId: StationId, uri: URI) {
        val expected = URI(origin(stationId))
        if (
            uri.scheme != "https" || uri.userInfo != null ||
            !uri.host.equals(expected.host, true) || uri.port != expected.port
        ) {
            throw IOException("Untrusted chat destination")
        }
    }

    private fun origin(stationId: StationId): String = ORIGINS[stationId.canonicalized()]
        ?: throw IOException("Unsupported station")

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.ISO_8859_1.name())

    private companion object {
        const val VIEW_PATH = "/modules/ClearChat/block-files/view.php?username=&sort=desc"
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val REQUEST_TIMEOUT_MILLIS = 10_000
        const val MAX_RESPONSE_CHARACTERS = 128_000
        const val MAX_MESSAGE_CHARACTERS = 255
        val ORIGINS = mapOf(
            StationId("sst") to "https://streamingsoundtracks.com/",
            StationId("1980s") to "https://1980s.fm/",
            StationId("afm") to "https://adagio.fm/",
            StationId("dfm") to "https://death.fm/",
            StationId("efm") to "https://entranced.fm/",
        )
    }
}
