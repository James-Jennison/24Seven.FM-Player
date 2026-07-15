package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ListenerActivityPageParserTest {
    private val parser = ListenerActivityPageParser()
    private val origin = "https://streamingsoundtracks.com/"

    @Test
    fun `discovery preserves ten explicit request summaries and trusted sources`() {
        val rows = (1..11).joinToString("") { position ->
            """<tr><td></td><td>$position. Album $position - Track $position - Artist $position</td><td>14 Jul 26 - 17:${position.toString().padStart(2, '0')}</td></tr>"""
        }
        val result = parser.parseDiscovery(
            """
                <html><body>
                <a href="/modules.php?name=Your_Account&op=logout">Logout</a>
                <iframe src="/modules/VIP_Subscribe/vip_req_timer.php"></iframe>
                <a href="/modules.php?name=Forums&amp;file=profile&amp;mode=viewprofile&amp;u=57">Listener</a>
                <table><tr><td><strong>Your Last 10 Requests</strong></td></tr>$rows</table>
                </body></html>
            """.trimIndent(),
            origin,
            "Listener",
        )

        assertEquals(10, result.recentRequests.size)
        assertEquals("Album 1 - Track 1 - Artist 1", result.recentRequests.first().trackSummary)
        assertEquals("14 Jul 26 - 17:01", result.recentRequests.first().requestedAtLabel)
        assertEquals(
            "https://streamingsoundtracks.com/modules/VIP_Subscribe/vip_req_timer.php",
            result.requestTimerUrl,
        )
        assertEquals(
            "https://streamingsoundtracks.com/modules.php?name=Forums&file=profile&mode=viewprofile&u=57",
            result.memberProfileUrl,
        )
    }

    @Test
    fun `discovery ignores cross-origin and unrecognized sources`() {
        val result = parser.parseDiscovery(
            """
                <html><body>
                <iframe src="https://example.com/modules/VIP_Subscribe/vip_req_timer.php"></iframe>
                <iframe src="/modules/VIP_Subscribe/not-a-timer.php"></iframe>
                <a href="https://example.com/modules.php?name=Forums&amp;file=profile&amp;mode=viewprofile&amp;u=57">Listener</a>
                <a href="/modules.php?name=Forums&amp;file=profile&amp;mode=viewprofile&amp;u=not-numeric">Listener</a>
                </body></html>
            """.trimIndent(),
            origin,
            "Listener",
        )

        assertNull(result.requestTimerUrl)
        assertNull(result.memberProfileUrl)
        assertEquals(emptyList<Any>(), result.recentRequests)
    }

    @Test
    fun `cooldown distinguishes ready and waiting evidence`() {
        assertEquals(
            RequestCooldownEvidence(RequestReadiness.Ready, 0),
            parser.parseCooldown("<div>Your Request: Ready</div><div>Request Wait: 0 Minutes</div>"),
        )
        assertEquals(
            RequestCooldownEvidence(RequestReadiness.Waiting, 37),
            parser.parseCooldown("<div>Your Request: Waiting</div><div>Request Wait: 37 Minutes</div>"),
        )
        assertEquals(
            RequestCooldownEvidence(RequestReadiness.Unknown, null),
            parser.parseCooldown("<div>Timer is temporarily unavailable</div>"),
        )
    }

    @Test
    fun `membership is read only from the matching profile table`() {
        val vip = parser.parseMembership(
            profilePage("Listener", "<a href=\"/modules.php?name=VIP_Subscribe\"><img alt=\"VIP\"></a>"),
            origin,
            "Listener",
        )
        val rip = parser.parseMembership(
            profilePage("Listener", "<a href=\"/modules.php?name=RIP_Subscribe\"><img alt=\"RIP\"></a>"),
            origin,
            "Listener",
        )
        val standard = parser.parseMembership(
            """
                <a href="/modules.php?name=VIP_Subscribe"><img alt="VIP"></a>
                ${profilePage("Listener", "<span>Listener</span>")}
            """.trimIndent(),
            origin,
            "Listener",
        )

        assertEquals(MembershipTier.Vip, vip)
        assertEquals(MembershipTier.Rip, rip)
        assertEquals(MembershipTier.Standard, standard)
    }

    @Test
    fun `login form is treated as expired authentication`() {
        assertThrows(ListenerActivityAuthenticationRequiredException::class.java) {
            parser.parseDiscovery("<form><input name=\"user_password\"></form>", origin, "Listener")
        }
    }

    private fun profilePage(displayName: String, membership: String) = """
        <table>
          <tr><th>Viewing profile :: $displayName</th></tr>
          <tr><td>Admiral (Administrator) $membership</td></tr>
        </table>
    """.trimIndent()
}
