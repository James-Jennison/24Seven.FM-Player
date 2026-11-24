package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FavoriteTrackSortOrderTest {
    @Test
    fun `station order shows the list as the station sent it`() {
        val ordered = (1..1_500).map { position ->
            FavoriteTrack(position, "Track $position", "Album", "Artist")
        }
        assertSame(ordered, ordered.sortedForFavorites(FavoriteTrackSortOrder.Position))

        // A redesigned row's position is its ranking number, so a later page's rows keep their place after the
        // rows already read instead of being sorted in among them.
        val pages = listOf(349, 1500, 47, 632, 921, 57).map { rank -> FavoriteTrack(rank, "Track $rank", "Album", "Artist") }
        assertEquals(
            listOf(349, 1500, 47, 632, 921, 57),
            pages.sortedForFavorites(FavoriteTrackSortOrder.Position).map(FavoriteTrack::position),
        )
    }

    @Test
    fun `a title is quoted once`() {
        assertEquals("\"Opening\"", quotedTitle("Opening"))
        assertEquals("\"Sons Of Scotland!\"", quotedTitle("\"Sons Of Scotland!\""))
        assertEquals("\"\"Let's Go, Boys\" Reprise\"", quotedTitle("\"Let's Go, Boys\" Reprise"))
    }

    @Test
    fun `favorites sort by every displayed metadata field`() {
        assertOrder(FavoriteTrackSortOrder.Position, 3, 1, 2)
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
