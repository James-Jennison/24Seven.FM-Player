package com.codeframe78.twentyfourseven.player.shortcuts

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.codeframe78.twentyfourseven.player.AppContainer
import com.codeframe78.twentyfourseven.player.MainActivity
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.ui.stationSelectorLogoResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Long-press shortcuts on the launcher icon, one per station, each starting that station playing. The selected
 * station is ranked first so it is always among the few the launcher shows.
 */
object StationShortcuts {
    const val EXTRA_STATION_ID = "shortcut_station_id"
    const val EXTRA_START_PLAYBACK = "shortcut_start_playback"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Republishes the shortcuts whenever the selected station changes, so it stays first. */
    fun observe(context: Context, container: AppContainer) {
        scope.launch {
            container.stationRepository.observeSelectedStation()
                .map { it.id }
                .distinctUntilChanged()
                .collect { selected -> publish(context, container.stationRepository.availableStations(), selected) }
        }
    }

    fun publish(context: Context, stations: List<Station>, selectedId: StationId?) {
        if (stations.isEmpty()) return
        val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtMost(stations.size)
        val ordered = stations.sortedBy { if (it.id == selectedId) 0 else 1 }
        val shortcuts = ordered.take(limit).mapIndexed { rank, station ->
            ShortcutInfoCompat.Builder(context, "station:${station.id.value}")
                .setShortLabel(station.shortName)
                .setLongLabel("Play ${station.name}")
                .setIcon(IconCompat.createWithAdaptiveBitmap(adaptiveLogo(context, station)))
                .setRank(rank)
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(EXTRA_STATION_ID, station.id.value)
                        .putExtra(EXTRA_START_PLAYBACK, true),
                )
                .build()
        }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    /** The station logo inside the adaptive icon's safe zone on the app's purple, so launcher masks keep it whole. */
    private fun adaptiveLogo(context: Context, station: Station): Bitmap {
        val size = ADAPTIVE_ICON_PIXELS
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#160A23"))
        val logo = ContextCompat.getDrawable(context, stationSelectorLogoResource(station.id)) ?: return bitmap
        val inset = (size * (1 - SAFE_ZONE_FRACTION) / 2).toInt()
        val bounds = Rect(inset, inset, size - inset, size - inset)
        if (logo is BitmapDrawable) logo.isFilterBitmap = true
        logo.bounds = bounds
        logo.draw(canvas)
        return bitmap
    }

    private const val ADAPTIVE_ICON_PIXELS = 432
    private const val SAFE_ZONE_FRACTION = 66f / 108f
}
