package com.codeframe78.twentyfourseven.player.data

import java.nio.charset.Charset

private val DECLARED_CHARSET = Regex("charset\\s*=\\s*\"?([^;\"\\s]+)", RegexOption.IGNORE_CASE)

/**
 * The character set a station response declares in its Content-Type, or [fallback] when it declares none.
 *
 * The stations serve some pages as UTF-8 and others as ISO-8859-1, so a fixed choice garbles accented text.
 */
internal fun declaredCharset(contentType: String?, fallback: Charset): Charset = contentType
    ?.let { DECLARED_CHARSET.find(it)?.groupValues?.get(1) }
    ?.let { name -> runCatching { Charset.forName(name) }.getOrNull() }
    ?: fallback
