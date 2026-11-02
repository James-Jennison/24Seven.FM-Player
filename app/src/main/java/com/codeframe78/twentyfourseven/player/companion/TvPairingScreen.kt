package com.codeframe78.twentyfourseven.player.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The phone's TV pairing screen. Stateless apart from the typed fields: it shows [state] and reports the listener's
 * choices upward. It is not reachable from the app until [TvCompanionFeature.ENABLED] is turned on and an entry point
 * is added.
 */
@Composable
fun TvPairingScreen(
    state: PairingUiState,
    onScanQr: () -> Unit,
    onPairWithCode: (host: String, port: Int, code: String) -> Unit,
    onConfirmFingerprint: (Boolean) -> Unit,
    onDecideRequest: (Boolean) -> Unit,
    onChooseTrust: (Boolean) -> Unit,
    onDecideHandoff: (Boolean) -> Unit,
    onAcknowledgeHandoffBlocked: () -> Unit,
    onRevoke: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pair with a TV", style = MaterialTheme.typography.titleLarge)
        when (state) {
            PairingUiState.Idle -> IdleContent(onScanQr, onPairWithCode)
            PairingUiState.Connecting -> Text("Connecting to the TV…")
            is PairingUiState.ConfirmFingerprint -> {
                Text("Check that this matches the fingerprint on the TV screen.")
                Text(state.fingerprint, style = MaterialTheme.typography.headlineMedium)
                Text("Only continue if every group matches. Your code is not sent until you confirm.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onConfirmFingerprint(true) }) { Text("They match") }
                    OutlinedButton(onClick = { onConfirmFingerprint(false) }) { Text("They differ") }
                }
            }
            is PairingUiState.HandoffBlocked -> {
                Text("Paired with ${state.tvName}.")
                Text("The TV can't be signed in yet.", style = MaterialTheme.typography.titleMedium)
                Text(state.reason)
                Button(onClick = onAcknowledgeHandoffBlocked) { Text("OK") }
            }
            is PairingUiState.HandoffOffer -> {
                Text("Paired with ${state.tvName}.")
                Text(
                    "Sign this TV in to ${state.station.uppercase()} as ${state.account}?",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "The TV could then search and request songs by itself with your account, without asking here. It keeps " +
                        "your sign-in after this pairing ends, until you remove it on the TV. Signing out here does not sign " +
                        "the TV out. Only do this on a TV you own.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDecideHandoff(false) }) { Text("Not now") }
                    OutlinedButton(onClick = { onDecideHandoff(true) }) { Text("Sign the TV in") }
                }
            }
            is PairingUiState.TrustChoice -> {
                Text("Paired with ${state.tvName}.")
                Text("Let this TV request songs without asking each time?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Until you end the pairing it can request songs with your account. The phone still checks each song " +
                        "and the community rules first. Nothing is saved.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onChooseTrust(false) }) { Text("Ask each time") }
                    OutlinedButton(onClick = { onChooseTrust(true) }) { Text("Yes, for this pairing") }
                }
            }
            is PairingUiState.Active -> {
                Text("Paired with ${state.tvName}.")
                Text(
                    if (state.trusted) {
                        "Songs you choose on the TV are requested on ${state.station.uppercase()} without asking, until you end the pairing."
                    } else {
                        "Songs you choose on the TV are requested on ${state.station.uppercase()}. You approve each one here."
                    },
                )
                state.notice?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                OutlinedButton(onClick = onRevoke) { Text("End pairing") }
            }
            is PairingUiState.Approval -> {
                Text("${state.tvName} asks to request:")
                Text(state.track.title, style = MaterialTheme.typography.titleMedium)
                if (state.track.artist.isNotEmpty()) Text(state.track.artist)
                if (state.track.album.isNotEmpty()) Text(state.track.album)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDecideRequest(true) }) { Text("Request it") }
                    OutlinedButton(onClick = { onDecideRequest(false) }) { Text("Not now") }
                }
            }
            is PairingUiState.Message -> {
                Text(state.text)
                Button(onClick = onDismissMessage) { Text("OK") }
            }
        }
    }
}

@Composable
private fun IdleContent(onScanQr: () -> Unit, onPairWithCode: (String, Int, String) -> Unit) {
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    Text("Open Pair with phone on the TV app, then scan the code it shows.")
    Button(onClick = onScanQr) { Text("Scan the TV's code") }
    Text("Or enter the details shown on the TV:", style = MaterialTheme.typography.titleSmall)
    OutlinedTextField(host, { host = it.trim() }, label = { Text("TV address") }, singleLine = true)
    OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, label = { Text("Port") }, singleLine = true)
    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("Six-digit code") }, singleLine = true)
    OutlinedButton(
        onClick = { onPairWithCode(host, port.toIntOrNull() ?: 0, code) },
        enabled = host.isNotEmpty() && port.isNotEmpty() && code.length == 6,
    ) { Text("Connect") }
}
