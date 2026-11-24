package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteChange
import com.codeframe78.twentyfourseven.player.domain.FavoriteChangeStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksLoadStatus
import com.codeframe78.twentyfourseven.player.domain.RankedFavoritesStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkFavoriteTracksRepositoryTest {
    private val stationId = StationId("sst")

    @Test
    fun `refresh publishes immutable favorites and clear removes prior account data`() = runTest {
        val expected = listOf(FavoriteTrack(1, "Title", "Album", "Artist"))
        val repository = NetworkFavoriteTracksRepository(FakeFavoriteTracksRemote(expected))

        repository.refresh(stationId)
        val ready = repository.observeFavorites(stationId).first()
        assertEquals(FavoriteTracksLoadStatus.Ready, ready.status)
        assertEquals(expected, ready.tracks)

        repository.clear(stationId)
        val cleared = repository.observeFavorites(stationId).first()
        assertEquals(FavoriteTracksLoadStatus.Idle, cleared.status)
        assertTrue(cleared.tracks.isEmpty())
    }

    @Test
    fun `missing session exposes sign in guidance`() = runTest {
        val repository = NetworkFavoriteTracksRepository(
            FakeFavoriteTracksRemote(failure = FavoritesAuthenticationRequiredException()),
        )

        repository.refresh(stationId)
        val state = repository.observeFavorites(stationId).first()

        assertEquals(FavoriteTracksLoadStatus.Error, state.status)
        assertEquals("Sign in to this station to load your favorite tracks.", state.errorMessage)
    }

    @Test
    fun `a move is sent for a known track and the ranked order is read again, not the full list`() = runTest {
        val before = listOf(
            FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"),
            FavoriteTrack(2, "Finale", "Album", "Composer", songId = "12"),
        )
        val remote = FakeFavoriteTracksRemote(before, ranked = before)
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refresh(stationId)
        repository.refreshRanked(stationId)
        assertEquals(listOf("Opening", "Finale"), repository.observeFavorites(stationId).first().ranked.tracks.map { it.title })

        remote.ranked = before.reversed().mapIndexed { index, track -> track.copy(position = index + 1) }
        remote.loads = 0
        repository.changeFavorite(stationId, "12", FavoriteChange.MoveUp)

        assertEquals(listOf("12" to FavoriteChange.MoveUp), remote.changes)
        val state = repository.observeFavorites(stationId).first()
        assertEquals(FavoriteChangeStatus.Done, state.change.status)
        assertEquals(listOf("Finale", "Opening"), state.ranked.tracks.map { it.title })
        assertEquals(listOf("Opening", "Finale"), state.tracks.map { it.title })
        assertEquals(0, remote.loads)

        repository.changeFavorite(stationId, "99", FavoriteChange.Remove)
        assertEquals(1, remote.changes.size)
    }

    @Test
    fun `a removal reads both lists again and ranked pages load on request`() = runTest {
        val tracks = listOf(
            FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"),
            FavoriteTrack(2, "Finale", "Album", "Composer", songId = "12"),
        )
        val remote = FakeFavoriteTracksRemote(tracks, ranked = tracks, rankedPages = 2)
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refresh(stationId)
        repository.refreshRanked(stationId)
        assertEquals(true, repository.observeFavorites(stationId).first().ranked.hasMore)

        repository.loadMoreRanked(stationId)
        with(repository.observeFavorites(stationId).first().ranked) {
            assertEquals(2, loadedPages)
            assertEquals(4, this.tracks.size)
            assertEquals(false, hasMore)
        }

        remote.tracks = tracks.drop(1)
        remote.ranked = tracks.drop(1)
        remote.loads = 0
        repository.changeFavorite(stationId, "11", FavoriteChange.Remove)
        val state = repository.observeFavorites(stationId).first()
        assertEquals(1, remote.loads)
        assertEquals(listOf("Finale"), state.tracks.map { it.title })
        assertEquals(listOf("Finale", "Finale"), state.ranked.tracks.map { it.title })
    }

    @Test
    fun `a change that needs sign-in or fails is reported against the track`() = runTest {
        val tracks = listOf(FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"))
        val signedOut = FakeFavoriteTracksRemote(tracks, changeFailure = FavoritesAuthenticationRequiredException())
        val repository = NetworkFavoriteTracksRepository(signedOut)
        repository.refresh(stationId)
        repository.changeFavorite(stationId, "11", FavoriteChange.Remove)
        with(repository.observeFavorites(stationId).first().change) {
            assertEquals(FavoriteChangeStatus.SignInRequired, status)
            assertEquals("11", songId)
        }

        val broken = NetworkFavoriteTracksRepository(FakeFavoriteTracksRemote(tracks, changeFailure = java.io.IOException("offline")))
        broken.refresh(stationId)
        broken.changeFavorite(stationId, "11", FavoriteChange.MoveDown)
        assertEquals(FavoriteChangeStatus.Failed, broken.observeFavorites(stationId).first().change.status)
    }

    private class FakeFavoriteTracksRemote(
        var tracks: List<FavoriteTrack> = emptyList(),
        var failure: Throwable? = null,
        private val changeFailure: Throwable? = null,
        var ranked: List<FavoriteTrack> = emptyList(),
        private val rankedPages: Int = 1,
    ) : FavoriteTracksRemoteDataSource {
        val changes = mutableListOf<Pair<String, FavoriteChange>>()
        var loads = 0

        var listPages = 1

        val listRequests = mutableListOf<Pair<Int, Boolean>>()

        override suspend fun load(stationId: StationId, page: Int, requestableOnly: Boolean): FavoriteTracksPage {
            failure?.let { throw it }
            loads++
            listRequests += page to requestableOnly
            val pageTracks = if (listPages == 1) tracks else tracks.map { it.copy(songId = it.songId?.let { id -> "$id-$page" }) }
            return FavoriteTracksPage(pageTracks, nextPage = if (page < listPages) page + 1 else null, totalTracks = tracks.size * listPages)
        }

        override suspend fun loadRanked(stationId: StationId, page: Int): RankedFavoritesPage {
            failure?.let { throw it }
            return RankedFavoritesPage(ranked, nextPage = if (page < rankedPages) page + 1 else null)
        }

        var albumSongIds = mapOf<Pair<String, String>, String>()
        val lookups = mutableListOf<Pair<String, String>>()

        override suspend fun findSongId(stationId: StationId, albumId: String, title: String): String? {
            lookups += albumId to title
            return albumSongIds[albumId to title]
        }

        /** What every read after the next change fails with, as when the connection drops once the change is sent. */
        var failureAfterChange: Throwable? = null

        override suspend fun change(stationId: StationId, songId: String, change: FavoriteChange) {
            changeFailure?.let { throw it }
            changes += songId to change
            failureAfterChange?.let { failure = it }
        }
    }

    @Test
    fun `requestable only re-reads the first page and stays chosen for later pages and refreshes`() = runTest {
        val tracks = listOf(FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"))
        val remote = FakeFavoriteTracksRemote(tracks).apply { listPages = 3 }
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refresh(stationId)
        repository.loadMore(stationId)

        repository.setRequestableOnly(stationId, true)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(true, requestableOnly)
            assertEquals(1, loadedPages)
            assertEquals(1, this.tracks.size)
        }
        repository.loadMore(stationId)
        repository.refresh(stationId)
        // Choosing the same value again reads nothing.
        repository.setRequestableOnly(stationId, true)
        repository.setRequestableOnly(stationId, false)

        assertEquals(
            listOf(1 to false, 2 to false, 1 to true, 2 to true, 1 to true, 1 to false),
            remote.listRequests,
        )
        assertEquals(false, repository.observeFavorites(stationId).first().requestableOnly)
    }

    @Test
    fun `a move posts the row address with no fields and a removal posts the station's confirmation form`() {
        assertEquals(
            "/modules.php?name=Favorites&op=movetrackup&songid=246835" to "",
            favoriteChangeRequest(FavoriteChange.MoveUp, "246835"),
        )
        assertEquals(
            "/modules.php?name=Favorites&op=movetrackdown&songid=246835" to "",
            favoriteChangeRequest(FavoriteChange.MoveDown, "246835"),
        )
        // The row's own remove address only opens the "Are you sure" page, so the second step is what is sent.
        assertEquals(
            "/modules.php?name=Favorites" to "songid=246835&Yes2Remove=1&op=removetrackfromlist2",
            favoriteChangeRequest(FavoriteChange.Remove, "246835"),
        )
        assertEquals(true, runCatching { favoriteChangeRequest(FavoriteChange.Remove, "1&op=x") }.isFailure)
    }

    @Test
    fun `a ranked track without a song number takes it from its album page when it is changed`() = runTest {
        val remote = FakeFavoriteTracksRemote(
            ranked = listOf(
                FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"),
                FavoriteTrack(2, "Unavailable", "Album Two", "Composer", albumId = "B000000002"),
                FavoriteTrack(3, "Unknown", "Album Three", "Composer", albumId = "B000000003"),
            ),
        ).apply { albumSongIds = mapOf(("B000000002" to "Unavailable") to "22") }
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refreshRanked(stationId)

        repository.changeRankedFavorite(stationId, 2, FavoriteChange.MoveUp)
        assertEquals(listOf("22" to FavoriteChange.MoveUp), remote.changes)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(FavoriteChangeStatus.Done, change.status)
            // The number is not kept: the next change reads the album page again, so it is never a stale one.
            assertEquals(null, ranked.tracks[1].songId)
        }
        remote.albumSongIds = mapOf(("B000000002" to "Unavailable") to "23")
        repository.changeRankedFavorite(stationId, 2, FavoriteChange.MoveDown)
        assertEquals(List(2) { "B000000002" to "Unavailable" }, remote.lookups)
        assertEquals("23" to FavoriteChange.MoveDown, remote.changes.last())

        // An album page that does not name the track sends nothing and reports the failure.
        repository.changeRankedFavorite(stationId, 3, FavoriteChange.Remove)
        assertEquals(2, remote.changes.size)
        assertEquals(FavoriteChangeStatus.Failed, repository.observeFavorites(stationId).first().change.status)
    }

    @Test
    fun `a removal the station answers but does not carry out is reported as failed`() = runTest {
        val tracks = listOf(
            FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"),
            FavoriteTrack(2, "Finale", "Album", "Composer", songId = "12"),
        )
        // The fake accepts the removal and keeps listing the track, as a station that ignored it would.
        val remote = FakeFavoriteTracksRemote(tracks, ranked = tracks)
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refresh(stationId)
        repository.refreshRanked(stationId)
        repository.changeFavorite(stationId, "11", FavoriteChange.Remove)
        assertEquals(FavoriteChangeStatus.Failed, repository.observeFavorites(stationId).first().change.status)

        // Once the lists read back no longer name it, the same removal is reported done.
        remote.tracks = tracks.drop(1)
        remote.ranked = tracks.drop(1)
        repository.changeFavorite(stationId, "11", FavoriteChange.Remove)
        assertEquals(FavoriteChangeStatus.Done, repository.observeFavorites(stationId).first().change.status)

        // A move has nothing to read back, so the station's answer stands.
        repository.changeFavorite(stationId, "12", FavoriteChange.MoveUp)
        assertEquals(FavoriteChangeStatus.Done, repository.observeFavorites(stationId).first().change.status)
    }

    @Test
    fun `a removal of a ranked row without a song number is checked by its album and title`() = runTest {
        val ranked = listOf(
            FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"),
            FavoriteTrack(2, "Unavailable", "Album Two", "Composer", albumId = "B000000002"),
        )
        val remote = FakeFavoriteTracksRemote(ranked = ranked).apply { albumSongIds = mapOf(("B000000002" to "Unavailable") to "22") }
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refreshRanked(stationId)

        // The ranked feed names no number for the row, so a station that ignored the removal still shows it by name.
        repository.changeRankedFavorite(stationId, 2, FavoriteChange.Remove)
        assertEquals(listOf("22" to FavoriteChange.Remove), remote.changes)
        assertEquals(FavoriteChangeStatus.Failed, repository.observeFavorites(stationId).first().change.status)

        remote.ranked = ranked.take(1)
        repository.changeRankedFavorite(stationId, 2, FavoriteChange.Remove)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(FavoriteChangeStatus.Done, change.status)
            assertEquals(listOf("Opening"), this.ranked.tracks.map { it.title })
        }
    }

    @Test
    fun `a removal no list could be read back for is reported as unconfirmed, and the rows shown stay usable`() = runTest {
        val tracks = listOf(
            FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"),
            FavoriteTrack(2, "Finale", "Album", "Composer", songId = "12"),
        )
        val remote = FakeFavoriteTracksRemote(tracks, ranked = tracks, rankedPages = 2)
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refresh(stationId)
        repository.refreshRanked(stationId)

        remote.failureAfterChange = java.io.IOException("offline")
        repository.changeFavorite(stationId, "11", FavoriteChange.Remove)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(FavoriteChangeStatus.Unconfirmed, change.status)
            assertEquals(RankedFavoritesStatus.Ready, ranked.status)
            assertEquals(2, ranked.tracks.size)
            assertEquals(2, this.tracks.size)
        }

        // A list that never named the track says nothing about it either.
        remote.failureAfterChange = null
        remote.failure = null
        val other = NetworkFavoriteTracksRepository(FakeFavoriteTracksRemote(tracks, ranked = tracks.drop(1)))
        other.refreshRanked(stationId)
        other.changeFavorite(stationId, "12", FavoriteChange.MoveUp)
        assertEquals(FavoriteChangeStatus.Done, other.observeFavorites(stationId).first().change.status)
    }

    @Test
    fun `a next page that failed stays marked when a change reads the same ranked pages again`() = runTest {
        val tracks = listOf(FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"))
        val remote = FakeFavoriteTracksRemote(tracks, ranked = tracks, rankedPages = 3)
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refreshRanked(stationId)
        remote.failure = java.io.IOException("offline")
        repository.loadMoreRanked(stationId)
        remote.failure = null

        repository.changeFavorite(stationId, "11", FavoriteChange.MoveDown)
        assertEquals(true, repository.observeFavorites(stationId).first().ranked.moreFailed)
        repository.refreshRanked(stationId)
        assertEquals(false, repository.observeFavorites(stationId).first().ranked.moreFailed)
    }

    @Test
    fun `a next page that cannot be read says so and keeps the pages already read`() = runTest {
        val tracks = listOf(FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"))
        val remote = FakeFavoriteTracksRemote(tracks, ranked = tracks, rankedPages = 3).apply { listPages = 3 }
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refresh(stationId)
        repository.refreshRanked(stationId)

        remote.failure = java.io.IOException("offline")
        repository.loadMore(stationId)
        repository.loadMoreRanked(stationId)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(true, moreFailed)
            assertEquals(false, loadingMore)
            assertEquals(true, hasMore)
            assertEquals(1, this.tracks.size)
            assertEquals(FavoriteTracksLoadStatus.Ready, status)
            assertEquals(true, ranked.moreFailed)
            assertEquals(1, ranked.tracks.size)
            assertEquals(true, ranked.hasMore)
        }

        remote.failure = null
        repository.loadMore(stationId)
        repository.loadMoreRanked(stationId)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(false, moreFailed)
            assertEquals(2, this.tracks.size)
            assertEquals(false, ranked.moreFailed)
            assertEquals(2, ranked.tracks.size)
        }
    }

    @Test
    fun `requestable only goes back to the earlier choice when its page cannot be read`() = runTest {
        val tracks = listOf(FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"))
        val remote = FakeFavoriteTracksRemote(tracks)
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refresh(stationId)

        remote.failure = java.io.IOException("offline")
        repository.setRequestableOnly(stationId, true)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(false, requestableOnly)
            assertEquals(FavoriteTracksLoadStatus.Error, status)
            assertEquals(1, this.tracks.size)
        }
    }

    @Test
    fun `a list page address carries the requestable choice and the page number`() {
        val list = "https://example.com/modules/Favorites/thelist.php?user2view=7"
        assertEquals(list, listPageUrl(list, 1, false))
        assertEquals("$list&page=3", listPageUrl(list, 3, false))
        assertEquals("$list&requestable=1", listPageUrl(list, 1, true))
        assertEquals("$list&requestable=1&page=2", listPageUrl(list, 2, true))
    }

    @Test
    fun `the full list loads a page at a time and keeps the pages already read when a removal re-reads it`() = runTest {
        val tracks = listOf(
            FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"),
            FavoriteTrack(2, "Finale", "Album", "Composer", songId = "12"),
        )
        val remote = FakeFavoriteTracksRemote(tracks).apply { listPages = 2 }
        val repository = NetworkFavoriteTracksRepository(remote)

        repository.refresh(stationId)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(2, this.tracks.size)
            assertEquals(1, loadedPages)
            assertEquals(true, hasMore)
            assertEquals(4, totalTracks)
        }

        repository.loadMore(stationId)
        with(repository.observeFavorites(stationId).first()) {
            assertEquals(4, this.tracks.size)
            assertEquals(2, loadedPages)
            assertEquals(false, hasMore)
            assertEquals(false, loadingMore)
        }
        repository.loadMore(stationId)
        assertEquals(4, repository.observeFavorites(stationId).first().tracks.size)

        remote.loads = 0
        repository.changeFavorite(stationId, "11-1", FavoriteChange.Remove)
        assertEquals(2, remote.loads)
        assertEquals(2, repository.observeFavorites(stationId).first().loadedPages)
    }

    @Test
    fun `a ranked track without a song number takes it from the same album and title in the full list`() = runTest {
        val full = listOf(FavoriteTrack(1, "Finale", "Album", "Composer", albumId = "B000000002", songId = "12"))
        val ranked = listOf(FavoriteTrack(1, "Finale", "Album", "Composer", albumId = "B000000002"))
        val repository = NetworkFavoriteTracksRepository(FakeFavoriteTracksRemote(full, ranked = ranked))

        repository.refresh(stationId)
        repository.refreshRanked(stationId)

        assertEquals("12", repository.observeFavorites(stationId).first().ranked.tracks.single().songId)
    }
}
