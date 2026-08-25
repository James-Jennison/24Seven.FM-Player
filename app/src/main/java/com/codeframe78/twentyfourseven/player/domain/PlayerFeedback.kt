package com.codeframe78.twentyfourseven.player.domain

import java.time.Instant

private const val MaximumFeedbackDescriptionCharacters = 1_000
private const val MaximumFeedbackDiagnosticCharacters = 4_000

enum class FeedbackCategory(val label: String) {
    Playback("Playback"),
    StationSwitching("Station switching"),
    SongRequest("Song request"),
    Chat("Chat"),
    AccountOrProfile("Account or profile"),
    UiOrDisplay("UI or display"),
    Other("Other"),
}

data class FeedbackSubmission(
    val category: FeedbackCategory,
    val optionalDescription: String,
)

/**
 * Creates a draft only. Android presents it to the user, who may edit, cancel, or send it.
 * Diagnostics are included only when the caller supplies the reviewed local snapshot after an
 * explicit consent action in the UI.
 */
fun feedbackEmailDraft(
    stationName: String?,
    submittedAt: Instant,
    submission: FeedbackSubmission,
    diagnosticSnapshot: String?,
): PlayerEmailDraft = PlayerEmailDraft(
    subject = "[24Seven.FM Player feedback] ${submission.category.label}",
    body = buildString {
        appendLine("[24Seven.FM Player feedback]")
        appendLine("Category: ${submission.category.label}")
        appendLine("Station: ${safeFeedbackValue(stationName ?: "None selected", 80)}")
        appendLine("Prepared: $submittedAt")
        safeFeedbackDescription(submission.optionalDescription)?.let {
            appendLine("Description: $it")
        }
        if (diagnosticSnapshot == null) {
            appendLine("Privacy-safe diagnostics: Not included")
        } else {
            appendLine("Privacy-safe diagnostics: Included at the sender's request")
            appendLine()
            appendLine(diagnosticSnapshot.trim().take(MaximumFeedbackDiagnosticCharacters))
        }
        appendLine()
        append("Prepared locally by the Player. Review, edit, cancel, or send this draft in your email app.")
    },
)

private fun safeFeedbackDescription(value: String): String? = safeFeedbackValue(
    value = value,
    maximumLength = MaximumFeedbackDescriptionCharacters,
).takeIf { it != "Not provided" }

private fun safeFeedbackValue(value: String, maximumLength: Int): String = value
    .asSequence()
    .map { if (it.isISOControl()) ' ' else it }
    .joinToString("")
    .trim()
    .replace(Regex("\\s+"), " ")
    .take(maximumLength)
    .ifEmpty { "Not provided" }
