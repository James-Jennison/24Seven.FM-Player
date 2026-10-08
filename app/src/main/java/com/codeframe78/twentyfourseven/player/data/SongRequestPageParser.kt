package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchResult
import com.codeframe78.twentyfourseven.player.domain.RequestSearchTarget
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.classifyStationRequestAvailability
import org.jsoup.Jsoup
import java.net.URI

internal data class RequestAlbum(val title: String?, val tracks: List<RequestableTrack>)

internal class SongRequestPageParser {
    fun parseSearch(html: String, origin: String): List<RequestSearchResult> {
        val expected = URI(origin)
        return Jsoup.parse(html, origin).select("tr").mapNotNull { row ->
            val albumLinks = row.select("a[href]").filter { link ->
                val uri = runCatching { URI(link.absUrl("href")) }.getOrNull() ?: return@filter false
                uri.scheme == "https" && uri.host.equals(expected.host, true) &&
                    queryValue(uri, "name") == "Album" && !queryValue(uri, "asin").isNullOrBlank()
            }
            val albumIds = albumLinks.mapNotNull { queryValue(URI(it.absUrl("href")), "asin") }.distinct()
            if (albumIds.size == 1 && albumLinks.isNotEmpty()) {
                val albumId = albumIds.single()
                val firstTitle = albumLinks.first().text().clean()
                val albumTitle = albumLinks.getOrNull(1)?.text()?.clean()
                if (firstTitle.isBlank() || albumLinks.size > 1 && albumTitle.isNullOrBlank()) {
                    return@mapNotNull null
                }
                val year = row.select("td").map { it.text().clean() }.lastOrNull { it.matches(YEAR) }
                return@mapNotNull RequestSearchResult(
                    target = RequestSearchTarget.Album(albumId),
                    title = firstTitle,
                    subtitle = albumTitle,
                    year = year,
                )
            }

            val artistLink = row.select("a[href]").firstOrNull { link ->
                val uri = runCatching { URI(link.absUrl("href")) }.getOrNull() ?: return@firstOrNull false
                uri.scheme == "https" && uri.host.equals(expected.host, true) &&
                    queryValue(uri, "name") == "Requests" &&
                    queryValue(uri, "postartistsearch").equals("true", true) &&
                    !queryValue(uri, "artist").isNullOrBlank()
            } ?: return@mapNotNull null
            val artistUri = URI(artistLink.absUrl("href"))
            val artistName = queryValue(artistUri, "artist")?.clean()?.takeIf { it.length <= MAX_ARTIST_NAME_LENGTH }
                ?: return@mapNotNull null
            val displayName = artistLink.text().clean()
            if (displayName.isBlank()) return@mapNotNull null
            val genre = row.select("td").map { it.text().clean() }
                .lastOrNull { it.isNotBlank() && it != displayName }
            RequestSearchResult(
                target = RequestSearchTarget.Artist(artistName),
                title = displayName,
                subtitle = genre,
            )
        }.distinctBy { listOf(it.target, it.title, it.subtitle) }.take(MAX_RESULTS)
    }

    fun parseAlbum(html: String, origin: String, expectedAlbumId: String): RequestAlbum {
        val expected = URI(origin)
        val document = Jsoup.parse(html, origin)
        // The stations end the shared title with their own name, as in "Album - Artist | Station".
        val albumTitle = document.selectFirst("meta[property=\"og:title\"]")?.attr("content")
            ?.substringBeforeLast(" | ")?.clean()?.takeIf(String::isNotBlank)
            ?: document.title().substringAfter(" - ", "").clean().ifBlank { null }
        val tracks = document.select("tr").mapNotNull { row ->
            val requestImage = row.selectFirst("img[src*=requestbutton]") ?: return@mapNotNull null
            val requestLink = requestImage.closest("a[href]")
            val requestUri = requestLink?.absUrl("href")?.let { runCatching { URI(it) }.getOrNull() }
            val songId = requestUri?.takeIf {
                it.scheme == "https" && it.host.equals(expected.host, true) &&
                    queryValue(it, "name") == "Req" && queryValue(it, "asin") == expectedAlbumId
            }?.let { queryValue(it, "songID") }
            val eligible = requestImage.attr("src").contains("requestbutton_request") &&
                songId?.matches(NUMERIC_ID) == true
            val stationMessage = requestImage.attr("title").ifBlank { requestImage.attr("alt") }
                .clean().takeIf(String::isNotBlank)

            val cells = row.select("td")
            val titleCell = cells.firstOrNull { cell ->
                cell.select("a[href*=postartistsearch]").isNotEmpty()
            } ?: cells.firstOrNull { cell ->
                cell.ownText().clean().isNotBlank() && cell.text().clean().length > 2 &&
                    !cell.text().clean().matches(DURATION) && !cell.text().clean().matches(NUMERIC_ID)
            } ?: return@mapNotNull null
            // The stations now mark the track title up as a named element of the cell; older pages left it as the
            // cell's own text.
            val title = titleCell.children()
                .firstOrNull { it.attr("itemprop") == "name" }
                ?.text()?.clean()?.takeIf(String::isNotBlank)
                ?: titleCell.ownText().clean().ifBlank {
                    titleCell.textNodes().joinToString(" ") { it.text() }.clean()
                }
            if (title.isBlank()) return@mapNotNull null
            val artist = titleCell.select("a[href*=postartistsearch]").joinToString(", ") { it.text().clean() }
                .ifBlank { null }
            val duration = cells.map { it.text().clean() }.firstOrNull { it.matches(DURATION) }
            RequestableTrack(
                albumId = expectedAlbumId,
                songId = songId.orEmpty(),
                title = title,
                artist = artist,
                duration = duration,
                eligible = eligible,
                albumTitle = albumTitle,
                availability = if (eligible) {
                    TrackRequestAvailability.available()
                } else {
                    classifyStationRequestAvailability(stationMessage)
                },
            )
        }.distinctBy { it.songId.ifBlank { "${it.title}|${it.artist}" } }.take(MAX_TRACKS)
        return RequestAlbum(albumTitle, tracks)
    }

