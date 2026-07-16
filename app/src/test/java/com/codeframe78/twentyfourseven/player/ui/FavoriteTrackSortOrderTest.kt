package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FavoriteTrackSortOrderTest {
    @Test
    fun `station position order reuses an already ordered large list`() {
        val ordered = (1..1_500).map { position ->
            FavoriteTrack(position, "Track $position", "Album", "Artist")
        }

        assertSame(ordered, ordered.sortedForFavorites(FavoriteTrackSortOrder.Position))
    }

    @Test
    fun `favorites sort by every displayed metadata field`() {
        assertOrder(FavoriteTrackSortOrder.Position, 1, 2, 3)
        assertOrder(FavoriteTrackSortOrder.TrackName, 1, 2, 3)
        assertOrder(FavoriteTrackSortOrder.Album, 3, 2, 1)
        assertOrder(FavoriteTrackSortOrder.Artist, 2, 3, 1)
        assertOrder(FavoriteTrackSortOrder.Genre, 3, 1, 2)
        assertOrder(FavoriteTrackSortOrder.Year, 1, 3, 2)
        assertOrder(FavoriteTrackSortOrder.Length, 1, 3, 2)
        assertOrder(FavoriteTrackSortOrder.PlayState, 1, 2, 3)
    }

    private fun assertOrder(order: FavoriteTrackSortOrder, vararg positions: Int) {
        assertEquals(
            positions.toList(),
            tracks.sortedForFavorites(order).map(FavoriteTrack::position),
        )
    }

    private val tracks = listOf(
        FavoriteTrack(
            position = 3,
            title = "Zebra",
            album = "Alpha",
            artist = "Beta",
            genre = "Game",
            year = "2020",
            duration = "10:00",
            availability = TrackRequestAvailability(TrackRequestStatus.RecentlyPlayed),
        ),
        FavoriteTrack(
            position = 1,
            title = "alpha",
            album = "Zulu",
            artist = "Gamma",
            genre = "Soundtrack",
            year = "1999",
            duration = "2:30",
            availability = TrackRequestAvailability(TrackRequestStatus.Available),
        ),
        FavoriteTrack(
            position = 2,
            title = "Beta",
            album = "beta",
            artist = "Alpha",
            duration = "1:00:00",
            availability = TrackRequestAvailability(TrackRequestStatus.InCurrentQueue),
        ),
    )
}
