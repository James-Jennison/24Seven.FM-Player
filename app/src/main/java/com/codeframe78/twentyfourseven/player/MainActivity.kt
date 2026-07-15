package com.codeframe78.twentyfourseven.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.codeframe78.twentyfourseven.player.ui.DoubleBackExitGate
import com.codeframe78.twentyfourseven.player.ui.MainViewModel
import com.codeframe78.twentyfourseven.player.ui.RadioApp
import com.codeframe78.twentyfourseven.player.ui.theme.TwentyFourSevenTheme

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
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(
                    container.stationRepository,
                    container.playbackController,
                    container.nowPlayingRepository,
                    container.queueRepository,
                    container.authRepository,
                    container.chatRepository,
                    container.songRequestRepository,
                    container.favoriteTracksRepository,
                    container.listenerActivityRepository,
                ),
            )
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            TwentyFourSevenTheme {
                var showExitConfirmation by remember { mutableStateOf(false) }
                val exitGate = remember { DoubleBackExitGate() }
                BackHandler(enabled = !showExitConfirmation) {
                    if (exitGate.registerPress(SystemClock.elapsedRealtime())) {
                        showExitConfirmation = true
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Press Back again to review exit options",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                RadioApp(
                    state = state,
                    onSelectStation = viewModel::selectStation,
                    onSelectDestination = viewModel::selectDestination,
                    onPlay = viewModel::play,
                    onPause = viewModel::pause,
                    onStop = viewModel::stop,
                    onRefreshQueue = viewModel::refreshQueue,
                    onRefreshChat = viewModel::refreshChat,
                    onRefreshFavorites = viewModel::refreshFavorites,
                    onRefreshListenerActivity = viewModel::refreshListenerActivity,
                    onSendChatMessage = viewModel::sendChatMessage,
                    onRefreshAuth = viewModel::refreshAuth,
                    onSignIn = viewModel::signIn,
                    onSignOut = viewModel::signOut,
                    onSearchRequests = viewModel::searchRequests,
                    onSuggestRequest = viewModel::suggestRequest,
                    onOpenRequestAlbum = viewModel::openRequestAlbum,
                    onPrepareRequest = viewModel::prepareSongRequest,
                    onPrepareFavoriteRequest = viewModel::prepareFavoriteRequest,
                    onCancelRequest = viewModel::cancelSongRequest,
                    onConfirmRequest = viewModel::confirmSongRequest,
                    onUseLastStationAtStartup = viewModel::useLastStationAtStartup,
                    onSetStartupStation = viewModel::setStartupStation,
                )
                if (showExitConfirmation) {
                    AlertDialog(
                        onDismissRequest = {
                            showExitConfirmation = false
                            exitGate.reset()
                        },
                        title = { Text("Exit 24Seven.FM Player?") },
                        text = { Text("Exiting stops live playback. You can leave the app normally to keep listening in the background.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.stop()
                                    finishAndRemoveTask()
                                },
                            ) { Text("Stop and exit") }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showExitConfirmation = false
                                    exitGate.reset()
                                },
                            ) { Text("Keep listening") }
                        },
                    )
                }
            }
        }
    }
}
