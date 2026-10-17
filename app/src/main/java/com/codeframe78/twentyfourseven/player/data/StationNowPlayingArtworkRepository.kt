package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.NowPlayingArtworkRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingDetailsRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import com.codeframe78.twentyfourseven.player.domain.normalizeTrailingTheArticle
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.parser.Parser

class StationNowPlayingArtworkRepository internal constructor(
    private val parser: CurrentTrackArtworkParser = CurrentTrackArtworkParser(),
    private val connectionFactory: (URI) -> HttpURLConnection = {
        it.toURL().openConnection() as HttpURLConnection
    },
    private val elapsedRealtimeMillis: () -> Long = { android.os.SystemClock.elapsedRealtime() },
) : NowPlayingArtworkRepository, NowPlayingDetailsRepository {
    override suspend fun fetchArtwork(stationId: StationId): String? =
        fetchNowPlaying(stationId)?.artworkUrl

    override suspend fun fetchNowPlaying(stationId: StationId): NowPlayingState? {
        val official = runCatching {
            fetch(stationId, registryFeedUrl(stationId) ?: OFFICIAL_FEED_PATH) { json, origin, receivedAt ->
                parser.parseOfficialFeed(json, origin, stationId, receivedAt)
            }
        }
        // An answered feed (even "unavailable") is authoritative; only fall back when it failed.
        if (official.isFailure) return fetchLegacy(stationId)
        val state = official.getOrNull() ?: return null
        // The public feed leaves out requester and listener data; the station endpoint still supplies it.
        val extras = runCatching { fetchLegacy(stationId) }.getOrNull()
            ?.takeIf { it.track != null && it.track == state.track }
            ?: return state
        return state.copy(
            albumId = state.albumId ?: extras.albumId,
            requesterName = extras.requesterName,
            requestMessage = extras.requestMessage,
            listenerCount = extras.listenerCount,
        )
    }

    private suspend fun fetchLegacy(stationId: StationId): NowPlayingState? =
        fetch(stationId, LEGACY_FEED_PATH) { json, origin, receivedAt ->
            parser.parseNowPlaying(json, origin, stationId, receivedAt)
        }

    // station-feeds.json is the documented discovery registry (https://24seven.fm/developers.php).
    // It is cached and only supplies each station's feed URL; the fetch still pins the station's origin.
    private suspend fun registryFeedUrl(stationId: StationId): String? {
        val now = System.currentTimeMillis()
        val cached = registryCache
        val feeds = if (cached != null && now - cached.fetchedAt < REGISTRY_TTL_MILLIS) {
            cached.feeds
        } else {
            val fetched = runCatching { fetchRegistry() }.getOrNull()
            if (fetched != null) registryCache = RegistryCache(now, fetched)
            fetched ?: cached?.feeds
        }
        return feeds?.get(registryIds[stationId.canonicalized()] ?: stationId.canonicalized().value)
    }

    private suspend fun fetchRegistry(): Map<String, String> = withContext(Dispatchers.IO) {
        val connection = connectionFactory(URI(REGISTRY_URL))
        try {
            connection.connectTimeout = REQUEST_TIMEOUT_MILLIS
            connection.readTimeout = REQUEST_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode !in 200..299) throw IOException("Registry returned HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use {
                it.readBounded(MAX_RESPONSE_CHARACTERS)
            }
            parseRegistry(body)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetch(
        stationId: StationId,
        path: String,
        parse: (json: String, origin: String, receivedAt: Long) -> NowPlayingState?,
    ): NowPlayingState? = withContext(Dispatchers.IO) {
        val domain = domains[stationId.canonicalized()] ?: throw IOException("Unsupported station")
        val origin = "https://$domain/"
        val expectedOrigin = URI(origin)
        var uri = expectedOrigin.resolve(path)
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
                return@withContext parse(response, origin, elapsedRealtimeMillis())
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Station request did not complete")
    }

    private data class RegistryCache(val fetchedAt: Long, val feeds: Map<String, String>)

    @Volatile
    private var registryCache: RegistryCache? = null

    internal companion object {
        /** Maps registry station id -> now_playing_json URL; ignores unknown fields and invalid entries. */
        fun parseRegistry(json: String): Map<String, String> {
            val root = JSONObject(json)
            if (root.optInt("schema_version", 0) != 1) throw IOException("Unsupported registry schema")
            val stations = root.getJSONArray("stations")
            return buildMap {
                for (i in 0 until stations.length()) {
                    val entry = stations.optJSONObject(i) ?: continue
                    val id = entry.optString("id", "").takeIf(String::isNotBlank) ?: continue
                    val feed = entry.optString("now_playing_json", "").takeIf { it.startsWith("https://") } ?: continue
                    put(id, feed)
                }
            }
        }

        const val REGISTRY_URL = "https://24seven.fm/station-feeds.json"
        const val REGISTRY_TTL_MILLIS = 6 * 60 * 60 * 1000L
        val registryIds = mapOf(StationId("1980s") to "80s")
        const val OFFICIAL_FEED_PATH = "now-playing.php"
        const val LEGACY_FEED_PATH = "soap/FM24sevenJSON.php?action=GetCurrentlyPlaying"
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; official non-commercial client)"
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
        return parseNowPlaying(json, baseUrl, StationId("unknown"))?.artworkUrl
    }

    fun parseNowPlaying(
        json: String,
        baseUrl: String,
        stationId: StationId,
        receivedAtElapsedRealtimeMillis: Long? = null,
    ): NowPlayingState? {
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
        val artworkUrl = if (asin != null) {
            URI(origin.scheme, null, origin.host, origin.port, "/images/cover/500/$asin.jpg", null, null).toString()
        } else {
            coverUri?.toString()
        }
        val artist = response.optString("Artist", "").decodeHtmlEntities()?.normalizeTrailingTheArticle()
        val album = response.optString("Album", "").decodeHtmlEntities()
        val track = response.optString("Track", "").decodeHtmlEntities()?.normalizeTrailingTheArticle()
        val displayTitle = listOfNotNull(artist, track).joinToString(" - ").takeIf(String::isNotBlank)
            ?: track
            ?: artist
        return if (displayTitle == null && artworkUrl == null) {
            null
        } else {
            NowPlayingState(
                stationId = stationId,
                displayTitle = displayTitle,
                artworkUrl = artworkUrl,
                artist = artist,
                album = album,
                track = track,
                albumId = asin,
                requesterName = response.optText("RequestedBy"),
                requestMessage = response.optText("Message"),
                listenerCount = response.optText("ListenerCount")?.toIntOrNull()?.takeIf { it >= 0 },
                trackLengthMillis = response.optText("Length")?.toLongOrNull()?.takeIf { it > 0 },
                trackStartedElapsedRealtimeMillis = receivedAtElapsedRealtimeMillis?.let { receivedAt ->
                    playedMillis(response)?.let { played -> receivedAt - played }
                },
            )
        }
    }

    /** How long the station says the track has been playing, from its own start and current times. */
    private fun playedMillis(response: JSONObject): Long? {
        val started = response.optText("PlayStart")?.toStationTime() ?: return null
        val now = response.optText("SystemTime")?.toStationTime() ?: return null
        return Duration.between(started, now).toMillis().takeIf { it in 0..MAX_PLAYED_MILLIS }
    }

    private fun String.toStationTime(): LocalDateTime? = runCatching { LocalDateTime.parse(this) }.getOrNull()

    private fun JSONObject.optText(name: String): String? =
        if (isNull(name)) null else optString(name, "").decodeHtmlEntities()

    /** Parses the documented schema-version-1 feed at https://24seven.fm/developers.php. */
    fun parseOfficialFeed(
        json: String,
        baseUrl: String,
        stationId: StationId,
        receivedAtElapsedRealtimeMillis: Long? = null,
    ): NowPlayingState? {
        val response = JSONObject(json)
        if (response.optInt("schema_version", 0) != 1) throw IOException("Unsupported feed schema")
        val origin = URI(baseUrl)
        val track = response.optJSONObject("track")
        if (response.optString("status") != "playing" || track == null) return null
        val artist = track.optText("artist")?.normalizeTrailingTheArticle()
        val title = track.optText("title")?.normalizeTrailingTheArticle()
        val displayTitle = listOfNotNull(artist, title).joinToString(" - ").takeIf(String::isNotBlank)
            ?: return null
        val artworkUri = response.optString("artwork_url", "")
            .trim()
            .takeIf(String::isNotEmpty)
            ?.let { runCatching { origin.resolve(it) }.getOrNull() }
            ?.takeIf { isSafeStationUrl(it, origin) }
        val played = runCatching {
            Duration.between(
                OffsetDateTime.parse(track.getString("started_at")),
                OffsetDateTime.parse(response.getString("generated_at")),
            ).toMillis()
        }.getOrNull()?.takeIf { it in 0..MAX_PLAYED_MILLIS }
        return NowPlayingState(
            stationId = stationId,
            displayTitle = displayTitle,
            artworkUrl = artworkUri?.toString(),
            artist = artist,
            album = track.optText("album"),
            track = title,
            albumId = artworkUri?.path?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf(ASIN::matches),
            trackLengthMillis = (track.optDouble("duration_seconds", 0.0) * 1000).toLong().takeIf { it > 0 },
            trackStartedElapsedRealtimeMillis = receivedAtElapsedRealtimeMillis?.let { at ->
                played?.let { at - it }
            },
        )
    }

    private fun isSafeStationUrl(candidate: URI, origin: URI): Boolean =
        candidate.scheme.equals("https", ignoreCase = true) &&
            candidate.host.equals(origin.host, ignoreCase = true) &&
            candidate.userInfo == null &&
            candidate.port in setOf(-1, origin.port)

    private companion object {
        val ASIN = Regex("[A-Za-z0-9]{10}")
        const val MAX_PLAYED_MILLIS = 6L * 60 * 60 * 1_000
    }
}

private fun String.decodeHtmlEntities(): String? = Parser.unescapeEntities(this, false)
    .trim()
    .takeIf(String::isNotBlank)
