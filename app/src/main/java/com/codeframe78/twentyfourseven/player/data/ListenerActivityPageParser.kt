package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.RequestHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal data class ListenerActivityDiscovery(
    val recentRequests: List<RequestHistoryEntry>,
    val requestTimerUrl: String?,
    val memberProfileUrl: String?,
    /** The request clock the stations now print on the page itself; null on a page without one. */
    val cooldown: RequestCooldownEvidence? = null,
)

internal data class RequestCooldownEvidence(
    val readiness: RequestReadiness,
    val waitMinutes: Int?,
    val queuedRequestWaitSeconds: Int? = null,
)

internal class ListenerActivityPageParser {
    fun parseDiscovery(
        html: String,
        origin: String,
        displayName: String,
    ): ListenerActivityDiscovery {
        val originUri = trustedOrigin(origin)
        val document = Jsoup.parse(html, origin)
        requireSignedIn(document)
        val currentRows = document.select("section.request-history tr")
        val requests = if (currentRows.isNotEmpty()) {
            currentRows.asSequence()
                .mapNotNull { row -> parseCurrentHistoryRow(row, originUri) }
                .take(MAX_HISTORY_ITEMS)
                .mapIndexed { index, entry -> entry.copy(position = index + 1) }
                .toList()
        } else {
            // Before the stations' 2026 upgrade the page listed ten numbered rows under a bold heading.
            document.select("strong")
                .firstOrNull { it.text().trim().equals(LEGACY_HISTORY_HEADING, ignoreCase = true) }
                ?.closest("table")
                ?.select("tr").orEmpty().asSequence()
                .mapNotNull(::parseHistoryRow)
                .take(LEGACY_MAX_HISTORY_ITEMS)
                .toList()
        }
        val timerUrl = document.select("iframe[src]").asSequence()
            .mapNotNull { trustedTimerUrl(it.absUrl("src"), originUri) }
            .firstOrNull()
        val profileUrl = document.select("a[href]").asSequence()
            .filter { it.text().trim().equals(displayName, ignoreCase = true) }
            .mapNotNull { trustedProfileUrl(it.absUrl("href"), originUri) }
            .firstOrNull()
        return ListenerActivityDiscovery(requests, timerUrl, profileUrl, parseRequestClock(document))
    }

    /**
     * The request clock block: seconds until the next request is allowed, the station's own word for the state, and
     * seconds until the listener's earliest queued request should start.
     */
    private fun parseRequestClock(document: org.jsoup.nodes.Document): RequestCooldownEvidence? {
        val clock = document.selectFirst("div[class*=request-clock][data-seconds]") ?: return null
        val seconds = clock.attr("data-seconds").trim().toIntOrNull()?.takeIf { it in 0..MAX_CLOCK_SECONDS }
        val word = clock.selectFirst("[class*=request-value]")?.text()?.trim().orEmpty()
        val readiness = when {
            seconds != null && seconds > 0 -> RequestReadiness.Waiting
            seconds == 0 && word.equals("Ready", ignoreCase = true) -> RequestReadiness.Ready
            else -> RequestReadiness.Unknown
        }
        return RequestCooldownEvidence(
            readiness = readiness,
            waitMinutes = seconds?.takeIf { it > 0 }?.let { (it + 59) / 60 },
            queuedRequestWaitSeconds = clock.attr("data-queue-seconds").trim().toIntOrNull()
                ?.takeIf { it in 0..MAX_CLOCK_SECONDS },
        )
    }

    /** The membership a public profile card names: the stations word it "VIP" or "RIP", and leave it out otherwise. */
    fun membershipTier(membership: String?): MembershipTier = when {
        membership == null -> MembershipTier.Standard
        membership.contains("RIP", ignoreCase = true) -> MembershipTier.Rip
        membership.contains("VIP", ignoreCase = true) -> MembershipTier.Vip
        else -> MembershipTier.Standard
    }

