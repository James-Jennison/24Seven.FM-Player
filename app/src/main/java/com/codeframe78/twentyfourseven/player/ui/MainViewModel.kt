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
import com.codeframe78.twentyfourseven.player.domain.QueueRepository
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.AuthRepository
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.ChatRepository
import com.codeframe78.twentyfourseven.player.domain.ChatState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainDestination { Player, Chat, Queue, More }

data class MainUiState(
    val stations: List<Station> = emptyList(),
    val selectedStation: Station? = null,
    val playback: PlaybackState = PlaybackState(),
    val nowPlaying: NowPlayingState = NowPlayingState(),
    val queue: QueueState? = null,
    val auth: AuthState? = null,
    val chat: ChatState? = null,
    val destination: MainDestination = MainDestination.Player,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val stations: StationRepository,
    private val playback: PlaybackController,
    private val nowPlaying: NowPlayingRepository,
    private val queue: QueueRepository,
    private val auth: AuthRepository,
    private val chat: ChatRepository,
) : ViewModel() {
    private val destination = MutableStateFlow(MainDestination.Player)

    private val selectedQueue = combine(
        stations.observeSelectedStation(),
        destination,
    ) { station, selectedDestination -> station to selectedDestination }
        .flatMapLatest { (station, selectedDestination) ->
            if (selectedDestination == MainDestination.Queue) {
                queue.observeQueue(station.id)
            } else {
                flowOf(QueueState(station.id))
            }
        }

    private val selectedAuth = stations.observeSelectedStation()
        .flatMapLatest { station -> auth.observeAuth(station.id) }

    private val selectedChat = combine(
        stations.observeSelectedStation(),
        destination,
    ) { station, selectedDestination -> station to selectedDestination }
        .flatMapLatest { (station, selectedDestination) ->
            if (selectedDestination == MainDestination.Chat) {
                chat.observeChat(station.id)
            } else {
                flowOf(ChatState(station.id))
            }
        }

    private val stationContent = combine(
        nowPlaying.observeNowPlaying(),
        selectedQueue,
        selectedAuth,
        selectedChat,
        ::StationContent,
    )

    val uiState: StateFlow<MainUiState> = combine(
        stations.observeStations(),
        stations.observeSelectedStation(),
        playback.state,
        stationContent,
        destination,
    ) { all, selected, playbackState, content, selectedDestination ->
        MainUiState(
            stations = all,
            selectedStation = selected,
            playback = playbackState,
            nowPlaying = content.nowPlaying.takeIf { it.stationId == selected.id }
                ?: NowPlayingState(stationId = selected.id),
            queue = content.queue.takeIf { it.stationId == selected.id },
            auth = content.auth.takeIf { it.stationId == selected.id },
            chat = content.chat.takeIf { it.stationId == selected.id },
            destination = selectedDestination,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            stations.observeSelectedStation().collect(playback::selectStation)
        }
        viewModelScope.launch {
            stations.observeSelectedStation().collect { station -> auth.restoreSession(station.id) }
        }
    }

    fun selectStation(id: StationId) = viewModelScope.launch { stations.selectStation(id) }
    fun play() = playback.play()
    fun pause() = playback.pause()
    fun stop() = playback.stop()
    fun selectDestination(destination: MainDestination) {
        this.destination.value = destination
    }

    fun refreshQueue() = viewModelScope.launch {
        queue.refresh(stations.observeSelectedStation().first().id)
    }

    fun refreshAuth() = viewModelScope.launch {
        auth.refreshChallenge(stations.observeSelectedStation().first().id)
    }

    fun refreshChat() = viewModelScope.launch {
        chat.refresh(stations.observeSelectedStation().first().id)
    }

    fun sendChatMessage(message: String) = viewModelScope.launch {
        chat.sendMessage(stations.observeSelectedStation().first().id, message)
    }

    fun signIn(username: String, password: String, securityCode: String) = viewModelScope.launch {
        auth.signIn(stations.observeSelectedStation().first().id, username, password, securityCode)
    }

    fun signOut() = viewModelScope.launch {
        auth.signOut(stations.observeSelectedStation().first().id)
    }

    class Factory(
        private val stations: StationRepository,
        private val playback: PlaybackController,
        private val nowPlaying: NowPlayingRepository,
        private val queue: QueueRepository,
        private val auth: AuthRepository,
        private val chat: ChatRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(stations, playback, nowPlaying, queue, auth, chat) as T
    }
}

private data class StationContent(
    val nowPlaying: NowPlayingState,
    val queue: QueueState,
    val auth: AuthState,
    val chat: ChatState,
)

