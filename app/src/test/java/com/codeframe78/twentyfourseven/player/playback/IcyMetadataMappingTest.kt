package com.codeframe78.twentyfourseven.player.playback

import androidx.media3.common.Metadata
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import com.codeframe78.twentyfourseven.player.domain.StationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class IcyMetadataMappingTest {
    @Test
    fun `raw ICY title is mapped without guessing field boundaries`() {
        val metadata = Metadata(IcyInfo(byteArrayOf(), "  Artist - Album - Track (3:45)  ", null))

        assertEquals(
            "Artist - Album - Track (3:45)",
            metadata.toNowPlayingState(StationId("sst"))?.displayTitle,
        )
    }

    @Test
    fun `blank ICY title is ignored`() {
        val metadata = Metadata(IcyInfo(byteArrayOf(), "   ", null))

        assertNull(metadata.toNowPlayingState(StationId("sst")))
    }

    @Test
    fun `windows 1252 punctuation in legacy ICY metadata is normalized without transcoding unicode`() {
        val metadata = Metadata(
            IcyInfo(
                byteArrayOf(),
                "Tycho - 東京 - Coastal Brake (Hatchback\u0092s Cosmic Caviar Dub)",
                null,
            ),
        )

        assertEquals(
            "Tycho - 東京 - Coastal Brake (Hatchback’s Cosmic Caviar Dub)",
            metadata.toNowPlayingState(StationId("entranced"))?.displayTitle,
        )
    }

    @Test
    fun `live title preserves station identity in session metadata`() {
        val stationMetadata = MediaMetadata.Builder()
            .setTitle("StreamingSoundtracks.com")
            .setArtist("24seven.FM")
            .setAlbumTitle("StreamingSoundtracks.com")
            .setSubtitle("Primary relay")
            .build()

        val liveMetadata = stationMetadata.withNowPlayingTitle("Raw ICY title")

        assertEquals("Raw ICY title", liveMetadata.title)
        assertEquals("StreamingSoundtracks.com", liveMetadata.artist)
        assertEquals("StreamingSoundtracks.com", liveMetadata.albumTitle)
        assertEquals("24seven.FM", liveMetadata.subtitle)
    }

    @Test
    fun `metadata-only media replacement preserves pending artwork`() {
        assertFalse(shouldClearNowPlaying("death:0", "death:0"))
        assertTrue(shouldClearNowPlaying("death:0", "death:1"))
        assertTrue(shouldClearNowPlaying("death:0", "sst:0"))
        assertTrue(shouldClearNowPlaying(null, "death:0"))
    }
}
