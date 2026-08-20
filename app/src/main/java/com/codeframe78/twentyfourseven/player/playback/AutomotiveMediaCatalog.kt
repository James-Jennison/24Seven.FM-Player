package com.codeframe78.twentyfourseven.player.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId

/**
 * The intentionally small, public media tree exposed to Android Auto.
 *
 * It contains only the five approved live-radio stations. Account state, community content, and
 * URLs supplied by a controller never enter the playback queue.
 */
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

    fun children(parentId: String, page: Int, pageSize: Int): List<MediaItem>? {
        if (parentId != ROOT_MEDIA_ID || page < 0 || pageSize <= 0) return null
        val fromIndex = (page.toLong() * pageSize).coerceAtMost(stationItems.size.toLong()).toInt()
        val toIndex = (fromIndex + pageSize).coerceAtMost(stationItems.size)
        return stationItems.subList(fromIndex, toIndex)
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
        val fromIndex = (page.toLong() * pageSize).coerceAtMost(matches.size.toLong()).toInt()
        val toIndex = (fromIndex + pageSize).coerceAtMost(matches.size)
        return matches.subList(fromIndex, toIndex)
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
        .setMediaMetadata(stationMetadata(station, subtitle = "Live radio"))
        .build()

    private fun stationMetadata(station: Station, subtitle: String): MediaMetadata = MediaMetadata.Builder()
        .setTitle(station.name)
        .setArtist("24seven.FM")
        .setAlbumTitle("24Seven.FM stations")
        .setDescription(station.description)
        .setSubtitle(subtitle)
        .setArtworkUri(station.logoUrl?.let(Uri::parse))
        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .build()

    private fun String.toStationIdOrNull(): StationId? = takeIf { startsWith(STATION_MEDIA_ID_PREFIX) }
        ?.removePrefix(STATION_MEDIA_ID_PREFIX)
        ?.takeIf(String::isNotBlank)
        ?.let(::StationId)

    private companion object {
        const val ROOT_MEDIA_ID = "24seven:auto:root"
        const val STATION_MEDIA_ID_PREFIX = "24seven:auto:station:"
    }
}
