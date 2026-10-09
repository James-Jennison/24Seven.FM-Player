package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteChange
import com.codeframe78.twentyfourseven.player.domain.FavoriteChangeStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksLoadStatus
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
        private val failure: Throwable? = null,
        private val changeFailure: Throwable? = null,
        var ranked: List<FavoriteTrack> = emptyList(),
        private val rankedPages: Int = 1,
    ) : FavoriteTracksRemoteDataSource {
        val changes = mutableListOf<Pair<String, FavoriteChange>>()
        var loads = 0

        override suspend fun load(stationId: StationId): List<FavoriteTrack> {
            failure?.let { throw it }
            loads++
            return tracks
        }

        override suspend fun loadRanked(stationId: StationId, page: Int): RankedFavoritesPage {
            failure?.let { throw it }
            return RankedFavoritesPage(ranked, nextPage = if (page < rankedPages) page + 1 else null)
        }

        override suspend fun change(stationId: StationId, songId: String, change: FavoriteChange) {
            changeFailure?.let { throw it }
            changes += songId to change
        }
    }
}
