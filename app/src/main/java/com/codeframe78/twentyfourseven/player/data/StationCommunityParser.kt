package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.CalendarDay
import com.codeframe78.twentyfourseven.player.domain.CalendarEntry
import com.codeframe78.twentyfourseven.player.domain.CalendarEntryKind
import com.codeframe78.twentyfourseven.player.domain.MemberSummary
import com.codeframe78.twentyfourseven.player.domain.OnlineMember
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI
import java.net.URLDecoder
import java.time.Month
import java.time.MonthDay
import java.time.format.TextStyle
import java.util.Locale

/** The stations' Online Now block, with the visitor and membership counts printed beside it. */
internal data class OnlineBlock(val members: List<OnlineMember>, val visitors: Int?, val totalMembers: Int?)

/** One page of the members list and the offset of the page after it, when there is one. */
internal data class MembersPage(val members: List<MemberSummary>, val nextStart: Int?)

/** Reads the stations' public community pages: who is online, the members list, and the birthdays calendar. */
internal class StationCommunityParser {
    /** The members in the Online Now block of a station page; every field is null or empty when the block is missing. */
    fun parseOnlineBlock(html: String, origin: String): OnlineBlock =
        runCatching {
            val document = Jsoup.parse(html, origin)
            val text = document.text()
            OnlineBlock(
                members = onlineMembers(document, origin),
                visitors = VISITORS.find(text)?.groupValues?.get(1)?.let(::count),
                // The "Overall" line under Membership counts every registered member.
                totalMembers = OVERALL_MEMBERS.find(text)?.groupValues?.get(1)?.let(::count),
            )
        }.getOrDefault(OnlineBlock(emptyList(), null, null))

    /** The members on one page of the members list, in the station's order. */
    fun parseMembersPage(html: String, origin: String): MembersPage =
        runCatching {
            val document = Jsoup.parse(html, origin)
            val rows = document.select("tr").mapNotNull { row ->
                val cells = row.select("> td")
                val number = cells.takeIf { it.size == MEMBER_CELLS }?.get(0)?.ownText()?.trim()
                    ?.takeIf { it.matches(DIGITS) }?.toIntOrNull()
                if (number == null) null else number to cells
            }
            val members = rows.mapNotNull { (_, cells) -> memberSummary(cells, origin) }.take(MAX_MEMBERS)
            // The page does not print its own offset, but its rows are numbered from it.
            val offset = rows.firstOrNull()?.first?.minus(1)?.coerceAtLeast(0)
            MembersPage(members, offset?.let { nextStart(document, origin, it) })
        }.getOrDefault(MembersPage(emptyList(), null))

    /** The theme days, composer birthdays, and member birthdays by date, from January 1 on. */
    fun parseCalendar(html: String, origin: String): List<CalendarDay> =
        runCatching {
            val days = sortedMapOf<MonthDay, CalendarDayBuilder>()
            for (row in Jsoup.parse(html, origin).select("tr")) {
                val cells = row.select("> td")
                // The page nests its two columns in a table of their own; only the innermost rows hold dates.
                if (cells.size != 2 || cells[0].selectFirst("table") != null) continue
                val dateLabel = collapse(cells[0].text())
                val day = monthDay(dateLabel) ?: continue
                val entries = calendarLines(cells[1]).mapNotNull { calendarEntry(it, origin) }
                if (entries.isEmpty()) continue
                days.getOrPut(day) { CalendarDayBuilder(dateLabel.take(MAX_LABEL_CHARACTERS)) }.entries += entries
            }
            var remaining = MAX_CALENDAR_ENTRIES
            days.mapNotNull { (day, builder) ->
                // Trimming by date keeps the calendar complete up to the point where it is cut off.
                val entries = builder.entries.sortedBy { it.kind.ordinal }.take(remaining)
                remaining -= entries.size
                if (entries.isEmpty()) null else CalendarDay(day, builder.label, entries)
            }
        }.getOrDefault(emptyList())

