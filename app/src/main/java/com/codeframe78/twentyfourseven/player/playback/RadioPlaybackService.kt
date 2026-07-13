package com.codeframe78.twentyfourseven.player.playback

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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.codeframe78.twentyfourseven.player.RadioApplication
import com.codeframe78.twentyfourseven.player.domain.NowPlayingPublisher
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.StationId

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class RadioPlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val nowPlayingPublisher: NowPlayingPublisher by lazy {
        (application as RadioApplication).appContainer.nowPlayingPublisher
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
            nowPlayingPublisher.clear(mediaItem?.stationId())
        }

        override fun onMetadata(metadata: Metadata) {
            val stationId = player.currentMediaItem?.stationId() ?: return
            val nowPlaying = metadata.toNowPlayingState(stationId) ?: return
            updateSessionMetadata(nowPlaying)
            nowPlayingPublisher.publish(nowPlaying)
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
        mediaSession = MediaSession.Builder(this, sessionPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    private fun updateSessionMetadata(nowPlaying: NowPlayingState) {
        val index = player.currentMediaItemIndex
        val currentItem = player.currentMediaItem ?: return
        val displayTitle = nowPlaying.displayTitle ?: return
        if (currentItem.mediaMetadata.title == displayTitle) return
        player.replaceMediaItem(
            index,
            currentItem.buildUpon()
                .setMediaMetadata(currentItem.mediaMetadata.withNowPlayingTitle(displayTitle))
                .build(),
        )
    }

    override fun onDestroy() {
        player.removeListener(fallbackListener)
        player.removeListener(metadataListener)
        nowPlayingPublisher.clear()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}

private fun MediaItem.stationId(): StationId? = mediaId
    .substringBefore(':')
    .takeIf(String::isNotBlank)
    ?.let(::StationId)

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal fun Metadata.toNowPlayingState(stationId: StationId): NowPlayingState? {
    val title = getFirstEntryOfType(IcyInfo::class.java)?.title?.trim()?.takeIf(String::isNotEmpty)
        ?: return null
    return NowPlayingState(stationId = stationId, displayTitle = title)
}

internal fun MediaMetadata.withNowPlayingTitle(displayTitle: String): MediaMetadata {
    val stationName = albumTitle ?: title
    return buildUpon()
        .setTitle(displayTitle)
        .setArtist(stationName)
        .setAlbumTitle(stationName)
        .setSubtitle("24seven.FM")
        .build()
}

