package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.CalendarDay
import com.codeframe78.twentyfourseven.player.domain.EditableProfile
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.MemberListSort
import com.codeframe78.twentyfourseven.player.domain.MemberProfile
import com.codeframe78.twentyfourseven.player.domain.PLAYED_HISTORY_BLOCK_HOURS
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.ProfileEditForm
import com.codeframe78.twentyfourseven.player.domain.RecentlyAddedBatch
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
    suspend fun recentlyAdded(stationId: StationId): List<RecentlyAddedBatch>
    suspend fun albumReviews(stationId: StationId, albumId: String): AlbumReviewsPage

    /**
     * Sends one review through the station's own form, which is read first so the post goes where the station says
     * and carries a rating it offers. Returns the album page as it is after the post.
     */
    suspend fun submitAlbumReview(
        stationId: StationId,
        albumId: String,
        title: String,
        body: String,
        rating: String,
    ): AlbumReviewsPage
    suspend fun onlineNow(stationId: StationId): OnlineBlock
    suspend fun members(stationId: StationId, query: String, sort: MemberListSort, start: Int): MembersPage
    suspend fun calendar(stationId: StationId): List<CalendarDay>

    /** The member's own Edit Profile form. Needs the listener's own station session. */
    suspend fun profileEditForm(stationId: StationId): ProfileEditForm

    /** Posts the whole form back with the edited fields, then reads it again. Returns the form as the station kept it. */
    suspend fun saveProfile(stationId: StationId, form: ProfileEditForm, edited: EditableProfile): ProfileEditForm
}

/** The station offered no form to a page that needs one; the response was not a sign-in page either. */
internal class StationFormUnavailableException : IOException("Station form was not offered")

