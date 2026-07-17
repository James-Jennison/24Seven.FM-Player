package com.codeframe78.twentyfourseven.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionError
import androidx.media3.session.SessionToken
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.codeframe78.twentyfourseven.player.domain.PlaybackController
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.SleepTimerState
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Media3PlaybackController(context: Context) : PlaybackController {
    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val networkRecovery = NetworkPlaybackRecovery(connectivityManager.hasValidatedDefaultNetwork())
    private val stateFlow = MutableStateFlow(
        PlaybackState(networkAvailable = networkRecovery.isNetworkUsable),
    )
    private val audioOutputMonitor = AndroidAudioOutputMonitor(appContext) { output ->
        stateFlow.value = stateFlow.value.copy(audioOutput = output)
    }

    private var controller: MediaController? = null
    private var selectedStation: Station? = null
    private var playRequested = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            dispatchNetworkState(networkCapabilities.hasValidatedInternet())
        }

        override fun onLost(network: Network) {
            dispatchNetworkState(false)
        }
    }

    override val state: StateFlow<PlaybackState> = stateFlow.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            maybeRetryAfterNetworkRestored()
            updateState()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()

        override fun onPlayerError(error: PlaybackException) {
            val waitingForNetwork = networkRecovery.onPlaybackError(
                shouldResume = playRequested && (controller?.playWhenReady != false),
            )
            stateFlow.value = stateFlow.value.copy(
                status = if (waitingForNetwork) PlaybackStatus.WaitingForNetwork else PlaybackStatus.Error,
                errorMessage = error.message,
                networkAvailable = networkRecovery.isNetworkUsable,
            )
        }
    }
    private val sessionListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            updateSleepTimerState(extras)
        }

        @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle,
        ) = if (command == SleepTimerSessionContract.expiredCommand) {
            playRequested = false
            updateSleepTimerState(controller.sessionExtras)
            updateState()
            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        } else {
            Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }
    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, RadioPlaybackService::class.java)),
    )
        .setListener(sessionListener)
        .buildAsync()

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { connected ->
                        controller = connected
                        connected.addListener(listener)
                        selectedStation?.let(::setStationMediaItems)
                        updateSleepTimerState(connected.sessionExtras)
                        updateState()
                    }
                    .onFailure { error ->
                        stateFlow.value = stateFlow.value.copy(
                            status = PlaybackStatus.Error,
                            errorMessage = error.message ?: "Unable to connect to playback service",
                        )
                    }
            },
            mainExecutor,
        )
    }

    override fun selectStation(station: Station) {
        if (selectedStation?.id == station.id) return
        networkRecovery.cancel()
        selectedStation = station
        stateFlow.value = stateFlow.value.copy(
            stationId = station.id,
            status = PlaybackStatus.Idle,
            errorMessage = null,
        )
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
        networkRecovery.cancel()
        controller?.pause()
        updateState()
    }

    override fun stop() {
        playRequested = false
        networkRecovery.cancel()
        cancelSleepTimer()
        controller?.stop()
        updateState()
    }

    override fun refreshAudioOutput() {
        audioOutputMonitor.refresh()
    }

    override fun setSleepTimer(durationMillis: Long) {
        val connected = controller ?: return
        connected.sendCustomCommand(
            SleepTimerSessionContract.setCommand,
            SleepTimerSessionContract.setArguments(durationMillis),
        )
    }

    override fun cancelSleepTimer() {
        stateFlow.value = stateFlow.value.copy(sleepTimer = SleepTimerState())
        controller?.sendCustomCommand(SleepTimerSessionContract.cancelCommand, Bundle.EMPTY)
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
                            .setAlbumTitle(station.name)
                            .setSubtitle(stream.label)
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .build(),
                    )
                    .build()
            }

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
            stateFlow.value = stateFlow.value.copy(
                stationId = stationId,
                status = if (playRequested) PlaybackStatus.Connecting else PlaybackStatus.Idle,
                errorMessage = null,
            )
            return
        }

        val status = when {
            connected.playerError != null && networkRecovery.isWaitingForNetwork -> PlaybackStatus.WaitingForNetwork
            connected.playerError != null -> PlaybackStatus.Error
            connected.playbackState == Player.STATE_BUFFERING && connected.currentMediaItemIndex > 0 -> PlaybackStatus.Retrying
            connected.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
            connected.isPlaying -> PlaybackStatus.Playing
            connected.playbackState == Player.STATE_READY -> PlaybackStatus.Paused
            playRequested -> PlaybackStatus.Connecting
            else -> PlaybackStatus.Idle
        }
        stateFlow.value = stateFlow.value.copy(
            stationId = stationId,
            status = status,
            errorMessage = connected.playerError?.message,
            networkAvailable = networkRecovery.isNetworkUsable,
        )
    }

    private fun updateSleepTimerState(extras: Bundle) {
        stateFlow.value = stateFlow.value.copy(sleepTimer = SleepTimerSessionContract.state(extras))
    }

    private fun dispatchNetworkState(isUsable: Boolean) {
        mainExecutor.execute {
            val shouldRetry = networkRecovery.onNetworkStateChanged(isUsable)
            val connected = controller
            if (!isUsable && connected?.playerError != null) {
                networkRecovery.onPlaybackError(
                    shouldResume = playRequested && connected.playWhenReady,
                )
            }
            if (shouldRetry) maybeRetryAfterNetworkRestored()
            updateState()
        }
    }

    private fun maybeRetryAfterNetworkRestored() {
        if (!networkRecovery.isNetworkUsable || !networkRecovery.isWaitingForNetwork || !playRequested) return
        val connected = controller ?: return
        if (!connected.playWhenReady) return
        networkRecovery.markRetryStarted()
        stateFlow.value = stateFlow.value.copy(
            status = PlaybackStatus.Connecting,
            errorMessage = null,
        )
        connected.prepare()
        connected.play()
    }
}

private fun ConnectivityManager.hasValidatedDefaultNetwork(): Boolean = activeNetwork
    ?.let(::getNetworkCapabilities)
    ?.hasValidatedInternet()
    ?: false

private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
