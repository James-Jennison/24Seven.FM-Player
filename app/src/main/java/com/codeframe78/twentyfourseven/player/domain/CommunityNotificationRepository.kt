package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class CommunityNotificationState(
    val chatMentionStationIds: Set<StationId> = emptySet(),
) {
    fun chatMentionsEnabled(stationId: StationId?): Boolean =
        stationId != null && stationId in chatMentionStationIds
}

data class ChatMentionSnapshot(
    val stationId: StationId,
    val stationName: String,
    val signedInDisplayName: String,
    val messages: List<ChatMessage>,
    val blockedAuthorDisplayNames: Set<String> = emptySet(),
)

interface CommunityNotificationRepository {
    fun observeSettings(): Flow<CommunityNotificationState>

    suspend fun setChatMentionsEnabled(stationId: StationId, enabled: Boolean)

    fun processChatSnapshot(snapshot: ChatMentionSnapshot)
}

object UnavailableCommunityNotificationRepository : CommunityNotificationRepository {
    override fun observeSettings(): Flow<CommunityNotificationState> = flowOf(CommunityNotificationState())

    override suspend fun setChatMentionsEnabled(stationId: StationId, enabled: Boolean) = Unit

    override fun processChatSnapshot(snapshot: ChatMentionSnapshot) = Unit
}

data class ChatMentionEvent(
    val stationId: StationId,
    val stationName: String,
    val authorDisplayName: String,
)

class ChatMentionTracker(
    private val maximumRememberedMessagesPerStation: Int = 200,
) {
    private val initializedStations = mutableSetOf<StationId>()
    private val seenFingerprints = mutableMapOf<StationId, LinkedHashSet<String>>()

    init {
        require(maximumRememberedMessagesPerStation > 0)
    }

    fun accept(snapshot: ChatMentionSnapshot): List<ChatMentionEvent> {
        val fingerprints = snapshot.messages.map(ChatMessage::mentionFingerprint)
        val seen = seenFingerprints.getOrPut(snapshot.stationId, ::linkedSetOf)
        if (initializedStations.add(snapshot.stationId)) {
            retain(seen, fingerprints)
            return emptyList()
        }

        val signedInIdentity = snapshot.signedInDisplayName.normalizedCommunityIdentity()
        val blockedIdentities = snapshot.blockedAuthorDisplayNames
            .map(String::normalizedCommunityIdentity)
            .toSet()
        val events = snapshot.messages.mapIndexedNotNull { index, message ->
            val fingerprint = fingerprints[index]
            if (fingerprint in seen) return@mapIndexedNotNull null
            val authoredBySignedInUser = message.authorDisplayName.normalizedCommunityIdentity() == signedInIdentity
            val authoredByBlockedUser = message.authorDisplayName.normalizedCommunityIdentity() in blockedIdentities
            if (
                !authoredBySignedInUser &&
                !authoredByBlockedUser &&
                message.messageText.containsExactDisplayName(snapshot.signedInDisplayName)
            ) {
                ChatMentionEvent(snapshot.stationId, snapshot.stationName, message.authorDisplayName)
            } else {
                null
            }
        }
        retain(seen, fingerprints)
        return events
    }

    private fun retain(seen: LinkedHashSet<String>, fingerprints: List<String>) {
        fingerprints.forEach(seen::add)
        while (seen.size > maximumRememberedMessagesPerStation) {
            seen.remove(seen.first())
        }
    }
}

internal fun String.containsExactDisplayName(displayName: String): Boolean {
    val name = displayName.trim()
    if (name.isEmpty()) return false
    val identityCharacter = "[\\p{L}\\p{N}_]"
    return Regex(
        "(?<!$identityCharacter)${Regex.escape(name)}(?!$identityCharacter)",
        RegexOption.IGNORE_CASE,
    ).containsMatchIn(this)
}

private fun ChatMessage.mentionFingerprint(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(
            listOf(authorDisplayName, postedAtLabel.orEmpty(), messageText)
                .joinToString(separator = "\u0000")
                .toByteArray(StandardCharsets.UTF_8),
        )
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
