package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

enum class QueueLoadStatus { Unavailable, Loading, Ready, Error }

data class QueueTrack(
    val position: Int,
    val displayTitle: String,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val durationLabel: String? = null,
    val artworkUrl: String? = null,
    val requesterName: String? = null,
    val requestMessage: String? = null,
)

data class HistoryTrack(
    val displayTitle: String,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val durationLabel: String? = null,
    val artworkUrl: String? = null,
    val requesterName: String? = null,
    val requestMessage: String? = null,
)

data class QueueState(
    val stationId: StationId,
    val status: QueueLoadStatus = QueueLoadStatus.Unavailable,
    val upcoming: List<QueueTrack> = emptyList(),
    val recentlyPlayed: List<HistoryTrack> = emptyList(),
    val errorMessage: String? = null,
)

interface QueueRepository {
    fun observeQueue(stationId: StationId): Flow<QueueState>
    suspend fun refresh(stationId: StationId)
}
