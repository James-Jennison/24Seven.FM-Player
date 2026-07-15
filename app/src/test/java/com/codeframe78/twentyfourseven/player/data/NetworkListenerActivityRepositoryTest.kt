package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.RequestHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkListenerActivityRepositoryTest {
    private val stationId = StationId("sst")

    @Test
    fun `refresh publishes immutable listener activity snapshot`() = runTest {
        val snapshot = ListenerActivitySnapshot(
            membershipTier = MembershipTier.Vip,
            requestReadiness = RequestReadiness.Ready,
            waitMinutes = 0,
            recentRequests = listOf(RequestHistoryEntry(1, "Album - Track - Artist", "14 Jul 26 - 17:38")),
        )
        val repository = NetworkListenerActivityRepository(FakeRemote(snapshot = snapshot))

        repository.refresh(stationId)

        val state = repository.observeActivity(stationId).first()
        assertEquals(ListenerActivityLoadStatus.Ready, state.status)
        assertEquals(MembershipTier.Vip, state.membershipTier)
        assertEquals(RequestReadiness.Ready, state.requestReadiness)
        assertEquals(0, state.waitMinutes)
        assertEquals(snapshot.recentRequests, state.recentRequests)
    }

    @Test
    fun `authentication failure is specific and clear removes station data`() = runTest {
        val remote = FakeRemote(failure = ListenerActivityAuthenticationRequiredException())
        val repository = NetworkListenerActivityRepository(remote)

        repository.refresh(stationId)

        val failed = repository.observeActivity(stationId).first()
        assertEquals(ListenerActivityLoadStatus.Error, failed.status)
        assertEquals(
            "Sign in to this station to load request activity and membership status.",
            failed.errorMessage,
        )

        repository.clear(stationId)
        assertEquals(ListenerActivityLoadStatus.Idle, repository.observeActivity(stationId).first().status)
    }

    private class FakeRemote(
        private val snapshot: ListenerActivitySnapshot? = null,
        private val failure: Throwable? = null,
    ) : ListenerActivityRemoteDataSource {
        override suspend fun load(stationId: StationId): ListenerActivitySnapshot {
            failure?.let { throw it }
            return checkNotNull(snapshot)
        }
    }
}
