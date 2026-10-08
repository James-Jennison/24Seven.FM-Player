package com.codeframe78.twentyfourseven.player.playback

import com.codeframe78.twentyfourseven.player.domain.PlaybackController
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * What the home-screen widget, the Quick Settings tile and the app shortcuts can do without any screen open: play
 * the selected station, play a named station, or stop. They go through the same station repository and playback
 * controller as the Player, so the app shows the same state when it is opened next.
 */
class ListenerControls(
    private val stations: StationRepository,
    private val playback: () -> PlaybackController,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    /** Stops live radio when it is on, otherwise starts the selected station. */
    fun togglePlayback(): Job = scope.launch {
        val controller = playback()
        if (controller.state.value.status.keepsListening) controller.stop() else start(stations.observeSelectedStation().first())
    }

    fun playSelected(): Job = scope.launch { start(stations.observeSelectedStation().first()) }

    fun playStation(id: StationId): Job = scope.launch {
        val station = stations.availableStations().firstOrNull { it.id == id } ?: return@launch
        stations.selectStation(id)
        start(station)
    }

    /** Switches station without starting it; a station that is already playing changes over as it does in the Player. */
    fun selectStation(id: StationId): Job = scope.launch {
        val station = stations.availableStations().firstOrNull { it.id == id } ?: return@launch
        stations.selectStation(id)
        playback().selectStation(station)
    }

    private fun start(station: Station) {
        val controller = playback()
        controller.selectStation(station)
        controller.play()
    }
}

/** True while the listener has asked for audio and the player is getting it or has it. */
val PlaybackStatus.keepsListening: Boolean
    get() = this == PlaybackStatus.Connecting ||
        this == PlaybackStatus.Buffering ||
        this == PlaybackStatus.Playing ||
        this == PlaybackStatus.Retrying ||
        this == PlaybackStatus.WaitingForNetwork
