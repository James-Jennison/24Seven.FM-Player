package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
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
}
