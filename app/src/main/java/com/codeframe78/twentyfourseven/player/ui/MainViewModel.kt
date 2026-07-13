package com.codeframe78.twentyfourseven.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import com.codeframe78.twentyfourseven.player.domain.PlaybackController
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.NowPlayingRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val stations: List<Station> = emptyList(),
    val selectedStation: Station? = null,
    val playback: PlaybackState = PlaybackState(),
    val nowPlaying: NowPlayingState = NowPlayingState(),
)

class MainViewModel(
    private val stations: StationRepository,
    private val playback: PlaybackController,
    private val nowPlaying: NowPlayingRepository,
) : ViewModel() {
    val uiState: StateFlow<MainUiState> = combine(
        stations.observeStations(),
        stations.observeSelectedStation(),
        playback.state,
        nowPlaying.observeNowPlaying(),
    ) { all, selected, playbackState, nowPlayingState ->
        MainUiState(
            stations = all,
            selectedStation = selected,
            playback = playbackState,
            nowPlaying = nowPlayingState.takeIf { it.stationId == selected.id }
                ?: NowPlayingState(stationId = selected.id),
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            stations.observeSelectedStation().collect(playback::selectStation)
        }
    }

    fun selectStation(id: StationId) = viewModelScope.launch { stations.selectStation(id) }
    fun play() = playback.play()
    fun pause() = playback.pause()
    fun stop() = playback.stop()

    class Factory(
        private val stations: StationRepository,
        private val playback: PlaybackController,
        private val nowPlaying: NowPlayingRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(stations, playback, nowPlaying) as T
    }
}

