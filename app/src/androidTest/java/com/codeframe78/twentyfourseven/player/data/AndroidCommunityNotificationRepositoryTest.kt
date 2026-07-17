package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codeframe78.twentyfourseven.player.domain.ChatMentionEvent
import com.codeframe78.twentyfourseven.player.domain.ChatMentionSnapshot
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidCommunityNotificationRepositoryTest {
    @Test
    fun stationOptInPersistsAndOnlyNewExactMentionsNotify() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferencesName = "m27-community-notification-test"
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val events = mutableListOf<ChatMentionEvent>()
        try {
            val repository = AndroidCommunityNotificationRepository(
                context,
                notifier = events::add,
                preferencesName = preferencesName,
            )
            val stationId = StationId("sst")
            repository.setChatMentionsEnabled(stationId, true)
            repository.processChatSnapshot(snapshot(ChatMessage("Listener", "old MorG mention", "12:00")))
            repository.processChatSnapshot(
                snapshot(
                    ChatMessage("Listener", "old MorG mention", "12:00"),
                    ChatMessage("New Listener", "Hello, morg!", "12:01"),
                    ChatMessage("Other", "MorgHubby is here", "12:02"),
                ).copy(blockedAuthorDisplayNames = setOf("New Listener")),
            )
            repository.processChatSnapshot(
                snapshot(
                    ChatMessage("Listener", "old MorG mention", "12:00"),
                    ChatMessage("New Listener", "Hello, morg!", "12:01"),
                ),
            )
            repository.processChatSnapshot(
                snapshot(ChatMessage("Fresh Listener", "MorG: this one is new", "12:03")),
            )

            assertEquals(listOf(ChatMentionEvent(stationId, "SST", "Fresh Listener")), events)
            assertTrue(
                AndroidCommunityNotificationRepository(context, events::add, preferencesName)
                    .observeSettings().first().chatMentionsEnabled(stationId),
            )
            assertFalse(repository.observeSettings().first().chatMentionsEnabled(StationId("adagio")))
        } finally {
            preferences.edit().clear().commit()
        }
    }

    private fun snapshot(vararg messages: ChatMessage) = ChatMentionSnapshot(
        StationId("sst"),
        "SST",
        "MorG",
        messages.toList(),
    )
}
