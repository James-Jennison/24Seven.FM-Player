package com.codeframe78.twentyfourseven.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.codeframe78.twentyfourseven.player.domain.PlaybackController
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Media3PlaybackController(context: Context) : PlaybackController {
    private val appContext = context.applicationContext
    private val stateFlow = MutableStateFlow(PlaybackState())
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, RadioPlaybackService::class.java)),
    ).buildAsync()

    private var controller: MediaController? = null
    private var selectedStation: Station? = null
    private var playRequested = false

    override val state: StateFlow<PlaybackState> = stateFlow.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = updateState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()

        override fun onPlayerError(error: PlaybackException) {
            stateFlow.value = stateFlow.value.copy(
                status = PlaybackStatus.Error,
                errorMessage = error.message,
            )
        }
    }

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { connected ->
                        controller = connected
                        connected.addListener(listener)
                        selectedStation?.let(::setStationMediaItems)
                        updateState()
                    }
                    .onFailure { error ->
                        stateFlow.value = stateFlow.value.copy(
                            status = PlaybackStatus.Error,
                            errorMessage = error.message ?: "Unable to connect to playback service",
                        )
                    }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    override fun selectStation(station: Station) {
        if (selectedStation?.id == station.id) return
        selectedStation = station
        stateFlow.value = PlaybackState(stationId = station.id)
        if (controller != null) setStationMediaItems(station)
    }

    override fun play() {
        playRequested = true
        val connected = controller
        if (connected == null) {
            stateFlow.value = stateFlow.value.copy(status = PlaybackStatus.Connecting)
            return
        }
        if (connected.mediaItemCount == 0) selectedStation?.let(::setStationMediaItems)
        connected.prepare()
        connected.play()
        updateState()
    }

    override fun pause() {
        playRequested = false
        controller?.pause()
        updateState()
    }

    override fun stop() {
        playRequested = false
        controller?.stop()
        updateState()
    }

    private fun setStationMediaItems(station: Station) {
        val connected = controller ?: return
        val resumePlayback = playRequested || connected.playWhenReady
        val mediaItems = station.streams
            .sortedBy { it.priority }
            .map { stream ->
                MediaItem.Builder()
                    .setMediaId("${station.id.value}:${stream.priority}")
                    .setUri(stream.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(station.name)
                            .setArtist("24seven.FM")
                            .setSubtitle(stream.label)
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .build(),
                    )
                    .build()
            }

        connected.stop()
        connected.setMediaItems(mediaItems, 0, 0L)
        if (resumePlayback) {
            connected.prepare()
            connected.play()
        }
        updateState()
    }

    private fun updateState() {
        val connected = controller
        val stationId = selectedStation?.id ?: stateFlow.value.stationId
        if (connected == null) {
            stateFlow.value = PlaybackState(
                stationId = stationId,
                status = if (playRequested) PlaybackStatus.Connecting else PlaybackStatus.Idle,
            )
            return
        }

        val status = when {
            connected.playerError != null -> PlaybackStatus.Error
            connected.playbackState == Player.STATE_BUFFERING && connected.currentMediaItemIndex > 0 -> PlaybackStatus.Retrying
            connected.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
            connected.isPlaying -> PlaybackStatus.Playing
            connected.playbackState == Player.STATE_READY -> PlaybackStatus.Paused
            playRequested -> PlaybackStatus.Connecting
            else -> PlaybackStatus.Idle
        }
        stateFlow.value = PlaybackState(
            stationId = stationId,
            status = status,
            errorMessage = connected.playerError?.message,
        )
    }
}
