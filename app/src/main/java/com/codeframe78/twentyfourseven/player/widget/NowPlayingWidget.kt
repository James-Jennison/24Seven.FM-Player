package com.codeframe78.twentyfourseven.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.codeframe78.twentyfourseven.player.AppContainer
import com.codeframe78.twentyfourseven.player.MainActivity
import com.codeframe78.twentyfourseven.player.R
import com.codeframe78.twentyfourseven.player.RadioApplication
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.playback.keepsListening
import com.codeframe78.twentyfourseven.player.ui.stationSelectorLogoResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The home-screen widget: cover, track, station, and one button that plays or stops the selected station. */
class NowPlayingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        NowPlayingWidget.refresh(context, container(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NowPlayingWidget.ACTION_TOGGLE_PLAYBACK) {
            container(context).listenerControls.togglePlayback()
        }
        super.onReceive(context, intent)
    }

    private fun container(context: Context) = (context.applicationContext as RadioApplication).appContainer
}

object NowPlayingWidget {
    const val ACTION_TOGGLE_PLAYBACK = "com.codeframe78.twentyfourseven.player.widget.TOGGLE_PLAYBACK"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Keeps every placed widget current for as long as the app process lives, which is as long as audio plays. */
    @OptIn(FlowPreview::class)
    fun observe(context: Context, container: AppContainer) {
        scope.launch {
            combine(
                container.stationRepository.observeSelectedStation(),
                container.nowPlayingRepository.observeNowPlaying(),
                container.observePlaybackState(),
            ) { station, nowPlaying, playback -> Triple(station, nowPlaying, playback) }
                .debounce(REFRESH_DEBOUNCE_MILLIS)
                .collect { (station, nowPlaying, playback) ->
                    if (widgetIds(context).isNotEmpty()) render(context, station, nowPlaying, playback)
                }
        }
    }

    fun refresh(context: Context, container: AppContainer) {
        scope.launch {
            render(
                context,
                container.stationRepository.observeSelectedStation().first(),
                container.nowPlayingRepository.observeNowPlaying().first(),
                container.observePlaybackState().first(),
            )
        }
    }

    private suspend fun render(context: Context, station: Station, nowPlaying: NowPlayingState, playback: PlaybackState) {
        val ids = widgetIds(context)
        if (ids.isEmpty()) return
        val listening = playback.status.keepsListening
        val showsTrack = nowPlaying.stationId == station.id && !nowPlaying.displayTitle.isNullOrBlank()
        val title = if (showsTrack) nowPlaying.track?.takeIf { it.isNotBlank() } ?: nowPlaying.displayTitle.orEmpty() else station.name
        val subtitle = when {
            showsTrack -> listOfNotNull(nowPlaying.artist?.takeIf { it.isNotBlank() }, station.shortName).joinToString(" · ")
            listening -> "Connecting…"
            else -> "Tap to listen"
        }
        val artwork = nowPlaying.artworkUrl?.takeIf { showsTrack && it.isNotBlank() }?.let { loadArtwork(context, it) }
        val views = RemoteViews(context.packageName, R.layout.widget_now_playing).apply {
            setTextViewText(R.id.widget_title, title)
            setTextViewText(R.id.widget_subtitle, subtitle)
            if (artwork != null) {
                setImageViewBitmap(R.id.widget_artwork, artwork)
            } else {
                setImageViewResource(R.id.widget_artwork, stationSelectorLogoResource(station.id))
            }
            setImageViewResource(R.id.widget_toggle, if (listening) R.drawable.ic_widget_stop else R.drawable.ic_widget_play)
            setContentDescription(R.id.widget_toggle, if (listening) "Stop radio" else "Play ${station.name}")
            setOnClickPendingIntent(R.id.widget_toggle, togglePendingIntent(context))
            setOnClickPendingIntent(R.id.widget_root, openPendingIntent(context))
        }
        AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
    }

    private suspend fun loadArtwork(context: Context, url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(ARTWORK_PIXELS)
            .allowHardware(false)
            .build()
        val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull() as? SuccessResult
        return result?.image?.toBitmap()
    }

    private fun widgetIds(context: Context): IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, NowPlayingWidgetProvider::class.java))

    private fun togglePendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NowPlayingWidgetProvider::class.java).setAction(ACTION_TOGGLE_PLAYBACK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        1,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private const val REFRESH_DEBOUNCE_MILLIS = 250L
    private const val ARTWORK_PIXELS = 256
}
