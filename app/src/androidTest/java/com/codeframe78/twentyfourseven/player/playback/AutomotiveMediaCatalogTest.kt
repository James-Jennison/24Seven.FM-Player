package com.codeframe78.twentyfourseven.player.playback

import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
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
}
