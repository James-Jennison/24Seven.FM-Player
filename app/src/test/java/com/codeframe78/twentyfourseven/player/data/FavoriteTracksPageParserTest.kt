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
                <td onclick="ViewInfoTrack(4821, 197908)"><span><b>Unavailable Track</b></span><br><span>Example Album</span></td>
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
        // The song number the favorites controls act on comes from the request link or the row's own handler.
        assertEquals("197907", tracks[0].songId)
        assertEquals("197908", tracks[1].songId)
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

    @Test
    fun `parses a page of the member's ranked favorites from the profile feed`() {
        val html = """
            <div class="profile-favorites"><table class="table01">
            <tr><th class="th01" colspan="4">Listener's Favorite Tracks</th></tr>
            <tr>
              <td class="td01"><div><a href="/modules.php?name=Album&amp;asin=B000AYI8YQ"><img src="/images/cover/040/B000AYI8YQ.jpg" alt="" title="Example Album (1997 TV)"></a></div></td>
              <td class="td01"><a href="/modules.php?name=Req&amp;asin=B000AYI8YQ&amp;songID=190659"><img title="Last played: 2026-09-03 10:17:50" src="/modules/SAM/images/requestbutton_request.png"></a></td>
              <td class="td02"><span style="font-weight:bold;">Opening Fanfare</span><br>Example Album (1997 TV) by <b>Composer One</b><br><span>1997, Soundtrack</span></td>
              <td class="td01"><form class="favorite-controls-form" method="post" action="/modules.php?name=Favorites"><input type="hidden" name="songid" value="190659"><input type="hidden" name="Yes2Remove" value="1"><button type="submit" name="op" value="movetrackup">▲</button></form></td>
            </tr>
            <tr>
              <td class="td01"><div><a href="/modules.php?name=Album&amp;asin=B000000002"><img src="/images/cover/040/B000000002.jpg" alt="" title="Second Album"></a></div></td>
              <td class="td01"><img title="The artist is already in queue." src="/modules/SAM/images/requestbutton_unavailable.gif"></td>
              <td class="td02"><span style="font-weight:bold;">Finale</span><br>Second Album by <b>Composer Two</b><br><span>2003, Game</span></td>
              <td class="td01"><form class="favorite-controls-form" method="post" action="/modules.php?name=Favorites"><input type="hidden" name="songid" value="235578"><input type="hidden" name="Yes2Remove" value="1"></form></td>
            </tr>
            </table>
            <nav><button type="button" class="profile-tab-btn favorite-page" data-page-url="/modules/Your_Profile/favsTabAJAX.php?tracks_page=1&amp;albums_page=1&amp;kind=tracks">Previous</button><button type="button" class="profile-tab-btn favorite-page" data-page-url="/modules/Your_Profile/favsTabAJAX.php?tracks_page=3&amp;albums_page=1&amp;kind=tracks">Next</button><span>Page 2 &middot; up to 50 per page</span></nav></div>
        """.trimIndent()
        val json = org.json.JSONObject().put("result", 0).put("HTML", html).toString()

        val page = parser.parseRankedPage(json, origin, firstPosition = 51)

        assertEquals(listOf(51, 52), page.tracks.map { it.position })
        assertEquals(listOf("190659", "235578"), page.tracks.map { it.songId })
        assertEquals("Opening Fanfare", page.tracks[0].title)
        assertEquals("Example Album (1997 TV)", page.tracks[0].album)
        assertEquals("Composer One", page.tracks[0].artist)
        assertEquals("1997", page.tracks[0].year)
        assertEquals("Soundtrack", page.tracks[0].genre)
        assertEquals("B000AYI8YQ", page.tracks[0].albumId)
        assertEquals("190659", page.tracks[0].requestTrack?.songId)
        assertNull(page.tracks[1].requestTrack)
        assertEquals("The artist is already in queue.", page.tracks[1].availabilityMessage)
        assertEquals("B000000002", page.tracks[1].albumId)
        assertEquals(3, page.nextPage)

        val lastPage = parser.parseRankedPage(org.json.JSONObject().put("HTML", html.replace(">Next<", ">Last<")).toString(), origin)
        assertNull(lastPage.nextPage)
        assertEquals(1, lastPage.tracks.first().position)
    }

    @Test
    fun `parses a page of the redesigned member list with its rank, controls, paging and stated total`() {
        val html = """
            <nav class="favorite-list-tabs"><a target="_top" href="/modules.php?name=Favorites&amp;user2view=4821" aria-current="page">Tracks</a></nav>
            <div class="favorite-toolbar-options"><span class="favorite-result-count">1&ndash;50 of 1500 tracks</span></div>
            <nav class="favorite-pager favorite-pager-top"><span>Page 1</span><a rel="next" href="/modules/Favorites/thelist.php?user2view=4821&amp;page=2">Next</a></nav>
            <form id="favorite-manage" action="/modules.php?name=Favorites" method="post" target="_top"></form>
            <table class="table01" id="favtracks"><tbody>
            <tr><th class="th01 favorite-rank-heading">#</th><th class="th01">Control</th><th class="th01 favorite-cover"></th><th class="th01 favorite-request">Request</th><th class="th01 favorite-info-heading">Track</th><th id="lastplaytr" class="th01 favorite-played">Last played</th></tr>
            <tr><td class="td01" align="center"><span class="favorite-rank">349</span><br><span style="color:#FFFF80;font-weight:bold;">22:32</span></td>
              <td class="td01" align="center"><div class="favorite-actions"><button type="button" class="favorite-control q-btn favorite-info-button" onclick="ViewInfoTrack(4821, 246835);">i</button><button type="submit" form="favorite-manage" formaction="/modules.php?name=Favorites&amp;op=movetrackup&amp;songid=246835" class="favorite-control q-btn btn-up">&#9650;</button><button type="submit" form="favorite-manage" formaction="/modules.php?name=Favorites&amp;op=movetrackdown&amp;songid=246835" class="favorite-control q-btn btn-down">&#9660;</button><button type="submit" form="favorite-manage" formaction="/modules.php?name=Favorites&amp;op=removetrackfromlist&amp;songid=246835" class="favorite-control q-btn btn-del">&#10006;</button></div></td>
              <td class="td01 favorite-cover"><a target="_top" href="/modules.php?name=Album&amp;asin=B000000001"><img src="/images/cover/040/B000000001.jpg" alt="View album"></a></td>
              <td class="td01 favorite-request"><img src="/modules/SAM/images/requestbutton_disabled.png" title="Last played: 2026-11-13 16:06:05; Request cooldown ends: 2026-11-28 16:06:05" alt="Request cooldown ends: 2026-11-28 16:06:05"></td>
              <td class="td02 favorite-info"><strong>Exodus</strong>Example Album: The Best by <a target="_top" href="/modules.php?name=Requests&amp;postartistsearch=true&amp;artist=Composer%20One">Composer One</a><small class="favorite-meta">2000, Soundtrack</small></td>
              <td class="td01 favorite-played" align="center"><time datetime="2026-11-13T16:06:05"><strong>2026-11-13</strong><br>16:06</time></td>
            </tr>
            <tr><td class="td01" align="center"><span class="favorite-rank">961</span><br><span style="color:#FFFF80;font-weight:bold;">18:36</span></td>
              <td class="td01" align="center"><div class="favorite-actions"><button type="button" class="favorite-control q-btn favorite-info-button" onclick="ViewInfoTrack(4821, 197907);">i</button><button type="submit" form="favorite-manage" formaction="/modules.php?name=Favorites&amp;op=removetrackfromlist&amp;songid=197907" class="favorite-control q-btn btn-del">&#10006;</button></div></td>
              <td class="td01 favorite-cover"><a target="_top" href="/modules.php?name=Album&amp;asin=B000KNB1IM"><img src="/images/cover/040/B000KNB1IM.jpg" alt="View album"></a></td>
              <td class="td01 favorite-request"><a target="_top" href="/modules.php?name=Req&amp;asin=B000KNB1IM&amp;songID=197907"><img src="/modules/SAM/images/requestbutton_request.png" title="Last played: 2026-07-30 15:02:47; Request this track." alt="Request this track."></a></td>
              <td class="td02 favorite-info"><strong>Scherzo Berzerko In 3 Portions</strong>Cartoon Concerto by <a target="_top" href="/modules.php?name=Requests&amp;postartistsearch=true&amp;artist=Bruce%20Broughton">Bruce Broughton</a><small class="favorite-meta">2003, Soundtrack</small></td>
              <td class="td01 favorite-played" align="center"><time datetime="2026-07-30T15:02:47"><strong>2026-07-30</strong><br>15:02</time></td>
            </tr>
            </tbody></table>
        """.trimIndent()

        val page = parser.parseTracksPage(html, origin)

        assertEquals(listOf(349, 961), page.tracks.map { it.position })
        assertEquals(listOf("Exodus", "Scherzo Berzerko In 3 Portions"), page.tracks.map { it.title })
        assertEquals("Example Album: The Best", page.tracks[0].album)
        assertEquals("Composer One", page.tracks[0].artist)
        assertEquals("2000", page.tracks[0].year)
        assertEquals("Soundtrack", page.tracks[0].genre)
        assertEquals("22:32", page.tracks[0].duration)
        assertEquals("B000000001", page.tracks[0].albumId)
        assertNull(page.tracks[0].requestTrack)
        assertEquals(TrackRequestStatus.RecentlyPlayed, page.tracks[0].availability.status)
        assertEquals("246835", page.tracks[0].songId)
        assertEquals("197907", page.tracks[1].requestTrack?.songId)
        assertEquals("B000KNB1IM", page.tracks[1].requestTrack?.albumId)
        assertEquals("Cartoon Concerto", page.tracks[1].album)
        assertEquals("18:36", page.tracks[1].duration)
        assertEquals(2, page.nextPage)
        assertEquals(1500, page.totalTracks)

        val last = parser.parseTracksPage(html.replace("""<a rel="next" href="/modules/Favorites/thelist.php?user2view=4821&amp;page=2">Next</a>""", ""), origin)
        assertNull(last.nextPage)
    }

    @Test
    fun `parses another member's redesigned list, whose rows carry the listener's own heart cell`() {
        val html = """
            <table id="favtracks"><tr>
              <td class="td01"><span class="favorite-rank">1131</span><br><span>21:08</span></td>
              <td class="td01"><button type="button" class="favorite-control q-btn favorite-info-button" onclick="ViewInfoTrack(4822, 300001);">i</button></td>
              <td class="td01 favorite-cover"><a target="_top" href="/modules.php?name=Album&amp;asin=B07YYWVGSK"><img src="/images/cover/040/B07YYWVGSK.jpg"></a></td>
              <td class="td01 favorite-request"><a target="_top" href="/modules.php?name=Req&amp;asin=B07YYWVGSK&amp;songID=300001"><img src="/modules/SAM/images/requestbutton_request.png" title="Request this track."></a></td>
              <td class="td02 favorite-info"><strong>Tuesday (Voiceless)</strong>Ad Astra by <a target="_top" href="/modules.php?name=Requests&amp;postartistsearch=true&amp;artist=Max%20Richter">Max Richter</a><small class="favorite-meta">2019, Soundtrack</small></td>
              <td class="td01" align="center"><a style="display:none" href="/modules.php?name=Favorites&amp;song2view=300001" target="_top"><img src="/images/favorites/heart-red.png"></a><a href="#" onclick="AddTrack(300001, 'ityes300001', 'itno300001'); return false;"><img src="/images/favorites/heart-gray.png"></a></td>
              <td class="td01 favorite-played"><time datetime="2026-11-08T03:04:21"><strong>2026-11-08</strong><br>03:04</time></td>
            </tr></table>
        """.trimIndent()

        val track = parser.parseTracks(html, origin).single()

        assertEquals(1131, track.position)
        assertEquals("Tuesday (Voiceless)", track.title)
        assertEquals("Ad Astra", track.album)
        assertEquals("Max Richter", track.artist)
        assertEquals("300001", track.songId)
        assertEquals("21:08", track.duration)
    }

    @Test
    fun `parses the redesigned ranked feed, whose rows no longer carry the controls form`() {
        val html = """
            <table class="table01"><tr>
              <td class="td01"><div><a href="/modules.php?name=Album&amp;asin=B000AYI8YQ"><img src="/images/cover/040/B000AYI8YQ.jpg" alt="View album details" title="Example Album (1997 TV)"></a></div></td>
              <td class="td01"><a href="/modules.php?name=Req&amp;asin=B000AYI8YQ&amp;songID=190659"><img src="/modules/SAM/images/requestbutton_request.png" title="Request this track."></a></td>
              <td class="td02"><strong>Opening Fanfare</strong><br>Example Album (1997 TV) by <b>Composer One</b><br><span>1997, Soundtrack</span></td>
            </tr><tr>
              <td class="td01"><div><a href="/modules.php?name=Album&amp;asin=B000000002"><img src="/images/cover/040/B000000002.jpg" title="Second Album"></a></div></td>
              <td class="td01"><img src="/modules/SAM/images/requestbutton_disabled.png" title="Request cooldown ends: 2026-11-28 16:06:05"></td>
              <td class="td02"><strong>Finale</strong><br>Second Album by <b>Composer Two</b><br><span>2003, Game</span></td>
            </tr></table>
            <button type="button" class="profile-tab-btn favorite-page" data-page-url="/modules/Your_Profile/favsTabAJAX.php?tracks_page=2&amp;albums_page=1&amp;kind=tracks">Next</button>
        """.trimIndent()

        val page = parser.parseRankedPage(org.json.JSONObject().put("result", 0).put("HTML", html).toString(), origin)

        assertEquals(listOf("Opening Fanfare", "Finale"), page.tracks.map { it.title })
        assertEquals("Example Album (1997 TV)", page.tracks[0].album)
        assertEquals("Composer One", page.tracks[0].artist)
        assertEquals("190659", page.tracks[0].songId)
        assertNull(page.tracks[1].songId)
        assertEquals("B000000002", page.tracks[1].albumId)
        assertEquals(2, page.nextPage)
    }

    @Test
    fun `an album page names the song number of a track that cannot be requested`() {
        val html = """
            <table><tr><td><table>
              <tr><td class="td01"><img src="/images/requestbutton_disabled.png" title="Last played: 2026-11-11"></td>
                <td class="td01"><span id="ityes246835"><img src="/images/heart.svg" alt="In Favorites"></span><a id="itno246835" style="display:none" href="javascript:void(0)" onclick="AddTrack(246835, &quot;ityes246835&quot;, &quot;itno246835&quot;); return false;"><img src="/images/heart.svg"></a></td>
                <td class="td01"><span itemprop="name">All For Love</span><br><i itemprop="byArtist">Composer</i></td></tr>
              <tr><td class="td01"><a href="/modules.php?name=Req&amp;asin=B000000OD8&amp;songID=246836"><img src="/images/requestbutton_request.png"></a></td>
                <td class="td01"></td>
                <td class="td01"><span itemprop="name">Main Title</span></td></tr>
              <tr><td class="td01"><img src="/images/requestbutton_disabled.png"></td><td class="td01"><a onclick="AddTrack(1, &quot;a&quot;, &quot;b&quot;)">x</a></td><td class="td01"><span itemprop="name">Twice</span></td></tr>
              <tr><td class="td01"><img src="/images/requestbutton_disabled.png"></td><td class="td01"><a onclick="AddTrack(2, &quot;a&quot;, &quot;b&quot;)">x</a></td><td class="td01"><span itemprop="name">Twice</span></td></tr>
            </table></td></tr></table>
        """.trimIndent()
        val parser = FavoriteTracksPageParser()

        assertEquals("246835", parser.albumTrackSongId(html, "All For Love"))
        assertEquals("246836", parser.albumTrackSongId(html, "Main Title"))
        // Two tracks with one title cannot be told apart, and a title the page does not have names nothing.
        assertEquals(null, parser.albumTrackSongId(html, "Twice"))
        assertEquals(null, parser.albumTrackSongId(html, "Missing"))
    }
}
