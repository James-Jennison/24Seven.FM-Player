package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.MAX_REQUEST_MESSAGE_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus

@Composable
internal fun RequestConfirmationDialog(
    state: MainUiState,
    onCancelRequest: () -> Unit,
    onConfirmRequest: (String) -> Unit,
    onReviewTerms: () -> Unit,
) {
    val requests = state.requests ?: return
    val prepared = requests.pendingRequest ?: return
    val track = prepared.track
    val availability = requests.tracks.firstOrNull { it.songId == track.songId }?.availability
        ?: track.availability
    var requestMessage by remember(track.albumId, track.songId) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelRequest,
        title = { Text("Request this track?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    buildString {
                        append(track.title)
                        track.artist?.let { append(" — $it") }
                        append("\n\nStation: ")
                        append(state.selectedStation?.name ?: "Selected station")
                        append("\nSigned in as: ")
                        append(prepared.accountDisplayName)
                        append("\n\nThe station enforces queue, artist, album, eligibility, and cooldown rules. This sends one request and will not retry automatically.")
                    },
                )
                RequestStatusIndicator(availability)
                availability.detail?.let { detail ->
                    Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!state.communitySafety.canContributeCommunityContent) {
                    Text(
                        "Community access and the current Terms of Participation are required because requests publish requester attribution.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (
                        state.communitySafety.ageGateStatus == com.codeframe78.twentyfourseven.player.domain.AgeGateStatus.Adult &&
                        !state.communitySafety.hasAcceptedCurrentTerms
                    ) {
                        TextButton(onClick = onReviewTerms) { Text("Review terms") }
                    } else {
                        Text(
                            "Use Community safety in More to complete access or show community content.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.selectedStation?.capabilities?.supportsRequestMessages == true) {
                    OutlinedTextField(
                        value = requestMessage,
                        onValueChange = { requestMessage = it.take(MAX_REQUEST_MESSAGE_CHARACTERS) },
                        label = { Text("Message (optional)") },
                        supportingText = { Text("${requestMessage.length}/$MAX_REQUEST_MESSAGE_CHARACTERS") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRequest(requestMessage) },
                enabled = requests.status != SongRequestLoadStatus.Submitting &&
                    availability.canRequest && state.communitySafety.canContributeCommunityContent &&
                    state.selectedStation?.id == prepared.stationId &&
                    state.auth?.displayName?.trim() == prepared.accountDisplayName,
            ) { Text("Send request") }
        },
        dismissButton = { TextButton(onClick = onCancelRequest) { Text("Cancel") } },
    )
}
