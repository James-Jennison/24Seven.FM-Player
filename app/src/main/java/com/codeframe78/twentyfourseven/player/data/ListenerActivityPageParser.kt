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
)

internal data class RequestCooldownEvidence(
    val readiness: RequestReadiness,
    val waitMinutes: Int?,
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
        val historyTable = document.select("strong")
            .firstOrNull { it.text().trim().equals(HISTORY_HEADING, ignoreCase = true) }
            ?.closest("table")
        val requests = historyTable?.select("tr").orEmpty().asSequence()
            .mapNotNull(::parseHistoryRow)
            .take(MAX_HISTORY_ITEMS)
            .toList()
        val timerUrl = document.select("iframe[src]").asSequence()
            .mapNotNull { trustedTimerUrl(it.absUrl("src"), originUri) }
            .firstOrNull()
        val profileUrl = document.select("a[href]").asSequence()
            .filter { it.text().trim().equals(displayName, ignoreCase = true) }
            .mapNotNull { trustedProfileUrl(it.absUrl("href"), originUri) }
            .firstOrNull()
        return ListenerActivityDiscovery(requests, timerUrl, profileUrl)
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
        const val HISTORY_HEADING = "Your Last 10 Requests"
        const val MODULES_PATH = "/modules.php"
        const val MAX_HISTORY_ITEMS = 10
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
