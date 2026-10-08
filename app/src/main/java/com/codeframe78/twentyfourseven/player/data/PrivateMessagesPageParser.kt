package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.PrivateMessage
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageFolder
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.IOException
import java.net.URI

internal data class PrivateMessagesPage(
    val messages: List<PrivateMessageSummary>,
    val page: Int,
    val pageCount: Int,
    val unreadCount: Int?,
)

/** The station's own message form: who it is addressed to, what it pre-fills, and the fields it must carry. */
internal data class PrivateMessageForm(
    val recipient: String,
    val subject: String,
    val body: String,
    val hiddenFields: List<Pair<String, String>>,
    val saveCopyValue: String,
)

internal class PrivateMessagesPageParser {
    fun parseFolder(html: String, origin: String): PrivateMessagesPage {
        val document = Jsoup.parse(html, origin)
        val originUri = URI(origin)
        val readLinks = document.select("a[href]").filter { it.messageQuery(originUri)?.get("mode") == "read" }
        if (readLinks.isEmpty() && document.selectFirst("select[name=folder]") == null) {
            throw IOException("Private messages were not found")
        }
        val messages = readLinks.mapNotNull { link ->
            val row = link.parents().firstOrNull { it.normalName() == "tr" } ?: return@mapNotNull null
            val cells = row.children().filter { it.normalName() == "td" }
            if (cells.size < 4) return@mapNotNull null
            val id = link.messageQuery(originUri)?.get("id")?.takeIf { it.matches(MESSAGE_ID) }
                ?: return@mapNotNull null
            PrivateMessageSummary(
                id = id,
                subject = link.text().trim().ifEmpty { "(no subject)" }.take(MAX_SUBJECT_CHARACTERS),
                correspondent = cells[2].text().trim().take(MAX_NAME_CHARACTERS),
                dateLabel = cells[3].text().trim().take(MAX_DATE_CHARACTERS),
                isUnread = cells[0].selectFirst("img[src]")?.attr("src").orEmpty()
                    .substringAfterLast('/')
                    .contains("new", ignoreCase = true),
            )
        }.distinctBy { it.id }
        val pageLinks = document.select("a[href]").mapNotNull { link ->
            link.messageQuery(originUri)?.takeIf { "mode" !in it }?.get("p")?.toIntOrNull()
        }
        val currentPage = document.select("b").firstNotNullOfOrNull { bold ->
            CURRENT_PAGE.matchEntire(bold.text().trim())?.groupValues?.get(1)?.toIntOrNull()
        } ?: 1
        return PrivateMessagesPage(
            messages = messages,
            page = currentPage,
            pageCount = (pageLinks + currentPage).max(),
            unreadCount = document.unreadCount(),
        )
    }

    fun parseMessage(html: String, origin: String, folder: PrivateMessageFolder, messageId: String): PrivateMessage {
        val document = Jsoup.parse(html, origin)
        val originUri = URI(origin)
        val table = document.select("b").firstOrNull { it.text().trim() == "From:" }
            ?.parents()?.firstOrNull { it.normalName() == "table" }
            ?: throw IOException("Private message was not found")
        val rows = table.select("tr").filter { row -> row.parents().first { it.normalName() == "table" } === table }
        fun field(label: String): String = rows.firstOrNull { row ->
            row.selectFirst("b")?.text()?.trim() == label
        }?.children()?.lastOrNull()?.text()?.trim().orEmpty()
        val subjectRowIndex = rows.indexOfFirst { it.selectFirst("b")?.text()?.trim() == "Subject:" }
        val bodyCell = rows.drop(subjectRowIndex + 1).firstOrNull()?.children()?.firstOrNull()
            ?: throw IOException("Private message was not found")
        return PrivateMessage(
            id = messageId,
            folder = folder,
            sender = field("From:").take(MAX_NAME_CHARACTERS),
            recipient = field("To:").take(MAX_NAME_CHARACTERS),
            dateLabel = field("Date:").take(MAX_DATE_CHARACTERS),
            subject = field("Subject:").ifEmpty { "(no subject)" }.take(MAX_SUBJECT_CHARACTERS),
            body = bodyCell.textWithLineBreaks().take(MAX_BODY_CHARACTERS),
            canReply = table.select("a[href]").any { link ->
                val query = link.messageQuery(originUri)
                query?.get("mode") == "post" && query["id"] == messageId
            },
        )
    }

