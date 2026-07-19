package com.codeframe78.twentyfourseven.player.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackRequestAvailabilityResolverTest {
    @Test
    fun `queued and recently played tracks keep distinct internal states`() {
        val queued = readyQueue(
            upcoming = listOf(
                QueueTrack(
                    position = 2,
                    displayTitle = "Example Track",
                    albumId = "ALBUM_1",
                    artistName = "Example Artist",
                    albumTitle = "Example Album",
                ),
            ),
        )
        val played = readyQueue(
            recentlyPlayed = listOf(
                HistoryTrack(
                    displayTitle = "Example Track",
                    albumId = "ALBUM_1",
                    artistName = "Example Artist",
                    albumTitle = "Example Album",
                ),
            ),
        )

        assertEquals(
            TrackRequestStatus.InCurrentQueue,
            TrackRequestAvailabilityResolver.resolve(stationId, identity, available, queued).status,
        )
        assertEquals(
            TrackRequestStatus.RecentlyPlayed,
            TrackRequestAvailabilityResolver.resolve(stationId, identity, available, played).status,
        )
    }

    @Test
    fun `matching is station scoped and never uses title alone`() {
        val otherStationQueue = QueueState(
            StationId("dfm"),
            QueueLoadStatus.Ready,
            upcoming = listOf(QueueTrack(1, "Example Track", albumId = "ALBUM_1")),
        )
        val titleOnlyQueue = readyQueue(upcoming = listOf(QueueTrack(1, "Example Track")))

        assertEquals(
            TrackRequestStatus.StationUnavailable,
            TrackRequestAvailabilityResolver.resolve(stationId, identity, available, otherStationQueue).status,
        )
        assertFalse(
            TrackRequestAvailabilityResolver.matches(
                identity.copy(albumId = null, artist = null, albumTitle = null),
                RequestTrackIdentity(title = "Example Track"),
            ),
        )
        assertEquals(
            TrackRequestStatus.RequestsUnavailable,
            TrackRequestAvailabilityResolver.resolve(stationId, identity, available, titleOnlyQueue).status,
        )
    }

    @Test
    fun `stale or missing queue never exposes request now`() {
        val loading = QueueState(stationId, QueueLoadStatus.Loading)

        val result = TrackRequestAvailabilityResolver.resolve(stationId, identity, available, loading)

        assertEquals(TrackRequestStatus.RequestsUnavailable, result.status)
        assertFalse(result.canRequest)
        assertTrue(result.detail.orEmpty().contains("Queue status"))
    }

    @Test
    fun `server restrictions remain distinct from recent and queued states`() {
        assertEquals(
            TrackRequestStatus.AuthenticationRequired,
            classifyStationRequestAvailability("Sign in before requesting").status,
        )
        assertEquals(
            TrackRequestStatus.UserCooldown,
            classifyStationRequestAvailability("Request cooldown active; wait before trying again").status,
        )
        assertEquals(
            TrackRequestStatus.MembershipRequired,
            classifyStationRequestAvailability("VIP membership required").status,
        )
        assertEquals(
            TrackRequestStatus.RequestLimitReached,
            classifyStationRequestAvailability("Maximum requests reached").status,
        )
        assertEquals(
            TrackRequestStatus.RequestsUnavailable,
            classifyStationRequestAvailability("The artist is already in queue.").status,
        )
    }

    @Test
    fun `batch resolution handles a full favorite list and preserves input order`() {
        val candidates = (1..1_500).map { position ->
            TrackRequestCandidate(
                RequestTrackIdentity(
                    songId = position.toString(),
                    albumId = "ALBUM_$position",
                    title = "Favorite $position",
                    artist = "Artist $position",
                    albumTitle = "Album $position",
                ),
                available,
            )
        }
        val queue = readyQueue(
            upcoming = listOf(QueueTrack(1, "Favorite 750", songId = "750")),
            recentlyPlayed = listOf(HistoryTrack("Favorite 1250", songId = "1250")),
        )

        val resolved = TrackRequestAvailabilityResolver.resolveAll(stationId, candidates, queue)

        assertEquals(1_500, resolved.size)
        assertEquals(TrackRequestStatus.Available, resolved.first().status)
        assertEquals(TrackRequestStatus.InCurrentQueue, resolved[749].status)
        assertEquals(TrackRequestStatus.RecentlyPlayed, resolved[1_249].status)
        assertEquals(TrackRequestStatus.Available, resolved.last().status)
    }

    private fun readyQueue(
        upcoming: List<QueueTrack> = emptyList(),
        recentlyPlayed: List<HistoryTrack> = emptyList(),
    ) = QueueState(stationId, QueueLoadStatus.Ready, upcoming, recentlyPlayed)

    private companion object {
        val stationId = StationId("sst")
        val available = TrackRequestAvailability.available()
        val identity = RequestTrackIdentity(
            songId = "12345",
            albumId = "ALBUM_1",
            title = "Example Track",
            artist = "Example Artist",
            albumTitle = "Example Album",
        )
    }
}
