package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.ChatMessagePart
import com.codeframe78.twentyfourseven.player.domain.ChatRole
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
                authorRole = authorElement.classNames().firstNotNullOfOrNull(NICK_ROLE_CLASSES::get) ?: ChatRole.Member,
            )
        }

    private fun Node.toChatParts(baseUrl: String): List<ChatMessagePart> = when (this) {
        is TextNode -> listOf(
            ChatMessagePart.Text(wholeText.replace(WHITESPACE, " ").decodeChatNumericEntities()),
        )
        is Element -> if (tagName() == "img") {
            chatEmoticonImage(baseUrl)?.let(::listOf).orEmpty()
        } else {
            childNodes().flatMap { it.toChatParts(baseUrl) }
        }
        else -> emptyList()
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
        val WHITESPACE = Regex("\\s+")
    }
}

/**
 * An `<img>` the station chat may show inline: same origin over HTTPS under one of its ClearChat smiley
 * directories, with bounded alt text (or [fallbackAlt] when the station leaves alt empty, as its picker does).
 */
internal fun Element.chatEmoticonImage(baseUrl: String, fallbackAlt: String? = null): ChatMessagePart.Emoticon? {
    val alt = attr("alt").trim().ifEmpty { fallbackAlt.orEmpty().trim() }
        .takeIf { it.isNotEmpty() && it.length <= MAX_EMOTICON_ALT_CHARACTERS }
        ?: return null
    val resolved = runCatching { URI(absUrl("src")) }.getOrNull() ?: return null
    val base = runCatching { URI(baseUrl) }.getOrNull() ?: return null
    if (
        resolved.scheme != "https" ||
        !resolved.host.equals(base.host, ignoreCase = true) ||
        resolved.port != base.port ||
        EMOTICON_PATH_PREFIXES.none { resolved.path.startsWith(it) }
    ) return null
    val width = declaredEmoticonSize("width")
    val height = declaredEmoticonSize("height")
    return if (width != null && height != null) {
        ChatMessagePart.Emoticon(alt, resolved.toString(), width, height)
    } else {
        ChatMessagePart.Emoticon(alt, resolved.toString())
    }
}

private fun Element.declaredEmoticonSize(attribute: String): Int? =
    attr(attribute).trim().toIntOrNull()?.takeIf { it in 1..MAX_EMOTICON_SIZE_PX }

private const val MAX_EMOTICON_ALT_CHARACTERS = 32
private const val MAX_EMOTICON_SIZE_PX = 256

/** The station's classic smiley set and the station-smilies module it added in October 2026. */
internal val EMOTICON_PATH_PREFIXES = listOf(
    "/modules/ClearChat/common/smilies/",
    "/modules/ClearChat/common/station-smilies/",
)

/**
 * Whether this feed row is the station's rendering of [submitted]. The exact comparison covers plain text and
 * the classic smileys, whose alt text equals what was typed. When the station replaced `-codes-` with pictures
 * (whose alt text the module sets to the code itself) or decoded `&#...;` emoji, the words around them must match.
 */
internal fun ChatMessage.confirmsSubmission(submitted: String): Boolean {
    val expected = submitted.toSubmittedChatVisibleText()
    if (messageText == expected || messageText.toSubmittedChatVisibleText() == expected) return true
    val feedWords = parts.filterIsInstance<ChatMessagePart.Text>().joinToString(" ") { it.value }.chatWords()
    val submittedWords = submitted.decodeChatNumericEntities().replace(PHPBB_EMOTICON_CODE, " ").chatWords()
    return feedWords == submittedWords
}

private fun String.chatWords(): List<String> = split(Regex("\\s+")).filter(String::isNotEmpty)

/**
 * The text the station receives for [this] draft: characters outside its Latin-1 form are sent as numeric
 * character references, which the station stores and renders as the characters themselves (its feed carries
 * both `&#128171;` and raw emoji today).
 */
internal fun String.toStationChatWire(): String = buildString {
    var index = 0
    while (index < this@toStationChatWire.length) {
        val codePoint = this@toStationChatWire.codePointAt(index)
        if (codePoint <= LATIN_1_MAX) append(codePoint.toChar()) else append("&#").append(codePoint).append(';')
        index += Character.charCount(codePoint)
    }
}

private const val LATIN_1_MAX = 0xFF

/** The text a submitted message reads back as: the parser collapses whitespace, so the comparison must too. */
internal fun String.toSubmittedChatVisibleText(): String = replace(SUBMITTED_WHITESPACE, " ")
    .decodeChatNumericEntities()
    .replace(PHPBB_EMOTICON_CODE) { match -> match.groupValues[1] }
    .trim()

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

private val SUBMITTED_WHITESPACE = Regex("\\s+")
private val NUMERIC_ENTITY = Regex("&#(x([0-9A-Fa-f]{1,6})|([0-9]{1,7}));", RegexOption.IGNORE_CASE)
private val PHPBB_EMOTICON_CODE = Regex("(?<![A-Za-z0-9])-([A-Za-z][A-Za-z0-9_]*)-(?![A-Za-z0-9])")

/** The stations' shared stylesheet classes for the roles in their legend. */
internal val NICK_ROLE_CLASSES = mapOf(
    "fleetadmiralnick" to ChatRole.Proprietor,
    "admiralnick" to ChatRole.Administrator,
    "viceadmiralnick" to ChatRole.Moderator,
    "rearadmiralnick" to ChatRole.Ambassador,
    "visitormodnick" to ChatRole.VisitorMod,
    "composernick" to ChatRole.Composer,
)
