package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** What a station's public profile card says about one member. */
data class MemberProfile(
    val username: String,
    val memberSince: String?,
    val location: String?,
    val rankTitle: String?,
    val membership: String?,
    val isOnline: Boolean,
    val avatarUrl: String?,
    val badges: List<String>,
    /** The station's number for this member, which its other pages use to name them. */
    val memberNumber: String? = null,
    /** The label of the badge a member wears when they have made their favorites list public. */
    val publicFavoritesBadge: String? = null,
    /** The staff role the station colours this member's name by. */
    val role: ChatRole = ChatRole.Member,
    val forumPosts: Int? = null,
    val flagUrl: String? = null,
    /** The station's insignia picture for the rank. */
    val rankImageUrl: String? = null,
    /** The symbol the station draws for a badge, by badge label. */
    val badgeSymbols: Map<String, String> = emptyMap(),
    /** The website the member lists on their card; it can be anywhere on the web. */
    val websiteUrl: String? = null,
    /** The station's own page for emailing this member, which the station shows to signed-in members. */
    val emailPageUrl: String? = null,
)

enum class MemberProfileStatus { Closed, Loading, Ready, NotFound, Error }

data class MemberProfileState(
    val status: MemberProfileStatus = MemberProfileStatus.Closed,
    val requestedName: String = "",
    val profile: MemberProfile? = null,
)

enum class MemberFavoritesStatus { Closed, Loading, Ready, SignInRequired, Error }

/** Another member's public favorites list, as the station shows it to a signed-in member. */
data class MemberFavoritesState(
    val status: MemberFavoritesStatus = MemberFavoritesStatus.Closed,
    val memberName: String = "",
    val tracks: List<FavoriteTrack> = emptyList(),
)

/** One track from the station's played-history archive. Times are on the station's own clock. */
data class PlayedHistoryEntry(
    val playedAtLabel: String,
    val lengthLabel: String?,
    val title: String,
    val artistName: String?,
    val albumTitle: String?,
    val albumId: String?,
    val artworkUrl: String?,
    val requesterName: String?,
    val requestMessage: String?,
)

enum class PlayedHistoryStatus { Closed, Loading, Ready, Error }

data class PlayedHistoryState(
    val status: PlayedHistoryStatus = PlayedHistoryStatus.Closed,
    val date: LocalDate? = null,
    /** The first hour of the two-hour block being shown, on the station's clock. */
    val startHour: Int = 0,
    val entries: List<PlayedHistoryEntry> = emptyList(),
)

data class StationNewsStory(
    val id: String,
    val title: String,
    val publishedLabel: String?,
    val author: String?,
    val body: String,
    val coverUrls: List<String>,
)

enum class StationNewsStatus { Idle, Loading, Ready, Error }

data class StationNewsState(
    val status: StationNewsStatus = StationNewsStatus.Idle,
    val stories: List<StationNewsStory> = emptyList(),
)

data class StationExtrasState(
    val stationId: StationId,
    val profile: MemberProfileState = MemberProfileState(),
    val memberFavorites: MemberFavoritesState = MemberFavoritesState(),
    val history: PlayedHistoryState = PlayedHistoryState(),
    val news: StationNewsState = StationNewsState(),
    val recentlyAdded: RecentlyAddedState = RecentlyAddedState(),
    val albumReviews: AlbumReviewsState = AlbumReviewsState(),
    val members: MembersState = MembersState(),
    val calendar: CalendarState = CalendarState(),
    val profileEdit: ProfileEditState = ProfileEditState(),
)

/** The stations answer a history request with the two hours around the requested time. */
const val PLAYED_HISTORY_BLOCK_HOURS = 2

/** How far back the stations' history archive reaches. */
const val PLAYED_HISTORY_ARCHIVE_DAYS = 365L

/** The stations keep their played history on United States Eastern time. */
val STATION_CLOCK_ZONE: ZoneId = ZoneId.of("America/New_York")

