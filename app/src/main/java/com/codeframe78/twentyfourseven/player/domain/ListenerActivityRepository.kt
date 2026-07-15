package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

enum class ListenerActivityLoadStatus { Idle, Loading, Ready, Error }

enum class MembershipTier { Unknown, Standard, Vip, Rip }

enum class RequestReadiness { Unknown, Ready, Waiting }

data class RequestHistoryEntry(
    val position: Int,
    val trackSummary: String,
    val requestedAtLabel: String,
)

data class ListenerActivityState(
    val stationId: StationId,
    val status: ListenerActivityLoadStatus = ListenerActivityLoadStatus.Idle,
    val membershipTier: MembershipTier = MembershipTier.Unknown,
    val requestReadiness: RequestReadiness = RequestReadiness.Unknown,
    val waitMinutes: Int? = null,
    val recentRequests: List<RequestHistoryEntry> = emptyList(),
    val errorMessage: String? = null,
)

interface ListenerActivityRepository {
    fun observeActivity(stationId: StationId): Flow<ListenerActivityState>
    suspend fun refresh(stationId: StationId)
    suspend fun clear(stationId: StationId)
}
