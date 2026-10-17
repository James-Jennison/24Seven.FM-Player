package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentTrackArtworkParserTest {
    private val parser = CurrentTrackArtworkParser()

    @Test
    fun `uses verified five hundred pixel cover path for an explicit asin`() {
        val response = JSONObject().put("ASIN", "B00Q5M2SYS").toString()

        assertEquals(
            "https://streamingsoundtracks.com/images/cover/500/B00Q5M2SYS.jpg",
            parser.parse(response, "https://streamingsoundtracks.com/"),
        )
    }

    @Test
    fun `upgrades a station cover link using its explicit album identifier`() {
        val response = JSONObject()
            .put("CoverLink", "https://adagio.fm/images/cover/040/B012345678.jpg")
            .toString()

        assertEquals(
            "https://adagio.fm/images/cover/500/B012345678.jpg",
            parser.parse(response, "https://adagio.fm/"),
        )
    }

    @Test
    fun `rejects artwork outside the selected station domain`() {
        val response = JSONObject()
            .put("CoverLink", "https://example.com/images/cover/040/B012345678.jpg")
            .toString()

        assertNull(parser.parse(response, "https://death.fm/"))
    }

    @Test
    fun `parses current track details for Cast metadata refresh`() {
        val response = JSONObject()
            .put("Artist", "Don Davis")
            .put("Album", "Jurassic Park III")
            .put("Track", "Bone Man Ben")
            .put("ASIN", "B00Q5M2SYS")
            .toString()

        val details = parser.parseNowPlaying(response, "https://streamingsoundtracks.com/", StationId("sst"))

        assertEquals("Don Davis - Bone Man Ben", details?.displayTitle)
        assertEquals("Don Davis", details?.artist)
        assertEquals("Jurassic Park III", details?.album)
        assertEquals("Bone Man Ben", details?.track)
        assertEquals(
            "https://streamingsoundtracks.com/images/cover/500/B00Q5M2SYS.jpg",
            details?.artworkUrl,
        )
    }

    @Test
    fun `parses who requested the track, the audience, and how far it has played`() {
        val response = JSONObject()
            .put("Artist", "Max Richter")
            .put("Album", "Black Mirror: Nosedive")
            .put("Track", "The Journey, Not The Destination")
            .put("Length", "276812")
            .put("PlayStart", "2026-10-02T06:37:17")
            .put("SystemTime", "2026-10-02T06:41:15")
            .put("CoverLink", "https://streamingsoundtracks.com/images/cover/B01M9B4IUW.jpg")
            .put("RequestedBy", "Listener")
            .put("ListenerCount", "92")
            .put("Message", JSONObject.NULL)
            .toString()

        val details = parser.parseNowPlaying(
            response,
            "https://streamingsoundtracks.com/",
            StationId("sst"),
            receivedAtElapsedRealtimeMillis = 1_000_000L,
        )

        assertEquals("Listener", details?.requesterName)
        assertNull(details?.requestMessage)
        assertEquals(92, details?.listenerCount)
        assertEquals("B01M9B4IUW", details?.albumId)
        assertEquals(276_812L, details?.trackLengthMillis)
        assertEquals(1_000_000L - 238_000L, details?.trackStartedElapsedRealtimeMillis)
    }

    @Test
    fun `leaves progress unknown when the station times are missing or impossible`() {
        val response = JSONObject()
            .put("Artist", "Don Davis")
            .put("Track", "Bone Man Ben")
            .put("Length", "0")
            .put("PlayStart", "2026-10-02T06:41:15")
            .put("SystemTime", "2026-10-02T06:37:17")
            .put("ListenerCount", "many")
            .toString()

        val details = parser.parseNowPlaying(response, "https://death.fm/", StationId("dfm"), 5_000L)

        assertNull(details?.trackLengthMillis)
        assertNull(details?.trackStartedElapsedRealtimeMillis)
        assertNull(details?.listenerCount)
        assertNull(details?.requesterName)
    }

    @Test
    fun `decodes HTML entities in station metadata before publishing it`() {
        val response = JSONObject()
            .put("Artist", "Billy Joel")
            .put("Track", "She&#039;s Right On Time")
            .toString()

        val details = parser.parseNowPlaying(response, "https://1980s.fm/", StationId("1980s"))

        assertEquals("Billy Joel - She's Right On Time", details?.displayTitle)
    }

    @Test
    fun `restores leading the from catalog sort order`() {
        val response = JSONObject()
            .put("Artist", "Passions, The")
            .put("Track", "The Story")
            .toString()

        val details = parser.parseNowPlaying(response, "https://1980s.fm/", StationId("1980s"))

        assertEquals("The Passions - The Story", details?.displayTitle)
    }

    @Test
    fun `parses the official v1 feed and keeps the artwork version query`() {
        val response = """{"schema_version":1,"status":"playing","generated_at":"2026-10-17T18:03:52+00:00",
            "track":{"id":"1","play_id":"2","title":"Power Play","artist":"Eddie And The Tide",
            "album":"Lost Boys, The","started_at":"2026-10-17T18:00:53+00:00","duration_seconds":240.7},
            "artwork_url":"https://1980s.fm/images/cover/500/B000002ILE.jpg?v=1784914957"}"""

        val details = parser.parseOfficialFeed(response, "https://1980s.fm/", StationId("1980s"))

        assertEquals("Eddie And The Tide - Power Play", details?.displayTitle)
        assertEquals("Lost Boys, The", details?.album)
        assertEquals("https://1980s.fm/images/cover/500/B000002ILE.jpg?v=1784914957", details?.artworkUrl)
    }

    @Test
    fun `official feed reports nothing when unavailable or off-domain artwork`() {
        val unavailable = """{"schema_version":1,"status":"unavailable","track":null}"""
        assertNull(parser.parseOfficialFeed(unavailable, "https://1980s.fm/", StationId("1980s")))

        val offDomain = """{"schema_version":1,"status":"playing","track":{"title":"A","artist":"B"},
            "artwork_url":"https://example.com/x.jpg"}"""
        assertNull(parser.parseOfficialFeed(offDomain, "https://1980s.fm/", StationId("1980s"))?.artworkUrl)
    }

    @Test
    fun `registry parsing keeps valid https feeds and skips the rest`() {
        val registry = """{"schema_version":1,"stations":[
            {"id":"80s","now_playing_json":"https://1980s.fm/now-playing.php","extra":1},
            {"id":"bad","now_playing_json":"http://bad.example/x"},{"id":"","now_playing_json":"https://a.b/c"}]}"""

        assertEquals(
            mapOf("80s" to "https://1980s.fm/now-playing.php"),
            StationNowPlayingArtworkRepository.parseRegistry(registry),
        )
    }
}
