package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.NowPlayingArtworkRepository
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class StationNowPlayingArtworkRepository internal constructor(
    private val parser: CurrentTrackArtworkParser = CurrentTrackArtworkParser(),
    private val connectionFactory: (URI) -> HttpURLConnection = {
        it.toURL().openConnection() as HttpURLConnection
    },
) : NowPlayingArtworkRepository {
    override suspend fun fetchArtwork(stationId: StationId): String? = withContext(Dispatchers.IO) {
        val domain = domains[stationId.canonicalized()] ?: throw IOException("Unsupported station")
        val origin = "https://$domain/"
        val expectedOrigin = URI(origin)
        var uri = expectedOrigin.resolve("soap/FM24sevenJSON.php?action=GetCurrentlyPlaying")
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            TrustedStationNavigation.requireSameHttpsOrigin(uri, expectedOrigin)
            val connection = connectionFactory(uri)
            try {
                connection.connectTimeout = REQUEST_TIMEOUT_MILLIS
                connection.readTimeout = REQUEST_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Referer", "${origin}player.php")
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
                val response = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    reader.readBounded(MAX_RESPONSE_CHARACTERS)
                }
                return@withContext parser.parse(response, origin)
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Station request did not complete")
    }

    private companion object {
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val REQUEST_TIMEOUT_MILLIS = 10_000
        const val MAX_RESPONSE_CHARACTERS = 64_000
        const val MAX_REDIRECTS = 5
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

internal class CurrentTrackArtworkParser {
    fun parse(json: String, baseUrl: String): String? {
        val response = JSONObject(json)
        val origin = URI(baseUrl)
        val explicitAsin = response.optString("ASIN", "")
            .trim()
            .takeIf(ASIN::matches)
        val coverUri = response.optString("CoverLink", "")
            .trim()
            .takeIf(String::isNotEmpty)
            ?.let { runCatching { origin.resolve(it) }.getOrNull() }
            ?.takeIf { isSafeStationUrl(it, origin) }
        val coverAsin = coverUri?.path
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.takeIf(ASIN::matches)
        val asin = explicitAsin ?: coverAsin
        return if (asin != null) {
            URI(origin.scheme, null, origin.host, origin.port, "/images/cover/500/$asin.jpg", null, null).toString()
        } else {
            coverUri?.toString()
        }
    }

    private fun isSafeStationUrl(candidate: URI, origin: URI): Boolean =
        candidate.scheme.equals("https", ignoreCase = true) &&
            candidate.host.equals(origin.host, ignoreCase = true) &&
            candidate.userInfo == null &&
            candidate.port in setOf(-1, origin.port)

    private companion object {
        val ASIN = Regex("[A-Za-z0-9]{10}")
    }
}
