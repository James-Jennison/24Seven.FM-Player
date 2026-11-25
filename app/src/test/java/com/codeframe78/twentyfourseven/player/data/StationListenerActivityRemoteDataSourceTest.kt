package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URL

class StationListenerActivityRemoteDataSourceTest {
    @Test
    fun `a history page with the clock block needs no other page for the status`() = runTest {
        val station = FakeStation(history = historyPage(CLOCK_READY))

        val snapshot = station.remote.load(stationId)

        assertEquals(RequestReadiness.Ready, snapshot.requestReadiness)
        assertEquals(1054, snapshot.queuedRequestWaitSeconds)
        assertTrue(station.requested.none { it == REQUESTS || it == TIMER })
    }

    @Test
    fun `a history page with a timer frame reads the frame and not the request page`() = runTest {
        val station = FakeStation(
            history = historyPage("<iframe src=\"$TIMER\"></iframe>"),
            timer = "<div>Your Request: Waiting</div><div>Request Wait: 37 Minutes</div>",
        )

        val snapshot = station.remote.load(stationId)

        assertEquals(RequestReadiness.Waiting, snapshot.requestReadiness)
        assertEquals(37, snapshot.waitMinutes)
        assertEquals(1, station.requested.count { it == TIMER })
        assertTrue(station.requested.none { it == REQUESTS })
    }

    @Test
    fun `a history page with neither reads the status from the request page once`() = runTest {
        val station = FakeStation(history = historyPage(""), requests = requestsPage("Ready to request"))

        val snapshot = station.remote.load(stationId)

        assertEquals(RequestReadiness.Ready, snapshot.requestReadiness)
        assertNull(snapshot.waitMinutes)
        assertEquals(1, station.requested.count { it == REQUESTS })
    }

    @Test
    fun `a request page that cannot be read leaves the status unknown and keeps the rest`() = runTest {
        val station = FakeStation(history = historyPage(""), requestsStatus = 500)

        val snapshot = station.remote.load(stationId)

        assertEquals(RequestReadiness.Unknown, snapshot.requestReadiness)
        assertEquals("Opening", snapshot.recentRequests.single().trackSummary)
        assertEquals("Listener", station.store.loadDisplayName(stationId))
    }

    @Test
    fun `a request page worded another way leaves the status unknown`() = runTest {
        val station = FakeStation(history = historyPage(""), requests = requestsPage("14 minutes"))

        assertEquals(RequestReadiness.Unknown, station.remote.load(stationId).requestReadiness)
    }

    @Test
    fun `a request page that asks for sign-in ends the session`() = runTest {
        val station = FakeStation(history = historyPage(""), requests = "<form><input name=\"user_password\"></form>")

        val failure = runCatching { station.remote.load(stationId) }.exceptionOrNull()

        assertTrue(failure is ListenerActivityAuthenticationRequiredException)
        assertNull(station.store.loadDisplayName(stationId))
    }

    private class FakeStation(
        private val history: String,
        private val requests: String = "",
        private val requestsStatus: Int = HttpURLConnection.HTTP_OK,
        private val timer: String = "",
    ) {
        val requested = mutableListOf<String>()
        val store = InMemoryAuthSessionStore().apply {
            save(
                stationId,
                "streamingsoundtracks.com",
                listOf(HttpCookie("session", "protected").apply {
                    domain = "streamingsoundtracks.com"
                    path = "/"
                    secure = true
                }),
                "Listener",
            )
        }
        val remote = StationListenerActivityRemoteDataSource(sessionStore = store) { uri ->
            val target = uri.rawPath + (uri.rawQuery?.let { "?$it" } ?: "")
            synchronized(requested) { requested += target }
            when {
                target == HISTORY -> FakeConnection(uri.toURL(), history)
                target == REQUESTS -> FakeConnection(uri.toURL(), requests, requestsStatus)
                target == TIMER -> FakeConnection(uri.toURL(), timer)
                else -> FakeConnection(uri.toURL(), "", HttpURLConnection.HTTP_NOT_FOUND)
            }
        }
    }

    private class FakeConnection(
        url: URL,
        private val response: String,
        private val status: Int = HTTP_OK,
    ) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getContentType() = "text/html; charset=UTF-8"
        override fun getInputStream() = ByteArrayInputStream(response.toByteArray())
        override fun getHeaderFields(): MutableMap<String?, MutableList<String>> = mutableMapOf()
        override fun getHeaderField(name: String?): String? = null
        override fun setRequestProperty(key: String, value: String) = Unit
    }

    private companion object {
        val stationId = StationId("sst")
        const val HISTORY = "/modules.php?name=Your_Requests"
        const val REQUESTS = "/modules.php?name=Requests"
        const val TIMER = "/modules/VIP_Subscribe/vip_req_timer.php"
        const val CLOCK_READY = """
            <div class="vip-request-clock" data-seconds="0" data-queue-seconds="1054">
              <strong class="vip-request-value request-ready">Ready</strong>
            </div>
        """

        fun historyPage(sidebar: String) = """
            <html><body>
            <a href="/modules.php?name=Your_Account&op=logout">Logout</a>
            $sidebar
            <section class="request-history"><h2>Your Last 50 Requests</h2><table>
              <tr><td class="request-history-track"><strong>Opening</strong></td><td>Nov 24, 2026</td></tr>
            </table></section>
            </body></html>
        """.trimIndent()

        fun requestsPage(nextRequest: String) = """
            <div class="requests-page"><section class="requests-intro"><div class="request-summary">
              <div class="request-stat"><span>Your Next Request</span><strong>$nextRequest</strong></div>
              <div class="request-stat"><span>Member Wait</span><strong>20 minutes</strong></div>
            </div></section></div>
        """.trimIndent()
    }
}
