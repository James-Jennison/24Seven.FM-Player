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

    @Test
    fun `parses extended queue fields without including requester text`() {
        val result = parser.parseExtended(
            extendedPage(
                queue = extendedRow(1, "4:05", "/covers/queue.jpg", "Album", "Artist", "Track"),
                history = extendedRow(7, "2:17", "/covers/history.jpg", "Played album", "Played artist", "Played track"),
            ),
            "https://streamingsoundtracks.com/",
        )

        with(result.upcoming.single()) {
            assertEquals(1, position)
            assertEquals("Track", displayTitle)
            assertEquals("Artist", artistName)
            assertEquals("Album", albumTitle)
            assertEquals("4:05", durationLabel)
            assertEquals("https://streamingsoundtracks.com/covers/queue.jpg", artworkUrl)
        }
        assertEquals("Played track", result.recentlyPlayed.single().displayTitle)
    }

    @Test
    fun `caps extended queue and history at thirty tracks`() {
        val queue = (1..35).joinToString("") { position ->
            extendedRow(position, title = "Queue track $position")
        }
        val history = (1..35).joinToString("") { position ->
            extendedRow(position, title = "Played track $position")
        }

        val result = parser.parseExtended(extendedPage(queue, history), "https://1980s.fm/")

        assertEquals(30, result.upcoming.size)
        assertEquals(30, result.recentlyPlayed.size)
        assertEquals(30, result.upcoming.last().position)
        assertEquals("Played track 30", result.recentlyPlayed.last().displayTitle)
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

    private fun extendedPage(queue: String, history: String) = """
        <table>
          <tr><th colspan="3">Queue</th></tr>
          <tr><th>Position</th><th>Cover</th><th>Track</th></tr>
          $queue
        </table>
        <table>
          <tr><th colspan="3">Played</th></tr>
          <tr><th>Position</th><th>Cover</th><th>Track</th></tr>
          $history
        </table>
    """.trimIndent()

    private fun extendedRow(
        position: Int,
        duration: String = "3:21",
        artwork: String = "/covers/$position.jpg",
        album: String = "Album $position",
        artist: String = "Artist $position",
        title: String = "Track $position",
    ) = """
        <tr>
          <td><b>$position</b><br>$duration</td>
          <td><img src="$artwork"></td>
          <td><b>$album</b> - <i>$artist</i><br>$title<br><span class="req-text">Request By: Listener</span></td>
        </tr>
    """.trimIndent()
}
