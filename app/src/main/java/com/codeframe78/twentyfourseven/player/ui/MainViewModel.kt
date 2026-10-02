package com.codeframe78.twentyfourseven.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationCapabilities
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
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.ChatRepository
import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMentionSnapshot
import com.codeframe78.twentyfourseven.player.domain.ChatState
import com.codeframe78.twentyfourseven.player.domain.CommunityNotificationRepository
import com.codeframe78.twentyfourseven.player.domain.CommunityNotificationState
import com.codeframe78.twentyfourseven.player.domain.UnavailableCommunityNotificationRepository
import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.RequestSearchTarget
import com.codeframe78.twentyfourseven.player.domain.RequestSuggestionMode
import com.codeframe78.twentyfourseven.player.domain.SongRequestRepository
import com.codeframe78.twentyfourseven.player.domain.SongRequestState
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksLoadStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksRepository
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksState
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailabilityResolver
import com.codeframe78.twentyfourseven.player.domain.TrackRequestCandidate
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus
import com.codeframe78.twentyfourseven.player.domain.RequestConfirmationContext
import com.codeframe78.twentyfourseven.player.domain.RequestTransactionBlock
import com.codeframe78.twentyfourseven.player.domain.LocalStationPreferences
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityRepository
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityState
import com.codeframe78.twentyfourseven.player.domain.AbuseReportState
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSubmission
import com.codeframe78.twentyfourseven.player.domain.AbuseReportTarget
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyRepository
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyState
import com.codeframe78.twentyfourseven.player.domain.TrackActionsRepository
import com.codeframe78.twentyfourseven.player.domain.TrackActionsState
import com.codeframe78.twentyfourseven.player.domain.UnavailableTrackActionsRepository
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageFolder
import com.codeframe78.twentyfourseven.player.domain.PrivateMessagesRepository
import com.codeframe78.twentyfourseven.player.domain.PrivateMessagesState
import com.codeframe78.twentyfourseven.player.domain.UnavailablePrivateMessagesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainDestination { Player, Favorites, Chat, Queue, More }

data class MainUiState(
    val stations: List<Station> = emptyList(),
    val selectedStation: Station? = null,
    val playback: PlaybackState = PlaybackState(),
    val nowPlaying: NowPlayingState = NowPlayingState(),
    val queue: QueueState? = null,
    val auth: AuthState? = null,
    val accounts: List<StationAccountUiState> = emptyList(),
    val chat: ChatState? = null,
    val requests: SongRequestState? = null,
    val favorites: FavoriteTracksState? = null,
    val listenerActivity: ListenerActivityState? = null,
    val communitySafety: CommunitySafetyState = CommunitySafetyState(),
    val abuseReport: AbuseReportState = AbuseReportState(),
    val communityNotifications: CommunityNotificationState = CommunityNotificationState(),
    val stationPreferences: LocalStationPreferences = LocalStationPreferences(),
    val diagnosticTransitions: List<DiagnosticTransition> = emptyList(),
    val destination: MainDestination = MainDestination.Player,
    val trackActions: TrackActionsState? = null,
    val privateMessages: PrivateMessagesState? = null,
)

