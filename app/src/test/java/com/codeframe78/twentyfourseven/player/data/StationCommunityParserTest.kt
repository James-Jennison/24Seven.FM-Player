package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.CalendarEntry
import com.codeframe78.twentyfourseven.player.domain.CalendarEntryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.MonthDay

class StationCommunityParserTest {
    private val parser = StationCommunityParser()
    private val origin = "https://streamingsoundtracks.com/"

    @Test
    fun `reads who is online and the counts beside them`() {
        val block = parser.parseOnlineBlock(StationCommunityFixtures.onlineBlock(), origin)

        assertEquals(2, block.members.size)
        assertEquals("Listener", block.members[0].username)
        assertEquals("101", block.members[0].memberNumber)
        assertEquals("Canada", block.members[0].countryName)
        assertEquals("Member Three", block.members[1].username)
        assertEquals("202", block.members[1].memberNumber)
        assertNull(block.members[1].countryName)
        assertEquals(40, block.visitors)
        assertEquals(12345, block.totalMembers)
    }

    @Test
    fun `an online member linked to another origin has no member number`() {
        val html = StationCommunityFixtures.onlineBlock(
            listenerHref = "https://example.com/modules.php?name=Forums&file=profile&mode=viewprofile&u=101",
        )

        val block = parser.parseOnlineBlock(html, origin)

        assertEquals("Listener", block.members[0].username)
        assertNull(block.members[0].memberNumber)
        assertEquals("202", block.members[1].memberNumber)
    }

    @Test
    fun `a page without the online block yields nothing and does not throw`() {
        val empty = parser.parseOnlineBlock("<html><body><p>Maintenance</p></body></html>", origin)
        val broken = parser.parseOnlineBlock("<<<table><tr><td><u>Online Now:", origin)

        assertTrue(empty.members.isEmpty())
        assertNull(empty.visitors)
        assertNull(empty.totalMembers)
        assertTrue(broken.members.isEmpty())
        assertTrue(parser.parseOnlineBlock("", origin).members.isEmpty())
    }

    @Test
    fun `reads a members page row by row`() {
        val page = parser.parseMembersPage(StationCommunityFixtures.membersPage(), origin)

        assertEquals(3, page.members.size)
        val first = page.members[0]
        assertEquals("Listener", first.username)
        assertEquals("2", first.memberNumber)
        assertEquals("Springfield, IL", first.location)
        // The list names flags by file, which is not a country name.
        assertNull(first.countryName)
        assertEquals("Captain", first.rankTitle)
        assertEquals("Feb 12, 2002", first.joinedLabel)
        assertEquals("Sun Oct 04, 2026 6:40 pm", first.lastPostLabel)
        assertEquals(4942, first.posts)
        assertFalse(first.isOnline)
        assertTrue(first.isVip)

        val second = page.members[1]
        assertEquals("ComposerTwo", second.username)
        assertTrue(second.isOnline)
        assertFalse(second.isVip)
        assertNull(second.location)
        assertNull(second.countryName)
        assertEquals("Feb 14, 2002", second.joinedLabel)
        assertNull(second.lastPostLabel)
        assertEquals(1204, second.posts)

        assertEquals("Canada", page.members[2].countryName)
        assertEquals("Toronto", page.members[2].location)
        assertEquals(0, page.members[2].posts)
    }

    @Test
    fun `the next page is the one the pager calls Next`() {
        val first = parser.parseMembersPage(
            StationCommunityFixtures.membersPage(firstNumber = 1, pager = StationCommunityFixtures.firstPagePager()),
            origin,
        )
        val middle = parser.parseMembersPage(
            StationCommunityFixtures.membersPage(firstNumber = 51, pager = StationCommunityFixtures.middlePagePager()),
            origin,
        )

        assertEquals(50, first.nextStart)
        assertEquals(100, middle.nextStart)
    }

    @Test
    fun `the last page and a page without a pager have no next page`() {
        val last = parser.parseMembersPage(
            StationCommunityFixtures.membersPage(firstNumber = 351, pager = StationCommunityFixtures.lastPagePager()),
            origin,
        )
        val bare = parser.parseMembersPage(StationCommunityFixtures.membersPage(), origin)

        assertEquals(3, last.members.size)
        assertNull(last.nextStart)
        assertNull(bare.nextStart)
    }

    @Test
    fun `without a Next label the nearest later page is used`() {
        val pager = "Goto page <b>1</b>, " +
            """<a href="modules.php?name=Members_List&file=index&mode=joined&amp;start=100">3</a>, """ +
            """<a href="modules.php?name=Members_List&file=index&mode=joined&amp;start=50">2</a>"""

        val page = parser.parseMembersPage(StationCommunityFixtures.membersPage(pager = pager), origin)

        assertEquals(50, page.nextStart)
    }

