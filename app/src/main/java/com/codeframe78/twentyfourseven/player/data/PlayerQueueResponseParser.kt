package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.HistoryTrack
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI

internal data class QueuePayload(
    val upcoming: List<QueueTrack>,
    val recentlyPlayed: List<HistoryTrack>,
)

internal class PlayerQueueResponseParser {
    fun parse(json: String, baseUrl: String): QueuePayload {
        val response = JSONObject(json)
        return QueuePayload(
            upcoming = rows(response.getString("queue_html"), baseUrl).mapIndexedNotNull { index, row ->
                val track = parseRow(row, baseUrl) ?: return@mapIndexedNotNull null
                QueueTrack(
                    position = index + 1,
                    displayTitle = track.displayTitle,
                    songId = track.songId,
                    albumId = track.albumId,
                    artistName = track.artistName,
                    albumTitle = track.albumTitle,
                    durationLabel = track.durationLabel,
                    artworkUrl = track.artworkUrl,
                    requesterName = track.requesterName,
                    requestMessage = track.requestMessage,
                )
            },
            recentlyPlayed = rows(response.getString("played_html"), baseUrl).mapNotNull { row ->
                val track = parseRow(row, baseUrl) ?: return@mapNotNull null
                HistoryTrack(
                    displayTitle = track.displayTitle,
                    songId = track.songId,
                    albumId = track.albumId,
                    artistName = track.artistName,
                    albumTitle = track.albumTitle,
                    durationLabel = track.durationLabel,
                    artworkUrl = track.artworkUrl,
                    requesterName = track.requesterName,
                    requestMessage = track.requestMessage,
                )
            },
        )
    }

    fun parseExtended(html: String, baseUrl: String, maxTracks: Int = DEFAULT_VISIBLE_TRACKS): QueuePayload {
        require(maxTracks in 1..MAX_EXTENDED_TRACKS)
        val document = Jsoup.parse(html, baseUrl)
        val queueTable = document.select("table").firstOrNull { table ->
            directRows(table).any { row ->
                row.children().any { cell ->
                    cell.normalName() == "th" && cell.text().trim().equals("Queue", ignoreCase = true)
                }
            }
        }
        val playedTable = document.select("table").firstOrNull { table ->
            directRows(table).any { row ->
                row.children().any { cell ->
                    cell.normalName() == "th" && cell.text().trim().equals("Played", ignoreCase = true)
                }
            }
        }
        return QueuePayload(
            upcoming = queueTable?.let(::directRows).orEmpty().mapNotNull { row ->
                val track = parseExtendedRow(row, baseUrl) ?: return@mapNotNull null
                QueueTrack(
                    position = track.position,
                    displayTitle = track.displayTitle,
                    songId = track.songId,
                    albumId = track.albumId,
                    artistName = track.artistName,
                    albumTitle = track.albumTitle,
                    durationLabel = track.durationLabel,
                    artworkUrl = track.artworkUrl,
                    requesterName = track.requesterName,
                    requestMessage = track.requestMessage,
                )
            }.take(maxTracks),
            recentlyPlayed = playedTable?.let(::directRows).orEmpty().mapNotNull { row ->
                val track = parseExtendedRow(row, baseUrl) ?: return@mapNotNull null
                HistoryTrack(
                    displayTitle = track.displayTitle,
                    songId = track.songId,
                    albumId = track.albumId,
                    artistName = track.artistName,
                    albumTitle = track.albumTitle,
                    durationLabel = track.durationLabel,
                    artworkUrl = track.artworkUrl,
                    requesterName = track.requesterName,
                    requestMessage = track.requestMessage,
                )
            }.take(maxTracks),
        )
    }

    private fun directRows(table: Element): List<Element> = table.children().flatMap { child ->
        when (child.normalName()) {
            "tr" -> listOf(child)
            "tbody", "thead", "tfoot" -> child.children().filter { it.normalName() == "tr" }
            else -> emptyList()
        }
    }

    private fun rows(html: String, baseUrl: String): List<Element> =
        Jsoup.parse("<table><tbody>$html</tbody></table>", baseUrl).select("tr")

    private fun parseRow(row: Element, baseUrl: String): ParsedTrack? {
        val cells = row.select("td")
        if (cells.size < 3) return null
        val artistName = cells[2].selectFirst("strong")?.text()?.trim()?.takeIf(String::isNotEmpty)
        val displayTitle = cells[2].selectFirst("span")?.text()?.trim().orEmpty()
        if (displayTitle.isEmpty()) return null
        return ParsedTrack(
            displayTitle = displayTitle,
            songId = requestIdentifier(cells[2], "songID"),
            albumId = requestIdentifier(cells[2], "asin"),
            artistName = artistName,
            albumTitle = null,
            durationLabel = null,
            artworkUrl = cells[1].selectFirst("img[src]")
                ?.absUrl("src")
                ?.takeIf { isSafeWebUrl(it, baseUrl) },
        )
    }

