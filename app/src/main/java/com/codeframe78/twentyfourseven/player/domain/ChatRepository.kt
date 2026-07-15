package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

enum class ChatLoadStatus { Unavailable, Loading, Ready, Error }

data class ChatMessage(
    val authorDisplayName: String,
    val messageText: String,
    val postedAtLabel: String? = null,
    val parts: List<ChatMessagePart> = listOf(ChatMessagePart.Text(messageText)),
)

sealed interface ChatMessagePart {
    data class Text(val value: String) : ChatMessagePart

    data class Emoticon(
        val altText: String,
        val imageUrl: String,
    ) : ChatMessagePart
}

data class ChatState(
    val stationId: StationId,
    val status: ChatLoadStatus = ChatLoadStatus.Unavailable,
    val messages: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val isSending: Boolean = false,
    val sendErrorMessage: String? = null,
)

interface ChatRepository {
    fun observeChat(stationId: StationId): Flow<ChatState>

    suspend fun refresh(stationId: StationId)

    suspend fun sendMessage(stationId: StationId, message: String)
}
