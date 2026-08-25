package com.codeframe78.twentyfourseven.player.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerContactTest {
    @Test
    fun `station contact draft uses the fixed monitored recipient and selected station`() {
        val station = Station(
            id = StationId("afm"),
            name = "Adagio.FM",
            shortName = "Adagio",
            description = "Classical",
            websiteUrl = "https://adagio.fm/",
        )

        val draft = stationContactEmailDraft(station)

        assertEquals(PLAYER_CONTACT_EMAIL, draft.recipient)
        assertTrue(draft.subject.contains("Adagio.FM"))
        assertTrue(draft.body.contains("Station: Adagio.FM"))
    }

    @Test
    fun `abuse report draft is station scoped bounded and user reviewed`() {
        mapOf(
            "sst" to "StreamingSoundtracks.com",
            "1980s" to "1980s.FM",
            "afm" to "Adagio.FM",
            "dfm" to "Death.FM",
            "efm" to "Entranced.FM",
        ).forEach { (stationId, stationName) ->
            val stationDraft = abuseReportEmailDraft(
                stationId = StationId(stationId),
                target = AbuseReportTarget(
                    kind = AbuseReportKind.User,
                    source = AbuseReportSource.Chat,
                    reportedUser = "Reported Listener",
                ),
                submission = AbuseReportSubmission(
                    reporterName = "Reporter",
                    category = AbuseReportCategory.Other,
                    optionalDetails = "",
                ),
            )
            assertTrue(stationDraft.subject.contains(stationName))
            assertTrue(stationDraft.body.contains("Station: $stationName"))
        }

        val draft = abuseReportEmailDraft(
            stationId = StationId("sst"),
            target = AbuseReportTarget(
                kind = AbuseReportKind.Content,
                source = AbuseReportSource.Chat,
                reportedUser = "Reported Listener",
                displayedTimestamp = "12:34",
                contentSnapshot = "x".repeat(800),
            ),
            submission = AbuseReportSubmission(
                reporterName = " Reporter ",
                category = AbuseReportCategory.Harassment,
                optionalDetails = "Please   review this harmless test.",
            ),
        )

        assertEquals(PLAYER_CONTACT_EMAIL, draft.recipient)
        assertTrue(draft.subject.contains("StreamingSoundtracks.com"))
        assertTrue(draft.body.contains("Reporter name or station nickname: Reporter"))
        assertTrue(draft.body.contains("Please review this harmless test."))
        assertTrue(draft.body.contains("x".repeat(500)))
        assertFalse(draft.body.contains("x".repeat(501)))
        assertTrue(draft.body.contains("review and send this draft"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `abuse report draft rejects unsupported stations`() {
        abuseReportEmailDraft(
            stationId = StationId("unknown"),
            target = AbuseReportTarget(
                kind = AbuseReportKind.User,
                source = AbuseReportSource.Request,
                reportedUser = "Listener",
            ),
            submission = AbuseReportSubmission(
                reporterName = "Reporter",
                category = AbuseReportCategory.Other,
                optionalDetails = "",
            ),
        )
    }

    @Test
    fun `feedback draft is locally reviewable and includes diagnostics only when explicitly supplied`() {
        val withoutDiagnostics = feedbackEmailDraft(
            stationName = "Adagio.FM",
            submittedAt = Instant.parse("2026-08-25T12:34:56Z"),
            submission = FeedbackSubmission(
                category = FeedbackCategory.UiOrDisplay,
                optionalDescription = " Large type\nclips the station picker ",
            ),
            diagnosticSnapshot = null,
        )

        assertEquals(PLAYER_CONTACT_EMAIL, withoutDiagnostics.recipient)
        assertTrue(withoutDiagnostics.subject.contains("UI or display"))
        assertTrue(withoutDiagnostics.body.contains("Station: Adagio.FM"))
        assertTrue(withoutDiagnostics.body.contains("Prepared: 2026-08-25T12:34:56Z"))
        assertTrue(withoutDiagnostics.body.contains("Description: Large type clips the station picker"))
        assertTrue(withoutDiagnostics.body.contains("Privacy-safe diagnostics: Not included"))
        assertTrue(withoutDiagnostics.body.contains("Review, edit, cancel, or send"))

        val withDiagnostics = feedbackEmailDraft(
            stationName = "Adagio.FM",
            submittedAt = Instant.parse("2026-08-25T12:34:56Z"),
            submission = FeedbackSubmission(FeedbackCategory.Playback, ""),
            diagnosticSnapshot = "24Seven.FM Player diagnostics\nApp version: 0.1.0-alpha08-debug",
        )

        assertTrue(withDiagnostics.body.contains("Privacy-safe diagnostics: Included at the sender's request"))
        assertTrue(withDiagnostics.body.contains("App version: 0.1.0-alpha08-debug"))
    }
}
