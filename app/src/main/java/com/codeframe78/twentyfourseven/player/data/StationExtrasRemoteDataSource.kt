package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.MemberProfile
import com.codeframe78.twentyfourseven.player.domain.PLAYED_HISTORY_BLOCK_HOURS
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationNewsStory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

internal interface ExtrasRemoteDataSource {
    /** The member's public profile card, or null when the station knows no such member. */
    suspend fun profile(stationId: StationId, username: String): MemberProfile?

    /** The public favorites list of the member with this number. Needs the listener's own station session. */
    suspend fun memberFavorites(stationId: StationId, memberNumber: String): List<FavoriteTrack>
    suspend fun history(stationId: StationId, date: LocalDate, startHour: Int): List<PlayedHistoryEntry>
    suspend fun news(stationId: StationId): List<StationNewsStory>
}

/** Reads station information with plain GET requests. Nothing here changes anything on the station. */
internal class StationExtrasRemoteDataSource(
    private val pages: StationPages,
    private val parser: StationExtrasParser = StationExtrasParser(),
    private val favoritesParser: FavoriteTracksPageParser = FavoriteTracksPageParser(),
) : ExtrasRemoteDataSource {
    override suspend fun profile(stationId: StationId, username: String): MemberProfile? =
        withContext(Dispatchers.IO) {
            val name = username.trim()
            require(name.isNotEmpty() && name.length <= MAX_NAME_CHARACTERS)
            val response = try {
                pages.get(stationId, PROFILE_PATH + encode(name), PROFILE_LIMIT)
            } catch (failure: StationHttpException) {
                // The station answers an unknown member with 404.
                if (failure.status == HTTP_NOT_FOUND) return@withContext null
                throw failure
            }
            parser.parseProfile(response.body, pages.origin(stationId))
                ?.takeIf { it.username.equals(name, ignoreCase = true) }
        }

    override suspend fun memberFavorites(stationId: StationId, memberNumber: String): List<FavoriteTrack> =
        withContext(Dispatchers.IO) {
            require(memberNumber.matches(MEMBER_NUMBER))
            // The stations show a favorites list to signed-in members only.
            if (!pages.hasSession(stationId)) throw FavoritesAuthenticationRequiredException()
            val origin = pages.origin(stationId)
            val discovery = pages.get(stationId, MEMBER_FAVORITES_PATH + memberNumber, FAVORITES_PAGE_LIMIT)
            val listUrl = try {
                favoritesParser.parseListUrl(discovery.body, origin)
            } catch (failure: FavoritesAuthenticationRequiredException) {
                pages.expireSession(stationId)
                throw failure
            }
            // The station must point at the list of the member that was asked for, never at somebody else's.
            if (favoritesParser.listMemberNumber(listUrl) != memberNumber) {
                throw IOException("Favorites list belongs to a different member")
            }
            val list = URI(listUrl)
            val response = pages.get(stationId, "${list.rawPath}?${list.rawQuery}", FAVORITES_LIST_LIMIT)
            favoritesParser.parseTracks(response.body, origin)
        }

    override suspend fun history(stationId: StationId, date: LocalDate, startHour: Int): List<PlayedHistoryEntry> =
        withContext(Dispatchers.IO) {
            require(startHour in 0..(24 - PLAYED_HISTORY_BLOCK_HOURS))
            // The station returns the hour before and the hour after the requested time.
            val middle = "%02d:00:00".format(startHour + 1)
            val response = pages.get(stationId, "$HISTORY_PATH?histdate=$date&histtime=$middle", HISTORY_LIMIT)
            parser.parseHistory(response.body, pages.origin(stationId))
        }

    override suspend fun news(stationId: StationId): List<StationNewsStory> = withContext(Dispatchers.IO) {
        parser.parseNews(pages.get(stationId, NEWS_PATH, NEWS_LIMIT).body, pages.origin(stationId))
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val PROFILE_PATH = "/modules/Your_Profile/hoverCardAJAX.php?username="
        const val HISTORY_PATH = "/modules/Played_History/HistTableAJAX.php"
        const val NEWS_PATH = "/modules.php?name=News"
        const val MEMBER_FAVORITES_PATH = "/modules.php?name=Favorites&user2view="
        const val FAVORITES_PAGE_LIMIT = 512_000
        const val FAVORITES_LIST_LIMIT = 5_000_000
        val MEMBER_NUMBER = Regex("[0-9]{1,10}")
        const val PROFILE_LIMIT = 50_000
        const val HISTORY_LIMIT = 500_000
        const val NEWS_LIMIT = 1_000_000
        const val MAX_NAME_CHARACTERS = 60
        const val HTTP_NOT_FOUND = 404
    }
}
