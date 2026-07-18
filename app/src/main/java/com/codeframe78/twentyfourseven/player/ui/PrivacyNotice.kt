package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.R

private const val ProjectIssuesUrl = "https://github.com/codeframe78/24Seven.FM-Player/issues"

@Composable
internal fun PrivacySection() {
    var showNotice by remember { mutableStateOf(false) }
    var showThirdPartyNotices by remember { mutableStateOf(false) }
    Text("Privacy", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No ads, analytics, tracking, or developer-operated data server.")
            TextButton(onClick = { showNotice = true }) { Text("Read privacy notice") }
            TextButton(onClick = { showThirdPartyNotices = true }) {
                Text("Open-source licenses")
            }
        }
    }

    if (showNotice) {
        PrivacyNoticeDialog(onDismiss = { showNotice = false })
    }
    if (showThirdPartyNotices) {
        ThirdPartyNoticesDialog(onDismiss = { showThirdPartyNotices = false })
    }
}

@Composable
private fun PrivacyNoticeDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy notice") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Data handled by the Alpha", style = MaterialTheme.typography.titleSmall)
                Text("The app connects directly to the selected 24Seven.FM station for live audio, artwork, public Queue/History, catalog, and Chat data.")
                Text("Credentials are sent only when you explicitly sign in. Passwords and security-code answers are not saved. Successful station sessions and display identity are encrypted locally with Android Keystore and are removed by Sign out, clearing app data, or uninstalling.")
                Text("The Player signs in to pre-existing station accounts; it does not create or delete them. Sign out removes only this device's protected session. It does not delete the station account, public posts, request history, station/server logs, or sent email. Station-side access, correction, retention, and deletion are controlled by the applicable station or network operator.")
                Text("Where verified, recent request summaries, station-reported request readiness, membership indicators, and favorite-track lists are loaded for the signed-in station account. They remain in memory and are cleared from the interface on Sign out.")
                Text("The app stores only the adult/not-adult age-screen result, accepted Terms version, mature-community-content visibility choice, station-scoped blocked identities, and which stations you enable for Chat-mention notifications. It does not save the entered date of birth.")
                Text("All app-private data is excluded from Android cloud backup and device-to-device transfer.")
                Text("When you explicitly choose Review email for an abuse report, the app prepares a bounded draft with the selected station, report category, reported user, content snapshot, your entered name or station nickname, and optional details. Android opens your chosen email app with the monitored moderation recipient, subject, and body prefilled. You may edit, cancel, or send it there. The Player cannot read your email account or confirm sending or delivery, and it does not save the draft or report. The email app and recipient may retain a sent report under their own practices.")
                Text("Selected public station pages open only after you tap them in a secure browser tab. The app passes an allowlisted station address but does not copy its protected session into the browser. Browser cookies and history are managed separately by your browser.")
                Text("Chat posts, song requests, and optional request messages are sent only after explicit actions. Chat history and pending request text are transient. If enabled, exact-name Chat mentions are matched on-device; notification deduplication retains only bounded message fingerprints in memory, and notification text contains the station and sender but not the Chat message. The current preview works only while Chat is actively refreshing and does not claim closed-app push delivery. The app contains no ads, analytics, tracking SDK, crash reporting, or developer-operated backend.")
                Text("Station, stream, content-delivery, and network providers can receive connection information such as the source IP address and may retain normal server or network logs under their own practices. The Player does not read or persist the source IP address itself.")
                Text("Permissions", style = MaterialTheme.typography.titleSmall)
                Text("Internet and network-state access support station connections. Foreground media playback and notification access support background audio and system controls. The app does not request contacts, location, microphone, camera, photos, phone, SMS, or broad file access.")
                TextButton(onClick = { uriHandler.openUri(ProjectIssuesUrl) }) {
                    Text("Open project privacy questions")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ThirdPartyNoticesDialog(onDismiss: () -> Unit) {
    val resources = LocalResources.current
    val notices = remember(resources) {
        resources.openRawResource(R.raw.third_party_notices)
            .bufferedReader()
            .use { it.readText() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open-source licenses") },
        text = {
            Text(
                text = notices,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
