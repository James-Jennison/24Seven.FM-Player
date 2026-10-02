package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerQueueResponseParserTest {
    private val parser = PlayerQueueResponseParser()

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
            assertEquals("ALBUM_1", albumId)
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
    fun `parses the current station queue markup`() {
        val result = parser.parseExtended(
            extendedPage(
                queue = """
                    <tr>
                      <td><span class="glowing-rank">1</span><br>4:08</td>
                      <td><a href="/modules.php?name=Album&amp;asin=ALBUM_1"><img src="/covers/queue.jpg"></a></td>
                      <td><b>Current artist</b> - Current album<br>
                        <span style="color: #AAAAAA;">Current title</span>
                        <br><span class="req-text"><a href="/modules.php?name=Your_Account&amp;op=userinfo&amp;username=Listener"><b>Listener</b></a></span>
                      </td>
                    </tr>
                """.trimIndent(),
                history = "",
            ),
            "https://1980s.fm/",
        )

        with(result.upcoming.single()) {
            assertEquals(1, position)
            assertEquals("Current title", displayTitle)
            assertEquals("Current artist", artistName)
            assertEquals("Current album", albumTitle)
            assertEquals("ALBUM_1", albumId)
            assertEquals("Listener", requesterName)
        }
    }

    @Test
    fun `reads album and artist in the order the table heading gives`() {
        val result = parser.parseExtended(
            extendedPage(
                queue = """
                    <tr>
                      <td class="td01"><b>No.</b><br>Time</td>
                      <td class="td01"><img src="/images/logos/logo-sst-40x40.jpg"></td>
                      <td class="td01"><b> Album</b> - Artist<br> Title</td>
                    </tr>
                    <tr>
                      <td><span class="glowing-rank">1</span><br>11:32</td>
                      <td><a href="/modules.php?name=Album&amp;asin=ALBUM_1"><img src="/covers/queue.jpg"></a></td>
                      <td><b>Listed album</b> - Listed artist<br>
                        <span style="color: #AAAAAA;">Listed title</span>
                      </td>
                    </tr>
                """.trimIndent(),
                history = "",
            ),
            "https://streamingsoundtracks.com/",
        )

        with(result.upcoming.single()) {
            assertEquals("Listed title", displayTitle)
            assertEquals("Listed artist", artistName)
            assertEquals("Listed album", albumTitle)
        }
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

    @Test
    fun `supports a larger bounded queue snapshot for request verification`() {
        val queue = (1..124).joinToString("") { position ->
            extendedRow(position, title = "Queue track $position")
        }

        val result = parser.parseExtended(
            extendedPage(queue, history = ""),
            "https://entranced.fm/",
            maxTracks = 500,
        )

        assertEquals(124, result.upcoming.size)
        assertEquals(124, result.upcoming.last().position)
    }

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
              <td><a href="/modules.php?name=Album&amp;asin=ALBUM_$position"><b>$album</b></a> - <i>$artist</i><br>$title<br>$requesterHtml</td>
            </tr>
        """.trimIndent()
    }
}
