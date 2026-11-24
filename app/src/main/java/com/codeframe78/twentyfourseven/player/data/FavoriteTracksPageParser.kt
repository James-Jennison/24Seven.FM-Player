package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.classifyStationRequestAvailability
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** One page of the member's ranked favorites and, when the station offers one, the number of the page after it. */
internal data class RankedFavoritesPage(val tracks: List<FavoriteTrack>, val nextPage: Int?)

/**
 * One page of a member's full favorites list. The stations list 50 tracks a page since the November 2026 redesign;
 * [nextPage] is the page after this one when the station links to it, and [totalTracks] is the count the station
 * states for the whole list, when it states one.
 */
internal data class FavoriteTracksPage(val tracks: List<FavoriteTrack>, val nextPage: Int?, val totalTracks: Int?)

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
            .filter { row -> row.selectFirst("td.td02") != null }
            .mapNotNull { row ->
                val details = row.selectFirst("td.td02") ?: return@mapNotNull null
                // The redesigned feed sets the title in <strong>; the earlier one used a <span>.
                val title = (details.selectFirst("strong") ?: details.selectFirst("span"))?.text()?.trim()
                    ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val artist = details.selectFirst("b")?.text()?.trim().orEmpty()
                // The second line reads "Album by Artist"; the album is what comes before the artist's name.
                val lines = details.html().split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE))
                val albumLine = lines.getOrNull(1)?.let { Jsoup.parse(it).text().trim() }.orEmpty()
                val album = albumLine.removeSuffix(artist).trim().removeSuffix("by").trim()
                    .ifBlank { row.selectFirst("td a[href*=asin=] img[title]")?.attr("title")?.trim().orEmpty() }
                val facts = lines.getOrNull(2)?.let { Jsoup.parse(it).text().trim() }.orEmpty().split(',', limit = 2)
                val requestCell = row.selectFirst("a[href*=name=Req&]")
                val requestTrack = requestCell?.absUrl("href")?.let { parseRequestTrack(it, originUri, title, album, artist, "") }
                val availability = if (requestTrack == null) {
                    row.selectFirst("img[src*=requestbutton]")?.let { image ->
                        image.attr("title").ifBlank { image.attr("alt") }.trim().takeIf(String::isNotBlank)
                    }
                } else {
                    null
                }
                // The earlier feed carried the station's own move/remove form on every row. The redesigned feed has
                // none, so the song number is known only from the request link, when the track can be requested.
                val songId = row.selectFirst("form.favorite-controls-form input[name=songid]")?.attr("value")?.trim()
                    ?.takeIf { it.matches(NUMERIC_ID) }
                    ?: requestTrack?.songId
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

    fun parseTracks(html: String, origin: String): List<FavoriteTrack> = parseTracksPage(html, origin).tracks

    /**
     * One page of a favorites list. Reads the redesigned rows (rank, controls, cover, request, info, and last played,
     * with one more cell on another member's list) and, for a station that still serves it, the earlier eight-cell
     * layout.
     */
    fun parseTracksPage(html: String, origin: String): FavoriteTracksPage {
        val originUri = trustedOrigin(origin)
        val document = Jsoup.parse(html, origin)
        val tracks = document.select("tr").asSequence()
            .mapNotNull { row ->
                if (row.selectFirst("td.favorite-info") != null) {
                    parseRedesignedRow(row, originUri)
                } else {
                    parseLegacyRow(row, originUri)
                }
            }
            .take(MAX_TRACKS)
            .toList()
        return FavoriteTracksPage(tracks, nextListPage(document, originUri), totalTracks(document))
    }

    private fun parseRedesignedRow(row: Element, originUri: URI): FavoriteTrack? {
        val position = row.selectFirst("span.favorite-rank")?.text()?.trim()?.toIntOrNull() ?: return null
        val info = row.selectFirst("td.favorite-info") ?: return null
        val title = info.selectFirst("strong")?.text()?.trim()?.takeIf(String::isNotBlank) ?: return null
        val artistLink = info.children().firstOrNull { it.tagName() == "a" }
        val artist = artistLink?.text()?.trim().orEmpty()
        // The cell reads "<strong>Title</strong>Album by <a>Artist</a><small>Year, Genre</small>".
        val album = info.textNodes().joinToString("") { it.text() }.trim()
            .let { text -> if (artist.isNotEmpty()) text.removeSuffix("by").trim() else text }
        val facts = info.selectFirst("small.favorite-meta")?.text()?.trim().orEmpty().split(',', limit = 2)
        val duration = row.selectFirst("td")?.select("span")
            ?.firstOrNull { !it.hasClass("favorite-rank") }
            ?.text()?.trim()?.takeIf { it.matches(DURATION) }
        val requestCell = row.selectFirst("td.favorite-request")
        val requestTrack = requestCell?.selectFirst("a[href]")?.absUrl("href")
            ?.let { parseRequestTrack(it, originUri, title, album, artist, duration.orEmpty()) }
        val availability = if (requestTrack == null) {
            requestCell?.selectFirst("img[src*=requestbutton]")?.let { image ->
                image.attr("title").ifBlank { image.attr("alt") }.trim().takeIf(String::isNotBlank)
            }
        } else {
            null
        }
        return FavoriteTrack(
            position = position,
            title = title,
            album = album,
            artist = artist,
            genre = facts.getOrNull(1)?.trim()?.takeIf(String::isNotBlank),
            year = facts.getOrNull(0)?.trim()?.takeIf { it.matches(YEAR) },
            duration = duration,
            requestTrack = requestTrack,
            availabilityMessage = availability,
            availability = requestTrack?.availability ?: classifyStationRequestAvailability(availability),
            albumId = requestTrack?.albumId ?: rowAlbumId(row),
            songId = requestTrack?.songId ?: rowControlSongId(row) ?: rowSongId(row),
        )
    }

    private fun parseLegacyRow(row: Element, originUri: URI): FavoriteTrack? {
        val cells = row.children().filter { it.tagName() == "td" }
        val position = cells.getOrNull(0)?.text()?.trim()?.toIntOrNull() ?: return null
        if (cells.size < 8) return null
        // Another member's list carries one more cell after the request button, saying whether the track
        // is also in the listener's own favorites.
        val shift = if (cells.size > 8 && cells[2].children().none { it.tagName() == "span" }) 1 else 0

        val titleParts = cells[2 + shift].children().filter { it.tagName() == "span" }.map { it.text().trim() }
        val artistParts = cells[3 + shift].children().filter { it.tagName() == "span" }.map { it.text().trim() }
        val title = titleParts.getOrNull(0)?.takeIf(String::isNotBlank) ?: return null
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
        return FavoriteTrack(
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

    /** The page the list's own "Next" link leads to, when it leads to another page of the same list. */
    private fun nextListPage(document: Document, originUri: URI): Int? = document.select("a[rel=next][href]")
        .asSequence()
        .mapNotNull { link -> runCatching { URI(link.absUrl("href")) }.getOrNull() }
        .filter { isSameOrigin(it, originUri) && it.path == LIST_PATH }
        .mapNotNull { queryValue(it.rawQuery, "page")?.toIntOrNull() }
        .firstOrNull { it > 1 }

    /** The count in "1–50 of 1500 tracks". */
    private fun totalTracks(document: Document): Int? = document.selectFirst(".favorite-result-count")?.text()
        ?.let { TOTAL_TRACKS.find(it)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() }

    /**
     * The song number of the track named [title] on a signed-in album page. Every track row there carries the number
     * in its favorite heart ("AddTrack(246835, ...)"), also when the track cannot be requested, and a requestable row
     * carries it in its request link as well. Null unless exactly one row has that title.
     */
    fun albumTrackSongId(html: String, title: String, albumId: String? = null): String? {
        val wanted = title.trim()
        val rows = Jsoup.parse(html).select("tr").filter { row ->
            row.select("tr").size == 1 && row.selectFirst("[itemprop=name]") != null
        }
        fun requestLinks(row: Element) = row.select("a[href*=name=Req&]").mapNotNull { link ->
            runCatching { URI(link.attr("href").substringAfter('?').let { "x:/?$it" }) }.getOrNull()
        }
        // A track row that can be requested names its album. One that names another album means the page is not
        // only the album asked for, so nothing on it is taken.
        if (albumId != null && rows.any { row -> requestLinks(row).any { queryValue(it.rawQuery, "asin") != albumId } }) return null
        // Every row that names the title counts, with or without a request button, so two tracks that share a title
        // are always seen as two and neither is taken for the other.
        val row = rows.filter { row -> row.select("[itemprop=name]").any { it.text().trim() == wanted } }
            .singleOrNull() ?: return null
        // A row is one track. More than one number in it means the row is not what this reads, so nothing is named.
        val numbers = row.select("[onclick]").mapNotNull { ADD_TRACK.find(it.attr("onclick"))?.groupValues?.get(1) } +
            requestLinks(row).mapNotNull { queryValue(it.rawQuery, "songID")?.takeIf { id -> id.matches(NUMERIC_ID) } }
        return numbers.distinct().singleOrNull()
    }

    /** The song number in the row's own move/remove controls. */
    private fun rowControlSongId(row: Element): String? = row.select("button[formaction]").firstNotNullOfOrNull { button ->
        val action = runCatching { URI(button.absUrl("formaction")) }.getOrNull() ?: return@firstNotNullOfOrNull null
        queryValue(action.rawQuery, "songid")?.takeIf { it.matches(NUMERIC_ID) }
    }

    /** Every row of the member's own list opens its track information with the station's song number. */
    private fun rowSongId(row: Element): String? = row.select("[onclick]").firstNotNullOfOrNull { element ->
        VIEW_INFO_TRACK.find(element.attr("onclick"))?.groupValues?.get(1)
    }

    /** A row that cannot be requested still names its album in the links beside the track. */
    private fun rowAlbumId(row: Element): String? = row.select("a[href]").firstNotNullOfOrNull { link ->
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
        val DURATION = Regex("[0-9]{1,3}:[0-5][0-9](:[0-5][0-9])?")
        val TOTAL_TRACKS = Regex("of\\s+([0-9][0-9,]*)\\s+tracks?", RegexOption.IGNORE_CASE)
        val ADD_TRACK = Regex("AddTrack\\(\\s*([0-9]{1,10})\\s*,")
        val VIEW_INFO_TRACK = Regex("ViewInfoTrack\\(\\s*[0-9]{1,10}\\s*,\\s*([0-9]{1,10})\\s*\\)")
        const val MAX_TRACKS = 5_000
        val NUMERIC_ID = Regex("^[0-9]{1,10}$")
        val SAFE_ALBUM_ID = Regex("^[A-Za-z0-9_.-]{1,64}$")
        val ALBUM_QUERY = Regex("[?&]asin=([^&#]+)")
        val ALBUM_STORE_PATH = Regex("/dp/ASIN/([^/?#]+)")
    }
}

internal class FavoritesAuthenticationRequiredException : IOException("Station sign-in is required")
