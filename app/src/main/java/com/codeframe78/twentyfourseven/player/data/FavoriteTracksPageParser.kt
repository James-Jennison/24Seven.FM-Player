package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.classifyStationRequestAvailability
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal class FavoriteTracksPageParser {
    fun parseListUrl(html: String, origin: String): String {
        val originUri = trustedOrigin(origin)
        val source = Jsoup.parse(html, origin)
            .selectFirst("iframe#thelist[src]")
            ?.absUrl("src")
            ?.takeIf(String::isNotBlank)
            ?: throw FavoritesAuthenticationRequiredException()
        val uri = runCatching { URI(source) }.getOrNull()
            ?: throw IOException("Favorites list URL was invalid")
        requireSameOrigin(uri, originUri)
        if (uri.path != LIST_PATH || queryValue(uri.rawQuery, "user2view")?.matches(NUMERIC_ID) != true) {
            throw IOException("Favorites list URL was not recognized")
        }
        return uri.toASCIIString()
    }

    fun parseTracks(html: String, origin: String): List<FavoriteTrack> {
        val originUri = trustedOrigin(origin)
        val document = Jsoup.parse(html, origin)
        return document.select("tr").asSequence()
            .mapNotNull { row ->
                val cells = row.children().filter { it.tagName() == "td" }
                val position = cells.getOrNull(0)?.text()?.trim()?.toIntOrNull() ?: return@mapNotNull null
                if (cells.size < 8) return@mapNotNull null

                val titleParts = cells[2].children().filter { it.tagName() == "span" }.map { it.text().trim() }
                val artistParts = cells[3].children().filter { it.tagName() == "span" }.map { it.text().trim() }
                val title = titleParts.getOrNull(0)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val album = titleParts.getOrNull(1).orEmpty()
                val artist = artistParts.getOrNull(0).orEmpty()
                val genre = artistParts.getOrNull(1)?.takeIf(String::isNotBlank)
                val requestCell = cells[1]
                val requestTrack = requestCell.selectFirst("a[href]")?.absUrl("href")
                    ?.let { parseRequestTrack(it, originUri, title, album, artist, cells[5].text().trim()) }
                val availability = if (requestTrack == null) {
                    requestCell.selectFirst("img")?.let { image ->
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
                    year = cells[4].text().trim().takeIf(String::isNotBlank),
                    duration = cells[5].text().trim().takeIf(String::isNotBlank),
                    requestTrack = requestTrack,
                    availabilityMessage = availability,
                    availability = requestTrack?.availability
                        ?: classifyStationRequestAvailability(availability),
                )
            }
            .take(MAX_TRACKS)
            .toList()
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
        const val MAX_TRACKS = 5_000
        val NUMERIC_ID = Regex("^[0-9]{1,10}$")
        val SAFE_ALBUM_ID = Regex("^[A-Za-z0-9_.-]{1,64}$")
    }
}

internal class FavoritesAuthenticationRequiredException : IOException("Station sign-in is required")
