package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.MemberProfileState
import com.codeframe78.twentyfourseven.player.domain.MemberProfileStatus
import com.codeframe78.twentyfourseven.player.domain.PLAYED_HISTORY_BLOCK_HOURS
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryState
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryStatus
import com.codeframe78.twentyfourseven.player.domain.StationExtrasRepository
import com.codeframe78.twentyfourseven.player.domain.StationExtrasState
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationNewsStatus
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

internal class NetworkStationExtrasRepository(
    private val remote: ExtrasRemoteDataSource,
) : StationExtrasRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<StationExtrasState>>()

    override fun observeExtras(stationId: StationId): Flow<StationExtrasState> =
        state(stationId.canonicalized()).asStateFlow()

    override suspend fun openProfile(stationId: StationId, username: String) {
        val name = username.trim()
        if (name.isEmpty()) return
        val state = state(stationId.canonicalized())
        state.update { it.copy(profile = MemberProfileState(MemberProfileStatus.Loading, name)) }
        val loaded = try {
            val profile = remote.profile(state.value.stationId, name)
            if (profile == null) {
                MemberProfileState(MemberProfileStatus.NotFound, name)
            } else {
                MemberProfileState(MemberProfileStatus.Ready, name, profile)
            }
        } catch (cancellation: CancellationException) {
            state.update { if (it.profile.isLoading(name)) it.copy(profile = MemberProfileState()) else it }
            throw cancellation
        } catch (_: Exception) {
            MemberProfileState(MemberProfileStatus.Error, name)
        }
        // A card the listener already closed, or replaced with another member's, is left alone.
        state.update { if (it.profile.isLoading(name)) it.copy(profile = loaded) else it }
    }

    override suspend fun closeProfile(stationId: StationId) {
        state(stationId.canonicalized()).update { it.copy(profile = MemberProfileState()) }
    }

    override suspend fun loadHistory(stationId: StationId, date: LocalDate, startHour: Int) {
        val block = startHour.coerceIn(0, 24 - PLAYED_HISTORY_BLOCK_HOURS)
            .let { it - it % PLAYED_HISTORY_BLOCK_HOURS }
        val state = state(stationId.canonicalized())
        state.update { it.copy(history = PlayedHistoryState(PlayedHistoryStatus.Loading, date, block)) }
        val loaded = try {
            PlayedHistoryState(
                PlayedHistoryStatus.Ready,
                date,
                block,
                remote.history(state.value.stationId, date, block),
            )
        } catch (cancellation: CancellationException) {
            state.update { if (it.history.isLoading(date, block)) it.copy(history = PlayedHistoryState()) else it }
            throw cancellation
        } catch (_: Exception) {
            PlayedHistoryState(PlayedHistoryStatus.Error, date, block)
        }
        state.update { if (it.history.isLoading(date, block)) it.copy(history = loaded) else it }
    }

    override suspend fun closeHistory(stationId: StationId) {
        state(stationId.canonicalized()).update { it.copy(history = PlayedHistoryState()) }
    }

    override suspend fun refreshNews(stationId: StationId) {
        val state = state(stationId.canonicalized())
        if (state.value.news.status == StationNewsStatus.Loading) return
        state.update { it.copy(news = it.news.copy(status = StationNewsStatus.Loading)) }
        try {
            val stories = remote.news(state.value.stationId)
            state.update { it.copy(news = it.news.copy(status = StationNewsStatus.Ready, stories = stories)) }
        } catch (cancellation: CancellationException) {
            state.update { it.copy(news = it.news.copy(status = StationNewsStatus.Idle)) }
            throw cancellation
        } catch (_: Exception) {
            state.update { it.copy(news = it.news.copy(status = StationNewsStatus.Error)) }
        }
    }

    private fun MemberProfileState.isLoading(name: String) =
        status == MemberProfileStatus.Loading && requestedName == name

    private fun PlayedHistoryState.isLoading(date: LocalDate, block: Int) =
        status == PlayedHistoryStatus.Loading && this.date == date && startHour == block

    private fun state(stationId: StationId): MutableStateFlow<StationExtrasState> =
        states.getOrPut(stationId) { MutableStateFlow(StationExtrasState(stationId)) }
}
