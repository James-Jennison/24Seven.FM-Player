package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.MemberFavoritesStatus
import com.codeframe78.twentyfourseven.player.domain.MemberProfile
import com.codeframe78.twentyfourseven.player.domain.MemberProfileStatus
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationNewsStatus
import com.codeframe78.twentyfourseven.player.domain.StationNewsStory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class NetworkStationExtrasRepositoryTest {
    private val station = StationId("sst")

    @Test
    fun `a profile opens, reports an unknown member, and closes`() = runTest {
        val repository = NetworkStationExtrasRepository(FakeRemote())

        repository.openProfile(station, " Listener ")
        with(repository.observeExtras(station).first().profile) {
            assertEquals(MemberProfileStatus.Ready, status)
            assertEquals("Listener", profile?.username)
        }

        repository.openProfile(station, "Nobody")
        assertEquals(MemberProfileStatus.NotFound, repository.observeExtras(station).first().profile.status)

        repository.closeProfile(station)
        with(repository.observeExtras(station).first().profile) {
            assertEquals(MemberProfileStatus.Closed, status)
            assertNull(profile)
        }
    }

    @Test
    fun `a member's favorites open, report a needed sign-in or a failure, and close`() = runTest {
        val repository = NetworkStationExtrasRepository(FakeRemote())

        repository.openMemberFavorites(station, "Listener", "4821")
        with(repository.observeExtras(station).first().memberFavorites) {
            assertEquals(MemberFavoritesStatus.Ready, status)
            assertEquals("Listener", memberName)
            assertEquals("Opening", tracks.single().title)
        }

        repository.closeMemberFavorites(station)
        with(repository.observeExtras(station).first().memberFavorites) {
            assertEquals(MemberFavoritesStatus.Closed, status)
            assertEquals(emptyList<FavoriteTrack>(), tracks)
        }

        val signedOut = NetworkStationExtrasRepository(FakeRemote(failure = FavoritesAuthenticationRequiredException()))
        signedOut.openMemberFavorites(station, "Listener", "4821")
        assertEquals(MemberFavoritesStatus.SignInRequired, signedOut.observeExtras(station).first().memberFavorites.status)

        val offline = NetworkStationExtrasRepository(FakeRemote(failure = IOException("offline")))
        offline.openMemberFavorites(station, "Listener", "4821")
        with(offline.observeExtras(station).first().memberFavorites) {
            assertEquals(MemberFavoritesStatus.Error, status)
            assertEquals("Listener", memberName)
        }
    }

    @Test
    fun `history loads the two-hour block that contains the requested hour`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkStationExtrasRepository(remote)
        val date = LocalDate.of(2026, 10, 2)

        repository.loadHistory(station, date, 3)

        with(repository.observeExtras(station).first().history) {
            assertEquals(PlayedHistoryStatus.Ready, status)
            assertEquals(date, this.date)
            assertEquals(2, startHour)
            assertEquals("Opening", entries.single().title)
        }
        assertEquals(listOf(date to 2), remote.historyRequests)
    }

    @Test
    fun `failures are reported per feature without disturbing the others`() = runTest {
        val repository = NetworkStationExtrasRepository(FakeRemote(failure = IOException("offline")))

        repository.openProfile(station, "Listener")
        repository.loadHistory(station, LocalDate.of(2026, 10, 2), 0)
        repository.refreshNews(station)

        val state = repository.observeExtras(station).first()
        assertEquals(MemberProfileStatus.Error, state.profile.status)
        assertEquals(PlayedHistoryStatus.Error, state.history.status)
        assertEquals(StationNewsStatus.Error, state.news.status)
    }

    @Test
    fun `news loads the station's stories`() = runTest {
        val repository = NetworkStationExtrasRepository(FakeRemote())

        repository.refreshNews(station)

        with(repository.observeExtras(station).first().news) {
            assertEquals(StationNewsStatus.Ready, status)
            assertEquals("Playlist update", stories.single().title)
        }
    }

    private class FakeRemote(private val failure: Exception? = null) : ExtrasRemoteDataSource {
        val historyRequests = mutableListOf<Pair<LocalDate, Int>>()

        override suspend fun profile(stationId: StationId, username: String): MemberProfile? {
            failure?.let { throw it }
            return if (username == "Listener") {
                MemberProfile("Listener", "Apr 20, 2002", null, "Captain", null, isOnline = false, avatarUrl = null, badges = emptyList())
            } else {
                null
            }
        }

        override suspend fun memberFavorites(stationId: StationId, memberNumber: String): List<FavoriteTrack> {
            failure?.let { throw it }
            return listOf(FavoriteTrack(1, "Opening", "Album Two", "Composer Two"))
        }

        override suspend fun history(stationId: StationId, date: LocalDate, startHour: Int): List<PlayedHistoryEntry> {
            failure?.let { throw it }
            historyRequests += date to startHour
            return listOf(PlayedHistoryEntry("03:48:56", "04:53", "Opening", "Composer Two", "Album Two", null, null, null, null))
        }

        override suspend fun news(stationId: StationId): List<StationNewsStory> {
            failure?.let { throw it }
            return listOf(StationNewsStory("1", "Playlist update", "August 27th, 2026", "Editor", "Body", emptyList()))
        }
    }
}
