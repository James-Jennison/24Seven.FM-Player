package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.MAX_PRIVATE_MESSAGE_BODY_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.MAX_PRIVATE_MESSAGE_SUBJECT_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageCompose
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageComposeStatus
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageFolder
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageOpenStatus
import com.codeframe78.twentyfourseven.player.domain.PrivateMessagesRepository
import com.codeframe78.twentyfourseven.player.domain.PrivateMessagesState
import com.codeframe78.twentyfourseven.player.domain.PrivateMessagesStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class NetworkPrivateMessagesRepository(
    private val remote: PrivateMessagesRemoteDataSource,
) : PrivateMessagesRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<PrivateMessagesState>>()
    private val locks = ConcurrentHashMap<StationId, Mutex>()

    /** The station's own form for the message being written. It is used for one send and then discarded. */
    private val forms = ConcurrentHashMap<StationId, PrivateMessageForm>()

    override fun observeMessages(stationId: StationId): Flow<PrivateMessagesState> =
        state(stationId.canonicalized()).asStateFlow()

    override suspend fun refresh(stationId: StationId, folder: PrivateMessageFolder, page: Int) {
        val canonical = stationId.canonicalized()
        lock(canonical).withLock {
            state(canonical).update { current ->
                current.copy(
                    status = PrivateMessagesStatus.Loading,
                    folder = folder,
                    page = page,
                    messages = if (current.folder == folder) current.messages else emptyList(),
                )
            }
            try {
                val loaded = remote.loadFolder(canonical, folder, page.coerceAtLeast(1))
                state(canonical).update { current ->
                    current.copy(
                        status = PrivateMessagesStatus.Ready,
                        page = loaded.page,
                        pageCount = loaded.pageCount,
                        messages = loaded.messages,
                        unreadCount = loaded.unreadCount ?: current.unreadCount,
                    )
                }
            } catch (cancellation: CancellationException) {
                state(canonical).update { it.copy(status = PrivateMessagesStatus.Idle) }
                throw cancellation
            } catch (_: PrivateMessagesSignInRequiredException) {
                state(canonical).value = PrivateMessagesState(canonical, PrivateMessagesStatus.SignInRequired)
            } catch (_: Exception) {
                state(canonical).update { it.copy(status = PrivateMessagesStatus.Error) }
            }
        }
    }

    override suspend fun openMessage(stationId: StationId, messageId: String) {
        val canonical = stationId.canonicalized()
        lock(canonical).withLock {
            val folder = state(canonical).value.folder
            state(canonical).update { it.copy(openStatus = PrivateMessageOpenStatus.Loading, openMessage = null) }
            try {
                val message = remote.loadMessage(canonical, folder, messageId)
                state(canonical).update { current ->
                    val wasUnread = current.messages.any { it.id == messageId && it.isUnread }
                    current.copy(
                        openStatus = PrivateMessageOpenStatus.Ready,
                        openMessage = message,
                        // Opening a message marks it read on the station, so the list and the count follow.
                        messages = current.messages.map { if (it.id == messageId) it.copy(isUnread = false) else it },
                        unreadCount = current.unreadCount?.let { if (wasUnread) (it - 1).coerceAtLeast(0) else it },
                    )
                }
            } catch (cancellation: CancellationException) {
                state(canonical).update { it.copy(openStatus = PrivateMessageOpenStatus.Closed) }
                throw cancellation
            } catch (_: PrivateMessagesSignInRequiredException) {
                state(canonical).value = PrivateMessagesState(canonical, PrivateMessagesStatus.SignInRequired)
            } catch (_: Exception) {
                state(canonical).update { it.copy(openStatus = PrivateMessageOpenStatus.Error) }
            }
        }
    }

    override suspend fun closeMessage(stationId: StationId) {
        state(stationId.canonicalized()).update {
            it.copy(openStatus = PrivateMessageOpenStatus.Closed, openMessage = null)
        }
    }

    override suspend fun beginReply(stationId: StationId) {
        val canonical = stationId.canonicalized()
        val message = state(canonical).value.openMessage?.takeIf { it.canReply } ?: return
        prepare(canonical, recipient = message.sender, isReply = true) { remote.replyForm(canonical, message.id) }
    }

    override suspend fun beginMessage(stationId: StationId, recipient: String) {
        val canonical = stationId.canonicalized()
        val name = recipient.trim()
        if (name.isEmpty()) return
        prepare(canonical, recipient = name, isReply = false) { remote.messageForm(canonical, name) }
    }

    private suspend fun prepare(
        stationId: StationId,
        recipient: String,
        isReply: Boolean,
        load: suspend () -> PrivateMessageForm?,
    ) {
        lock(stationId).withLock {
            if (state(stationId).value.compose?.status == PrivateMessageComposeStatus.Sending) return
            forms.remove(stationId)
            val preparing = PrivateMessageCompose(PrivateMessageComposeStatus.Preparing, recipient, isReply = isReply)
            state(stationId).update { it.copy(compose = preparing) }
            val compose = try {
                val form = load()
                if (form == null) {
                    preparing.copy(
                        status = if (isReply) {
                            PrivateMessageComposeStatus.Failed
                        } else {
                            PrivateMessageComposeStatus.RecipientNotFound
                        },
                    )
                } else {
                    forms[stationId] = form
                    PrivateMessageCompose(
                        status = PrivateMessageComposeStatus.Ready,
                        recipient = form.recipient,
                        subject = form.subject.take(MAX_PRIVATE_MESSAGE_SUBJECT_CHARACTERS),
                        body = form.body.take(MAX_PRIVATE_MESSAGE_BODY_CHARACTERS),
                        isReply = isReply,
                    )
                }
            } catch (cancellation: CancellationException) {
                state(stationId).update { it.copy(compose = null) }
                throw cancellation
            } catch (_: Exception) {
                preparing.copy(status = PrivateMessageComposeStatus.Failed)
            }
            state(stationId).update { it.copy(compose = compose) }
        }
    }

    override suspend fun send(stationId: StationId, subject: String, body: String) {
        val canonical = stationId.canonicalized()
        if (
            subject.isBlank() || subject.length > MAX_PRIVATE_MESSAGE_SUBJECT_CHARACTERS ||
            body.isBlank() || body.length > MAX_PRIVATE_MESSAGE_BODY_CHARACTERS
        ) {
            return
        }
        // Only the caller that moves the draft from Ready to Sending sends it, so a repeat tap sends nothing.
        val previous = state(canonical).getAndUpdate { current ->
            val compose = current.compose
            if (compose?.status == PrivateMessageComposeStatus.Ready) {
                current.copy(
                    compose = compose.copy(
                        status = PrivateMessageComposeStatus.Sending,
                        subject = subject,
                        body = body,
                    ),
                )
            } else {
                current
            }
        }
        if (previous.compose?.status != PrivateMessageComposeStatus.Ready) return
        // The form is spent whatever happens next: an uncertain send must not be repeated from the same draft.
        val form = forms.remove(canonical)
        val status = if (form == null) {
            PrivateMessageComposeStatus.Failed
        } else {
            try {
                when (remote.send(canonical, form, subject, body)) {
                    PrivateMessageSendOutcome.Sent -> PrivateMessageComposeStatus.Sent
                    PrivateMessageSendOutcome.Unconfirmed -> PrivateMessageComposeStatus.Unconfirmed
                }
            } catch (cancellation: CancellationException) {
                setComposeStatus(canonical, PrivateMessageComposeStatus.Unconfirmed)
                throw cancellation
            } catch (_: PrivateMessagesSignInRequiredException) {
                // The station turned the request away as a visitor, so nothing was delivered.
                PrivateMessageComposeStatus.Failed
            } catch (_: Exception) {
                // The request may have reached the station, so the outcome is unknown rather than failed.
                PrivateMessageComposeStatus.Unconfirmed
            }
        }
        setComposeStatus(canonical, status)
    }

    override suspend fun cancelCompose(stationId: StationId) {
        val canonical = stationId.canonicalized()
        val previous = state(canonical).getAndUpdate { current ->
            // A message that is on its way keeps its draft, so its result is still reported.
            if (current.compose?.status == PrivateMessageComposeStatus.Sending) current else current.copy(compose = null)
        }
        if (previous.compose?.status != PrivateMessageComposeStatus.Sending) forms.remove(canonical)
    }

    override suspend fun clear(stationId: StationId) {
        val canonical = stationId.canonicalized()
        forms.remove(canonical)
        state(canonical).value = PrivateMessagesState(canonical)
    }

    private fun setComposeStatus(stationId: StationId, status: PrivateMessageComposeStatus) {
        state(stationId).update { current -> current.copy(compose = current.compose?.copy(status = status)) }
    }

    private fun state(stationId: StationId) = states.getOrPut(stationId) {
        MutableStateFlow(PrivateMessagesState(stationId))
    }

    private fun lock(stationId: StationId) = locks.getOrPut(stationId, ::Mutex)
}
