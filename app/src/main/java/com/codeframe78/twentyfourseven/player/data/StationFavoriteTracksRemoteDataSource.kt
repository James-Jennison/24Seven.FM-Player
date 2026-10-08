package com.codeframe78.twentyfourseven.player.data

import android.util.Log
import com.codeframe78.twentyfourseven.player.domain.FavoriteChange
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
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

internal interface FavoriteTracksRemoteDataSource {
    suspend fun load(stationId: StationId): List<FavoriteTrack>

    /** One page of the member's own ranked favorites, from the profile feed. Needs the station session. */
    suspend fun loadRanked(stationId: StationId, page: Int): RankedFavoritesPage

    /** Sends one change for one track the way the station's own favorites controls do. Needs the station session. */
    suspend fun change(stationId: StationId, songId: String, change: FavoriteChange)
}

internal class StationFavoriteTracksRemoteDataSource(
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val parser: FavoriteTracksPageParser = FavoriteTracksPageParser(),
    private val sessions: StationAuthSessionCoordinator = StationAuthSessionCoordinator(sessionStore),
    private val connectionFactory: (URI) -> HttpURLConnection = {
        it.toURL().openConnection() as HttpURLConnection
    },
) : FavoriteTracksRemoteDataSource {
    override suspend fun load(stationId: StationId): List<FavoriteTrack> = withContext(Dispatchers.IO) {
        try {
            val origin = origin(stationId)
            val manager = authenticatedCookieManager(stationId, origin)
            val discovery = request(stationId, URI(origin).resolve(FAVORITES_PATH), origin, manager, DISCOVERY_LIMIT)
            val listUrl = parser.parseListUrl(discovery, origin)
            parser.parseTracks(request(stationId, URI(listUrl), origin, manager, LIST_LIMIT), origin)
        } catch (failure: FavoritesAuthenticationRequiredException) {
            sessions.expire(stationId)
            throw failure
        }
    }

    override suspend fun loadRanked(stationId: StationId, page: Int): RankedFavoritesPage = withContext(Dispatchers.IO) {
        require(page in 1..MAX_RANKED_PAGES)
        try {
            val origin = origin(stationId)
            val manager = authenticatedCookieManager(stationId, origin)
            // The favorites page names the member's own list, which carries the member number the feed needs.
            val discovery = request(stationId, URI(origin).resolve(FAVORITES_PATH), origin, manager, DISCOVERY_LIMIT)
            val memberNumber = parser.listMemberNumber(parser.parseListUrl(discovery, origin))
                ?: throw IOException("Favorites list did not name the member")
            val feed = URI(origin).resolve("$RANKED_PATH?user2view=$memberNumber&kind=tracks&tracks_page=$page&albums_page=1")
            parser.parseRankedPage(request(stationId, feed, origin, manager, RANKED_LIMIT), origin, (page - 1) * RANKED_PAGE_SIZE + 1)
        } catch (failure: FavoritesAuthenticationRequiredException) {
            sessions.expire(stationId)
            throw failure
        }
    }

    override suspend fun change(stationId: StationId, songId: String, change: FavoriteChange) =
        withContext(Dispatchers.IO) {
            require(songId.matches(SONG_ID))
            val origin = origin(stationId)
            val manager = authenticatedCookieManager(stationId, origin)
            val operation = when (change) {
                FavoriteChange.MoveUp -> "movetrackup"
                FavoriteChange.MoveDown -> "movetrackdown"
                FavoriteChange.Remove -> "removetrackfromlist2"
            }
            // The station's own controls form posts these three fields; Yes2Remove is its confirmation flag.
            val form = listOf("songid" to songId, "Yes2Remove" to "1", "op" to operation)
                .joinToString("&") { (name, value) -> "${encode(name)}=${encode(value)}" }
            val response = request(stationId, URI(origin).resolve(FAVORITES_PATH), origin, manager, CHANGE_LIMIT, form)
            if (Log.isLoggable(CHANGE_LOG_TAG, Log.DEBUG)) {
                Log.d(CHANGE_LOG_TAG, "op=$operation responseChars=${response.length} signedOut=${parser.showsSignedOutVisitor(response, origin)}")
            }
            if (parser.showsSignedOutVisitor(response, origin)) {
                sessions.expire(stationId)
                throw FavoritesAuthenticationRequiredException()
            }
            // The station's own page treats these phrases in the answer as a failed update.
            if (CHANGE_FAILURE.containsMatchIn(response)) throw IOException("Favorites update failed")
        }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun authenticatedCookieManager(stationId: StationId, origin: String): CookieManager {
        return sessions.cookieManager(stationId, origin).also { manager ->
            if (manager.cookieStore.cookies.isEmpty()) throw FavoritesAuthenticationRequiredException()
        }
    }

    private fun request(
        stationId: StationId,
        initialUri: URI,
        origin: String,
        manager: CookieManager,
        limit: Int,
        form: String? = null,
    ): String {
        val expected = URI(origin)
        var uri = initialUri
        var pendingForm = form
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            requireSameOrigin(uri, expected)
            val connection = connectionFactory(uri)
            try {
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "text/html")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                manager.get(uri, emptyMap()).forEach { (name, values) ->
                    connection.setRequestProperty(name, values.joinToString("; "))
                }
                pendingForm?.let { body ->
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    // The station's own controls form is submitted from its pages, so the post says where it came from.
                    connection.setRequestProperty("Origin", origin.trimEnd('/'))
                    connection.setRequestProperty("Referer", origin + FAVORITES_PATH.trimStart('/'))
                    connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.US_ASCII)) }
                }
                val status = connection.responseCode
                if (form != null && Log.isLoggable(CHANGE_LOG_TAG, Log.DEBUG)) {
                    // Only when the tag is enabled with setprop: no cookies, identities, or page text, just how the
                    // station answered the post.
                    Log.d(
                        CHANGE_LOG_TAG,
                        "step=${redirectCount} method=${connection.requestMethod} path=${uri.path} status=$status " +
                            "location=${connection.getHeaderField("Location")?.substringBefore('?')}",
                    )
                }
                sessions.captureResponse(stationId, origin, uri, connection.headerFields)
                if (status in REDIRECT_STATUSES) {
                    if (redirectCount == MAX_REDIRECTS) throw IOException("Too many favorites redirects")
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Favorites redirect was invalid")
                    uri = uri.resolve(location)
                    // A redirect after a post leads to the page that shows the result, which is read with GET.
                    if (status != 307 && status != 308) pendingForm = null
                    return@repeat
                }
                if (status !in 200..299) throw IOException("Station returned HTTP $status")
                val charset = declaredCharset(connection.contentType, StandardCharsets.ISO_8859_1)
                return connection.inputStream.bufferedReader(charset).use {
                    it.readBounded(limit)
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("Favorites request did not complete")
    }

    private fun requireSameOrigin(uri: URI, origin: URI) {
        if (
            uri.scheme != "https" || uri.userInfo != null ||
            !uri.host.equals(origin.host, true) || uri.port != origin.port
        ) {
            throw IOException("Untrusted favorites destination")
        }
    }

    private fun origin(stationId: StationId): String = ORIGINS[stationId.canonicalized()]
        ?: throw IOException("Unsupported station")

    private companion object {
        const val FAVORITES_PATH = "/modules.php?name=Favorites"
        const val USER_AGENT = "24Seven.FM-Player/0.1 (Android; unofficial non-commercial client)"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val DISCOVERY_LIMIT = 512_000
        const val LIST_LIMIT = 5_000_000
        const val CHANGE_LIMIT = 512_000
        const val CHANGE_LOG_TAG = "FavoriteChange"
        const val RANKED_PATH = "/modules/Your_Profile/favsTabAJAX.php"
        const val RANKED_LIMIT = 1_000_000
        const val RANKED_PAGE_SIZE = 50
        const val MAX_RANKED_PAGES = 40
        val CHANGE_FAILURE = Regex("Can't (?:delete|update|open)|Fatal error|Hacking attempt", RegexOption.IGNORE_CASE)
        val SONG_ID = Regex("[0-9]{1,10}")
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
