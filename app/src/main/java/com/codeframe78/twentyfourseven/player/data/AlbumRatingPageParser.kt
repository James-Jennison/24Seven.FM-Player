package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AlbumRatingOption
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal enum class AlbumRatingAccess { CanRate, AlreadyRated, SignInRequired, Unknown }

internal data class AlbumRatingPage(
    val access: AlbumRatingAccess,
    val albumTitle: String?,
    val artist: String?,
    val currentRating: String?,
    val voteCount: String?,
    val options: List<AlbumRatingOption>,
    /** Every field the station's own form carries besides the rating, sent back unchanged. */
    val formFields: List<Pair<String, String>>,
)

internal class AlbumRatingPageParser {
    fun parse(html: String, origin: String): AlbumRatingPage {
        val document = Jsoup.parse(html, origin)
        val card = document.selectFirst(".rating-card") ?: document.body()
        val form = card.select("form").firstOrNull { it.selectFirst("select[name=rating]") != null }
        val options = form?.select("select[name=rating] option").orEmpty()
            .map { AlbumRatingOption(it.attr("value").trim(), it.text().trim()) }
            .filter { it.value.matches(RATING_VALUE) && it.value.toDouble() > 0.0 && it.label.isNotEmpty() }
        val text = card.text()
        val access = when {
            form != null && options.isNotEmpty() -> AlbumRatingAccess.CanRate
            ALREADY_RATED.containsMatchIn(text) -> AlbumRatingAccess.AlreadyRated
            SIGN_IN_PROMPT.containsMatchIn(text) -> AlbumRatingAccess.SignInRequired
            else -> AlbumRatingAccess.Unknown
        }
        return AlbumRatingPage(
            access = access,
            albumTitle = card.selectFirst(".rating-title").textOrNull(),
            artist = card.selectFirst(".rating-artist").textOrNull(),
            currentRating = card.selectFirst(".rating-box[title]")?.attr("title")?.trim()?.takeIf(String::isNotEmpty),
            voteCount = VOTES.find(text)?.groupValues?.get(1),
            options = if (access == AlbumRatingAccess.CanRate) options else emptyList(),
            formFields = if (access == AlbumRatingAccess.CanRate) {
                form?.select("input[name]").orEmpty()
                    .filter { it.attr("type").lowercase() in setOf("hidden", "submit") }
                    .map { it.attr("name") to it.attr("value") }
            } else {
                emptyList()
            },
        )
    }

    private fun Element?.textOrNull(): String? = this?.text()?.trim()?.takeIf(String::isNotEmpty)

    private companion object {
        val RATING_VALUE = Regex("\\d(\\.\\d)?")
        val ALREADY_RATED = Regex("already\\s+rated", RegexOption.IGNORE_CASE)
        val SIGN_IN_PROMPT = Regex("log\\s+in\\s+to\\s+rate", RegexOption.IGNORE_CASE)
        val VOTES = Regex("\\((\\d[\\d,]*)\\s+votes?\\)", RegexOption.IGNORE_CASE)
    }
}