    fun parseForm(html: String, origin: String): PrivateMessageForm? {
        val document = Jsoup.parse(html, origin)
        val message = document.selectFirst("textarea[name=message]") ?: return null
        val table = message.parents().firstOrNull { it.normalName() == "table" } ?: return null
        val recipient = table.select("b").firstOrNull { it.text().trim() == "To:" }
            ?.parent()?.nextElementSibling()?.text()?.trim()
            ?.takeIf(String::isNotEmpty) ?: return null
        val hidden = table.select("input[type=hidden][name]").map { it.attr("name") to it.attr("value") }
        // Without the recipient's member number and the send operation the form cannot address anyone.
        if (hidden.none { it.first == "u" && it.second.matches(MEMBER_NUMBER) }) return null
        if (hidden.none { it.first == "op" && it.second == "sendmsg" }) return null
        return PrivateMessageForm(
            recipient = recipient.take(MAX_NAME_CHARACTERS),
            subject = table.selectFirst("input[name=subject]")?.attr("value").orEmpty(),
            body = message.wholeText(),
            hiddenFields = hidden,
            saveCopyValue = table.selectFirst("input[name=savecopy]")?.attr("value")
                ?.takeIf(String::isNotEmpty) ?: "1",
        )
    }

    /** The member number a profile page offers for sending that member a private message. */
    fun parseProfileMemberNumber(html: String, origin: String): String? {
        val originUri = URI(origin)
        return Jsoup.parse(html, origin).select("a[href]").firstNotNullOfOrNull { link ->
            val query = link.messageQuery(originUri) ?: return@firstNotNullOfOrNull null
            query["u"]?.takeIf { query["mode"] == "post" && it.matches(MEMBER_NUMBER) }
        }
    }

    private fun Document.unreadCount(): Int? = selectFirst("span.pm-badge")
        ?.text()?.trim()?.toIntOrNull()?.takeIf { it >= 0 }

    /** The query of a same-origin Private_Messages link, or null for any other link. */
    private fun Element.messageQuery(origin: URI): Map<String, String>? {
        val uri = runCatching { URI(absUrl("href")) }.getOrNull() ?: return null
        if (
            uri.scheme != "https" || !uri.host.equals(origin.host, ignoreCase = true) ||
            uri.port != origin.port || uri.path != "/modules.php"
        ) {
            return null
        }
        val query = uri.rawQuery.orEmpty().split('&')
            .mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
            .associate { it[0] to it[1] }
        return query.takeIf { it["name"] == "Private_Messages" }
    }

    private fun Element.textWithLineBreaks(): String {
        val text = StringBuilder()
        fun visit(node: Node) {
            when {
                node is TextNode -> text.append(node.text())
                node is Element && node.normalName() == "br" -> text.append('\n')
                node is Element && node.normalName() == "img" -> text.append(node.attr("alt"))
                node is Element -> {
                    val isBlock = node.isBlock
                    if (isBlock && text.isNotEmpty() && text.last() != '\n') text.append('\n')
                    node.childNodes().forEach(::visit)
                    if (isBlock && text.isNotEmpty() && text.last() != '\n') text.append('\n')
                }
            }
        }
        childNodes().forEach(::visit)
        return text.lines().joinToString("\n") { it.trim() }.replace(EXTRA_BLANK_LINES, "\n\n").trim()
    }

    private companion object {
        val MESSAGE_ID = Regex("\\d{1,12}")
        val MEMBER_NUMBER = Regex("\\d{1,10}")
        val CURRENT_PAGE = Regex("\\[(\\d{1,5})]")
        val EXTRA_BLANK_LINES = Regex("\\n{3,}")
        const val MAX_SUBJECT_CHARACTERS = 200
        const val MAX_NAME_CHARACTERS = 80
        const val MAX_DATE_CHARACTERS = 60
        const val MAX_BODY_CHARACTERS = 60_000
    }
}
