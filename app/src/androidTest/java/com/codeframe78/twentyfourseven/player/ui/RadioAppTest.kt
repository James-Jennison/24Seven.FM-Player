package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.HistoryTrack
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.SleepTimerState
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationCapabilities
import com.codeframe78.twentyfourseven.player.domain.StationPage
import com.codeframe78.twentyfourseven.player.domain.StationPageKind
import com.codeframe78.twentyfourseven.player.domain.StreamVariant
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.AudioOutputKind
import com.codeframe78.twentyfourseven.player.domain.AudioOutputState
import com.codeframe78.twentyfourseven.player.domain.AbuseReportKind
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSource
import com.codeframe78.twentyfourseven.player.domain.AbuseReportState
import com.codeframe78.twentyfourseven.player.domain.AbuseReportStatus
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSubmission
import com.codeframe78.twentyfourseven.player.domain.AbuseReportTarget
import com.codeframe78.twentyfourseven.player.domain.BlockedCommunityUser
import com.codeframe78.twentyfourseven.player.domain.AgeGateStatus
import com.codeframe78.twentyfourseven.player.domain.CommunitySafetyState
import com.codeframe78.twentyfourseven.player.domain.CURRENT_COMMUNITY_TERMS_VERSION
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.ChatMessagePart
import com.codeframe78.twentyfourseven.player.domain.ChatState
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus
import com.codeframe78.twentyfourseven.player.domain.SongRequestState
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksLoadStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksState
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus
import com.codeframe78.twentyfourseven.player.domain.LocalStationPreferences
import com.codeframe78.twentyfourseven.player.domain.StartupStationMode
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityState
import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.RequestHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class RadioAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun destinationsRenderNativeContentAndPersistentMiniPlayer() {
        composeRule.setContent {
            var state by remember { mutableStateOf(sampleState()) }
            MaterialTheme {
                RadioApp(
                    state = state,
                    onSelectStation = {},
                    onSelectDestination = { state = state.copy(destination = it) },
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Chat").performClick()
        composeRule.onNodeWithText("No supported chat connection has been verified for this station yet.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("SST").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Queue").performClick()
        composeRule.onNodeWithText("No supported queue or history source has been verified for this station yet.")
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("More").performClick()
        composeRule.onNodeWithText("Account").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Player").performClick()
        composeRule.onNodeWithText("LIVE • Not connected").assertIsDisplayed()
    }

    @Test
    fun tabletWidthUsesNavigationRail() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(800.dp, 1000.dp)) {
                    RadioApp(
                        state = sampleState(),
                        onSelectStation = {},
                        onSelectDestination = {},
                        onPlay = {},
                        onPause = {},
                        onStop = {},
                        onRefreshQueue = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("tablet_navigation_rail").assertExists()
    }

    @Test
    fun compactWidthUsesBottomNavigation() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(430.dp, 900.dp)) {
                    RadioApp(
                        state = sampleState(),
                        onSelectStation = {},
                        onSelectDestination = {},
                        onPlay = {},
                        onPause = {},
                        onStop = {},
                        onRefreshQueue = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("phone_navigation_bar").assertExists()
        composeRule.onNodeWithTag("tablet_navigation_rail").assertDoesNotExist()
    }

    @Test
    fun navigationAdaptsAcrossWidthChangesWithoutLosingDestination() {
        var containerWidth by mutableStateOf(430.dp)
        val queueState = sampleState().copy(destination = MainDestination.Queue)

        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(containerWidth, 900.dp)) {
                    RadioApp(
                        state = queueState,
                        onSelectStation = {},
                        onSelectDestination = {},
                        onPlay = {},
                        onPause = {},
                        onStop = {},
                        onRefreshQueue = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("phone_navigation_bar").assertExists()
        composeRule.onNodeWithText("No supported queue or history source has been verified for this station yet.")
            .assertIsDisplayed()

        composeRule.runOnIdle { containerWidth = 720.dp }

        composeRule.onNodeWithTag("phone_navigation_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("tablet_navigation_rail").assertExists()
        composeRule.onNodeWithText("No supported queue or history source has been verified for this station yet.")
            .assertIsDisplayed()
    }

    @Test
    fun largeFontCompactPlayerKeepsPrimaryControlReachable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f),
            ) {
                MaterialTheme {
                    Box(Modifier.requiredSize(430.dp, 760.dp)) {
                        RadioApp(
                            state = sampleState(),
                            onSelectStation = {},
                            onSelectDestination = {},
                            onPlay = {},
                            onPause = {},
                            onStop = {},
                            onRefreshQueue = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Play live radio").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun maximumFontAndDisplayScaleKeepCompactNavigationAndStationDetailsReachable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    Box(Modifier.requiredSize(343.dp, 762.dp)) {
                        RadioApp(
                            state = sampleState(),
                            onSelectStation = {},
                            onSelectDestination = {},
                            onPlay = {},
                            onPause = {},
                            onStop = {},
                            onRefreshQueue = {},
                        )
                    }
                }
            }
        }

        listOf("Player", "Favorites", "Chat", "Queue", "More").forEach { destination ->
            composeRule.onNodeWithContentDescription(destination).assertIsDisplayed().assertHasClickAction()
        }
        composeRule.onNodeWithContentDescription("Play live radio").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("station_card_sst").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("station_card_description_sst", useUnmergedTree = true).assertIsDisplayed()

        val cardBounds = composeRule.onNodeWithTag("station_card_sst").fetchSemanticsNode().boundsInRoot
        val descriptionBounds = composeRule
            .onNodeWithTag("station_card_description_sst", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(descriptionBounds.top >= cardBounds.top)
        assertTrue(descriptionBounds.bottom <= cardBounds.bottom)
    }

    @Test
    fun maximumFontAndDisplayScaleStackAccountIdentityAboveStatus() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    Box(Modifier.requiredSize(343.dp, 762.dp)) {
                        RadioApp(
                            state = sampleState().copy(destination = MainDestination.More),
                            onSelectStation = {},
                            onSelectDestination = {},
                            onPlay = {},
                            onPause = {},
                            onStop = {},
                            onRefreshQueue = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("account_station_name_sst", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("account_status_sst", useUnmergedTree = true).assertIsDisplayed()
        val nameBounds = composeRule
            .onNodeWithTag("account_station_name_sst", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val statusBounds = composeRule
            .onNodeWithTag("account_status_sst", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(nameBounds.bottom <= statusBounds.top)
    }

    @Test
    fun maximumFontAndDisplayScaleKeepMediumNavigationAndPlayerReachable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    Box(Modifier.requiredSize(701.dp, 584.dp)) {
                        RadioApp(
                            state = sampleState(),
                            onSelectStation = {},
                            onSelectDestination = {},
                            onPlay = {},
                            onPause = {},
                            onStop = {},
                            onRefreshQueue = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("tablet_navigation_rail").assertIsDisplayed()
        listOf("Player", "Favorites", "Chat", "Queue", "More").forEach { destination ->
            composeRule.onNodeWithContentDescription(destination).assertIsDisplayed().assertHasClickAction()
        }
        composeRule.onNodeWithContentDescription("Play live radio").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("compact_player_scroll").performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("station_card_description_sst", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun foldedPortraitShowsPlayerControlsAndStationCarouselWithoutScrolling() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1f),
            ) {
                MaterialTheme {
                    Box(Modifier.requiredSize(412.dp, 731.dp)) {
                        RadioApp(
                            state = sampleState().copy(
                                nowPlaying = NowPlayingState(
                                    station.id,
                                    "James Horner - Legends Of The Fall - The Ludlows (5:35)",
                                ),
                            ),
                            onSelectStation = {},
                            onSelectDestination = {},
                            onPlay = {},
                            onPause = {},
                            onStop = {},
                            onRefreshQueue = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("primary_play_pause").assertIsDisplayed()
        composeRule.onNodeWithTag("station_selector").assertIsDisplayed()
    }

    @Test
    fun nowPlayingArtworkRendersOnPlayerAndMiniPlayer() {
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    sampleState().copy(
                        nowPlaying = NowPlayingState(
                            station.id,
                            "Current track",
                            "https://streamingsoundtracks.com/images/cover/500/B00Q5M2SYS.jpg",
                        ),
                    ),
                )
            }
            MaterialTheme {
                RadioApp(
                    state = state,
                    onSelectStation = {},
                    onSelectDestination = { state = state.copy(destination = it) },
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Album artwork").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Chat").performClick()
        composeRule.onNodeWithContentDescription("Now playing album artwork").assertIsDisplayed()
    }

    @Test
    fun artworkPlayerControlsRemainReachableInShortWideLayout() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(1000.dp, 400.dp)) {
                    RadioApp(
                        state = sampleState().copy(
                            nowPlaying = NowPlayingState(
                                station.id,
                                "Current track",
                                "https://streamingsoundtracks.com/images/cover/500/B00Q5M2SYS.jpg",
                            ),
                        ),
                        onSelectStation = {},
                        onSelectDestination = {},
                        onPlay = {},
                        onPause = {},
                        onStop = {},
                        onRefreshQueue = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Stop").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playerControlsDispatchPlaybackAndWrappedStationActions() {
        val selectedStations = mutableListOf<StationId>()
        var playCount = 0
        var pauseCount = 0
        val adagio = station.copy(id = StationId("adagio"), name = "Adagio.FM", shortName = "Adagio")
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    sampleState().copy(
                        stations = listOf(station, adagio),
                        selectedStation = station.copy(
                            streams = listOf(StreamVariant("https://example.invalid/live", "Test", 0)),
                        ),
                    ),
                )
            }
            MaterialTheme {
                RadioApp(
                    state = state,
                    onSelectStation = { id ->
                        selectedStations += id
                        val selected = state.stations.first { it.id == id }
                        state = state.copy(selectedStation = selected.copy(streams = state.selectedStation?.streams.orEmpty()))
                    },
                    onSelectDestination = {},
                    onPlay = {
                        playCount += 1
                        state = state.copy(playback = PlaybackState(state.selectedStation?.id, PlaybackStatus.Playing))
                    },
                    onPause = {
                        pauseCount += 1
                        state = state.copy(playback = PlaybackState(state.selectedStation?.id, PlaybackStatus.Paused))
                    },
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Next station").performClick()
        composeRule.onNodeWithContentDescription("Previous station").performClick()
        composeRule.onNodeWithContentDescription("Play live radio").performClick()
        composeRule.onNodeWithContentDescription("Pause live radio").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(StationId("adagio"), StationId("sst")), selectedStations)
            assertEquals(1, playCount)
            assertEquals(1, pauseCount)
        }
    }

    @Test
    fun sleepTimerPresetDisplaysRemainingTimeAndCanBeCancelled() {
        var selectedDuration = 0L
        var cancelCount = 0
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    sampleState().copy(
                        selectedStation = station.copy(
                            streams = listOf(StreamVariant("https://example.invalid/live", "Test", 0)),
                        ),
                    ),
                )
            }
            MaterialTheme {
                RadioApp(
                    state = state,
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    sleepTimerActions = SleepTimerActions(
                        onSet = { duration ->
                            selectedDuration = duration
                            state = state.copy(
                                playback = state.playback.copy(
                                    sleepTimer = SleepTimerState(100_000L, duration),
                                ),
                            )
                        },
                        onCancel = { cancelCount += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("sleep_timer_open").performScrollTo().performClick()
        composeRule.onNodeWithTag("sleep_timer_preset_30").performClick()
        composeRule.onNodeWithTag("sleep_timer_remaining").assertIsDisplayed()
        composeRule.onNodeWithText("Stops in 30:00").assertIsDisplayed()
        composeRule.onNodeWithTag("sleep_timer_cancel").performClick()

        composeRule.runOnIdle {
            assertEquals(30L * 60L * 1_000L, selectedDuration)
            assertEquals(1, cancelCount)
        }
    }

    @Test
    fun audioOutputDisplaysCurrentRouteAndEmitsChooserAction() {
        var chooserOpenCount = 0
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        playback = PlaybackState(
                            audioOutput = AudioOutputState(
                                displayName = "Razr Buds",
                                kind = AudioOutputKind.Bluetooth,
                            ),
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    audioOutputActions = AudioOutputActions(
                        onOpenChooser = { chooserOpenCount += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("audio_output_open").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Using Razr Buds").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, chooserOpenCount) }
    }

    @Test
    fun networkLossExplainsAutomaticRecoveryAndKeepsPauseAvailable() {
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        selectedStation = station.copy(
                            streams = listOf(StreamVariant("https://example.invalid/live", "Test", 0)),
                        ),
                        playback = PlaybackState(station.id, PlaybackStatus.WaitingForNetwork),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithText("No network · playback will resume automatically")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause live radio").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun readyQueueRendersUpcomingAndHistoryNatively() {
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.Queue,
                        selectedStation = station.copy(
                            capabilities = StationCapabilities(supportsQueue = true, supportsHistory = true),
                        ),
                        queue = QueueState(
                            stationId = station.id,
                            status = QueueLoadStatus.Ready,
                            upcoming = listOf(
                                QueueTrack(
                                    position = 1,
                                    displayTitle = "Upcoming track",
                                    artistName = "Upcoming artist",
                                    requesterName = "Listener",
                                    requestMessage = "Enjoy this one",
                                ),
                            ),
                            recentlyPlayed = listOf(
                                HistoryTrack(displayTitle = "Played track", artistName = "Played artist"),
                            ),
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithText("Up next").assertIsDisplayed()
        composeRule.onNodeWithText("Upcoming track").assertIsDisplayed()
        composeRule.onNodeWithText("Requested by Listener").assertIsDisplayed()
        composeRule.onNodeWithText("“Enjoy this one”").assertIsDisplayed()
        composeRule.onNodeWithText("Recently played").assertIsDisplayed()
        composeRule.onNodeWithText("Played track").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun signedOutAccountRendersNativeCredentialFields() {
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.More,
                        selectedStation = station.copy(
                            capabilities = StationCapabilities(supportsAuthentication = true),
                        ),
                        auth = AuthState(
                            station.id,
                            AuthStatus.SignedOut,
                            challengeImageUrl = "https://streamingsoundtracks.com/security-code.png",
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithText("Username").assertExists()
        composeRule.onNodeWithText("Password").assertExists()
        composeRule.onNodeWithText("Security code").assertExists()
        composeRule.onNodeWithText("Sign in to SST").assertExists()
    }

    @Test
    fun stationAccountsExposeFiveIndependentStatusesAndTargetActions() {
        val refreshed = mutableListOf<StationId>()
        val signedOut = mutableListOf<StationId>()
        val signedIn = mutableListOf<StationId>()
        val stations = listOf(
            accountStation("sst", "StreamingSoundtracks.com", "SST"),
            accountStation("1980s", "1980s.FM", "80s"),
            accountStation("adagio", "Adagio.FM", "Adagio"),
            accountStation("death", "Death.FM", "Death"),
            accountStation("entranced", "Entranced.FM", "Entranced"),
        )
        val accounts = stations.map { accountStation ->
            val auth = when (accountStation.id.value) {
                "sst" -> AuthState(accountStation.id, AuthStatus.SignedIn, displayName = "Listener")
                "adagio" -> AuthState(accountStation.id, AuthStatus.Expired, errorMessage = "Session expired")
                "entranced" -> AuthState(
                    accountStation.id,
                    AuthStatus.SignedOut,
                    challengeImageUrl = "https://entranced.fm/security-code.png",
                )
                else -> AuthState(accountStation.id)
            }
            StationAccountUiState(accountStation, auth)
        }

        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.More,
                        stations = stations,
                        selectedStation = stations.first(),
                        auth = accounts.first().auth,
                        accounts = accounts,
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    onRefreshAuth = { refreshed += it },
                    onSignIn = { stationId, _, _, _ -> signedIn += stationId },
                    onSignOut = { signedOut += it },
                )
            }
        }

        composeRule.onAllNodesWithTag("account_card_sst").assertCountEquals(1)
        composeRule.onAllNodesWithTag("account_card_1980s").assertCountEquals(0)
        composeRule.onNodeWithTag("toggle_other_station_accounts").performScrollTo().performClick()
        composeRule.onAllNodesWithTag("account_card_1980s").assertCountEquals(1)
        composeRule.onAllNodesWithTag("account_card_adagio").assertCountEquals(1)
        composeRule.onAllNodesWithTag("account_card_death").assertCountEquals(1)
        composeRule.onAllNodesWithTag("account_card_entranced").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("StreamingSoundtracks.com account status: Signed in").assertExists()
        composeRule.onNodeWithContentDescription("Adagio.FM account status: Expired").assertExists()

        composeRule.onNodeWithTag("account_sign_out_sst").performScrollTo().performClick()
        composeRule.onNodeWithTag("account_sign_in_again_adagio").performScrollTo().performClick()
        composeRule.onNodeWithTag("account_sign_in_entranced").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(StationId("sst")), signedOut)
            assertEquals(listOf(StationId("adagio")), refreshed)
            assertEquals(listOf(StationId("entranced")), signedIn)
        }
    }

    @Test
    fun compactMorePrioritizesSelectedAccountAndTogglesDisclosures() {
        val sst = accountStation("sst", "StreamingSoundtracks.com", "SST")
        val adagio = accountStation("adagio", "Adagio.FM", "Adagio")
        val accounts = listOf(
            StationAccountUiState(sst, AuthState(sst.id)),
            StationAccountUiState(adagio, AuthState(adagio.id)),
        )

        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.More,
                        stations = listOf(sst, adagio),
                        selectedStation = adagio,
                        auth = accounts.last().auth,
                        accounts = accounts,
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onAllNodesWithTag("account_card_adagio").assertCountEquals(1)
        composeRule.onAllNodesWithTag("account_card_sst").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("Song requests, collapsed").assertExists()
        composeRule.onNodeWithText("Song requests have not been verified for this station.")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("more_song_requests").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Song requests, expanded").assertExists()
        composeRule.onNodeWithText("Song requests have not been verified for this station.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("more_song_requests").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Song requests, collapsed").assertExists()
        composeRule.onNodeWithText("Song requests have not been verified for this station.")
            .assertDoesNotExist()

        composeRule.onNodeWithTag("toggle_other_station_accounts").performScrollTo().performClick()
        composeRule.onAllNodesWithTag("account_card_sst").assertCountEquals(1)
        composeRule.onNodeWithTag("toggle_other_station_accounts").performScrollTo().performClick()
        composeRule.onAllNodesWithTag("account_card_sst").assertCountEquals(0)
    }

    @Test
    fun deviceStartupPreferenceIsDistinctAndEmitsExplicitActions() {
        val useLastCalls = mutableListOf<Unit>()
        val fixedStations = mutableListOf<StationId>()
        val adagio = accountStation("adagio", "Adagio.FM", "Adagio")
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.More,
                        stations = listOf(station, adagio),
                        selectedStation = adagio,
                        stationPreferences = LocalStationPreferences(
                            startupMode = StartupStationMode.Fixed,
                            defaultStationId = adagio.id,
                            lastStationId = station.id,
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    onUseLastStationAtStartup = { useLastCalls += Unit },
                    onSetStartupStation = { fixedStations += it },
                )
            }
        }

        composeRule.onNodeWithTag("more_device_preferences").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Always start with Adagio.FM").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("startup_use_last_station").performScrollTo().performClick()
        composeRule.onNodeWithTag("startup_use_current_station").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(1, useLastCalls.size)
            assertEquals(listOf(StationId("adagio")), fixedStations)
        }
    }

    @Test
    fun verifiedListenerActivityShowsMembershipCooldownAndRequestHistory() {
        var refreshCalls = 0
        val activityStation = station.copy(
            capabilities = StationCapabilities(
                supportsAuthentication = true,
                supportsRequests = true,
                supportsListenerActivity = true,
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.More,
                        stations = listOf(activityStation),
                        selectedStation = activityStation,
                        auth = AuthState(activityStation.id, AuthStatus.SignedIn, displayName = "Listener"),
                        listenerActivity = ListenerActivityState(
                            stationId = activityStation.id,
                            status = ListenerActivityLoadStatus.Ready,
                            membershipTier = MembershipTier.Vip,
                            requestReadiness = RequestReadiness.Waiting,
                            waitMinutes = 37,
                            recentRequests = listOf(
                                RequestHistoryEntry(
                                    position = 1,
                                    trackSummary = "For The Love Of Spock - Clipped Ears - Nicholas Pike",
                                    requestedAtLabel = "14 Jul 26 - 17:38",
                                ),
                            ),
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    onRefreshListenerActivity = { refreshCalls++ },
                )
            }
        }

        composeRule.onNodeWithTag("more_request_activity").performScrollTo().performClick()
        composeRule.onNodeWithTag("listener_activity_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Membership: VIP member").assertExists()
        composeRule.onNodeWithContentDescription("Request status: Wait 37 minutes").assertExists()
        composeRule.onNodeWithContentDescription(
            "Request 1: For The Love Of Spock - Clipped Ears - Nicholas Pike; 14 Jul 26 - 17:38",
        ).assertExists()
        composeRule.onNodeWithTag("refresh_listener_activity").performClick()
        composeRule.runOnIdle { assertEquals(1, refreshCalls) }
    }

    @Test
    fun privacyNoticeIsReachableNativelyFromMore() {
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(destination = MainDestination.More),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithText("Read privacy notice").performScrollTo().performClick()
        composeRule.onNodeWithText("Data handled by the Alpha").assertIsDisplayed()
        composeRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun openSourceLicensesArePackagedAndReachableFromMore() {
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(destination = MainDestination.More),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithText("Open-source licenses").performScrollTo().performClick()
        composeRule.onNodeWithText("third-party software notices", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("jsoup 1.22.2", substring = true, ignoreCase = true).assertIsDisplayed()
        composeRule.onNodeWithText("Close").assertIsDisplayed()
    }

    @Test
    fun verifiedSecondaryContentIsReachableAndEmitsTheSelectedPage() {
        val opened = mutableListOf<StationPage>()
        val contact = StationPage(
            StationPageKind.Contact,
            "Contact",
            "Contact the station team",
            "https://streamingsoundtracks.com/modules.php?name=Contact_Us",
        )
        val contentStation = station.copy(
            capabilities = StationCapabilities(supportsSecondaryContent = true),
            secondaryPages = listOf(contact),
        )
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.More,
                        stations = listOf(contentStation),
                        selectedStation = contentStation,
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    onOpenStationPage = { opened += it },
                )
            }
        }

        composeRule.onNodeWithTag("secondary_content_directory").assertExists()
        composeRule.onNodeWithContentDescription(
            "Open Contact for StreamingSoundtracks.com in browser",
        ).performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(listOf(contact), opened) }
    }

    @Test
    fun unverifiedSecondaryContentIsOmittedFromCompactMore() {
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(destination = MainDestination.More),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithTag("secondary_content_directory").assertDoesNotExist()
        composeRule.onNodeWithText("Station links").assertDoesNotExist()
    }

    @Test
    fun readyChatRendersMessagesAndEmitsNativeSendAction() {
        val sentMessages = mutableListOf<String>()
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.Chat,
                        selectedStation = station.copy(
                            capabilities = StationCapabilities(
                                supportsAuthentication = true,
                                supportsChat = true,
                            ),
                        ),
                        auth = AuthState(station.id, AuthStatus.SignedIn, displayName = "Listener"),
                        chat = ChatState(
                            station.id,
                            ChatLoadStatus.Ready,
                            messages = listOf(
                                ChatMessage("Other listener", "Existing message", "13 Jul 26 - 19:04:56"),
                                ChatMessage(
                                    "MorG",
                                    "heart",
                                    "15 Jul 26 - 17:13:13",
                                    parts = listOf(
                                        ChatMessagePart.Emoticon(
                                            "heart",
                                            "https://streamingsoundtracks.com/modules/ClearChat/common/smilies/default/heart.gif",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    onSendChatMessage = { sentMessages += it },
                )
            }
        }

        composeRule.onNodeWithText("Existing message").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("heart emoticon").assertIsDisplayed()
        composeRule.onNodeWithText("Message").performTextInput("Hello chat")
        composeRule.onNodeWithText("Send").performClick()
        composeRule.runOnIdle { assertEquals(listOf("Hello chat"), sentMessages) }
    }

    @Test
    fun songRequestRequiresExplicitConfirmation() {
        val prepared = mutableListOf<String>()
        val confirmedMessages = mutableListOf<String>()
        val track = RequestableTrack("ALBUM_1", "12345", "Requestable track", "Composer", "3:21", true)
        composeRule.setContent {
            var pending by remember { mutableStateOf(false) }
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.More,
                        selectedStation = station.copy(
                            capabilities = StationCapabilities(
                                supportsAuthentication = true,
                                supportsRequests = true,
                                supportsRequestMessages = true,
                            ),
                        ),
                        auth = AuthState(station.id, AuthStatus.SignedIn, displayName = "Listener"),
                        requests = SongRequestState(
                            station.id,
                            SongRequestLoadStatus.Ready,
                            albumTitle = "Example album",
                            tracks = listOf(track),
                            pendingRequest = track.takeIf { pending },
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    onPrepareRequest = {
                        prepared += it
                        pending = true
                    },
                    onConfirmRequest = { confirmedMessages += it },
                )
            }
        }

        composeRule.onNodeWithTag("more_song_requests").performScrollTo().performClick()
        composeRule.onNodeWithTag("library_track_sort").performScrollTo().performClick()
        composeRule.onNodeWithText("Play state").performClick()
        composeRule.onNodeWithText("Sort: Play state").assertIsDisplayed()
        composeRule.onAllNodesWithText("Request Now").assertCountEquals(2)[1].performScrollTo().performClick()
        composeRule.onNodeWithText("Request this track?").assertIsDisplayed()
        composeRule.onNodeWithText("Message (optional)").performTextInput("Enjoy this one")
        composeRule.onNodeWithText("14/80").assertIsDisplayed()
        composeRule.onNodeWithText("Send request").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("12345"), prepared)
            assertEquals(listOf("Enjoy this one"), confirmedMessages)
        }
    }

    @Test
    fun favoritesShowAccessibleStoplightsAndReuseRequestConfirmation() {
        val availableDescription = "Request Now — track is currently available to request"
        val unavailableDescription = "Track Recently Played — track is not currently available to request"
        val queuedDescription = "Track Recently Played — track is currently in the station queue and cannot be requested again"
        val prepared = mutableListOf<FavoriteTrack>()
        val availableRequest = RequestableTrack("ALBUM_1", "12345", "Available favorite", "Composer", "3:21", true)
        val available = FavoriteTrack(
            position = 1,
            title = "Available favorite",
            album = "Available album",
            artist = "Composer",
            duration = "3:21",
            requestTrack = availableRequest,
        )
        val unavailable = FavoriteTrack(
            position = 2,
            title = "Unavailable favorite",
            album = "Unavailable album",
            artist = "Other composer",
            availabilityMessage = "Last played today; requestable again tomorrow.",
        )
        val queued = FavoriteTrack(
            position = 3,
            title = "Queued favorite",
            album = "Queued album",
            artist = "Queue composer",
            availability = TrackRequestAvailability(
                TrackRequestStatus.InCurrentQueue,
                "This track is currently in the station queue.",
            ),
        )
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    sampleState().copy(
                        selectedStation = station.copy(
                            capabilities = StationCapabilities(
                                supportsAuthentication = true,
                                supportsFavorites = true,
                                supportsRequests = true,
                            ),
                        ),
                        auth = AuthState(station.id, AuthStatus.SignedIn, displayName = "Listener"),
                        favorites = FavoriteTracksState(
                            station.id,
                            FavoriteTracksLoadStatus.Ready,
                            tracks = listOf(available, unavailable, queued),
                        ),
                        requests = SongRequestState(station.id, SongRequestLoadStatus.Ready),
                    ),
                )
            }
            MaterialTheme {
                RadioApp(
                    state = state,
                    onSelectStation = {},
                    onSelectDestination = { state = state.copy(destination = it) },
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    onPrepareFavoriteRequest = {
                        prepared += it
                        state = state.copy(requests = state.requests?.copy(pendingRequest = it.requestTrack))
                    },
                    onConfirmRequest = {
                        state = state.copy(
                            requests = state.requests?.copy(
                                pendingRequest = null,
                                notice = "Your request has successfully been delivered to the DJ application.",
                            ),
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Favorites").performClick()
        val favoritesList = composeRule.onNodeWithTag("favorite_tracks_list")
        composeRule.onNodeWithContentDescription(availableDescription).assertIsDisplayed()
        composeRule.onNodeWithTag("request_status_green").assertIsDisplayed()
        favoritesList.performScrollToNode(hasContentDescription(unavailableDescription))
        composeRule.onNodeWithContentDescription(unavailableDescription).assertIsDisplayed()
        composeRule.onNodeWithText("Last played today; requestable again tomorrow.")
            .performScrollTo()
            .assertIsDisplayed()
        favoritesList.performScrollToNode(hasContentDescription(queuedDescription))
        composeRule.onNodeWithContentDescription(queuedDescription).assertIsDisplayed()
        favoritesList.performScrollToNode(hasContentDescription(availableDescription))
        composeRule.onAllNodesWithText("Request Now").assertCountEquals(2)[1].performScrollTo().performClick()
        composeRule.onNodeWithText("Request this track?").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(listOf(available), prepared) }
        composeRule.onNodeWithText("Send request").performClick()
        composeRule.onNodeWithTag("favorite_request_notice").assertIsDisplayed()
        composeRule.onNodeWithText("Request sent").assertIsDisplayed()
        composeRule.onNodeWithText("Your request has successfully been delivered to the DJ application.").assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_tracks_list").assertExists()
    }

    @Test
    fun fullVipFavoritesListRemainsBrowsableAndOffersPlayStateSorting() {
        val tracks = (1..1_500).map { position ->
            val status = when {
                position % 10 == 0 -> TrackRequestStatus.Available
                position % 3 == 0 -> TrackRequestStatus.RecentlyPlayed
                else -> TrackRequestStatus.RequestsUnavailable
            }
            FavoriteTrack(
                position = position,
                title = "Favorite $position",
                album = "Album $position",
                artist = "Artist $position",
                availability = TrackRequestAvailability(status),
            )
        }
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.Favorites,
                        selectedStation = station.copy(
                            capabilities = StationCapabilities(
                                supportsAuthentication = true,
                                supportsFavorites = true,
                                supportsRequests = true,
                            ),
                        ),
                        auth = AuthState(station.id, AuthStatus.SignedIn, displayName = "VIP Listener"),
                        favorites = FavoriteTracksState(
                            station.id,
                            FavoriteTracksLoadStatus.Ready,
                            tracks = tracks,
                        ),
                        requests = SongRequestState(station.id, SongRequestLoadStatus.Ready),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                )
            }
        }

        composeRule.onNodeWithText("1500 tracks").assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_tracks_list").performScrollToIndex(1_502)
        composeRule.onNodeWithTag("favorite_track_1500").assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_tracks_list").performScrollToIndex(2)
        composeRule.onNodeWithTag("favorite_track_sort").performScrollTo().performClick()
        listOf("#", "Track Name", "Album", "Artist", "Genre", "Year", "Length", "Play state").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
        composeRule.onNodeWithText("Track Name").performClick()
        composeRule.onNodeWithText("Sort: Track Name").assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_tracks_list").performScrollToIndex(4)
        composeRule.onNodeWithText("Favorite 10").assertIsDisplayed()
    }

    @Test
    fun communityContentRequiresAgeTermsAndSeparateRevealActions() {
        val chatStation = station.copy(capabilities = StationCapabilities(supportsChat = true))
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    sampleState().copy(
                        selectedStation = chatStation,
                        stations = listOf(chatStation),
                        destination = MainDestination.Chat,
                        communitySafety = CommunitySafetyState(),
                        chat = ChatState(
                            chatStation.id,
                            ChatLoadStatus.Ready,
                            messages = listOf(ChatMessage("Listener", "Visible only after access", "12:00")),
                        ),
                    ),
                )
            }
            MaterialTheme {
                RadioApp(
                    state = state,
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    communitySafetyActions = CommunitySafetyActions(
                        onSubmitAgeScreen = { _, _, _ ->
                            state = state.copy(
                                communitySafety = state.communitySafety.copy(ageGateStatus = AgeGateStatus.Adult),
                            )
                        },
                        onAcceptTerms = {
                            state = state.copy(
                                communitySafety = state.communitySafety.copy(
                                    acceptedTermsVersion = CURRENT_COMMUNITY_TERMS_VERSION,
                                ),
                            )
                        },
                        onSetCommunityContentVisible = { visible ->
                            state = state.copy(
                                communitySafety = state.communitySafety.copy(communityContentVisible = visible),
                            )
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Visible only after access").assertDoesNotExist()
        composeRule.onNodeWithTag("age_month").performTextInput("1")
        composeRule.onNodeWithTag("age_day").performTextInput("2")
        composeRule.onNodeWithTag("age_year").performTextInput("1990")
        composeRule.onNodeWithTag("submit_age_screen").performClick()
        composeRule.onNodeWithText("Terms required").assertIsDisplayed()
        composeRule.onNodeWithTag("review_community_terms").performClick()
        composeRule.onNodeWithTag("agree_community_terms").performScrollTo().performClick()
        composeRule.onNodeWithTag("accept_community_terms").performClick()
        composeRule.onNodeWithText("Mature community content is hidden").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("show_community_content").performScrollTo().performClick()
        composeRule.onNodeWithText("Visible only after access").assertIsDisplayed()
    }

    @Test
    fun chatOffersSeparateReportAndImmediateBlockActions() {
        val chatStation = station.copy(capabilities = StationCapabilities(supportsChat = true))
        composeRule.setContent {
            var state by remember {
                mutableStateOf(
                    sampleState().copy(
                        selectedStation = chatStation,
                        stations = listOf(chatStation),
                        destination = MainDestination.Chat,
                        chat = ChatState(
                            chatStation.id,
                            ChatLoadStatus.Ready,
                            messages = listOf(ChatMessage("Troublemaker", "Reportable text", "12:34")),
                        ),
                    ),
                )
            }
            MaterialTheme {
                RadioApp(
                    state = state,
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    communitySafetyActions = CommunitySafetyActions(
                        onBlockUser = { stationId, name ->
                            state = state.copy(
                                chat = state.chat?.copy(messages = emptyList()),
                                communitySafety = state.communitySafety.copy(
                                    blockedUsers = listOf(BlockedCommunityUser(stationId, name)),
                                ),
                            )
                        },
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Safety actions for Troublemaker").performClick()
        composeRule.onNodeWithText("Report content").assertIsDisplayed()
        composeRule.onNodeWithText("Report user").assertIsDisplayed()
        composeRule.onNodeWithText("Block user").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Reportable text").assertDoesNotExist()
    }

    @Test
    fun nativeReportDialogCollectsContactCaptchaAndBoundedDetails() {
        var submitted: AbuseReportSubmission? = null
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        abuseReport = AbuseReportState(
                            stationId = station.id,
                            target = AbuseReportTarget(
                                AbuseReportKind.Content,
                                AbuseReportSource.Chat,
                                "Troublemaker",
                                "12:34",
                                "Reportable text",
                            ),
                            status = AbuseReportStatus.Ready,
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    communitySafetyActions = CommunitySafetyActions(onSubmitReport = { submitted = it }),
                )
            }
        }

        composeRule.onNodeWithText("Report content").assertIsDisplayed()
        composeRule.onNodeWithTag("reporter_name").performTextInput("Reporter")
        composeRule.onNodeWithTag("reporter_email").performTextInput("reporter@example.com")
        composeRule.onNodeWithTag("report_details").performTextInput("Harmless test details")
        composeRule.onNodeWithTag("report_security_code").performTextInput("123")
        composeRule.onNodeWithTag("submit_abuse_report").performClick()
        composeRule.runOnIdle {
            assertEquals("Reporter", submitted?.reporterName)
            assertEquals("reporter@example.com", submitted?.reporterEmail)
            assertEquals("123", submitted?.securityCode)
        }
    }

    @Test
    fun indeterminateReportDialogSuppressesDuplicateRetry() {
        var dismissed = false
        composeRule.setContent {
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        abuseReport = AbuseReportState(
                            stationId = station.id,
                            target = AbuseReportTarget(
                                AbuseReportKind.User,
                                AbuseReportSource.Chat,
                                "Troublemaker",
                            ),
                            status = AbuseReportStatus.Error,
                            errorMessage = "The station response could not confirm delivery. Do not resend until an administrator checks receipt.",
                            retryAllowed = false,
                        ),
                    ),
                    onSelectStation = {},
                    onSelectDestination = {},
                    onPlay = {},
                    onPause = {},
                    onStop = {},
                    onRefreshQueue = {},
                    communitySafetyActions = CommunitySafetyActions(onDismissReport = { dismissed = true }),
                )
            }
        }

        composeRule.onNodeWithText("Try again").assertDoesNotExist()
        composeRule.onNodeWithText("Done").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(dismissed) }
    }

    private fun sampleState() = MainUiState(
        stations = listOf(station),
        selectedStation = station,
        communitySafety = CommunitySafetyState(
            ageGateStatus = AgeGateStatus.Adult,
            acceptedTermsVersion = CURRENT_COMMUNITY_TERMS_VERSION,
            communityContentVisible = true,
        ),
    )

    private fun accountStation(id: String, name: String, shortName: String) = Station(
        id = StationId(id),
        name = name,
        shortName = shortName,
        description = "$name live radio",
        websiteUrl = "https://$id.example/",
        capabilities = StationCapabilities(supportsAuthentication = true),
    )

    private companion object {
        val station = Station(
            id = StationId("sst"),
            name = "StreamingSoundtracks.com",
            shortName = "SST",
            description = "Movie, game, TV and anime scores",
            websiteUrl = "https://www.streamingsoundtracks.com/",
        )
    }
}
