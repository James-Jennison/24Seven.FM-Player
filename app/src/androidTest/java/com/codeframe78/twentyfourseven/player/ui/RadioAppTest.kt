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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.HistoryTrack
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueState
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationCapabilities
import org.junit.Rule
import org.junit.Test

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
        composeRule.onNodeWithText("Recently played").assertIsDisplayed()
        composeRule.onNodeWithText("Played track").assertIsDisplayed()
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
