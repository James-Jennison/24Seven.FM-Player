package com.codeframe78.twentyfourseven.player.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.codeframe78.twentyfourseven.player.MainActivity
import com.codeframe78.twentyfourseven.player.RadioApplication
import com.codeframe78.twentyfourseven.player.domain.NowPlayingPublisher
import com.codeframe78.twentyfourseven.player.domain.NowPlayingArtworkRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class RadioPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val nowPlayingPublisher: NowPlayingPublisher by lazy {
        (application as RadioApplication).appContainer.nowPlayingPublisher
    }
    private val artworkRepository: NowPlayingArtworkRepository by lazy {
        (application as RadioApplication).appContainer.nowPlayingArtworkRepository
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var artworkJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var sleepTimerButtonVisible = false
    private var activeNowPlaying: NowPlayingState? = null
    private var activeMediaId: String? = null
    private val sleepTimerPreferences by lazy {
        getSharedPreferences(SLEEP_TIMER_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    private val cancelSleepTimerButton by lazy {
        CommandButton.Builder(CommandButton.ICON_MINUS_CIRCLE_FILLED)
            .setSessionCommand(SleepTimerSessionContract.cancelCommand)
            .setDisplayName("Cancel sleep timer")
            .setSlots(CommandButton.SLOT_OVERFLOW)
            .build()
    }
    private val sessionPlayer by lazy {
        object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands()
                .buildUpon()
                .remove(Player.COMMAND_SEEK_TO_NEXT)
                .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
        }
    }
    private val fallbackListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
                player.play()
            }
        }
    }
    private val metadataListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val incomingMediaId = mediaItem?.mediaId
            if (!shouldClearNowPlaying(activeMediaId, incomingMediaId)) return
            artworkJob?.cancel()
            activeNowPlaying = null
            activeMediaId = incomingMediaId
            nowPlayingPublisher.clear(mediaItem?.stationId())
        }

        override fun onMetadata(metadata: Metadata) {
            val stationId = player.currentMediaItem?.stationId() ?: return
            val nowPlaying = metadata.toNowPlayingState(stationId) ?: return
            if (activeNowPlaying?.stationId == stationId && activeNowPlaying?.displayTitle == nowPlaying.displayTitle) return
            artworkJob?.cancel()
            activeMediaId = player.currentMediaItem?.mediaId
            activeNowPlaying = nowPlaying
            updateSessionMetadata(nowPlaying)
            nowPlayingPublisher.publish(nowPlaying)
            artworkJob = serviceScope.launch {
                val artworkUrl = runCatching { artworkRepository.fetchArtwork(stationId) }.getOrNull() ?: return@launch
                if (activeNowPlaying != nowPlaying || player.currentMediaItem?.stationId() != stationId) return@launch
                val enriched = nowPlaying.copy(artworkUrl = artworkUrl)
                activeNowPlaying = enriched
                updateSessionMetadata(enriched)
                nowPlayingPublisher.publish(enriched)
            }
        }
    }
    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val access = MediaSessionControllerPolicy.access(controller, packageName)
            if (access == ControllerAccess.Foreign) {
                return MediaSession.ConnectionResult.accept(
                    MediaSessionControllerPolicy.sessionCommands(
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                        access,
                    ),
                    MediaSessionControllerPolicy.playerCommands(session.player.availableCommands, access),
                )
            }
            val base = super.onConnect(session, controller)
            if (!base.isAccepted) return base
            return MediaSession.ConnectionResult.accept(
                MediaSessionControllerPolicy.sessionCommands(base.availableSessionCommands, access),
                MediaSessionControllerPolicy.playerCommands(base.availablePlayerCommands, access),
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ) = if (
            MediaSessionControllerPolicy.mayChangeMedia(
                MediaSessionControllerPolicy.access(controller, packageName),
            )
        ) {
            super.onAddMediaItems(mediaSession, controller, mediaItems)
        } else {
            Futures.immediateFailedFuture(SecurityException("Controller cannot change station media"))
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ) = when (customCommand) {
            SleepTimerSessionContract.setCommand -> {
                val access = MediaSessionControllerPolicy.access(controller, packageName)
                if (!MediaSessionControllerPolicy.maySetSleepTimer(access)) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                } else {
                    val durationMillis = SleepTimerSessionContract.durationMillis(args)
                    if (durationMillis == null) {
                        Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                    } else {
                        startSleepTimer(durationMillis)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }
            }

            SleepTimerSessionContract.cancelCommand -> {
                val access = MediaSessionControllerPolicy.access(controller, packageName)
                if (!MediaSessionControllerPolicy.mayCancelSleepTimer(access)) {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                } else {
                    cancelSleepTimer()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }

            else -> super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onPlayerInteractionFinished(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommands: Player.Commands,
        ) {
            if (playerCommands.contains(Player.COMMAND_STOP)) cancelSleepTimer()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(attributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(fallbackListener)
        player.addListener(metadataListener)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, sessionPlayer)
            .setSessionActivity(sessionActivity)
            .setCallback(sessionCallback)
            .build()
        restoreSleepTimer()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    private fun updateSessionMetadata(nowPlaying: NowPlayingState) {
        val index = player.currentMediaItemIndex
        val currentItem = player.currentMediaItem ?: return
        val displayTitle = nowPlaying.displayTitle ?: return
        val artworkUri = nowPlaying.artworkUrl?.let(Uri::parse)
        if (currentItem.mediaMetadata.title == displayTitle && currentItem.mediaMetadata.artworkUri == artworkUri) return
        player.replaceMediaItem(
            index,
            currentItem.buildUpon()
                .setMediaMetadata(
                    currentItem.mediaMetadata.withNowPlayingTitle(displayTitle)
                        .buildUpon()
                        .setArtworkUri(artworkUri)
                        .build(),
                )
                .build(),
        )
    }

    private fun startSleepTimer(durationMillis: Long) {
        val elapsedRealtimeDeadline = SystemClock.elapsedRealtime() + durationMillis
        val epochDeadline = System.currentTimeMillis() + durationMillis
        sleepTimerPreferences.edit()
            .putLong(KEY_ELAPSED_REALTIME_DEADLINE, elapsedRealtimeDeadline)
            .putLong(KEY_EPOCH_DEADLINE, epochDeadline)
            .apply()
        runSleepTimer(elapsedRealtimeDeadline, durationMillis)
    }

    private fun restoreSleepTimer() {
        val elapsedDeadline = sleepTimerPreferences.getLong(KEY_ELAPSED_REALTIME_DEADLINE, 0L)
        val epochDeadline = sleepTimerPreferences.getLong(KEY_EPOCH_DEADLINE, 0L)
        if (elapsedDeadline <= 0L || epochDeadline <= 0L) {
            clearPersistedSleepTimer()
            publishSleepTimer(null, 0L)
            return
        }
        val remaining = PersistedSleepTimer(elapsedDeadline, epochDeadline).restoredRemainingMillis(
            elapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            epochMillis = System.currentTimeMillis(),
        )
        when {
            remaining == null -> cancelSleepTimer()
            remaining <= 0L -> expireSleepTimer()
            else -> runSleepTimer(elapsedDeadline, remaining)
        }
    }

    private fun runSleepTimer(elapsedRealtimeDeadline: Long, initialRemainingMillis: Long) {
        sleepTimerJob?.cancel()
        publishSleepTimer(elapsedRealtimeDeadline, initialRemainingMillis)
        sleepTimerJob = serviceScope.launch {
            while (isActive) {
                val remaining = elapsedRealtimeDeadline - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    expireSleepTimer()
                    break
                }
                publishSleepTimer(elapsedRealtimeDeadline, remaining)
                delay(minOf(SLEEP_TIMER_UPDATE_INTERVAL_MILLIS, remaining))
            }
        }
    }

    private fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        clearPersistedSleepTimer()
        publishSleepTimer(null, 0L)
    }

    private fun expireSleepTimer() {
        sleepTimerJob = null
        clearPersistedSleepTimer()
        publishSleepTimer(null, 0L)
        player.stop()
        mediaSession.broadcastCustomCommand(SleepTimerSessionContract.expiredCommand, Bundle.EMPTY)
    }

    private fun clearPersistedSleepTimer() {
        sleepTimerPreferences.edit()
            .remove(KEY_ELAPSED_REALTIME_DEADLINE)
            .remove(KEY_EPOCH_DEADLINE)
            .apply()
    }

    private fun publishSleepTimer(deadlineElapsedRealtimeMillis: Long?, remainingMillis: Long) {
        val isActive = deadlineElapsedRealtimeMillis != null && remainingMillis > 0L
        mediaSession.setSessionExtras(
            SleepTimerSessionContract.extras(deadlineElapsedRealtimeMillis, remainingMillis),
        )
        if (sleepTimerButtonVisible != isActive) {
            sleepTimerButtonVisible = isActive
            val buttons = if (isActive) listOf(cancelSleepTimerButton) else emptyList()
            mediaSession.setMediaButtonPreferences(buttons)
            mediaSession.setCustomLayout(buttons)
        }
    }

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        player.removeListener(fallbackListener)
        player.removeListener(metadataListener)
        serviceScope.cancel()
        nowPlayingPublisher.clear()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}