/** Reads station information with plain GET requests. Nothing here changes anything on the station. */
internal class StationExtrasRemoteDataSource(
    private val pages: StationPages,
    private val parser: StationExtrasParser = StationExtrasParser(),
    private val favoritesParser: FavoriteTracksPageParser = FavoriteTracksPageParser(),
    private val catalogParser: StationCatalogParser = StationCatalogParser(),
    private val communityParser: StationCommunityParser = StationCommunityParser(),
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

    override suspend fun recentlyAdded(stationId: StationId): List<RecentlyAddedBatch> = withContext(Dispatchers.IO) {
        catalogParser.parseRecentlyAdded(pages.get(stationId, RECENTLY_ADDED_PATH, NEWS_LIMIT).body, pages.origin(stationId))
    }

    override suspend fun albumReviews(stationId: StationId, albumId: String): AlbumReviewsPage =
        withContext(Dispatchers.IO) {
            require(albumId.matches(ALBUM_ID))
            catalogParser.parseAlbumReviews(
                pages.get(stationId, ALBUM_PATH + encode(albumId), ALBUM_LIMIT).body,
                pages.origin(stationId),
            )
        }

    override suspend fun submitAlbumReview(
        stationId: StationId,
        albumId: String,
        title: String,
        body: String,
        rating: String,
    ): AlbumReviewsPage = withContext(Dispatchers.IO) {
        require(albumId.matches(ALBUM_ID))
        if (!pages.hasSession(stationId)) throw FavoritesAuthenticationRequiredException()
        val origin = pages.origin(stationId)
        val formPage = pages.get(stationId, REVIEW_FORM_PATH + encode(albumId), ALBUM_LIMIT)
        val form = catalogParser.parseReviewForm(formPage.body, origin) ?: throw signedOutOrUnavailable(formPage.body, origin, stationId)
        // Only an address the form names, for this album, is posted to.
        val action = URI(form.actionPath)
        if (action.path != "/modules.php" || queryValue(action.rawQuery, "asin") != albumId) {
            throw IOException("Review form posts elsewhere")
        }
        if (form.ratings.none { it.value == rating }) throw StationFormUnavailableException()
        pages.postForm(
            stationId,
            form.actionPath,
            listOf("title" to title, "content" to body, "reviewrating" to rating),
            ALBUM_LIMIT,
        )
        albumReviews(stationId, albumId)
    }

    override suspend fun onlineNow(stationId: StationId): OnlineBlock = withContext(Dispatchers.IO) {
        communityParser.parseOnlineBlock(pages.get(stationId, HOME_PATH, HOME_LIMIT).body, pages.origin(stationId))
    }

    override suspend fun members(stationId: StationId, query: String, sort: MemberListSort, start: Int): MembersPage =
        withContext(Dispatchers.IO) {
            require(start >= 0)
            val order = if (sort == MemberListSort.Name) "ASC" else "DESC"
            val path = "$MEMBERS_PATH&mode=${sort.query}&order=$order&namepart=${encode(query.take(MAX_NAME_CHARACTERS))}&start=$start"
            communityParser.parseMembersPage(pages.get(stationId, path, MEMBERS_LIMIT).body, pages.origin(stationId))
        }

    override suspend fun calendar(stationId: StationId): List<CalendarDay> = withContext(Dispatchers.IO) {
        communityParser.parseCalendar(pages.get(stationId, CALENDAR_PATH, CALENDAR_LIMIT).body, pages.origin(stationId))
    }

    override suspend fun profileEditForm(stationId: StationId): ProfileEditForm = withContext(Dispatchers.IO) {
        if (!pages.hasSession(stationId)) throw FavoritesAuthenticationRequiredException()
        val origin = pages.origin(stationId)
        val page = pages.get(stationId, PROFILE_EDIT_PATH, PROFILE_EDIT_LIMIT)
        catalogParser.parseProfileEditForm(page.body, origin) ?: throw signedOutOrUnavailable(page.body, origin, stationId)
    }

    override suspend fun saveProfile(stationId: StationId, form: ProfileEditForm, edited: EditableProfile): ProfileEditForm =
        withContext(Dispatchers.IO) {
            if (!pages.hasSession(stationId)) throw FavoritesAuthenticationRequiredException()
            // The form's own address is the only place the member's details are sent.
            val action = URI(form.actionPath)
            if (action.path != "/modules.php" || queryValue(action.rawQuery, "name") != "Your_Account") {
                throw IOException("Profile form posts elsewhere")
            }
            pages.postForm(stationId, form.actionPath, catalogParser.profileFormFields(form, edited), PROFILE_EDIT_LIMIT)
            profileEditForm(stationId)
        }

    private fun signedOutOrUnavailable(html: String, origin: String, stationId: StationId): IOException =
        if (favoritesParser.showsSignedOutVisitor(html, origin)) {
            pages.expireSession(stationId)
            FavoritesAuthenticationRequiredException()
        } else {
            StationFormUnavailableException()
        }

    private fun queryValue(query: String?, name: String): String? = query
        ?.split('&')
        ?.mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 } }
        ?.firstOrNull { it[0] == name }
        ?.get(1)

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val PROFILE_PATH = "/modules/Your_Profile/hoverCardAJAX.php?username="
        const val HISTORY_PATH = "/modules/Played_History/HistTableAJAX.php"
        const val NEWS_PATH = "/modules.php?name=News"
        const val MEMBER_FAVORITES_PATH = "/modules.php?name=Favorites&user2view="
        const val RECENTLY_ADDED_PATH = "/modules.php?name=News&view=recent"
        const val ALBUM_PATH = "/modules.php?name=Album&asin="
        const val REVIEW_FORM_PATH = "/modules.php?name=Album&action=newreview&asin="
        const val HOME_PATH = "/"
        const val MEMBERS_PATH = "/modules.php?name=Members_List&file=index"
        const val CALENDAR_PATH = "/modules.php?name=Birthdays"
        const val PROFILE_EDIT_PATH = "/modules.php?name=Your_Account&op=edituser"
        const val ALBUM_LIMIT = 1_000_000
        const val HOME_LIMIT = 2_000_000
        const val MEMBERS_LIMIT = 1_000_000
        const val CALENDAR_LIMIT = 1_000_000
        const val PROFILE_EDIT_LIMIT = 1_000_000
        val ALBUM_ID = Regex("[A-Za-z0-9_.-]{1,64}")
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
