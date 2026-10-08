package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AlbumReview
import com.codeframe78.twentyfourseven.player.domain.AlbumReviewSendStatus
import com.codeframe78.twentyfourseven.player.domain.AlbumReviewsStatus
import com.codeframe78.twentyfourseven.player.domain.CalendarDay
import com.codeframe78.twentyfourseven.player.domain.CalendarEntry
import com.codeframe78.twentyfourseven.player.domain.CalendarEntryKind
import com.codeframe78.twentyfourseven.player.domain.CalendarStatus
import com.codeframe78.twentyfourseven.player.domain.EditableProfile
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.FlagOption
import com.codeframe78.twentyfourseven.player.domain.MemberListSort
import com.codeframe78.twentyfourseven.player.domain.MemberSummary
import com.codeframe78.twentyfourseven.player.domain.MembersStatus
import com.codeframe78.twentyfourseven.player.domain.OnlineMember
import com.codeframe78.twentyfourseven.player.domain.ProfileEditForm
import com.codeframe78.twentyfourseven.player.domain.ProfileEditStatus
import com.codeframe78.twentyfourseven.player.domain.RecentlyAddedAlbum
import com.codeframe78.twentyfourseven.player.domain.RecentlyAddedBatch
import com.codeframe78.twentyfourseven.player.domain.RecentlyAddedStatus
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.MonthDay

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

    @Test
    fun `recently added and the calendar load once and report errors`() = runTest {
        val repository = NetworkStationExtrasRepository(FakeRemote())
        repository.refreshRecentlyAdded(station)
        repository.refreshCalendar(station)
        with(repository.observeExtras(station).first()) {
            assertEquals(RecentlyAddedStatus.Ready, recentlyAdded.status)
            assertEquals("B000000001", recentlyAdded.batches.single().albums.single().albumId)
            assertEquals(CalendarStatus.Ready, calendar.status)
            assertEquals(MonthDay.of(1, 1), calendar.days.single().day)
        }

        val offline = NetworkStationExtrasRepository(FakeRemote(failure = IOException("offline")))
        offline.refreshRecentlyAdded(station)
        offline.refreshCalendar(station)
        with(offline.observeExtras(station).first()) {
            assertEquals(RecentlyAddedStatus.Error, recentlyAdded.status)
            assertEquals(CalendarStatus.Error, calendar.status)
        }
    }

    @Test
    fun `album reviews open for one album and a sent review is confirmed from the album page`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkStationExtrasRepository(remote)

        repository.openAlbumReviews(station, "B000000001")
        with(repository.observeExtras(station).first().albumReviews) {
            assertEquals(AlbumReviewsStatus.Ready, status)
            assertEquals("B000000001", albumId)
            assertTrue(canWrite)
            assertEquals(listOf("Great Score"), reviews.map { it.title })
        }

        repository.submitAlbumReview(station, "  My take ", "Loved it", "4.5")
        assertEquals(listOf(Triple("My take", "Loved it", "4.5")), remote.sentReviews)
        with(repository.observeExtras(station).first().albumReviews) {
            assertEquals(AlbumReviewSendStatus.Sent, sendStatus)
            assertEquals(listOf("Great Score", "My take"), reviews.map { it.title })
            assertFalse(canWrite)
        }
    }

    @Test
    fun `a review is not sent without a title, body, or the station's offer to write one`() = runTest {
        val remote = FakeRemote(canWrite = false)
        val repository = NetworkStationExtrasRepository(remote)
        repository.openAlbumReviews(station, "B000000001")

        repository.submitAlbumReview(station, "Title", "Body", "5")
        assertTrue(remote.sentReviews.isEmpty())

        val offered = FakeRemote()
        val writable = NetworkStationExtrasRepository(offered)
        writable.openAlbumReviews(station, "B000000001")
        writable.submitAlbumReview(station, " ", "Body", "5")
        writable.submitAlbumReview(station, "Title", "", "5")
        assertTrue(offered.sentReviews.isEmpty())
        assertEquals(AlbumReviewSendStatus.Idle, writable.observeExtras(station).first().albumReviews.sendStatus)
    }

    @Test
    fun `a review the station does not show yet, a lost session, and a withdrawn form are each reported`() = runTest {
        val hidden = FakeRemote(showsSentReview = false)
        val repository = NetworkStationExtrasRepository(hidden)
        repository.openAlbumReviews(station, "B000000001")
        repository.submitAlbumReview(station, "Title", "Body", "5")
        assertEquals(AlbumReviewSendStatus.Unconfirmed, repository.observeExtras(station).first().albumReviews.sendStatus)

        val signedOut = FakeRemote(sendFailure = FavoritesAuthenticationRequiredException())
        val lost = NetworkStationExtrasRepository(signedOut)
        lost.openAlbumReviews(station, "B000000001")
        lost.submitAlbumReview(station, "Title", "Body", "5")
        assertEquals(AlbumReviewSendStatus.SignInRequired, lost.observeExtras(station).first().albumReviews.sendStatus)

        val withdrawn = NetworkStationExtrasRepository(FakeRemote(sendFailure = StationFormUnavailableException()))
        withdrawn.openAlbumReviews(station, "B000000001")
        withdrawn.submitAlbumReview(station, "Title", "Body", "5")
        with(withdrawn.observeExtras(station).first().albumReviews) {
            assertEquals(AlbumReviewSendStatus.Rejected, sendStatus)
            assertFalse(canWrite)
        }
    }

    @Test
    fun `members open with the online block, search again, page on, and close`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkStationExtrasRepository(remote)

        repository.openMembers(station)
        with(repository.observeExtras(station).first().members) {
            assertEquals(MembersStatus.Ready, status)
            assertEquals(listOf("Listener"), online.map { it.username })
            assertEquals(760, visitors)
            assertEquals(listOf("Member One", "Member Two"), members.map { it.username })
            assertEquals(2, nextStart)
        }
        assertEquals(listOf(Triple("", MemberListSort.Newest, 0)), remote.memberRequests)

        repository.loadMoreMembers(station)
        with(repository.observeExtras(station).first().members) {
            // The second page repeats one row, which is not shown twice.
            assertEquals(listOf("Member One", "Member Two", "Member Three"), members.map { it.username })
            assertNull(nextStart)
        }

        repository.searchMembers(station, " two ", MemberListSort.Name)
        with(repository.observeExtras(station).first().members) {
            assertEquals("two", query)
            assertEquals(MemberListSort.Name, sort)
            assertEquals(listOf("Member One", "Member Two"), members.map { it.username })
        }
        assertEquals(Triple("two", MemberListSort.Name, 0), remote.memberRequests.last())

        repository.closeMembers(station)
        assertEquals(MembersStatus.Closed, repository.observeExtras(station).first().members.status)
    }

    @Test
    fun `the profile editor loads the form, saves it, and reports what the station kept`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkStationExtrasRepository(remote)

        repository.openProfileEditor(station)
        with(repository.observeExtras(station).first().profileEdit) {
            assertEquals(ProfileEditStatus.Ready, status)
            assertEquals("Pat Listener", form?.profile?.realName)
        }

        val edited = EditableProfile(realName = "Pat L.", location = "Springfield", flag = "us.gif", newsletter = true)
        repository.saveProfile(station, edited)
        assertEquals(listOf(edited), remote.savedProfiles)
        with(repository.observeExtras(station).first().profileEdit) {
            assertEquals(ProfileEditStatus.Saved, status)
            assertEquals("Pat L.", form?.profile?.realName)
        }

        val stubborn = FakeRemote(keepsEdits = false)
        val ignored = NetworkStationExtrasRepository(stubborn)
        ignored.openProfileEditor(station)
        ignored.saveProfile(station, edited)
        with(ignored.observeExtras(station).first().profileEdit) {
            assertEquals(ProfileEditStatus.Ready, status)
            assertEquals("Pat Listener", form?.profile?.realName)
            assertEquals("The station kept the profile shown here; not every change was accepted.", message)
        }

        val signedOut = NetworkStationExtrasRepository(FakeRemote(failure = FavoritesAuthenticationRequiredException()))
        signedOut.openProfileEditor(station)
        assertEquals(ProfileEditStatus.SignInRequired, signedOut.observeExtras(station).first().profileEdit.status)
    }

    private class FakeRemote(
        private val failure: Exception? = null,
        private val canWrite: Boolean = true,
        private val showsSentReview: Boolean = true,
        private val sendFailure: Exception? = null,
        private val keepsEdits: Boolean = true,
    ) : ExtrasRemoteDataSource {
        val historyRequests = mutableListOf<Pair<LocalDate, Int>>()
        val sentReviews = mutableListOf<Triple<String, String, String>>()
        val memberRequests = mutableListOf<Triple<String, MemberListSort, Int>>()
        val savedProfiles = mutableListOf<EditableProfile>()
        private var kept = EditableProfile(realName = "Pat Listener", location = "Springfield", flag = "us.gif")

        override suspend fun recentlyAdded(stationId: StationId): List<RecentlyAddedBatch> {
            failure?.let { throw it }
            return listOf(RecentlyAddedBatch("August 27, 2026", "Editor", listOf(RecentlyAddedAlbum("B000000001", "Opening", null))))
        }

        override suspend fun albumReviews(stationId: StationId, albumId: String): AlbumReviewsPage {
            failure?.let { throw it }
            return AlbumReviewsPage(listOf(AlbumReview("Great Score", "Reviewer", "23 Sep 2022", "4.5", "Warm themes.", null)), canWrite)
        }

        override suspend fun submitAlbumReview(
            stationId: StationId,
            albumId: String,
            title: String,
            body: String,
            rating: String,
        ): AlbumReviewsPage {
            sendFailure?.let { throw it }
            sentReviews += Triple(title, body, rating)
            val page = albumReviews(stationId, albumId)
            return if (showsSentReview) {
                page.copy(reviews = page.reviews + AlbumReview(title, "Me", "today", rating, body, null), canWrite = false)
            } else {
                page
            }
        }

        override suspend fun onlineNow(stationId: StationId): OnlineBlock {
            failure?.let { throw it }
            return OnlineBlock(listOf(OnlineMember("Listener", "101", "Canada")), visitors = 760, totalMembers = 49_767)
        }

        override suspend fun members(stationId: StationId, query: String, sort: MemberListSort, start: Int): MembersPage {
            failure?.let { throw it }
            memberRequests += Triple(query, sort, start)
            fun member(name: String) = MemberSummary(name, null, null, null, null, null, null, 1, isOnline = false, isVip = false)
            return if (start == 0) {
                MembersPage(listOf(member("Member One"), member("Member Two")), nextStart = 2)
            } else {
                MembersPage(listOf(member("Member Two"), member("Member Three")), nextStart = null)
            }
        }

        override suspend fun calendar(stationId: StationId): List<CalendarDay> {
            failure?.let { throw it }
            return listOf(CalendarDay(MonthDay.of(1, 1), "January 1", listOf(CalendarEntry("New Year's Day", CalendarEntryKind.Event))))
        }

        override suspend fun profileEditForm(stationId: StationId): ProfileEditForm {
            failure?.let { throw it }
            return ProfileEditForm("/modules.php?name=Your_Account", listOf("op" to "saveuser"), kept, listOf(FlagOption("us.gif", "United States")))
        }

        override suspend fun saveProfile(stationId: StationId, form: ProfileEditForm, edited: EditableProfile): ProfileEditForm {
            failure?.let { throw it }
            savedProfiles += edited
            if (keepsEdits) kept = edited
            return profileEditForm(stationId)
        }

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
