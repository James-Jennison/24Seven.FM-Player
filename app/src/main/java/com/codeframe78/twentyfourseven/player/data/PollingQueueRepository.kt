package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueRepository
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class PollingQueueRepository internal constructor(
    private val remote: QueueRemoteDataSource,
    private val pollIntervalMillis: Long = MINIMUM_POLL_INTERVAL_MILLIS,
    private val elapsedRealtimeMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) : QueueRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<QueueState>>()
    private val locks = ConcurrentHashMap<StationId, Mutex>()
    private val lastAttempts = ConcurrentHashMap<StationId, Long>()

    constructor() : this(PlayerQueueRemoteDataSource())

    init {
        require(pollIntervalMillis >= MINIMUM_POLL_INTERVAL_MILLIS)
    }

    override fun observeQueue(stationId: StationId): Flow<QueueState> = channelFlow {
        val state = state(stationId)
        if (state.value.status == QueueLoadStatus.Unavailable) {
            state.value = state.value.copy(status = QueueLoadStatus.Loading)
        }
        val forwarding = launch { state.collect { send(it) } }
        try {
            while (currentCoroutineContext().isActive) {
                refresh(stationId)
                delay(pollIntervalMillis)
            }
        } finally {
            forwarding.cancel()
        }
    }

    override suspend fun refresh(stationId: StationId) {
        locks.getOrPut(stationId, ::Mutex).withLock {
            val now = elapsedRealtimeMillis()
            val lastAttempt = lastAttempts[stationId]
            if (lastAttempt != null && now - lastAttempt < MINIMUM_POLL_INTERVAL_MILLIS) return
            lastAttempts[stationId] = now
            val state = state(stationId)
            if (state.value.status != QueueLoadStatus.Ready) {
                state.value = state.value.copy(status = QueueLoadStatus.Loading, errorMessage = null)
            }
            runCatching { remote.fetch(stationId) }
                .onSuccess { payload ->
                    state.value = QueueState(
                        stationId = stationId,
                        status = QueueLoadStatus.Ready,
                        upcoming = payload.upcoming,
                        recentlyPlayed = payload.recentlyPlayed,
                    )
                }
                .onFailure {
                    if (state.value.status != QueueLoadStatus.Ready) {
                        state.value = QueueState(
                            stationId = stationId,
                            status = QueueLoadStatus.Error,
                            errorMessage = "Queue and history could not be refreshed.",
                        )
                    }
                }
        }
    }

    private fun state(stationId: StationId): MutableStateFlow<QueueState> =
        states.getOrPut(stationId) { MutableStateFlow(QueueState(stationId)) }

    internal companion object {
        const val MINIMUM_POLL_INTERVAL_MILLIS = 60_000L
    }
}