    fun parseCooldown(html: String): RequestCooldownEvidence {
        val document = Jsoup.parse(html)
        requireSignedIn(document)
        val text = document.text().replace(WHITESPACE, " ").trim()
        val waitMinutes = WAIT_MINUTES.find(text)?.groupValues?.get(1)?.toIntOrNull()
        val explicitlyReady = READY.containsMatchIn(text)
        val readiness = when {
            waitMinutes != null && waitMinutes > 0 -> RequestReadiness.Waiting
            explicitlyReady || waitMinutes == 0 -> RequestReadiness.Ready
            else -> RequestReadiness.Unknown
        }
        return RequestCooldownEvidence(readiness, waitMinutes)
    }

    fun parseMembership(html: String, origin: String, displayName: String): MembershipTier {
        val originUri = trustedOrigin(origin)
        val document = Jsoup.parse(html, origin)
        requireSignedIn(document)
        val profileTable = document.select("th")
            .firstOrNull {
                it.text().contains("Viewing profile", ignoreCase = true) &&
                    it.text().contains(displayName, ignoreCase = true)
            }
            ?.closest("table")
            ?: currentProfileCard(document)
            ?: return MembershipTier.Unknown
        val membershipNames = profileTable.select("a[href]").asSequence()
            .mapNotNull { membershipName(it, originUri) }
            .toSet()
        return when {
            "RIP_Subscribe" in membershipNames -> MembershipTier.Rip
            "VIP_Subscribe" in membershipNames -> MembershipTier.Vip
            else -> MembershipTier.Standard
        }
    }

    /**
     * The current station profile page replaced its legacy \"Viewing profile\" table with a
     * profile card beside the `.profile-tabs` panel. Scope membership evidence to that card so
     * site-wide membership navigation cannot turn every account into a VIP/RIP account.
     */
    private fun currentProfileCard(document: org.jsoup.nodes.Document): Element? = document
        .selectFirst(".profile-tabs")
        ?.parent()
        ?.parent()
        ?.selectFirst("table.table01")

    /** A row of the current page: a cover, the track in bold, the artist, a link to the album, and the time. */
    private fun parseCurrentHistoryRow(row: Element, origin: URI): RequestHistoryEntry? {
        val cells = row.children().filter { it.tagName() == "td" }
        if (cells.size < 2) return null
        val heading = cells[0].selectFirst("strong") ?: return null
        val artist = heading.parent()?.ownText()?.trim()?.takeIf(String::isNotEmpty)
        // The station has a few library tracks with no title; the request still counts and still names its album.
        val track = heading.text().trim().ifEmpty { UNTITLED_TRACK }
        val albumLink = cells[0].select("a[href]").firstOrNull { link ->
            val uri = runCatching { URI(link.absUrl("href")) }.getOrNull()
            uri != null && isSameOrigin(uri, origin) && uri.path == MODULES_PATH &&
                queryValues(uri.rawQuery)["name"] == "Album"
        }
        val albumId = albumLink?.let { queryValues(URI(it.absUrl("href")).rawQuery)["asin"] }
            ?.takeIf { it.matches(ALBUM_ID) }
        val artwork = cells[0].selectFirst("img[src]")?.absUrl("src")
            ?.let { runCatching { URI(it) }.getOrNull() }
            ?.takeIf { isSameOrigin(it, origin) && it.path.startsWith("/images/cover/") }
            ?.toASCIIString()
        val requestedAt = cells[1].text().trim().take(MAX_REQUESTED_AT_CHARACTERS)
        if (requestedAt.isBlank()) return null
        return RequestHistoryEntry(
            position = 0,
            trackSummary = listOfNotNull(track, artist).joinToString(" — ").take(MAX_HISTORY_SUMMARY_CHARACTERS),
            requestedAtLabel = requestedAt,
            albumTitle = albumLink?.text()?.trim()?.takeIf(String::isNotEmpty)?.take(MAX_HISTORY_SUMMARY_CHARACTERS),
            albumId = albumId,
            artworkUrl = artwork,
        )
    }

