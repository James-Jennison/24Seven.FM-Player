package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatRole
import com.codeframe78.twentyfourseven.player.domain.MemberProfile
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.StationNewsStory
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

/** Reads the stations' public profile card, played-history archive, and news page. */
internal class StationExtrasParser {
    /** The member a profile card describes, or null when the station knows no such member. */
    fun parseProfile(json: String, origin: String): MemberProfile? {
        val profile = JSONObject(json).optJSONObject("profile") ?: return null
        val username = profile.text("username", MAX_NAME_CHARACTERS) ?: return null
        val badges = profile.optJSONArray("badges")
        val badgeEntries = (0 until (badges?.length() ?: 0)).mapNotNull { index -> badges?.optJSONObject(index) }
        val contacts = profile.optJSONArray("contacts")
        val contactLinks = (0 until minOf(contacts?.length() ?: 0, MAX_CONTACTS))
            .mapNotNull { index -> contacts?.optJSONObject(index) }
            .mapNotNull { contact ->
                val kind = contact.text("kind", MAX_LABEL_CHARACTERS) ?: return@mapNotNull null
                kind to (contact.text("href", MAX_URL_CHARACTERS) ?: return@mapNotNull null)
            }.toMap()
        return MemberProfile(
            username = username,
            memberSince = profile.text("since", MAX_LABEL_CHARACTERS),
            location = profile.text("location", MAX_LABEL_CHARACTERS),
            rankTitle = profile.optJSONObject("rank")?.text("title", MAX_LABEL_CHARACTERS),
            membership = profile.text("membership", MAX_LABEL_CHARACTERS),
            isOnline = profile.optBoolean("online", false),
            // The stations' placeholder avatar is a vector image the Player does not decode.
            avatarUrl = profile.text("avatar", MAX_URL_CHARACTERS)
                ?.takeUnless { it.substringBefore('?').endsWith(".svg", ignoreCase = true) }
                ?.let { stationUrl(it, origin) },
            badges = badgeEntries.mapNotNull { it.text("label", MAX_LABEL_CHARACTERS) }.take(MAX_BADGES),
            memberNumber = profile.text("id", MAX_LABEL_CHARACTERS)?.takeIf { it.matches(MEMBER_NUMBER) },
            publicFavoritesBadge = badgeEntries.firstOrNull { it.marksPublicFavorites() }
                ?.let { it.text("label", MAX_LABEL_CHARACTERS) ?: PUBLIC_FAVORITES_LABEL },
            role = profile.text("nameClass", MAX_LABEL_CHARACTERS)
                ?.split(' ')?.firstNotNullOfOrNull(NICK_ROLE_CLASSES::get) ?: ChatRole.Member,
            forumPosts = (profile.opt("posts") as? Number)?.toInt()?.takeIf { it >= 0 },
            flagUrl = profile.text("flag", MAX_URL_CHARACTERS)
                ?.takeUnless { it.substringBefore('?').endsWith(".svg", ignoreCase = true) }
                ?.let { stationUrl(it, origin) },
            rankImageUrl = profile.optJSONObject("rank")?.text("image", MAX_URL_CHARACTERS)
                ?.takeUnless { it.substringBefore('?').endsWith(".svg", ignoreCase = true) }
                ?.let { stationUrl(it, origin) },
            badgeSymbols = badgeEntries.take(MAX_BADGES).mapNotNull { badge ->
                val label = badge.text("label", MAX_LABEL_CHARACTERS) ?: return@mapNotNull null
                // The symbol arrives as a character reference, such as &#128176;.
                val symbol = badge.text("icon", MAX_SYMBOL_ENTITY_CHARACTERS)
                    ?.let { org.jsoup.parser.Parser.unescapeEntities(it, false).trim() }
                    ?.takeIf { it.isNotEmpty() && it.codePointCount(0, it.length) <= MAX_SYMBOL_CODE_POINTS && '&' !in it }
                    ?: return@mapNotNull null
                label to symbol
            }.toMap(),
            websiteUrl = contactLinks["website"]?.let(::webUrl),
            emailPageUrl = contactLinks["email"]?.let { stationUrl(it, origin) }?.takeIf { it.matches(EMAIL_PAGE) },
        )
    }

    /** The tracks in one history answer, newest first as the station lists them. */
    fun parseHistory(json: String, origin: String): List<PlayedHistoryEntry> {
        val html = JSONObject(json).optString("html")
        return Jsoup.parse(html, origin).select("tr").mapNotNull { row -> historyEntry(row, origin) }
            .take(MAX_HISTORY_ENTRIES)
    }

