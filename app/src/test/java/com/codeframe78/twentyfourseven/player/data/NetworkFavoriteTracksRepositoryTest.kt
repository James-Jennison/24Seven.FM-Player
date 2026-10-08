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
    fun `a change is sent for a known track and the list is read again afterwards`() = runTest {
        val before = listOf(
            FavoriteTrack(1, "Opening", "Album", "Composer", songId = "11"),
            FavoriteTrack(2, "Finale", "Album", "Composer", songId = "12"),
        )
        val remote = FakeFavoriteTracksRemote(before)
        val repository = NetworkFavoriteTracksRepository(remote)
        repository.refresh(stationId)

        remote.tracks = before.reversed().mapIndexed { index, track -> track.copy(position = index + 1) }
        repository.changeFavorite(stationId, "12", FavoriteChange.MoveUp)

        assertEquals(listOf("12" to FavoriteChange.MoveUp), remote.changes)
        val state = repository.observeFavorites(stationId).first()
        assertEquals(FavoriteChangeStatus.Done, state.change.status)
        assertEquals(listOf("Finale", "Opening"), state.tracks.map { it.title })

        repository.changeFavorite(stationId, "99", FavoriteChange.Remove)
        assertEquals(1, remote.changes.size)
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
    ) : FavoriteTracksRemoteDataSource {
        val changes = mutableListOf<Pair<String, FavoriteChange>>()

        override suspend fun load(stationId: StationId): List<FavoriteTrack> {
            failure?.let { throw it }
            return tracks
        }

        override suspend fun change(stationId: StationId, songId: String, change: FavoriteChange) {
            changeFailure?.let { throw it }
            changes += songId to change
        }
    }
}
