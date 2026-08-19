package com.codeframe78.twentyfourseven.player.playback

import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
import com.codeframe78.twentyfourseven.player.domain.StreamVariant
import com.google.android.gms.cast.MediaStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CastMediaDescriptorFactoryTest {
    @Test
    fun liveAacStationProducesReceiverSafeLiveDescriptor() {
        val station = Station(
            id = StationId("test"),
            name = "Test Station",
            shortName = "Test",
            description = "Test station",
            websiteUrl = "https://example.invalid/",
        )
        val stream = StreamVariant(
            url = "https://example.invalid/live",
            label = "Live",
            priority = 0,
            format = StreamFormat.Aac,
        )

        val descriptor = CastMediaDescriptorFactory.create(station, stream)

        assertEquals("https://example.invalid/live", descriptor.contentId)
        assertEquals("audio/aacp", descriptor.contentType)
        assertEquals("Test Station", descriptor.title)
        assertEquals("Test Station", descriptor.stationName)
    }

    @Test
    fun contentTypesMatchTheExistingStationFormatContract() {
        assertEquals("audio/mpeg", StreamFormat.Mp3.castContentType())
        assertEquals("application/x-mpegURL", StreamFormat.Hls.castContentType())
        assertEquals("audio/aacp", StreamFormat.Unknown.castContentType())
    }

    @Test
    fun idleReceiverDoesNotEraseTheSenderPlaybackIntent() {
        assertEquals(null, remotePlaybackIntentFor(MediaStatus.PLAYER_STATE_IDLE))
        assertEquals(true, remotePlaybackIntentFor(MediaStatus.PLAYER_STATE_PLAYING))
        assertEquals(true, remotePlaybackIntentFor(MediaStatus.PLAYER_STATE_BUFFERING))
        assertEquals(false, remotePlaybackIntentFor(MediaStatus.PLAYER_STATE_PAUSED))
    }
}
