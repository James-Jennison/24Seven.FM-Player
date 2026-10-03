package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationExtrasParserTest {
    private val parser = StationExtrasParser()
    private val origin = "https://streamingsoundtracks.com/"

    @Test
    fun `reads a profile card and keeps only station-hosted images`() {
        val profile = parser.parseProfile(StationExtrasFixtures.profileCard(), origin)!!

        assertEquals("Listener", profile.username)
        assertEquals("Apr 20, 2002", profile.memberSince)
        assertEquals("Springfield", profile.location)
        assertEquals("Captain", profile.rankTitle)
        assertEquals("VIP", profile.membership)
        assertTrue(profile.isOnline)
        assertEquals("https://streamingsoundtracks.com/images/avatars/listener.png", profile.avatarUrl)
        assertEquals(listOf("Public Favorites"), profile.badges)
        assertEquals("4821", profile.memberNumber)
        assertEquals("Public Favorites", profile.publicFavoritesBadge)
        assertEquals(ChatRole.Member, profile.role)
        assertEquals(12, profile.forumPosts)
        assertEquals("https://streamingsoundtracks.com/modules/MS_Analysis/images/flags/us.gif", profile.flagUrl)
    }

    @Test
    fun `reads the staff role and treats an unset field as missing`() {
        val card = StationExtrasFixtures.profileCard()
            .replace("\"nameClass\":\"\"", "\"nameClass\":\"admiralnick\"")
            .replace("\"location\":\"Springfield\"", "\"location\":false")

        val profile = parser.parseProfile(card, origin)!!

        assertEquals(ChatRole.Administrator, profile.role)
        assertNull(profile.location)
    }

    @Test
    fun `a member without the public favorites badge has no list to open`() {
        val card = StationExtrasFixtures.profileCard()
            .replace("Public Favorites", "Donor")
            .replace("/images/favorites/heart.svg", "/images/badges/donor.svg")

        val profile = parser.parseProfile(card, origin)!!

        assertEquals(listOf("Donor"), profile.badges)
        assertNull(profile.publicFavoritesBadge)
    }

    @Test
    fun `the public favorites badge is recognized by its picture when a station words it differently`() {
        val card = StationExtrasFixtures.profileCard()
            .replace("Public Favorites", "Shares favorites")
            .replace("/images/favorites/heart.svg", "/images/favorites/biohazard.svg")

        assertEquals("Shares favorites", parser.parseProfile(card, origin)!!.publicFavoritesBadge)
    }

    @Test
    fun `placeholder and off-site avatars are dropped and an unknown member yields no profile`() {
        assertNull(parser.parseProfile(StationExtrasFixtures.profileCard(avatar = "/images/avatar-default.svg"), origin)!!.avatarUrl)
        assertNull(parser.parseProfile(StationExtrasFixtures.profileCard(avatar = "https://example.com/a.png"), origin)!!.avatarUrl)
        assertNull(parser.parseProfile("""{"profile":null}""", origin))
    }

    @Test
    fun `reads history rows with times, requester, and request message`() {
        val entries = parser.parseHistory(StationExtrasFixtures.history(), origin)

        assertEquals(2, entries.size)
        with(entries[0]) {
            assertEquals("03:53:45", playedAtLabel)
            assertEquals("09:13", lengthLabel)
            assertEquals("Final Confrontation", title)
            assertEquals("Composer One", artistName)
            assertEquals("Album One", albumTitle)
            assertEquals("B004F4AQEC", albumId)
            assertEquals("https://streamingsoundtracks.com/images/cover/040/B004F4AQEC.jpg", artworkUrl)
            assertEquals("Listener", requesterName)
            assertEquals("Hey everyone", requestMessage)
        }
        with(entries[1]) {
            assertEquals("Opening", title)
            assertNull(requesterName)
            assertNull(requestMessage)
        }
    }

    @Test
    fun `reads a signed-in member's history rows, which carry an extra favorites cell`() {
        val entries = parser.parseHistory(StationExtrasFixtures.history(signedIn = true), origin)

        assertEquals(2, entries.size)
        assertEquals("Listener", entries[0].requesterName)
        assertEquals("Hey everyone", entries[0].requestMessage)
        assertNull(entries[1].requesterName)
    }

    @Test
    fun `history ignores the heading row and rows without a title or time`() {
        val broken = """{"result":0,"html":"<table><tr><th>Cover<th>Artist/Album/Title<th>Time/Length<th>Req By/Msg<tr><td><td>Artist<br><span>Album</span><td>03:00:00<br>02:00<td><i>System/Admin</i>"}"""

        assertEquals(emptyList<Any>(), parser.parseHistory(broken, origin))
    }

    @Test
    fun `reads news stories as plain text with line breaks and station covers`() {
        val stories = parser.parseNews(StationExtrasFixtures.newsPage(), origin)

        assertEquals(1, stories.size)
        with(stories.single()) {
            assertEquals("10000313", id)
            assertEquals("Playlist Update & Thanks", title)
            assertEquals("August 27th, 2026", publishedLabel)
            assertEquals("Editor", author)
            assertEquals(
                "Added the following albums to the playlist:\n\nFirst Album - Composer One *1\nSecond Album - Composer Two *1\n\nThanks to our contributors.",
                body,
            )
            assertEquals(listOf("https://streamingsoundtracks.com/images/cover/B0FQ689Q8F.jpg"), coverUrls)
            assertFalse(body.contains("<"))
        }
    }

    @Test
    fun `a news page without stories yields none`() {
        assertEquals(emptyList<Any>(), parser.parseNews("<html><body><p>The station is being upgraded.</p></body></html>", origin))
    }
}

