package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode

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

            val messageCopy = messageElement.clone()
            messageCopy.select("img[alt]").forEach { image ->
                image.after(TextNode(image.attr("alt")))
                image.remove()
            }
            val message = messageCopy.text().trim()
            if (message.isEmpty() || message.length > MAX_MESSAGE_CHARACTERS) return@mapNotNull null

            ChatMessage(
                authorDisplayName = author,
                messageText = message,
                postedAtLabel = sequenceOf(messageElement, authorElement)
                    .mapNotNull { it.attr("title").takeIf { title -> title.startsWith("Posted ") } }
                    .firstOrNull()
                    ?.removePrefix("Posted ")
                    ?.take(MAX_TIMESTAMP_CHARACTERS),
            )
        }

    private companion object {
        const val MAX_MESSAGES = 50
        const val MAX_AUTHOR_CHARACTERS = 64
        const val MAX_MESSAGE_CHARACTERS = 255
        const val MAX_TIMESTAMP_CHARACTERS = 64
    }
}
