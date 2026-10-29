package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.codeframe78.twentyfourseven.player.domain.ChatMessagePart
import com.codeframe78.twentyfourseven.player.domain.ChatRole

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
    fun `reads the role a nick is coloured by`() {
        val result = parser.parse(
            """
                <div class="msg-row"><span class="nick">Listener: </span><span class="say">one</span></div>
                <div class="msg-row"><span class="fleetadmiralnick">Owner: </span><span class="say">two</span></div>
                <div class="msg-row"><span class="admiralnick">Admin: </span><span class="say">three</span></div>
                <div class="msg-row"><span class="viceadmiralnick">Mod: </span><span class="say">four</span></div>
                <div class="msg-row"><span class="rearadmiralnick">Envoy: </span><span class="say">five</span></div>
                <div class="msg-row"><span class="visitormodnick">Guest Mod: </span><span class="say">six</span></div>
                <div class="msg-row"><span class="composernick">Composer Two: </span><span class="say">seven</span></div>
            """.trimIndent(),
            "https://streamingsoundtracks.com/modules/ClearChat/block-files/view.php",
        )

        assertEquals(
            listOf(
                ChatRole.Member, ChatRole.Proprietor, ChatRole.Administrator, ChatRole.Moderator,
                ChatRole.Ambassador, ChatRole.VisitorMod, ChatRole.Composer,
            ),
            result.map { it.authorRole },
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
    fun `keeps station-smilies module pictures with their declared shape`() {
        val result = parser.parse(
            """
                <div class="msg-row">
                  <span class="admiralnick" title="Posted 14 Oct 26 - 08:25:44">MorG: </span>
                  <span class="say" title="Posted 14 Oct 26 - 08:25:44"><picture class="station-smiley station-smiley-wide" data-smiley="Moto01" data-regular="/modules/ClearChat/common/station-smilies/originals/Moto01.gif"><source media="(prefers-reduced-motion: reduce)" srcset="/modules/ClearChat/common/station-smilies/originals/Moto01.png"><img src="/modules/ClearChat/common/station-smilies/originals/Moto01.gif" alt="-Moto01-" width="72" height="32"></picture> <picture class="station-smiley"><img src="/modules/ClearChat/common/station-smilies/assets/sst/regular/heart.gif" alt="-heart-" width="20" height="20"></picture><img src="/modules/ClearChat/common/station-smilies/station/stationafm.ico" alt="-stationafm-" width="0" height="abc"></span>
                </div>
            """.trimIndent(),
            "https://streamingsoundtracks.com/modules/ClearChat/block-files/view.php",
        )

        val message = result.single()
        assertEquals("-Moto01- -heart--stationafm-", message.messageText)
        assertEquals("14 Oct 26 - 08:25:44", message.postedAtLabel)
        assertEquals(
            listOf(
                ChatMessagePart.Emoticon(
                    "-Moto01-",
                    "https://streamingsoundtracks.com/modules/ClearChat/common/station-smilies/originals/Moto01.gif",
                    widthPx = 72,
                    heightPx = 32,
                ),
                ChatMessagePart.Text(" "),
                ChatMessagePart.Emoticon(
                    "-heart-",
                    "https://streamingsoundtracks.com/modules/ClearChat/common/station-smilies/assets/sst/regular/heart.gif",
                ),
                ChatMessagePart.Emoticon(
                    "-stationafm-",
                    "https://streamingsoundtracks.com/modules/ClearChat/common/station-smilies/station/stationafm.ico",
                ),
            ),
            message.parts,
        )
        assertEquals(2.25f, message.parts.filterIsInstance<ChatMessagePart.Emoticon>().first().aspectRatio)
    }

    @Test
    fun `a picture only message is confirmed by its own pictures, not by any picture only row`() {
        val rows = parser.parse(
            """
                <div class="msg-row"><span class="nick">MorG: </span><span class="say"><img src="/modules/ClearChat/common/station-smilies/originals/Moto01.gif" alt="-Moto01-" width="72" height="32"></span></div>
            """.trimIndent(),
            "https://streamingsoundtracks.com/modules/ClearChat/block-files/view.php",
        )

        assertTrue(rows[0].confirmsSubmission("-Moto01-"))
        assertFalse(rows[0].confirmsSubmission("-Other-"))
        assertFalse(rows[0].confirmsSubmission("-Moto01- -Moto01-"))
        assertFalse(rows[0].confirmsSubmission("   "))
    }

    @Test
    fun `confirms the station's rendering of a submitted message`() {
        val rows = parser.parse(
            """
                <div class="msg-row"><span class="nick">MorG: </span><span class="say"><picture><img src="/modules/ClearChat/common/station-smilies/originals/Moto01.gif" alt="-Moto01-" width="72" height="32"></picture> vroom <img src="/modules/ClearChat/common/station-smilies/assets/sst/regular/smile.gif" alt=":)" width="20" height="20"></span></div>
                <div class="msg-row"><span class="nick">MorG: </span><span class="say">Dash &amp;#8212; and &amp;#128171; ok</span></div>
                <div class="msg-row"><span class="nick">MorG: </span><span class="say">plain words</span></div>
            """.trimIndent(),
            "https://streamingsoundtracks.com/modules/ClearChat/block-files/view.php",
        )

        assertTrue(rows[0].confirmsSubmission("-Moto01- vroom :)"))
        assertTrue(rows[1].confirmsSubmission("Dash &#8212; and &#128171; ok"))
        assertTrue(rows[2].confirmsSubmission("plain words"))
        assertFalse(rows[2].confirmsSubmission("other words"))
        assertFalse(rows[0].confirmsSubmission("-Moto01- vroom and more"))
    }

    @Test
    fun `encodes characters outside Latin-1 for the station`() {
        assertEquals("Dash &#8212; and &#128171; caf\u00e9", "Dash — and 💫 café".toStationChatWire())
        assertEquals("plain -smile-", "plain -smile-".toStationChatWire())
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
    fun `submitted text with repeated whitespace matches the message the station shows`() {
        val submitted = "hi  there\tfriend "
        val shown = parser.parse(
            "<div class=msg-row><span class=nick>Listener:</span><span class=say>hi  there\tfriend</span></div>",
            "https://streamingsoundtracks.com/",
        ).single().messageText

        assertEquals("hi there friend", shown)
        assertEquals(shown, submitted.toSubmittedChatVisibleText())
    }

    @Test
    fun `limits retained message rows`() {
        val html = (1..60).joinToString("") { index ->
            "<div class=msg-row><span class=nick>User:</span><span class=say>Message $index</span></div>"
        }

        assertEquals(50, parser.parse(html, "https://streamingsoundtracks.com/").size)
    }
}
