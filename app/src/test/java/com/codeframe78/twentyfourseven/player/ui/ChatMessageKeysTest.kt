package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageKeysTest {
    private val older = ChatMessage("Listener", "hello", postedAtLabel = "01 Oct 26 - 15:00:00")
    private val repeated = ChatMessage("Listener", "hello", postedAtLabel = "01 Oct 26 - 15:00:00")
    private val other = ChatMessage("Other", "hi", postedAtLabel = "01 Oct 26 - 15:00:05")

    @Test
    fun `keys are unique even for identical messages`() {
        val keys = chatMessageKeys(listOf(other, repeated, older))

        assertEquals(3, keys.toSet().size)
    }

    @Test
    fun `existing messages keep their keys when a newer message arrives`() {
        val before = chatMessageKeys(listOf(other, repeated, older))
        val newest = ChatMessage("Listener", "hello", postedAtLabel = "01 Oct 26 - 15:00:00")
        val after = chatMessageKeys(listOf(newest, other, repeated, older))

        assertEquals(before, after.drop(1))
    }
}
