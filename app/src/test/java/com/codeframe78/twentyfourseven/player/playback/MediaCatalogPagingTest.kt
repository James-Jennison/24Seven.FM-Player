package com.codeframe78.twentyfourseven.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaCatalogPagingTest {
    private val items = listOf("a", "b", "c", "d", "e")

    @Test
    fun `pages are bounded by the list`() {
        assertEquals(listOf("a", "b"), items.page(page = 0, pageSize = 2))
        assertEquals(listOf("e"), items.page(page = 2, pageSize = 2))
        assertEquals(items, items.page(page = 0, pageSize = 10))
        assertEquals(emptyList<String>(), items.page(page = 3, pageSize = 2))
    }

    @Test
    fun `extreme page and size values return a bounded page instead of overflowing`() {
        assertEquals(items, items.page(page = 0, pageSize = Int.MAX_VALUE))
        assertEquals(emptyList<String>(), items.page(page = 1, pageSize = Int.MAX_VALUE))
        assertEquals(emptyList<String>(), items.page(page = Int.MAX_VALUE, pageSize = Int.MAX_VALUE))
    }
}
