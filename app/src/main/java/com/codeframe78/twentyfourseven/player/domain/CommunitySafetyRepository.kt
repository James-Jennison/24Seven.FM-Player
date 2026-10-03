package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow
import java.text.Normalizer
import java.time.DateTimeException
import java.time.LocalDate
import java.util.Locale

const val CURRENT_COMMUNITY_TERMS_VERSION = "2026-07-18"

enum class AgeGateStatus { NotCompleted, Adult, Underage }

data class BlockedCommunityUser(
    val stationId: StationId,
    val displayName: String,
    val normalizedIdentity: String = displayName.normalizedCommunityIdentity(),
)

data class CommunitySafetyState(
    val ageGateStatus: AgeGateStatus = AgeGateStatus.NotCompleted,
    val acceptedTermsVersion: String? = null,
    val communityContentVisible: Boolean = false,
    val blockedUsers: List<BlockedCommunityUser> = emptyList(),
    val ageGateErrorMessage: String? = null,
    /** True once the listener has been told, or found out, that tapping a chat line opens report and block. */
    val messageActionsHintSeen: Boolean = false,
) {
    val hasAcceptedCurrentTerms: Boolean
        get() = acceptedTermsVersion == CURRENT_COMMUNITY_TERMS_VERSION

    val canViewCommunityContent: Boolean
        get() = ageGateStatus == AgeGateStatus.Adult && hasAcceptedCurrentTerms && communityContentVisible

    val canContributeCommunityContent: Boolean
        get() = canViewCommunityContent

    fun isBlocked(stationId: StationId, displayName: String?): Boolean {
        val identity = displayName?.normalizedCommunityIdentity()?.takeIf(String::isNotEmpty) ?: return false
        return blockedUsers.any { it.stationId == stationId && it.normalizedIdentity == identity }
    }
}

enum class AbuseReportKind(val label: String) {
    Content("Report content"),
    User("Report user"),
}

enum class AbuseReportSource(val label: String) {
    Chat("Chat"),
    Request("request attribution"),
    PrivateMessage("private message"),
}

enum class AbuseReportCategory(val label: String) {
    Harassment("Harassment, bullying, or threats"),
    Hate("Hate speech or discrimination"),
    Sexual("Sexual content or unwanted advances"),
    ChildSafety("Child safety or exploitation"),
    PersonalInformation("Private information or impersonation"),
    IllegalActivity("Illegal or dangerous activity"),
    Spam("Spam or commercial solicitation"),
    Other("Other violation"),
}

data class AbuseReportTarget(
    val kind: AbuseReportKind,
    val source: AbuseReportSource,
    val reportedUser: String,
    val displayedTimestamp: String? = null,
    val contentSnapshot: String? = null,
)

enum class AbuseReportStatus { Idle, Ready, PreparingEmail, EmailReady, EmailHandoffStarted, Error }

data class AbuseReportState(
    val stationId: StationId? = null,
    val target: AbuseReportTarget? = null,
    val status: AbuseReportStatus = AbuseReportStatus.Idle,
    val emailDraft: PlayerEmailDraft? = null,
    val errorMessage: String? = null,
    val retryAllowed: Boolean = false,
)

data class AbuseReportSubmission(
    val reporterName: String,
    val category: AbuseReportCategory,
    val optionalDetails: String,
)

interface CommunitySafetyRepository {
    fun observeSafety(): Flow<CommunitySafetyState>

    fun observeReport(): Flow<AbuseReportState>

    suspend fun submitAgeScreen(year: Int, month: Int, day: Int)

    suspend fun acceptTerms()

    suspend fun setCommunityContentVisible(visible: Boolean)

    suspend fun acknowledgeMessageActionsHint()

    suspend fun blockUser(stationId: StationId, displayName: String)

    suspend fun unblockUser(stationId: StationId, displayName: String)

    suspend fun beginReport(stationId: StationId, target: AbuseReportTarget)

    suspend fun submitReport(submission: AbuseReportSubmission)

    fun reportEmailComposerResult(opened: Boolean)

    fun dismissReport()
}

fun isAdultOnDate(birthDate: LocalDate, today: LocalDate): Boolean =
    !birthDate.isAfter(today.minusYears(18))

fun validatedBirthDate(year: Int, month: Int, day: Int, today: LocalDate): LocalDate? = try {
    LocalDate.of(year, month, day).takeIf { !it.isAfter(today) }
} catch (_: DateTimeException) {
    null
}

fun String.normalizedCommunityIdentity(): String = Normalizer.normalize(trim(), Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT)
    .replace(Regex("\\s+"), " ")
