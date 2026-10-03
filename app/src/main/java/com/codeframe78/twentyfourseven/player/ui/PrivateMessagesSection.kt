package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codeframe78.twentyfourseven.player.domain.AbuseReportKind
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSource
import com.codeframe78.twentyfourseven.player.domain.AbuseReportTarget
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.MAX_PRIVATE_MESSAGE_BODY_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.MAX_PRIVATE_MESSAGE_SUBJECT_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.PrivateMessage
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageCompose
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageComposeStatus
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageFolder
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageOpenStatus
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageSummary
import com.codeframe78.twentyfourseven.player.domain.PrivateMessagesState
import com.codeframe78.twentyfourseven.player.domain.PrivateMessagesStatus
import com.codeframe78.twentyfourseven.player.domain.Station

@Immutable
internal data class PrivateMessageActions(
    val onRefresh: (PrivateMessageFolder, Int) -> Unit = { _, _ -> },
    val onOpen: (String) -> Unit = {},
    val onClose: () -> Unit = {},
    val onReply: () -> Unit = {},
    val onNewMessage: (String) -> Unit = {},
    val onSend: (String, String) -> Unit = { _, _ -> },
    val onCancelCompose: () -> Unit = {},
)

/** Opens the private messages screen from anywhere in the app; null where the station has no private messages. */
internal val LocalMessagesOpener = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Private messages as a screen of their own, opened from the top bar or from More. */
@Composable
internal fun PrivateMessagesScreen(
    state: MainUiState,
    actions: PrivateMessageActions,
    communityActions: CommunitySafetyActions,
    onClose: () -> Unit,
) {
    val station = state.selectedStation ?: return
    val messages = state.privateMessages
    var askingRecipient by rememberSaveable { mutableStateOf(false) }
    val canCompose = station.capabilities.supportsPrivateMessageSending &&
        state.auth?.status == AuthStatus.SignedIn &&
        state.communitySafety.canViewCommunityContent &&
        messages != null
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("private_messages_screen")) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Private messages", style = MaterialTheme.typography.titleLarge)
                            Text(
                                station.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close private messages")
                        }
                    }
                    RefreshableBox(
                        onRefresh = { messages?.let { actions.onRefresh(it.folder, it.page) } },
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        isLoading = messages?.status == PrivateMessagesStatus.Loading,
                    ) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PrivateMessagesSection(state, actions, communityActions)
                            // Room for the last row to scroll clear of the floating button.
                            if (canCompose) Spacer(Modifier.height(80.dp))
                        }
                    }
                }
                if (canCompose) {
                    ExtendedFloatingActionButton(
                        onClick = { askingRecipient = true },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        text = { Text("New message") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("private_messages_new"),
                    )
                }
            }
        }
    }
    if (askingRecipient) {
        NewMessageRecipientDialog(
            station = station,
            onDismiss = { askingRecipient = false },
            onContinue = { recipient ->
                askingRecipient = false
                actions.onNewMessage(recipient)
            },
        )
    }
}

