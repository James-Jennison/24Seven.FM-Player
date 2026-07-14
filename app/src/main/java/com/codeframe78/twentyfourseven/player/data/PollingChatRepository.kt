package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatRepository
import com.codeframe78.twentyfourseven.player.domain.ChatState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class PollingChatRepository internal constructor(
    private val remote: ChatRemoteDataSource,
    private val pollIntervalMillis: Long = MINIMUM_POLL_INTERVAL_MILLIS,
    private val elapsedRealtimeMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) : ChatRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<ChatState>>()
    private val locks = ConcurrentHashMap<StationId, Mutex>()
    private val lastAttempts = ConcurrentHashMap<StationId, Long>()

    init {
        require(pollIntervalMillis >= MINIMUM_POLL_INTERVAL_MILLIS)
    }

    override fun observeChat(stationId: StationId): Flow<ChatState> = channelFlow {
        val state = state(stationId)
        if (state.value.status == ChatLoadStatus.Unavailable) {
            state.value = state.value.copy(status = ChatLoadStatus.Loading)
        }
        val forwarding = launch { state.collect { send(it) } }
        try {
            while (currentCoroutineContext().isActive) {
                refresh(stationId)
                delay(pollIntervalMillis)
            }
        } finally {
            forwarding.cancel()
        }
    }

    override suspend fun refresh(stationId: StationId) {
        lock(stationId).withLock {
            val now = elapsedRealtimeMillis()
            val lastAttempt = lastAttempts[stationId]
            if (lastAttempt != null && now - lastAttempt < MINIMUM_POLL_INTERVAL_MILLIS) return
            lastAttempts[stationId] = now
            val state = state(stationId)
            if (state.value.status != ChatLoadStatus.Ready) {
                state.value = state.value.copy(status = ChatLoadStatus.Loading, errorMessage = null)
            }
            runCatching { remote.fetch(stationId) }
                .onSuccess { messages ->
                    state.value = ChatState(stationId, ChatLoadStatus.Ready, messages)
                }
                .onFailure {
                    if (state.value.status != ChatLoadStatus.Ready) {
                        state.value = ChatState(
                            stationId,
                            ChatLoadStatus.Error,
                            errorMessage = "Chat could not be refreshed.",
                        )
                    }
                }
        }
    }

    override suspend fun sendMessage(stationId: StationId, message: String) {
        lock(stationId).withLock {
            val value = message.trim()
            val state = state(stationId)
            val validationError = when {
                value.isEmpty() -> "Enter a message."
                value.length > MAX_MESSAGE_CHARACTERS -> "Messages can be up to 255 characters."
                !StandardCharsets.ISO_8859_1.newEncoder().canEncode(value) ->
                    "This station cannot send one or more characters in that message."
                else -> null
            }
            if (validationError != null) {
                state.value = state.value.copy(sendErrorMessage = validationError)
                return
            }
            state.value = state.value.copy(isSending = true, sendErrorMessage = null)
            runCatching { remote.send(stationId, value) }
                .onSuccess { messages ->
                    lastAttempts[stationId] = elapsedRealtimeMillis()
                    state.value = ChatState(stationId, ChatLoadStatus.Ready, messages)
                }
                .onFailure {
                    state.value = state.value.copy(
                        isSending = false,
                        sendErrorMessage = "Message could not be sent. Confirm that you are signed in to this station.",
                    )
                }
        }
    }

    private fun state(stationId: StationId): MutableStateFlow<ChatState> =
        states.getOrPut(stationId) { MutableStateFlow(ChatState(stationId)) }

    private fun lock(stationId: StationId) = locks.getOrPut(stationId, ::Mutex)

    internal companion object {
        const val MINIMUM_POLL_INTERVAL_MILLIS = 30_000L
        const val MAX_MESSAGE_CHARACTERS = 255
    }
}
