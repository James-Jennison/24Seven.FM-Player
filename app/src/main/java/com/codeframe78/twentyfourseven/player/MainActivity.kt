package com.codeframe78.twentyfourseven.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
import com.codeframe78.twentyfourseven.player.ui.MainUiState
import com.codeframe78.twentyfourseven.player.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        val container = (application as RadioApplication).appContainer
        setContent {
            val vm: MainViewModel = viewModel(
                factory = MainViewModel.Factory(
                    container.stationRepository,
                    container.playbackController,
                    container.nowPlayingRepository,
                ),
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = darkColorScheme()) {
                RadioApp(state, vm::selectStation, vm::play, vm::pause, vm::stop)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioApp(
    state: MainUiState,
    onSelect: (StationId) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Box {
                    TextButton(onClick = { menuOpen = true }) {
                        Text(state.selectedStation?.name ?: "Choose station")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        state.stations.forEach { station ->
                            DropdownMenuItem(
                                text = { Text(station.name) },
                                onClick = {
                                    onSelect(station.id)
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(true, {}, { Icon(Icons.Default.Radio, null) }, label = { Text("Playing") })
                NavigationBarItem(false, {}, { Icon(Icons.AutoMirrored.Filled.Chat, null) }, label = { Text("Chat") })
                NavigationBarItem(false, {}, { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }, label = { Text("Queue") })
                NavigationBarItem(false, {}, { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("More") })
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(state.selectedStation?.shortName ?: "24seven.FM", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                state.nowPlaying.displayTitle ?: "Live radio",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                state.playback.errorMessage ?: state.selectedStation?.description.orEmpty(),
                color = if (state.playback.status == PlaybackStatus.Error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(32.dp))
            val isPlaying = state.playback.status in setOf(
                PlaybackStatus.Connecting,
                PlaybackStatus.Buffering,
                PlaybackStatus.Playing,
                PlaybackStatus.Retrying,
            )
            FilledIconButton(
                onClick = if (isPlaying) onPause else onPlay,
                enabled = state.selectedStation?.streams?.isNotEmpty() == true,
                shape = CircleShape,
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
            TextButton(onClick = onStop, enabled = state.playback.status != PlaybackStatus.Idle) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Text("Stop")
            }
            Text("LIVE • ${state.playback.status.displayName}", style = MaterialTheme.typography.labelLarge)
            state.selectedStation?.streams?.minByOrNull { it.priority }?.qualityLabel?.let { quality ->
                Text(
                    quality,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
            ?.joinToString(" • ")
    }

private val PlaybackStatus.displayName: String
    get() = when (this) {
        PlaybackStatus.Idle -> "Not connected"
        PlaybackStatus.Connecting -> "Connecting"
        PlaybackStatus.Buffering -> "Buffering"
        PlaybackStatus.Playing -> "Playing"
        PlaybackStatus.Paused -> "Paused"
        PlaybackStatus.Retrying -> "Trying fallback"
        PlaybackStatus.Error -> "Playback error"
    }
