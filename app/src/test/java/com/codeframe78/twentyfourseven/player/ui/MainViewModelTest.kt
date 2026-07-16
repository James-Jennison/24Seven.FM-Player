package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
import com.codeframe78.twentyfourseven.player.data.InMemoryStationPreferencesRepository
import com.codeframe78.twentyfourseven.player.data.InMemoryCommunitySafetyRepository
import com.codeframe78.twentyfourseven.player.data.UnavailableAuthRepository
import com.codeframe78.twentyfourseven.player.data.UnavailableChatRepository
import com.codeframe78.twentyfourseven.player.data.UnavailableSongRequestRepository
import com.codeframe78.twentyfourseven.player.data.UnavailableFavoriteTracksRepository
import com.codeframe78.twentyfourseven.player.data.UnavailableListenerActivityRepository
import com.codeframe78.twentyfourseven.player.domain.ChatRepository
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.ChatState
import com.codeframe78.twentyfourseven.player.domain.AuthRepository
import com.codeframe78.twentyfourseven.player.domain.AgeGateStatus
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyState
import com.codeframe78.twentyfourseven.player.domain.CURRENT_COMMUNITY_TERMS_VERSION
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.PlaybackController
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.NowPlayingRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueRepository
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksLoadStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksRepository
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksState
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.LocalStationPreferences
import com.codeframe78.twentyfourseven.player.domain.StartupStationMode
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityRepository
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityState
import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.RequestHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import org.junit.Assert.assertSame
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
        val viewModel = MainViewModel(
            stations,
            playback,
            FakeNowPlayingRepository(),
            queue,
            UnavailableAuthRepository(),
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
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
            UnavailableAuthRepository(),
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
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
            UnavailableAuthRepository(),
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(MainDestination.Player, viewModel.uiState.value.destination)

        viewModel.selectDestination(MainDestination.Chat)
        advanceUntilIdle()

        assertEquals(MainDestination.Chat, viewModel.uiState.value.destination)
    }

    @Test
    fun `startup preference actions update immutable state without switching playback`() = runTest(dispatcher) {
        val preferences = InMemoryStationPreferencesRepository()
        val playback = FakePlaybackController()
        val viewModel = MainViewModel(
            BootstrapStationRepository(preferences),
            playback,
            FakeNowPlayingRepository(),
            FakeQueueRepository(),
            UnavailableAuthRepository(),
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.setStartupStation(StationId("death"))
        advanceUntilIdle()

        assertEquals(StartupStationMode.Fixed, viewModel.uiState.value.stationPreferences.startupMode)
        assertEquals(StationId("death"), viewModel.uiState.value.stationPreferences.defaultStationId)
        assertEquals(StationId("sst"), viewModel.uiState.value.selectedStation?.id)
        assertEquals(listOf(StationId("sst")), playback.selectedStations)

        viewModel.selectStation(StationId("adagio"))
        viewModel.useLastStationAtStartup()
        advanceUntilIdle()

        assertEquals(StartupStationMode.LastSelected, viewModel.uiState.value.stationPreferences.startupMode)
        assertEquals(StationId("adagio"), viewModel.uiState.value.stationPreferences.lastStationId)
        assertEquals(null, viewModel.uiState.value.stationPreferences.defaultStationId)
    }

    @Test
    fun `fixed startup station is the first and only station delivered to playback`() = runTest(dispatcher) {
        val preferences = InMemoryStationPreferencesRepository(
            LocalStationPreferences(
                startupMode = StartupStationMode.Fixed,
                defaultStationId = StationId("death"),
            ),
        )
        val playback = FakePlaybackController()

        MainViewModel(
            BootstrapStationRepository(preferences),
            playback,
            FakeNowPlayingRepository(),
            FakeQueueRepository(),
            UnavailableAuthRepository(),
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        advanceUntilIdle()

        assertEquals(listOf(StationId("death")), playback.selectedStations)
    }

    @Test
    fun `all station accounts restore and remain independently visible`() = runTest(dispatcher) {
        val auth = FakeAuthRepository()
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            FakeQueueRepository(),
            auth,
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(
            listOf("sst", "1980s", "adagio", "death", "entranced"),
            viewModel.uiState.value.accounts.map { it.station.id.value },
        )
        assertEquals(
            setOf("sst", "1980s", "adagio", "death", "entranced"),
            auth.restoredStations.map { it.value }.toSet(),
        )

        auth.emit(AuthState(StationId("sst"), AuthStatus.SignedIn, displayName = "SST listener"))
        auth.emit(AuthState(StationId("adagio"), AuthStatus.Expired, errorMessage = "Expired"))
        advanceUntilIdle()

        assertEquals(AuthStatus.SignedIn, viewModel.uiState.value.auth?.status)
        assertEquals(
            AuthStatus.Expired,
            viewModel.uiState.value.accounts.first { it.station.id == StationId("adagio") }.auth.status,
        )

        viewModel.selectStation(StationId("adagio"))
        advanceUntilIdle()
        assertEquals(AuthStatus.Expired, viewModel.uiState.value.auth?.status)
        assertEquals(
            AuthStatus.SignedIn,
            viewModel.uiState.value.accounts.first { it.station.id == StationId("sst") }.auth.status,
        )
    }

    @Test
    fun `account actions target explicit station without changing another session`() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply {
            emit(AuthState(StationId("sst"), AuthStatus.SignedIn, displayName = "SST listener"))
            emit(AuthState(StationId("adagio"), AuthStatus.SignedIn, displayName = "Adagio listener"))
        }
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            FakeQueueRepository(),
            auth,
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.refreshAuth(StationId("death"))
        viewModel.signIn(StationId("entranced"), "Listener", "transient", "A1B2C3")
        viewModel.signOut(StationId("sst"))
        advanceUntilIdle()

        assertEquals(listOf(StationId("death")), auth.refreshedStations)
        assertEquals(listOf(StationId("entranced")), auth.signedInStations)
        assertEquals(listOf(StationId("sst")), auth.signedOutStations)
        assertEquals(
            AuthStatus.SignedIn,
            viewModel.uiState.value.accounts.first { it.station.id == StationId("adagio") }.auth.status,
        )
    }

    @Test
    fun `listener activity is refreshed and observed only for verified signed-in station`() = runTest(dispatcher) {
        val auth = FakeAuthRepository().apply {
            emit(AuthState(StationId("sst"), AuthStatus.SignedIn, displayName = "Listener"))
        }
        val listenerActivity = FakeListenerActivityRepository()
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            FakeQueueRepository(),
            auth,
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            listenerActivity,
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.selectDestination(MainDestination.More)
        advanceUntilIdle()

        assertEquals(StationId("sst"), listenerActivity.observedStation)
        assertEquals(listOf(StationId("sst")), listenerActivity.refreshedStations)
        assertEquals(1, listenerActivity.activeObservations)

        listenerActivity.emit(
            ListenerActivityState(
                stationId = StationId("sst"),
                status = ListenerActivityLoadStatus.Ready,
                membershipTier = MembershipTier.Vip,
                requestReadiness = RequestReadiness.Ready,
                recentRequests = listOf(RequestHistoryEntry(1, "Album - Track - Artist", "14 Jul 26 - 17:38")),
            ),
        )
        advanceUntilIdle()
        assertEquals(MembershipTier.Vip, viewModel.uiState.value.listenerActivity?.membershipTier)

        viewModel.selectStation(StationId("adagio"))
        advanceUntilIdle()
        assertEquals(0, listenerActivity.activeObservations)
        assertEquals(StationId("adagio"), viewModel.uiState.value.listenerActivity?.stationId)
        assertEquals(listOf(StationId("sst")), listenerActivity.refreshedStations)

        viewModel.signOut(StationId("sst"))
        advanceUntilIdle()
        assertEquals(listOf(StationId("sst")), listenerActivity.clearedStations)
    }

    @Test
    fun `queue state follows selected station and stays immutable`() = runTest(dispatcher) {
        val queue = FakeQueueRepository()
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            queue,
            UnavailableAuthRepository(),
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(StationId("sst"), viewModel.uiState.value.queue?.stationId)
        assertEquals(StationId("sst"), viewModel.uiState.value.auth?.stationId)

        viewModel.selectDestination(MainDestination.Queue)
        advanceUntilIdle()
        assertEquals(StationId("sst"), queue.observedStation)
        assertEquals(1, queue.activeObservations)

        viewModel.selectStation(StationId("death"))
        advanceUntilIdle()

        assertEquals(StationId("death"), viewModel.uiState.value.queue?.stationId)
        assertEquals(StationId("death"), viewModel.uiState.value.auth?.stationId)
        assertEquals(QueueLoadStatus.Unavailable, viewModel.uiState.value.queue?.status)
        assertEquals(StationId("death"), queue.observedStation)
        assertEquals(1, queue.activeObservations)

        viewModel.selectDestination(MainDestination.Player)
        advanceUntilIdle()
        assertEquals(0, queue.activeObservations)
    }

    @Test
    fun `chat state is station scoped and observed only on chat destination`() = runTest(dispatcher) {
        val chat = FakeChatRepository()
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            FakeQueueRepository(),
            UnavailableAuthRepository(),
            chat,
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(StationId("sst"), viewModel.uiState.value.chat?.stationId)
        assertEquals(0, chat.activeObservations)

        viewModel.selectDestination(MainDestination.Chat)
        advanceUntilIdle()
        assertEquals(StationId("sst"), chat.observedStation)
        assertEquals(1, chat.activeObservations)

        viewModel.refreshChat()
        viewModel.sendChatMessage("Hello")
        advanceUntilIdle()
        assertEquals(StationId("sst"), chat.refreshedStation)
        assertEquals(StationId("sst"), chat.sentStation)
        assertEquals("Hello", chat.sentMessage)

        viewModel.selectStation(StationId("adagio"))
        advanceUntilIdle()
        assertEquals(StationId("adagio"), viewModel.uiState.value.chat?.stationId)
        assertEquals(StationId("adagio"), chat.observedStation)
        assertEquals(1, chat.activeObservations)

        viewModel.selectDestination(MainDestination.Player)
        advanceUntilIdle()
        assertEquals(0, chat.activeObservations)
    }

    @Test
    fun `community content stays unobserved and cannot be posted before the complete access flow`() = runTest(dispatcher) {
        val chat = FakeChatRepository()
        val safety = InMemoryCommunitySafetyRepository(today = { java.time.LocalDate.of(2026, 7, 15) })
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            FakeQueueRepository(),
            UnavailableAuthRepository(),
            chat,
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            safety,
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        viewModel.selectDestination(MainDestination.Chat)
        viewModel.refreshChat()
        viewModel.sendChatMessage("Blocked before consent")
        advanceUntilIdle()

        assertEquals(0, chat.activeObservations)
        assertNull(chat.refreshedStation)
        assertNull(chat.sentMessage)

        viewModel.submitCommunityAgeScreen(1990, 1, 2)
        viewModel.acceptCommunityTerms()
        viewModel.setCommunityContentVisible(true)
        advanceUntilIdle()
        viewModel.sendChatMessage("Allowed after consent")
        advanceUntilIdle()

        assertEquals(1, chat.activeObservations)
        assertEquals("Allowed after consent", chat.sentMessage)
        assertEquals(true, viewModel.uiState.value.communitySafety.canContributeCommunityContent)
    }

    @Test
    fun `blocking a station user immediately hides chat and request attribution`() = runTest(dispatcher) {
        val stationId = StationId("sst")
        val chat = FakeChatRepository().apply {
            emit(
                ChatState(
                    stationId,
                    messages = listOf(
                        ChatMessage("Blocked Listener", "unsafe text", "12:00"),
                        ChatMessage("Visible Listener", "ordinary text", "12:01"),
                    ),
                ),
            )
        }
        val queue = FakeQueueRepository()
        val safety = enabledSafetyRepository()
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            queue,
            UnavailableAuthRepository(),
            chat,
            UnavailableSongRequestRepository(),
            UnavailableFavoriteTracksRepository(),
            UnavailableListenerActivityRepository(),
            safety,
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        viewModel.selectDestination(MainDestination.Chat)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.chat?.messages?.size)

        viewModel.blockCommunityUser(stationId, "  blocked listener ")
        advanceUntilIdle()
        assertEquals(listOf("Visible Listener"), viewModel.uiState.value.chat?.messages?.map { it.authorDisplayName })

        viewModel.selectDestination(MainDestination.Queue)
        queue.emit(
            QueueState(
                stationId,
                QueueLoadStatus.Ready,
                upcoming = listOf(
                    QueueTrack(1, "One", requesterName = "Blocked Listener", requestMessage = "hidden"),
                    QueueTrack(2, "Two", requesterName = "Visible Listener", requestMessage = "visible"),
                ),
            ),
        )
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.queue?.upcoming?.first()?.requesterName)
        assertNull(viewModel.uiState.value.queue?.upcoming?.first()?.requestMessage)
        assertEquals("Visible Listener", viewModel.uiState.value.queue?.upcoming?.last()?.requesterName)
        assertEquals("visible", viewModel.uiState.value.queue?.upcoming?.last()?.requestMessage)
    }

    @Test
    fun `favorites recalculate when selected station queue changes`() = runTest(dispatcher) {
        val stationId = StationId("sst")
        val queue = FakeQueueRepository()
        val favoriteRequest = RequestableTrack(
            albumId = "ALBUM_1",
            songId = "12345",
            title = "Example Track",
            artist = "Example Artist",
            eligible = true,
            albumTitle = "Example Album",
        )
        val favorites = FakeFavoriteTracksRepository(
            FavoriteTracksState(
                stationId,
                FavoriteTracksLoadStatus.Ready,
                tracks = listOf(
                    FavoriteTrack(
                        position = 1,
                        title = "Example Track",
                        album = "Example Album",
                        artist = "Example Artist",
                        requestTrack = favoriteRequest,
                    ),
                ),
            ),
        )
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            FakeNowPlayingRepository(),
            queue,
            UnavailableAuthRepository(),
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            favorites,
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        viewModel.selectDestination(MainDestination.Favorites)
        queue.emit(QueueState(stationId, QueueLoadStatus.Ready))
        advanceUntilIdle()
        assertEquals(TrackRequestStatus.Available, viewModel.uiState.value.favorites?.tracks?.single()?.availability?.status)

        queue.emit(
            QueueState(
                stationId,
                QueueLoadStatus.Ready,
                upcoming = listOf(
                    QueueTrack(
                        1,
                        "Example Track",
                        albumId = "ALBUM_1",
                        artistName = "Example Artist",
                        albumTitle = "Example Album",
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(TrackRequestStatus.InCurrentQueue, viewModel.uiState.value.favorites?.tracks?.single()?.availability?.status)
        assertEquals(1, queue.activeObservations)
    }

    @Test
    fun `unrelated metadata updates preserve a full favorites list instance`() = runTest(dispatcher) {
        val stationId = StationId("sst")
        val sourceTracks = (1..1_500).map { position ->
            FavoriteTrack(position, "Favorite $position", "Album $position", "Artist $position")
        }
        val favorites = FakeFavoriteTracksRepository(
            FavoriteTracksState(
                stationId,
                FavoriteTracksLoadStatus.Ready,
                tracks = sourceTracks,
            ),
        )
        val nowPlaying = FakeNowPlayingRepository()
        val viewModel = MainViewModel(
            BootstrapStationRepository(),
            FakePlaybackController(),
            nowPlaying,
            FakeQueueRepository(),
            UnavailableAuthRepository(),
            UnavailableChatRepository(),
            UnavailableSongRequestRepository(),
            favorites,
            UnavailableListenerActivityRepository(),
            enabledSafetyRepository(),
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        viewModel.selectDestination(MainDestination.Favorites)
        advanceUntilIdle()
        val beforeMetadataUpdate = viewModel.uiState.value.favorites?.tracks

        nowPlaying.state.value = NowPlayingState(stationId, "A newly reported track")
        advanceUntilIdle()

        assertSame(sourceTracks, beforeMetadataUpdate)
        assertSame(beforeMetadataUpdate, viewModel.uiState.value.favorites?.tracks)
    }

    private fun enabledSafetyRepository() = InMemoryCommunitySafetyRepository(
        CommunitySafetyState(
            ageGateStatus = AgeGateStatus.Adult,
            acceptedTermsVersion = CURRENT_COMMUNITY_TERMS_VERSION,
            communityContentVisible = true,
        ),
    )

    private class FakeNowPlayingRepository : NowPlayingRepository {
        val state = MutableStateFlow(NowPlayingState())

        override fun observeNowPlaying() = state
    }

    private class FakeAuthRepository : AuthRepository {
        private val states = mutableMapOf<StationId, MutableStateFlow<AuthState>>()
        val restoredStations = mutableListOf<StationId>()
        val refreshedStations = mutableListOf<StationId>()
        val signedInStations = mutableListOf<StationId>()
        val signedOutStations = mutableListOf<StationId>()

        override fun observeAuth(stationId: StationId): Flow<AuthState> = state(stationId)

        override suspend fun restoreSession(stationId: StationId) {
            restoredStations += stationId
        }

        override suspend fun refreshChallenge(stationId: StationId) {
            refreshedStations += stationId
        }

        override suspend fun signIn(
            stationId: StationId,
            username: String,
            password: String,
            securityCode: String,
        ) {
            signedInStations += stationId
            state(stationId).value = AuthState(stationId, AuthStatus.SignedIn, displayName = username)
        }

        override suspend fun signOut(stationId: StationId) {
            signedOutStations += stationId
            state(stationId).value = AuthState(stationId)
        }

        fun emit(auth: AuthState) {
            state(auth.stationId).value = auth
        }

        private fun state(stationId: StationId) = states.getOrPut(stationId) {
            MutableStateFlow(AuthState(stationId))
        }
    }

    private class FakePlaybackController : PlaybackController {
        override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
        var selectedStation: Station? = null
        var playCalls = 0
        var pauseCalls = 0
        var stopCalls = 0
        val selectedStations = mutableListOf<StationId>()

        override fun selectStation(station: Station) {
            selectedStation = station
            selectedStations += station.id
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
        private val states = mutableMapOf<StationId, MutableStateFlow<QueueState>>()

        override fun observeQueue(stationId: StationId): Flow<QueueState> {
            observedStation = stationId
            return flow {
                activeObservations++
                try {
                    state(stationId).collect { emit(it) }
                } finally {
                    activeObservations--
                }
            }
        }

        override suspend fun refresh(stationId: StationId) {
            refreshedStation = stationId
        }

        override suspend fun currentQueue(stationId: StationId): QueueState = state(stationId).value

        fun emit(queue: QueueState) {
            state(queue.stationId).value = queue
        }

        private fun state(stationId: StationId) = states.getOrPut(stationId) {
            MutableStateFlow(QueueState(stationId))
        }
    }

    private class FakeFavoriteTracksRepository(initial: FavoriteTracksState) : FavoriteTracksRepository {
        private val state = MutableStateFlow(initial)
        override fun observeFavorites(stationId: StationId): Flow<FavoriteTracksState> = state
        override suspend fun refresh(stationId: StationId) = Unit
        override suspend fun clear(stationId: StationId) {
            state.value = FavoriteTracksState(stationId)
        }
    }

    private class FakeListenerActivityRepository : ListenerActivityRepository {
        private val states = mutableMapOf<StationId, MutableStateFlow<ListenerActivityState>>()
        var observedStation: StationId? = null
        var activeObservations = 0
        val refreshedStations = mutableListOf<StationId>()
        val clearedStations = mutableListOf<StationId>()

        override fun observeActivity(stationId: StationId): Flow<ListenerActivityState> {
            observedStation = stationId
            return flow {
                activeObservations++
                try {
                    state(stationId).collect { emit(it) }
                } finally {
                    activeObservations--
                }
            }
        }

        override suspend fun refresh(stationId: StationId) {
            refreshedStations += stationId
        }

        override suspend fun clear(stationId: StationId) {
            clearedStations += stationId
            state(stationId).value = ListenerActivityState(stationId)
        }

        fun emit(activity: ListenerActivityState) {
            state(activity.stationId).value = activity
        }

        private fun state(stationId: StationId) = states.getOrPut(stationId) {
            MutableStateFlow(ListenerActivityState(stationId))
        }
    }

    private class FakeChatRepository : ChatRepository {
        var observedStation: StationId? = null
        var activeObservations = 0
        var refreshedStation: StationId? = null
        var sentStation: StationId? = null
        var sentMessage: String? = null
        private val states = mutableMapOf<StationId, MutableStateFlow<ChatState>>()

        override fun observeChat(stationId: StationId): Flow<ChatState> {
            observedStation = stationId
            return flow {
                activeObservations++
                try {
                    state(stationId).collect { emit(it) }
                } finally {
                    activeObservations--
                }
            }
        }

        override suspend fun refresh(stationId: StationId) {
            refreshedStation = stationId
        }

        override suspend fun sendMessage(stationId: StationId, message: String) {
            sentStation = stationId
            sentMessage = message
        }

        fun emit(chat: ChatState) {
            state(chat.stationId).value = chat
        }

        private fun state(stationId: StationId) = states.getOrPut(stationId) {
            MutableStateFlow(ChatState(stationId))
        }
    }
}
