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
    fun `discovery reads the current page's request list, request clock, and queued request`() {
        val result = parser.parseDiscovery(currentPage(seconds = "0", word = "Ready", queueSeconds = "1054"), origin, "Listener")

        assertEquals(3, result.recentRequests.size)
        assertEquals("Untitled track — Composer Four", result.recentRequests[2].trackSummary)
        assertEquals("Album Four", result.recentRequests[2].albumTitle)
        with(result.recentRequests[0]) {
            assertEquals(1, position)
            assertEquals("Opening — Composer Two", trackSummary)
            assertEquals("2026-10-02 16:16:02", requestedAtLabel)
            assertEquals("Album Two", albumTitle)
            assertEquals("B000000002", albumId)
            assertEquals("https://streamingsoundtracks.com/images/cover/040/B000000002.jpg", artworkUrl)
        }
        assertEquals(2, result.recentRequests[1].position)
        assertEquals("Finale — Composer Three", result.recentRequests[1].trackSummary)
        assertEquals(RequestReadiness.Ready, result.cooldown?.readiness)
        assertNull(result.cooldown?.waitMinutes)
        assertEquals(1054, result.cooldown?.queuedRequestWaitSeconds)
    }

    @Test
    fun `the request clock reports a wait in whole minutes and no queued request when none is queued`() {
        val waiting = parser.parseDiscovery(currentPage(seconds = "125", word = "02:05", queueSeconds = ""), origin, "Listener")

        assertEquals(RequestReadiness.Waiting, waiting.cooldown?.readiness)
        assertEquals(3, waiting.cooldown?.waitMinutes)
        assertNull(waiting.cooldown?.queuedRequestWaitSeconds)

        // Zero seconds without the station's own "Ready" is not treated as ready.
        val unclear = parser.parseDiscovery(currentPage(seconds = "0", word = "Checking…", queueSeconds = ""), origin, "Listener")
        assertEquals(RequestReadiness.Unknown, unclear.cooldown?.readiness)
    }

    @Test
    fun `membership follows the wording on the member's profile card`() {
        assertEquals(MembershipTier.Vip, parser.membershipTier("VIP"))
        assertEquals(MembershipTier.Rip, parser.membershipTier("RIP"))
        assertEquals(MembershipTier.Standard, parser.membershipTier(null))
        assertEquals(MembershipTier.Standard, parser.membershipTier("Member"))
    }

    private fun currentPage(seconds: String, word: String, queueSeconds: String) = """
        <html><body>
        <a href="/modules.php?name=Your_Account&op=logout">Logout</a>
        <table><tr><th class="th01"><h2>VIP</h2></th></tr><tr><td class="td01">
          <div class="vip-request-clock" data-seconds="$seconds" data-queue-seconds="$queueSeconds" data-queue-status="Queued">
            <div><span class="vip-next-dot vip-status-dot is-ready">●</span> Next Request:
              <strong class="vip-request-value request-ready">$word</strong> <span class="vip-request-detail"></span></div>
            <div><span class="vip-play-dot vip-status-dot is-waiting">●</span> Request Play:
              <strong class="vip-queue-value">17:34</strong></div>
          </div>
        </td></tr></table>
        <section class="request-history"><h2>Your Last 50 Requests</h2>
        <table>
          <tr><th class="th01">Track / Album</th><th class="th01">Requested</th><th class="th01">Favorites</th></tr>
          <tr>
            <td class="td01"><div class="request-history-track"><img src="/images/cover/040/B000000002.jpg" width="40" height="40" alt=""><div><strong>Opening</strong><br>Composer Two<br><a href="/modules.php?name=Album&amp;asin=B000000002">Album Two</a></div></div></td>
            <td class="td01">2026-10-02 16:16:02</td>
            <td class="td01"><a id="request-saved-1" style="display:none" href="/modules.php?name=Favorites&amp;song2view=1">Saved</a><button id="request-add-1" type="button">Add favorite</button></td>
          </tr>
          <tr>
            <td class="td01"><div class="request-history-track"><img src="https://example.com/cover.jpg" alt=""><div><strong>Finale</strong><br>Composer Three<br><a href="https://example.com/modules.php?name=Album&amp;asin=B000000003">Album Three</a></div></div></td>
            <td class="td01">2026-10-02 14:23:17</td>
            <td class="td01"></td>
          </tr>
          <tr>
            <td class="td01"><div class="request-history-track"><img src="/images/cover/040/B000000004.jpg" alt=""><div><strong></strong><br>Composer Four<br><a href="/modules.php?name=Album&amp;asin=B000000004">Album Four</a></div></div></td>
            <td class="td01">2026-08-22 12:35:26</td>
            <td class="td01"></td>
          </tr>
        </table></section>
        </body></html>
    """.trimIndent()

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
    fun `membership is read from the current profile card not site navigation`() {
        val vip = parser.parseMembership(
            currentProfilePage("<a href=\"/modules.php?name=VIP_Subscribe\"><img alt=\"VIP\"></a>"),
            origin,
            "Listener",
        )
        val standard = parser.parseMembership(currentProfilePage(""), origin, "Listener")

        assertEquals(MembershipTier.Vip, vip)
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

    private fun currentProfilePage(membership: String) = """
        <a href="/modules.php?name=VIP_Subscribe">Site-wide membership navigation</a>
        <table><tr>
          <td><div><table class="table01"><tr><td>$membership</td></tr></table></div></td>
          <td><div class="profile-tabs"><button>Favorites</button></div></td>
        </tr></table>
    """.trimIndent()
}
