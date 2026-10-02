package com.codeframe78.twentyfourseven.player.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI

internal class AuthLoginResultParser {
    fun parseSignedInDisplayName(html: String, origin: String, username: String): String {
        val originUri = URI(origin)
        require(originUri.scheme == "https" && originUri.path == "/")
        val document = Jsoup.parse(html, origin)
        if (document.selectFirst("input[name=user_password]") != null) {
            throw IOException("Station still shows the login form")
        }
        val logoutLinks = document.select("a[href]").filter { it.accountOperation(originUri) == "logout" }
        if (logoutLinks.isEmpty()) throw IOException("Signed-in account was not recognized")

        // The station names the signed-in account in a welcome line or as the label of the account menu that holds
        // the logout action. A page naming a different account is rejected. A page naming nobody is still a
        // signed-in page, so a station wording change cannot lock listeners out.
        val hasWelcome = document.text().contains("Welcome, $username", ignoreCase = true)
        val accountMenuLabels = logoutLinks.flatMap { it.accountMenuLabels(originUri) }
        if (
            !hasWelcome &&
            accountMenuLabels.isNotEmpty() &&
            accountMenuLabels.none { it.contains(username, ignoreCase = true) }
        ) {
            throw IOException("Signed-in account did not match")
        }
        return username
    }

    /** The Your_Account operation of a same-origin account link, or an empty string for the account page itself. */
    private fun Element.accountOperation(origin: URI): String? {
        val uri = runCatching { URI(absUrl("href")) }.getOrNull() ?: return null
        if (
            uri.scheme != "https" ||
            !uri.host.equals(origin.host, ignoreCase = true) ||
            uri.port != origin.port ||
            uri.path != "/modules.php" ||
            queryValue(uri.rawQuery, "name") != "Your_Account"
        ) {
            return null
        }
        return queryValue(uri.rawQuery, "op").orEmpty()
    }

    private fun Element.accountMenuLabels(origin: URI): List<String> = parents()
        .filter { it.tagName() == "li" }
        .flatMap { item -> item.children().filter { it.tagName() == "a" && it.accountOperation(origin) == "" } }
        .map { it.ownText().trim() }
        .filter { it.isNotEmpty() }

    private fun queryValue(query: String?, name: String): String? = query
        ?.split('&')
        ?.mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
        ?.firstOrNull { it[0] == name }
        ?.get(1)
}
