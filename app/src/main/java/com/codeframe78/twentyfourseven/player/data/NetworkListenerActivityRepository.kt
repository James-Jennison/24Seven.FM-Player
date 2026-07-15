package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityRepository
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class NetworkListenerActivityRepository(
    private val remote: ListenerActivityRemoteDataSource,
) : ListenerActivityRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<ListenerActivityState>>()
    private val locks = ConcurrentHashMap<StationId, Mutex>()

    override fun observeActivity(stationId: StationId): Flow<ListenerActivityState> =
        state(stationId).asStateFlow()

    override suspend fun refresh(stationId: StationId): Unit = lock(stationId).withLock {
        update(stationId) { it.copy(status = ListenerActivityLoadStatus.Loading, errorMessage = null) }
        runCatching { remote.load(stationId) }
            .onSuccess { snapshot ->
                update(stationId) {
                    ListenerActivityState(
                        stationId = stationId,
                        status = ListenerActivityLoadStatus.Ready,
                        membershipTier = snapshot.membershipTier,
                        requestReadiness = snapshot.requestReadiness,
                        waitMinutes = snapshot.waitMinutes,
                        recentRequests = snapshot.recentRequests,
                    )
                }
            }
            .onFailure { failure ->
                update(stationId) {
                    it.copy(
                        status = ListenerActivityLoadStatus.Error,
                        errorMessage = if (failure is ListenerActivityAuthenticationRequiredException) {
                            "Sign in to this station to load request activity and membership status."
                        } else {
                            "Request activity could not be loaded right now."
                        },
                    )
                }
            }
        Unit
    }

    override suspend fun clear(stationId: StationId) = lock(stationId).withLock {
        state(stationId).value = ListenerActivityState(stationId)
    }

    private fun state(stationId: StationId) = states.getOrPut(stationId) {
        MutableStateFlow(ListenerActivityState(stationId))
    }

    private fun lock(stationId: StationId) = locks.getOrPut(stationId, ::Mutex)

    private fun update(stationId: StationId, transform: (ListenerActivityState) -> ListenerActivityState) {
        state(stationId).value = transform(state(stationId).value)
    }
}

class UnavailableListenerActivityRepository : ListenerActivityRepository {
    override fun observeActivity(stationId: StationId): Flow<ListenerActivityState> =
        kotlinx.coroutines.flow.flowOf(ListenerActivityState(stationId))

    override suspend fun refresh(stationId: StationId) = Unit
    override suspend fun clear(stationId: StationId) = Unit
}
