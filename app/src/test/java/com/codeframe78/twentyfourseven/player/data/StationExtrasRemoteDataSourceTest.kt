package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class StationExtrasRemoteDataSourceTest {
    private val station = StationId("sst")

    @Test
    fun `history asks for the middle of the two-hour block`() = runTest {
        val pages = FakeStationPages { _, _, _ -> StationExtrasFixtures.history() }

        val entries = StationExtrasRemoteDataSource(pages).history(station, LocalDate.of(2026, 10, 2), 2)

        assertEquals(2, entries.size)
        assertEquals(
            "GET /modules/Played_History/HistTableAJAX.php?histdate=2026-10-02&histtime=03:00:00",
            pages.requests.single(),
        )
    }

    @Test
    fun `a member the station does not know has no profile`() = runTest {
        val pages = FakeStationPages { _, _, _ -> throw StationHttpException(404) }

        assertNull(StationExtrasRemoteDataSource(pages).profile(station, "Nobody"))
    }

    @Test
    fun `a profile card for a different member than the one asked for is refused`() = runTest {
        val pages = FakeStationPages { _, _, _ -> StationExtrasFixtures.profileCard() }
        val remote = StationExtrasRemoteDataSource(pages)

        assertEquals("Listener", remote.profile(station, "listener")?.username)
        assertNull(remote.profile(station, "Someone Else"))
        assertEquals("GET /modules/Your_Profile/hoverCardAJAX.php?username=Someone+Else", pages.requests.last())
    }

    @Test
    fun `a member's favorites are read from the list the station names for that member`() = runTest {
        val pages = FakeStationPages { _, path, _ ->
            if (path.startsWith("/modules.php")) favoritesPage(member = "4821") else FAVORITES_LIST
        }

        val tracks = StationExtrasRemoteDataSource(pages).memberFavorites(station, "4821")

        assertEquals("Opening", tracks.single().title)
        assertEquals(
            listOf(
                "GET /modules.php?name=Favorites&user2view=4821",
                "GET /modules/Favorites/thelist.php?user2view=4821",
            ),
            pages.requests,
        )
    }

    @Test
    fun `a favorites page that points at another member's list is refused`() = runTest {
        val pages = FakeStationPages { _, _, _ -> favoritesPage(member = "57") }

        val failure = runCatching { StationExtrasRemoteDataSource(pages).memberFavorites(station, "4821") }.exceptionOrNull()

        assertTrue(failure is IOException && failure !is FavoritesAuthenticationRequiredException)
        assertEquals(1, pages.requests.size)
        assertFalse(pages.expired)
    }

    @Test
    fun `a member's favorites need the listener's session and are not asked for without one`() = runTest {
        val signedOut = FakeStationPages(signedIn = false) { _, _, _ -> error("not expected") }

        assertTrue(
            runCatching { StationExtrasRemoteDataSource(signedOut).memberFavorites(station, "4821") }
                .exceptionOrNull() is FavoritesAuthenticationRequiredException,
        )
        assertTrue(signedOut.requests.isEmpty())
    }

    @Test
    fun `a session the station no longer honours is ended when it answers with the sign-in form`() = runTest {
        val pages = FakeStationPages { _, _, _ -> "<form><input name=user_password></form>" }

        assertTrue(
            runCatching { StationExtrasRemoteDataSource(pages).memberFavorites(station, "4821") }
                .exceptionOrNull() is FavoritesAuthenticationRequiredException,
        )
        assertTrue(pages.expired)
    }

    @Test
    fun `a member number that is not a number is never sent to the station`() = runTest {
        val pages = FakeStationPages { _, _, _ -> error("not expected") }

        assertTrue(
            runCatching { StationExtrasRemoteDataSource(pages).memberFavorites(station, "57&op=add") }
                .exceptionOrNull() is IllegalArgumentException,
        )
        assertTrue(pages.requests.isEmpty())
    }

    private fun favoritesPage(member: String) =
        """<iframe id="thelist" src="/modules/Favorites/thelist.php?user2view=$member"></iframe>"""

    private companion object {
        val FAVORITES_LIST = """
            <table><tr>
              <td>1</td><td><img src="/images/requestbutton_unavailable.gif" title="Played recently."></td>
              <td><a href="#"><img src="/images/heart-gray.png"></a></td>
              <td><span><b>Opening</b></span><br><span>Album Two</span></td>
              <td><span><b>Composer Two</b></span><br><span>Soundtrack</span></td>
              <td>2001</td><td>4:53</td><td></td><td></td>
            </tr></table>
        """.trimIndent()
    }
}
