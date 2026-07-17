package com.codeframe78.twentyfourseven.player.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.codeframe78.twentyfourseven.player.MainActivity
import com.codeframe78.twentyfourseven.player.R
import com.codeframe78.twentyfourseven.player.domain.ChatMentionEvent
import com.codeframe78.twentyfourseven.player.domain.ChatMentionSnapshot
import com.codeframe78.twentyfourseven.player.domain.ChatMentionTracker
import com.codeframe78.twentyfourseven.player.domain.CommunityNotificationRepository
import com.codeframe78.twentyfourseven.player.domain.CommunityNotificationState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidCommunityNotificationRepository internal constructor(
    context: Context,
    private val notifier: ChatMentionNotifier = AndroidChatMentionNotifier(context.applicationContext),
    preferencesName: String = PREFERENCES_NAME,
) : CommunityNotificationRepository {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val settings = MutableStateFlow(readSettings())
    private val trackers = mutableMapOf<StationId, ChatMentionTracker>()

    override fun observeSettings(): Flow<CommunityNotificationState> = settings.asStateFlow()

    override suspend fun setChatMentionsEnabled(stationId: StationId, enabled: Boolean) {
        val updatedIds = settings.value.chatMentionStationIds.toMutableSet().apply {
            if (enabled) add(stationId) else remove(stationId)
        }
        settings.value = CommunityNotificationState(updatedIds)
        preferences.edit().putStringSet(KEY_CHAT_MENTION_STATIONS, updatedIds.map { it.value }.toSet()).apply()
        if (!enabled) trackers.remove(stationId)
    }

    override fun processChatSnapshot(snapshot: ChatMentionSnapshot) {
        if (!settings.value.chatMentionsEnabled(snapshot.stationId)) return
        val tracker = trackers.getOrPut(snapshot.stationId, ::ChatMentionTracker)
        tracker.accept(snapshot).forEach(notifier::show)
    }

    private fun readSettings(): CommunityNotificationState = CommunityNotificationState(
        chatMentionStationIds = preferences.getStringSet(KEY_CHAT_MENTION_STATIONS, emptySet()).orEmpty()
            .mapNotNull { value -> value.takeIf(String::isNotBlank)?.let(::StationId) }
            .toSet(),
    )

    internal companion object {
        const val EXTRA_CHAT_STATION_ID = "community_chat_station_id"
        private const val PREFERENCES_NAME = "community_notifications"
        private const val KEY_CHAT_MENTION_STATIONS = "chat_mention_stations"
    }
}

internal fun interface ChatMentionNotifier {
    fun show(event: ChatMentionEvent)
}

private class AndroidChatMentionNotifier(
    private val context: Context,
) : ChatMentionNotifier {
    private val manager = context.getSystemService(NotificationManager::class.java)

    override fun show(event: ChatMentionEvent) {
        ensureChannel()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(AndroidCommunityNotificationRepository.EXTRA_CHAT_STATION_ID, event.stationId.value)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            event.stationId.value.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon_monochrome)
            .setContentTitle("${event.stationName} Chat mention")
            .setContentText("${event.authorDisplayName} mentioned you in Chat.")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        manager.notify(event.stationId.value.hashCode(), notification)
    }

    private fun ensureChannel() {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Chat mentions",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Mentions of your signed-in station name in Chat"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    private companion object {
        const val CHANNEL_ID = "community_chat_mentions"
    }
}