internal object StationExtrasFixtures {
    fun profileCard(avatar: String = "/images/avatars/listener.png") = """
        {"profile":{"id":4821,"username":"Listener","avatar":"$avatar","flag":"/modules/MS_Analysis/images/flags/us.gif",
        "since":"Apr 20, 2002","location":"Springfield","posts":12,"nameClass":"",
        "contacts":[{"kind":"pm","label":"Private message","href":"/modules.php?name=Forums&file=privmsg&mode=post&u=4821"}],
        "online":true,"rank":{"title":"Captain","image":"/images/ranks/rank.gif"},
        "membershipIcon":"/images/vip-sst.svg","membership":"VIP",
        "badges":[{"icon":"&#10084;","label":"Public Favorites","image":"/images/favorites/heart.svg"}]}}
    """.trimIndent()

    fun history(signedIn: Boolean = false): String {
        // A signed-in member's rows end with a cell holding the station's own favorites controls.
        val favorites = if (signedIn) "<td width=\\\"80\\\" class=\\\"td01\\\"><a id=\\\"itno1\\\" href=\\\"javascript:AddTrack(1);\\\">Add</a>" else ""
        val html = "<table width=\\\"100%\\\"><tr>\\n" +
            "<th width=\\\"40\\\" class=\\\"th01\\\">Cover<th class=\\\"th01\\\">Artist/Album/Title<th width=\\\"80\\\" class=\\\"th01\\\">Time/Length<th width=\\\"200\\\" class=\\\"th01\\\">Req By/Msg<tr>\\n" +
            "<td width=\\\"40\\\" class=\\\"td01\\\"><a href=\\\"/modules.php?name=Album&amp;asin=B004F4AQEC\\\"><img src=\\\"/images/cover/040/B004F4AQEC.jpg\\\" alt=\\\"\\\" title=\\\"B004F4AQEC\\\"></a>" +
            "<td class=\\\"td01\\\">Composer One<br><span style=\\\"color:#ffff00;\\\">Album One</span><br><span style=\\\"font-weight:bold;\\\">Final Confrontation</span>" +
            "<td width=\\\"80\\\" class=\\\"td01\\\">03:53:45<br>09:13" +
            "<td width=\\\"200\\\" class=\\\"td01\\\"><a href=\\\"/modules.php?name=Your_Account&amp;op=userinfo&amp;username=Listener\\\">Listener<br></a>Hey everyone$favorites<tr>\\n" +
            "<td width=\\\"40\\\" class=\\\"td01\\\"><a href=\\\"/modules.php?name=Album&amp;asin=B09TY6GHMR\\\"><img src=\\\"/images/cover/040/B09TY6GHMR.jpg\\\" alt=\\\"\\\" title=\\\"B09TY6GHMR\\\"></a>" +
            "<td class=\\\"td01\\\">Composer Two<br><span style=\\\"color:#ffff00;\\\">Album Two</span><br><span style=\\\"font-weight:bold;\\\">Opening</span>" +
            "<td width=\\\"80\\\" class=\\\"td01\\\">03:48:56<br>04:53" +
            "<td width=\\\"200\\\" class=\\\"td01\\\"></b><i>System/Admin</i><b><br>$favorites"
        return "{\"result\":0,\"html\":\"$html\",\"sql\":\"\"}"
    }

    fun newsPage() = """
        <html><body><div class="news-listing news-view-news">
        <article class="news-story"><header class="th01"><h2><a href="/modules.php?name=News&amp;file=article&amp;sid=10000313">Playlist Update &amp; Thanks</a></h2></header>
        <table class="news-story-meta"><tr><td class="td01"><time datetime="2026-08-27T00:31:32">August 27th, 2026 - 00:31</time> &middot; Published by <a href="/modules.php?name=Forums&amp;file=profile&amp;mode=viewprofile&amp;username=Editor">Editor</a></td></tr></table>
        <div class="news-story-body td01"><div class="news-story-layout">
        <aside class="news-selected-covers"><a href="/modules.php?name=Album&amp;asin=B0FQ689Q8F"><img src="/images/cover/B0FQ689Q8F.jpg" alt="Album cover"></a><img src="https://example.com/tracker.gif"></aside>
        <div class="news-full-text"><b>Added the following albums to the playlist:</b><br />
         <br />
        <a href="/modules.php?name=Album&amp;asin=B0FMDDCMX7">First Album - Composer One</a>  *1<br />
        <a href="/modules.php?name=Album&amp;asin=B0FQ689Q8F">Second Album - Composer Two</a>  *1<br />
         <br />
        Thanks to our contributors.</div></div></div></article>
        </div></body></html>
    """.trimIndent()
}