/** The archive date and two-hour block that contain [now] on the station's clock. */
fun currentPlayedHistoryBlock(now: ZonedDateTime = ZonedDateTime.now(STATION_CLOCK_ZONE)): Pair<LocalDate, Int> {
    val stationNow = now.withZoneSameInstant(STATION_CLOCK_ZONE)
    return stationNow.toLocalDate() to stationNow.hour - stationNow.hour % PLAYED_HISTORY_BLOCK_HOURS
}

/**
 * Station information that sits beside the core player: member profile cards, a member's public favorites list, the
 * played-history archive, station news, Recently Added, album reviews, the members list, the calendar, and the
 * member's own Edit Profile form. Everything is fetched on request and held in memory only. The only writes are a
 * review and a profile edit, each sent once after an explicit confirmation.
 */
interface StationExtrasRepository {
    fun observeExtras(stationId: StationId): Flow<StationExtrasState>
    suspend fun openProfile(stationId: StationId, username: String)
    suspend fun closeProfile(stationId: StationId)
    suspend fun openMemberFavorites(stationId: StationId, memberName: String, memberNumber: String)
    suspend fun closeMemberFavorites(stationId: StationId)
    suspend fun loadHistory(stationId: StationId, date: LocalDate, startHour: Int)
    suspend fun closeHistory(stationId: StationId)
    suspend fun refreshNews(stationId: StationId)
    suspend fun refreshRecentlyAdded(stationId: StationId)

    /** Loads the reviews on this album's page; they sit beside the album browser until [closeAlbumReviews]. */
    suspend fun openAlbumReviews(stationId: StationId, albumId: String)
    suspend fun closeAlbumReviews(stationId: StationId)

    /** Sends one review for the album whose reviews are open. The station's form is read again first. */
    suspend fun submitAlbumReview(stationId: StationId, title: String, body: String, rating: String)

    /** Loads the Online Now block and the first page of the members list with the current query and sort. */
    suspend fun openMembers(stationId: StationId)
    suspend fun searchMembers(stationId: StationId, query: String, sort: MemberListSort)
    suspend fun loadMoreMembers(stationId: StationId)
    suspend fun closeMembers(stationId: StationId)
    suspend fun refreshCalendar(stationId: StationId)

    /** Loads the signed-in member's own Edit Profile form. Needs the station session. */
    suspend fun openProfileEditor(stationId: StationId)

    /** Sends the whole form back with these fields changed, then reads it again to confirm what the station kept. */
    suspend fun saveProfile(stationId: StationId, profile: EditableProfile)
    suspend fun closeProfileEditor(stationId: StationId)
}

object UnavailableStationExtrasRepository : StationExtrasRepository {
    override fun observeExtras(stationId: StationId): Flow<StationExtrasState> = flowOf(StationExtrasState(stationId))
    override suspend fun openProfile(stationId: StationId, username: String) = Unit
    override suspend fun closeProfile(stationId: StationId) = Unit
    override suspend fun openMemberFavorites(stationId: StationId, memberName: String, memberNumber: String) = Unit
    override suspend fun closeMemberFavorites(stationId: StationId) = Unit
    override suspend fun loadHistory(stationId: StationId, date: LocalDate, startHour: Int) = Unit
    override suspend fun closeHistory(stationId: StationId) = Unit
    override suspend fun refreshNews(stationId: StationId) = Unit
    override suspend fun refreshRecentlyAdded(stationId: StationId) = Unit
    override suspend fun openAlbumReviews(stationId: StationId, albumId: String) = Unit
    override suspend fun closeAlbumReviews(stationId: StationId) = Unit
    override suspend fun submitAlbumReview(stationId: StationId, title: String, body: String, rating: String) = Unit
    override suspend fun openMembers(stationId: StationId) = Unit
    override suspend fun searchMembers(stationId: StationId, query: String, sort: MemberListSort) = Unit
    override suspend fun loadMoreMembers(stationId: StationId) = Unit
    override suspend fun closeMembers(stationId: StationId) = Unit
    override suspend fun refreshCalendar(stationId: StationId) = Unit
    override suspend fun openProfileEditor(stationId: StationId) = Unit
    override suspend fun saveProfile(stationId: StationId, profile: EditableProfile) = Unit
    override suspend fun closeProfileEditor(stationId: StationId) = Unit
}
