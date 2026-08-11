package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AbuseReportState
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSubmission
import com.codeframe78.twentyfourseven.player.domain.AbuseReportTarget
import com.codeframe78.twentyfourseven.player.domain.AgeGateStatus
import com.codeframe78.twentyfourseven.player.domain.AuthRepository
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMentionSnapshot
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.ChatRepository
import com.codeframe78.twentyfourseven.player.domain.ChatState
import com.codeframe78.twentyfourseven.player.domain.CommunityNotificationRepository
import com.codeframe78.twentyfourseven.player.domain.CommunityNotificationState
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyRepository
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundChatMentionMonitorTest {
    @Test
    fun `refreshes and processes a snapshot only for opted-in signed-in station`() = runTest {
        val stationId = StationId("sst")
        val chat = FakeChatRepository(
            ChatState(stationId, ChatLoadStatus.Ready, listOf(ChatMessage("Friend", "Hello Listener"))),
        )
        val auth = FakeAuthRepository(AuthState(stationId, AuthStatus.SignedIn, "Listener"))
        val notifications = FakeNotifications(stationId)
        val monitor = ForegroundChatMentionMonitor(chat, auth, FakeSafetyRepository(visibleSafety()), notifications)

        assertTrue(monitor.refresh(stationId))
        assertEquals(stationId, auth.restoredStation)
        assertEquals(stationId, chat.refreshedStation)
        assertEquals(1, notifications.snapshots.size)
        assertEquals("Listener", notifications.snapshots.single().signedInDisplayName)
    }

    @Test
    fun `stops without fetching when closed-app monitor is disabled`() = runTest {
        val stationId = StationId("sst")
        val chat = FakeChatRepository(ChatState(stationId))
        val monitor = ForegroundChatMentionMonitor(
            chat,
            FakeAuthRepository(AuthState(stationId, AuthStatus.SignedIn, "Listener")),
            FakeSafetyRepository(visibleSafety()),
            FakeNotifications(stationId, monitorEnabled = false),
        )

        assertFalse(monitor.refresh(stationId))
        assertEquals(null, chat.refreshedStation)
    }

    private fun visibleSafety() = CommunitySafetyState(
        ageGateStatus = AgeGateStatus.Adult,
        acceptedTermsVersion = "2026-07-18",
        communityContentVisible = true,
    )

    private class FakeChatRepository(initial: ChatState) : ChatRepository {
        private val state = MutableStateFlow(initial)
        var refreshedStation: StationId? = null

        override fun observeChat(stationId: StationId): Flow<ChatState> = state

        override fun observeCachedChat(stationId: StationId): Flow<ChatState> = state

        override suspend fun refresh(stationId: StationId) {
            refreshedStation = stationId
        }

        override suspend fun sendMessage(stationId: StationId, message: String) = Unit
    }

    private class FakeAuthRepository(private val state: AuthState) : AuthRepository {
        var restoredStation: StationId? = null

        override fun observeAuth(stationId: StationId): Flow<AuthState> = flowOf(state)

        override suspend fun restoreSession(stationId: StationId) {
            restoredStation = stationId
        }

        override suspend fun refreshChallenge(stationId: StationId) = Unit

        override suspend fun signIn(stationId: StationId, username: String, password: String, securityCode: String) = Unit

        override suspend fun signOut(stationId: StationId) = Unit
    }

    private class FakeSafetyRepository(private val state: CommunitySafetyState) : CommunitySafetyRepository {
        override fun observeSafety(): Flow<CommunitySafetyState> = flowOf(state)

        override fun observeReport(): Flow<AbuseReportState> = flowOf(AbuseReportState())

        override suspend fun submitAgeScreen(year: Int, month: Int, day: Int) = Unit

        override suspend fun acceptTerms() = Unit

        override suspend fun setCommunityContentVisible(visible: Boolean) = Unit

        override suspend fun blockUser(stationId: StationId, displayName: String) = Unit

        override suspend fun unblockUser(stationId: StationId, displayName: String) = Unit

        override suspend fun beginReport(stationId: StationId, target: AbuseReportTarget) = Unit

        override suspend fun submitReport(submission: AbuseReportSubmission) = Unit

        override fun reportEmailComposerResult(opened: Boolean) = Unit

        override fun dismissReport() = Unit
    }

    private class FakeNotifications(stationId: StationId, monitorEnabled: Boolean = true) : CommunityNotificationRepository {
        private val settings = CommunityNotificationState(
            chatMentionStationIds = setOf(stationId),
            foregroundMonitorStationId = if (monitorEnabled) stationId else null,
        )
        val snapshots = mutableListOf<ChatMentionSnapshot>()

        override fun observeSettings(): Flow<CommunityNotificationState> = flowOf(settings)

        override suspend fun setChatMentionsEnabled(stationId: StationId, enabled: Boolean) = Unit

        override fun processChatSnapshot(snapshot: ChatMentionSnapshot) {
            snapshots += snapshot
        }
    }
}
