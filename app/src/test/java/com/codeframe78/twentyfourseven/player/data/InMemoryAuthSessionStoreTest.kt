package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpCookie

class InMemoryAuthSessionStoreTest {
    @Test
    fun `sessions remain isolated and are copied`() {
        val store = InMemoryAuthSessionStore()
        val sst = StationId("sst")
        val cookie = HttpCookie("session", "sensitive").apply {
            domain = "streamingsoundtracks.com"
            path = "/"
            secure = true
            isHttpOnly = true
        }

        store.save(sst, "streamingsoundtracks.com", listOf(cookie), "Listener")
        val restored = store.load(sst, "streamingsoundtracks.com").single()

        assertEquals("sensitive", restored.value)
        assertNotSame(cookie, restored)
        assertEquals("Listener", store.loadDisplayName(sst))
        assertTrue(store.load(StationId("afm"), "adagio.fm").isEmpty())

        store.clear(sst)
        assertTrue(store.load(sst, "streamingsoundtracks.com").isEmpty())
    }

    @Test
    fun `clearing one of five station sessions preserves every other session`() {
        val store = InMemoryAuthSessionStore()
        val stations = listOf(
            StationId("sst") to "streamingsoundtracks.com",
            StationId("1980s") to "1980s.fm",
            StationId("afm") to "adagio.fm",
            StationId("dfm") to "death.fm",
            StationId("efm") to "entranced.fm",
        )

        stations.forEachIndexed { index, (stationId, domain) ->
            store.save(
                stationId,
                domain,
                listOf(HttpCookie("session", "value-$index").apply { this.domain = domain }),
                "Listener $index",
            )
        }

        store.clear(StationId("dfm"))

        stations.forEachIndexed { index, (stationId, domain) ->
            if (stationId == StationId("dfm")) {
                assertTrue(store.load(stationId, domain).isEmpty())
                assertEquals(null, store.loadDisplayName(stationId))
            } else {
                assertEquals("value-$index", store.load(stationId, domain).single().value)
                assertEquals("Listener $index", store.loadDisplayName(stationId))
            }
        }
    }

    @Test
    fun `legacy station ids resolve to canonical isolated session keys`() {
        val store = InMemoryAuthSessionStore()
        store.save(
            StationId("adagio"),
            "adagio.fm",
            listOf(cookie("session", "legacy", "adagio.fm")),
            "Listener",
        )

        assertEquals("legacy", store.load(StationId("afm"), "adagio.fm").single().value)
        assertEquals("Listener", store.loadDisplayName(StationId("afm")))

        store.clear(StationId("adagio"))
        assertTrue(store.load(StationId("afm"), "adagio.fm").isEmpty())
    }

    @Test
    fun `unchanged cookie updates preserve absolute expiry`() {
        var now = 1_000L
        val store = InMemoryAuthSessionStore { now }
        val station = StationId("sst")
        val original = cookie("session", "value", "streamingsoundtracks.com").apply { maxAge = 10L }
        store.save(station, "streamingsoundtracks.com", listOf(original), "Listener")

        now += 5_000L
        store.updateCookies(
            station,
            "streamingsoundtracks.com",
            store.load(station, "streamingsoundtracks.com"),
            refreshedCookies = emptyList(),
        )
        now += 5_001L

        assertTrue(store.load(station, "streamingsoundtracks.com").isEmpty())
        assertEquals(null, store.loadDisplayName(station))
    }

    @Test
    fun `rotation extends only the cookie named by the station response`() {
        var now = 10_000L
        val store = InMemoryAuthSessionStore { now }
        val station = StationId("sst")
        val rotating = cookie("session", "old", "streamingsoundtracks.com").apply { maxAge = 10L }
        val unchanged = cookie("preference", "fixed", "streamingsoundtracks.com").apply { maxAge = 20L }
        store.save(station, "streamingsoundtracks.com", listOf(rotating, unchanged), "Listener")

        now += 5_000L
        val rotated = cookie("session", "new", "streamingsoundtracks.com").apply { maxAge = 30L }
        store.updateCookies(
            station,
            "streamingsoundtracks.com",
            listOf(rotated, unchanged),
            refreshedCookies = listOf(rotated),
        )
        now += 15_001L

        val restored = store.load(station, "streamingsoundtracks.com")
        assertEquals(listOf("new"), restored.map(HttpCookie::getValue))
    }

    private fun cookie(name: String, value: String, domain: String) = HttpCookie(name, value).apply {
        this.domain = domain
        path = "/"
        secure = true
        isHttpOnly = true
    }
}
