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
                    artistName = track.artistName,
                    albumTitle = track.albumTitle,
                    durationLabel = track.durationLabel,
                    artworkUrl = track.artworkUrl,
                )
            },
            recentlyPlayed = rows(response.getString("played_html"), baseUrl).mapNotNull { row ->
                val track = parseRow(row, baseUrl) ?: return@mapNotNull null
                HistoryTrack(
                    displayTitle = track.displayTitle,
                    artistName = track.artistName,
                    albumTitle = track.albumTitle,
                    durationLabel = track.durationLabel,
                    artworkUrl = track.artworkUrl,
                )
            },
        )
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
            artistName = artistName,
            albumTitle = null,
            durationLabel = null,
            artworkUrl = cells[1].selectFirst("img[src]")
                ?.absUrl("src")
                ?.takeIf { isSafeWebUrl(it, baseUrl) },
        )
    }

    private fun isSafeWebUrl(url: String, baseUrl: String): Boolean = runCatching {
        val uri = URI(url)
        val host = uri.host?.lowercase() ?: return@runCatching false
        val baseHost = URI(baseUrl).host?.lowercase() ?: return@runCatching false
        uri.scheme.lowercase() in setOf("http", "https") &&
            (host == baseHost || host.endsWith(".$baseHost"))
    }.getOrDefault(false)

    private data class ParsedTrack(
        val displayTitle: String,
        val artistName: String?,
        val albumTitle: String?,
        val durationLabel: String?,
        val artworkUrl: String?,
    )
}
