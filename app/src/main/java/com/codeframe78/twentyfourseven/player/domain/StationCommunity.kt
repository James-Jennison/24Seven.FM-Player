package com.codeframe78.twentyfourseven.player.domain

import java.time.MonthDay

/** One album the station added to its playlist, as listed on the station's Recently Added page. */
data class RecentlyAddedAlbum(
    val albumId: String,
    val title: String,
    val coverUrl: String?,
)

/** A batch of albums added on one day, in the order the station lists them. */
data class RecentlyAddedBatch(
    val dateLabel: String,
    val author: String?,
    val albums: List<RecentlyAddedAlbum>,
)

enum class RecentlyAddedStatus { Idle, Loading, Ready, Error }

data class RecentlyAddedState(
    val status: RecentlyAddedStatus = RecentlyAddedStatus.Idle,
    val batches: List<RecentlyAddedBatch> = emptyList(),
)

/** A member review of an album, as the station shows it on the album's page. */
data class AlbumReview(
    val title: String,
    val author: String?,
    val dateLabel: String?,
    /** The reviewer's rating out of 5, when the station shows one, e.g. "5.0". */
    val rating: String?,
    val body: String,
    val helpfulLabel: String?,
)

enum class AlbumReviewsStatus { Closed, Loading, Ready, Error }

/** The reviews the station shows for one album. The Player reads them only; reviews are written on the station. */
data class AlbumReviewsState(
    val albumId: String? = null,
    val status: AlbumReviewsStatus = AlbumReviewsStatus.Closed,
    val reviews: List<AlbumReview> = emptyList(),
)

/** A member who is on the station right now, from the station's Online Now block. */
data class OnlineMember(
    val username: String,
    val memberNumber: String?,
    val countryName: String?,
)

/** One row of the station's members list. */
data class MemberSummary(
    val username: String,
    val memberNumber: String?,
    val location: String?,
    val countryName: String?,
    val rankTitle: String?,
    val joinedLabel: String?,
    val lastPostLabel: String?,
    val posts: Int?,
    val isOnline: Boolean,
    val isVip: Boolean,
)

enum class MemberListSort(val query: String, val label: String) {
    Newest("joined", "Newest members"),
    Name("username", "Name"),
    Posts("posts", "Most posts"),
}

enum class MembersStatus { Closed, Loading, Ready, Error }

data class MembersState(
    val status: MembersStatus = MembersStatus.Closed,
    val online: List<OnlineMember> = emptyList(),
    val visitors: Int? = null,
    val totalMembers: Int? = null,
    val query: String = "",
    val sort: MemberListSort = MemberListSort.Newest,
    val members: List<MemberSummary> = emptyList(),
    /** The offset of the next page the station would serve, or null when the list is complete. */
    val nextStart: Int? = null,
)

/** One line of the station's calendar: a theme day, a composer's birthday, or a member's birthday. */
data class CalendarEntry(
    val label: String,
    val kind: CalendarEntryKind,
    /** The member's name when the entry is a member birthday, so it can open the profile card. */
    val username: String? = null,
)

enum class CalendarEntryKind { Event, Composer, Member }

data class CalendarDay(
    val day: MonthDay,
    val label: String,
    val entries: List<CalendarEntry>,
)

enum class CalendarStatus { Idle, Loading, Ready, Error }

data class CalendarState(
    val status: CalendarStatus = CalendarStatus.Idle,
    val days: List<CalendarDay> = emptyList(),
)

/** The fields of the station's Edit Profile form that the Player lets a member change. */
data class EditableProfile(
    val realName: String = "",
    val location: String = "",
    /** The station's flag file name, e.g. "us.gif"; "blank.gif" when no country is chosen. */
    val flag: String = "blank.gif",
    val occupation: String = "",
    val interests: String = "",
    val website: String = "",
    val signature: String = "",
    val bio: String = "",
    val newsletter: Boolean = false,
    val hideOnlineStatus: Boolean = false,
)

data class FlagOption(val value: String, val label: String)

/** The station's whole Edit Profile form: every field the station expects back, plus the ones the Player edits. */
data class ProfileEditForm(
    val actionPath: String,
    val fields: List<Pair<String, String>>,
    val profile: EditableProfile,
    val flags: List<FlagOption>,
)

enum class ProfileEditStatus { Closed, Loading, Ready, Saving, Saved, SignInRequired, Error }

data class ProfileEditState(
    val status: ProfileEditStatus = ProfileEditStatus.Closed,
    val form: ProfileEditForm? = null,
    val message: String? = null,
)
