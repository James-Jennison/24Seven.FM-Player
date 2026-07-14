package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.RequestSuggestionMode
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
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
                    location = "http://www.streamingsoundtracks.com:80/modules.php?name=Album&action=writemessage&asin=B00005BG8G&id=2055716",
                )
                1 -> FakeConnection(
                    uri.toURL(),
                    """
                        Your request has successfully been delivered to the DJ application.
                        <form action="/modules.php?name=Album&amp;action=submitmessage&amp;asin=B00005BG8G&amp;id=2055716">
                          <textarea name="msg"></textarea>
                          <input name="send" type="submit" value="Send">
                          <input name="remLen" value="80" readonly>
                          <input name="messageToken" type="hidden" value="station-issued">
                        </form>
                    """.trimIndent(),
                )
                else -> FakeConnection(uri.toURL(), "Your message has been saved.")
            }.also(connections::add)
        }

        val result = remote.submit(
            stationId,
            RequestableTrack("B00005BG8G", "263260", "Just Testing", eligible = true),
            "Great choice!",
        )

        assertTrue(result is RequestSubmissionResult.Submitted)
        assertEquals(3, connections.size)
        assertEquals("GET", connections[0].requestMethod)
        assertTrue(connections[0].url.file.contains("songID=263260"))
        assertEquals("https", connections[1].url.protocol)
        assertEquals("POST", connections[2].requestMethod)
        assertEquals(
            "/modules.php?name=Album&action=submitmessage&asin=B00005BG8G&id=2055716",
            connections[2].url.file,
        )
        assertEquals(
            "send=Send&remLen=67&messageToken=station-issued&msg=Great+choice%21",
            connections[2].postedBody.toString(Charsets.UTF_8.name()),
        )
        assertEquals(
            "https://streamingsoundtracks.com/modules.php?name=Album&action=writemessage&asin=B00005BG8G&id=2055716",
            connections[2].capturedRequestProperties["Referer"],
        )
        assertEquals("https://streamingsoundtracks.com", connections[2].capturedRequestProperties["Origin"])
    }

    @Test
    fun `random suggestions use the verified station parameters`() = runTest {
        val connections = mutableListOf<FakeConnection>()
        val response = """
            <table><tr>
              <td><a href="/modules.php?name=Req&amp;asin=ALBUM_1&amp;songID=12345"><img src="/modules/SAM/images/requestbutton_request.png"></a></td>
              <td><a href="/modules.php?name=Album&amp;asin=ALBUM_1"><img src="/images/cover/040/ALBUM_1.jpg"></a></td>
              <td><b>Example Album - Composer</b><br>8. Suggested Track (1:24)</td>
            </tr></table>
        """.trimIndent()
        val remote = StationSongRequestRemoteDataSource { uri ->
            FakeConnection(uri.toURL(), response).also(connections::add)
        }

        val random = remote.suggest(stationId, RequestSuggestionMode.Random)
        val leastPlayed = remote.suggest(stationId, RequestSuggestionMode.LeastPlayed)

        assertEquals("Suggested Track", random.tracks.single().title)
        assertEquals("Suggested Track", leastPlayed.tracks.single().title)
        assertTrue(connections[0].url.query.contains("random=1"))
        assertTrue(connections[1].url.query.contains("randomleast=1"))
        assertTrue(connections.all { it.url.query.contains("searchpgstart=1") })
    }

    @Test
    fun `optional message does not wait for the rest of a large accepted page`() = runTest {
        val store = sessionStore()
        val connections = mutableListOf<FakeConnection>()
        val acceptedPrefix = """
            Your request has successfully been delivered to the DJ application.
            <form action="/modules.php?name=Album&amp;action=submitmessage&amp;asin=B00005BG8G&amp;id=2055716">
              <textarea name="msg"></textarea>
              <input name="send" type="submit" value="Send">
              <input name="remLen" value="80" readonly>
            </form>
        """.trimIndent()
        val remote = StationSongRequestRemoteDataSource(sessionStore = store) { uri ->
            when (connections.size) {
                0 -> FakeConnection(uri.toURL(), acceptedPrefix + "x".repeat(1_000_000))
                else -> FakeConnection(uri.toURL(), "Your message has been saved." + "x".repeat(1_000_000))
            }.also(connections::add)
        }

        val result = remote.submit(stationId, track(), "M10 Android app test")

        assertEquals(
            RequestSubmissionResult.Submitted(
                "The station reported the request and optional message saved. " +
                    "Confirm it in Queue before requesting again.",
            ),
            result,
        )
        assertEquals(2, connections.size)
        assertEquals("POST", connections[1].requestMethod)
    }

    @Test
    fun `message form without explicit request success is not posted`() = runTest {
        val connections = mutableListOf<FakeConnection>()
        val response = """
            <form action="/modules.php?name=Album&amp;action=submitmessage&amp;asin=B00005BG8G&amp;id=2055716">
              <textarea name="msg"></textarea>
              <input name="send" type="submit" value="Send">
              <input name="remLen" value="80" readonly>
            </form>
        """.trimIndent()
        val remote = StationSongRequestRemoteDataSource(sessionStore = sessionStore()) { uri ->
            FakeConnection(uri.toURL(), response).also(connections::add)
        }

        val failure = runCatching {
            remote.submit(stationId, track(), "M10 Android app test")
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertEquals("Unrecognized station request confirmation", failure?.message)
        assertEquals(1, connections.size)
        assertEquals("GET", connections.single().requestMethod)
    }

    @Test
    fun `large rejection page is classified before the response limit`() = runTest {
        val store = sessionStore()
        val connections = mutableListOf<FakeConnection>()
        val remote = StationSongRequestRemoteDataSource(sessionStore = store) { uri ->
            FakeConnection(
                uri.toURL(),
                "<h1>Request Failed</h1><p>This track was played recently.</p>" + "x".repeat(1_000_000),
            ).also(connections::add)
        }

        val result = remote.submit(stationId, track(), "M10 Android app test")

        assertTrue(result is RequestSubmissionResult.Rejected)
        assertEquals(1, connections.size)
    }

    @Test
    fun `indeterminate request does not guess message id or retry song`() = runTest {
        val store = sessionStore()
        val connections = mutableListOf<FakeConnection>()
        val remote = StationSongRequestRemoteDataSource(sessionStore = store) { uri ->
            FakeConnection(uri.toURL(), "", responseFailure = SocketTimeoutException("slow response"))
                .also(connections::add)
        }

        val failure = runCatching {
            remote.submit(stationId, track(), "M10 Android app test")
        }.exceptionOrNull()

        assertTrue(failure is SocketTimeoutException)
        assertEquals(1, connections.size)
        assertEquals("GET", connections[0].requestMethod)
        assertTrue(connections[0].url.file.contains("name=Req"))
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

    private fun track() = RequestableTrack("B00005BG8G", "263260", "Just Testing", eligible = true)

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
