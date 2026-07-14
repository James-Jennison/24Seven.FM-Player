package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.RequestSearchResult
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.MAX_REQUEST_MESSAGE_CHARACTERS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal sealed interface RequestSubmissionResult {
    data class Submitted(val message: String) : RequestSubmissionResult
    data class Rejected(val message: String) : RequestSubmissionResult
    data object AuthenticationRequired : RequestSubmissionResult
}

internal interface SongRequestRemoteDataSource {
    suspend fun search(stationId: StationId, query: String, field: RequestSearchField): List<RequestSearchResult>
    suspend fun loadAlbum(stationId: StationId, albumId: String): RequestAlbum
    suspend fun submit(stationId: StationId, track: RequestableTrack, message: String): RequestSubmissionResult
}

internal class StationSongRequestRemoteDataSource(
    private val parser: SongRequestPageParser = SongRequestPageParser(),
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val connectionFactory: (URI) -> HttpURLConnection = {
        it.toURL().openConnection() as HttpURLConnection
    },
) : SongRequestRemoteDataSource {
    override suspend fun search(
        stationId: StationId,
        query: String,
        field: RequestSearchField,
    ): List<RequestSearchResult> = withContext(Dispatchers.IO) {
        val origin = origin(stationId)
        val path = "/modules.php?name=Requests&searchfor=1&searchpgstart=1" +
            "&searchby=${encode(field.wireValue)}&searchtext=${encode(query)}&search=Search"
        parser.parseSearch(request(stationId, URI(origin).resolve(path), authenticated = false).html, origin)
    }

    override suspend fun loadAlbum(stationId: StationId, albumId: String): RequestAlbum =
        withContext(Dispatchers.IO) {
            require(albumId.matches(SAFE_ALBUM_ID)) { "Invalid album identifier" }
            val origin = origin(stationId)
            val page = request(
                stationId,
                URI(origin).resolve("/modules.php?name=Album&asin=${encode(albumId)}"),
                authenticated = false,
            )
            parser.parseAlbum(page.html, origin, albumId)
        }

    override suspend fun submit(
        stationId: StationId,
        track: RequestableTrack,
        message: String,
    ): RequestSubmissionResult = withContext(Dispatchers.IO) {
        require(track.eligible && track.songId.matches(NUMERIC_ID) && track.albumId.matches(SAFE_ALBUM_ID)) {
            "Track is not eligible for requests"
        }
        require(message.length <= MAX_REQUEST_MESSAGE_CHARACTERS) { "Request message is too long" }
        require(message.isBlank() || stationId == StationId("sst")) {
            "Request messages have not been verified for this station"
        }
        val origin = origin(stationId)
        val manager = authenticatedCookieManager(stationId, origin)
        if (manager.cookieStore.cookies.isEmpty()) return@withContext RequestSubmissionResult.AuthenticationRequired
        val page = try {
            request(
                stationId,
                URI(origin).resolve(
                    "/modules.php?name=Req&asin=${encode(track.albumId)}&songID=${encode(track.songId)}",
                ),
                authenticated = true,
                cookieManager = manager,
            )
        } catch (failure: IOException) {
            if (message.isBlank()) throw failure
            return@withContext postOptionalMessage(
                stationId,
                track,
                message,
                manager,
                "The request response could not be read, but the optional message form was sent once. " +
                    "Check Queue for both. The song request was not retried.",
            ) ?: throw failure
        }
        val submission = classifySubmission(page.html)
        if (submission !is RequestSubmissionResult.Submitted || message.isBlank()) {
            return@withContext submission
        }

        postOptionalMessage(
            stationId,
            track,
            message,
            manager,
            "Request and optional message sent.",
        ) ?: RequestSubmissionResult.Submitted(
            "Request accepted, but the optional message could not be confirmed. The request was not retried.",
        )
    }

    private fun postOptionalMessage(
        stationId: StationId,
        track: RequestableTrack,
        message: String,
        manager: CookieManager,
        successNotice: String,
    ): RequestSubmissionResult? {
        val origin = origin(stationId)
        val messageFormUri = URI(origin).resolve(
            "/modules.php?name=Album&action=writemessage" +
                "&asin=${encode(track.albumId)}&id=${encode(track.songId)}",
        )
        val messageResult = runCatching {
            request(
                stationId,
                URI(origin).resolve(
                    "/modules.php?name=Album&action=submitmessage" +
                        "&asin=${encode(track.albumId)}&id=${encode(track.songId)}",
                ),
                authenticated = true,
                cookieManager = manager,
                method = "POST",
                formFields = linkedMapOf(
                    "msg" to message,
                    "send" to "Send",
                    "remLen" to (MAX_REQUEST_MESSAGE_CHARACTERS - message.length).toString(),
                ),
                referer = messageFormUri,
            )
        }.getOrNull()
        if (messageResult == null) return null
        val messageText = Jsoup.parse(messageResult.html).text().replace(Regex("\\s+"), " ").trim()
        return if (messageText.contains("log in", true) || messageText.contains("login", true)) {
            RequestSubmissionResult.Submitted(
                "Request accepted, but the optional message was not added because the station sign-in expired.",
            )
        } else {
            RequestSubmissionResult.Submitted(successNotice)
        }
    }

    private fun classifySubmission(html: String): RequestSubmissionResult {
        val text = Jsoup.parse(html).text().replace(Regex("\\s+"), " ").trim()
        if (text.contains("log in", true) || text.contains("login", true) && text.contains("request", true)) {
            return RequestSubmissionResult.AuthenticationRequired
        }
        val rejection = REJECTION_PATTERNS.firstOrNull { it.containsMatchIn(text) }
        if (rejection != null) {
            return RequestSubmissionResult.Rejected(
                text.take(MAX_NOTICE_CHARACTERS).ifBlank { "The station rejected this request." },
            )
        }
        return RequestSubmissionResult.Submitted(
            text.take(MAX_NOTICE_CHARACTERS).ifBlank {
                "The station received the request. Check the queue for confirmation."
            },
        )
    }

    private fun request(
        stationId: StationId,
        initialUri: URI,
        authenticated: Boolean,
        cookieManager: CookieManager? = null,
        method: String = "GET",
        formFields: Map<String, String> = emptyMap(),
        referer: URI? = null,
    ): AuthenticatedPage {
        var uri = initialUri
        var requestMethod = method
        var requestBody = formFields.takeIf { it.isNotEmpty() }
            ?.entries
            ?.joinToString("&") { (name, value) -> "${encode(name)}=${encode(value)}" }
            ?.toByteArray(StandardCharsets.UTF_8)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            requireSameOrigin(stationId, uri)
            val connection = connectionFactory(uri)
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.requestMethod = requestMethod
                connection.setRequestProperty("Accept", "text/html")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                referer?.let { connection.setRequestProperty("Referer", it.toASCIIString()) }
                if (authenticated) {
                    cookieManager?.get(uri, emptyMap())?.forEach { (name, values) ->
                        connection.setRequestProperty(name, values.joinToString("; "))
                    }
                }
                requestBody?.let { body ->
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    connection.setFixedLengthStreamingMode(body.size)
                    connection.outputStream.use { it.write(body) }
                }
                val status = connection.responseCode
                if (authenticated) cookieManager?.put(uri, connection.headerFields.filterKeys { it != null })
                if (status in REDIRECT_STATUSES) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("Too many station redirects")
                    uri = trustedRedirect(
                        stationId,
                        uri.resolve(connection.getHeaderField("Location") ?: throw IOException("Invalid redirect")),
                    )
                    if (status == 303 || requestMethod == "POST" && status in setOf(301, 302)) {
                        requestMethod = "GET"
                        requestBody = null
                    }
                    return@repeat
                }
                if (status !in 200..299) throw IOException("Station returned HTTP $status")
                val charset = responseCharset(connection.contentType)
                val html = connection.inputStream.bufferedReader(charset).use {
                    it.readBounded(MAX_RESPONSE_CHARACTERS)
                }
                return AuthenticatedPage(html, uri.toASCIIString())
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Station request did not complete")
    }

    private fun authenticatedCookieManager(stationId: StationId, origin: String): CookieManager {
        val uri = URI(origin)
        return CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER).also { manager ->
            sessionStore.load(stationId, uri.host).forEach { manager.cookieStore.add(uri, it) }
        }
    }

    private fun requireSameOrigin(stationId: StationId, uri: URI) {
        val expected = URI(origin(stationId))
        if (uri.scheme != "https" || !uri.host.equals(expected.host, true) || uri.port != expected.port) {
            throw IOException("Untrusted request destination")
        }
    }

    private fun trustedRedirect(stationId: StationId, redirect: URI): URI {
        val expected = URI(origin(stationId))
        if (
            redirect.scheme == "http" && redirect.host.equals(expected.host, true) &&
            redirect.port in setOf(-1, 80)
        ) {
            return URI("https", null, redirect.host, -1, redirect.path, redirect.query, redirect.fragment)
        }
        return redirect
    }

    private fun responseCharset(contentType: String?): Charset {
        val declared = contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim()?.trim('"')
        return runCatching { Charset.forName(declared.orEmpty()) }.getOrDefault(StandardCharsets.ISO_8859_1)
    }

    private fun origin(stationId: StationId): String = ORIGINS[stationId] ?: throw IOException("Unsupported station")
    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val MAX_RESPONSE_CHARACTERS = 1_000_000
        const val MAX_REDIRECTS = 5
        const val MAX_NOTICE_CHARACTERS = 240
        val SAFE_ALBUM_ID = Regex("[A-Za-z0-9_-]{1,64}")
        val NUMERIC_ID = Regex("\\d{1,20}")
        val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
        val REJECTION_PATTERNS = listOf(
            Regex("cannot request", RegexOption.IGNORE_CASE),
            Regex("not available", RegexOption.IGNORE_CASE),
            Regex("already (?:in|on) the queue", RegexOption.IGNORE_CASE),
            Regex("(?:wait|cooldown|too soon|request limit)", RegexOption.IGNORE_CASE),
            Regex("not eligible", RegexOption.IGNORE_CASE),
        )
        val ORIGINS = mapOf(
            StationId("sst") to "https://streamingsoundtracks.com/",
            StationId("1980s") to "https://1980s.fm/",
            StationId("adagio") to "https://adagio.fm/",
            StationId("death") to "https://death.fm/",
            StationId("entranced") to "https://entranced.fm/",
        )
    }
}
