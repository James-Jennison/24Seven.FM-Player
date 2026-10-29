package com.codeframe78.twentyfourseven.player.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMentionTrackerTest {
    private val stationId = StationId("sst")

    @Test
    fun `embedded separators cannot alias distinct message field tuples`() {
        val tracker = ChatMentionTracker()
        tracker.accept(snapshot())
        val first = ChatMessage("Listener\u0000part", "Hello MorG", "12:05")
        val second = ChatMessage("Listener", "Hello MorG", "part\u000012:05")
        assertEquals(1, tracker.accept(snapshot(first)).size)
        assertEquals(1, tracker.accept(snapshot(second)).size)
        assertTrue(tracker.accept(snapshot(first, second)).isEmpty())
    }

    @Test
    fun `separator safe fingerprints preserve exact replay and distinct ordinary rows`() {
        val tracker = ChatMentionTracker()
        tracker.accept(snapshot())
        val first = ChatMessage("Listener", "Hello MorG", "12:05")
        val second = first.copy(postedAtLabel = "12:06")
        assertEquals(1, tracker.accept(snapshot(first)).size)
        assertEquals(1, tracker.accept(snapshot(first, second)).size)
        assertTrue(tracker.accept(snapshot(first, second)).isEmpty())
    }

    @Test
    fun `identical rows in one new snapshot emit only one mention`() {
        val tracker = ChatMentionTracker()
        tracker.accept(snapshot())
        val message = ChatMessage("Listener", "Hello MorG", "12:05")
        assertEquals(1, tracker.accept(snapshot(message, message)).size)
        assertTrue(tracker.accept(snapshot(message, message)).isEmpty())
    }

    @Test
    fun `duplicate suppression preserves distinct messages and retention bound`() {
        val tracker = ChatMentionTracker(maximumRememberedMessagesPerStation = 1)
        tracker.accept(snapshot())
        val first = ChatMessage("Listener", "Hello MorG", "12:05")
        val second = ChatMessage("Listener", "Hi MorG", "12:06")
        assertEquals(2, tracker.accept(snapshot(first, first, second, second)).size)
        assertTrue(tracker.accept(snapshot(second)).isEmpty())
    }

    @Test
    fun `exact display name matching is case insensitive and requires identity boundaries`() {
        assertTrue("Hello, MorG!".containsExactDisplayName("morg"))
        assertTrue("@MorG are you listening?".containsExactDisplayName("MorG"))
        assertFalse("MorgHubby is listening".containsExactDisplayName("MorG"))
        assertFalse("super_morg_user".containsExactDisplayName("MorG"))
        assertFalse("ordinary text".containsExactDisplayName(""))
    }

    @Test
    fun `first snapshot establishes a baseline and a later mention is emitted once`() {
        val tracker = ChatMentionTracker()
        val baseline = snapshot(
            ChatMessage("Listener", "MorG was mentioned before alerts started", "12:00"),
        )
        assertTrue(tracker.accept(baseline).isEmpty())

        val updated = snapshot(
            ChatMessage("Listener", "MorG was mentioned before alerts started", "12:00"),
            ChatMessage("Another Listener", "Hello MorG!", "12:01"),
        )
        assertEquals(
            listOf(ChatMentionEvent(stationId, "StreamingSoundtracks.com", "Another Listener")),
            tracker.accept(updated),
        )
        assertTrue(tracker.accept(updated).isEmpty())
    }

    @Test
    fun `signed in author does not notify for their own message`() {
        val tracker = ChatMentionTracker()
        tracker.accept(snapshot())

        assertTrue(
            tracker.accept(snapshot(ChatMessage("morg", "MorG checking notifications", "12:02"))).isEmpty(),
        )
    }

    @Test
    fun `blocked author is remembered without alerting after a later unblock`() {
        val tracker = ChatMentionTracker()
        tracker.accept(snapshot())
        val blockedMessage = ChatMessage("Blocked Listener", "Hello MorG", "12:03")

        assertTrue(
            tracker.accept(
                snapshot(blockedMessage).copy(blockedAuthorDisplayNames = setOf("blocked listener")),
            ).isEmpty(),
        )
        assertTrue(tracker.accept(snapshot(blockedMessage)).isEmpty())
    }

    private fun snapshot(vararg messages: ChatMessage) = ChatMentionSnapshot(
        stationId = stationId,
        stationName = "StreamingSoundtracks.com",
        signedInDisplayName = "MorG",
        messages = messages.toList(),
    )
}
