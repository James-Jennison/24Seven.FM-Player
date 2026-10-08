package com.codeframe78.twentyfourseven.player.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId

/**
 * The intentionally small, public media tree exposed to Android Auto.
 *
 * It contains only the five approved live-radio stations. Account state, community content, and
 * URLs supplied by a controller never enter the playback queue.
 */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal class AutomotiveMediaCatalog(stations: List<Station>) {
    private val stationsById = stations.associateBy { it.id }
    private val stationItems = stations.map(::stationItem)

    fun rootItem(): MediaItem = MediaItem.Builder()
        .setMediaId(ROOT_MEDIA_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("24Seven.FM stations")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build(),
        )
        .build()

    /** Five stations with square logos read best as a grid of artwork, all on one screen, rather than a list. */
    fun rootParams(): LibraryParams = LibraryParams.Builder().setExtras(contentStyle()).build()

    fun children(parentId: String, page: Int, pageSize: Int): List<MediaItem>? {
        if (parentId != ROOT_MEDIA_ID || page < 0 || pageSize <= 0) return null
        return stationItems.page(page, pageSize)
    }

    fun item(mediaId: String): MediaItem? = stationsById[mediaId.toStationIdOrNull()]?.let(::stationItem)

    fun search(query: String, page: Int, pageSize: Int): List<MediaItem>? {
        if (page < 0 || pageSize <= 0) return null
        val terms = query.trim().lowercase()
        val matches = if (terms.isEmpty()) stationItems else stationItems.filter { item ->
            val station = stationsById[item.mediaId.toStationIdOrNull()] ?: return@filter false
            listOf(station.name, station.shortName, station.description)
                .any { value -> value.lowercase().contains(terms) }
        }
        return matches.page(page, pageSize)
    }

    fun playbackItems(mediaItems: List<MediaItem>): List<MediaItem>? {
        val station = mediaItems.singleOrNull()
            ?.mediaId
            ?.toStationIdOrNull()
            ?.let(stationsById::get)
            ?: return null
        return station.streams
            .sortedBy { it.priority }
            .map { stream ->
                MediaItem.Builder()
                    .setMediaId("${station.id.value}:${stream.priority}")
                    .setUri(stream.url)
                    .setMediaMetadata(stationMetadata(station, stream.label))
                    .build()
            }
            .takeIf(List<MediaItem>::isNotEmpty)
    }

    fun stationIdFor(mediaItems: List<MediaItem>): StationId? = mediaItems.singleOrNull()
        ?.mediaId
        ?.toStationIdOrNull()
        ?.takeIf(stationsById::containsKey)

    private fun stationItem(station: Station): MediaItem = MediaItem.Builder()
        .setMediaId("$STATION_MEDIA_ID_PREFIX${station.id.value}")
        .setUri(station.streams.minByOrNull { it.priority }?.url)
        .setMediaMetadata(stationMetadata(station, subtitle = station.description))
        .build()

    private fun stationMetadata(station: Station, subtitle: String): MediaMetadata = MediaMetadata.Builder()
        .setTitle(station.name)
        // A browser shows this line under the name, so it says what the station plays.
        .setArtist(station.description)
        .setAlbumTitle(station.name)
        .setStation(station.name)
        .setDescription(station.description)
        .setSubtitle(subtitle)
        .setExtras(contentStyle())
        .setArtworkUri(station.logoUrl?.let(Uri::parse))
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()

    private fun contentStyle() = Bundle().apply {
        putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
        putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
    }

    private fun String.toStationIdOrNull(): StationId? = takeIf { startsWith(STATION_MEDIA_ID_PREFIX) }
        ?.removePrefix(STATION_MEDIA_ID_PREFIX)
        ?.takeIf(String::isNotBlank)
        ?.let(::StationId)

    private companion object {
        const val ROOT_MEDIA_ID = "24seven:auto:root"
        const val STATION_MEDIA_ID_PREFIX = "24seven:auto:station:"
    }
}

/** One page of a list for any non-negative page and positive size a media browser sends, however large. */
internal fun <T> List<T>.page(page: Int, pageSize: Int): List<T> {
    val fromIndex = (page.toLong() * pageSize).coerceAtMost(size.toLong())
    val toIndex = (fromIndex + pageSize).coerceAtMost(size.toLong())
    return subList(fromIndex.toInt(), toIndex.toInt())
}
