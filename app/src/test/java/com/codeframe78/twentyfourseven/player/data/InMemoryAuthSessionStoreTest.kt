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

        store.save(sst, "streamingsoundtracks.com", listOf(cookie))
        val restored = store.load(sst, "streamingsoundtracks.com").single()

        assertEquals("sensitive", restored.value)
        assertNotSame(cookie, restored)
        assertTrue(store.load(StationId("adagio"), "adagio.fm").isEmpty())

        store.clear(sst)
        assertTrue(store.load(sst, "streamingsoundtracks.com").isEmpty())
    }
}
