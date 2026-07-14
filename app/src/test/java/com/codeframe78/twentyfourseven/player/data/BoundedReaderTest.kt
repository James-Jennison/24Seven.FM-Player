package com.codeframe78.twentyfourseven.player.data

import java.io.IOException
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedReaderTest {
    @Test
    fun `returns response within configured limit`() {
        assertEquals("response", StringReader("response").readBounded(8))
    }

    @Test
    fun `rejects response above configured limit`() {
        assertThrows(IOException::class.java) {
            StringReader("too large").readBounded(8)
        }
    }
}
