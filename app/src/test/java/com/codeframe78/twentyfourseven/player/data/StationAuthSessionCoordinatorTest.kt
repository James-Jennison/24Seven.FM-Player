package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import java.net.HttpCookie
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class StationAuthSessionCoordinatorTest {
    @Test
    fun `rotation and deletion converge in memory and protected persistence`() = runTest {
        val station = StationId("sst")
        val origin = "https://streamingsoundtracks.com/"
        val store = InMemoryAuthSessionStore().apply {
            save(
                station,
                "streamingsoundtracks.com",
                listOf(sessionCookie("old")),
                "Listener",
            )
        }
        val coordinator = StationAuthSessionCoordinator(store)
        val manager = coordinator.cookieManager(station, origin)

        coordinator.captureResponse(
            station,
            origin,
            URI(origin),
            mapOf("Set-Cookie" to listOf("session=new; Max-Age=60; Path=/; Secure; HttpOnly")),
        )

        assertSame(manager, coordinator.cookieManager(station, origin))
        assertEquals("new", manager.cookieStore.cookies.single().value)
        assertEquals("new", store.load(station, "streamingsoundtracks.com").single().value)
        assertEquals(ProtectedSessionValidity.Active, coordinator.observeValidity(station).first())

        coordinator.captureResponse(
            station,
            origin,
            URI(origin),
            mapOf("Set-Cookie" to listOf("session=gone; Max-Age=0; Path=/; Secure; HttpOnly")),
        )

        assertTrue(manager.cookieStore.cookies.isEmpty())
        assertTrue(store.load(station, "streamingsoundtracks.com").isEmpty())
        assertNull(store.loadDisplayName(station))
        assertEquals(ProtectedSessionValidity.Expired, coordinator.observeValidity(station).first())
    }

    private fun sessionCookie(value: String) = HttpCookie("session", value).apply {
        domain = "streamingsoundtracks.com"
        path = "/"
        secure = true
        isHttpOnly = true
    }
}
