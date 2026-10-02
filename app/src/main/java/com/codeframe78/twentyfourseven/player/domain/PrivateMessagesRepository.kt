package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

const val MAX_PRIVATE_MESSAGE_SUBJECT_CHARACTERS = 100
const val MAX_PRIVATE_MESSAGE_BODY_CHARACTERS = 4_000

enum class PrivateMessageFolder(val key: String, val label: String) {
    Inbox("inbox", "Inbox"),
    Sent("outbox", "Sent"),
    Saved("savebox", "Saved"),
}

data class PrivateMessageSummary(
    val id: String,
    val subject: String,
    /** The sender in the Inbox and Saved folders, the recipient in the Sent folder. */
    val correspondent: String,
    val dateLabel: String,
    val isUnread: Boolean,
)

data class PrivateMessage(
    val id: String,
    val folder: PrivateMessageFolder,
    val sender: String,
    val recipient: String,
    val dateLabel: String,
    val subject: String,
    val body: String,
    val canReply: Boolean,
)

enum class PrivateMessagesStatus { Idle, Loading, Ready, SignInRequired, Error }

enum class PrivateMessageOpenStatus { Closed, Loading, Ready, Error }

enum class PrivateMessageComposeStatus {
    /** Looking up the recipient and the station's own form. */
    Preparing,
    Ready,
    Sending,
    Sent,

    /** The message was sent once, but the station did not show it in the Sent folder. It is never sent again. */
    Unconfirmed,
    RecipientNotFound,
    Failed,
}

data class PrivateMessageCompose(
    val status: PrivateMessageComposeStatus,
    val recipient: String,
    val subject: String = "",
    val body: String = "",
    val isReply: Boolean = false,
)

data class PrivateMessagesState(
    val stationId: StationId,
    val status: PrivateMessagesStatus = PrivateMessagesStatus.Idle,
    val folder: PrivateMessageFolder = PrivateMessageFolder.Inbox,
    val page: Int = 1,
    val pageCount: Int = 1,
    val messages: List<PrivateMessageSummary> = emptyList(),
    val unreadCount: Int? = null,
    val openStatus: PrivateMessageOpenStatus = PrivateMessageOpenStatus.Closed,
    val openMessage: PrivateMessage? = null,
    val compose: PrivateMessageCompose? = null,
)

/**
 * A listener's station private messages. Everything is held in memory for the current page and the one open
 * message only, and a message is sent by a single explicit request that is never repeated automatically.
 */
interface PrivateMessagesRepository {
    fun observeMessages(stationId: StationId): Flow<PrivateMessagesState>
    suspend fun refresh(stationId: StationId, folder: PrivateMessageFolder, page: Int)
    suspend fun openMessage(stationId: StationId, messageId: String)
    suspend fun closeMessage(stationId: StationId)
    suspend fun beginReply(stationId: StationId)
    suspend fun beginMessage(stationId: StationId, recipient: String)
    suspend fun send(stationId: StationId, subject: String, body: String)
    suspend fun cancelCompose(stationId: StationId)
    suspend fun clear(stationId: StationId)
}

object UnavailablePrivateMessagesRepository : PrivateMessagesRepository {
    override fun observeMessages(stationId: StationId): Flow<PrivateMessagesState> =
        flowOf(PrivateMessagesState(stationId))

    override suspend fun refresh(stationId: StationId, folder: PrivateMessageFolder, page: Int) = Unit
    override suspend fun openMessage(stationId: StationId, messageId: String) = Unit
    override suspend fun closeMessage(stationId: StationId) = Unit
    override suspend fun beginReply(stationId: StationId) = Unit
    override suspend fun beginMessage(stationId: StationId, recipient: String) = Unit
    override suspend fun send(stationId: StationId, subject: String, body: String) = Unit
    override suspend fun cancelCompose(stationId: StationId) = Unit
    override suspend fun clear(stationId: StationId) = Unit
}
