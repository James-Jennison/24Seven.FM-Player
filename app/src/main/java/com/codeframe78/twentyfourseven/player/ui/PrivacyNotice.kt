package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.R

@Composable
internal fun PrivacySection() {
    var showNotice by remember { mutableStateOf(false) }
    var showThirdPartyNotices by remember { mutableStateOf(false) }
    MoreDisclosure(
        title = "Privacy",
        summary = "No ads, analytics, or tracking.",
        icon = Icons.Default.Lock,
        testTag = "more_privacy",
    ) {
        Text(
            "No ads or developer-operated analytics, tracking, or data server. Google Cast sends anonymous encrypted usage diagnostics to Google.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy notice") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Data handled by the Player", style = MaterialTheme.typography.titleSmall)
                Text("The app connects directly to the selected 24Seven.FM station for live audio, artwork, public now-playing details, while playing and while the app is open and not yet playing (including who requested the track and the listener count), Queue/History and the played-history archive, station news, catalog, and Chat data. Choosing View profile on a member's name loads that member's public station profile card, which stays in memory only. When you are signed in, tapping the Public Favorites badge on that card loads the favorites list that member has made public, also in memory only.")
                Text("Credentials are sent only when you explicitly sign in. Passwords and security-code answers are not saved. Successful station sessions and display identity are encrypted locally with Android Keystore and are removed by Sign out, clearing app data, or uninstalling.")
                Text("The Player signs in to pre-existing station accounts; it does not create or delete them. Sign out removes only this device's protected session. It does not delete the station account, public posts, request history, station/server logs, or sent email. Station-side access, correction, retention, and deletion are controlled by the applicable station or network operator.")
                Text("Where verified, recent request summaries, station-reported request readiness, membership indicators, and favorite-track lists are loaded for the signed-in station account. They remain in memory and are cleared from the interface on Sign out.")
                Text("The app stores only the adult/not-adult age-screen result, accepted Terms version, mature-community-content visibility choice, station-scoped blocked identities, and which stations you enable for Chat-mention notifications. It does not save the entered date of birth.")
                Text("All app-private data is excluded from Android cloud backup and device-to-device transfer.")
                Text("When you explicitly choose Review email for an abuse report, the app prepares a bounded draft with the selected station, report category, reported user, content snapshot, your entered name or station nickname, and optional details. Android opens your chosen email app with the monitored moderation recipient, subject, and body prefilled. You may edit, cancel, or send it there. The Player cannot read your email account or confirm sending or delivery, and it does not save the draft or report. The email app and recipient may retain a sent report under their own practices.")
                Text("When you explicitly choose Review email draft for Report a problem, the app prepares a local draft with the selected category, selected station, preparation time, and optional description. Privacy-safe diagnostics are included only when you select the separate checkbox; they contain app/build and Android details, coarse device model, selected station, playback state, and recent non-sensitive transitions. Android opens your email app with the monitored Player contact. You may edit, cancel, or send it there. The Player cannot read your email account or confirm sending or delivery, and it does not save the draft or feedback.")
                Text("The current Player does not link to VIP/RIP purchase or activation, account registration, recovery, management, or deletion pages. A member's profile card can show two links from the station's own card: Email, which opens the station's email-a-member page, and the member's listed website. Each opens in your browser only after you tap it; the Player sends nothing to either and does not share its station session with the browser. Contact Us opens only a reviewed draft in your email app and does not copy the protected station session.")
                Text("Where verified, a signed-in member can read their station private messages and reply to or start one. Message lists and opened messages are fetched on request, and the unread count shown on the More tab is checked when you sign in, return to the app, or change tabs (at most once every two minutes). They stay in memory and are cleared on Sign out; opening a message marks it read on the station. A message is sent once, only after you review it and choose Send, and the draft is not saved. The station stores private messages with both members' accounts.")
                Text("Where verified, a signed-in member can add the current track to their station favorites or rate the current album. Each is sent only after an explicit tap, and the station stores it with the member's account. From the Favorites tab, a signed-in member can view My ranking, the order shown on their own station profile, read 50 at a time from the station's profile favorites feed; move a track up or down in it; or remove a track (after confirming). Each change is one request, and the ranking is then read again from the station.")
                Text("Opening an album also loads the member reviews the station shows for it. The Player does not write reviews; they are written on the station's own site.")
                Text("On request, the Player loads the station's public Recently Added albums, the Online Now block and members list, and the events and birthdays calendar, as the station publishes them. Member names and birthdays are shown only while community content is visible. A member search sends only the typed name part to the station. These pages stay in memory only.")
                Text("Where verified, a signed-in member can open Edit profile to change their own real name, location, country, occupation, interests, website, signature, about text, newsletter choice, and hidden online status. The Player reads the station's whole form with your session, sends it back once to the form's own address when you choose Save, always with empty password fields, and then reads it again to show what the station kept. Email, forum, and chat settings are returned unchanged and cannot be edited in the Player.")
                Text("Chat posts, song requests, optional request messages, private messages, album ratings, favorites changes, and profile edits are sent only after explicit actions. Chat history and pending request text are transient. If enabled, exact-name Chat mentions are matched on-device; notification deduplication retains only bounded message fingerprints in memory, and notification text contains the station and sender but not the Chat message. An optional user-started foreground monitor can poll one signed-in station about once a minute while a persistent notification and Stop action remain visible.")
                Text("The app contains no ads, crash reporting, tracking SDK, developer-operated analytics, or developer-operated backend. The Google Cast SDK automatically sends Google encrypted, anonymous interaction diagnostics, including generic discovery and session events plus mobile-device and client-app information. Google says these logs are not linked to an identifiable user, are retained briefly before aggregation, are used to improve Cast, and cannot be disabled or deleted by the app or user.")
                Text("Station, stream, content-delivery, and network providers can receive connection information such as the source IP address and may retain normal server or network logs under their own practices. The Player does not read or persist the source IP address itself.")
                Text("Permissions", style = MaterialTheme.typography.titleSmall)
                Text("Internet and network-state access support station connections. Foreground media playback and notification access support background audio and system controls. A special-use foreground service supports only the optional, visibly disclosed Chat-mention monitor. The app does not request contacts, location, microphone, camera, photos, phone, SMS, or broad file access.")
                Text("For privacy questions, close this notice and use Contact Us in More. The Player will open a reviewed email draft and you control whether it is sent.")
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
