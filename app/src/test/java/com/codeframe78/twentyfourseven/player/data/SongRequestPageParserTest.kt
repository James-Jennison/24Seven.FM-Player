package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongRequestPageParserTest {
    private val parser = SongRequestPageParser()
    private val origin = "https://station.example/"

    @Test
    fun `parses same-origin search results and ignores foreign album links`() {
        val results = parser.parseSearch(
            """
                <table><tr><td><table>
                  <tr><td><a href="/modules.php?name=Album&amp;asin=ALBUM_1">Track One</a></td>
                      <td><a href="/modules.php?name=Album&amp;asin=ALBUM_1">Album One</a></td><td>2004</td></tr>
                  <tr><td><a href="/modules.php?name=Album&amp;asin=ALBUM_2">Track Two</a></td>
                      <td><a href="/modules.php?name=Album&amp;asin=ALBUM_2">Album Two</a></td><td>2016</td></tr>
                  <tr><td><a href="https://foreign.example/modules.php?name=Album&amp;asin=X">Foreign</a></td>
                      <td><a href="https://foreign.example/modules.php?name=Album&amp;asin=X">Album</a></td></tr>
                </table></td></tr></table>
            """.trimIndent(),
            origin,
        )

        assertEquals(2, results.size)
        assertEquals("ALBUM_1", results.first().albumId)
        assertEquals("Track One", results.first().trackTitle)
        assertEquals("Album One", results.first().albumTitle)
        assertEquals("2004", results.first().year)
    }

    @Test
    fun `album parser preserves server eligibility and exact request identifiers`() {
        val album = parser.parseAlbum(
            """
                <html><head><meta property="og:title" content="Example Album"></head><body><table>
                  <tr><td><a href="/modules.php?name=Req&amp;asin=ALBUM_1&amp;songID=12345"><img src="/images/requestbutton_request.png"></a></td>
                      <td>01</td><td>Available Track <a href="/modules.php?name=Requests&amp;postartistsearch=true&amp;artist=Composer">Composer</a></td><td>3:21</td></tr>
                  <tr><td><img src="/images/requestbutton_disabled.png"></td>
                      <td>02</td><td>Unavailable Track <a href="/modules.php?name=Requests&amp;postartistsearch=true&amp;artist=Composer">Composer</a></td><td>4:02</td></tr>
                </table></body></html>
            """.trimIndent(),
            origin,
            "ALBUM_1",
        )

        assertEquals("Example Album", album.title)
        assertEquals(2, album.tracks.size)
        assertTrue(album.tracks[0].eligible)
        assertEquals("12345", album.tracks[0].songId)
        assertEquals("Available Track", album.tracks[0].title)
        assertEquals("Composer", album.tracks[0].artist)
        assertEquals("3:21", album.tracks[0].duration)
        assertFalse(album.tracks[1].eligible)
        assertEquals("", album.tracks[1].songId)
    }
}
