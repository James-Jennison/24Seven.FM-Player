package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.EditableProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationCatalogParserTest {
    private val parser = StationCatalogParser()
    private val origin = "https://streamingsoundtracks.com/"

    @Test
    fun `reads recently added batches with author, date, and station covers`() {
        val batches = parser.parseRecentlyAdded(StationCatalogFixtures.recentlyAdded(), origin)

        assertEquals(2, batches.size)
        with(batches[0]) {
            assertEquals("August 27, 2026", dateLabel)
            assertEquals("Editor", author)
            assertEquals(listOf("B000000001", "B000000002"), albums.map { it.albumId })
            assertEquals("Composer One - First Album", albums[0].title)
            assertEquals("https://streamingsoundtracks.com/images/cover/040/B000000001.jpg", albums[0].coverUrl)
        }
        with(batches[1]) {
            assertEquals("June 26, 2026", dateLabel)
            assertEquals("Curator", author)
            assertEquals(listOf("Third Album"), albums.map { it.title })
        }
    }

    @Test
    fun `recently added ignores off-site album links and unsafe album ids`() {
        val html = StationCatalogFixtures.recentlyAdded()
            .replace("asin=B000000002", "asin=B0%3Cscript%3E")

        val albums = parser.parseRecentlyAdded(html, origin).first().albums

        assertEquals(listOf("B000000001"), albums.map { it.albumId })
    }

    @Test
    fun `recently added caps the batches and handles a page without any`() {
        val story = StationCatalogFixtures.recentlyAdded().substringAfter("<main class=\"news-listing news-view-recent\">")
            .substringBefore("</main>")
        val many = "<main>" + story.repeat(20) + "</main>"

        assertEquals(30, parser.parseRecentlyAdded(many, origin).size)
        assertEquals(emptyList<Any>(), parser.parseRecentlyAdded("<html><body><p>Down for maintenance</p>", origin))
        assertEquals(emptyList<Any>(), parser.parseRecentlyAdded("<<article class=\"news-story\"><time>", origin))
    }

    @Test
    fun `reads an album review without the helpful line or the log-in prompt`() {
        val page = parser.parseAlbumReviews(StationCatalogFixtures.albumPage(canWrite = false), origin)

        assertEquals(1, page.reviews.size)
        with(page.reviews.single()) {
            assertEquals("Great Score", title)
            assertEquals("Reviewer", author)
            assertEquals("23 Sep 2022", dateLabel)
            assertEquals("4.5", rating)
            assertEquals("Warm themes & strong orchestration.\n\nWorth a listen.", body)
            assertEquals("1 of 2 found this review helpful", helpfulLabel)
            assertFalse(body.contains("log in"))
        }
    }

    @Test
    fun `a reviewer name that is a link still reads as the author`() {
        val html = StationCatalogFixtures.albumPage(canWrite = false, author = """<a href="/modules.php?name=Your_Account&amp;op=userinfo&amp;username=Reviewer">Reviewer</a>""")

        assertEquals("Reviewer", parser.parseAlbumReviews(html, origin).reviews.single().author)
    }

    @Test
    fun `an album page offers writing only when it carries the new review link`() {
        assertTrue(parser.parseAlbumReviews(StationCatalogFixtures.albumPage(canWrite = true), origin).canWrite)
        assertFalse(parser.parseAlbumReviews(StationCatalogFixtures.albumPage(canWrite = false), origin).canWrite)
        val none = parser.parseAlbumReviews("<html><body><p>No reviews yet</p></body></html>", origin)
        assertEquals(emptyList<Any>(), none.reviews)
        assertFalse(none.canWrite)
    }

    @Test
    fun `a long review body is cut to the limit`() {
        val html = StationCatalogFixtures.albumPage(canWrite = false).replace("Worth a listen.", "x".repeat(5_000))

        assertEquals(4_000, parser.parseAlbumReviews(html, origin).reviews.single().body.length)
    }

    @Test
    fun `reads the review form action as a path and leaves out the select choice`() {
        val form = parser.parseReviewForm(StationCatalogFixtures.reviewForm(), origin)!!

        assertEquals("/modules.php?name=Album&action=submitnewreview&asin=B000000001", form.actionPath)
        assertEquals(
            listOf("5", "4.5", "4", "3.5", "3", "2.5", "2", "1.5", "1"),
            form.ratings.map { it.value },
        )
        assertEquals("5.0 - Perfect", form.ratings.first().label)
        assertEquals("1.0 - Not Listenable", form.ratings.last().label)
    }

    @Test
    fun `the station's own www spelling of its address is accepted but other sites are not`() {
        val www = parser.parseReviewForm(
            StationCatalogFixtures.reviewForm("https://www.streamingsoundtracks.com/modules.php?name=Album&action=submitnewreview&asin=B000000001"),
            origin,
        )

        assertEquals("/modules.php?name=Album&action=submitnewreview&asin=B000000001", www?.actionPath)
        assertNull(parser.parseReviewForm(StationCatalogFixtures.reviewForm("https://example.com/modules.php?name=Album"), origin))
        assertNull(parser.parseReviewForm(StationCatalogFixtures.reviewForm("http://streamingsoundtracks.com/modules.php"), origin))
        assertNull(parser.parseReviewForm("<html><body>Please log in.</body></html>", origin))
    }

    @Test
    fun `picks the saving form from the profile page and reads its values`() {
        val form = parser.parseProfileEditForm(StationCatalogFixtures.profileEditPage(), origin)!!

        assertEquals("/modules.php?name=Your_Account", form.actionPath)
        with(form.profile) {
            assertEquals("Pat Listener", realName)
            assertEquals("Springfield", location)
            assertEquals("us.gif", flag)
            assertEquals("Engineer", occupation)
            assertEquals("Scores & themes", interests)
            assertEquals("http://www.example.org/listener", website)
            assertEquals("Line one\nLine two", signature)
            assertEquals("About me", bio)
            assertTrue(newsletter)
            assertTrue(hideOnlineStatus)
        }
        assertEquals(listOf("blank.gif", "af.gif", "us.gif"), form.flags.map { it.value })
        assertEquals("United States", form.flags.last().label)
    }

    @Test
    fun `profile fields keep document order, one value per radio group, and blank passwords`() {
        val form = parser.parseProfileEditForm(StationCatalogFixtures.profileEditPage(), origin)!!

        assertEquals(
            listOf(
                "realname", "user_password", "vpass", "user_email", "femail", "user_website", "user_from", "user_flag",
                "user_occ", "user_interests", "newsletter", "user_allow_viewonline", "user_notify", "user_timezone",
                "user_sig", "bio", "username", "user_id", "op",
            ),
            form.fields.map { it.first },
        )
        val values = form.fields.toMap()
        assertEquals("", values["user_password"])
        assertEquals("", values["vpass"])
        assertEquals("listener@example.org", values["user_email"])
        assertEquals("1", values["newsletter"])
        assertEquals("0", values["user_allow_viewonline"])
        assertEquals("0", values["user_notify"])
        assertEquals("0", values["user_timezone"])
        assertEquals("saveuser", values["op"])
        assertFalse(form.fields.any { it.first in setOf("query", "birthday", "avatar") })
    }

    @Test
    fun `a profile page without the saving form or with an off-site action yields no form`() {
        assertNull(parser.parseProfileEditForm("<html><body><form action=\"modules.php\"><input name=\"op\" value=\"search\"></form></body></html>", origin))
        val offSite = StationCatalogFixtures.profileEditPage().replace("<form action=\"modules.php?name=Your_Account\" method=\"post\">", "<form action=\"https://example.com/a\" method=\"post\">")
        assertNotNull(parser.parseProfileEditForm(StationCatalogFixtures.profileEditPage(), origin))
        assertNull(parser.parseProfileEditForm(offSite, origin))
    }

    @Test
    fun `edited profile values replace only their own fields`() {
        val form = parser.parseProfileEditForm(StationCatalogFixtures.profileEditPage(), origin)!!
        val edited = EditableProfile(
            realName = "Sam Listener",
            location = "Shelbyville",
            flag = "af.gif",
            occupation = "Pilot",
            interests = "Jazz",
            website = "https://www.example.org/sam",
            signature = "New signature",
            bio = "New bio",
            newsletter = false,
            hideOnlineStatus = false,
        )

        val fields = parser.profileFormFields(form, edited)
        val values = fields.toMap()

        assertEquals(form.fields.map { it.first }, fields.map { it.first })
        assertEquals("Sam Listener", values["realname"])
        assertEquals("Shelbyville", values["user_from"])
        assertEquals("af.gif", values["user_flag"])
        assertEquals("Pilot", values["user_occ"])
        assertEquals("Jazz", values["user_interests"])
        assertEquals("https://www.example.org/sam", values["user_website"])
        assertEquals("New signature", values["user_sig"])
        assertEquals("New bio", values["bio"])
        assertEquals("0", values["newsletter"])
        assertEquals("1", values["user_allow_viewonline"])
        val untouched = setOf("user_email", "femail", "user_notify", "user_timezone", "username", "user_id", "op")
        assertEquals(form.fields.filter { it.first in untouched }, fields.filter { it.first in untouched })
        assertEquals("", values["user_password"])
        assertEquals("", values["vpass"])
    }

    @Test
    fun `hiding the online status is sent as zero and long text is cut to the form's limits`() {
        val form = parser.parseProfileEditForm(StationCatalogFixtures.profileEditPage(), origin)!!
        val edited = form.profile.copy(
            realName = "n".repeat(100),
            signature = "s".repeat(900),
            bio = "b".repeat(2_000),
            flag = "zz.gif",
            hideOnlineStatus = true,
            newsletter = true,
        )

        val values = parser.profileFormFields(form, edited).toMap()

        assertEquals("0", values["user_allow_viewonline"])
        assertEquals("1", values["newsletter"])
        assertEquals(60, values["realname"]!!.length)
        assertEquals(500, values["user_sig"]!!.length)
        assertEquals(1_024, values["bio"]!!.length)
        // A country the station did not offer is not sent.
        assertEquals("us.gif", values["user_flag"])
    }
}
