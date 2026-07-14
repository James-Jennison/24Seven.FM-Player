package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL

class StationSongRequestRemoteDataSourceTest {
    @Test
    fun `optional message is posted once after accepted request`() = runTest {
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
        val connections = mutableListOf<FakeConnection>()
        val remote = StationSongRequestRemoteDataSource(sessionStore = store) { uri ->
            when (connections.size) {
                0 -> FakeConnection(
                    uri.toURL(),
                    "",
                    status = HttpURLConnection.HTTP_MOVED_TEMP,
                    location = "http://streamingsoundtracks.com/modules.php?name=Album&action=writemessage&asin=B0F1S53ZB6&id=2055693",
                )
                1 -> FakeConnection(
                    uri.toURL(),
                    "Your request has successfully been delivered to the DJ application.",
                )
                else -> FakeConnection(uri.toURL(), "Your message has been added.")
            }.also(connections::add)
        }

        val result = remote.submit(
            stationId,
            RequestableTrack("B0F1S53ZB6", "2055693", "Track", eligible = true),
            "Great choice!",
        )

        assertTrue(result is RequestSubmissionResult.Submitted)
        assertEquals(3, connections.size)
        assertEquals("GET", connections[0].requestMethod)
        assertEquals("https", connections[1].url.protocol)
        assertEquals("POST", connections[2].requestMethod)
        assertEquals(
            "/modules.php?name=Album&action=submitmessage&asin=B0F1S53ZB6&id=2055693",
            connections[2].url.file,
        )
        assertEquals(
            "msg=Great+choice%21&send=Send&remLen=67",
            connections[2].postedBody.toString(Charsets.UTF_8.name()),
        )
        assertEquals(
            "https://streamingsoundtracks.com/modules.php?name=Album&action=writemessage&asin=B0F1S53ZB6&id=2055693",
            connections[2].capturedRequestProperties["Referer"],
        )
    }

    @Test
    fun `indeterminate request sends message once without retrying song`() = runTest {
        val store = sessionStore()
        val connections = mutableListOf<FakeConnection>()
        val remote = StationSongRequestRemoteDataSource(sessionStore = store) { uri ->
            when (connections.size) {
                0 -> FakeConnection(uri.toURL(), "", responseFailure = SocketTimeoutException("slow response"))
                1 -> FakeConnection(
                    uri.toURL(),
                    """
                        <form action="/modules.php?name=Album&amp;action=submitmessage&amp;asin=B0F1S53ZB6&amp;id=2055693">
                          <textarea name="msg"></textarea>
                          <input name="send" type="submit" value="Send">
                          <input name="remLen" value="80" readonly>
                        </form>
                    """.trimIndent(),
                )
                else -> FakeConnection(uri.toURL(), "Message accepted")
            }.also(connections::add)
        }

        val result = remote.submit(stationId, track(), "M10 Android app test")

        assertTrue(result is RequestSubmissionResult.Submitted)
        assertEquals(3, connections.size)
        assertEquals("GET", connections[0].requestMethod)
        assertTrue(connections[0].url.file.contains("name=Req"))
        assertEquals("GET", connections[1].requestMethod)
        assertTrue(connections[1].url.file.contains("action=writemessage"))
        assertEquals("POST", connections[2].requestMethod)
        assertTrue(connections[2].url.file.contains("action=submitmessage"))
        assertEquals(
            "msg=M10+Android+app+test&send=Send&remLen=60",
            connections[2].postedBody.toString(Charsets.UTF_8.name()),
        )
    }

    private fun sessionStore() = InMemoryAuthSessionStore().apply {
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

    private fun track() = RequestableTrack("B0F1S53ZB6", "2055693", "Track", eligible = true)

    private class FakeConnection(
        url: URL,
        private val response: String,
        private val status: Int = HTTP_OK,
        private val location: String? = null,
        private val responseFailure: Exception? = null,
    ) : HttpURLConnection(url) {
        val postedBody = ByteArrayOutputStream()
        val capturedRequestProperties = mutableMapOf<String, String>()
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = responseFailure?.let { throw it } ?: status
        override fun getContentType() = "text/html; charset=UTF-8"
        override fun getInputStream() = ByteArrayInputStream(response.toByteArray())
        override fun getOutputStream() = postedBody
        override fun getHeaderFields(): MutableMap<String?, MutableList<String>> = mutableMapOf()
        override fun getHeaderField(name: String?): String? = if (name.equals("Location", true)) location else null
        override fun setRequestProperty(key: String, value: String) {
            capturedRequestProperties[key] = value
        }
    }

    private companion object {
        val stationId = StationId("sst")
    }
}
