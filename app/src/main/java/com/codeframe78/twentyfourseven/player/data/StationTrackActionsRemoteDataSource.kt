package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.trackMatchKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal enum class AddFavoriteOutcome { Added, AlreadyFavorite, TrackChanged, SignInRequired }

internal sealed interface AlbumRatingSubmission {
    data class Rated(val page: AlbumRatingPage) : AlbumRatingSubmission
    data class NotAccepted(val page: AlbumRatingPage) : AlbumRatingSubmission
    data object Unconfirmed : AlbumRatingSubmission
}

internal interface TrackActionsRemoteDataSource {
    suspend fun addCurrentTrackToFavorites(stationId: StationId, trackTitle: String): AddFavoriteOutcome
    suspend fun loadAlbumRating(stationId: StationId, albumId: String): AlbumRatingPage
    suspend fun submitAlbumRating(stationId: StationId, albumId: String, value: String): AlbumRatingSubmission
}

internal class StationTrackActionsRemoteDataSource(
    private val pages: StationPages,
    private val studioParser: StudioNowPlayingParser = StudioNowPlayingParser(),
    private val ratingParser: AlbumRatingPageParser = AlbumRatingPageParser(),
    private val sessionEvidence: AuthLoginResultParser = AuthLoginResultParser(),
) : TrackActionsRemoteDataSource {
    override suspend fun addCurrentTrackToFavorites(
        stationId: StationId,
        trackTitle: String,
    ): AddFavoriteOutcome = withContext(Dispatchers.IO) {
        if (!pages.hasSession(stationId)) return@withContext AddFavoriteOutcome.SignInRequired
        val origin = pages.origin(stationId)
        // The Studio page is where the station names the current track's identifier for the signed-in listener.
        val studio = studioParser.parse(pages.get(stationId, STUDIO_PATH, PAGE_LIMIT).body, origin)
        if (studio.asksToSignIn) {
            pages.expireSession(stationId)
            return@withContext AddFavoriteOutcome.SignInRequired
        }
        if (studio.trackTitle?.trackMatchKey() != trackTitle.trackMatchKey()) {
            return@withContext AddFavoriteOutcome.TrackChanged
        }
        if (studio.isFavorite) return@withContext AddFavoriteOutcome.AlreadyFavorite
        val songId = studio.songId ?: throw IOException("Station did not offer the favorite action")
        val response = pages.get(stationId, "$ADD_FAVORITE_PATH$songId", PAGE_LIMIT)
        if (sessionEvidence.showsSignedOutVisitor(response.body, origin)) {
            pages.expireSession(stationId)
            return@withContext AddFavoriteOutcome.SignInRequired
        }
        AddFavoriteOutcome.Added
    }

    override suspend fun loadAlbumRating(stationId: StationId, albumId: String): AlbumRatingPage =
        withContext(Dispatchers.IO) { ratingPage(stationId, albumId) }

    override suspend fun submitAlbumRating(
        stationId: StationId,
        albumId: String,
        value: String,
    ): AlbumRatingSubmission = withContext(Dispatchers.IO) {
        // A fresh read supplies the form's current anti-forgery value and confirms the album can still be rated.
        val fresh = ratingPage(stationId, albumId)
        if (fresh.access != AlbumRatingAccess.CanRate) return@withContext AlbumRatingSubmission.NotAccepted(fresh)
        if (fresh.options.none { it.value == value }) throw IOException("Station does not offer this rating")
        val (submitFields, hiddenFields) = fresh.formFields
            .filter { (name, _) -> name != RATING_FIELD }
            .partition { (name, _) -> name.equals("submit", ignoreCase = true) }
        val origin = pages.origin(stationId)
        val posted = ratingParser.parse(
            pages.postForm(
                stationId,
                ratingPath(albumId),
                hiddenFields + (RATING_FIELD to value) + submitFields,
                PAGE_LIMIT,
            ).body,
            origin,
        )
        if (posted.access == AlbumRatingAccess.AlreadyRated) return@withContext AlbumRatingSubmission.Rated(posted)
        // The rating is never sent twice. One further read asks the station whether it now counts the rating.
        val confirmed = runCatching { ratingPage(stationId, albumId) }.getOrNull()
        if (confirmed?.access == AlbumRatingAccess.AlreadyRated) {
            AlbumRatingSubmission.Rated(confirmed)
        } else {
            AlbumRatingSubmission.Unconfirmed
        }
    }

    private fun ratingPage(stationId: StationId, albumId: String): AlbumRatingPage {
        val hadSession = pages.hasSession(stationId)
        val page = ratingParser.parse(
            pages.get(stationId, ratingPath(albumId), PAGE_LIMIT).body,
            pages.origin(stationId),
        )
        if (page.access == AlbumRatingAccess.SignInRequired && hadSession) pages.expireSession(stationId)
        return page
    }

    private fun ratingPath(albumId: String): String {
        if (!albumId.matches(SAFE_ALBUM_ID)) throw IOException("Album identifier was not recognized")
        return "$RATING_PATH$albumId"
    }

    private companion object {
        const val STUDIO_PATH = "/studio.php"
        const val ADD_FAVORITE_PATH = "/modules.php?name=Favorites&op=add&songid="
        const val RATING_PATH = "/modules/Ratings/playing_rating.php?asin="
        const val RATING_FIELD = "rating"
        const val PAGE_LIMIT = 512_000
        val SAFE_ALBUM_ID = Regex("[A-Za-z0-9_.-]{1,64}")
    }
}
