package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class AppGuideStep(
    val title: String,
    val body: String,
)

@Composable
internal fun AppGuideDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
) {
    val steps = listOf(
        AppGuideStep(
            "Pick a station and press Play",
            "Start with any station card, then use Play to listen. The Player stays useful without a tutorial, and this quick tour is always optional.",
        ),
        AppGuideStep(
            "Everything adapts to the station",
            "Use the station selector or previous/next controls to switch. Queue, requests, and community options only appear when the selected station supports them.",
        ),
        AppGuideStep(
            "Explore when you want",
            "24Seven.FM brings the network together in one Player. Browse Player, Favorites, Chat, Queue, and More; each screen only shows network features that are supported. You can reopen this guide later from More whenever you need it.",
        ),
    )
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Dialog already applies a platform dim behind this window. Keep the in-app
                // scrim subtle so the Player remains readable through the coach overlay.
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f))
                .padding(20.dp)
                .testTag("app_guide"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .testTag("app_guide_overlay"),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                tonalElevation = 8.dp,
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Quick tour",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Step ${stepIndex + 1} of ${steps.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("app_guide_progress"),
                        )
                    }
                    Text(
                        step.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("app_guide_title")
                            .semantics { heading() },
                    )
                    Text(
                        step.body,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("app_guide_body"),
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("app_guide_skip"),
                        ) { Text("Skip") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (stepIndex > 0) {
                                TextButton(
                                    onClick = { stepIndex -= 1 },
                                    modifier = Modifier.testTag("app_guide_back"),
                                ) { Text("Back") }
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
}
