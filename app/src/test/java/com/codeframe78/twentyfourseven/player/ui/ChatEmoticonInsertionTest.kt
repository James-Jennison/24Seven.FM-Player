package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatEmoticonInsertionTest {
    @Test
    fun `inserts at the cursor and separates a code from letters`() {
        val result = insertChatEmoticon(TextFieldValue("hello world", TextRange(5)), "-smile-")
        assertEquals("hello -smile- world", result?.text)
        assertEquals(TextRange(13), result?.selection)
    }

    @Test
    fun `does not pad a code already surrounded by spaces or an emoji`() {
        assertEquals("hi -smile-", insertChatEmoticon(TextFieldValue("hi ", TextRange(3)), "-smile-")?.text)
        assertEquals("hi😀", insertChatEmoticon(TextFieldValue("hi", TextRange(2)), "😀")?.text)
    }

    @Test
    fun `replaces a selection`() {
        assertEquals("a -wink- d", insertChatEmoticon(TextFieldValue("a bc d", TextRange(2, 4)), "-wink-")?.text)
    }

    @Test
    fun `refuses to pass the limit`() {
        assertNull(insertChatEmoticon(TextFieldValue("x".repeat(250), TextRange(250)), "-smile-"))
    }
}
