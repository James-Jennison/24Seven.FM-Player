package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.ChatMessagePart
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

internal class ChatResponseParser {
    fun parse(html: String, baseUrl: String): List<ChatMessage> = Jsoup.parse(html, baseUrl)
        .select(".msg-row")
        .take(MAX_MESSAGES)
        .mapNotNull { row ->
            val authorElement = row.children().firstOrNull { element ->
                element.tagName() == "span" && element.classNames().any { it.endsWith("nick") }
            } ?: return@mapNotNull null
            val messageElement = row.selectFirst("span.say") ?: return@mapNotNull null
            val author = authorElement.text().trim().removeSuffix(":").trim()
            if (author.isEmpty() || author.length > MAX_AUTHOR_CHARACTERS) return@mapNotNull null

            val parts = messageElement.childNodes()
                .flatMap { node -> node.toChatParts(baseUrl) }
                .mergeTextParts()
                .trimTextEdges()
            val message = parts.joinToString("") { part ->
                when (part) {
                    is ChatMessagePart.Emoticon -> part.altText
                    is ChatMessagePart.Text -> part.value
                }
            }.trim()
            if (message.isEmpty() || message.length > MAX_MESSAGE_CHARACTERS) return@mapNotNull null

            ChatMessage(
                authorDisplayName = author,
                messageText = message,
                postedAtLabel = sequenceOf(messageElement, authorElement)
                    .mapNotNull { it.attr("title").takeIf { title -> title.startsWith("Posted ") } }
                    .firstOrNull()
                    ?.removePrefix("Posted ")
                    ?.take(MAX_TIMESTAMP_CHARACTERS),
                parts = parts,
            )
        }

    private fun Node.toChatParts(baseUrl: String): List<ChatMessagePart> = when (this) {
        is TextNode -> listOf(
            ChatMessagePart.Text(wholeText.replace(WHITESPACE, " ").decodeChatNumericEntities()),
        )
        is Element -> if (tagName() == "img") {
            safeEmoticon(baseUrl)?.let(::listOf).orEmpty()
        } else {
            childNodes().flatMap { it.toChatParts(baseUrl) }
        }
        else -> emptyList()
    }

    private fun Element.safeEmoticon(baseUrl: String): ChatMessagePart.Emoticon? {
        val alt = attr("alt").trim().takeIf { it.isNotEmpty() && it.length <= MAX_EMOTICON_ALT_CHARACTERS }
            ?: return null
        val resolved = runCatching { URI(absUrl("src")) }.getOrNull() ?: return null
        val base = runCatching { URI(baseUrl) }.getOrNull() ?: return null
        if (
            resolved.scheme != "https" ||
            !resolved.host.equals(base.host, ignoreCase = true) ||
            resolved.port != base.port ||
            !resolved.path.startsWith(EMOTICON_PATH_PREFIX)
        ) return null
        return ChatMessagePart.Emoticon(alt, resolved.toString())
    }

    private fun List<ChatMessagePart>.mergeTextParts(): List<ChatMessagePart> = buildList {
        this@mergeTextParts.forEach { part ->
            val previous = lastOrNull()
            if (part is ChatMessagePart.Text && previous is ChatMessagePart.Text) {
                removeAt(lastIndex)
                add(ChatMessagePart.Text(previous.value + part.value))
            } else {
                add(part)
            }
        }
    }

    private fun List<ChatMessagePart>.trimTextEdges(): List<ChatMessagePart> = mapIndexedNotNull { index, part ->
        if (part !is ChatMessagePart.Text) return@mapIndexedNotNull part
        val value = when (index) {
            0 -> part.value.trimStart()
            lastIndex -> part.value.trimEnd()
            else -> part.value
        }
        value.takeIf(String::isNotEmpty)?.let(ChatMessagePart::Text)
    }

    private companion object {
        const val MAX_MESSAGES = 50
        const val MAX_AUTHOR_CHARACTERS = 64
        const val MAX_MESSAGE_CHARACTERS = 255
        const val MAX_TIMESTAMP_CHARACTERS = 64
        const val MAX_EMOTICON_ALT_CHARACTERS = 32
        const val EMOTICON_PATH_PREFIX = "/modules/ClearChat/common/smilies/"
        val WHITESPACE = Regex("\\s+")
    }
}

internal fun String.toSubmittedChatVisibleText(): String = decodeChatNumericEntities()
    .replace(PHPBB_EMOTICON_CODE) { match -> match.groupValues[1] }

private fun String.decodeChatNumericEntities(): String = replace(NUMERIC_ENTITY) { match ->
    val digits = match.groups[2]?.value ?: match.groups[3]?.value.orEmpty()
    val radix = if (match.groups[2] != null) 16 else 10
    val codePoint = digits.toIntOrNull(radix)
    if (codePoint != null && Character.isValidCodePoint(codePoint) &&
        codePoint !in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code
    ) {
        String(Character.toChars(codePoint))
    } else {
        match.value
    }
}

private val NUMERIC_ENTITY = Regex("&#(x([0-9A-Fa-f]{1,6})|([0-9]{1,7}));", RegexOption.IGNORE_CASE)
private val PHPBB_EMOTICON_CODE = Regex("(?<![A-Za-z0-9])-([A-Za-z][A-Za-z0-9_]*)-(?![A-Za-z0-9])")
