package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

enum class ListenerActivityLoadStatus { Idle, Loading, Ready, Error }

enum class MembershipTier { Unknown, Standard, Vip, Rip }

enum class RequestReadiness { Unknown, Ready, Waiting }

data class RequestHistoryEntry(
    val position: Int,
    val trackSummary: String,
    val requestedAtLabel: String,
    val albumTitle: String? = null,
    val albumId: String? = null,
    val artworkUrl: String? = null,
)

data class ListenerActivityState(
    val stationId: StationId,
    val status: ListenerActivityLoadStatus = ListenerActivityLoadStatus.Idle,
    val membershipTier: MembershipTier = MembershipTier.Unknown,
    val requestReadiness: RequestReadiness = RequestReadiness.Unknown,
    val waitMinutes: Int? = null,
    val recentRequests: List<RequestHistoryEntry> = emptyList(),
    val errorMessage: String? = null,
    /** The member's station rank as the station words it, such as an administrator's title. */
    val rankTitle: String? = null,
    /** Seconds until the listener's earliest queued request should start, when one is queued. */
    val queuedRequestWaitSeconds: Int? = null,
)

interface ListenerActivityRepository {
    fun observeActivity(stationId: StationId): Flow<ListenerActivityState>
    suspend fun refresh(stationId: StationId)
    suspend fun clear(stationId: StationId)
}
