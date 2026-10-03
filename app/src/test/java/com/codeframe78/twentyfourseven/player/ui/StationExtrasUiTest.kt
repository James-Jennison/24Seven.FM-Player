package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.MemberProfile
import com.codeframe78.twentyfourseven.player.domain.currentPlayedHistoryBlock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class StationExtrasUiTest {
    @Test
    fun `history block labels read as clock ranges`() {
        assertEquals("12:00 – 2:00 AM", historyBlockLabel(0))
        assertEquals("10:00 AM – 12:00 PM", historyBlockLabel(10))
        assertEquals("2:00 – 4:00 PM", historyBlockLabel(14))
        assertEquals("10:00 PM – 12:00 AM", historyBlockLabel(22))
    }

    @Test
    fun `history times read on a twelve-hour clock`() {
        assertEquals("12:05 AM", historyTimeLabel("00:05:59"))
        assertEquals("1:50 PM", historyTimeLabel("13:50:12"))
        assertEquals("12:00 PM", historyTimeLabel("12:00:00"))
        assertEquals("unknown", historyTimeLabel("unknown"))
    }

    @Test
    fun `history blocks step across midnight`() {
        val date = LocalDate.of(2026, 10, 2)

        assertEquals(date.minusDays(1) to 22, previousHistoryBlock(date, 0))
        assertEquals(date to 2, previousHistoryBlock(date, 4))
        assertEquals(date.plusDays(1) to 0, nextHistoryBlock(date, 22))
        assertEquals(date to 6, nextHistoryBlock(date, 4))
    }

    @Test
    fun `the current history block follows the station clock, not the device`() {
        // 02:30 UTC on October 3 is 22:30 on October 2 in US Eastern daylight time.
        val now = ZonedDateTime.of(2026, 10, 3, 2, 30, 0, 0, ZoneOffset.UTC)

        assertEquals(LocalDate.of(2026, 10, 2) to 22, currentPlayedHistoryBlock(now))
    }

    @Test
    fun `the public favorites badge is left out of the badge pills because the card shows it as a button`() {
        val member = MemberProfile(
            "Listener", null, null, null, null, false, null, listOf("Public Favorites", "Donor"),
            memberNumber = "4821",
            publicFavoritesBadge = "Public Favorites",
        )

        assertEquals(listOf("Donor"), memberProfileBadges(member))
        assertEquals("example.org", websiteLabel("http://www.example.org/listener"))
        assertEquals("1:14", shortDuration("01:14"))
        assertEquals("12:05", shortDuration("12:05"))
        assertEquals("0:36", shortDuration("0:36"))
        assertEquals(
            "Last played: Sep 20 · 1:56 AM\nRequest cooldown ends: Oct 5 · 1:56 AM",
            availabilityDetailLabel("Last played: 2026-09-20 01:56:25; Request cooldown ends: 2026-10-05 01:56:25", 2026),
        )
        assertEquals("Requestable Again: 12 days", availabilityDetailLabel("Requestable Again: 12 days", 2026))
        assertEquals(emptyList<String>(), memberProfileBadges(member.copy(badges = listOf("Public Favorites"))))
        assertEquals(listOf("Public Favorites", "Donor"), memberProfileBadges(member.copy(publicFavoritesBadge = null)))
    }

    @Test
    fun `the More tab announces unread private messages`() {
        assertEquals("More", navigationItemDescription(MainDestination.More, "More", 0))
        assertEquals("More, 1 unread private message", navigationItemDescription(MainDestination.More, "More", 1))
        assertEquals("More, 3 unread private messages", navigationItemDescription(MainDestination.More, "More", 3))
        assertEquals("Chat", navigationItemDescription(MainDestination.Chat, "Chat", 3))
    }
}
