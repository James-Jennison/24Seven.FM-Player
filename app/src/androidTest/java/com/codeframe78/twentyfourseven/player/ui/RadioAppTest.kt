package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.HistoryTrack
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationCapabilities
import com.codeframe78.twentyfourseven.player.domain.StreamVariant
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
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

        composeRule.onNodeWithText("Chat").performClick()
        composeRule.onNodeWithText("No supported chat connection has been verified for this station yet.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("SST").assertIsDisplayed()

        composeRule.onNodeWithText("Queue").performClick()
        composeRule.onNodeWithText("No supported queue or history source has been verified for this station yet.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Feature availability").assertIsDisplayed()

        composeRule.onNodeWithText("Player").performClick()
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
        composeRule.onNodeWithText("Chat").performClick()
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

        composeRule.onNodeWithText("Device preferences").assertIsDisplayed()
        composeRule.onNodeWithText("Always start with Adagio.FM").assertIsDisplayed()
        composeRule.onNodeWithText(
            "These settings stay on this Android device. They do not change station accounts, server Favorites, or membership settings.",
        ).assertIsDisplayed()
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
                )
            }
        }

        composeRule.onNodeWithText("Favorites").performClick()
        composeRule.onNodeWithContentDescription("Request Now — track is currently available to request").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Track Recently Played — track is not currently available to request").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Track Recently Played — track is currently in the station queue and cannot be requested again").assertIsDisplayed()
        composeRule.onNodeWithTag("request_status_green").assertIsDisplayed()
        composeRule.onAllNodesWithTag("request_status_red").assertCountEquals(2)
        composeRule.onAllNodesWithText("Track Recently Played").assertCountEquals(2)
        composeRule.onNodeWithText("Last played today; requestable again tomorrow.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Request Now").assertCountEquals(2)[1].performClick()
        composeRule.onNodeWithText("Request this track?").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(listOf(available), prepared) }
    }

    private fun sampleState() = MainUiState(
        stations = listOf(station),
        selectedStation = station,
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