internal const val SLEEP_TIMER_PREFERENCES_NAME = "playback_sleep_timer"
private const val KEY_ELAPSED_REALTIME_DEADLINE = "elapsed_realtime_deadline"
private const val KEY_EPOCH_DEADLINE = "epoch_deadline"
private const val SLEEP_TIMER_UPDATE_INTERVAL_MILLIS = 1_000L

private fun MediaItem.stationId(): StationId? = mediaId
    .substringBefore(':')
    .takeIf(String::isNotBlank)
    ?.let(::StationId)

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal fun Metadata.toNowPlayingState(stationId: StationId): NowPlayingState? {
    val title = getFirstEntryOfType(IcyInfo::class.java)
        ?.title
        ?.trim()
        ?.normalizeLegacyIcyPunctuation()
        ?.takeIf(String::isNotEmpty)
        ?: return null
    return NowPlayingState(stationId = stationId, displayTitle = title)
}

/**
 * Media3 exposes legacy ICY title bytes as ISO-8859-1 text. Some station catalog entries were authored with
 * Windows-1252 punctuation, whose bytes otherwise become C1 control characters such as U+0092. Map only those
 * defined code points so unrelated Unicode metadata remains untouched.
 */
internal fun String.normalizeLegacyIcyPunctuation(): String = map { character ->
    when (character) {
        '\u0080' -> '€'
        '\u0082' -> '‚'
        '\u0083' -> 'ƒ'
        '\u0084' -> '„'
        '\u0085' -> '…'
        '\u0086' -> '†'
        '\u0087' -> '‡'
        '\u0088' -> 'ˆ'
        '\u0089' -> '‰'
        '\u008A' -> 'Š'
        '\u008B' -> '‹'
        '\u008C' -> 'Œ'
        '\u008E' -> 'Ž'
        '\u0091' -> '‘'
        '\u0092' -> '’'
        '\u0093' -> '“'
        '\u0094' -> '”'
        '\u0095' -> '•'
        '\u0096' -> '–'
        '\u0097' -> '—'
        '\u0098' -> '˜'
        '\u0099' -> '™'
        '\u009A' -> 'š'
        '\u009B' -> '›'
        '\u009C' -> 'œ'
        '\u009E' -> 'ž'
        '\u009F' -> 'Ÿ'
        else -> character
    }
}.joinToString("")

internal fun MediaMetadata.withNowPlayingTitle(displayTitle: String): MediaMetadata {
    val stationName = albumTitle ?: title
    return buildUpon()
        .setTitle(displayTitle)
        .setArtist(stationName)
        .setAlbumTitle(stationName)
        .setSubtitle("24seven.FM")
        .build()
}

internal fun shouldClearNowPlaying(activeMediaId: String?, incomingMediaId: String?): Boolean =
    activeMediaId == null || incomingMediaId == null || activeMediaId != incomingMediaId
