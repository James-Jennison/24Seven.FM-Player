package com.codeframe78.twentyfourseven.player.domain

const val PLAYER_CONTACT_EMAIL = "morg@24seven.fm"

data class PlayerEmailDraft(
    val recipient: String = PLAYER_CONTACT_EMAIL,
    val subject: String,
    val body: String,
)

fun stationContactEmailDraft(station: Station): PlayerEmailDraft = PlayerEmailDraft(
    subject = "24Seven.FM Player contact — ${station.name}",
    body = buildString {
        appendLine("Station: ${station.name}")
        appendLine("App: 24Seven.FM Player")
        appendLine()
        append("Please describe your question or concern.")
    },
)

fun abuseReportEmailDraft(
    stationId: StationId,
    target: AbuseReportTarget,
    submission: AbuseReportSubmission,
): PlayerEmailDraft {
    val stationName = STATION_NAMES[stationId.canonicalized()]
        ?: throw IllegalArgumentException("Unsupported station.")
    val reporterName = submission.reporterName.trim().take(MAX_REPORTER_NAME_CHARACTERS)
    val details = submission.optionalDetails.trim().replace(Regex("\\s+"), " ").take(MAX_DETAILS_CHARACTERS)
    require(reporterName.length >= 2) { "Enter your name or station nickname." }

    val boundedTarget = target.copy(
        reportedUser = target.reportedUser.trim().take(MAX_REPORTED_USER_CHARACTERS),
        displayedTimestamp = target.displayedTimestamp?.trim()?.take(MAX_TIMESTAMP_CHARACTERS)
            ?.takeIf(String::isNotEmpty),
        contentSnapshot = target.contentSnapshot?.trim()?.replace(Regex("\\s+"), " ")
            ?.take(MAX_CONTENT_SNAPSHOT_CHARACTERS)?.takeIf(String::isNotEmpty),
    )
    require(boundedTarget.reportedUser.isNotEmpty()) { "The reported user is missing." }

    return PlayerEmailDraft(
        subject = "[24Seven.FM Player report] $stationName — ${target.kind.label}",
        body = buildString {
            appendLine("[24Seven.FM Player abuse report]")
            appendLine("Report type: ${target.kind.label}")
            appendLine("Category: ${submission.category.label}")
            appendLine("Station: $stationName")
            appendLine("Source: ${target.source.label}")
            appendLine("Reported username: ${boundedTarget.reportedUser}")
            boundedTarget.displayedTimestamp?.let { appendLine("Displayed timestamp: $it") }
            boundedTarget.contentSnapshot?.let { appendLine("Content snapshot: $it") }
            appendLine("Reporter name or station nickname: $reporterName")
            if (details.isNotEmpty()) appendLine("Optional details: $details")
            appendLine()
            append("Prepared by the authorized native reporting flow. The sender must review and send this draft in their email app.")
        }.take(MAX_REPORT_BODY_CHARACTERS),
    )
}

private const val MAX_REPORTER_NAME_CHARACTERS = 100
private const val MAX_REPORTED_USER_CHARACTERS = 80
private const val MAX_TIMESTAMP_CHARACTERS = 40
private const val MAX_CONTENT_SNAPSHOT_CHARACTERS = 500
private const val MAX_DETAILS_CHARACTERS = 500
private const val MAX_REPORT_BODY_CHARACTERS = 2_000

private val STATION_NAMES = mapOf(
    StationId("sst") to "StreamingSoundtracks.com",
    StationId("1980s") to "1980s.FM",
    StationId("afm") to "Adagio.FM",
    StationId("dfm") to "Death.FM",
    StationId("efm") to "Entranced.FM",
)
