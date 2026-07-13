package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
import com.codeframe78.twentyfourseven.player.domain.PlaybackController
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
        val viewModel = MainViewModel(stations, playback)
        advanceUntilIdle()

        assertEquals("sst", playback.selectedStation?.id?.value)

        viewModel.selectStation(StationId("adagio"))
        viewModel.play()
        viewModel.pause()
        viewModel.stop()
        advanceUntilIdle()

        assertEquals("adagio", playback.selectedStation?.id?.value)
        assertEquals(1, playback.playCalls)
        assertEquals(1, playback.pauseCalls)
        assertEquals(1, playback.stopCalls)
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
}