    @Test
    fun `a pager link to another origin is not followed and a member link to another origin has no number`() {
        val foreignPager = """<a href="https://example.com/modules.php?name=Members_List&start=50">Next</a>"""
        val html = StationCommunityFixtures.membersPage(pager = foreignPager)
            .replace("modules.php?name=Forums&file=profile&mode=viewprofile&amp;u=2", "https://example.com/modules.php?name=Forums&file=profile&mode=viewprofile&amp;u=2")

        val page = parser.parseMembersPage(html, origin)

        assertNull(page.nextStart)
        assertEquals("Listener", page.members[0].username)
        assertNull(page.members[0].memberNumber)
        assertEquals("3", page.members[1].memberNumber)
    }

    @Test
    fun `a members page that is not a list yields nothing and does not throw`() {
        assertTrue(parser.parseMembersPage("<html><body>Not a list</body></html>", origin).members.isEmpty())
        assertTrue(parser.parseMembersPage("<<<tr><td>1<td>", origin).members.isEmpty())
        assertNull(parser.parseMembersPage("", origin).nextStart)
    }

    @Test
    fun `merges the calendar's columns by date and keeps calendar order`() {
        val days = parser.parseCalendar(StationCommunityFixtures.calendar(), origin)

        assertEquals(
            listOf(MonthDay.of(1, 1), MonthDay.of(2, 14), MonthDay.of(3, 3), MonthDay.of(12, 31)),
            days.map { it.day },
        )
        assertEquals(listOf("January 1", "February 14", "March 3", "December 31"), days.map { it.label })
        assertEquals(
            listOf(
                CalendarEntry("New Year's Day - Theme Day: Opening Titles Day", CalendarEntryKind.Event),
                CalendarEntry("ComposerOne (53)", CalendarEntryKind.Composer),
                CalendarEntry("ComposerTwo (61)", CalendarEntryKind.Composer),
                CalendarEntry("Listener (62)", CalendarEntryKind.Member, username = "Listener"),
                CalendarEntry("Member Three (40)", CalendarEntryKind.Member, username = "Member Three"),
            ),
            days[0].entries,
        )
        assertEquals(
            listOf(CalendarEntry("Sweetheart (30)", CalendarEntryKind.Member, username = "Sweetheart")),
            days[1].entries,
        )
        assertEquals(
            listOf(
                CalendarEntry("ComposerThree (70)", CalendarEntryKind.Composer),
                CalendarEntry("Listener (62)", CalendarEntryKind.Member, username = "Listener"),
            ),
            days[2].entries,
        )
        assertEquals(
            listOf(CalendarEntry("Theme Day: End Titles Day", CalendarEntryKind.Event)),
            days[3].entries,
        )
    }

    @Test
    fun `calendar rows with a date that does not parse are skipped`() {
        val days = parser.parseCalendar(StationCommunityFixtures.calendar(), origin)

        assertTrue(days.none { it.label == "February 30" || it.label == "Smarch 3" })
        assertTrue(days.flatMap { it.entries }.none { it.label.startsWith("Nobody") || it.label == "Not A Month" })
    }

    @Test
    fun `a member birthday linked to another origin keeps its label but not its name`() {
        val html = StationCommunityFixtures.calendar().replace(
            "modules.php?name=Your_Account&amp;op=userinfo&amp;username=Sweetheart",
            "https://example.com/modules.php?name=Your_Account&amp;op=userinfo&amp;username=Sweetheart",
        )

        val entry = parser.parseCalendar(html, origin).first { it.day == MonthDay.of(2, 14) }.entries.single()

        assertEquals(CalendarEntryKind.Member, entry.kind)
        assertEquals("Sweetheart (30)", entry.label)
        assertNull(entry.username)
    }

    @Test
    fun `the calendar keeps at most two thousand entries and cuts off by date`() {
        val rows = (1..300).joinToString("") { index ->
            val month = (index - 1) / 25 + 1
            val day = (index - 1) % 25 + 1
            val monthName = java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
            "<tr><td>$monthName $day</td><td>" + (1..10).joinToString("<br>") { "Composer $it ($index)" } + "</td></tr>"
        }

        val days = parser.parseCalendar("<table>$rows</table>", origin)

        assertEquals(2_000, days.sumOf { it.entries.size })
        assertEquals(200, days.size)
        assertEquals(MonthDay.of(8, 25), days.last().day)
    }

    @Test
    fun `a page without a calendar yields nothing and does not throw`() {
        assertTrue(parser.parseCalendar("<html><body>Closed</body></html>", origin).isEmpty())
        assertTrue(parser.parseCalendar("<<<tr><td>January 1<td>", origin).isEmpty())
        assertTrue(parser.parseCalendar("", origin).isEmpty())
    }
}
