package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.codeframe78.twentyfourseven.player.R
import com.codeframe78.twentyfourseven.player.domain.AudioOutputKind
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
import com.codeframe78.twentyfourseven.player.ui.theme.StationPalette
import com.codeframe78.twentyfourseven.player.ui.theme.stationPalette

private val ExpandedPlayerBreakpoint = 840.dp
private val CompactPlayerReservedHeight = 420.dp
private val MinimumCompactArtworkSize = 140.dp
private val MaximumCompactArtworkSize = 300.dp
private val SleepTimerPresetsMinutes = listOf(15, 30, 45, 60, 90)

@Immutable
internal data class SleepTimerActions(
    val onSet: (Long) -> Unit = {},
    val onCancel: () -> Unit = {},
)

@Immutable
internal data class AudioOutputActions(
    val onOpenChooser: () -> Unit = {},
)

@Composable
internal fun AdaptivePlayerScreen(
    state: MainUiState,
    padding: PaddingValues,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions = SleepTimerActions(),
    audioOutputActions: AudioOutputActions = AudioOutputActions(),
) {
    val palette = stationPalette(state.selectedStation?.id)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.glow.copy(alpha = 0.62f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        if (maxWidth >= ExpandedPlayerBreakpoint) {
            ExpandedPlayerContent(state, palette, onSelectStation, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions)
        } else {
            val availableArtworkWidth = maxWidth - 40.dp
            val availableArtworkHeight = (maxHeight - CompactPlayerReservedHeight)
                .coerceIn(MinimumCompactArtworkSize, MaximumCompactArtworkSize)
            val artworkSize = minOf(
                availableArtworkWidth,
                availableArtworkHeight,
                MaximumCompactArtworkSize,
            )
            CompactPlayerContent(state, palette, artworkSize, onSelectStation, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions)
        }
    }
}

@Composable
private fun CompactPlayerContent(
    state: MainUiState,
    palette: StationPalette,
    artworkSize: androidx.compose.ui.unit.Dp,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("compact_player_scroll")
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NowPlayingArtwork(state, palette, Modifier.size(artworkSize))
        NowPlayingDetails(state, palette)
        PrimaryPlayerControls(state, onSelectStation, onPlay, onPause)
        StationSelector(state, onSelectStation)
        PlaybackDetails(state, onStop, sleepTimerActions, audioOutputActions)
    }
}

