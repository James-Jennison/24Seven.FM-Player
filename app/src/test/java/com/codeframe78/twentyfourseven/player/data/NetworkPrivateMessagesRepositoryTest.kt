package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.PrivateMessage
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageComposeStatus
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageFolder
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageOpenStatus
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageSummary
import com.codeframe78.twentyfourseven.player.domain.PrivateMessagesStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class NetworkPrivateMessagesRepositoryTest {
    private val station = StationId("sst")

    @Test
    fun `opening an unread message marks it read and lowers the unread count`() = runTest {
        val repository = NetworkPrivateMessagesRepository(FakeRemote())
        repository.refresh(station, PrivateMessageFolder.Inbox, 1)

        repository.openMessage(station, "901")

        val state = repository.observeMessages(station).first()
        assertEquals(PrivateMessagesStatus.Ready, state.status)
        assertEquals(PrivateMessageOpenStatus.Ready, state.openStatus)
        assertFalse(state.messages.single().isUnread)
        assertEquals(0, state.unreadCount)
    }

    @Test
    fun `the unread count is learned without opening the folder and is asked for sparingly`() = runTest {
        val remote = FakeRemote()
        var now = 0L
        val repository = NetworkPrivateMessagesRepository(remote, elapsedMillis = { now })

        repository.refreshUnreadCount(station)
        now = 60_000
        repository.refreshUnreadCount(station)

        val state = repository.observeMessages(station).first()
        assertEquals(1, state.unreadCount)
        assertEquals(PrivateMessagesStatus.Idle, state.status)
        assertEquals(emptyList<PrivateMessageSummary>(), state.messages)
        assertEquals(1, remote.folderLoads)

        now = 121_000
        repository.refreshUnreadCount(station)
        assertEquals(2, remote.folderLoads)
    }

    @Test
    fun `a failed unread check keeps the last known count`() = runTest {
        val repository = NetworkPrivateMessagesRepository(FakeRemote(folderFailure = java.io.IOException("offline")))

        repository.refreshUnreadCount(station)

        val state = repository.observeMessages(station).first()
        assertNull(state.unreadCount)
        assertEquals(PrivateMessagesStatus.Idle, state.status)
    }

    @Test
    fun `a station that asks for sign-in clears the messages`() = runTest {
        val repository = NetworkPrivateMessagesRepository(FakeRemote(folderFailure = PrivateMessagesSignInRequiredException()))

        repository.refresh(station, PrivateMessageFolder.Inbox, 1)

        val state = repository.observeMessages(station).first()
        assertEquals(PrivateMessagesStatus.SignInRequired, state.status)
        assertEquals(emptyList<PrivateMessageSummary>(), state.messages)
    }

    @Test
    fun `a reply is prepared from the station form and sent once`() = runTest {
        val remote = FakeRemote()
        val repository = NetworkPrivateMessagesRepository(remote)
        repository.refresh(station, PrivateMessageFolder.Inbox, 1)
        repository.openMessage(station, "901")

        repository.beginReply(station)
        assertEquals("Re: Hello", repository.observeMessages(station).first().compose?.subject)
        repository.send(station, "Re: Hello", "Thanks")
        repository.send(station, "Re: Hello", "Thanks")

        assertEquals(1, remote.sent)
        assertEquals(PrivateMessageComposeStatus.Sent, repository.observeMessages(station).first().compose?.status)
    }

    @Test
    fun `a send whose request failed is unconfirmed and cannot be repeated from the same draft`() = runTest {
        val remote = FakeRemote(sendFailure = IOException("timeout"))
        val repository = NetworkPrivateMessagesRepository(remote)
        repository.beginMessage(station, "Listener")

        repository.send(station, "Hello", "A short note")
        repository.send(station, "Hello", "A short note")

        assertEquals(1, remote.sent)
        assertEquals(PrivateMessageComposeStatus.Unconfirmed, repository.observeMessages(station).first().compose?.status)
    }

    @Test
    fun `an unknown recipient is reported and nothing can be sent`() = runTest {
        val remote = FakeRemote(form = null)
        val repository = NetworkPrivateMessagesRepository(remote)

        repository.beginMessage(station, "NoSuchMember")
        repository.send(station, "Hello", "A short note")

        assertEquals(
            PrivateMessageComposeStatus.RecipientNotFound,
            repository.observeMessages(station).first().compose?.status,
        )
        assertEquals(0, remote.sent)
    }

    @Test
    fun `discarding a draft removes it`() = runTest {
        val repository = NetworkPrivateMessagesRepository(FakeRemote())
        repository.beginMessage(station, "Listener")

        repository.cancelCompose(station)

        assertNull(repository.observeMessages(station).first().compose)
    }

    private class FakeRemote(
        private val folderFailure: Exception? = null,
        private val form: PrivateMessageForm? = PrivateMessageForm("Listener", "Re: Hello", "", listOf("u" to "57", "op" to "sendmsg"), "1"),
        private val sendFailure: Exception? = null,
    ) : PrivateMessagesRemoteDataSource {
        var sent = 0
        var folderLoads = 0

        override suspend fun loadFolder(stationId: StationId, folder: PrivateMessageFolder, page: Int): PrivateMessagesPage {
            folderLoads++
            folderFailure?.let { throw it }
            return PrivateMessagesPage(
                messages = listOf(PrivateMessageSummary("901", "Hello", "Listener", "Oct 02, 2026", isUnread = true)),
                page = 1,
                pageCount = 1,
                unreadCount = 1,
            )
        }

        override suspend fun loadMessage(stationId: StationId, folder: PrivateMessageFolder, messageId: String) =
            PrivateMessage(messageId, folder, "Listener", "Member", "October 02, 2026", "Hello", "Body", canReply = true)

        override suspend fun replyForm(stationId: StationId, messageId: String) = form

        override suspend fun messageForm(stationId: StationId, recipient: String) = form

        override suspend fun send(
            stationId: StationId,
            form: PrivateMessageForm,
            subject: String,
            body: String,
        ): PrivateMessageSendOutcome {
            sent++
            sendFailure?.let { throw it }
            return PrivateMessageSendOutcome.Sent
        }
    }
}
