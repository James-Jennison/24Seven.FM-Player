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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.HistoryTrack
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationCapabilities
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.ChatState
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus
import com.codeframe78.twentyfourseven.player.domain.SongRequestState
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
        composeRule.onNodeWithText("Sign in").assertExists()
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
        var confirmed = 0
        val track = RequestableTrack("ALBUM_1", "12345", "Requestable track", "Composer", "3:21", true)
        composeRule.setContent {
            var pending by remember { mutableStateOf(false) }
            MaterialTheme {
                RadioApp(
                    state = sampleState().copy(
                        destination = MainDestination.More,
                        selectedStation = station.copy(
                            capabilities = StationCapabilities(supportsAuthentication = true, supportsRequests = true),
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
                    onConfirmRequest = { confirmed++ },
                )
            }
        }

        composeRule.onNodeWithText("Request").performScrollTo().performClick()
        composeRule.onNodeWithText("Request this track?").assertIsDisplayed()
        composeRule.onNodeWithText("Send request").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("12345"), prepared)
            assertEquals(1, confirmed)
        }
    }

    private fun sampleState() = MainUiState(
        stations = listOf(station),
        selectedStation = station,
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
