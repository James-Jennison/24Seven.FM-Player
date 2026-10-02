package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus

class FavoriteTracksPageParserTest {
    private val parser = FavoriteTracksPageParser()
    private val origin = "https://streamingsoundtracks.com/"

    @Test
    fun `discovers signed in member list without hard coded member id`() {
        val html = """<iframe id="thelist" src="/modules/Favorites/thelist.php?user2view=57"></iframe>"""

        assertEquals(
            "https://streamingsoundtracks.com/modules/Favorites/thelist.php?user2view=57",
            parser.parseListUrl(html, origin),
        )
    }

    @Test
    fun `rejects missing and cross origin favorites destinations`() {
        assertThrows(FavoritesAuthenticationRequiredException::class.java) {
            parser.parseListUrl("<form><input name=user_password></form>", origin)
        }
        assertThrows(FavoritesAuthenticationRequiredException::class.java) {
            parser.parseListUrl("<a href='modules.php?name=Your_Account&op=new_user'>Register</a>", origin)
        }
        val unrecognized = assertThrows(IOException::class.java) {
            parser.parseListUrl("<p>The station is being upgraded.</p>", origin)
        }
        assertFalse(unrecognized is FavoritesAuthenticationRequiredException)
        assertThrows(IOException::class.java) {
            parser.parseListUrl(
                """<iframe id="thelist" src="https://example.com/modules/Favorites/thelist.php?user2view=57"></iframe>""",
                origin,
            )
        }
    }

    @Test
    fun `parses another member's list, whose rows carry an extra cell for the listener's own favorites`() {
        val html = """
            <table>
              <tr><td colspan="3">Listener's Favorites</td></tr>
              <tr>
                <td>1</td>
                <td><a href="/modules.php?name=Req&amp;asin=B000KNB1IM&amp;songID=197907" target="_top"><img src="/images/requestbutton_request.png" title="Last Played: Jun 18"></a></td>
                <td><a href="/modules.php?name=Favorites&amp;song2view=197907" target="_top"><img src="/images/heart-red.png"></a><a href="#" onclick="return false"><img src="/images/heart-gray.png"></a></td>
                <td><span><b>Scherzo Berzerko</b></span><br><span>Cartoon Concerto</span></td>
                <td><span><b>Bruce Broughton</b></span><br><span>Soundtrack</span></td>
                <td>2003</td><td>18:36</td><td><a href="https://example.com/buy">Buy</a></td><td><a href="/detail">Detail</a></td>
              </tr>
              <tr>
                <td>2</td>
                <td><img src="/images/requestbutton_unavailable.gif" title="The artist is already in queue."></td>
                <td><a href="#" onclick="return false"><img src="/images/heart-gray.png"></a></td>
                <td><span><b>Unavailable Track</b></span><br><span>Example Album</span></td>
                <td><span><b>Example Artist</b></span><br><span>Game</span></td>
                <td>2020</td><td>3:10</td><td></td><td></td>
              </tr>
            </table>
        """.trimIndent()

        val tracks = parser.parseTracks(html, origin)

        assertEquals(listOf("Scherzo Berzerko", "Unavailable Track"), tracks.map { it.title })
        assertEquals("Cartoon Concerto", tracks[0].album)
        assertEquals("Bruce Broughton", tracks[0].artist)
        assertEquals("Soundtrack", tracks[0].genre)
        assertEquals("2003", tracks[0].year)
        assertEquals("18:36", tracks[0].duration)
        assertEquals("197907", tracks[0].requestTrack?.songId)
        assertNull(tracks[1].requestTrack)
        assertEquals("The artist is already in queue.", tracks[1].availabilityMessage)
        assertEquals("B000KNB1IM", tracks[0].albumId)
        assertNull(tracks[1].albumId)
    }

    @Test
    fun `a track that cannot be requested still names its album from the links beside it`() {
        val html = """
            <table><tr>
              <td>7</td>
              <td><img src="/images/requestbutton_unavailable.gif" title="Played recently."></td>
              <td><span><b>Opening</b></span><br><span>Album Two</span></td>
              <td><span><b>Composer Two</b></span><br><span>Soundtrack</span></td>
              <td>2001</td><td>4:53</td>
              <td><a href="https://www.amazon.com/dp/ASIN/B000000002/example-20" target="_blank">Buy</a></td>
              <td><a href="/modules.php?name=Album&amp;asin=B000000002">Detail</a></td>
            </tr></table>
        """.trimIndent()

        assertEquals("B000000002", parser.parseTracks(html, origin).single().albumId)
    }

    @Test
    fun `names the member a list address belongs to`() {
        assertEquals(
            "4821",
            parser.listMemberNumber("https://streamingsoundtracks.com/modules/Favorites/thelist.php?user2view=4821"),
        )
    }

    @Test
    fun `parses requestable and unavailable favorite tracks with station status`() {
        val html = """
            <table>
              <tr><th>#</th><th>Request</th><th>Tracks</th><th>Artists</th><th>Year</th><th>Length</th><th>Buy</th><th>Detail</th></tr>
              <tr>
                <td>4</td>
                <td><a href="/modules.php?name=Req&amp;asin=B000KNB1IM&amp;songID=197907"><img title="Last Played: Jun 18"></a></td>
                <td><span><b>Scherzo Berzerko</b></span><br><span>Cartoon Concerto</span></td>
                <td><span><b>Bruce Broughton</b></span><br><span>Soundtrack</span></td>
                <td>2003</td><td>18:36</td><td></td><td></td>
              </tr>
              <tr>
                <td>5</td>
                <td><img src="/images/requestbutton_unavailable.gif" title="The artist is already in queue."></td>
                <td><span><b>Unavailable Track</b></span><br><span>Example Album</span></td>
                <td><span><b>Example Artist</b></span><br><span>Game</span></td>
                <td>2020</td><td>3:10</td><td></td><td></td>
              </tr>
            </table>
        """.trimIndent()

        val tracks = parser.parseTracks(html, origin)

        assertEquals(2, tracks.size)
        assertEquals("Scherzo Berzerko", tracks[0].title)
        assertEquals("Cartoon Concerto", tracks[0].album)
        assertEquals("197907", tracks[0].requestTrack?.songId)
        assertEquals("B000KNB1IM", tracks[0].requestTrack?.albumId)
        assertTrue(tracks[0].requestTrack?.eligible == true)
        assertEquals(TrackRequestStatus.Available, tracks[0].availability.status)
        assertNull(tracks[0].availabilityMessage)
        assertNull(tracks[1].requestTrack)
        assertFalse(tracks[1].availabilityMessage.isNullOrBlank())
        assertEquals("The artist is already in queue.", tracks[1].availabilityMessage)
        assertEquals(TrackRequestStatus.RequestsUnavailable, tracks[1].availability.status)
    }

    @Test
    fun `ignores station navigation imagery when no request control is present`() {
        val tracks = parser.parseTracks(
            """
                <table><tr>
                  <td>6</td>
                  <td><img src="/images/station-logo.png" title="Station account navigation"></td>
                  <td><span>Track title</span><span>Album title</span></td>
                  <td><span>Artist</span><span>Soundtrack</span></td>
                  <td>2020</td><td>3:10</td><td></td><td></td>
                </tr></table>
            """.trimIndent(),
            origin,
        )

        assertEquals(1, tracks.size)
        assertNull(tracks.single().availabilityMessage)
        assertEquals(TrackRequestStatus.Unknown, tracks.single().availability.status)
    }
}
