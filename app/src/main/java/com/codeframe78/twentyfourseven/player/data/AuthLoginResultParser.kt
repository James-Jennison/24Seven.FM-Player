package com.codeframe78.twentyfourseven.player.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.URI

/** What a station page says about the session that fetched it. */
internal enum class SignedInEvidence {
    /** The page carries the member-only logout action and does not name a different account. */
    Confirmed,

    /** The page shows the login form, offers registration to a visitor, or names a different account. */
    SignedOut,

    /** The page shows neither, as a maintenance or error page would; it says nothing about the session. */
    Unknown,
}

internal class AuthLoginResultParser {
    fun parseSignedInDisplayName(html: String, origin: String, username: String): String =
        when (signedInEvidence(html, origin, username)) {
            SignedInEvidence.Confirmed -> username
            SignedInEvidence.SignedOut -> throw IOException("Station did not sign in this account")
            SignedInEvidence.Unknown -> throw IOException("Signed-in account was not recognized")
        }

    fun signedInEvidence(html: String, origin: String, username: String): SignedInEvidence {
        val originUri = trustedOrigin(origin)
        val document = Jsoup.parse(html, origin)
        if (document.showsLoginForm()) return SignedInEvidence.SignedOut
        val logoutLinks = document.accountLinks(originUri, operation = "logout")
        if (logoutLinks.isEmpty()) {
            return if (document.offersRegistration(originUri)) SignedInEvidence.SignedOut else SignedInEvidence.Unknown
        }

        // The station names the signed-in account in a welcome line or as the label of the account menu that holds
        // the logout action. A page naming a different account is rejected. A page naming nobody is still a
        // signed-in page, so a station wording change cannot lock listeners out.
        val hasWelcome = document.text().contains("Welcome, $username", ignoreCase = true)
        val accountMenuLabels = logoutLinks.flatMap { it.accountMenuLabels(originUri) }
        val namesAnotherAccount = !hasWelcome &&
            accountMenuLabels.isNotEmpty() &&
            accountMenuLabels.none { it.contains(username, ignoreCase = true) }
        return if (namesAnotherAccount) SignedInEvidence.SignedOut else SignedInEvidence.Confirmed
    }

    /** True only when the page positively shows a visitor: the login form, or registration without a logout. */
    fun showsSignedOutVisitor(html: String, origin: String): Boolean {
        val originUri = trustedOrigin(origin)
        val document = Jsoup.parse(html, origin)
        return document.showsLoginForm() ||
            (document.accountLinks(originUri, operation = "logout").isEmpty() && document.offersRegistration(originUri))
    }

    private fun trustedOrigin(origin: String): URI = URI(origin).also {
        require(it.scheme == "https" && it.path == "/")
    }

    private fun Document.showsLoginForm(): Boolean = selectFirst("input[name=user_password]") != null

    private fun Document.offersRegistration(origin: URI): Boolean =
        accountLinks(origin, operation = "new_user").isNotEmpty()

    private fun Document.accountLinks(origin: URI, operation: String): List<Element> =
        select("a[href]").filter { it.accountOperation(origin) == operation }

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