    fun parseSuggestion(html: String, origin: String): RequestAlbum {
        val expected = URI(origin)
        val suggestion = Jsoup.parse(html, origin).select("tr").firstNotNullOfOrNull { row ->
            val requestImage = row.selectFirst("img[src*=requestbutton_request]") ?: return@firstNotNullOfOrNull null
            val requestUri = requestImage.closest("a[href]")?.absUrl("href")
                ?.let { runCatching { URI(it) }.getOrNull() }
                ?.takeIf {
                    it.scheme == "https" && it.host.equals(expected.host, true) &&
                        queryValue(it, "name") == "Req"
                } ?: return@firstNotNullOfOrNull null
            val albumId = queryValue(requestUri, "asin")?.takeIf { it.isNotBlank() }
                ?: return@firstNotNullOfOrNull null
            val songId = queryValue(requestUri, "songID")?.takeIf { it.matches(NUMERIC_ID) }
                ?: return@firstNotNullOfOrNull null
            val albumUri = row.select("a[href]").mapNotNull { link ->
                runCatching { URI(link.absUrl("href")) }.getOrNull()
            }.firstOrNull {
                it.scheme == "https" && it.host.equals(expected.host, true) &&
                    queryValue(it, "name") == "Album" && queryValue(it, "asin") == albumId
            } ?: return@firstNotNullOfOrNull null
            if (queryValue(albumUri, "asin") != albumId) return@firstNotNullOfOrNull null

            val details = row.selectFirst("td:has(b)") ?: return@firstNotNullOfOrNull null
            val descriptor = details.selectFirst("b")?.text()?.clean().orEmpty()
            val trackText = details.ownText().clean()
            val duration = DURATION.find(trackText)?.value
            val title = trackText
                .replace(LEADING_TRACK_NUMBER, "")
                .replace(TRAILING_DURATION, "")
                .clean()
            if (title.isBlank()) return@firstNotNullOfOrNull null
            val albumTitle = descriptor.substringBeforeLast(" - ", descriptor).clean().ifBlank { null }
            val artist = descriptor.substringAfterLast(" - ", "").clean().ifBlank { null }
            RequestAlbum(
                albumTitle,
                listOf(
                    RequestableTrack(
                        albumId = albumId,
                        songId = songId,
                        title = title,
                        artist = artist,
                        duration = duration,
                        eligible = true,
                        albumTitle = albumTitle,
                        availability = TrackRequestAvailability.available(),
                    ),
                ),
            )
        }
        return suggestion ?: RequestAlbum(null, emptyList())
    }

    private fun queryValue(uri: URI, name: String): String? = uri.rawQuery.orEmpty().split('&')
        .mapNotNull { pair -> pair.split('=', limit = 2).takeIf { it.size == 2 } }
        .firstOrNull { it[0].equals(name, true) }
        ?.get(1)
        ?.let(::decodeQueryValue)

    @Suppress("DEPRECATION")
    private fun decodeQueryValue(value: String): String = java.net.URLDecoder.decode(value, "UTF-8")

    private fun String.clean() = replace(Regex("\\s+"), " ").trim()

    private companion object {
        val YEAR = Regex("(?:19|20)\\d{2}")
        val DURATION = Regex("\\d{1,2}:\\d{2}")
        val NUMERIC_ID = Regex("\\d+")
        val LEADING_TRACK_NUMBER = Regex("^\\d+\\.\\s*")
        val TRAILING_DURATION = Regex("\\s*\\(\\d{1,2}:\\d{2}\\)\\s*$")
        const val MAX_RESULTS = 100
        const val MAX_TRACKS = 250
        const val MAX_ARTIST_NAME_LENGTH = 200
    }
}