@Composable
private fun NewMessageRecipientDialog(station: Station, onDismiss: () -> Unit, onContinue: (String) -> Unit) {
    var recipient by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New message") },
        text = {
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it.take(MAX_RECIPIENT_CHARACTERS) },
                label = { Text("${station.shortName} member name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("private_message_recipient"),
            )
        },
        confirmButton = {
            Button(onClick = { onContinue(recipient.trim()) }, enabled = recipient.isNotBlank()) { Text("Continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** The summary line of the Private messages entry in More. */
internal fun privateMessagesSummary(messages: PrivateMessagesState?): String = when (val unread = messages?.unreadCount) {
    null -> "Read and reply to messages from station members."
    0 -> "No unread messages."
    1 -> "1 unread message."
    else -> "$unread unread messages."
}

@Composable
internal fun PrivateMessagesSection(
    state: MainUiState,
    actions: PrivateMessageActions,
    communityActions: CommunitySafetyActions,
) {
    val station = state.selectedStation ?: return
    val messages = state.privateMessages
    when {
        state.auth?.status != AuthStatus.SignedIn -> Text(
            "Sign in to ${station.shortName} from the More tab to read your private messages.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        !state.communitySafety.canViewCommunityContent || messages == null -> Text(
            "Private messages are community content. Complete Community safety in the More tab to read them.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> {
            LaunchedEffect(station.id) {
                if (messages.status == PrivateMessagesStatus.Idle) actions.onRefresh(PrivateMessageFolder.Inbox, 1)
            }
            PrivateMessagesFolder(station, messages, actions)
            PrivateMessageDialog(state, station, messages, actions, communityActions)
        }
    }
}

@Composable
private fun PrivateMessagesFolder(station: Station, messages: PrivateMessagesState, actions: PrivateMessageActions) {
    Column(Modifier.fillMaxWidth().testTag("private_messages_card"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrivateMessageFolder.entries.forEach { folder ->
                FilterChip(
                    selected = messages.folder == folder,
                    onClick = { actions.onRefresh(folder, 1) },
                    label = { Text(folder.label) },
                    modifier = Modifier.testTag("private_messages_folder_${folder.key}"),
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { actions.onRefresh(messages.folder, messages.page) },
                modifier = Modifier.testTag("private_messages_refresh"),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh private messages")
            }
        }
        when (messages.status) {
            PrivateMessagesStatus.Idle, PrivateMessagesStatus.Loading ->
                SkeletonList(rows = 5, contentPadding = PaddingValues(0.dp), description = "Loading messages")
            PrivateMessagesStatus.SignInRequired -> Text(
                "${station.shortName} asked for sign-in again. Sign in from the More tab to read your private messages.",
                color = MaterialTheme.colorScheme.error,
            )
            PrivateMessagesStatus.Error -> Text(
                "Your private messages could not be loaded right now.",
                color = MaterialTheme.colorScheme.error,
            )
            PrivateMessagesStatus.Ready -> if (messages.messages.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Mail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                    Text("No messages in this folder.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    messages.messages.forEachIndexed { index, summary ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        PrivateMessageRow(summary, messages.folder) { actions.onOpen(summary.id) }
                    }
                }
            }
        }
        if (messages.status == PrivateMessagesStatus.Ready && messages.pageCount > 1) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { actions.onRefresh(messages.folder, messages.page - 1) },
                    enabled = messages.page > 1,
                ) { Text("Newer") }
                Text(
                    "Page ${messages.page} of ${messages.pageCount}",
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = { actions.onRefresh(messages.folder, messages.page + 1) },
                    enabled = messages.page < messages.pageCount,
                ) { Text("Older") }
            }
        }
    }
}

/** One message the way a mail list shows it: who, when, and the subject, with unread ones in bold beside a dot. */
@Composable
private fun PrivateMessageRow(summary: PrivateMessageSummary, folder: PrivateMessageFolder, onOpen: () -> Unit) {
    val direction = if (folder == PrivateMessageFolder.Sent) "To" else "From"
    val weight = if (summary.isUnread) FontWeight.Bold else FontWeight.Normal
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .testTag("private_message_${summary.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(
                    "Unread".takeIf { summary.isUnread },
                    summary.subject,
                    "$direction ${summary.correspondent}",
                    summary.dateLabel,
                ).joinToString(", ")
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                summary.correspondent.trim().take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (folder == PrivateMessageFolder.Sent) "To ${summary.correspondent}" else summary.correspondent,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = weight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    summary.dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                summary.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = weight,
                color = if (summary.isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (summary.isUnread) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun PrivateMessageDialog(
    state: MainUiState,
    station: Station,
    messages: PrivateMessagesState,
    actions: PrivateMessageActions,
    communityActions: CommunitySafetyActions,
) {
    if (messages.openStatus == PrivateMessageOpenStatus.Closed || messages.compose != null) return
    val message = messages.openMessage
    AlertDialog(
        onDismissRequest = actions.onClose,
        modifier = Modifier.testTag("private_message_dialog"),
        title = { Text(message?.subject ?: "Private message", maxLines = 3, overflow = TextOverflow.Ellipsis) },
        text = {
            when {
                messages.openStatus == PrivateMessageOpenStatus.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("Loading message…")
                }
                message == null -> Text(
                    "This message could not be loaded right now.",
                    color = MaterialTheme.colorScheme.error,
                )
                else -> PrivateMessageBody(state, station, message, actions, communityActions)
            }
        },
        confirmButton = {
            if (message?.canReply == true && station.capabilities.supportsPrivateMessageSending) {
                Button(onClick = actions.onReply, modifier = Modifier.testTag("private_message_reply")) {
                    Text("Reply")
                }
            }
        },
        dismissButton = { TextButton(onClick = actions.onClose) { Text("Close") } },
    )
}

@Composable
private fun PrivateMessageBody(
    state: MainUiState,
    station: Station,
    message: PrivateMessage,
    actions: PrivateMessageActions,
    communityActions: CommunitySafetyActions,
) {
    Column(
        Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "From ${message.sender} to ${message.recipient}\n${message.dateLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SelectionContainer { Text(message.body, modifier = Modifier.testTag("private_message_body")) }
        val fromAnotherMember = !message.sender.equals(state.auth?.displayName, ignoreCase = true)
        val otherMember = if (message.folder == PrivateMessageFolder.Sent) message.recipient else message.sender
        LocalMemberProfileOpener.current
            ?.takeIf { !otherMember.equals(state.auth?.displayName, ignoreCase = true) }
            ?.let { openProfile ->
                TextButton(
                    onClick = { openProfile(otherMember) },
                    modifier = Modifier.testTag("private_message_profile"),
                ) { Text("View $otherMember's profile") }
            }
        if (fromAnotherMember && message.folder != PrivateMessageFolder.Sent) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        communityActions.onBeginReport(
                            AbuseReportTarget(
                                kind = AbuseReportKind.Content,
                                source = AbuseReportSource.PrivateMessage,
                                reportedUser = message.sender,
                                displayedTimestamp = message.dateLabel,
                                contentSnapshot = message.body.take(MAX_REPORT_SNAPSHOT_CHARACTERS),
                            ),
                        )
                        actions.onClose()
                    },
                ) { Text("Report") }
                TextButton(
                    onClick = {
                        communityActions.onBlockUser(station.id, message.sender)
                        actions.onClose()
                    },
                ) { Text("Block ${message.sender}") }
            }
        }
    }
}

