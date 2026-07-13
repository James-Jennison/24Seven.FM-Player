package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.StateFlow

enum class PlaybackStatus {
    Idle,
    Connecting,
    Buffering,
    Playing,
    Paused,
    Retrying,
    Error,
}

data class PlaybackState(
    val stationId: StationId? = null,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val errorMessage: String? = null,
)

interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun selectStation(station: Station)
    fun play()
    fun pause()
    fun stop()
}
