package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationTrackActionsRemoteDataSourceTest {
    private val station = StationId("sst")

    @Test
    fun `adds the track the Studio page names for the signed-in listener`() = runTest {
        val pages = FakeStationPages { _, path, _ ->
            if (path == "/studio.php") studioPage(favoriteControl = signedInFavoriteControl(songId = "48213")) else "OK"
        }

        val outcome = StationTrackActionsRemoteDataSource(pages)
            .addCurrentTrackToFavorites(station, "The Story")

        assertEquals(AddFavoriteOutcome.Added, outcome)
        assertEquals(
            listOf("GET /studio.php", "GET /modules.php?name=Favorites&op=add&songid=48213"),
            pages.requests,
        )
    }

    @Test
    fun `does not add a favorite when the station has moved to another track`() = runTest {
        val pages = FakeStationPages { _, _, _ ->
            studioPage(track = "Another Track", favoriteControl = signedInFavoriteControl(songId = "9"))
        }

        val outcome = StationTrackActionsRemoteDataSource(pages)
            .addCurrentTrackToFavorites(station, "The Story")

        assertEquals(AddFavoriteOutcome.TrackChanged, outcome)
        assertEquals(listOf("GET /studio.php"), pages.requests)
    }

    @Test
    fun `reports a track that is already a favorite without sending anything`() = runTest {
        val pages = FakeStationPages { _, _, _ ->
            studioPage(favoriteControl = signedInFavoriteControl(songId = "48213", heartClass = ""))
        }

        val outcome = StationTrackActionsRemoteDataSource(pages)
            .addCurrentTrackToFavorites(station, "Story, The")

        assertEquals(AddFavoriteOutcome.AlreadyFavorite, outcome)
        assertEquals(listOf("GET /studio.php"), pages.requests)
    }

    @Test
    fun `ends the saved session when the Studio page asks the listener to log in`() = runTest {
        val pages = FakeStationPages { _, _, _ -> studioPage(favoriteControl = SIGNED_OUT_FAVORITE_CONTROL) }

        val outcome = StationTrackActionsRemoteDataSource(pages)
            .addCurrentTrackToFavorites(station, "The Story")

        assertEquals(AddFavoriteOutcome.SignInRequired, outcome)
        assertTrue(pages.expired)
        assertEquals(listOf("GET /studio.php"), pages.requests)
    }

    @Test
    fun `asks for sign-in without any request when no session is saved`() = runTest {
        val pages = FakeStationPages(signedIn = false) { _, _, _ -> error("no request expected") }

        val outcome = StationTrackActionsRemoteDataSource(pages)
            .addCurrentTrackToFavorites(station, "The Story")

        assertEquals(AddFavoriteOutcome.SignInRequired, outcome)
        assertTrue(pages.requests.isEmpty())
    }

    @Test
    fun `sends one rating with the station's own form fields and confirms it`() = runTest {
        var rated = false
        val pages = FakeStationPages { method, _, _ ->
            if (method == "POST") rated = true
            if (rated) ratingPage(body = "<div>You have already rated this album.</div>") else ratingPage(body = RATING_FORM)
        }

        val result = StationTrackActionsRemoteDataSource(pages).submitAlbumRating(station, "B0028ERCMU", "4.5")

        assertTrue(result is AlbumRatingSubmission.Rated)
        assertEquals(1, pages.posted.size)
        assertEquals(
            listOf("csrf" to "token-value", "asin" to "B0028ERCMU", "rating" to "4.5", "submit" to "Rate It!"),
            pages.posted.single(),
        )
        assertEquals("/modules/Ratings/playing_rating.php?asin=B0028ERCMU", pages.requests.last().substringAfter(' '))
    }

    @Test
    fun `reports an unconfirmed rating instead of sending it again`() = runTest {
        val pages = FakeStationPages { _, _, _ -> ratingPage(body = RATING_FORM) }

        val result = StationTrackActionsRemoteDataSource(pages).submitAlbumRating(station, "B0028ERCMU", "5")

        assertEquals(AlbumRatingSubmission.Unconfirmed, result)
        assertEquals(1, pages.posted.size)
    }

    @Test
    fun `does not send a rating for an album that was already rated`() = runTest {
        val pages = FakeStationPages { _, _, _ -> ratingPage(body = "<div>You have already rated this album.</div>") }

        val result = StationTrackActionsRemoteDataSource(pages).submitAlbumRating(station, "B0028ERCMU", "5")

        assertTrue(result is AlbumRatingSubmission.NotAccepted)
        assertTrue(pages.posted.isEmpty())
    }

    @Test
    fun `rating page parser reads the signed-out, rated, and open states`() {
        val parser = AlbumRatingPageParser()
        val signedOut = parser.parse(
            ratingPage(body = "<div><a href=\"/modules.php?name=Your_Account\">Log in</a> to Rate.</div>"),
            FakeStationPages.ORIGIN,
        )
        val open = parser.parse(ratingPage(body = RATING_FORM), FakeStationPages.ORIGIN)

        assertEquals(AlbumRatingAccess.SignInRequired, signedOut.access)
        assertEquals("Battlestar Galactica: Season 4", signedOut.albumTitle)
        assertEquals("Bear McCreary", signedOut.artist)
        assertEquals("4.5 out of 5", signedOut.currentRating)
        assertEquals("114", signedOut.voteCount)
        assertEquals(AlbumRatingAccess.CanRate, open.access)
        assertEquals(listOf("5", "4.5", "1"), open.options.map { it.value })
        assertFalse(open.options.any { it.label.startsWith("Select") })
    }

    private fun studioPage(track: String = "Story, The", favoriteControl: String) = """
        <html><body>
          <table id="now-playing-zone"><tr><td>
            <div class="studio-info-heading">
              <span class="studio-info-line"><a href="/modules.php?name=Requests&amp;artist=x">The Passions</a> - <span>$track</span></span>
              <span class="dim-text studio-info-line">Thirty Thousand Feet Over China</span>
            </div>
            $favoriteControl
          </td></tr></table>
        </body></html>
    """.trimIndent()

    private fun signedInFavoriteControl(songId: String, heartClass: String = "favorite-inactive") = """
        <a href="javascript:void(0);" onclick="addToFavorites($songId); return false;" title="Add to Favorites" class="action-icon fav-button">
          <img src="/images/favorites/heart.svg" id="fav_icon_img" class="$heartClass" alt="Fav">
        </a>
    """.trimIndent()

    private fun ratingPage(body: String) = """
        <html class="rating-popup"><body class="station-sst">
          <div class="rating-card">
            <div class="rating-title" title="Battlestar Galactica: Season 4">Battlestar Galactica: Season 4</div>
            <div class="rating-artist">Bear McCreary</div>
            <div>Current Rating:</div>
            <div class="rating-box" title="4.5 out of 5"><div class="rating-bg"></div><div class="rating-fg"></div></div>
            <div>(114 votes)</div>
            $body
          </div>
        </body></html>
    """.trimIndent()

    private companion object {
        const val SIGNED_OUT_FAVORITE_CONTROL = """
            <a href="javascript:void(0);" onclick="alert('You must be logged in to add to Favorites.'); return false;" title="Log in to add to Favorites" class="action-icon fav-button">
              <img src="/images/favorites/heart.svg" id="fav_icon_img" class="favorite-inactive" alt="Fav">
            </a>
        """
        const val RATING_FORM = """
            <form action="" method="post">
              <input type="hidden" name="csrf" value="token-value">
              <input type="hidden" name="asin" value="B0028ERCMU">
              <select name="rating">
                <option value="0">Select a Rating...</option>
                <option value="5">5.0 - Perfect</option>
                <option value="4.5">4.5 - Excellent</option>
                <option value="1">1.0 - Not Listenable</option>
              </select><br>
              <input type="submit" name="submit" value="Rate It!">
            </form>
        """
    }
}
