package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AlbumReview
import com.codeframe78.twentyfourseven.player.domain.EditableProfile
import com.codeframe78.twentyfourseven.player.domain.FlagOption
import com.codeframe78.twentyfourseven.player.domain.ProfileEditForm
import com.codeframe78.twentyfourseven.player.domain.RecentlyAddedAlbum
import com.codeframe78.twentyfourseven.player.domain.RecentlyAddedBatch
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

/** The reviews an album page shows. */
internal data class AlbumReviewsPage(val reviews: List<AlbumReview>)

/** Reads the stations' Recently Added page, an album page's reviews, and the Edit Profile form. */
internal class StationCatalogParser {
    /** The batches on the Recently Added page, newest first as the station lists them. */
    fun parseRecentlyAdded(html: String, origin: String): List<RecentlyAddedBatch> =
        Jsoup.parse(html, origin).select("article.news-story").mapNotNull { story ->
            val dateLabel = story.selectFirst(".news-story-meta time")?.text()?.trim()
                ?.takeIf(String::isNotEmpty)?.take(MAX_LABEL_CHARACTERS) ?: return@mapNotNull null
            val albums = story.select("table.news-album-table a[href]")
                .mapNotNull { link -> recentlyAddedAlbum(link, origin) }
                .take(MAX_ALBUMS_PER_BATCH)
            if (albums.isEmpty()) return@mapNotNull null
            RecentlyAddedBatch(
                dateLabel = dateLabel,
                author = story.selectFirst(".news-story-meta a")?.text()?.trim()
                    ?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARACTERS },
                albums = albums,
            )
        }.take(MAX_BATCHES)

    private fun recentlyAddedAlbum(link: Element, origin: String): RecentlyAddedAlbum? {
        val url = stationUrl(link.attr("href"), origin) ?: return null
        val albumId = ALBUM_ID_QUERY.find(url)?.groupValues?.get(1)?.takeIf { it.matches(SAFE_IDENTIFIER) } ?: return null
        val title = link.text().trim().takeIf(String::isNotEmpty) ?: return null
        return RecentlyAddedAlbum(
            albumId = albumId,
            title = title.take(MAX_LABEL_CHARACTERS),
            coverUrl = link.selectFirst("img[src]")?.attr("src")?.let { stationUrl(it, origin) },
        )
    }

    /** The reviews on an album page. */
    fun parseAlbumReviews(html: String, origin: String): AlbumReviewsPage {
        val document = Jsoup.parse(html, origin)
        return AlbumReviewsPage(reviews = document.select("table.album-review-table").mapNotNull(::albumReview).take(MAX_REVIEWS))
    }

    private fun albumReview(table: Element): AlbumReview? {
        val rows = directRows(table)
        val title = table.selectFirst("th")?.text()?.trim().orEmpty()
        // The review text is in the last row; the first nested table holds the By, Date, and Rating lines.
        val bodyCell = rows.lastOrNull()?.selectFirst("> td") ?: return null
        val details = rows.firstNotNullOfOrNull { it.selectFirst("table") }
            ?.select("tr")?.mapNotNull { row ->
                val cells = row.select("> td")
                val label = cells.getOrNull(0)?.text()?.trim()?.trimEnd(':', ' ', ' ')?.lowercase()
                if (label == null || cells.size < 2) null else label to cells[1]
            }?.toMap().orEmpty()

        val text = bodyCell.clone()
        val helpful = text.select("div").firstOrNull { it.ownText().contains(HELPFUL_MARKER, ignoreCase = true) }
        if (helpful != null) {
            // Everything from the helpful-vote line on is the station's own voting and log-in controls.
            var anchor: Element = helpful
            while (anchor.parent() != null && anchor.parent() !== text) anchor = anchor.parent() as Element
            val trailing = generateSequence<Node>(anchor) { it.nextSibling() }.toList()
            trailing.forEach(Node::remove)
        }
        val body = plainText(text).take(MAX_REVIEW_CHARACTERS)
        if (body.isEmpty()) return null

        return AlbumReview(
            title = title.take(MAX_LABEL_CHARACTERS),
            author = details["by"]?.text()?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARACTERS },
            dateLabel = details["date"]?.text()?.trim()?.takeIf(String::isNotEmpty)?.take(MAX_LABEL_CHARACTERS),
            rating = details["rating"]?.selectFirst("[title]")?.attr("title")
                ?.let { RATING_TITLE.find(it)?.groupValues?.get(1) },
            body = body,
            helpfulLabel = helpful?.text()?.trim()?.takeIf(String::isNotEmpty)?.take(MAX_LABEL_CHARACTERS),
        )
    }

    /**
     * The Edit Profile form that saves, with every control it would submit. Controls are read by name, never by
     * the page's table layout.
     */
    fun parseProfileEditForm(html: String, origin: String): ProfileEditForm? {
        val document = Jsoup.parse(html, origin)
        // The page also holds a search form, a birthday form, and avatar forms.
        val form = document.select("form").firstOrNull { it.selectFirst("input[name=op][value=saveuser]") != null }
            ?: return null
        val actionPath = stationPath(form.attr("action"), origin) ?: return null
        val fields = formFields(form)
        val values = fields.toMap()
        val flagSelect = form.selectFirst("select[name=user_flag]")
        val flags = flagSelect?.select("option")?.mapNotNull { option ->
            val value = optionValue(option).takeIf(String::isNotEmpty) ?: return@mapNotNull null
            FlagOption(value = value.take(MAX_NAME_CHARACTERS), label = option.text().trim().take(MAX_LABEL_CHARACTERS))
        }?.take(MAX_FLAGS).orEmpty()

        fun value(name: String, maxCharacters: Int) = values[name].orEmpty().take(maxCharacters)
        return ProfileEditForm(
            actionPath = actionPath,
            fields = fields,
            profile = EditableProfile(
                realName = value("realname", MAX_REAL_NAME_CHARACTERS),
                location = value("user_from", MAX_SHORT_FIELD_CHARACTERS),
                flag = values["user_flag"]?.takeIf(String::isNotEmpty) ?: BLANK_FLAG,
                occupation = value("user_occ", MAX_SHORT_FIELD_CHARACTERS),
                interests = value("user_interests", MAX_SHORT_FIELD_CHARACTERS),
                website = value("user_website", MAX_WEBSITE_CHARACTERS),
                signature = value("user_sig", MAX_SIGNATURE_CHARACTERS),
                bio = value("bio", MAX_BIO_CHARACTERS),
                newsletter = values["newsletter"] == "1",
                // The page labels this radio "Hide your online status", and its value 0 is the Yes choice.
                hideOnlineStatus = values["user_allow_viewonline"] == "0",
            ),
            flags = flags,
        )
    }

    /** The form's fields as the station expects them back, with the member's edits applied in place. */
    fun profileFormFields(form: ProfileEditForm, edited: EditableProfile): List<Pair<String, String>> {
        val flag = edited.flag.takeIf { chosen -> form.flags.isEmpty() || form.flags.any { it.value == chosen } }
        val replacements = mapOf(
            "realname" to edited.realName.take(MAX_REAL_NAME_CHARACTERS),
            "user_from" to edited.location.take(MAX_SHORT_FIELD_CHARACTERS),
            "user_flag" to flag,
            "user_occ" to edited.occupation.take(MAX_SHORT_FIELD_CHARACTERS),
            "user_interests" to edited.interests.take(MAX_SHORT_FIELD_CHARACTERS),
            "user_website" to edited.website.take(MAX_WEBSITE_CHARACTERS),
            "user_sig" to edited.signature.take(MAX_SIGNATURE_CHARACTERS),
            "bio" to edited.bio.take(MAX_BIO_CHARACTERS),
            "newsletter" to if (edited.newsletter) "1" else "0",
            "user_allow_viewonline" to if (edited.hideOnlineStatus) "0" else "1",
            // The Player never changes a password.
            "user_password" to "",
            "vpass" to "",
        )
        return form.fields.map { (name, value) -> name to (replacements[name] ?: value) }
    }

    /** Every successful control of the form as name and current value, in document order. */
    private fun formFields(form: Element): List<Pair<String, String>> {
        val entries = mutableListOf<Pair<String, String?>>()
        val groupIndexes = mutableMapOf<String, Int>()
        for (control in form.select("input, select, textarea")) {
            val name = control.attr("name").trim().takeIf(String::isNotEmpty) ?: continue
            if (control.hasAttr("disabled")) continue
            when (control.normalName()) {
                "textarea" -> entries += name to control.wholeText()
                "select" -> {
                    val options = control.select("option")
                    val chosen = options.firstOrNull { it.hasAttr("selected") } ?: options.firstOrNull()
                    entries += name to (chosen?.let(::optionValue) ?: "")
                }
                else -> when (control.attr("type").lowercase()) {
                    "submit", "reset", "button", "image", "file" -> Unit
                    "password" -> entries += name to ""
                    "radio", "checkbox" -> {
                        // A group sits where its first control is and sends its checked value, if any.
                        val index = groupIndexes.getOrPut(name) { entries.size.also { entries += name to null } }
                        if (control.hasAttr("checked")) {
                            entries[index] = name to (if (control.hasAttr("value")) control.attr("value") else "on")
                        }
                    }
                    else -> entries += name to control.attr("value")
                }
            }
        }
        return entries.mapNotNull { (name, value) -> value?.let { name to it } }
    }

    private fun optionValue(option: Element): String =
        if (option.hasAttr("value")) option.attr("value").trim() else option.text().trim()

    /** A table's own rows, whether or not the parser wrapped them in a tbody. */
    private fun directRows(table: Element): List<Element> =
        table.children().flatMap { child ->
            if (child.normalName() == "tr") listOf(child) else child.children().filter { it.normalName() == "tr" }
        }

    /** Text with the page's line breaks kept and its markup, links, and images dropped. */
    private fun plainText(element: Element): String {
        val text = StringBuilder()
        fun visit(node: Node) {
            when {
                node is TextNode -> text.append(node.text())
                node is Element && node.normalName() == "br" -> text.append('\n')
                node is Element -> {
                    val isBlock = node.normalName() in BLOCK_ELEMENTS
                    if (isBlock) text.append('\n')
                    node.childNodes().forEach(::visit)
                    if (isBlock) text.append('\n')
                }
            }
        }
        element.childNodes().forEach(::visit)
        return text.lines().joinToString("\n") { it.replace(SPACES, " ").trim() }
            .replace(BLANK_RUN, "\n\n")
            .trim()
    }

    /** An absolute address on the station's own site, or null for anything else. */
    private fun stationUrl(value: String, origin: String): String? = runCatching {
        val base = URI(origin)
        val uri = base.resolve(value.trim())
        uri.takeIf {
            it.scheme == "https" && it.userInfo == null && it.port == base.port &&
                it.host.equals(base.host, ignoreCase = true)
        }?.toASCIIString()
    }.getOrNull()

    /**
     * The path and query of a link into the station's site, or null for anything else. The station writes its own
     * addresses with a www prefix, so that spelling of the origin's host is the same site.
     */
    private fun stationPath(value: String, origin: String): String? = runCatching {
        val base = URI(origin)
        val uri = base.resolve(value.trim())
        val host = uri.host ?: return@runCatching null
        val sameSite = host.equals(base.host, ignoreCase = true) || host.equals("www.${base.host}", ignoreCase = true)
        if (uri.scheme != "https" || uri.userInfo != null || uri.port != base.port || !sameSite) return@runCatching null
        val path = uri.rawPath?.takeIf(String::isNotEmpty) ?: "/"
        uri.rawQuery?.let { "$path?$it" } ?: path
    }.getOrNull()

    private companion object {
        const val MAX_BATCHES = 30
        const val MAX_ALBUMS_PER_BATCH = 100
        const val MAX_REVIEWS = 100
        const val MAX_REVIEW_CHARACTERS = 4_000
        const val MAX_FLAGS = 300
        const val MAX_NAME_CHARACTERS = 60
        const val MAX_LABEL_CHARACTERS = 200
        const val MAX_REAL_NAME_CHARACTERS = 60
        const val MAX_SHORT_FIELD_CHARACTERS = 100
        const val MAX_WEBSITE_CHARACTERS = 255
        const val MAX_SIGNATURE_CHARACTERS = 500
        const val MAX_BIO_CHARACTERS = 1_024
        const val BLANK_FLAG = "blank.gif"
        const val HELPFUL_MARKER = "found this review helpful"
        val ALBUM_ID_QUERY = Regex("[?&]asin=([^&#]+)")
        val SAFE_IDENTIFIER = Regex("[A-Za-z0-9_.-]{1,64}")
        val RATING_TITLE = Regex("""(\d+(?:\.\d+)?)\s+out of 5""")
        val SPACES = Regex("[ \\t\\u00A0]+")
        val BLANK_RUN = Regex("\\n{3,}")
        val BLOCK_ELEMENTS = setOf("p", "div", "li", "ul", "ol", "tr", "table", "h1", "h2", "h3", "h4", "blockquote")
    }
}
