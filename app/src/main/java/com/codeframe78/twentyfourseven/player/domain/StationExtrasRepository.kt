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
)

enum class MemberProfileStatus { Closed, Loading, Ready, NotFound, Error }

data class MemberProfileState(
    val status: MemberProfileStatus = MemberProfileStatus.Closed,
    val requestedName: String = "",
    val profile: MemberProfile? = null,
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
    val history: PlayedHistoryState = PlayedHistoryState(),
    val news: StationNewsState = StationNewsState(),
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
 * Public, read-only station information that sits beside the core player: member profile cards, the played-history
 * archive, and station news. Everything is fetched on request and held in memory only.
 */
interface StationExtrasRepository {
    fun observeExtras(stationId: StationId): Flow<StationExtrasState>
    suspend fun openProfile(stationId: StationId, username: String)
    suspend fun closeProfile(stationId: StationId)
    suspend fun loadHistory(stationId: StationId, date: LocalDate, startHour: Int)
    suspend fun closeHistory(stationId: StationId)
    suspend fun refreshNews(stationId: StationId)
}

object UnavailableStationExtrasRepository : StationExtrasRepository {
    override fun observeExtras(stationId: StationId): Flow<StationExtrasState> = flowOf(StationExtrasState(stationId))
    override suspend fun openProfile(stationId: StationId, username: String) = Unit
    override suspend fun closeProfile(stationId: StationId) = Unit
    override suspend fun loadHistory(stationId: StationId, date: LocalDate, startHour: Int) = Unit
    override suspend fun closeHistory(stationId: StationId) = Unit
    override suspend fun refreshNews(stationId: StationId) = Unit
}
