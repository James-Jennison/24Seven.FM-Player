package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedStationNavigationTest {
    @Test
    fun `navigation accepts same-origin https and rejects every trust-boundary variant`() {
        val origin = URI("https://death.fm/")
        TrustedStationNavigation.requireSameHttpsOrigin(URI("https://death.fm/player.php"), origin)

        listOf(
            "http://death.fm/player.php",
            "https://example.com/player.php",
            "https://death.fm:444/player.php",
            "https://user@death.fm/player.php",
        ).forEach { candidate ->
            assertTrue(
                runCatching {
                    TrustedStationNavigation.requireSameHttpsOrigin(URI(candidate), origin)
                }.exceptionOrNull() is IOException,
            )
        }
        assertTrue(
            runCatching {
                TrustedStationNavigation.resolveRedirect(origin, "http://[", origin)
            }.exceptionOrNull() is IOException,
        )
    }

    @Test
    fun `queue follows bounded same-origin redirect and rejects cross-origin redirect`() = runTest {
        val acceptedConnections = mutableListOf<FakeConnection>()
        val accepted = PlayerQueueRemoteDataSource { uri ->
            val connection = if (acceptedConnections.isEmpty()) {
                FakeConnection(uri.toURL(), status = 302, location = "/player.php?redirected=1")
            } else {
                FakeConnection(uri.toURL(), response = "{\"queue_html\":\"\",\"played_html\":\"\"}")
            }
            connection.also(acceptedConnections::add)
        }

        val payload = accepted.fetch(StationId("dfm"))

        assertTrue(payload.upcoming.isEmpty())
        assertEquals(2, acceptedConnections.size)
        assertEquals("death.fm", acceptedConnections.last().url.host)

        val rejectedConnections = mutableListOf<FakeConnection>()
        val rejected = PlayerQueueRemoteDataSource { uri ->
            FakeConnection(uri.toURL(), status = 302, location = "https://example.com/collect")
                .also(rejectedConnections::add)
        }
        assertTrue(runCatching { rejected.fetch(StationId("dfm")) }.exceptionOrNull() is IOException)
        assertEquals(1, rejectedConnections.size)

        val loopingConnections = mutableListOf<FakeConnection>()
        val looping = PlayerQueueRemoteDataSource { uri ->
            FakeConnection(uri.toURL(), status = 302, location = "/player.php?loop=1")
                .also(loopingConnections::add)
        }
        assertTrue(runCatching { looping.fetch(StationId("dfm")) }.exceptionOrNull() is IOException)
        assertEquals(6, loopingConnections.size)
    }

    @Test
    fun `artwork follows bounded same-origin redirect and rejects downgrade`() = runTest {
        val acceptedConnections = mutableListOf<FakeConnection>()
        val accepted = StationNowPlayingArtworkRepository(connectionFactory = { uri ->
            val connection = if (acceptedConnections.isEmpty()) {
                FakeConnection(uri.toURL(), status = 307, location = "/soap/current.json")
            } else {
                FakeConnection(uri.toURL(), response = "{\"ASIN\":\"B00Q5M2SYS\"}")
            }
            connection.also(acceptedConnections::add)
        })

        assertEquals(
            "https://streamingsoundtracks.com/images/cover/500/B00Q5M2SYS.jpg",
            accepted.fetchArtwork(StationId("sst")),
        )
        assertEquals(2, acceptedConnections.size)

        val rejectedConnections = mutableListOf<FakeConnection>()
        val rejected = StationNowPlayingArtworkRepository(connectionFactory = { uri ->
            FakeConnection(uri.toURL(), status = 302, location = "http://streamingsoundtracks.com/soap/current.json")
                .also(rejectedConnections::add)
        })
        assertTrue(runCatching { rejected.fetchArtwork(StationId("sst")) }.exceptionOrNull() is IOException)
        assertEquals(1, rejectedConnections.size)
    }

    private class FakeConnection(
        url: URL,
        private val response: String = "",
        private val status: Int = HTTP_OK,
        private val location: String? = null,
    ) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getContentType() = "application/json; charset=UTF-8"
        override fun getInputStream() = ByteArrayInputStream(response.toByteArray())
        override fun getHeaderField(name: String?): String? = if (name.equals("Location", true)) location else null
    }
}
