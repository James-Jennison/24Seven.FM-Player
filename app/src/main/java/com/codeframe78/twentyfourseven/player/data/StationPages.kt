package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** A station answered with a status other than success, such as 403 for a members-only page. */
internal class StationHttpException(val status: Int) : IOException("Station returned HTTP $status")

internal data class StationPageResponse(val body: String, val uri: URI)

/** Reads and submits pages on one station's own site with that station's protected session. */
internal interface StationPages {
    fun origin(stationId: StationId): String

    /** Whether a saved session exists; it may still have ended on the station's side. */
    fun hasSession(stationId: StationId): Boolean

    /** Ends the saved session after a page showed clear evidence that the station no longer honours it. */
    fun expireSession(stationId: StationId)

    fun get(stationId: StationId, path: String, maxCharacters: Int): StationPageResponse

    fun postForm(
        stationId: StationId,
        path: String,
        fields: List<Pair<String, String>>,
        maxCharacters: Int,
        charset: Charset = StandardCharsets.UTF_8,
    ): StationPageResponse
}

internal class HttpStationPages(
    private val sessions: StationAuthSessionCoordinator,
    private val connectionFactory: (URI) -> HttpURLConnection = {
        it.toURL().openConnection() as HttpURLConnection
    },
) : StationPages {
    override fun origin(stationId: StationId): String = ORIGINS[stationId.canonicalized()]
        ?: throw IOException("Unsupported station")

    override fun hasSession(stationId: StationId): Boolean =
        sessions.cookieManager(stationId, origin(stationId)).cookieStore.cookies.isNotEmpty()

    override fun expireSession(stationId: StationId) = sessions.expire(stationId)

    override fun get(stationId: StationId, path: String, maxCharacters: Int): StationPageResponse =
        request(stationId, path, body = null, maxCharacters = maxCharacters)

    override fun postForm(
        stationId: StationId,
        path: String,
        fields: List<Pair<String, String>>,
        maxCharacters: Int,
        charset: Charset,
    ): StationPageResponse {
        val encoded = fields.joinToString("&") { (name, value) ->
            "${URLEncoder.encode(name, charset.name())}=${URLEncoder.encode(value, charset.name())}"
        }
        return request(stationId, path, FormBody(encoded.toByteArray(StandardCharsets.US_ASCII), charset), maxCharacters)
    }

    private fun request(stationId: StationId, path: String, body: FormBody?, maxCharacters: Int): StationPageResponse {
        val origin = origin(stationId)
        val expectedOrigin = URI(origin)
        val cookies = sessions.cookieManager(stationId, origin)
        var uri = expectedOrigin.resolve(path)
        var pendingBody = body
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            TrustedStationNavigation.requireSameHttpsOrigin(uri, expectedOrigin)
            val connection = connectionFactory(uri)
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.requestMethod = if (pendingBody == null) "GET" else "POST"
                connection.setRequestProperty("Accept", "text/html")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                cookies.get(uri, emptyMap()).forEach { (name, values) ->
                    connection.setRequestProperty(name, values.joinToString("; "))
                }
                pendingBody?.let { form ->
                    connection.doOutput = true
                    connection.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded; charset=${form.charset.name()}",
                    )
                    connection.outputStream.use { it.write(form.bytes) }
                }
                val status = connection.responseCode
                sessions.captureResponse(stationId, origin, uri, connection.headerFields)
                if (status in REDIRECT_STATUSES) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("Too many station redirects")
                    uri = TrustedStationNavigation.resolveRedirect(
                        uri,
                        connection.getHeaderField("Location"),
                        expectedOrigin,
                    )
                    if (status != 307 && status != 308) pendingBody = null
                    return@repeat
                }
                if (status !in 200..299) throw StationHttpException(status)
                val charset = declaredCharset(connection.contentType, StandardCharsets.UTF_8)
                val text = connection.inputStream.bufferedReader(charset).use { it.readBounded(maxCharacters) }
                return StationPageResponse(text, uri)
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Station request did not complete")
    }

    private class FormBody(val bytes: ByteArray, val charset: Charset)

    private companion object {
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
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
