package com.codeframe78.twentyfourseven.player

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.mediarouter.app.SystemOutputSwitcherDialogController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.codeframe78.twentyfourseven.player.domain.StationPageTrustPolicy
import com.codeframe78.twentyfourseven.player.ui.DoubleBackExitGate
import com.codeframe78.twentyfourseven.player.ui.AudioOutputActions
import com.codeframe78.twentyfourseven.player.ui.DiagnosticActions
import com.codeframe78.twentyfourseven.player.ui.DiagnosticEnvironment
import com.codeframe78.twentyfourseven.player.ui.DiagnosticUi
import com.codeframe78.twentyfourseven.player.ui.MainViewModel
import com.codeframe78.twentyfourseven.player.ui.RadioApp
import com.codeframe78.twentyfourseven.player.ui.SleepTimerActions
import com.codeframe78.twentyfourseven.player.ui.theme.TwentyFourSevenTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var audioOutputRefreshJob: Job? = null
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
                    container.communitySafetyRepository,
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
                    sleepTimerActions = SleepTimerActions(
                        onSet = viewModel::setSleepTimer,
                        onCancel = viewModel::cancelSleepTimer,
                    ),
                    audioOutputActions = AudioOutputActions(
                        onOpenChooser = { showAudioOutputSwitcher(viewModel::refreshAudioOutput) },
                    ),
                    diagnosticUi = DiagnosticUi(
                        environment = diagnosticEnvironment(),
                        actions = DiagnosticActions(
                            onCopy = ::copyDiagnostics,
                            onShare = ::shareDiagnostics,
                        ),
                    ),
                    onRefreshQueue = viewModel::refreshQueue,
                    onRefreshChat = viewModel::refreshChat,
                    onRefreshFavorites = viewModel::refreshFavorites,
                    onRefreshListenerActivity = viewModel::refreshListenerActivity,
                    onSendChatMessage = viewModel::sendChatMessage,
                    communitySafetyActions = com.codeframe78.twentyfourseven.player.ui.CommunitySafetyActions(
                        onSubmitAgeScreen = viewModel::submitCommunityAgeScreen,
                        onAcceptTerms = viewModel::acceptCommunityTerms,
                        onSetCommunityContentVisible = viewModel::setCommunityContentVisible,
                        onBlockUser = viewModel::blockCommunityUser,
                        onUnblockUser = viewModel::unblockCommunityUser,
                        onBeginReport = viewModel::beginAbuseReport,
                        onRetryReport = viewModel::retryAbuseReport,
                        onSubmitReport = viewModel::submitAbuseReport,
                        onDismissReport = viewModel::dismissAbuseReport,
                    ),
                    onRefreshAuth = viewModel::refreshAuth,
                    onSignIn = viewModel::signIn,
                    onSignOut = viewModel::signOut,
                    onSearchRequests = viewModel::searchRequests,
                    onSuggestRequest = viewModel::suggestRequest,
                    onOpenRequestAlbum = viewModel::openRequestSearchResult,
                    onPrepareRequest = viewModel::prepareSongRequest,
                    onPrepareFavoriteRequest = viewModel::prepareFavoriteRequest,
                    onCancelRequest = viewModel::cancelSongRequest,
                    onConfirmRequest = viewModel::confirmSongRequest,
                    onUseLastStationAtStartup = viewModel::useLastStationAtStartup,
                    onSetStartupStation = viewModel::setStartupStation,
                    onOpenStationPage = { page ->
                        val trustedUrl = StationPageTrustPolicy.trustedUrl(state.selectedStation, page)
                        if (trustedUrl == null) {
                            Toast.makeText(this@MainActivity, "This station page is not available.", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                    .launchUrl(this@MainActivity, android.net.Uri.parse(trustedUrl))
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(this@MainActivity, "No browser is available on this device.", Toast.LENGTH_SHORT).show()
                            } catch (_: SecurityException) {
                                Toast.makeText(this@MainActivity, "The browser could not open this station page.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
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

    private fun showAudioOutputSwitcher(refreshAudioOutput: () -> Unit) {
        val shown = runCatching {
            SystemOutputSwitcherDialogController.showDialog(this)
        }.getOrDefault(false)
        if (!shown) {
            Toast.makeText(
                this,
                "Audio output selection is unavailable on this device.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        audioOutputRefreshJob?.cancel()
        audioOutputRefreshJob = lifecycleScope.launch {
            repeat(30) {
                delay(500L)
                refreshAudioOutput()
            }
        }
    }

    private fun copyDiagnostics(report: String) {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("24Seven.FM Player diagnostics", report))
        Toast.makeText(this, "Diagnostics copied", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    private fun diagnosticEnvironment(): DiagnosticEnvironment {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return DiagnosticEnvironment(
            appVersion = packageInfo.versionName ?: "Unknown",
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            buildType = if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) "debug" else "release",
            androidRelease = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            deviceManufacturer = Build.MANUFACTURER,
            deviceModel = Build.MODEL,
        )
    }

    private fun shareDiagnostics(report: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "24Seven.FM Player diagnostics")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        try {
            startActivity(Intent.createChooser(shareIntent, "Share diagnostics"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No sharing app is available on this device.", Toast.LENGTH_SHORT).show()
        }
    }
}
