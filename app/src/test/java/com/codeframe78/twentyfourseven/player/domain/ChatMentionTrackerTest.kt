package com.codeframe78.twentyfourseven.player.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMentionTrackerTest {
    private val stationId = StationId("sst")

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
