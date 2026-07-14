package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
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
}

internal class PlayerQueueRemoteDataSource(
    private val parser: PlayerQueueResponseParser = PlayerQueueResponseParser(),
) : QueueRemoteDataSource {
    override suspend fun fetch(stationId: StationId): QueuePayload = withContext(Dispatchers.IO) {
        val endpoint = endpoints[stationId] ?: throw IOException("Unsupported station")
        if (endpoint.extendedQueue) {
            val origin = "https://${endpoint.domain}/"
            val response = get(
                url = "${origin}modules/Queue_Played/Queue_Played-gen.php",
                referer = "${origin}modules.php?name=Queue_Played",
                accept = "text/html",
                fallbackCharset = StandardCharsets.ISO_8859_1,
            )
            return@withContext parser.parseExtended(response, origin)
        }
        val playerUrl = "https://${endpoint.domain}/player.php"
        val response = get(
            url = "$playerUrl?ajax_action=get_db_info&station=${endpoint.stationCode}&asin=",
            referer = playerUrl,
            accept = "application/json",
            fallbackCharset = StandardCharsets.UTF_8,
        )
        parser.parse(response, "https://${endpoint.domain}/")
    }

    private fun get(url: String, referer: String, accept: String, fallbackCharset: Charset): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = REQUEST_TIMEOUT_MILLIS
            connection.readTimeout = REQUEST_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", accept)
            connection.setRequestProperty("Referer", referer)
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (accept == "application/json") {
                connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
            }
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("Station returned HTTP $status")
            val charset = connection.contentType
                ?.substringAfter("charset=", "")
                ?.substringBefore(';')
                ?.trim()
                ?.trim('"')
                ?.takeIf(String::isNotEmpty)
                ?.let { runCatching { Charset.forName(it) }.getOrNull() }
                ?: fallbackCharset
            connection.inputStream.bufferedReader(charset).use { reader ->
                reader.readBounded(MAX_RESPONSE_CHARACTERS)
            }
        } finally {
            connection.disconnect()
        }
    }

    private data class Endpoint(
        val domain: String,
        val stationCode: String,
        val extendedQueue: Boolean = true,
    )

    private companion object {
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val REQUEST_TIMEOUT_MILLIS = 10_000
        const val MAX_RESPONSE_CHARACTERS = 512_000
        val endpoints = mapOf(
            StationId("sst") to Endpoint("streamingsoundtracks.com", "sst"),
            StationId("1980s") to Endpoint("1980s.fm", "80s"),
            StationId("adagio") to Endpoint("adagio.fm", "afm"),
            StationId("death") to Endpoint("death.fm", "dfm", extendedQueue = false),
            StationId("entranced") to Endpoint("entranced.fm", "efm"),
        )
    }
}

internal fun Reader.readBounded(maxCharacters: Int): String {
    val result = StringBuilder()
    val buffer = CharArray(8_192)
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toString()
        if (result.length + count > maxCharacters) throw IOException("Station response was too large")
        result.append(buffer, 0, count)
    }
}
