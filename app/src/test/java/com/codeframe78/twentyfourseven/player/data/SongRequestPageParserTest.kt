package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestSearchTarget
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
        assertEquals(RequestSearchTarget.Album("ALBUM_1"), results.first().target)
        assertEquals("Track One", results.first().title)
        assertEquals("Album One", results.first().subtitle)
        assertEquals("2004", results.first().year)
    }

    @Test
    fun `parses album and artist refinement search rows`() {
        val results = parser.parseSearch(
            """
                <table>
                  <tr><td><a href="/modules.php?name=Album&amp;asin=ALBUM_1">Album One</a></td><td>2004</td></tr>
                  <tr><td><a href="/modules.php?name=Requests&amp;postartistsearch=true&amp;artist=Example+Composer">Example Composer</a></td><td>Soundtrack</td></tr>
                </table>
            """.trimIndent(),
            origin,
        )

        assertEquals(2, results.size)
        assertEquals(RequestSearchTarget.Album("ALBUM_1"), results[0].target)
        assertEquals("Album One", results[0].title)
        assertEquals(null, results[0].subtitle)
        assertEquals("2004", results[0].year)
        assertEquals(RequestSearchTarget.Artist("Example Composer"), results[1].target)
        assertEquals("Example Composer", results[1].title)
        assertEquals("Soundtrack", results[1].subtitle)
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

    @Test
    fun `album parser reads the track title from the stations' structured markup`() {
        val album = parser.parseAlbum(
            """
                <meta property="og:title" content="Album Two - Composer Two | StreamingSoundtracks.com">
                <table>
                  <tr><th>Request</th><th>#</th><th>Track Listing</th><th>Length</th><th>Played</th></tr>
                  <tr itemprop="track" itemscope itemtype="https://schema.org/MusicRecording">
                    <td><a href="../../modules.php?name=Req&amp;asin=B000000002&songID=328994"><img src="modules/SAM/images/requestbutton_request.png" title="Last played: 2026-08-20 11:31:16" alt="Request status"></a></td>
                    <td>01</td>
                    <td><span itemprop="name">Opening</span><br><i itemprop="byArtist" itemscope><a href="https://www.streamingsoundtracks.com/modules.php?name=Requests&amp;postartistsearch=true&amp;artist=Composer+Two"><span itemprop="name">Composer Two</span></a></i></td>
                    <td><time itemprop="duration" datetime="PT6M48S">6:48</time></td>
                    <td>128</td>
                  </tr>
                  <tr itemprop="track" itemscope itemtype="https://schema.org/MusicRecording">
                    <td><img src="modules/SAM/images/requestbutton_disabled.png" title="Last played: 2026-09-18 03:58:54; Request cooldown ends: 2026-10-03 03:58:54" alt="Request status"></td>
                    <td>02</td>
                    <td><span itemprop="name">Finale</span><br><i itemprop="byArtist" itemscope><a href="https://www.streamingsoundtracks.com/modules.php?name=Requests&amp;postartistsearch=true&amp;artist=Composer+Two"><span itemprop="name">Composer Two</span></a></i></td>
                    <td><time itemprop="duration" datetime="PT3M05S">3:05</time></td>
                    <td>7</td>
                  </tr>
                </table>
            """.trimIndent(),
            "https://streamingsoundtracks.com/",
            "B000000002",
        )

        assertEquals("Album Two - Composer Two", album.title)
        assertEquals(listOf("Opening", "Finale"), album.tracks.map { it.title })
        assertEquals(listOf("Composer Two", "Composer Two"), album.tracks.map { it.artist })
        assertEquals(listOf("6:48", "3:05"), album.tracks.map { it.duration })
        assertEquals("328994", album.tracks[0].songId)
        assertEquals(true, album.tracks[0].eligible)
        assertEquals(false, album.tracks[1].eligible)
    }

    @Test
    fun `parses one same-origin random suggestion`() {
        val suggestion = parser.parseSuggestion(
            """
                <table><tr>
                  <td><a href="/modules.php?name=Req&amp;asin=ALBUM_1&amp;songID=12345"><img src="/modules/SAM/images/requestbutton_request.png"></a></td>
                  <td><a href="/modules.php?name=Album&amp;asin=ALBUM_1"><img src="/images/cover/040/ALBUM_1.jpg"></a></td>
                  <td><b>Example Album - Composer</b><br>8. Suggested Track (1:24)</td>
                  <td>2016</td><td>3</td>
                </tr></table>
            """.trimIndent(),
            origin,
        )

        assertEquals("Example Album", suggestion.title)
        assertEquals(1, suggestion.tracks.size)
        assertEquals("ALBUM_1", suggestion.tracks.single().albumId)
        assertEquals("12345", suggestion.tracks.single().songId)
        assertEquals("Suggested Track", suggestion.tracks.single().title)
        assertEquals("Composer", suggestion.tracks.single().artist)
        assertEquals("1:24", suggestion.tracks.single().duration)
        assertTrue(suggestion.tracks.single().eligible)
    }
}