    private fun historyEntry(row: Element, origin: String): PlayedHistoryEntry? {
        val cells = row.select("> td")
        // A signed-in member's rows carry a fifth cell with the station's own favorites controls.
        if (cells.size !in 4..5) return null
        val spans = cells[1].select("span")
        val title = spans.getOrNull(1)?.text()?.trim().orEmpty()
        if (title.isEmpty()) return null
        val times = cells[2].textNodes().map { it.text().trim() }.filter(String::isNotEmpty)
        val playedAt = times.firstOrNull()?.takeIf { it.matches(CLOCK_TIME) } ?: return null
        val requesterLink = cells[3].selectFirst("a[href]")
        val requester = requesterLink?.text()?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARACTERS }
        return PlayedHistoryEntry(
            playedAtLabel = playedAt,
            lengthLabel = times.getOrNull(1)?.takeIf { it.matches(TRACK_LENGTH) },
            title = title.take(MAX_LABEL_CHARACTERS),
            artistName = cells[1].textNodes().firstOrNull()?.text()?.trim()?.takeIf(String::isNotEmpty)
                ?.take(MAX_LABEL_CHARACTERS),
            albumTitle = spans.getOrNull(0)?.text()?.trim()?.takeIf(String::isNotEmpty)?.take(MAX_LABEL_CHARACTERS),
            albumId = cells[0].selectFirst("img[title]")?.attr("title")?.takeIf { it.matches(SAFE_IDENTIFIER) },
            artworkUrl = cells[0].selectFirst("img[src]")?.attr("src")?.let { stationUrl(it, origin) },
            requesterName = requester,
            // A request message follows the requester's link as plain text in the same cell.
            requestMessage = requester?.let {
                cells[3].ownText().trim().takeIf(String::isNotEmpty)?.take(MAX_REQUEST_MESSAGE_CHARACTERS)
            },
        )
    }

    /** The stories on the station's news page, newest first. */
    fun parseNews(html: String, origin: String): List<StationNewsStory> =
        Jsoup.parse(html, origin).select("article.news-story").mapNotNull { story ->
            val link = story.selectFirst("header a[href]") ?: return@mapNotNull null
            val title = link.text().trim().takeIf(String::isNotEmpty) ?: return@mapNotNull null
            val id = STORY_ID.find(link.attr("href"))?.groupValues?.get(1) ?: return@mapNotNull null
            val body = story.selectFirst(".news-full-text")?.let(::plainText).orEmpty()
            if (body.isEmpty()) return@mapNotNull null
            StationNewsStory(
                id = id,
                title = title.take(MAX_LABEL_CHARACTERS),
                publishedLabel = story.selectFirst(".news-story-meta time")?.text()?.substringBefore(" - ")?.trim()
                    ?.takeIf(String::isNotEmpty)?.take(MAX_LABEL_CHARACTERS),
                author = story.selectFirst(".news-story-meta a")?.text()?.trim()
                    ?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_CHARACTERS },
                body = body.take(MAX_STORY_CHARACTERS),
                coverUrls = story.select(".news-selected-covers img[src]")
                    .mapNotNull { stationUrl(it.attr("src"), origin) }
                    .take(MAX_STORY_COVERS),
            )
        }.take(MAX_STORIES)

    /** Story text with the page's line breaks kept and its markup, links, and images dropped. */
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
    /** A member's own link: any web address, but only a plain http or https one. */
    private fun webUrl(value: String): String? = runCatching {
        URI(value.trim()).takeIf {
            (it.scheme.equals("https", ignoreCase = true) || it.scheme.equals("http", ignoreCase = true)) &&
                it.userInfo == null && !it.host.isNullOrBlank() && '.' in it.host
        }?.toASCIIString()
    }.getOrNull()

    private fun stationUrl(value: String, origin: String): String? = runCatching {
        val base = URI(origin)
        val uri = base.resolve(value.trim())
        uri.takeIf {
            it.scheme == "https" && it.userInfo == null &&
                it.host.equals(base.host, ignoreCase = true) && it.port == base.port
        }?.toASCIIString()
    }.getOrNull()

    /** The stations mark a public favorites list with a named badge whose picture differs from station to station. */
    private fun JSONObject.marksPublicFavorites(): Boolean =
        text("label", MAX_LABEL_CHARACTERS).equals(PUBLIC_FAVORITES_LABEL, ignoreCase = true) ||
            text("image", MAX_URL_CHARACTERS)?.substringBefore('?') in PUBLIC_FAVORITES_IMAGES

    private fun JSONObject.text(name: String, maxCharacters: Int): String? =
        // An unset field arrives as `false`, which is not the text "false".
        if (isNull(name) || opt(name) is Boolean) null
        else optString(name).trim().takeIf { it.isNotEmpty() && it.length <= maxCharacters }

    private companion object {
        const val MAX_NAME_CHARACTERS = 60
        const val MAX_LABEL_CHARACTERS = 200
        const val MAX_URL_CHARACTERS = 500
        const val MAX_BADGES = 12
        const val MAX_CONTACTS = 8
        val EMAIL_PAGE = Regex("""https://[^/?#]+/modules\.php\?name=Forums&file=profile&mode=email&u=\d{1,10}""")
        const val MAX_SYMBOL_ENTITY_CHARACTERS = 24
        const val MAX_SYMBOL_CODE_POINTS = 2
        const val MAX_HISTORY_ENTRIES = 120
        const val MAX_REQUEST_MESSAGE_CHARACTERS = 240
        const val MAX_STORIES = 10
        const val MAX_STORY_CHARACTERS = 8_000
        const val MAX_STORY_COVERS = 4
        val CLOCK_TIME = Regex("\\d{2}:\\d{2}:\\d{2}")
        val TRACK_LENGTH = Regex("\\d{1,3}:\\d{2}")
        val SAFE_IDENTIFIER = Regex("[A-Za-z0-9_.-]{1,64}")
        val MEMBER_NUMBER = Regex("[0-9]{1,10}")
        const val PUBLIC_FAVORITES_LABEL = "Public Favorites"
        val PUBLIC_FAVORITES_IMAGES = setOf(
            "/images/favorites/heart.svg",
            "/images/favorites/biohazard.svg",
            "/images/badges/public-favorites.svg",
        )
        val STORY_ID = Regex("[?&]sid=(\\d{1,12})(?:&|$)")
        val SPACES = Regex("[ \\t\\u00A0]+")
        val BLANK_RUN = Regex("\\n{3,}")
        val BLOCK_ELEMENTS = setOf("p", "div", "li", "ul", "ol", "tr", "table", "h1", "h2", "h3", "h4", "blockquote")
    }
}
