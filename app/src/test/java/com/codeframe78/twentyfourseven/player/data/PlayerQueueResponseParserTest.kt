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
    fun `parses extended queue fields and explicit requester separately from title`() {
        val result = parser.parseExtended(
            extendedPage(
                queue = extendedRow(
                    1,
                    "4:05",
                    "/covers/queue.jpg",
                    "Album",
                    "Artist",
                    "Track",
                    requesterName = "Listener",
                    requestMessage = "A message for the queue",
                ),
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
            assertEquals("Listener", requesterName)
            assertEquals("A message for the queue", requestMessage)
        }
        assertEquals("Played track", result.recentlyPlayed.single().displayTitle)
        assertEquals("Listener", result.recentlyPlayed.single().requesterName)
    }

    @Test
    fun `ignores malformed requester labels without changing track fields`() {
        val result = parser.parseExtended(
            extendedPage(
                queue = extendedRow(1, requesterName = null, rawRequesterHtml = "<span class=\"req-text\">Requested for Listener</span>"),
                history = "",
            ),
            "https://streamingsoundtracks.com/",
        )

        assertEquals("Track 1", result.upcoming.single().displayTitle)
        assertNull(result.upcoming.single().requesterName)
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
        <table class="layout">
          <tr>
            <td>
              <table class="queue">
                <tr><th colspan="3">Queue</th></tr>
                <tr><th>Position</th><th>Cover</th><th>Track</th></tr>
                $queue
              </table>
            </td>
            <td>
              <table class="played">
                <tr><th colspan="3">Played</th></tr>
                <tr><th>Position</th><th>Cover</th><th>Track</th></tr>
                $history
              </table>
            </td>
          </tr>
        </table>
    """.trimIndent()

    private fun extendedRow(
        position: Int,
        duration: String = "3:21",
        artwork: String = "/covers/$position.jpg",
        album: String = "Album $position",
        artist: String = "Artist $position",
        title: String = "Track $position",
        requesterName: String? = "Listener",
        requestMessage: String? = null,
        rawRequesterHtml: String? = null,
    ): String {
        val requesterHtml = rawRequesterHtml ?: requesterName?.let { name ->
            val messageHtml = requestMessage?.let { " - <i>$it</i>" }.orEmpty()
            "<span class=\"req-text\">&nbsp;Request By: <a href=\"/modules.php?name=Your_Account&amp;op=userinfo&amp;username=$name\"><b>$name</b></a>$messageHtml</span>"
        }.orEmpty()
        return """
            <tr>
              <td><b>$position</b><br>$duration</td>
              <td><img src="$artwork"></td>
              <td><b>$album</b> - <i>$artist</i><br>$title<br>$requesterHtml</td>
            </tr>
        """.trimIndent()
    }
}
