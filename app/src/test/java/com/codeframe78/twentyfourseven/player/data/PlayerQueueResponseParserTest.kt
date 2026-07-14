package com.codeframe78.twentyfourseven.player.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerQueueResponseParserTest {
    private val parser = PlayerQueueResponseParser()

    @Test
    fun `parses public player rows without guessing title fields`() {
        val queue = row("4:05", "https://adagio.fm/covers/queue.jpg", "Artist - Raw title", "Album")
        val history = row("2:17", "/covers/history.jpg", "Played raw title", "Played album")

        val result = parser.parse(response(queue, history), "https://adagio.fm/")

        assertEquals(1, result.upcoming.single().position)
        assertEquals("Album", result.upcoming.single().displayTitle)
        assertEquals("Artist - Raw title", result.upcoming.single().artistName)
        assertNull(result.upcoming.single().albumTitle)
        assertNull(result.upcoming.single().durationLabel)
        assertEquals("https://adagio.fm/covers/queue.jpg", result.upcoming.single().artworkUrl)
        assertEquals("https://adagio.fm/covers/history.jpg", result.recentlyPlayed.single().artworkUrl)
    }

    @Test
    fun `drops malformed rows and artwork outside the station domain`() {
        val malformed = "<tr><td>marker</td><td></td><td><strong>Artist only</strong></td></tr>"
        val externalArtwork = row("3:00", "https://example.com/cover.jpg", "Raw title", "Album")

        val result = parser.parse(response(malformed + externalArtwork, ""), "https://death.fm/")

        assertEquals(1, result.upcoming.size)
        assertEquals("Album", result.upcoming.single().displayTitle)
        assertEquals("Raw title", result.upcoming.single().artistName)
        assertNull(result.upcoming.single().artworkUrl)
        assertEquals(emptyList<Any>(), result.recentlyPlayed)
    }

    private fun response(queue: String, history: String) = JSONObject()
        .put("queue_html", queue)
        .put("played_html", history)
        .toString()

    private fun row(duration: String, artwork: String, title: String, album: String) = """
        <tr>
          <td>$duration</td>
          <td><img src="$artwork" onerror="ignored()"></td>
          <td><strong>$title</strong><br><span>$album</span></td>
        </tr>
    """.trimIndent()
}
