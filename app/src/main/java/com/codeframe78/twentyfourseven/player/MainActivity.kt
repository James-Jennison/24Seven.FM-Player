package com.codeframe78.twentyfourseven.player

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.ui.MainUiState
import com.codeframe78.twentyfourseven.player.ui.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RadioApplication).appContainer
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(container.stationRepository))
            val state by vm.uiState.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = darkColorScheme()) { RadioApp(state, vm::selectStation) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioApp(state: MainUiState, onSelect: (StationId) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Box {
                    TextButton(onClick = { menuOpen = true }) { Text(state.selectedStation?.name ?: "Choose station") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        state.stations.forEach { station ->
                            DropdownMenuItem(text = { Text(station.name) }, onClick = {
                                onSelect(station.id); menuOpen = false
                            })
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
            Text("Track metadata will appear here", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Stream endpoints pending verification", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            FilledIconButton(onClick = {}, enabled = false, shape = CircleShape) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
            Spacer(Modifier.height(16.dp))
            Text("LIVE • Not connected", style = MaterialTheme.typography.labelLarge)
        }
    }
}