    private fun parseExtendedRow(row: Element, baseUrl: String): ExtendedTrack? {
        val cells = row.select("td")
        if (cells.size != 3) return null
        val position = cells[0].selectFirst("b, .glowing-rank")?.text()?.trim()?.toIntOrNull() ?: return null
        val duration = DURATION.find(cells[0].text())?.value ?: return null
        val details = cells[2]
        val titleSpan = details.select("span")
            .firstOrNull { !it.hasClass("req-text") && it.text().trim().isNotEmpty() }
        val usesCurrentStationMarkup = titleSpan != null && details.selectFirst("i") == null
        val (album, artist, title) = if (usesCurrentStationMarkup) {
            val parsedArtist = details.selectFirst("b")?.text()?.trim()?.takeIf(String::isNotEmpty) ?: return null
            val parsedAlbum = details.clone().apply { select("b, span, br").remove() }
                .text()
                .trim()
                .removePrefix("-")
                .trim()
                .takeIf(String::isNotEmpty) ?: return null
            Triple(parsedAlbum, parsedArtist, titleSpan.text().trim())
        } else {
            val parsedAlbum = details.selectFirst("b")?.text()?.trim()?.takeIf(String::isNotEmpty) ?: return null
            val parsedArtist = details.selectFirst("i")?.text()?.trim()?.takeIf(String::isNotEmpty)
            val parsedTitle = details.clone().apply { select("b, i, span").remove() }
                .text()
                .trim()
                .removePrefix("-")
                .trim()
                .takeIf(String::isNotEmpty) ?: return null
            Triple(parsedAlbum, parsedArtist, parsedTitle)
        }
        val attribution = parseRequestAttribution(details)
        return ExtendedTrack(
            position = position,
            displayTitle = title,
            songId = requestIdentifier(details, "songID") ?: requestIdentifier(cells[1], "songID"),
            albumId = requestIdentifier(details, "asin") ?: requestIdentifier(cells[1], "asin"),
            artistName = artist,
            albumTitle = album,
            durationLabel = duration,
            artworkUrl = cells[1].selectFirst("img[src]")
                ?.absUrl("src")
                ?.takeIf { isSafeWebUrl(it, baseUrl) },
            requesterName = attribution?.requesterName,
            requestMessage = attribution?.message,
        )
    }

    private fun parseRequestAttribution(details: Element): RequestAttribution? {
        val label = details.selectFirst("span.req-text") ?: return null
        val requesterLink = label.selectFirst("a[href*=username]")
        if (!REQUESTER_PREFIX.containsMatchIn(label.text()) && requesterLink == null) return null
        val requesterName = requesterLink?.text()?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: label.clone().apply { select("i, img").remove() }.text()
                .replaceFirst(REQUESTER_PREFIX, "")
                .trim()
                .removeSuffix("-")
                .trim()
                .takeIf(String::isNotEmpty)
            ?: return null
        val message = label.selectFirst("i")?.text()?.trim()?.takeIf(String::isNotEmpty)
        return RequestAttribution(
            requesterName = requesterName.take(MAX_REQUESTER_CHARACTERS),
            message = message?.take(MAX_REQUEST_MESSAGE_CHARACTERS),
        )
    }

    private fun isSafeWebUrl(url: String, baseUrl: String): Boolean = runCatching {
        val uri = URI(url)
        val host = uri.host?.lowercase() ?: return@runCatching false
        val baseHost = URI(baseUrl).host?.lowercase() ?: return@runCatching false
        uri.scheme.lowercase() in setOf("http", "https") &&
            (host == baseHost || host.endsWith(".$baseHost"))
    }.getOrDefault(false)

    private fun requestIdentifier(details: Element, name: String): String? = details.select("a[href]")
        .asSequence()
        .mapNotNull { link -> runCatching { URI(link.absUrl("href")) }.getOrNull() }
        .flatMap { uri -> uri.rawQuery.orEmpty().split('&').asSequence() }
        .mapNotNull { pair -> pair.split('=', limit = 2).takeIf { it.size == 2 } }
        .firstOrNull { it[0].equals(name, ignoreCase = true) }
        ?.get(1)
        ?.takeIf { it.matches(SAFE_IDENTIFIER) }

    private data class ParsedTrack(
        val displayTitle: String,
        val songId: String?,
        val albumId: String?,
        val artistName: String?,
        val albumTitle: String?,
        val durationLabel: String?,
        val artworkUrl: String?,
        val requesterName: String? = null,
        val requestMessage: String? = null,
    )

    private data class ExtendedTrack(
        val position: Int,
        val displayTitle: String,
        val songId: String?,
        val albumId: String?,
        val artistName: String?,
        val albumTitle: String,
        val durationLabel: String,
        val artworkUrl: String?,
        val requesterName: String?,
        val requestMessage: String?,
    )

    private data class RequestAttribution(val requesterName: String, val message: String?)

    private companion object {
        const val DEFAULT_VISIBLE_TRACKS = 30
        const val MAX_EXTENDED_TRACKS = 500
        val DURATION = Regex("\\b\\d{1,2}:\\d{2}\\b")
        val REQUESTER_PREFIX = Regex("^\\s*Request\\s+By:\\s*", RegexOption.IGNORE_CASE)
        const val MAX_REQUESTER_CHARACTERS = 80
        const val MAX_REQUEST_MESSAGE_CHARACTERS = 240
        val SAFE_IDENTIFIER = Regex("[A-Za-z0-9_.-]{1,64}")
    }
}
