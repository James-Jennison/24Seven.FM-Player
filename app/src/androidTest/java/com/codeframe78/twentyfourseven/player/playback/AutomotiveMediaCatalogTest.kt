package com.codeframe78.twentyfourseven.player.playback

import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomotiveMediaCatalogTest {
    private val catalog = AutomotiveMediaCatalog(BootstrapStationRepository().availableStations())

    @Test
    fun exposesOnlyTheFivePublicStationsAndTheirApprovedStreams() {
        val root = catalog.rootItem()
        val stations = requireNotNull(catalog.children(root.mediaId, page = 0, pageSize = 10))

        assertTrue(root.mediaMetadata.isBrowsable == true)
        assertFalse(root.mediaMetadata.isPlayable == true)
        assertEquals(5, stations.size)
        assertTrue(stations.all { it.mediaMetadata.isPlayable == true })
        assertTrue(stations.all { it.mediaMetadata.isBrowsable == false })
        // Each station says what it plays under its name, and the five are offered as a grid of artwork.
        assertTrue(stations.all { !it.mediaMetadata.artist.isNullOrBlank() && it.mediaMetadata.artist != "24seven.FM" })
        assertTrue(stations.all { it.mediaMetadata.station == it.mediaMetadata.title })
        assertEquals(
            androidx.media3.session.MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM,
            catalog.rootParams().extras.getInt(androidx.media3.session.MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE),
        )

        val playbackItems = requireNotNull(catalog.playbackItems(listOf(stations.first())))
        assertEquals(1, playbackItems.size)
        assertTrue(playbackItems.first().mediaId.startsWith("1980s:"))
        assertTrue(playbackItems.first().localConfiguration?.uri.toString().startsWith("https://"))
    }

    @Test
    fun rejectsUnknownMediaIdsAndControllerSuppliedUrls() {
        val injectedItem = MediaItem.Builder()
            .setMediaId("24seven:auto:station:unknown")
            .setUri("https://example.invalid/unapproved-stream")
            .build()

        assertNull(catalog.item(injectedItem.mediaId))
        assertNull(catalog.playbackItems(listOf(injectedItem)))
        assertNull(catalog.children("unknown-parent", page = 0, pageSize = 10))
    }

    @Test
    fun supportsStationNameSearchWithoutExposingProtectedContent() {
        val matches = requireNotNull(catalog.search("classical", page = 0, pageSize = 10))

        assertEquals(1, matches.size)
        assertEquals("Adagio.FM", matches.single().mediaMetadata.title)
        assertNotNull(catalog.item(matches.single().mediaId))
    }

    @Test
    fun upNextListsQueuedTracksWithoutCommunityContentOrTheTrackOnAir() {
        val upcoming = listOf(
            QueueTrack(1, "Final Confrontation", artistName = "Composer One", albumTitle = "Album One"),
            QueueTrack(
                2, "Opening", artistName = "Composer Two", albumTitle = "Album Two",
                artworkUrl = "https://streamingsoundtracks.com/images/cover/040/B09TY6GHMR.jpg",
                requesterName = "Listener", requestMessage = "Hey everyone",
            ),
        )

        val items = upNextMediaItems(StationId("sst"), upcoming, onAirTitle = "Composer One - Final Confrontation")

        assertEquals(1, items.size)
        val item = items.single()
        assertEquals("Opening", item.mediaMetadata.title)
        assertEquals("Composer Two", item.mediaMetadata.artist)
        assertEquals("Album Two", item.mediaMetadata.albumTitle)
        assertFalse(item.mediaMetadata.isPlayable == true)
        assertNull(item.localConfiguration)
        assertFalse(item.mediaMetadata.toString().contains("Listener"))
        assertEquals(2, upNextMediaItems(StationId("sst"), upcoming, onAirTitle = null).size)
    }

    @Test
    fun aStreamIsAFallbackOnlyWhenItIsNotTheStationsFirstChoice() {
        val station = BootstrapStationRepository().availableStations().first()
        val first = station.streams.minOf { it.priority }

        assertFalse(isFallbackStream("${station.id.value}:$first", station))
        assertTrue(isFallbackStream("${station.id.value}:${first + 1}", station))
        assertFalse(isFallbackStream("another:${first + 1}", station))
        assertFalse(isFallbackStream(null, station))
    }
}