    private fun parseHistoryRow(row: Element): RequestHistoryEntry? {
        val cells = row.children().filter { it.tagName() == "td" }
        if (cells.size < 3) return null
        val match = HISTORY_POSITION.matchEntire(cells[1].text().trim()) ?: return null
        val position = match.groupValues[1].toIntOrNull() ?: return null
        val summary = match.groupValues[2].trim().take(MAX_HISTORY_SUMMARY_CHARACTERS)
        val requestedAt = cells[2].text().trim().take(MAX_REQUESTED_AT_CHARACTERS)
        if (summary.isBlank() || requestedAt.isBlank()) return null
        return RequestHistoryEntry(position, summary, requestedAt)
    }

    private fun trustedTimerUrl(url: String, origin: URI): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!isSameOrigin(uri, origin) || uri.rawQuery != null) return null
        if (uri.path !in TIMER_PATHS) return null
        return uri.toASCIIString()
    }

    private fun trustedProfileUrl(url: String, origin: URI): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!isSameOrigin(uri, origin) || uri.path != MODULES_PATH) return null
        val query = queryValues(uri.rawQuery)
        if (
            query["name"] != "Forums" ||
            query["file"] != "profile" ||
            query["mode"] != "viewprofile" ||
            query["u"]?.matches(NUMERIC_ID) != true
        ) return null
        return uri.toASCIIString()
    }

    private fun membershipName(link: Element, origin: URI): String? {
        val uri = runCatching { URI(link.absUrl("href")) }.getOrNull() ?: return null
        if (!isSameOrigin(uri, origin) || uri.path != MODULES_PATH) return null
        val name = queryValues(uri.rawQuery)["name"] ?: return null
        val badge = sequenceOf(link.text(), link.selectFirst("img")?.attr("alt"), link.selectFirst("img")?.attr("title"))
            .filterNotNull()
            .joinToString(" ")
        return name.takeIf {
            (it == "VIP_Subscribe" && badge.contains("VIP", ignoreCase = true)) ||
                (it == "RIP_Subscribe" && badge.contains("RIP", ignoreCase = true))
        }
    }

    private fun requireSignedIn(document: org.jsoup.nodes.Document) {
        if (document.selectFirst("input[name=user_password]") != null) {
            throw ListenerActivityAuthenticationRequiredException()
        }
    }

    private fun trustedOrigin(origin: String): URI = URI(origin).also {
        require(it.scheme == "https" && it.path == "/")
    }

    private fun isSameOrigin(uri: URI, origin: URI): Boolean =
        uri.scheme == "https" && uri.host.equals(origin.host, true) && uri.port == origin.port

    private fun queryValues(query: String?): Map<String, String> = query
        ?.split('&')
        ?.mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
        ?.associate { decode(it[0]) to decode(it[1]) }
        .orEmpty()

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val LEGACY_HISTORY_HEADING = "Your Last 10 Requests"
        const val MODULES_PATH = "/modules.php"
        const val LEGACY_MAX_HISTORY_ITEMS = 10
        const val MAX_HISTORY_ITEMS = 50
        const val UNTITLED_TRACK = "Untitled track"
        const val MAX_CLOCK_SECONDS = 7 * 24 * 60 * 60
        val ALBUM_ID = Regex("^[A-Za-z0-9_.-]{1,64}$")
        const val MAX_HISTORY_SUMMARY_CHARACTERS = 300
        const val MAX_REQUESTED_AT_CHARACTERS = 64
        val HISTORY_POSITION = Regex("^([0-9]{1,2})\\.\\s+(.+)$")
        val WAIT_MINUTES = Regex("Request\\s+Wait:\\s*([0-9]{1,4})\\s+Minutes?", RegexOption.IGNORE_CASE)
        val READY = Regex("Your\\s+Request:\\s*Ready", RegexOption.IGNORE_CASE)
        val WHITESPACE = Regex("\\s+")
        val NUMERIC_ID = Regex("^[0-9]{1,10}$")
        val TIMER_PATHS = setOf(
            "/modules/VIP_Subscribe/vip_req_timer.php",
            "/modules/RIP_Subscribe/rip_req_timer.php",
        )
    }
}

internal class ListenerActivityAuthenticationRequiredException : IOException("Station sign-in is required")
