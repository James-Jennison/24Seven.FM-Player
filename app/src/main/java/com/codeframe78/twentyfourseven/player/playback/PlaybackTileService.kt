package com.codeframe78.twentyfourseven.player.playback

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.codeframe78.twentyfourseven.player.R
import com.codeframe78.twentyfourseven.player.RadioApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** A Quick Settings tile that plays or stops the selected station and says what is on. */
class PlaybackTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null
    private val container get() = (application as RadioApplication).appContainer

    override fun onStartListening() {
        listening?.cancel()
        listening = scope.launch {
            combine(
                container.stationRepository.observeSelectedStation(),
                container.observePlaybackState(),
                container.nowPlayingRepository.observeNowPlaying(),
            ) { station, playback, nowPlaying -> Triple(station, playback, nowPlaying) }
                .collect { (station, playback, nowPlaying) ->
                    val tile = qsTile ?: return@collect
                    val active = playback.status.keepsListening
                    tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    tile.label = station.shortName
                    tile.icon = Icon.createWithResource(this@PlaybackTileService, R.drawable.ic_tile_radio)
                    tile.contentDescription = if (active) "Stop ${station.name}" else "Play ${station.name}"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = when {
                            active && nowPlaying.stationId == station.id && !nowPlaying.displayTitle.isNullOrBlank() ->
                                nowPlaying.track?.takeIf { it.isNotBlank() } ?: nowPlaying.displayTitle
                            active -> "Playing"
                            else -> "Tap to listen"
                        }
                    }
                    tile.updateTile()
                }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
    }

    override fun onClick() {
        container.listenerControls.togglePlayback()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
