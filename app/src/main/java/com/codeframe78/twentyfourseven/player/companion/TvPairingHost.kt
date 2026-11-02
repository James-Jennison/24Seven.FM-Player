package com.codeframe78.twentyfourseven.player.companion

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codeframe78.twentyfourseven.player.RadioApplication
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opens the TV pairing screen over the current one and owns its [CompanionPairingController]. Closing it, or leaving
 * the Player, ends any pairing, so the phone never keeps a TV connection open in the background.
 */
@Composable
fun TvPairingHost(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as RadioApplication).appContainer
    val scope = rememberCoroutineScope()
    val station by container.stationRepository.observeSelectedStation().collectAsState(initial = null)
    val stationId = station?.id
    val auth by remember(stationId) {
        stationId?.let { container.authRepository.observeAuth(it) } ?: kotlinx.coroutines.flow.emptyFlow()
    }.collectAsState(initial = null)
    val account = auth?.takeIf { it.status == AuthStatus.SignedIn }?.displayName?.trim().orEmpty()

    val controller = remember {
        CompanionPairingController(
            scope = scope,
            clockMillis = System::currentTimeMillis,
            connect = { qr -> withContext(Dispatchers.IO) { PinnedTlsConnector().connect(qr) } },
            probe = { host, port -> withContext(Dispatchers.IO) { PinnedTlsConnector().probe(host, port) } },
            handlerFor = { approve ->
                PlayerCompanionRequestHandler(
                    stations = container.stationRepository,
                    requests = container.songRequestRepository,
                    auth = container.authRepository,
                    queue = container.queueRepository,
                    listenerActivity = container.listenerActivityRepository,
                    communitySafety = container.communitySafetyRepository,
                    approveOnPhone = approve,
                )
            },
        )
    }
    DisposableEffect(controller) { onDispose { controller.close() } }
    LaunchedEffect(stationId, account) {
        if (stationId != null) controller.onStationOrAccountChanged(stationId.value, account)
    }

    val state by controller.uiState.collectAsState()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (stationId == null || account.isEmpty()) {
                    Text(
                        "Sign in to a station first. Requests from the TV are made with the account signed in here.",
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    TvPairingScreen(
                        state = state,
                        onScanQr = {
                            CompanionQrScanner(context).scan { result ->
                                when (result) {
                                    is QrScanResult.Scanned ->
                                        controller.pairWithQr(result.text, Build.MODEL, account, stationId.value)
                                    QrScanResult.Cancelled -> Unit
                                    QrScanResult.Unavailable -> controller.showMessage(
                                        "The code scanner is not available on this phone. Enter the details shown on the TV instead.",
                                    )
                                }
                            }
                        },
                        onPairWithCode = { host, port, code ->
                            controller.pairWithCode(host, port, code, Build.MODEL, account, stationId.value)
                        },
                        onConfirmFingerprint = controller::confirmFingerprint,
                        onDecideRequest = controller::decideRequest,
                        onRevoke = controller::revoke,
                        onDismissMessage = controller::dismissMessage,
                    )
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Close") }
            }
        }
    }
}
