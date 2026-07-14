package com.codeframe78.twentyfourseven.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codeframe78.twentyfourseven.player.ui.MainViewModel
import com.codeframe78.twentyfourseven.player.ui.RadioApp

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
                ),
            )
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            MaterialTheme(colorScheme = darkColorScheme()) {
                RadioApp(
                    state = state,
                    onSelectStation = viewModel::selectStation,
                    onSelectDestination = viewModel::selectDestination,
                    onPlay = viewModel::play,
                    onPause = viewModel::pause,
                    onStop = viewModel::stop,
                    onRefreshQueue = viewModel::refreshQueue,
                    onRefreshChat = viewModel::refreshChat,
                    onSendChatMessage = viewModel::sendChatMessage,
                    onRefreshAuth = viewModel::refreshAuth,
                    onSignIn = viewModel::signIn,
                    onSignOut = viewModel::signOut,
                    onSearchRequests = viewModel::searchRequests,
                    onOpenRequestAlbum = viewModel::openRequestAlbum,
                    onPrepareRequest = viewModel::prepareSongRequest,
                    onCancelRequest = viewModel::cancelSongRequest,
                    onConfirmRequest = viewModel::confirmSongRequest,
                )
            }
        }
    }
}
