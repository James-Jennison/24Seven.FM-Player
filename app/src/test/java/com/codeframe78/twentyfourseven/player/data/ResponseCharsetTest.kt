package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class ResponseCharsetTest {
    private val fallback = StandardCharsets.ISO_8859_1

    @Test
    fun `declared character set is used whatever its case or quoting`() {
        assertEquals(StandardCharsets.UTF_8, declaredCharset("text/html; charset=UTF-8", fallback))
        assertEquals(StandardCharsets.UTF_8, declaredCharset("text/html;Charset=\"utf-8\"; boundary=x", fallback))
        assertEquals(StandardCharsets.ISO_8859_1, declaredCharset("text/html; charset=ISO-8859-1", StandardCharsets.UTF_8))
    }

    @Test
    fun `missing or unknown character set falls back`() {
        assertEquals(fallback, declaredCharset(null, fallback))
        assertEquals(fallback, declaredCharset("text/html", fallback))
        assertEquals(fallback, declaredCharset("text/html; charset=not-a-charset", fallback))
    }
}
