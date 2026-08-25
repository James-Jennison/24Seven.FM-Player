package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codeframe78.twentyfourseven.player.domain.Station

private data class AppGuideStep(
    val title: String,
    val body: String,
)

@Composable
internal fun AppGuideDialog(
    station: Station?,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
) {
    val capabilities = station?.capabilities
    val stationName = station?.shortName ?: "the selected station"
    val queueText = if (capabilities?.supportsQueue == true || capabilities?.supportsHistory == true) {
        "$stationName provides Queue or recently-played information in the Queue tab."
    } else {
        "Queue and history availability can differ by station; the app only shows sources that have been verified."
    }
    val requestText = if (capabilities?.supportsRequests == true) {
        " Song requests are available for $stationName when the current account and station rules allow them."
    } else {
        " Song request availability also differs by station and is never assumed."
    }
    val communityFeatures = buildList {
        if (capabilities?.supportsChat == true) add("Chat")
        if (capabilities?.supportsFavorites == true) add("Favorites")
        if (capabilities?.supportsAuthentication == true) add("station accounts")
        if (capabilities?.supportsListenerActivity == true) add("request activity")
    }
    val communityText = if (communityFeatures.isNotEmpty()) {
        "$stationName currently supports ${communityFeatures.joinToString()}. Community features remain subject to the app's age and participation controls."
    } else {
        "Some stations offer additional Chat, Favorites, account, or request-activity features. The Player only exposes features verified for each station."
    }
    val steps = listOf(
        AppGuideStep(
            "Welcome to 24Seven.FM Player",
            "Listen to the supported 24Seven.FM network stations from one native Android player. This short guide covers the controls and features you'll use most.",
        ),
        AppGuideStep(
            "Choose and switch stations",
            "Use the station selector or the previous/next station controls to move between stations. Features can differ by station, so the Player adapts what it shows.",
        ),
        AppGuideStep(
            "Player and Now Playing",
            "The Player screen contains Play/Pause, the current station, live Now Playing details, and artwork when the station provides it. You can leave the app normally and keep listening in the background.",
        ),
        AppGuideStep(
            "Queue and requests",
            queueText + requestText,
        ),
        AppGuideStep(
            "Community and personal features",
            communityText,
        ),
        AppGuideStep(
            "You're ready",
            "Start listening now. You can reopen this guide later from the More screen without changing your saved completion state.",
        ),
    )
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize().testTag("app_guide")) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 36.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Getting started",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        step.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("app_guide_title")
                            .semantics { heading() },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        step.body,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("app_guide_body"),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Step ${stepIndex + 1} of ${steps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("app_guide_progress"),
                    )
                }
                Spacer(Modifier.height(32.dp))
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (stepIndex > 0) {
                            TextButton(
                                onClick = { stepIndex -= 1 },
                                modifier = Modifier.testTag("app_guide_back"),
                            ) { Text("Back") }
                        } else {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("app_guide_skip"),
                            ) { Text("Skip") }
                        }
                        if (stepIndex < steps.lastIndex) {
                            Button(
                                onClick = { stepIndex += 1 },
                                modifier = Modifier.testTag("app_guide_next"),
                            ) { Text("Next") }
                        } else {
                            Button(
                                onClick = onComplete,
                                modifier = Modifier.testTag("app_guide_complete"),
                            ) { Text("Start listening") }
                        }
                    }
                }
            }
        }
    }
}
