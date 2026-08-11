package com.codeframe78.twentyfourseven.player.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.codeframe78.twentyfourseven.player.RadioApplication
import com.codeframe78.twentyfourseven.player.R
import com.codeframe78.twentyfourseven.player.domain.AuthRepository
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMentionSnapshot
import com.codeframe78.twentyfourseven.player.domain.ChatRepository
import com.codeframe78.twentyfourseven.player.domain.CommunityNotificationRepository
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyRepository
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import com.codeframe78.twentyfourseven.player.domain.toSupportedStationIdOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * A visible, user-started monitor for one opted-in station chat. It neither starts
 * at boot nor includes Chat text in the permanent Android notification.
 */
class ChatMentionMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    private val container get() = (application as RadioApplication).appContainer
    private val monitor by lazy {
        ForegroundChatMentionMonitor(
            chat = container.chatRepository,
            auth = container.authRepository,
            safety = container.communitySafetyRepository,
            notifications = container.communityNotificationRepository,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopFromUserAction(intent.stationId())
            return START_NOT_STICKY
        }

        val stationId = intent?.stationId() ?: runBlocking {
            container.communityNotificationRepository.observeSettings().first().foregroundMonitorStationId
        } ?: run {
            stopMonitoring()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, monitorNotification(stationId))
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive && monitor.refresh(stationId)) {
                delay(POLL_INTERVAL_MILLIS)
            }
            if (container.communityNotificationRepository.observeSettings().first().foregroundMonitorEnabled(stationId)) {
                container.communityNotificationRepository.setForegroundChatMonitorEnabled(stationId, false)
            }
            stopMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopFromUserAction(stationId: StationId?) {
        monitorJob?.cancel()
        if (stationId == null) {
            stopMonitoring()
            return
        }
        monitorJob = serviceScope.launch {
            container.communityNotificationRepository.setForegroundChatMonitorEnabled(stationId, false)
            stopMonitoring()
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun monitorNotification(stationId: StationId): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                MONITOR_CHANNEL_ID,
                "Chat mention monitor",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps an opted-in station chat monitor active"
                setShowBadge(false)
            },
        )
        val stopIntent = PendingIntent.getService(
            this,
            stationId.value.hashCode(),
            stopIntent(this, stationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon_monochrome)
            .setContentTitle("Monitoring chat mentions")
            .setContentText("${stationLabel(stationId)} · checks about once a minute")
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
            .build()
    }

    private fun Intent.stationId(): StationId? =
        getStringExtra(EXTRA_STATION_ID)?.toSupportedStationIdOrNull()

    companion object {
        private const val ACTION_START = "com.codeframe78.twentyfourseven.player.action.START_CHAT_MENTION_MONITOR"
        private const val ACTION_STOP = "com.codeframe78.twentyfourseven.player.action.STOP_CHAT_MENTION_MONITOR"
        private const val EXTRA_STATION_ID = "chat_mention_monitor_station_id"
        private const val MONITOR_CHANNEL_ID = "community_chat_monitor"
        private const val NOTIFICATION_ID = 3107
        private const val POLL_INTERVAL_MILLIS = 60_000L

        fun startIntent(context: Context, stationId: StationId): Intent =
            Intent(context, ChatMentionMonitorService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_STATION_ID, stationId.canonicalized().value)

        fun stopIntent(context: Context, stationId: StationId): Intent =
            Intent(context, ChatMentionMonitorService::class.java)
                .setAction(ACTION_STOP)
                .putExtra(EXTRA_STATION_ID, stationId.canonicalized().value)
    }
}

/** Runs one refresh and returns false when the monitor must stop. */
internal class ForegroundChatMentionMonitor(
    private val chat: ChatRepository,
    private val auth: AuthRepository,
    private val safety: CommunitySafetyRepository,
    private val notifications: CommunityNotificationRepository,
) {
    suspend fun refresh(stationId: StationId): Boolean {
        val canonicalStationId = stationId.canonicalized()
        val settings = notifications.observeSettings().first()
        if (
            !settings.foregroundMonitorEnabled(canonicalStationId) ||
            !settings.chatMentionsEnabled(canonicalStationId)
        ) return false

        val communitySafety = safety.observeSafety().first()
        if (!communitySafety.canViewCommunityContent) return false

        auth.restoreSession(canonicalStationId)
        val authState = auth.observeAuth(canonicalStationId).first()
        val displayName = authState.displayName
        if (authState.status != AuthStatus.SignedIn || displayName.isNullOrBlank()) return false

        chat.refresh(canonicalStationId)
        val chatState = chat.observeCachedChat(canonicalStationId).first()
        if (chatState.status == ChatLoadStatus.Ready) {
            notifications.processChatSnapshot(
                ChatMentionSnapshot(
                    stationId = canonicalStationId,
                    stationName = stationLabel(canonicalStationId),
                    signedInDisplayName = displayName,
                    messages = chatState.messages,
                    blockedAuthorDisplayNames = communitySafety.blockedUsers
                        .asSequence()
                        .filter { it.stationId == canonicalStationId }
                        .map { it.displayName }
                        .toSet(),
                ),
            )
        }
        return true
    }
}

private fun stationLabel(stationId: StationId): String = when (stationId.canonicalized().value) {
    "sst" -> "StreamingSoundtracks.com"
    "1980s" -> "80s.FM"
    "afm" -> "Adagio.FM"
    "dfm" -> "Death.FM"
    "efm" -> "Entranced.FM"
    else -> "Station chat"
}