@Composable
private fun ExpandedPlayerContent(
    state: MainUiState,
    palette: StationPalette,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
) {
    Row(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NowPlayingArtwork(state, palette, Modifier.fillMaxWidth().aspectRatio(1f))
        }
        Column(
            Modifier.weight(1.15f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            NowPlayingDetails(state, palette, Alignment.Start)
            Spacer(Modifier.height(28.dp))
            PrimaryPlayerControls(state, onSelectStation, onPlay, onPause)
            Spacer(Modifier.height(8.dp))
            PlaybackDetails(state, onStop, sleepTimerActions, audioOutputActions)
            Spacer(Modifier.height(32.dp))
            Text("Choose a station", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            StationSelector(state, onSelectStation, edgePadding = 0.dp)
            state.selectedStation?.let { station ->
                Spacer(Modifier.height(20.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(station.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            station.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingArtwork(
    state: MainUiState,
    palette: StationPalette,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(palette.glow, palette.secondary.copy(alpha = 0.48f)),
                ),
            )
            .padding(8.dp),
    ) {
        AsyncImage(
            model = state.nowPlaying.artworkUrl,
            contentDescription = if (state.nowPlaying.artworkUrl == null) {
                "24Seven.FM station artwork"
            } else {
                "Album artwork"
            },
            contentScale = ContentScale.Crop,
            fallback = painterResource(R.drawable.app_logo),
            error = painterResource(R.drawable.app_logo),
            placeholder = painterResource(R.drawable.app_logo),
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)).testTag("now_playing_artwork"),
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f),
            contentColor = palette.accent,
            shape = RoundedCornerShape(100.dp),
        ) {
            Text(
                "●  LIVE",
                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun NowPlayingDetails(
    state: MainUiState,
    palette: StationPalette,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    val metadata = parseNowPlayingMetadata(state.nowPlaying.displayTitle)
    Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            state.selectedStation?.name ?: "24Seven.FM",
            color = palette.accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            metadata.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = if (alignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("now_playing_title"),
        )
        metadata.artist?.let { artist ->
            Text(
                artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (alignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PlaybackStatusPill(state, palette)
    }
}

@Composable
private fun PlaybackStatusPill(state: MainUiState, palette: StationPalette) {
    Surface(
        color = palette.glow.copy(alpha = 0.88f),
        contentColor = palette.accent,
        shape = RoundedCornerShape(100.dp),
        modifier = Modifier
            .semantics { stateDescription = state.playback.status.accessibleName }
            .testTag("playback_status"),
    ) {
        Text(
            state.playback.status.userMessage,
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PrimaryPlayerControls(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val isActive = state.playback.status.isActive
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { adjacentStationId(state.stations, state.selectedStation?.id, -1)?.let(onSelectStation) },
            enabled = state.stations.size > 1,
            modifier = Modifier.size(56.dp).testTag("previous_station"),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous station")
        }
        Spacer(Modifier.width(22.dp))
        FilledIconButton(
            onClick = if (isActive) onPause else onPlay,
            enabled = state.selectedStation?.streams?.isNotEmpty() == true,
            modifier = Modifier.size(76.dp).testTag("primary_play_pause"),
            shape = CircleShape,
        ) {
            Icon(
                if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isActive) "Pause live radio" else "Play live radio",
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.width(22.dp))
        IconButton(
            onClick = { adjacentStationId(state.stations, state.selectedStation?.id, 1)?.let(onSelectStation) },
            enabled = state.stations.size > 1,
            modifier = Modifier.size(56.dp).testTag("next_station"),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next station")
        }
    }
}

@Composable
private fun PlaybackDetails(
    state: MainUiState,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            state.selectedStation?.streams?.minByOrNull { it.priority }?.qualityLabel.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onStop, enabled = state.playback.status != PlaybackStatus.Idle) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Stop")
            }
            SleepTimerControl(state, sleepTimerActions)
        }
        AudioOutputControl(state, audioOutputActions)
    }
}

@Composable
private fun AudioOutputControl(state: MainUiState, actions: AudioOutputActions) {
    val output = state.playback.audioOutput
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = actions.onOpenChooser,
            modifier = Modifier
                .semantics { stateDescription = "Current output: ${output.displayName}" }
                .testTag("audio_output_open"),
        ) {
            Icon(output.kind.icon, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Audio output")
        }
        Text(
            "Using ${output.displayName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("audio_output_current"),
        )
    }
}

private val AudioOutputKind.icon: ImageVector
    get() = when (this) {
        AudioOutputKind.Device -> Icons.Default.Speaker
        AudioOutputKind.Bluetooth -> Icons.Default.BluetoothAudio
        AudioOutputKind.Wired -> Icons.Default.Headphones
        AudioOutputKind.Remote -> Icons.Default.CastConnected
    }

@Composable
private fun SleepTimerControl(state: MainUiState, actions: SleepTimerActions) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val sleepTimer = state.playback.sleepTimer
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = { showDialog = true },
            enabled = state.selectedStation?.streams?.isNotEmpty() == true,
            modifier = Modifier.testTag("sleep_timer_open"),
        ) {
            Icon(Icons.Default.Bedtime, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (sleepTimer.isActive) "Adjust timer" else "Sleep timer")
        }
        if (sleepTimer.isActive) {
            Text(
                "Stops in ${formatSleepTimerRemaining(sleepTimer.remainingMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .semantics { stateDescription = "Sleep timer active" }
                    .testTag("sleep_timer_remaining"),
            )
            TextButton(
                onClick = actions.onCancel,
                modifier = Modifier.testTag("sleep_timer_cancel"),
            ) {
                Text("Cancel timer")
            }
        }
    }
    if (showDialog) {
        SleepTimerDialog(
            isAdjusting = sleepTimer.isActive,
            onSet = { durationMillis ->
                actions.onSet(durationMillis)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun SleepTimerDialog(
    isAdjusting: Boolean,
    onSet: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var customMinutes by rememberSaveable { mutableStateOf("") }
    val parsedMinutes = customMinutes.toLongOrNull()
    val customIsValid = parsedMinutes != null && parsedMinutes in 1L..720L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAdjusting) "Adjust sleep timer" else "Set sleep timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Playback stops when the countdown ends, even if the app is in the background.")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SleepTimerPresetsMinutes) { minutes ->
                        Button(
                            onClick = { onSet(minutes * 60_000L) },
                            modifier = Modifier.testTag("sleep_timer_preset_$minutes"),
                        ) {
                            Text("$minutes min")
                        }
                    }
                }
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { input -> customMinutes = input.filter(Char::isDigit).take(3) },
                    label = { Text("Custom minutes") },
                    supportingText = { Text("Enter 1–720 minutes") },
                    isError = customMinutes.isNotEmpty() && !customIsValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sleep_timer_custom_minutes"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedMinutes?.let { onSet(it * 60_000L) } },
                enabled = customIsValid,
                modifier = Modifier.testTag("sleep_timer_confirm_custom"),
            ) {
                Text(if (isAdjusting) "Update" else "Start")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

internal fun formatSleepTimerRemaining(remainingMillis: Long): String {
    val totalSeconds = ((remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

@Composable
private fun StationSelector(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
    edgePadding: androidx.compose.ui.unit.Dp = 2.dp,
) {
    LazyRow(
        Modifier.fillMaxWidth().testTag("station_selector"),
        contentPadding = PaddingValues(horizontal = edgePadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.stations, key = { it.id.value }) { station ->
            val selected = station.id == state.selectedStation?.id
            val palette = stationPalette(station.id)
            Card(
                onClick = { onSelectStation(station.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) palette.glow else MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.accent else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .width(152.dp)
                    .heightIn(min = 86.dp)
                    .semantics {
                        this.selected = selected
                        role = Role.RadioButton
                        contentDescription = if (selected) "${station.name}, selected" else station.name
                    }
                    .testTag("station_card_${station.id.value}"),
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        station.shortName,
                        color = palette.accent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        station.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("station_card_description_${station.id.value}"),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PersistentMiniPlayer(
    state: MainUiState,
    onSelectDestination: (MainDestination) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val palette = stationPalette(state.selectedStation?.id)
    Surface(
        onClick = { onSelectDestination(MainDestination.Player) },
        modifier = Modifier.fillMaxWidth().testTag("persistent_mini_player"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 5.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = state.nowPlaying.artworkUrl,
                contentDescription = "Now playing album artwork",
                contentScale = ContentScale.Crop,
                fallback = painterResource(R.drawable.app_logo),
                error = painterResource(R.drawable.app_logo),
                placeholder = painterResource(R.drawable.app_logo),
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.nowPlaying.displayTitle ?: "Live radio",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.selectedStation?.shortName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = if (state.playback.status.isActive) onPause else onPlay,
                enabled = state.selectedStation?.streams?.isNotEmpty() == true,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    if (state.playback.status.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.playback.status.isActive) "Pause live radio" else "Play live radio",
                )
            }
        }
    }
}

internal fun adjacentStationId(
    stations: List<Station>,
    selectedId: StationId?,
    offset: Int,
): StationId? {
    if (stations.isEmpty()) return null
    val selectedIndex = stations.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    val destinationIndex = Math.floorMod(selectedIndex + offset, stations.size)
    return stations[destinationIndex].id
}

@Immutable
private data class NowPlayingMetadata(val title: String, val artist: String?)

private fun parseNowPlayingMetadata(raw: String?): NowPlayingMetadata {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return NowPlayingMetadata("Live radio", null)
    val separator = value.indexOf(" - ")
    return if (separator > 0 && separator < value.lastIndex - 2) {
        NowPlayingMetadata(
            title = value.substring(separator + 3).trim(),
            artist = value.substring(0, separator).trim(),
        )
    } else {
        NowPlayingMetadata(value, null)
    }
}

private val com.codeframe78.twentyfourseven.player.domain.StreamVariant.qualityLabel: String?
    get() {
        val formatLabel = when (format) {
            StreamFormat.Aac -> "AAC"
            StreamFormat.Mp3 -> "MP3"
            StreamFormat.Hls -> "HLS"
            StreamFormat.Unknown -> null
        }
        return listOfNotNull(formatLabel, bitrateKbps?.let { "$it kbps" })
            .takeIf(List<String>::isNotEmpty)
            ?.joinToString(" · ")
    }

private val PlaybackStatus.isActive: Boolean
    get() = this in setOf(
        PlaybackStatus.Connecting,
        PlaybackStatus.Buffering,
        PlaybackStatus.Playing,
        PlaybackStatus.Retrying,
        PlaybackStatus.WaitingForNetwork,
    )

private val PlaybackStatus.accessibleName: String
    get() = when (this) {
        PlaybackStatus.Idle -> "Ready"
        PlaybackStatus.Connecting -> "Connecting"
        PlaybackStatus.Buffering -> "Buffering"
        PlaybackStatus.Playing -> "Playing"
        PlaybackStatus.Paused -> "Paused"
        PlaybackStatus.Retrying -> "Reconnecting with fallback"
        PlaybackStatus.WaitingForNetwork -> "Waiting for network"
        PlaybackStatus.Error -> "Playback error"
    }

private val PlaybackStatus.userMessage: String
    get() = when (this) {
        PlaybackStatus.Idle -> "LIVE • Not connected"
        PlaybackStatus.Connecting -> "Connecting to live radio…"
        PlaybackStatus.Buffering -> "Buffering live audio…"
        PlaybackStatus.Playing -> "Playing live"
        PlaybackStatus.Paused -> "Playback paused"
        PlaybackStatus.Retrying -> "Primary stream unavailable · trying fallback"
        PlaybackStatus.WaitingForNetwork -> "No network · playback will resume automatically"
        PlaybackStatus.Error -> "Unable to play this station · try again"
    }
