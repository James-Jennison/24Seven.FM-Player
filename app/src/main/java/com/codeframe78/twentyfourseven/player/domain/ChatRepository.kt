package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

enum class ChatLoadStatus { Unavailable, Loading, Ready, Error }

data class ChatMessage(
    val authorDisplayName: String,
    val messageText: String,
    val postedAtLabel: String? = null,
    val parts: List<ChatMessagePart> = listOf(ChatMessagePart.Text(messageText)),
    val authorRole: ChatRole = ChatRole.Member,
)

/** The station staff roles its chat colours by, as named in the station's own legend. */
enum class ChatRole(val label: String?) {
    Member(null),
    Proprietor("Proprietor"),
    Administrator("Administrator"),
    Moderator("Moderator"),
    Ambassador("Ambassador"),
    VisitorMod("Visitor Mod"),
    Composer("Composer"),
}

sealed interface ChatMessagePart {
    data class Text(val value: String) : ChatMessagePart

    /**
     * A station emoticon image. [widthPx] and [heightPx] are the sizes the station declares for it, so wide
     * animated ones keep their shape when drawn inline; both default to the classic square smiley.
     */
    data class Emoticon(
        val altText: String,
        val imageUrl: String,
        val widthPx: Int = DEFAULT_SIZE_PX,
        val heightPx: Int = DEFAULT_SIZE_PX,
    ) : ChatMessagePart {
        val aspectRatio: Float get() = widthPx.toFloat() / heightPx.toFloat()

        companion object {
            const val DEFAULT_SIZE_PX = 20
        }
    }
}

/** The station's smiley catalog as its own chat picker page lists it: tabs of grouped choices. */
data class ChatEmoticonCatalog(val tabs: List<ChatEmoticonTab>)

data class ChatEmoticonTab(val id: String, val label: String, val groups: List<ChatEmoticonGroup>)

data class ChatEmoticonGroup(val label: String, val choices: List<ChatEmoticonChoice>)

/** One pickable entry. [code] goes into the draft; [image] is null when the code is itself the glyph (an emoji). */
data class ChatEmoticonChoice(
    val code: String,
    val label: String,
    val image: ChatMessagePart.Emoticon? = null,
)

data class ChatState(
    val stationId: StationId,
    val status: ChatLoadStatus = ChatLoadStatus.Unavailable,
    val messages: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val isSending: Boolean = false,
    val sendErrorMessage: String? = null,
    val emoticons: ChatEmoticonCatalog? = null,
    val isLoadingEmoticons: Boolean = false,
    val emoticonsErrorMessage: String? = null,
)

interface ChatRepository {
    fun observeChat(stationId: StationId): Flow<ChatState>

    /** Observes cached state without creating a second polling loop. */
    fun observeCachedChat(stationId: StationId): Flow<ChatState>

    suspend fun refresh(stationId: StationId)

    suspend fun sendMessage(stationId: StationId, message: String)

    /** Loads the station's smiley catalog into the state; a no-op while a catalog is cached or loading. */
    suspend fun loadEmoticons(stationId: StationId)
}
