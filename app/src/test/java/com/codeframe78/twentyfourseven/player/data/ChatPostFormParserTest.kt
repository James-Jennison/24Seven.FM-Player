package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ChatPostFormParserTest {
    private val parser = ChatPostFormParser()
    private val origin = "https://streamingsoundtracks.com/"

    @Test
    fun `accepts one exact same-origin public input frame`() {
        val result = parser.parse(
            "<iframe name=inputframe src='modules/ClearChat/block-files/input.php?username=Listener&userpw=test-token'></iframe>",
            origin,
        )

        assertEquals("Listener", result.username)
        assertEquals("test-token", result.token)
        assertEquals("ChatPostCredentials([redacted])", result.toString())
    }

    @Test
    fun `rejects cross-origin input frame`() {
        assertThrows(IOException::class.java) {
            parser.parse(
                "<iframe name=inputframe src='https://example.com/modules/ClearChat/block-files/input.php?username=a&userpw=b'></iframe>",
                origin,
            )
        }
    }

    @Test
    fun `rejects duplicate or missing secret fields`() {
        assertThrows(IOException::class.java) {
            parser.parse(
                "<iframe name=inputframe src='modules/ClearChat/block-files/input.php?username=a&userpw=b&userpw=c'></iframe>",
                origin,
            )
        }
    }
}
