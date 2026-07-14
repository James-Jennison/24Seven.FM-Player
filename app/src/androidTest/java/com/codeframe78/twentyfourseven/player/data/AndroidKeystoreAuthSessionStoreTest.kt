package com.codeframe78.twentyfourseven.player.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codeframe78.twentyfourseven.player.domain.StationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpCookie

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreAuthSessionStoreTest {
    @Test
    fun encryptedSessionRoundTripsAndClears() {
        val store = AndroidKeystoreAuthSessionStore(ApplicationProvider.getApplicationContext())
        val stationId = StationId("keystore-test")
        store.clear(stationId)
        val cookie = HttpCookie("session", "instrumentation-only-value").apply {
            domain = "streamingsoundtracks.com"
            path = "/"
            secure = true
            isHttpOnly = true
        }

        store.save(stationId, "streamingsoundtracks.com", listOf(cookie))
        val restored = store.load(stationId, "streamingsoundtracks.com")

        assertEquals(1, restored.size)
        assertEquals("session", restored.single().name)
        assertEquals("instrumentation-only-value", restored.single().value)
        assertTrue(store.load(stationId, "example.com").isEmpty())
        store.clear(stationId)
        assertTrue(store.load(stationId, "streamingsoundtracks.com").isEmpty())
    }
}
