package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codeframe78.twentyfourseven.player.domain.AbuseReportCategory
import com.codeframe78.twentyfourseven.player.domain.AbuseReportKind
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSource
import com.codeframe78.twentyfourseven.player.domain.AbuseReportStatus
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSubmission
import com.codeframe78.twentyfourseven.player.domain.AbuseReportTarget
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyRepository
import com.codeframe78.twentyfourseven.player.domain.PLAYER_CONTACT_EMAIL
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SharedPreferencesCommunitySafetyRepositoryTest {
    @Test
    fun adultTermsVisibilityAndStationBlocksSurviveRepositoryRecreation() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "m28-community-safety-test"
        val stored = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        stored.edit().clear().commit()
        try {
            repository(context, name).apply {
                submitAgeScreen(1990, 4, 3)
                acceptTerms()
                setCommunityContentVisible(true)
                blockUser(StationId("sst"), " MorG Hubby ")
            }

            val restored = repository(context, name).observeSafety().first()
            assertTrue(restored.canViewCommunityContent)
            assertTrue(restored.isBlocked(StationId("sst"), "morg hubby"))
            assertFalse(restored.isBlocked(StationId("afm"), "morg hubby"))
        } finally {
            stored.edit().clear().commit()
        }
    }

    @Test
    fun legacyBlockedUserStationIdMigratesInMemoryAndOnNextWrite() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "m32-community-safety-migration-test"
        val stored = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        stored.edit().clear().putStringSet("blocked_users", setOf("adagio|Legacy+Listener")).commit()
        try {
            val repository = repository(context, name)
            assertTrue(repository.observeSafety().first().isBlocked(StationId("afm"), "Legacy Listener"))

            repository.blockUser(StationId("sst"), "Current Listener")
            assertTrue(stored.getStringSet("blocked_users", emptySet()).orEmpty().any { it.startsWith("afm|") })
            assertFalse(stored.getStringSet("blocked_users", emptySet()).orEmpty().any { it.startsWith("adagio|") })
        } finally {
            stored.edit().clear().commit()
        }
    }

    @Test
    fun reportStatePreparesBoundedTransientEmailDraftAndDoesNotPersistIt() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "m28-community-report-email-test"
        val stored = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        stored.edit().clear().commit()
        try {
            val repository = repository(context, name)
            repository.beginReport(
                StationId("sst"),
                AbuseReportTarget(
                    AbuseReportKind.Content,
                    AbuseReportSource.Chat,
                    reportedUser = "Reported Listener",
                    displayedTimestamp = "12:34",
                    contentSnapshot = "x".repeat(800),
                ),
            )
            assertEquals(AbuseReportStatus.Ready, repository.observeReport().first().status)

            repository.submitReport(
                AbuseReportSubmission(
                    reporterName = "Reporter",
                    category = AbuseReportCategory.Harassment,
                    optionalDetails = "Harmless test details",
                ),
            )

            val prepared = repository.observeReport().first()
            assertEquals(AbuseReportStatus.EmailReady, prepared.status)
            assertEquals(PLAYER_CONTACT_EMAIL, prepared.emailDraft?.recipient)
            assertTrue(prepared.emailDraft?.body.orEmpty().contains("Reporter name or station nickname: Reporter"))
            assertTrue(prepared.emailDraft?.body.orEmpty().contains("x".repeat(500)))
            assertFalse(prepared.emailDraft?.body.orEmpty().contains("x".repeat(501)))

            repository.reportEmailComposerResult(opened = true)
            val opened = repository.observeReport().first()
            assertEquals(AbuseReportStatus.EmailHandoffStarted, opened.status)
            assertNull(opened.emailDraft)

            val recreated = repository(context, name)
            assertEquals(AbuseReportStatus.Idle, recreated.observeReport().first().status)
        } finally {
            stored.edit().clear().commit()
        }
    }

    @Test
    fun unavailableEmailComposerReportsNoDeliveryAndAllowsAnotherHandoffAttempt() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "m28-community-report-email-unavailable-test"
        val stored = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        stored.edit().clear().commit()
        try {
            val repository = repository(context, name)
            repository.beginReport(
                StationId("sst"),
                AbuseReportTarget(
                    AbuseReportKind.User,
                    AbuseReportSource.Chat,
                    reportedUser = "Reported Listener",
                ),
            )
            repository.submitReport(
                AbuseReportSubmission(
                    reporterName = "Reporter",
                    category = AbuseReportCategory.Other,
                    optionalDetails = "",
                ),
            )
            repository.reportEmailComposerResult(opened = false)

            val report = repository.observeReport().first()
            assertEquals(AbuseReportStatus.Error, report.status)
            assertTrue(report.retryAllowed)
            assertTrue(report.errorMessage.orEmpty().contains("not sent"))
            assertNull(report.emailDraft)
        } finally {
            stored.edit().clear().commit()
        }
    }

    private fun repository(context: Context, name: String): CommunitySafetyRepository =
        SharedPreferencesCommunitySafetyRepository(
            context = context,
            today = { LocalDate.of(2026, 7, 18) },
            preferencesName = name,
        )
}
