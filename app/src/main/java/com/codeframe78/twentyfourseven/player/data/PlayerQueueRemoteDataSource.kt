package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal interface QueueRemoteDataSource {
    suspend fun fetch(stationId: StationId): QueuePayload
    suspend fun fetchForVerification(stationId: StationId): QueuePayload = fetch(stationId)
}

internal class PlayerQueueRemoteDataSource(
    private val parser: PlayerQueueResponseParser = PlayerQueueResponseParser(),
    private val connectionFactory: (URI) -> HttpURLConnection = {
        it.toURL().openConnection() as HttpURLConnection
    },
) : QueueRemoteDataSource {
    override suspend fun fetch(stationId: StationId): QueuePayload = fetch(
        stationId,
        maxExtendedTracks = VISIBLE_QUEUE_TRACK_LIMIT,
    )

    override suspend fun fetchForVerification(stationId: StationId): QueuePayload = fetch(
        stationId,
        maxExtendedTracks = VERIFICATION_QUEUE_TRACK_LIMIT,
    )

    private suspend fun fetch(
        stationId: StationId,
        maxExtendedTracks: Int,
    ): QueuePayload = withContext(Dispatchers.IO) {
        val domain = domains[stationId.canonicalized()] ?: throw IOException("Unsupported station")
        val origin = "https://$domain/"
        val response = get(
            url = "${origin}modules/Queue_Played/Queue_Played-gen.php",
            referer = "${origin}modules.php?name=Queue_Played",
            fallbackCharset = StandardCharsets.ISO_8859_1,
            expectedOrigin = URI(origin),
        )
        parser.parseExtended(response, origin, maxExtendedTracks)
    }

    private fun get(
        url: String,
        referer: String,
        fallbackCharset: Charset,
        expectedOrigin: URI,
    ): String {
        var uri = URI(url)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            TrustedStationNavigation.requireSameHttpsOrigin(uri, expectedOrigin)
            val connection = connectionFactory(uri)
            try {
                connection.connectTimeout = REQUEST_TIMEOUT_MILLIS
                connection.readTimeout = REQUEST_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "text/html")
                connection.setRequestProperty("Referer", referer)
                connection.setRequestProperty("User-Agent", USER_AGENT)
                val status = connection.responseCode
                if (status in REDIRECT_STATUSES) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("Too many station redirects")
                    uri = TrustedStationNavigation.resolveRedirect(
                        uri,
                        connection.getHeaderField("Location"),
                        expectedOrigin,
                    )
                    return@repeat
                }
                if (status !in 200..299) throw IOException("Station returned HTTP $status")
                val charset = declaredCharset(connection.contentType, fallbackCharset)
                return connection.inputStream.bufferedReader(charset).use { reader ->
                    reader.readBounded(MAX_RESPONSE_CHARACTERS)
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Station request did not complete")
    }

    private companion object {
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val REQUEST_TIMEOUT_MILLIS = 10_000
        const val MAX_RESPONSE_CHARACTERS = 512_000
        const val MAX_REDIRECTS = 5
        const val VISIBLE_QUEUE_TRACK_LIMIT = 30
        const val VERIFICATION_QUEUE_TRACK_LIMIT = 500
        val REDIRECT_STATUSES = setOf(301, 302, 303, 307, 308)
        val domains = mapOf(
            StationId("sst") to "streamingsoundtracks.com",
            StationId("1980s") to "1980s.fm",
            StationId("afm") to "adagio.fm",
            StationId("dfm") to "death.fm",
            StationId("efm") to "entranced.fm",
        )
    }
}

internal fun Reader.readBounded(maxCharacters: Int): String {
    return readBoundedUntil(maxCharacters)
}

internal fun Reader.readBoundedUntil(
    maxCharacters: Int,
    stopReadingWhen: ((String) -> Boolean)? = null,
): String {
    val result = StringBuilder()
    val buffer = CharArray(8_192)
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toString()
        if (result.length + count > maxCharacters) throw IOException("Station response was too large")
        result.append(buffer, 0, count)
        if (stopReadingWhen?.invoke(result.toString()) == true) return result.toString()
    }
}
