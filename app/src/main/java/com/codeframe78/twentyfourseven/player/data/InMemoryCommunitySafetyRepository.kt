package com.codeframe78.twentyfourseven.player.data

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

internal class InMemoryCommunitySafetyRepository(
    initialSafety: CommunitySafetyState = CommunitySafetyState(),
    private val today: () -> LocalDate = LocalDate::now,
) : CommunitySafetyRepository {
    private val safety = MutableStateFlow(initialSafety)
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
        val status = if (isAdultOnDate(birthDate, currentDate)) AgeGateStatus.Adult else AgeGateStatus.Underage
        safety.value = safety.value.copy(
            ageGateStatus = status,
            acceptedTermsVersion = safety.value.acceptedTermsVersion.takeIf { status == AgeGateStatus.Adult },
            communityContentVisible = safety.value.communityContentVisible && status == AgeGateStatus.Adult,
            ageGateErrorMessage = null,
        )
    }

    override suspend fun acceptTerms() {
        if (safety.value.ageGateStatus == AgeGateStatus.Adult) {
            safety.value = safety.value.copy(acceptedTermsVersion = CURRENT_COMMUNITY_TERMS_VERSION)
        }
    }

    override suspend fun acknowledgeMessageActionsHint() {
        safety.value = safety.value.copy(messageActionsHintSeen = true)
    }

    override suspend fun setCommunityContentVisible(visible: Boolean) {
        safety.value = safety.value.copy(
            communityContentVisible = visible && safety.value.ageGateStatus == AgeGateStatus.Adult &&
                safety.value.hasAcceptedCurrentTerms,
        )
    }

    override suspend fun blockUser(stationId: StationId, displayName: String) {
        val name = displayName.trim().take(80)
        if (name.isEmpty()) return
        val identity = name.normalizedCommunityIdentity()
        safety.value = safety.value.copy(
            blockedUsers = safety.value.blockedUsers.filterNot {
                it.stationId == stationId && it.normalizedIdentity == identity
            } + BlockedCommunityUser(stationId, name, identity),
        )
    }

    override suspend fun unblockUser(stationId: StationId, displayName: String) {
        val identity = displayName.normalizedCommunityIdentity()
        safety.value = safety.value.copy(
            blockedUsers = safety.value.blockedUsers.filterNot {
                it.stationId == stationId && it.normalizedIdentity == identity
            },
        )
    }

    override suspend fun beginReport(stationId: StationId, target: AbuseReportTarget) {
        report.value = AbuseReportState(
            stationId = stationId,
            target = target,
            status = AbuseReportStatus.Ready,
        )
    }

    override suspend fun submitReport(submission: AbuseReportSubmission) {
        val current = report.value
        val stationId = current.stationId ?: return
        val target = current.target ?: return
        if (current.status != AbuseReportStatus.Ready) return
        report.value = runCatching { abuseReportEmailDraft(stationId, target, submission) }
            .fold(
                onSuccess = { draft ->
                    current.copy(status = AbuseReportStatus.EmailReady, emailDraft = draft)
                },
                onFailure = { error ->
                    current.copy(
                        status = AbuseReportStatus.Error,
                        errorMessage = error.message ?: "Check the report fields and try again.",
                        retryAllowed = true,
                    )
                },
            )
    }

    override fun reportEmailComposerResult(opened: Boolean) {
        val current = report.value
        if (current.status != AbuseReportStatus.EmailReady) return
        report.value = if (opened) {
            current.copy(status = AbuseReportStatus.EmailHandoffStarted, emailDraft = null)
        } else {
            current.copy(
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
}
