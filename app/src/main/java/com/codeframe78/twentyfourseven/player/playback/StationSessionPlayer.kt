package com.codeframe78.twentyfourseven.player.playback

import android.net.Uri
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * The player the media session shows to the notification, a car display, and the Player's own controller.
 *
 * The real playlist is one station's stream and its fallbacks, which is no use as a queue. Controllers instead see
 * the stream that is on air followed by the tracks the station has queued, so Android Auto's Queue lists what is
 * coming up. Those tracks cannot be played or skipped to: a live station decides what plays next.
 */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal class StationSessionPlayer(player: Player) : ForwardingSimpleBasePlayer(player) {
    private var upNext: List<MediaItem> = emptyList()

    /** Replaces the tracks listed after the stream on air. Call on the player's thread. */
    fun setUpNext(tracks: List<MediaItem>) {
        if (tracks == upNext) return
        upNext = tracks
        invalidateState()
    }

    override fun getState(): State {
        val state = super.getState()
        val commands = state.availableCommands.buildUpon()
            .remove(Player.COMMAND_SEEK_TO_NEXT)
            .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
            .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .remove(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
            .build()
        val onAir = state.playlist.getOrNull(state.currentMediaItemIndex)
            ?: return state.buildUpon().setAvailableCommands(commands).build()
        val queued = upNext.mapIndexed { index, item ->
            MediaItemData.Builder("up-next:$index:${item.mediaId}").setMediaItem(item).build()
        }
        return state.buildUpon()
            .setAvailableCommands(commands)
            .setPlaylist(listOf(onAir) + queued)
            .setCurrentMediaItemIndex(0)
            .build()
    }

    /** Nothing in this list can be jumped to; the fallback streams are switched on the real player. */
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> =
        Futures.immediateVoidFuture()
}

/**
 * The station's queued tracks as a car display lists them: title over artist, with the cover. Requester names and
 * request messages are community content and are left out. A first entry that is already on air is dropped.
 */
internal fun upNextMediaItems(stationId: StationId, upcoming: List<QueueTrack>, onAirTitle: String?): List<MediaItem> =
    upcoming
        .dropWhile { track -> onAirTitle != null && onAirTitle.contains(track.displayTitle, ignoreCase = true) }
        .take(MAX_UP_NEXT_TRACKS)
        .map { track ->
            MediaItem.Builder()
                .setMediaId("$UP_NEXT_MEDIA_ID_PREFIX${stationId.value}:${track.position}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.displayTitle)
                        .setArtist(track.artistName)
                        .setAlbumTitle(track.albumTitle)
                        .setArtworkUri(track.artworkUrl?.let(Uri::parse))
                        .setIsBrowsable(false)
                        .setIsPlayable(false)
                        .build(),
                )
                .build()
        }

private const val UP_NEXT_MEDIA_ID_PREFIX = "24seven:up-next:"
private const val MAX_UP_NEXT_TRACKS = 20
