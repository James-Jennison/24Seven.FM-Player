package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.classifyStationRequestAvailability
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** One page of the member's ranked favorites and, when the station offers one, the number of the page after it. */
internal data class RankedFavoritesPage(val tracks: List<FavoriteTrack>, val nextPage: Int?)

internal class FavoriteTracksPageParser {
    private val sessionEvidence = AuthLoginResultParser()

    /**
     * The ranked favorites the station's profile feed answers with: a JSON wrapper whose HTML holds one row per
     * track with the station's own move/remove form. Positions count from [firstPosition].
     */
    fun parseRankedPage(json: String, origin: String, firstPosition: Int = 1): RankedFavoritesPage {
        val originUri = trustedOrigin(origin)
        val html = runCatching { JSONObject(json).optString("HTML") }.getOrNull().orEmpty()
        if (html.isBlank()) throw IOException("Ranked favorites feed was not recognized")
        val document = Jsoup.parse(html, origin)
        val tracks = document.select("tr").asSequence()
            .filter { row -> row.selectFirst("form.favorite-controls-form input[name=songid]") != null }
            .mapNotNull { row ->
                val songId = row.selectFirst("form.favorite-controls-form input[name=songid]")?.attr("value")?.trim()
                    ?.takeIf { it.matches(NUMERIC_ID) } ?: return@mapNotNull null
                val details = row.selectFirst("td.td02") ?: return@mapNotNull null
                val title = details.selectFirst("span")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val artist = details.selectFirst("b")?.text()?.trim().orEmpty()
                // The second line reads "Album by Artist"; the album is what comes before the artist's name.
                val lines = details.html().split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE))
                val albumLine = lines.getOrNull(1)?.let { Jsoup.parse(it).text().trim() }.orEmpty()
                val album = albumLine.removeSuffix(artist).trim().removeSuffix("by").trim()
                    .ifBlank { row.selectFirst("td a[href*=asin=] img[title]")?.attr("title")?.trim().orEmpty() }
                val facts = lines.getOrNull(2)?.let { Jsoup.parse(it).text().trim() }.orEmpty().split(',', limit = 2)
                val requestCell = row.selectFirst("a[href*=name=Req]")
                val requestTrack = requestCell?.absUrl("href")?.let { parseRequestTrack(it, originUri, title, album, artist, "") }
                val availability = if (requestTrack == null) {
                    row.selectFirst("img[src*=requestbutton]")?.let { image ->
                        image.attr("title").ifBlank { image.attr("alt") }.trim().takeIf(String::isNotBlank)
                    }
                } else {
                    null
                }
                FavoriteTrack(
                    position = 0,
                    title = title,
                    album = album,
                    artist = artist,
                    genre = facts.getOrNull(1)?.trim()?.takeIf(String::isNotBlank),
                    year = facts.getOrNull(0)?.trim()?.takeIf { it.matches(YEAR) },
                    requestTrack = requestTrack,
                    availabilityMessage = availability,
                    availability = requestTrack?.availability ?: classifyStationRequestAvailability(availability),
                    albumId = requestTrack?.albumId ?: rowAlbumId(row),
                    songId = songId,
                )
            }
            .take(MAX_RANKED_PER_PAGE)
            .mapIndexed { index, track -> track.copy(position = firstPosition + index) }
            .toList()
        val nextPage = document.select("button.favorite-page[data-page-url]")
            .firstOrNull { it.text().trim().equals("Next", ignoreCase = true) }
            ?.attr("data-page-url")
            ?.let { url -> runCatching { URI(originUri.resolve(url).toString()) }.getOrNull() }
            ?.takeIf { it.path == RANKED_PATH }
            ?.let { queryValue(it.rawQuery, "tracks_page")?.toIntOrNull() }
            ?.takeIf { it > 0 }
        return RankedFavoritesPage(tracks, nextPage)
    }

    fun parseListUrl(html: String, origin: String): String {
        val originUri = trustedOrigin(origin)
        val source = Jsoup.parse(html, origin)
            .selectFirst("iframe#thelist[src]")
            ?.absUrl("src")
            ?.takeIf(String::isNotBlank)
            // Only a page that shows a signed-out visitor ends the session; any other page is a load failure.
            ?: throw if (sessionEvidence.showsSignedOutVisitor(html, origin)) {
                FavoritesAuthenticationRequiredException()
            } else {
                IOException("Favorites list was not found")
            }
        val uri = runCatching { URI(source) }.getOrNull()
            ?: throw IOException("Favorites list URL was invalid")
        requireSameOrigin(uri, originUri)
        if (uri.path != LIST_PATH || queryValue(uri.rawQuery, "user2view")?.matches(NUMERIC_ID) != true) {
            throw IOException("Favorites list URL was not recognized")
        }
        return uri.toASCIIString()
    }

    /** Whether a page answers as it would to a visitor, which is how an ended session shows itself. */
    fun showsSignedOutVisitor(html: String, origin: String): Boolean = sessionEvidence.showsSignedOutVisitor(html, origin)

    /** The member number a list address from [parseListUrl] belongs to. */
    fun listMemberNumber(listUrl: String): String? = queryValue(URI(listUrl).rawQuery, "user2view")

    fun parseTracks(html: String, origin: String): List<FavoriteTrack> {
        val originUri = trustedOrigin(origin)
        val document = Jsoup.parse(html, origin)
        return document.select("tr").asSequence()
            .mapNotNull { row ->
                val cells = row.children().filter { it.tagName() == "td" }
                val position = cells.getOrNull(0)?.text()?.trim()?.toIntOrNull() ?: return@mapNotNull null
                if (cells.size < 8) return@mapNotNull null
                // Another member's list carries one more cell after the request button, saying whether the track
                // is also in the listener's own favorites.
                val shift = if (cells.size > 8 && cells[2].children().none { it.tagName() == "span" }) 1 else 0

                val titleParts = cells[2 + shift].children().filter { it.tagName() == "span" }.map { it.text().trim() }
                val artistParts = cells[3 + shift].children().filter { it.tagName() == "span" }.map { it.text().trim() }
                val title = titleParts.getOrNull(0)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val album = titleParts.getOrNull(1).orEmpty()
                val artist = artistParts.getOrNull(0).orEmpty()
                val genre = artistParts.getOrNull(1)?.takeIf(String::isNotBlank)
                val requestCell = cells[1]
                val requestTrack = requestCell.selectFirst("a[href]")?.absUrl("href")
                    ?.let { parseRequestTrack(it, originUri, title, album, artist, cells[5 + shift].text().trim()) }
                val availability = if (requestTrack == null) {
                    requestCell.selectFirst("img[src*=requestbutton]")?.let { image ->
                        image.attr("title").ifBlank { image.attr("alt") }.trim().takeIf(String::isNotBlank)
                    }
                } else {
                    null
                }
                FavoriteTrack(
                    position = position,
                    title = title,
                    album = album,
                    artist = artist,
                    genre = genre,
                    year = cells[4 + shift].text().trim().takeIf(String::isNotBlank),
                    duration = cells[5 + shift].text().trim().takeIf(String::isNotBlank),
                    requestTrack = requestTrack,
                    availabilityMessage = availability,
                    availability = requestTrack?.availability
                        ?: classifyStationRequestAvailability(availability),
                    albumId = requestTrack?.albumId ?: rowAlbumId(row),
                    songId = requestTrack?.songId ?: rowSongId(row),
                )
            }
            .take(MAX_TRACKS)
            .toList()
    }

    /** Every row of the member's own list opens its track information with the station's song number. */
    private fun rowSongId(row: org.jsoup.nodes.Element): String? = row.select("[onclick]").firstNotNullOfOrNull { element ->
        VIEW_INFO_TRACK.find(element.attr("onclick"))?.groupValues?.get(1)
    }

    /** A row that cannot be requested still names its album in the links beside the track. */
    private fun rowAlbumId(row: org.jsoup.nodes.Element): String? = row.select("a[href]").firstNotNullOfOrNull { link ->
        val href = link.attr("href")
        (ALBUM_QUERY.find(href) ?: ALBUM_STORE_PATH.find(href))?.groupValues?.get(1)?.takeIf { it.matches(SAFE_ALBUM_ID) }
    }

    private fun parseRequestTrack(
        url: String,
        origin: URI,
        title: String,
        album: String,
        artist: String,
        duration: String,
    ): RequestableTrack? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!isSameOrigin(uri, origin) || uri.path != "/modules.php") return null
        if (queryValue(uri.rawQuery, "name") != "Req") return null
        val albumId = queryValue(uri.rawQuery, "asin")?.takeIf { it.matches(SAFE_ALBUM_ID) } ?: return null
        val songId = queryValue(uri.rawQuery, "songID")?.takeIf { it.matches(NUMERIC_ID) } ?: return null
        return RequestableTrack(
            albumId = albumId,
            songId = songId,
            title = title,
            artist = artist.takeIf(String::isNotBlank),
            duration = duration.takeIf(String::isNotBlank),
            eligible = true,
            albumTitle = album.takeIf(String::isNotBlank),
            availability = TrackRequestAvailability.available(),
        )
    }

    private fun trustedOrigin(origin: String): URI = URI(origin).also {
        require(it.scheme == "https" && it.path == "/")
    }

    private fun requireSameOrigin(uri: URI, origin: URI) {
        if (!isSameOrigin(uri, origin)) throw IOException("Untrusted favorites destination")
    }

    private fun isSameOrigin(uri: URI, origin: URI): Boolean =
        uri.scheme == "https" && uri.userInfo == null &&
            uri.host.equals(origin.host, true) && uri.port == origin.port

    private fun queryValue(query: String?, name: String): String? = query
        ?.split('&')
        ?.mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
        ?.firstOrNull { decode(it[0]) == name }
        ?.let { decode(it[1]) }

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val LIST_PATH = "/modules/Favorites/thelist.php"
        const val RANKED_PATH = "/modules/Your_Profile/favsTabAJAX.php"
        const val MAX_RANKED_PER_PAGE = 200
        val YEAR = Regex("[0-9]{4}")
        val VIEW_INFO_TRACK = Regex("ViewInfoTrack\\(\\s*[0-9]{1,10}\\s*,\\s*([0-9]{1,10})\\s*\\)")
        const val MAX_TRACKS = 5_000
        val NUMERIC_ID = Regex("^[0-9]{1,10}$")
        val SAFE_ALBUM_ID = Regex("^[A-Za-z0-9_.-]{1,64}$")
        val ALBUM_QUERY = Regex("[?&]asin=([^&#]+)")
        val ALBUM_STORE_PATH = Regex("/dp/ASIN/([^/?#]+)")
    }
}

internal class FavoritesAuthenticationRequiredException : IOException("Station sign-in is required")