/** Shown from the app root, so a message can also be started from a member's profile card on any screen. */
@Composable
internal fun PrivateMessageComposeDialog(compose: PrivateMessageCompose?, actions: PrivateMessageActions) {
    if (compose == null) return
    // The draft lives only in memory: it is not saved across process death or written anywhere.
    var subject by remember(compose.recipient, compose.isReply) { mutableStateOf(compose.subject) }
    var body by remember(compose.recipient, compose.isReply) { mutableStateOf(compose.body) }
    var reviewing by remember(compose.recipient, compose.isReply) { mutableStateOf(false) }
    LaunchedEffect(compose.status) {
        if (compose.status == PrivateMessageComposeStatus.Ready && subject.isEmpty() && body.isEmpty()) {
            subject = compose.subject
            body = compose.body
        }
    }
    val sending = compose.status == PrivateMessageComposeStatus.Sending
    val editable = compose.status == PrivateMessageComposeStatus.Ready
    val canSend = subject.isNotBlank() && body.isNotBlank()
    AlertDialog(
        onDismissRequest = { if (!sending) actions.onCancelCompose() },
        modifier = Modifier.testTag("private_message_compose"),
        title = { Text(if (compose.isReply) "Reply to ${compose.recipient}" else "Message to ${compose.recipient}") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    compose.status == PrivateMessageComposeStatus.Preparing || sending -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Text(if (sending) "Sending…" else "Preparing…")
                    }
                    !editable -> Text(
                        privateMessageComposeResult(compose),
                        color = if (compose.status == PrivateMessageComposeStatus.Sent) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    reviewing -> {
                        Text("Review before sending. The message is sent once and cannot be recalled.")
                        Text(subject, fontWeight = FontWeight.SemiBold)
                        Text(body)
                    }
                    else -> {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it.take(MAX_PRIVATE_MESSAGE_SUBJECT_CHARACTERS) },
                            label = { Text("Subject") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("private_message_subject"),
                        )
                        OutlinedTextField(
                            value = body,
                            onValueChange = { body = it.take(MAX_PRIVATE_MESSAGE_BODY_CHARACTERS) },
                            label = { Text("Message") },
                            supportingText = { Text("${body.length}/$MAX_PRIVATE_MESSAGE_BODY_CHARACTERS") },
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth().testTag("private_message_text"),
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                editable && reviewing -> Button(
                    onClick = { actions.onSend(subject.trim(), body.trim()) },
                    enabled = canSend,
                    modifier = Modifier.testTag("private_message_send"),
                ) { Text("Send") }
                editable -> Button(
                    onClick = { reviewing = true },
                    enabled = canSend,
                    modifier = Modifier.testTag("private_message_review"),
                ) { Text("Review") }
                else -> TextButton(onClick = actions.onCancelCompose, enabled = !sending) { Text("Close") }
            }
        },
        dismissButton = {
            when {
                editable && reviewing -> TextButton(onClick = { reviewing = false }) { Text("Edit") }
                editable -> TextButton(onClick = actions.onCancelCompose) { Text("Discard") }
            }
        },
    )
}

internal fun privateMessageComposeResult(compose: PrivateMessageCompose): String = when (compose.status) {
    PrivateMessageComposeStatus.Sent -> "Your message to ${compose.recipient} was sent."
    PrivateMessageComposeStatus.Unconfirmed ->
        "The station did not confirm this message. Check your Sent folder before writing it again."
    PrivateMessageComposeStatus.RecipientNotFound ->
        "No member named ${compose.recipient} could be found. Check the spelling and try again."
    PrivateMessageComposeStatus.Failed -> "The message could not be prepared or sent. Nothing was delivered."
    PrivateMessageComposeStatus.Preparing,
    PrivateMessageComposeStatus.Ready,
    PrivateMessageComposeStatus.Sending,
    -> ""
}

private const val MAX_RECIPIENT_CHARACTERS = 60
private const val MAX_REPORT_SNAPSHOT_CHARACTERS = 500