    private fun onlineMembers(document: Element, origin: String): List<OnlineMember> {
        // Other parts of the page also link member profiles, so read only what follows the "Online Now" heading.
        val marker = document.select("u").firstOrNull { it.text().trim().startsWith("Online Now", ignoreCase = true) }
            ?: return emptyList()
        var node: Node? = marker
        while (node != null && node.nextSibling() == null) node = node.parent()
        node = node?.nextSibling()
        val members = mutableListOf<OnlineMember>()
        var flag: Element? = null
        while (node != null && members.size < MAX_ONLINE) {
            if (node is Element) {
                val name = node.normalName()
                when {
                    name == "hr" -> break
                    name == "br" -> flag = null
                    name == "img" && "/flags/" in node.attr("src") -> flag = node
                    name == "a" && PROFILE_LINK in node.attr("href") -> {
                        val username = node.text().trim().takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARACTERS }
                        if (username != null) {
                            members += OnlineMember(username, memberNumber(node.attr("href"), origin), countryName(flag))
                        }
                    }
                }
            }
            node = node.nextSibling()
        }
        return members
    }

    private fun memberSummary(cells: List<Element>, origin: String): MemberSummary? {
        val identity = cells[1]
        val link = identity.selectFirst("a[href*=mode=viewprofile]") ?: return null
        val username = link.text().trim().takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARACTERS } ?: return null
        val dates = cells[3].textNodes().map { it.text().trim() }.filter(String::isNotEmpty)
        return MemberSummary(
            username = username,
            memberNumber = memberNumber(link.attr("href"), origin),
            // The location is the plain text beside the name; the name and the flag are elements.
            location = collapse(identity.textNodes().joinToString("") { it.text() })
                .takeIf(String::isNotEmpty)?.take(MAX_LABEL_CHARACTERS),
            countryName = countryName(identity.selectFirst("img[src*=/flags/]")),
            rankTitle = cells[2].text().trim().ifEmpty { cells[2].selectFirst("img")?.attr("alt")?.trim().orEmpty() }
                .takeIf(String::isNotEmpty)?.take(MAX_LABEL_CHARACTERS),
            joinedLabel = dates.getOrNull(0)?.takeUnless { it.equals(NO_DATE, ignoreCase = true) }
                ?.take(MAX_LABEL_CHARACTERS),
            lastPostLabel = dates.getOrNull(1)?.takeUnless { it.equals(NO_DATE, ignoreCase = true) }
                ?.take(MAX_LABEL_CHARACTERS),
            posts = count(cells[4].text()),
            isOnline = cells[0].select("img").any { it.attr("alt").trim().endsWith("is online", ignoreCase = true) },
            isVip = identity.select("img").any { it.attr("alt").trim().equals("VIP", ignoreCase = true) },
        )
    }

    /** The offset of the page after this one: the "Next" link, else the nearest later page the pager links to. */
    private fun nextStart(document: Element, origin: String, offset: Int): Int? {
        val later = document.select("a[href*=start=]").mapNotNull { link ->
            val query = stationUrl(link.attr("href"), origin)?.let { runCatching { URI(it).rawQuery }.getOrNull() }
            if (query == null || LIST_PAGE !in query) return@mapNotNull null
            val start = PAGER_START.find(query)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > offset }
                ?: return@mapNotNull null
            start to link.text().trim()
        }
        return later.firstOrNull { it.second.equals("Next", ignoreCase = true) }?.first
            ?: later.minOfOrNull { it.first }
    }

    private class CalendarLine {
        val text = StringBuilder()
        val bold = StringBuilder()
        var memberLink: Element? = null
    }

    private class CalendarDayBuilder(val label: String) {
        val entries = mutableListOf<CalendarEntry>()
    }

    /** One text line per `<br>`-separated line of a calendar cell, remembering what was bold and any member link. */
    private fun calendarLines(cell: Element): List<CalendarLine> {
        val lines = mutableListOf(CalendarLine())
        fun visit(node: Node, bold: Boolean) {
            when {
                node is TextNode -> {
                    lines.last().text.append(node.text())
                    if (bold) lines.last().bold.append(node.text())
                }
                node is Element && node.normalName() == "br" -> lines.add(CalendarLine())
                node is Element -> {
                    if (node.normalName() == "a" && USERNAME_PARAMETER in node.attr("href") && lines.last().memberLink == null) {
                        lines.last().memberLink = node
                    }
                    val childrenBold = bold || node.normalName() == "b"
                    node.childNodes().forEach { visit(it, childrenBold) }
                }
            }
        }
        cell.childNodes().forEach { visit(it, false) }
        return lines
    }

    private fun calendarEntry(line: CalendarLine, origin: String): CalendarEntry? {
        val label = collapse(line.text.toString()).takeIf(String::isNotEmpty)?.take(MAX_LABEL_CHARACTERS) ?: return null
        val link = line.memberLink
        return when {
            link != null -> CalendarEntry(label, CalendarEntryKind.Member, username = linkedUsername(link.attr("href"), origin))
            collapse(line.bold.toString()).isNotEmpty() -> CalendarEntry(label, CalendarEntryKind.Event)
            else -> CalendarEntry(label, CalendarEntryKind.Composer)
        }
    }

    private fun linkedUsername(href: String, origin: String): String? = runCatching {
        val query = URI(stationUrl(href, origin) ?: return null).rawQuery ?: return null
        val value = USERNAME_QUERY.find(query)?.groupValues?.get(1) ?: return null
        URLDecoder.decode(value, "UTF-8").trim().takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARACTERS }
    }.getOrNull()

    /** "January 1" as printed on the page, or null for anything that is not a real calendar date. */
    private fun monthDay(label: String): MonthDay? {
        val match = DATE_LABEL.matchEntire(label) ?: return null
        val month = Month.values().firstOrNull {
            it.getDisplayName(TextStyle.FULL, Locale.ENGLISH).equals(match.groupValues[1], ignoreCase = true)
        } ?: return null
        return runCatching { MonthDay.of(month, match.groupValues[2].toInt()) }.getOrNull()
    }

    private fun memberNumber(href: String, origin: String): String? = runCatching {
        val query = URI(stationUrl(href, origin) ?: return null).rawQuery ?: return null
        MEMBER_ID.find(query)?.groupValues?.get(1)
    }.getOrNull()

    /** The country a flag names. The members list labels flags with their file names, which are not names. */
    private fun countryName(flag: Element?): String? {
        flag ?: return null
        val name = flag.attr("title").trim().ifEmpty { flag.attr("alt").trim() }
        return name.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARACTERS && !it.matches(IMAGE_FILE_NAME) }
    }

    private fun count(value: String): Int? =
        value.trim().replace(",", "").takeIf { it.matches(DIGITS) }?.toIntOrNull()

    private fun collapse(value: String): String = value.replace(SPACES, " ").trim()

    private fun stationUrl(value: String, origin: String): String? = runCatching {
        val base = URI(origin)
        val uri = base.resolve(value.trim())
        uri.takeIf {
            it.scheme == "https" && it.userInfo == null &&
                it.host.equals(base.host, ignoreCase = true) && it.port == base.port
        }?.toASCIIString()
    }.getOrNull()

    private companion object {
        const val MAX_ONLINE = 200
        const val MAX_MEMBERS = 200
        const val MAX_CALENDAR_ENTRIES = 2_000
        const val MAX_NAME_CHARACTERS = 60
        const val MAX_LABEL_CHARACTERS = 200
        const val MEMBER_CELLS = 5
        const val NO_DATE = "None"
        const val PROFILE_LINK = "mode=viewprofile"
        const val USERNAME_PARAMETER = "username="
        const val LIST_PAGE = "name=Members_List"
        val VISITORS = Regex("""Visitors:\s*([\d,]{1,12})""")
        val OVERALL_MEMBERS = Regex("""Overall:\s*([\d,]{1,12})""")
        val DIGITS = Regex("""\d{1,9}""")
        val MEMBER_ID = Regex("""(?:^|&)u=(\d{1,10})(?:&|$)""")
        val PAGER_START = Regex("""(?:^|&)start=(\d{1,9})(?:&|$)""")
        val USERNAME_QUERY = Regex("""(?:^|&)username=([^&]*)""")
        val DATE_LABEL = Regex("""([A-Za-z]{3,9})\s+(\d{1,2})""")
        val IMAGE_FILE_NAME = Regex(""".*\.(gif|png|jpe?g|svg|webp|ico)""", RegexOption.IGNORE_CASE)
        val SPACES = Regex("""[\s ]+""")
    }
}
