package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchResult
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
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
            if (albumLinks.size < 2) return@mapNotNull null
            val albumIds = albumLinks.mapNotNull { queryValue(URI(it.absUrl("href")), "asin") }.distinct()
            if (albumIds.size != 1) return@mapNotNull null
            val albumId = albumIds.single()
            val title = albumLinks[0].text().clean()
            val album = albumLinks[1].text().clean()
            if (title.isBlank() || album.isBlank()) return@mapNotNull null
            val year = row.select("td").map { it.text().clean() }.lastOrNull { it.matches(YEAR) }
            RequestSearchResult(albumId, title, album, year)
        }.distinctBy { listOf(it.albumId, it.trackTitle, it.albumTitle) }.take(MAX_RESULTS)
    }

    fun parseAlbum(html: String, origin: String, expectedAlbumId: String): RequestAlbum {
        val expected = URI(origin)
        val document = Jsoup.parse(html, origin)
        val albumTitle = document.selectFirst("meta[property=\"og:title\"]")?.attr("content")?.clean()
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

            val cells = row.select("td")
            val titleCell = cells.firstOrNull { cell ->
                cell.select("a[href*=postartistsearch]").isNotEmpty()
            } ?: cells.firstOrNull { cell ->
                cell.ownText().clean().isNotBlank() && cell.text().clean().length > 2 &&
                    !cell.text().clean().matches(DURATION) && !cell.text().clean().matches(NUMERIC_ID)
            } ?: return@mapNotNull null
            val title = titleCell.ownText().clean().ifBlank {
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
            )
        }.distinctBy { it.songId.ifBlank { "${it.title}|${it.artist}" } }.take(MAX_TRACKS)
        return RequestAlbum(albumTitle, tracks)
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
        const val MAX_RESULTS = 100
        const val MAX_TRACKS = 250
    }
}
