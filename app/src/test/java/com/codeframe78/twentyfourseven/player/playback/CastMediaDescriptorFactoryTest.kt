package com.codeframe78.twentyfourseven.player.playback

import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
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

    @Test
    fun currentTrackMetadataReplacesStationFallbackForTheSelectedStation() {
        val station = Station(
            id = StationId("test"),
            name = "Test Station",
            shortName = "Test",
            description = "Test station",
            logoUrl = "https://example.invalid/station.png",
            websiteUrl = "https://example.invalid/",
        )
        val stream = StreamVariant(
            url = "https://example.invalid/live",
            label = "Live",
            priority = 0,
            format = StreamFormat.Aac,
        )
        val nowPlaying = NowPlayingState(
            stationId = station.id,
            displayTitle = "Artist - Current track",
            artworkUrl = "https://example.invalid/current-track.png",
        )

        val descriptor = CastMediaDescriptorFactory.create(station, stream, nowPlaying)
        val message = CastNowPlayingMessageFactory.create(station, nowPlaying)

        assertEquals("Artist - Current track", descriptor.title)
        assertEquals("https://example.invalid/current-track.png", descriptor.artworkUrl)
        assertEquals("Artist - Current track", message.title)
        assertEquals("https://example.invalid/current-track.png", message.artworkUrl)
    }

    @Test
    fun currentTrackMetadataForAnotherStationDoesNotLeakIntoTheCastPayload() {
        val station = Station(
            id = StationId("test"),
            name = "Test Station",
            shortName = "Test",
            description = "Test station",
            logoUrl = "https://example.invalid/station.png",
            websiteUrl = "https://example.invalid/",
        )

        val message = CastNowPlayingMessageFactory.create(
            station,
            NowPlayingState(
                stationId = StationId("another"),
                displayTitle = "Other station track",
                artworkUrl = "https://example.invalid/other.png",
            ),
        )

        assertEquals("Test Station", message.title)
        assertEquals("https://example.invalid/station.png", message.artworkUrl)
    }
}
