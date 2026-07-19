package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.RequestSearchResult
import com.codeframe78.twentyfourseven.player.domain.RequestSuggestionMode
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import com.codeframe78.twentyfourseven.player.domain.MAX_REQUEST_MESSAGE_CHARACTERS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
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
    suspend fun loadArtistAlbums(stationId: StationId, artistName: String): List<RequestSearchResult>
    suspend fun suggest(stationId: StationId, mode: RequestSuggestionMode): RequestAlbum
    suspend fun loadAlbum(stationId: StationId, albumId: String): RequestAlbum
    suspend fun submit(stationId: StationId, track: RequestableTrack, message: String): RequestSubmissionResult
}

internal class StationSongRequestRemoteDataSource(
    private val parser: SongRequestPageParser = SongRequestPageParser(),
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val sessions: StationAuthSessionCoordinator = StationAuthSessionCoordinator(sessionStore),
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

    override suspend fun loadArtistAlbums(
        stationId: StationId,
        artistName: String,
    ): List<RequestSearchResult> = withContext(Dispatchers.IO) {
        require(artistName.isNotBlank() && artistName.length <= MAX_ARTIST_NAME_LENGTH) {
            "Invalid artist name"
        }
        val origin = origin(stationId)
        val path = "/modules.php?name=Requests&postartistsearch=true&artist=${encode(artistName)}"
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

    override suspend fun suggest(
        stationId: StationId,
        mode: RequestSuggestionMode,
    ): RequestAlbum = withContext(Dispatchers.IO) {
        val origin = origin(stationId)
        val path = "/modules.php?name=Requests&${mode.wireValue}=1&searchpgstart=1"
        parser.parseSuggestion(
            request(stationId, URI(origin).resolve(path), authenticated = false).html,
            origin,
        )
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
        val page = request(
            stationId,
            URI(origin).resolve(
                "/modules.php?name=Req&asin=${encode(track.albumId)}&songID=${encode(track.songId)}",
            ),
            authenticated = true,
            cookieManager = manager,
            stopReadingWhen = ::containsTerminalRequestResponse,
        )
        val submission = classifySubmission(page.html)
        if (submission == RequestSubmissionResult.AuthenticationRequired) {
            sessions.expire(stationId)
        }
        if (submission !is RequestSubmissionResult.Submitted || message.isBlank()) {
            return@withContext submission
        }

        val messageForm = requestMessageForm(stationId, track.albumId, page)
            ?: return@withContext RequestSubmissionResult.Submitted(
                "The station reported the request accepted, but did not provide a valid optional-message form. " +
                    "The request was not retried.",
            )
        postOptionalMessage(
            stationId,
            message,
            manager,
            messageForm,
        ) ?: RequestSubmissionResult.Submitted(
            "The station reported the request accepted, but the optional message could not be confirmed. " +
                "The request was not retried.",
        )
    }

    private fun requestMessageForm(
        stationId: StationId,
        albumId: String,
        page: AuthenticatedPage,
    ): RequestMessageForm? {
        val stationOrigin = URI(origin(stationId))
        val referer = runCatching { URI(page.finalUrl) }.getOrNull() ?: return null
        if (!isSameHttpsOrigin(referer, stationOrigin)) {
            return null
        }
        val form = Jsoup.parse(page.html, page.finalUrl).selectFirst(
            "form:has(textarea[name=msg]):has([name=send]):has([name=remLen])",
        ) ?: return null
        if (form.selectFirst("[name=send][value=Send]") == null) return null
        val action = runCatching { stationOrigin.resolve(form.attr("action")) }.getOrNull() ?: return null
        val expected = stationOrigin
        if (!isSameHttpsOrigin(action, expected)) {
            return null
        }
        val parameters = runCatching { queryParameters(action) }.getOrNull() ?: return null
        if (
            action.path != "/modules.php" || parameters["name"] != "Album" ||
            parameters["action"] != "submitmessage" || parameters["asin"] != albumId ||
            parameters["id"]?.matches(NUMERIC_ID) != true
        ) {
            return null
        }
        val fields = linkedMapOf<String, String>()
        form.select("input[name]").forEach { input ->
            if (!input.hasAttr("disabled") && (input.attr("type") !in setOf("checkbox", "radio") || input.hasAttr("checked"))) {
                fields.putIfAbsent(input.attr("name"), input.`val`())
            }
        }
        if (setOf("msg", "send", "remLen").any { it !in fields && it != "msg" }) return null
        return RequestMessageForm(
            action = action,
            referer = referer,
            fields = fields,
        )
    }

    private fun postOptionalMessage(
        stationId: StationId,
        message: String,
        manager: CookieManager,
        form: RequestMessageForm,
    ): RequestSubmissionResult? {
        val messageResult = runCatching {
            request(
                stationId,
                form.action,
                authenticated = true,
                cookieManager = manager,
                method = "POST",
                formFields = form.fields.toMutableMap().apply {
                    this["msg"] = message
                    this["send"] = "Send"
                    this["remLen"] = (MAX_REQUEST_MESSAGE_CHARACTERS - message.length).toString()
                },
                referer = form.referer,
                stopReadingWhen = { it.contains("message has been saved", ignoreCase = true) },
            )
        }.getOrNull()
        if (messageResult == null) return null
        val messageText = Jsoup.parse(messageResult.html).text().replace(Regex("\\s+"), " ").trim()
        return if (messageText.contains("log in", true) || messageText.contains("login", true)) {
            RequestSubmissionResult.Submitted(
                "The station reported the request accepted, but the optional message was not added because " +
                    "the station sign-in expired.",
            )
        } else if (messageText.contains("message has been saved", true)) {
            RequestSubmissionResult.Submitted(
                "The station reported the request and optional message saved. " +
                    "Confirm it in Queue before requesting again.",
            )
        } else {
            RequestSubmissionResult.Submitted(
                "The station reported the request accepted, but the optional message response was not recognized. " +
                    "The request was not retried.",
            )
        }
    }

    private fun queryParameters(uri: URI): Map<String, String> = uri.rawQuery.orEmpty()
        .split('&')
        .mapNotNull { field ->
            if (field.isBlank()) return@mapNotNull null
            val separator = field.indexOf('=')
            val name = if (separator >= 0) field.substring(0, separator) else field
            val value = if (separator >= 0) field.substring(separator + 1) else ""
            URLDecoder.decode(name, StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }
        .toMap()

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
        if (ACCEPTANCE_PATTERNS.none { it.containsMatchIn(text) }) {
            throw IOException("Unrecognized station request confirmation")
        }
        return RequestSubmissionResult.Submitted(
            "The station reported the request accepted. Confirm it in Queue before requesting again.",
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
        stopReadingWhen: ((String) -> Boolean)? = null,
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
                if (requestMethod == "POST") {
                    connection.setRequestProperty("Origin", "${uri.scheme}://${uri.host}")
                }
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
                if (authenticated && cookieManager != null) {
                    sessions.captureResponse(stationId, origin(stationId), uri, connection.headerFields)
                }
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
                    it.readBoundedUntil(MAX_RESPONSE_CHARACTERS, stopReadingWhen)
                }
                return AuthenticatedPage(html, uri.toASCIIString())
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Station request did not complete")
    }

    private fun authenticatedCookieManager(stationId: StationId, origin: String): CookieManager {
        return sessions.cookieManager(stationId, origin)
    }

    private fun requireSameOrigin(stationId: StationId, uri: URI) {
        val expected = URI(origin(stationId))
        if (uri.scheme != "https") throw IOException("Untrusted request scheme")
        if (uri.userInfo != null) throw IOException("Untrusted request authority")
        if (!uri.host.equals(expected.host, true)) throw IOException("Untrusted request host")
        if (effectivePort(uri) != effectivePort(expected)) throw IOException("Untrusted request port")
    }

    private fun trustedRedirect(stationId: StationId, redirect: URI): URI {
        val expected = URI(origin(stationId))
        val trustedHosts = REDIRECT_HOSTS[stationId.canonicalized()] ?: setOf(expected.host)
        if (trustedHosts.none { it.equals(redirect.host, ignoreCase = true) }) return redirect
        if (
            (redirect.scheme == "http" && effectivePort(redirect) == 80) ||
            (redirect.scheme == "https" && effectivePort(redirect) == effectivePort(expected))
        ) {
            return URI(
                "https",
                null,
                expected.host,
                expected.port,
                redirect.path,
                redirect.query,
                redirect.fragment,
            )
        }
        return redirect
    }

    private fun isSameHttpsOrigin(candidate: URI, expected: URI): Boolean =
        candidate.scheme == "https" && candidate.host.equals(expected.host, true) &&
            effectivePort(candidate) == effectivePort(expected)

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", true) -> 443
        uri.scheme.equals("http", true) -> 80
        else -> -1
    }

    private fun responseCharset(contentType: String?): Charset {
        val declared = contentType?.substringAfter("charset=", "")?.substringBefore(';')?.trim()?.trim('"')
        return runCatching { Charset.forName(declared.orEmpty()) }.getOrDefault(StandardCharsets.ISO_8859_1)
    }

    private fun containsCompleteMessageForm(html: String): Boolean {
        val actionIndex = html.indexOf("submitmessage", ignoreCase = true)
        return actionIndex >= 0 && html.indexOf("</form>", startIndex = actionIndex, ignoreCase = true) >= 0
    }

    private fun containsTerminalRequestResponse(html: String): Boolean {
        if (containsCompleteMessageForm(html)) return true
        val text = Jsoup.parse(html).text().replace(Regex("\\s+"), " ").trim()
        return REJECTION_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun origin(stationId: StationId): String =
        ORIGINS[stationId.canonicalized()] ?: throw IOException("Unsupported station")
    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 60_000
        const val MAX_RESPONSE_CHARACTERS = 1_000_000
        const val MAX_REDIRECTS = 5
        const val MAX_ARTIST_NAME_LENGTH = 200
        const val MAX_NOTICE_CHARACTERS = 240
        val SAFE_ALBUM_ID = Regex("[A-Za-z0-9_-]{1,64}")
        val NUMERIC_ID = Regex("\\d{1,20}")
        val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
        val REJECTION_PATTERNS = listOf(
            Regex("request failed", RegexOption.IGNORE_CASE),
            Regex("played recently", RegexOption.IGNORE_CASE),
            Regex("cannot request", RegexOption.IGNORE_CASE),
            Regex("can only request", RegexOption.IGNORE_CASE),
            Regex("not available", RegexOption.IGNORE_CASE),
            Regex("already (?:in|on) the queue", RegexOption.IGNORE_CASE),
            Regex("(?:wait|cooldown|too soon|request limit)", RegexOption.IGNORE_CASE),
            Regex("not eligible", RegexOption.IGNORE_CASE),
        )
        val ACCEPTANCE_PATTERNS = listOf(
            Regex("request successful", RegexOption.IGNORE_CASE),
            Regex("request has successfully been delivered", RegexOption.IGNORE_CASE),
            Regex("request was successfully delivered", RegexOption.IGNORE_CASE),
        )
        val ORIGINS = mapOf(
            StationId("sst") to "https://streamingsoundtracks.com/",
            StationId("1980s") to "https://1980s.fm/",
            StationId("afm") to "https://adagio.fm/",
            StationId("dfm") to "https://death.fm/",
            StationId("efm") to "https://entranced.fm/",
        )
        val REDIRECT_HOSTS = mapOf(
            StationId("sst") to setOf("streamingsoundtracks.com", "www.streamingsoundtracks.com"),
        )
    }

    private data class RequestMessageForm(
        val action: URI,
        val referer: URI,
        val fields: Map<String, String>,
    )
}
