package com.codeframe78.twentyfourseven.player.data

import org.jsoup.Jsoup

/** What a station's Studio page says about the current track and the signed-in listener's favorite control. */
internal data class StudioNowPlaying(
    val trackTitle: String?,
    val songId: String?,
    val isFavorite: Boolean,
    val asksToSignIn: Boolean,
)

internal class StudioNowPlayingParser {
    fun parse(html: String, origin: String): StudioNowPlaying {
        val document = Jsoup.parse(html, origin)
        val zone = document.getElementById("now-playing-zone")
        val favoriteControl = zone?.selectFirst("a.fav-button")
        val action = favoriteControl?.attr("onclick").orEmpty()
        val heart = favoriteControl?.selectFirst("img")
        return StudioNowPlaying(
            trackTitle = zone?.selectFirst(".studio-info-heading .studio-info-line span")
                ?.text()
                ?.trim()
                ?.takeIf(String::isNotEmpty),
            songId = ADD_TO_FAVORITES.find(action)?.groupValues?.get(1),
            isFavorite = heart != null && !heart.hasClass("favorite-inactive"),
            asksToSignIn = SIGN_IN_PROMPT.containsMatchIn(action) ||
                SIGN_IN_PROMPT.containsMatchIn(favoriteControl?.attr("title").orEmpty()),
        )
    }

    private companion object {
        val ADD_TO_FAVORITES = Regex("addToFavorites\\(\\s*'?(\\d{1,12})'?\\s*\\)")
        val SIGN_IN_PROMPT = Regex("log(ged)?\\s+in", RegexOption.IGNORE_CASE)
    }
}
