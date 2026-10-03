package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.ui.theme.stationPalette
import com.codeframe78.twentyfourseven.player.ui.theme.themedAccent

/**
 * The first-run screen, and the guide reopened from More: one sheet that names the five stations and lets the
 * listener pick where to start. There are no steps to page through; everything else is discovered in use.
 */
@Composable
internal fun AppGuideDialog(
    stations: List<Station>,
    selectedStationId: StationId?,
    onSelectStation: (StationId) -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Dialog already applies a platform dim behind this window. Keep the in-app
                // scrim subtle so the Player remains readable through the sheet.
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
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp,
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "Welcome to 24Seven.FM",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Pick a station",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("app_guide_title")
                            .semantics { heading() },
                    )
                    Text(
                        "Five live stations, one player. Press Play on the station you choose; Queue, requests and " +
                            "Chat appear when that station offers them, and you can change station any time from " +
                            "the cards under the artwork or by swiping it.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("app_guide_body"),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stations.forEach { station ->
                            GuideStationRow(
                                station = station,
                                selected = station.id == selectedStationId,
                                onClick = { onSelectStation(station.id) },
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("app_guide_skip"),
                        ) { Text("Skip") }
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

@Composable
private fun GuideStationRow(station: Station, selected: Boolean, onClick: () -> Unit) {
    val palette = stationPalette(station.id)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) palette.glow else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) palette.accent else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics {
                this.selected = selected
                role = Role.RadioButton
                contentDescription = if (selected) "${station.name}, selected" else station.name
            }
            .testTag("app_guide_station_${station.id.value}"),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(stationSelectorLogoResource(station.id)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    station.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) palette.accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    station.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) palette.accent.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = palette.themedAccent())
            }
        }
    }
}
