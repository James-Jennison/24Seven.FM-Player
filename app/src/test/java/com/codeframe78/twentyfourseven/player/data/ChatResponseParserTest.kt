package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.codeframe78.twentyfourseven.player.domain.ChatMessagePart

class ChatResponseParserTest {
    private val parser = ChatResponseParser()

    @Test
    fun `parses explicit author message timestamp and smiley text`() {
        val result = parser.parse(
            """
                <div class="msg-row">
                  <span class="nick" title="Posted 13 Jul 26 - 19:04:56">Listener:</span>
                  <span class="say" title="Posted 13 Jul 26 - 19:04:56">Hello <img src="../common/smilies/default/smile.gif" alt=":)"></span>
                </div>
            """.trimIndent(),
            "https://streamingsoundtracks.com/modules/ClearChat/block-files/view.php",
        )

        assertEquals(1, result.size)
        assertEquals("Listener", result.single().authorDisplayName)
        assertEquals("Hello :)", result.single().messageText)
        assertEquals("13 Jul 26 - 19:04:56", result.single().postedAtLabel)
        assertEquals(
            listOf(
                ChatMessagePart.Text("Hello "),
                ChatMessagePart.Emoticon(
                    ":)",
                    "https://streamingsoundtracks.com/modules/ClearChat/common/smilies/default/smile.gif",
                ),
            ),
            result.single().parts,
        )
    }

    @Test
    fun `preserves same station phpBB emoticons and rejects untrusted images`() {
        val result = parser.parse(
            """
                <div class="msg-row">
                  <span class="nick">Listener:</span>
                  <span class="say"><img src="../common/smilies/default/heart.gif" alt="heart"> &amp;#128064; <img src="https://example.com/tracker.gif" alt="tracker"></span>
                </div>
            """.trimIndent(),
            "https://streamingsoundtracks.com/modules/ClearChat/block-files/view.php",
        )

        assertEquals("heart 👀", result.single().messageText)
        assertEquals(
            listOf(
                ChatMessagePart.Emoticon(
                    "heart",
                    "https://streamingsoundtracks.com/modules/ClearChat/common/smilies/default/heart.gif",
                ),
                ChatMessagePart.Text(" 👀"),
            ),
            result.single().parts,
        )
    }

    @Test
    fun `ignores unrelated markup and incomplete rows`() {
        val result = parser.parse(
            """
                <script>secret()</script>
                <div class="msg-row"><span class="nick">Listener:</span></div>
                <div class="other"><span class="nick">Other:</span><span class="say">Not chat</span></div>
            """.trimIndent(),
            "https://streamingsoundtracks.com/",
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `limits retained message rows`() {
        val html = (1..60).joinToString("") { index ->
            "<div class=msg-row><span class=nick>User:</span><span class=say>Message $index</span></div>"
        }

        assertEquals(50, parser.parse(html, "https://streamingsoundtracks.com/").size)
    }
}
