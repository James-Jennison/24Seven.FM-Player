package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonCatalog
import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonChoice
import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonGroup
import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonTab
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException

/**
 * Reads the station's chat smiley picker page (`/modules/ClearChat/common/smilies.php`): a tab list whose
 * entries name a panel, and panels of `<h3>` groups holding `[data-smiley-code]` buttons. Picture choices keep
 * only images the chat itself may show; a choice without a picture is an emoji whose code is the glyph.
 */
internal class ChatEmoticonCatalogParser {
    fun parse(html: String, baseUrl: String): ChatEmoticonCatalog {
        val document = Jsoup.parse(html, baseUrl)
        var remaining = MAX_CHOICES
        val tabs = document.select("[data-smiley-tab]").take(MAX_TABS).mapNotNull { tab ->
            val id = tab.attr("data-smiley-tab").trim().takeIf { TAB_ID.matches(it) } ?: return@mapNotNull null
            val label = tab.text().trim().take(MAX_LABEL_CHARACTERS).ifEmpty { id }
            val panelId = tab.attr("aria-controls").trim().ifEmpty { "panel-$id" }
            val panel = document.getElementById(panelId) ?: return@mapNotNull null
            val groups = panel.groups(baseUrl, label, remaining)
            remaining -= groups.sumOf { it.choices.size }
            groups.takeIf { it.isNotEmpty() }?.let { ChatEmoticonTab(id, label, it) }
        }
        if (tabs.isEmpty()) throw IOException("Station smiley catalog was unavailable")
        return ChatEmoticonCatalog(tabs)
    }

    private fun Element.groups(baseUrl: String, tabLabel: String, limit: Int): List<ChatEmoticonGroup> {
        val groups = mutableListOf<Pair<String, MutableList<ChatEmoticonChoice>>>()
        var taken = 0
        for (element in select("h2, h3, h4, [data-smiley-code]")) {
            if (element.hasAttr("data-smiley-code")) {
                if (taken >= limit) break
                val choice = element.choice(baseUrl) ?: continue
                if (groups.isEmpty()) groups += tabLabel to mutableListOf()
                groups.last().second += choice
                taken++
            } else if (element.tagName() != "h2" || groups.isNotEmpty()) {
                val label = element.text().trim().take(MAX_LABEL_CHARACTERS)
                if (label.isNotEmpty() && groups.lastOrNull()?.second?.isEmpty() == true) groups.removeAt(groups.lastIndex)
                if (label.isNotEmpty()) groups += label to mutableListOf()
            }
        }
        return groups.filter { it.second.isNotEmpty() }.map { (label, choices) -> ChatEmoticonGroup(label, choices) }
    }

    private fun Element.choice(baseUrl: String): ChatEmoticonChoice? {
        val code = attr("data-smiley-code").trim()
        if (code.isEmpty() || code.length > MAX_CODE_CHARACTERS || code.any { it.isWhitespace() || it.isISOControl() }) {
            return null
        }
        val label = attr("aria-label").trim().ifEmpty { attr("title").trim() }.ifEmpty { code }.take(MAX_LABEL_CHARACTERS)
        val picture = selectFirst("img")
        return if (picture == null) {
            ChatEmoticonChoice(code, label)
        } else {
            picture.chatEmoticonImage(baseUrl, fallbackAlt = label)?.let { ChatEmoticonChoice(code, label, it) }
        }
    }

    private companion object {
        const val MAX_TABS = 12
        const val MAX_CHOICES = 800
        const val MAX_CODE_CHARACTERS = 32
        const val MAX_LABEL_CHARACTERS = 64
        val TAB_ID = Regex("[a-z][a-z0-9-]{0,31}")
    }
}
