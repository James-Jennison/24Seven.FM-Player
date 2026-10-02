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
import com.codeframe78.twentyfourseven.player.domain.NowPlayingDetailsRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingRepository
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
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
import com.codeframe78.twentyfourseven.player.domain.MemberFavoritesState
import com.codeframe78.twentyfourseven.player.domain.MemberProfileState
import com.codeframe78.twentyfourseven.player.domain.StationExtrasRepository
import com.codeframe78.twentyfourseven.player.domain.StationExtrasState
import com.codeframe78.twentyfourseven.player.domain.UnavailableStationExtrasRepository
import com.codeframe78.twentyfourseven.player.domain.currentPlayedHistoryBlock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
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

/** The album whose page is open over the current screen, with what the tapped item already knew about it. */
data class AlbumBrowserState(
    val stationId: StationId,
    val albumId: String,
    val title: String? = null,
    val artworkUrl: String? = null,
)

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
    val extras: StationExtrasState? = null,
    val album: AlbumBrowserState? = null,
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
    private val extras: StationExtrasRepository = UnavailableStationExtrasRepository,
    private val nowPlayingDetails: NowPlayingDetailsRepository? = null,
    private val elapsedRealtimeMillis: () -> Long = { android.os.SystemClock.elapsedRealtime() },
) : ViewModel() {
    private val observedSessionStations = mutableSetOf<StationId>()
    private val destination = MutableStateFlow(MainDestination.Player)
    private val albumBrowser = MutableStateFlow<AlbumBrowserState?>(null)

    @Volatile
    private var latestOnAir: NowPlayingState? = null

    /**
     * What the selected station is playing while this device is not playing it, so the Player is never blank. It is
     * read only while the screen is being shown, about once per track, and is never published to the media session.
     */
    private val onAirPreview: Flow<NowPlayingState?> = combine(
        stations.observeSelectedStation().map { it.id }.distinctUntilChanged(),
        playback.state.map { it.status.showsOnAirPreview }.distinctUntilChanged(),
    ) { stationId, wanted -> stationId to wanted }
        .transformLatest { (stationId, wanted) ->
            val details = nowPlayingDetails ?: return@transformLatest
            if (!wanted) {
                // Once playback has had time to report its own track, the last reading is too old to fall back on.
                delay(ON_AIR_STALE_AFTER_MILLIS)
                latestOnAir = null
                emit(null)
                return@transformLatest
            }
            while (true) {
                val current = try {
                    details.fetchNowPlaying(stationId)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    null
                }
                if (current != null) {
                    latestOnAir = current
                    emit(current)
                }
                delay(onAirRefreshDelayMillis(current, elapsedRealtimeMillis()))
            }
        }
        .onStart { emit(null) }

    private val screenContent = combine(destination, albumBrowser, onAirPreview, ::ScreenContent)
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

    private val selectedExtras = stations.observeSelectedStation()
        .flatMapLatest { station -> extras.observeExtras(station.id) }

    private val listenerActions =
        combine(selectedTrackActions, selectedPrivateMessages, selectedExtras, ::ListenerActionsContent)

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
            extras = listenerActionsState.extras,
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
        screenContent,
        safetyContent,
    ) { selection, playbackContent, content, screen, safety ->
        val selected = selection.selected
        val selectedDestination = screen.destination
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
            nowPlaying = shownNowPlaying(content.nowPlaying, screen.onAir, selected.id, playbackContent.state.status)
                .withCommunityVisibility(selected.id, safety.safety),
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
            extras = content.extras.takeIf { it.stationId == selected.id }
                ?.withCommunityVisibility(selected.id, safety.safety),
            album = screen.album?.takeIf { it.stationId == selected.id },
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
            // A sign-in, a restored session, or a station switch is a moment to learn the unread count.
            selectedAuth
                .map { state -> state.stationId to state.status }
                .distinctUntilChanged()
                .collect { (_, status) -> if (status == AuthStatus.SignedIn) refreshUnreadPrivateMessages() }
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
        refreshUnreadPrivateMessages()
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

    /** Keeps the unread badge current. The repository limits how often the station is actually asked. */
    fun refreshUnreadPrivateMessages() = viewModelScope.launch {
        val station = privateMessageStation() ?: return@launch
        privateMessages.refreshUnreadCount(station.id)
    }

    fun openMemberProfile(username: String) = viewModelScope.launch {
        val station = stations.observeSelectedStation().first()
        val safety = communitySafety.observeSafety().first()
        if (
            !station.capabilities.supportsMemberProfiles ||
            !safety.canViewCommunityContent ||
            safety.isBlocked(station.id, username)
        ) {
            return@launch
        }
        extras.openProfile(station.id, username)
    }

    fun closeMemberProfile() = viewModelScope.launch {
        extras.closeProfile(stations.observeSelectedStation().first().id)
    }

    /** Opens the public favorites list of the member whose profile card is showing. */
    fun openMemberFavorites() = viewModelScope.launch {
        val station = stations.observeSelectedStation().first()
        val safety = communitySafety.observeSafety().first()
        val profile = extras.observeExtras(station.id).first().profile.profile ?: return@launch
        val memberNumber = profile.memberNumber ?: return@launch
        if (
            !station.capabilities.supportsMemberFavorites ||
            profile.publicFavoritesBadge == null ||
            !safety.canViewCommunityContent ||
            safety.isBlocked(station.id, profile.username) ||
            auth.observeAuth(station.id).first().status != AuthStatus.SignedIn
        ) {
            return@launch
        }
        extras.openMemberFavorites(station.id, profile.username, memberNumber)
    }

    fun closeMemberFavorites() = viewModelScope.launch {
        extras.closeMemberFavorites(stations.observeSelectedStation().first().id)
    }

    fun openPlayedHistory() {
        val (date, startHour) = currentPlayedHistoryBlock()
        loadPlayedHistory(date, startHour)
    }

    fun loadPlayedHistory(date: java.time.LocalDate, startHour: Int) = viewModelScope.launch {
        val station = stations.observeSelectedStation().first()
        if (!station.capabilities.supportsPlayedHistoryArchive) return@launch
        extras.loadHistory(station.id, date, startHour)
    }

    fun closePlayedHistory() = viewModelScope.launch {
        extras.closeHistory(stations.observeSelectedStation().first().id)
    }

    fun refreshStationNews() = viewModelScope.launch {
        val station = stations.observeSelectedStation().first()
        if (!station.capabilities.supportsStationNews) return@launch
        extras.refreshNews(station.id)
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

    /** The track the Player is showing for this station, which is what the favorite and rating buttons act on. */
    private suspend fun currentStationTrack(stationId: StationId): NowPlayingState? = shownNowPlaying(
        nowPlaying.observeNowPlaying().first(),
        latestOnAir,
        stationId,
        playback.state.value.status,
    ).takeIf { it.displayTitle != null }

    /** Opens an album's page: its tracks, their request status, and the album rating. */
    fun openAlbum(albumId: String, title: String?, artworkUrl: String?) = viewModelScope.launch {
        val station = stations.observeSelectedStation().first()
        if (!station.capabilities.supportsRequests || !albumId.matches(ALBUM_ID)) return@launch
        albumBrowser.value = AlbumBrowserState(station.id, albumId, title, artworkUrl)
        requests.openSearchResult(station.id, RequestSearchTarget.Album(albumId))
    }

    fun closeAlbum() {
        albumBrowser.value = null
    }

    fun rateAlbum(albumId: String) = viewModelScope.launch {
        val station = signedInStation { it.supportsAlbumRating } ?: return@launch
        if (!albumId.matches(ALBUM_ID)) return@launch
        trackActions.openAlbumRating(station.id, albumId)
    }

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
        // A request made from an album page or a member's favorites list leaves it, so the Queue is what the
        // listener sees.
        albumBrowser.value = null
        extras.closeMemberFavorites(stationId)
        extras.closeProfile(stationId)
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
        private val extras: StationExtrasRepository,
        private val nowPlayingDetails: NowPlayingDetailsRepository,
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
                extras,
                nowPlayingDetails,
            ) as T
    }

    private companion object {
        const val ON_AIR_STALE_AFTER_MILLIS = 60_000L
        val ALBUM_ID = Regex("[A-Za-z0-9_.-]{1,64}")
    }
}

