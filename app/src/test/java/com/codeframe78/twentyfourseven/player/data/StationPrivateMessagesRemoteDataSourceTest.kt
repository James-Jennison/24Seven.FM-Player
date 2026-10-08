package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.PrivateMessageFolder
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationPrivateMessagesRemoteDataSourceTest {
    private val station = StationId("sst")
    private val form = PrivateMessageForm(
        recipient = "Listener",
        subject = "",
        body = "",
        hiddenFields = listOf("u" to "57", "op" to "sendmsg"),
        saveCopyValue = "1",
    )

    @Test
    fun `sends one message and confirms it from the Sent folder`() = runTest {
        val pages = FakeStationPages { _, _, _ ->
            PrivateMessageFixtures.folderPage(firstSubject = "Hello", firstCorrespondent = "Listener")
        }

        val outcome = StationPrivateMessagesRemoteDataSource(pages).send(station, form, "Hello", "A short note")

        assertEquals(PrivateMessageSendOutcome.Sent, outcome)
        assertEquals(
            listOf("subject" to "Hello", "message" to "A short note", "savecopy" to "1", "u" to "57", "op" to "sendmsg"),
            pages.posted.single(),
        )
        assertEquals(
            listOf(
                "POST /modules.php?name=Private_Messages",
                "GET /modules.php?name=Private_Messages&folder=outbox&p=1",
            ),
            pages.requests,
        )
    }

    @Test
    fun `a message the Sent folder does not show is unconfirmed and is not sent again`() = runTest {
        val pages = FakeStationPages { _, _, _ -> PrivateMessageFixtures.folderPage(firstSubject = "Something else") }

        val outcome = StationPrivateMessagesRemoteDataSource(pages).send(station, form, "Hello", "A short note")

        assertEquals(PrivateMessageSendOutcome.Unconfirmed, outcome)
        assertEquals(1, pages.posted.size)
    }

    @Test
    fun `a visitor answer to the members-only module asks for sign-in`() = runTest {
        val pages = FakeStationPages { _, _, _ -> throw StationHttpException(403) }

        val failure = runCatching {
            StationPrivateMessagesRemoteDataSource(pages).loadFolder(station, PrivateMessageFolder.Inbox, 1)
        }.exceptionOrNull()

        assertTrue(failure is PrivateMessagesSignInRequiredException)
    }

    @Test
    fun `a name the station does not know never addresses a message to someone else`() = runTest {
        // The station answers an unknown name with the signed-in member's own profile and form.
        val pages = FakeStationPages { _, path, _ ->
            if ("op=userinfo" in path) {
                "<html><body><a href=\"modules.php?name=Private_Messages&amp;file=index&amp;mode=post&amp;u=57\">PM</a></body></html>"
            } else {
                PrivateMessageFixtures.formPage(recipient = "Member")
            }
        }

        assertNull(StationPrivateMessagesRemoteDataSource(pages).messageForm(station, "NoSuchMember"))
    }

    @Test
    fun `a known member's profile leads to that member's message form`() = runTest {
        val pages = FakeStationPages { _, path, _ ->
            if ("op=userinfo" in path) {
                "<html><body><a href=\"modules.php?name=Private_Messages&amp;file=index&amp;mode=post&amp;u=4821\">PM</a></body></html>"
            } else {
                PrivateMessageFixtures.formPage(recipient = "Listener", memberNumber = "4821")
            }
        }

        val found = StationPrivateMessagesRemoteDataSource(pages).messageForm(station, "listener")

        assertEquals("Listener", found?.recipient)
        assertEquals("GET /modules.php?name=Private_Messages&file=index&mode=post&u=4821", pages.requests.last())
    }
}
