package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
import com.codeframe78.twentyfourseven.player.domain.PlaybackController
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.NowPlayingRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueRepository
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selection and playback actions are delegated through domain contracts`() = runTest(dispatcher) {
        val stations = BootstrapStationRepository()
        val playback = FakePlaybackController()
        val queue = FakeQueueRepository()
        val viewModel = MainViewModel(stations, playback, FakeNowPlayingRepository(), queue)
        advanceUntilIdle()

        assertEquals("sst", playback.selectedStation?.id?.value)

        viewModel.selectStation(StationId("adagio"))
        viewModel.play()
        viewModel.pause()
        viewModel.stop()
        viewModel.refreshQueue()
        advanceUntilIdle()

        assertEquals("adagio", playback.selectedStation?.id?.value)
        assertEquals(1, playback.playCalls)
        assertEquals(1, playback.pauseCalls)
        assertEquals(1, playback.stopCalls)
        assertEquals(StationId("adagio"), queue.refreshedStation)
    }

    @Test
    fun `now playing title updates remain scoped to the selected station`() = runTest(dispatcher) {
        val nowPlaying = FakeNowPlayingRepository()
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            nowPlaying,
            FakeQueueRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        nowPlaying.state.value = NowPlayingState(StationId("sst"), "First raw title")
        advanceUntilIdle()
        assertEquals("First raw title", viewModel.uiState.value.nowPlaying.displayTitle)

        nowPlaying.state.value = NowPlayingState(StationId("sst"), "Updated raw title")
        advanceUntilIdle()
        assertEquals("Updated raw title", viewModel.uiState.value.nowPlaying.displayTitle)

        viewModel.selectStation(StationId("adagio"))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.nowPlaying.displayTitle)
    }

    @Test
    fun `destination selection is exposed as immutable ui state`() = runTest(dispatcher) {
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            FakeQueueRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(MainDestination.Player, viewModel.uiState.value.destination)

        viewModel.selectDestination(MainDestination.Chat)
        advanceUntilIdle()

        assertEquals(MainDestination.Chat, viewModel.uiState.value.destination)
    }

    @Test
    fun `queue state follows selected station and stays immutable`() = runTest(dispatcher) {
        val queue = FakeQueueRepository()
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            queue,
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(StationId("sst"), viewModel.uiState.value.queue?.stationId)

        viewModel.selectDestination(MainDestination.Queue)
        advanceUntilIdle()
        assertEquals(StationId("sst"), queue.observedStation)
        assertEquals(1, queue.activeObservations)

        viewModel.selectStation(StationId("death"))
        advanceUntilIdle()

        assertEquals(StationId("death"), viewModel.uiState.value.queue?.stationId)
        assertEquals(QueueLoadStatus.Unavailable, viewModel.uiState.value.queue?.status)
        assertEquals(StationId("death"), queue.observedStation)
        assertEquals(1, queue.activeObservations)

        viewModel.selectDestination(MainDestination.Player)
        advanceUntilIdle()
        assertEquals(0, queue.activeObservations)
    }

    private class FakeNowPlayingRepository : NowPlayingRepository {
        val state = MutableStateFlow(NowPlayingState())

        override fun observeNowPlaying() = state
    }

    private class FakePlaybackController : PlaybackController {
        override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
        var selectedStation: Station? = null
        var playCalls = 0
        var pauseCalls = 0
        var stopCalls = 0

        override fun selectStation(station: Station) {
            selectedStation = station
        }

        override fun play() {
            playCalls++
        }

        override fun pause() {
            pauseCalls++
        }

        override fun stop() {
            stopCalls++
        }
    }

    private class FakeQueueRepository : QueueRepository {
        var refreshedStation: StationId? = null
        var observedStation: StationId? = null
        var activeObservations = 0

        override fun observeQueue(stationId: StationId): Flow<QueueState> {
            observedStation = stationId
            return flow {
                activeObservations++
                emit(QueueState(stationId))
                try {
                    awaitCancellation()
                } finally {
                    activeObservations--
                }
            }
        }

        override suspend fun refresh(stationId: StationId) {
            refreshedStation = stationId
        }
    }
}