private data class ScreenContent(
    val destination: MainDestination,
    val album: AlbumBrowserState?,
    val onAir: NowPlayingState?,
)

/** Playback states in which this device has no track of its own to show. */
internal val PlaybackStatus.showsOnAirPreview: Boolean
    get() = this == PlaybackStatus.Idle || this == PlaybackStatus.Paused || this == PlaybackStatus.Error

/**
 * The track to show for [stationId]. While this device is playing, that is the track playback reported; before it
 * has reported one, and while the device is not playing, it is what the station says is on air.
 */
internal fun shownNowPlaying(
    live: NowPlayingState,
    onAir: NowPlayingState?,
    stationId: StationId,
    status: PlaybackStatus,
): NowPlayingState {
    val liveHere = live.takeIf { it.stationId == stationId }
    val onAirHere = onAir?.takeIf { it.stationId == stationId }
    val preferred = if (status.showsOnAirPreview) {
        onAirHere ?: liveHere
    } else {
        liveHere?.takeIf { it.displayTitle != null } ?: onAirHere ?: liveHere
    }
    return preferred ?: NowPlayingState(stationId = stationId)
}

/** Asks again a few seconds after the current track should end, and never more often than every fifteen seconds. */
internal fun onAirRefreshDelayMillis(current: NowPlayingState?, nowElapsedRealtimeMillis: Long): Long {
    val length = current?.trackLengthMillis
    val started = current?.trackStartedElapsedRealtimeMillis
    if (length == null || started == null) return 30_000L
    return (started + length - nowElapsedRealtimeMillis + 3_000L).coerceIn(15_000L, 60_000L)
}

private data class StationContent(
    val nowPlaying: NowPlayingState,
    val queue: QueueState,
    val account: AccountContent,
    val chat: ChatState,
    val requestContent: RequestContent,
    val trackActions: TrackActionsState,
    val privateMessages: PrivateMessagesState,
    val extras: StationExtrasState,
)

private data class ListenerActionsContent(
    val trackActions: TrackActionsState,
    val privateMessages: PrivateMessagesState,
    val extras: StationExtrasState,
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

/** A profile card is community content, and a history row's requester follows the same rules as the Queue. */
private fun StationExtrasState.withCommunityVisibility(
    stationId: StationId,
    safety: CommunitySafetyState,
): StationExtrasState = copy(
    profile = profile.takeIf {
        safety.canViewCommunityContent && !safety.isBlocked(stationId, it.requestedName)
    } ?: MemberProfileState(),
    memberFavorites = memberFavorites.takeIf {
        safety.canViewCommunityContent && !safety.isBlocked(stationId, it.memberName)
    } ?: MemberFavoritesState(),
    history = history.copy(
        entries = history.entries.map { entry ->
            val hide = !safety.canViewCommunityContent || safety.isBlocked(stationId, entry.requesterName)
            if (hide) entry.copy(requesterName = null, requestMessage = null) else entry
        },
    ),
    // A story's byline is a member name, so it follows the same rule as other member names.
    news = if (safety.canViewCommunityContent) {
        news
    } else {
        news.copy(stories = news.stories.map { it.copy(author = null) })
    },
)

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