data class StationAccountUiState(
    val station: Station,
    val auth: AuthState,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val stations: StationRepository,
    private val playback: PlaybackController,
    private val nowPlaying: NowPlayingRepository,
    private val queue: QueueRepository,
    private val auth: AuthRepository,
    private val chat: ChatRepository,
    private val requests: SongRequestRepository,
    private val favorites: FavoriteTracksRepository,
    private val listenerActivity: ListenerActivityRepository,
    private val communitySafety: CommunitySafetyRepository,
    private val communityNotifications: CommunityNotificationRepository = UnavailableCommunityNotificationRepository,
    private val trackActions: TrackActionsRepository = UnavailableTrackActionsRepository,
    private val privateMessages: PrivateMessagesRepository = UnavailablePrivateMessagesRepository,
) : ViewModel() {
    private val observedSessionStations = mutableSetOf<StationId>()
    private val destination = MutableStateFlow(MainDestination.Player)
    private val diagnosticTransitions = MutableStateFlow<List<DiagnosticTransition>>(emptyList())
    private val playbackContent = combine(
        playback.state,
        diagnosticTransitions,
        ::PlaybackContent,
    )

    private val stationSelection = combine(
        stations.observeStations(),
        stations.observeSelectedStation(),
        stations.observeStationPreferences(),
        ::StationSelectionContent,
    )

    private val selectedQueue = combine(
        stations.observeSelectedStation(),
        destination,
    ) { station, selectedDestination -> station to selectedDestination }
        .flatMapLatest { (station, selectedDestination) ->
            if (selectedDestination in setOf(MainDestination.Queue, MainDestination.Favorites, MainDestination.More)) {
                queue.observeQueue(station.id)
            } else {
                flowOf(QueueState(station.id))
            }
        }

    private val accounts = stations.observeStations()
        .flatMapLatest { all ->
            if (all.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(all.map { station -> auth.observeAuth(station.id) }) { states ->
                    all.mapIndexed { index, station -> StationAccountUiState(station, states[index]) }
                }
            }
        }

    private val selectedAuth = combine(
        stations.observeSelectedStation(),
        accounts,
    ) { selected, accountStates ->
        accountStates.firstOrNull { it.station.id == selected.id }?.auth ?: AuthState(selected.id)
    }

    private val authContent = combine(selectedAuth, accounts, ::AuthContent)

    private val selectedListenerActivity = combine(
        stations.observeSelectedStation(),
        destination,
    ) { station, selectedDestination -> station to selectedDestination }
        .flatMapLatest { (station, selectedDestination) ->
            if (selectedDestination == MainDestination.More && station.capabilities.supportsListenerActivity) {
                listenerActivity.observeActivity(station.id)
            } else {
                flowOf(ListenerActivityState(station.id))
            }
        }

    private val accountContent = combine(authContent, selectedListenerActivity, ::AccountContent)

    private val safetyState = communitySafety.observeSafety()

    private val selectedChat = combine(
        stations.observeSelectedStation(),
        destination,
        safetyState,
        selectedAuth,
    ) { station, selectedDestination, safety, authState ->
        SelectedChatContext(station, selectedDestination, safety, authState)
    }
        .flatMapLatest { (station, selectedDestination, safety, authState) ->
            if (selectedDestination == MainDestination.Chat && safety.canViewCommunityContent) {
                chat.observeChat(station.id).map { state ->
                    val filtered = state.copy(
                        messages = state.messages.filterNot { message ->
                            safety.isBlocked(station.id, message.authorDisplayName)
                        },
                    )
                    val displayName = authState.displayName
                    if (
                        filtered.status == ChatLoadStatus.Ready &&
                        authState.status == AuthStatus.SignedIn &&
                        !displayName.isNullOrBlank()
                    ) {
                        communityNotifications.processChatSnapshot(
                            ChatMentionSnapshot(
                                stationId = station.id,
                                stationName = station.name,
                                signedInDisplayName = displayName,
                                messages = state.messages,
                                blockedAuthorDisplayNames = safety.blockedUsers
                                    .asSequence()
                                    .filter { it.stationId == station.id }
                                    .map { it.displayName }
                                    .toSet(),
                            ),
                        )
                    }
                    filtered
                }
            } else {
                flowOf(ChatState(station.id))
            }
        }

    private val selectedRequests = stations.observeSelectedStation()
        .flatMapLatest { station -> requests.observeRequests(station.id) }

    private val selectedFavorites = combine(
        stations.observeSelectedStation(),
        destination,
    ) { station, selectedDestination -> station to selectedDestination }
        .flatMapLatest { (station, selectedDestination) ->
            if (selectedDestination == MainDestination.Favorites) {
                favorites.observeFavorites(station.id)
            } else {
                flowOf(FavoriteTracksState(station.id))
            }
        }

    private val resolvedFavorites = combine(
        selectedFavorites,
        selectedQueue,
        selectedRequests,
    ) { favoriteState, queueState, requestState ->
        ResolvedFavoritesContent(
            favorites = favoriteState.resolveAvailability(
                favoriteState.stationId,
                queueState,
                requestState.transactionBlocks,
            ),
            queue = queueState,
        )
    }

    private val requestContent = combine(
        selectedRequests,
        resolvedFavorites,
    ) { requestState, favoriteContent ->
        RequestContent(
            requests = requestState,
            favorites = favoriteContent.favorites,
            queue = favoriteContent.queue,
        )
    }

    private val selectedTrackActions = stations.observeSelectedStation()
        .flatMapLatest { station -> trackActions.observeTrackActions(station.id) }

    private val selectedPrivateMessages = stations.observeSelectedStation()
        .flatMapLatest { station -> privateMessages.observeMessages(station.id) }

    private val listenerActions = combine(selectedTrackActions, selectedPrivateMessages, ::ListenerActionsContent)

    private val stationContent = combine(
        nowPlaying.observeNowPlaying(),
        accountContent,
        selectedChat,
        requestContent,
        listenerActions,
    ) { nowPlayingState, accountState, chatState, requestsState, listenerActionsState ->
        StationContent(
            nowPlaying = nowPlayingState,
            queue = requestsState.queue,
            account = accountState,
            chat = chatState,
            requestContent = requestsState,
            trackActions = listenerActionsState.trackActions,
            privateMessages = listenerActionsState.privateMessages,
        )
    }

    private val safetyContent = combine(
        safetyState,
        communitySafety.observeReport(),
        communityNotifications.observeSettings(),
        ::SafetyContent,
    )

    val uiState: StateFlow<MainUiState> = combine(
        stationSelection,
        playbackContent,
        stationContent,
        destination,
        safetyContent,
    ) { selection, playbackContent, content, selectedDestination, safety ->
        val selected = selection.selected
        val selectedQueueState = content.queue.takeIf { it.stationId == selected.id }
            ?: QueueState(selected.id)
        val selectedAuthState = content.account.auth.selected.takeIf { it.stationId == selected.id }
        val resolvedRequests = content.requestContent.requests
            .takeIf { it.stationId == selected.id }
            ?.resolveAvailability(
                selected.id,
                selectedQueueState,
                selectedAuthState?.status == AuthStatus.SignedIn,
                content.requestContent.requests.transactionBlocks,
            )
        val resolvedFavorites = content.requestContent.favorites
            .takeIf { it.stationId == selected.id }
        MainUiState(
            stations = selection.all,
            selectedStation = selected,
            playback = playbackContent.state,
            nowPlaying = content.nowPlaying.takeIf { it.stationId == selected.id }
                ?.withCommunityVisibility(selected.id, safety.safety)
                ?: NowPlayingState(stationId = selected.id),
            queue = selectedQueueState.withCommunityVisibility(selected.id, safety.safety),
            auth = selectedAuthState,
            accounts = content.account.auth.accounts,
            chat = content.chat.takeIf { it.stationId == selected.id },
            requests = resolvedRequests,
            favorites = resolvedFavorites,
            listenerActivity = content.account.listenerActivity.takeIf { it.stationId == selected.id },
            communitySafety = safety.safety,
            abuseReport = safety.report,
            communityNotifications = safety.notifications,
            stationPreferences = selection.preferences,
            diagnosticTransitions = playbackContent.transitions,
            destination = selectedDestination,
            trackActions = content.trackActions.takeIf { it.stationId == selected.id },
            privateMessages = content.privateMessages.takeIf { it.stationId == selected.id }
                ?.withCommunityVisibility(selected.id, safety.safety),
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            nowPlaying.observeNowPlaying()
                .distinctUntilChanged()
                .collect(playback::updateNowPlaying)
        }
        viewModelScope.launch {
            playback.state
                .map { state ->
                    DiagnosticTransition(
                        playbackStatus = state.status,
                        networkAvailable = state.networkAvailable,
                        audioOutputKind = state.audioOutput.kind,
                    )
                }
                .distinctUntilChanged()
                .collect { transition ->
                    diagnosticTransitions.value = (diagnosticTransitions.value + transition).takeLast(5)
                }
        }
        viewModelScope.launch {
            stations.observeSelectedStation().collect(playback::selectStation)
        }
        viewModelScope.launch {
            stations.observeStations().collect { all ->
                all.forEach { station ->
                    auth.restoreSession(station.id)
                    if (observedSessionStations.add(station.id)) {
                        launch {
                            auth.observeAuth(station.id)
                                .map { state -> state.status }
                                .distinctUntilChanged()
                                .collect { status ->
                                    if (status == AuthStatus.Expired) {
                                        favorites.clear(station.id)
                                        listenerActivity.clear(station.id)
                                        requests.clear(station.id)
                                        trackActions.clear(station.id)
                                        privateMessages.clear(station.id)
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    fun selectStation(id: StationId) = viewModelScope.launch {
        val current = stations.observeSelectedStation().first()
        if (current.id != id) requests.cancelRequest(current.id)
        stations.selectStation(id)
    }
    fun useLastStationAtStartup() = viewModelScope.launch { stations.useLastStationAtStartup() }
    fun setStartupStation(id: StationId) = viewModelScope.launch { stations.setStartupStation(id) }
    fun play() = playback.play()
    fun pause() = playback.pause()
    fun stop() = playback.stop()
    fun refreshAudioOutput() = playback.refreshAudioOutput()
    fun setSleepTimer(durationMillis: Long) = playback.setSleepTimer(durationMillis)
    fun cancelSleepTimer() = playback.cancelSleepTimer()
    fun selectDestination(destination: MainDestination) {
        this.destination.value = destination
        if (destination == MainDestination.Favorites) {
            viewModelScope.launch {
                val stationId = stations.observeSelectedStation().first().id
                if (favorites.observeFavorites(stationId).first().status == FavoriteTracksLoadStatus.Idle) {
                    favorites.refresh(stationId)
                }
            }
        }
        if (destination == MainDestination.More) {
            viewModelScope.launch {
                val station = stations.observeSelectedStation().first()
                if (
                    station.capabilities.supportsListenerActivity &&
                    auth.observeAuth(station.id).first().status == AuthStatus.SignedIn &&
                    listenerActivity.observeActivity(station.id).first().status == ListenerActivityLoadStatus.Idle
                ) {
                    listenerActivity.refresh(station.id)
                }
            }
        }
    }

    fun refreshQueue() = viewModelScope.launch {
        queue.refresh(stations.observeSelectedStation().first().id)
    }

    fun refreshAuth(stationId: StationId) = viewModelScope.launch {
        auth.refreshChallenge(stationId)
    }

    fun refreshChat() = viewModelScope.launch {
        if (!communitySafety.observeSafety().first().canViewCommunityContent) return@launch
        chat.refresh(stations.observeSelectedStation().first().id)
    }

    fun refreshFavorites() = viewModelScope.launch {
        favorites.refresh(stations.observeSelectedStation().first().id)
    }

    fun refreshListenerActivity() = viewModelScope.launch {
        listenerActivity.refresh(stations.observeSelectedStation().first().id)
    }

    fun sendChatMessage(message: String) = viewModelScope.launch {
        if (!communitySafety.observeSafety().first().canContributeCommunityContent) return@launch
        chat.sendMessage(stations.observeSelectedStation().first().id, message)
    }

    fun submitCommunityAgeScreen(year: Int, month: Int, day: Int) = viewModelScope.launch {
        communitySafety.submitAgeScreen(year, month, day)
    }

    fun acceptCommunityTerms() = viewModelScope.launch {
        communitySafety.acceptTerms()
    }

    fun setCommunityContentVisible(visible: Boolean) = viewModelScope.launch {
        communitySafety.setCommunityContentVisible(visible)
    }

    fun setChatMentionNotificationsEnabled(stationId: StationId, enabled: Boolean) = viewModelScope.launch {
        communityNotifications.setChatMentionsEnabled(stationId, enabled)
    }

    fun setForegroundChatMentionMonitorEnabled(stationId: StationId, enabled: Boolean) = viewModelScope.launch {
        communityNotifications.setForegroundChatMonitorEnabled(stationId, enabled)
    }

    fun blockCommunityUser(stationId: StationId, displayName: String) = viewModelScope.launch {
        communitySafety.blockUser(stationId, displayName)
    }

    fun unblockCommunityUser(stationId: StationId, displayName: String) = viewModelScope.launch {
        communitySafety.unblockUser(stationId, displayName)
    }

    fun beginAbuseReport(target: AbuseReportTarget) = viewModelScope.launch {
        communitySafety.beginReport(stations.observeSelectedStation().first().id, target)
    }

    fun retryAbuseReport() = viewModelScope.launch {
        val current = communitySafety.observeReport().first()
        val stationId = current.stationId ?: return@launch
        val target = current.target ?: return@launch
        communitySafety.beginReport(stationId, target)
    }

    fun submitAbuseReport(submission: AbuseReportSubmission) = viewModelScope.launch {
        communitySafety.submitReport(submission)
    }

    fun reportEmailComposerResult(opened: Boolean) {
        communitySafety.reportEmailComposerResult(opened)
    }

    fun dismissAbuseReport() = communitySafety.dismissReport()

    fun signIn(stationId: StationId, username: String, password: String, securityCode: String) = viewModelScope.launch {
        auth.signIn(stationId, username, password, securityCode)
        if (
            stations.observeStations().first().firstOrNull { it.id == stationId }?.capabilities?.supportsListenerActivity == true &&
            auth.observeAuth(stationId).first().status == AuthStatus.SignedIn
        ) {
            listenerActivity.refresh(stationId)
        }
    }

    fun signOut(stationId: StationId) = viewModelScope.launch {
        auth.signOut(stationId)
        favorites.clear(stationId)
        listenerActivity.clear(stationId)
        requests.clear(stationId)
        trackActions.clear(stationId)
        privateMessages.clear(stationId)
    }

    fun refreshPrivateMessages(folder: PrivateMessageFolder, page: Int) = viewModelScope.launch {
        val station = privateMessageStation() ?: return@launch
        privateMessages.refresh(station.id, folder, page)
    }

    fun openPrivateMessage(messageId: String) = viewModelScope.launch {
        val station = privateMessageStation() ?: return@launch
        privateMessages.openMessage(station.id, messageId)
    }

    fun closePrivateMessage() = viewModelScope.launch {
        privateMessages.closeMessage(stations.observeSelectedStation().first().id)
    }

    fun replyToPrivateMessage() = viewModelScope.launch {
        val station = privateMessageStation(sending = true) ?: return@launch
        privateMessages.beginReply(station.id)
    }

    fun beginPrivateMessage(recipient: String) = viewModelScope.launch {
        val station = privateMessageStation(sending = true) ?: return@launch
        privateMessages.beginMessage(station.id, recipient)
    }

    fun sendPrivateMessage(subject: String, body: String) = viewModelScope.launch {
        val station = privateMessageStation(sending = true) ?: return@launch
        privateMessages.send(station.id, subject, body)
    }

    fun cancelPrivateMessage() = viewModelScope.launch {
        privateMessages.cancelCompose(stations.observeSelectedStation().first().id)
    }

    /**
     * The selected station when private messages may be used on it: the capability is certified, the listener is
     * signed in, and community access allows viewing, or contributing when [sending].
     */
    private suspend fun privateMessageStation(sending: Boolean = false): Station? {
        val safety = communitySafety.observeSafety().first()
        if (!safety.canViewCommunityContent || (sending && !safety.canContributeCommunityContent)) return null
        return signedInStation { capabilities ->
            capabilities.supportsPrivateMessages && (!sending || capabilities.supportsPrivateMessageSending)
        }
    }

    fun addCurrentTrackToFavorites() = viewModelScope.launch {
        val station = signedInStation { it.supportsNowPlayingFavorite } ?: return@launch
        val track = currentStationTrack(station.id)?.track ?: return@launch
        trackActions.addCurrentTrackToFavorites(station.id, track)
    }

    fun openAlbumRating() = viewModelScope.launch {
        val station = signedInStation { it.supportsAlbumRating } ?: return@launch
        val albumId = currentStationTrack(station.id)?.albumId ?: return@launch
        trackActions.openAlbumRating(station.id, albumId)
    }

    fun submitAlbumRating(value: String) = viewModelScope.launch {
        val station = signedInStation { it.supportsAlbumRating } ?: return@launch
        trackActions.submitAlbumRating(station.id, value)
    }

    fun closeAlbumRating() = viewModelScope.launch {
        trackActions.closeAlbumRating(stations.observeSelectedStation().first().id)
    }

    /** The selected station when it has the capability and the listener is signed in to it. */
    private suspend fun signedInStation(hasCapability: (StationCapabilities) -> Boolean): Station? {
        val station = stations.observeSelectedStation().first()
        val signedIn = auth.observeAuth(station.id).first().status == AuthStatus.SignedIn
        return station.takeIf { hasCapability(it.capabilities) && signedIn }
    }

    private suspend fun currentStationTrack(stationId: StationId): NowPlayingState? =
        nowPlaying.observeNowPlaying().first().takeIf { it.stationId == stationId }

    fun searchRequests(query: String, field: RequestSearchField) = viewModelScope.launch {
        requests.search(stations.observeSelectedStation().first().id, query, field)
    }

    fun suggestRequest(mode: RequestSuggestionMode) = viewModelScope.launch {
        requests.suggest(stations.observeSelectedStation().first().id, mode)
    }

    fun openRequestSearchResult(target: RequestSearchTarget) = viewModelScope.launch {
        requests.openSearchResult(stations.observeSelectedStation().first().id, target)
    }

    fun prepareSongRequest(songId: String) = viewModelScope.launch {
        val station = stations.observeSelectedStation().first()
        val account = auth.observeAuth(station.id).first()
        if (station.capabilities.supportsRequests && account.status == AuthStatus.SignedIn && !account.displayName.isNullOrBlank()) {
            requests.prepareRequest(station.id, songId, account.displayName)
        }
    }

    fun prepareFavoriteRequest(track: FavoriteTrack) = viewModelScope.launch {
        val station = stations.observeSelectedStation().first()
        val account = auth.observeAuth(station.id).first()
        if (station.capabilities.supportsRequests && account.status == AuthStatus.SignedIn && !account.displayName.isNullOrBlank()) {
            track.requestTrack?.let { requests.prepareRequest(station.id, it, account.displayName) }
        }
    }

    fun cancelSongRequest() = viewModelScope.launch {
        requests.cancelRequest(stations.observeSelectedStation().first().id)
    }

    fun confirmSongRequest(message: String) = viewModelScope.launch {
        if (!communitySafety.observeSafety().first().canContributeCommunityContent) return@launch
        val station = stations.observeSelectedStation().first()
        val stationId = station.id
        // Queue is the confirmation surface. Navigate immediately; the result remains
        // station-authoritative and the refresh below renders the final queue state.
        destination.value = MainDestination.Queue
        requests.confirmRequest(
            stationId,
            RequestConfirmationContext(
                auth = auth.observeAuth(stationId).first(),
                queue = queue.currentQueue(stationId),
                listenerActivity = listenerActivity.observeActivity(stationId).first(),
                requiresListenerActivity = station.capabilities.supportsListenerActivity,
            ),
            message,
        )
        queue.refresh(stationId)
    }

    class Factory(
        private val stations: StationRepository,
        private val playback: PlaybackController,
        private val nowPlaying: NowPlayingRepository,
        private val queue: QueueRepository,
        private val auth: AuthRepository,
        private val chat: ChatRepository,
        private val requests: SongRequestRepository,
        private val favorites: FavoriteTracksRepository,
        private val listenerActivity: ListenerActivityRepository,
        private val communitySafety: CommunitySafetyRepository,
        private val communityNotifications: CommunityNotificationRepository,
        private val trackActions: TrackActionsRepository,
        private val privateMessages: PrivateMessagesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(
                stations,
                playback,
                nowPlaying,
                queue,
                auth,
                chat,
                requests,
                favorites,
                listenerActivity,
                communitySafety,
                communityNotifications,
                trackActions,
                privateMessages,
            ) as T
    }
}

private data class StationContent(
    val nowPlaying: NowPlayingState,
    val queue: QueueState,
    val account: AccountContent,
    val chat: ChatState,
    val requestContent: RequestContent,
    val trackActions: TrackActionsState,
    val privateMessages: PrivateMessagesState,
)

private data class ListenerActionsContent(
    val trackActions: TrackActionsState,
    val privateMessages: PrivateMessagesState,
)

private data class StationSelectionContent(
    val all: List<Station>,
    val selected: Station,
    val preferences: LocalStationPreferences,
)

private data class AuthContent(
    val selected: AuthState,
    val accounts: List<StationAccountUiState>,
)

private data class AccountContent(
    val auth: AuthContent,
    val listenerActivity: ListenerActivityState,
)

private data class RequestContent(
    val requests: SongRequestState,
    val favorites: FavoriteTracksState,
    val queue: QueueState,
)

private data class ResolvedFavoritesContent(
    val favorites: FavoriteTracksState,
    val queue: QueueState,
)

private data class SafetyContent(
    val safety: CommunitySafetyState,
    val report: AbuseReportState,
    val notifications: CommunityNotificationState,
)

private data class SelectedChatContext(
    val station: Station,
    val destination: MainDestination,
    val safety: CommunitySafetyState,
    val auth: AuthState,
)

private data class PlaybackContent(
    val state: PlaybackState,
    val transitions: List<DiagnosticTransition>,
)

private fun NowPlayingState.withCommunityVisibility(
    stationId: StationId,
    safety: CommunitySafetyState,
): NowPlayingState {
    val hide = !safety.canViewCommunityContent || safety.isBlocked(stationId, requesterName)
    return if (hide) copy(requesterName = null, requestMessage = null) else this
}

/** Private messages are community content: hidden without access, and never shown from a blocked member. */
private fun PrivateMessagesState.withCommunityVisibility(
    stationId: StationId,
    safety: CommunitySafetyState,
): PrivateMessagesState = when {
    !safety.canViewCommunityContent -> PrivateMessagesState(stationId)
    folder == PrivateMessageFolder.Sent -> this
    else -> copy(
        messages = messages.filterNot { safety.isBlocked(stationId, it.correspondent) },
        openMessage = openMessage?.takeUnless { safety.isBlocked(stationId, it.sender) },
    )
}

private fun QueueState.withCommunityVisibility(
    stationId: StationId,
    safety: CommunitySafetyState,
): QueueState = copy(
    upcoming = upcoming.map { track ->
        val hide = !safety.canViewCommunityContent || safety.isBlocked(stationId, track.requesterName)
        if (hide) track.copy(requesterName = null, requestMessage = null) else track
    },
    recentlyPlayed = recentlyPlayed.map { track ->
        val hide = !safety.canViewCommunityContent || safety.isBlocked(stationId, track.requesterName)
        if (hide) track.copy(requesterName = null, requestMessage = null) else track
    },
)

private fun SongRequestState.resolveAvailability(
    stationId: StationId,
    queue: QueueState,
    signedIn: Boolean,
    blocks: List<RequestTransactionBlock>,
): SongRequestState = copy(
    tracks = tracks.map { track -> track.resolveAvailability(stationId, queue, signedIn, blocks) },
    pendingRequest = pendingRequest?.let { prepared ->
        prepared.copy(track = prepared.track.resolveAvailability(stationId, queue, signedIn, blocks))
    },
)

private fun com.codeframe78.twentyfourseven.player.domain.RequestableTrack.resolveAvailability(
    stationId: StationId,
    queue: QueueState,
    signedIn: Boolean,
    blocks: List<RequestTransactionBlock>,
): com.codeframe78.twentyfourseven.player.domain.RequestableTrack {
        val queueAvailability = TrackRequestAvailabilityResolver.resolve(
            stationId,
            identity,
            availability,
            queue,
        )
        val resolved = blocks.firstOrNull { block ->
            block.identity == null || TrackRequestAvailabilityResolver.matches(identity, block.identity)
        }?.availability ?: if (
            !signedIn && queueAvailability.status !in setOf(
                TrackRequestStatus.StationUnavailable,
                TrackRequestStatus.RequestsUnavailable,
                TrackRequestStatus.Unknown,
            )
        ) {
            TrackRequestAvailability(TrackRequestStatus.AuthenticationRequired)
        } else {
            queueAvailability
        }
        return copy(eligible = resolved.canRequest, availability = resolved)
}

private fun FavoriteTracksState.resolveAvailability(
    stationId: StationId,
    queue: QueueState,
    blocks: List<RequestTransactionBlock>,
): FavoriteTracksState {
    val resolvedAvailability = TrackRequestAvailabilityResolver.resolveAll(
        stationId,
        tracks.map { TrackRequestCandidate(it.identity, it.availability) },
        queue,
    )
    var updatedTracks: MutableList<FavoriteTrack>? = null
    tracks.forEachIndexed { index, track ->
        val queueResolved = resolvedAvailability[index]
        val blocked = blocks.firstOrNull { block ->
            block.identity == null || TrackRequestAvailabilityResolver.matches(track.identity, block.identity)
        }?.availability
        val resolved = blocked ?: queueResolved
        val requestTrack = track.requestTrack
        val requestNeedsUpdate = requestTrack != null && (
            requestTrack.eligible != resolved.canRequest ||
                requestTrack.albumTitle != track.album ||
                requestTrack.availability != resolved
            )
        val resolvedRequestTrack = if (requestNeedsUpdate) {
            requestTrack?.copy(
                eligible = resolved.canRequest,
                albumTitle = track.album,
                availability = resolved,
            )
        } else {
            requestTrack
        }
        if (track.availability != resolved || track.requestTrack != resolvedRequestTrack) {
            if (updatedTracks == null) updatedTracks = tracks.toMutableList()
            updatedTracks[index] = track.copy(availability = resolved, requestTrack = resolvedRequestTrack)
        }
    }
    return updatedTracks?.let { copy(tracks = it) } ?: this
}
