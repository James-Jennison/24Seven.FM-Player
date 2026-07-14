package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatRepository
import com.codeframe78.twentyfourseven.player.domain.ChatState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UnavailableChatRepository : ChatRepository {
    override fun observeChat(stationId: StationId): Flow<ChatState> = flowOf(ChatState(stationId))

    override suspend fun refresh(stationId: StationId) = Unit

    override suspend fun sendMessage(stationId: StationId, message: String) = Unit
}
