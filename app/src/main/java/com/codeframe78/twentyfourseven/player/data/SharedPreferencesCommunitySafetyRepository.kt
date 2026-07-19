package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import com.codeframe78.twentyfourseven.player.domain.AbuseReportState
import com.codeframe78.twentyfourseven.player.domain.AbuseReportStatus
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSubmission
import com.codeframe78.twentyfourseven.player.domain.AbuseReportTarget
import com.codeframe78.twentyfourseven.player.domain.AgeGateStatus
import com.codeframe78.twentyfourseven.player.domain.BlockedCommunityUser
import com.codeframe78.twentyfourseven.player.domain.CURRENT_COMMUNITY_TERMS_VERSION
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyRepository
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyState
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.abuseReportEmailDraft
import com.codeframe78.twentyfourseven.player.domain.isAdultOnDate
import com.codeframe78.twentyfourseven.player.domain.normalizedCommunityIdentity
import com.codeframe78.twentyfourseven.player.domain.validatedBirthDate
import com.codeframe78.twentyfourseven.player.domain.toSupportedStationIdOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

class SharedPreferencesCommunitySafetyRepository internal constructor(
    context: Context,
    private val today: () -> LocalDate = LocalDate::now,
    preferencesName: String = PREFERENCES_NAME,
) : CommunitySafetyRepository {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val safety = MutableStateFlow(readSafety())
    private val report = MutableStateFlow(AbuseReportState())

    override fun observeSafety(): Flow<CommunitySafetyState> = safety.asStateFlow()

    override fun observeReport(): Flow<AbuseReportState> = report.asStateFlow()

    override suspend fun submitAgeScreen(year: Int, month: Int, day: Int) {
        val currentDate = today()
        val birthDate = validatedBirthDate(year, month, day, currentDate)
        if (birthDate == null) {
            safety.value = safety.value.copy(ageGateErrorMessage = "Enter a valid date of birth.")
            return
        }
        val result = if (isAdultOnDate(birthDate, currentDate)) AgeGateStatus.Adult else AgeGateStatus.Underage
        updateSafety(
            safety.value.copy(
                ageGateStatus = result,
                acceptedTermsVersion = safety.value.acceptedTermsVersion.takeIf { result == AgeGateStatus.Adult },
                communityContentVisible = safety.value.communityContentVisible && result == AgeGateStatus.Adult,
                ageGateErrorMessage = null,
            ),
        )
    }

    override suspend fun acceptTerms() {
        if (safety.value.ageGateStatus != AgeGateStatus.Adult) return
        updateSafety(safety.value.copy(acceptedTermsVersion = CURRENT_COMMUNITY_TERMS_VERSION))
    }

    override suspend fun setCommunityContentVisible(visible: Boolean) {
        val allowed = safety.value.ageGateStatus == AgeGateStatus.Adult && safety.value.hasAcceptedCurrentTerms
        updateSafety(safety.value.copy(communityContentVisible = visible && allowed))
    }

    override suspend fun blockUser(stationId: StationId, displayName: String) {
        val boundedName = displayName.trim().take(MAX_REPORTED_USER_CHARACTERS)
        if (boundedName.isEmpty()) return
        val identity = boundedName.normalizedCommunityIdentity()
        val retained = safety.value.blockedUsers.filterNot {
            it.stationId == stationId && it.normalizedIdentity == identity
        }
        updateSafety(
            safety.value.copy(
                blockedUsers = (retained + BlockedCommunityUser(stationId, boundedName, identity))
                    .sortedWith(compareBy({ it.stationId.value }, { it.displayName.lowercase() })),
            ),
        )
    }

    override suspend fun unblockUser(stationId: StationId, displayName: String) {
        val identity = displayName.normalizedCommunityIdentity()
        updateSafety(
            safety.value.copy(
                blockedUsers = safety.value.blockedUsers.filterNot {
                    it.stationId == stationId && it.normalizedIdentity == identity
                },
            ),
        )
    }

    override suspend fun beginReport(stationId: StationId, target: AbuseReportTarget) {
        report.value = AbuseReportState(
            stationId = stationId,
            target = target.bounded(),
            status = AbuseReportStatus.Ready,
        )
    }

    override suspend fun submitReport(submission: AbuseReportSubmission) {
        val currentReport = report.value
        val target = currentReport.target
        val stationId = currentReport.stationId
        if (currentReport.status != AbuseReportStatus.Ready || target == null || stationId == null) return

        report.value = currentReport.copy(status = AbuseReportStatus.PreparingEmail, errorMessage = null)
        runCatching { abuseReportEmailDraft(stationId, target, submission) }
            .onSuccess { draft ->
                report.value = report.value.copy(
                    status = AbuseReportStatus.EmailReady,
                    emailDraft = draft,
                    errorMessage = null,
                    retryAllowed = false,
                )
            }
            .onFailure { error ->
                report.value = currentReport.copy(
                    errorMessage = error.message?.takeIf(String::isNotBlank)
                        ?: "Check the report fields and try again.",
                    retryAllowed = true,
                )
            }
    }

    override fun reportEmailComposerResult(opened: Boolean) {
        val currentReport = report.value
        if (currentReport.status != AbuseReportStatus.EmailReady) return
        report.value = if (opened) {
            currentReport.copy(
                status = AbuseReportStatus.EmailHandoffStarted,
                emailDraft = null,
                errorMessage = null,
                retryAllowed = false,
            )
        } else {
            currentReport.copy(
                status = AbuseReportStatus.Error,
                emailDraft = null,
                errorMessage = "No email app is available. The report was not sent.",
                retryAllowed = true,
            )
        }
    }

    override fun dismissReport() {
        report.value = AbuseReportState()
    }

    private fun updateSafety(updated: CommunitySafetyState) {
        safety.value = updated
        preferences.edit()
            .putString(KEY_AGE_GATE_STATUS, updated.ageGateStatus.name)
            .apply {
                if (updated.acceptedTermsVersion == null) remove(KEY_TERMS_VERSION)
                else putString(KEY_TERMS_VERSION, updated.acceptedTermsVersion)
            }
            .putBoolean(KEY_COMMUNITY_VISIBLE, updated.communityContentVisible)
            .putStringSet(KEY_BLOCKED_USERS, updated.blockedUsers.map(::encodeBlockedUser).toSet())
            .apply()
    }

    private fun readSafety(): CommunitySafetyState {
        val ageStatus = preferences.getString(KEY_AGE_GATE_STATUS, null)
            ?.let { stored -> AgeGateStatus.entries.firstOrNull { it.name == stored } }
            ?: AgeGateStatus.NotCompleted
        val acceptedVersion = preferences.getString(KEY_TERMS_VERSION, null)
        val visibilityAllowed = ageStatus == AgeGateStatus.Adult && acceptedVersion == CURRENT_COMMUNITY_TERMS_VERSION
        return CommunitySafetyState(
            ageGateStatus = ageStatus,
            acceptedTermsVersion = acceptedVersion,
            communityContentVisible = preferences.getBoolean(KEY_COMMUNITY_VISIBLE, false) && visibilityAllowed,
            blockedUsers = preferences.getStringSet(KEY_BLOCKED_USERS, emptySet()).orEmpty()
                .mapNotNull(::decodeBlockedUser)
                .distinctBy { it.stationId to it.normalizedIdentity }
                .sortedWith(compareBy({ it.stationId.value }, { it.displayName.lowercase() })),
        )
    }

    private fun encodeBlockedUser(user: BlockedCommunityUser): String =
        "${encodePreference(user.stationId.value)}|${encodePreference(user.displayName)}"

    private fun decodeBlockedUser(value: String): BlockedCommunityUser? {
        val (station, displayName) = value.split('|', limit = 2).takeIf { it.size == 2 } ?: return null
        return runCatching {
            BlockedCommunityUser(
                decodePreference(station).toSupportedStationIdOrNull() ?: return null,
                decodePreference(displayName),
            )
        }.getOrNull()?.takeIf { it.stationId.value.isNotBlank() && it.displayName.isNotBlank() }
    }

    private fun encodePreference(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun decodePreference(value: String) = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val PREFERENCES_NAME = "community_safety"
        const val KEY_AGE_GATE_STATUS = "age_gate_status"
        const val KEY_TERMS_VERSION = "terms_version"
        const val KEY_COMMUNITY_VISIBLE = "community_content_visible"
        const val KEY_BLOCKED_USERS = "blocked_users"
        const val MAX_REPORTED_USER_CHARACTERS = 80
    }
}

private fun AbuseReportTarget.bounded() = copy(
    reportedUser = reportedUser.trim().take(80),
    displayedTimestamp = displayedTimestamp?.trim()?.take(40)?.takeIf(String::isNotEmpty),
    contentSnapshot = contentSnapshot?.trim()?.replace(Regex("\\s+"), " ")?.take(500)
        ?.takeIf(String::isNotEmpty),
)
