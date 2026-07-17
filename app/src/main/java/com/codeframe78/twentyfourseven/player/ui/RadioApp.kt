package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.AbuseReportCategory
import com.codeframe78.twentyfourseven.player.domain.AbuseReportKind
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSource
import com.codeframe78.twentyfourseven.player.domain.AbuseReportStatus
import com.codeframe78.twentyfourseven.player.domain.AbuseReportSubmission
import com.codeframe78.twentyfourseven.player.domain.AbuseReportTarget
import com.codeframe78.twentyfourseven.player.domain.AgeGateStatus
import com.codeframe78.twentyfourseven.player.domain.ChatLoadStatus
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.ChatMessagePart
import com.codeframe78.twentyfourseven.player.domain.HistoryTrack
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationPage
import com.codeframe78.twentyfourseven.player.domain.RequestSearchField
import com.codeframe78.twentyfourseven.player.domain.RequestSearchTarget
import com.codeframe78.twentyfourseven.player.domain.RequestSuggestionMode
import com.codeframe78.twentyfourseven.player.domain.RequestReadiness
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus
import com.codeframe78.twentyfourseven.player.domain.StartupStationMode
import com.codeframe78.twentyfourseven.player.domain.Station
import coil3.compose.AsyncImage
import com.codeframe78.twentyfourseven.player.R
import com.codeframe78.twentyfourseven.player.ui.theme.stationPalette
import com.codeframe78.twentyfourseven.player.ui.theme.StationPalette

private val navigationItems = listOf(
    NavigationItem(MainDestination.Player, "Player", Icons.Default.Radio),
    NavigationItem(MainDestination.Favorites, "Favorites", Icons.Default.Favorite),
    NavigationItem(MainDestination.Chat, "Chat", Icons.AutoMirrored.Filled.Chat),
    NavigationItem(MainDestination.Queue, "Queue", Icons.AutoMirrored.Filled.QueueMusic),
    NavigationItem(MainDestination.More, "More", Icons.Default.MoreHoriz),
)

private data class NavigationItem(
    val destination: MainDestination,
    val label: String,
    val icon: ImageVector,
)

internal data class CommunitySafetyActions(
    val onSubmitAgeScreen: (Int, Int, Int) -> Unit = { _, _, _ -> },
    val onAcceptTerms: () -> Unit = {},
    val onSetCommunityContentVisible: (Boolean) -> Unit = {},
    val onBlockUser: (StationId, String) -> Unit = { _, _ -> },
    val onUnblockUser: (StationId, String) -> Unit = { _, _ -> },
    val onBeginReport: (AbuseReportTarget) -> Unit = {},
    val onRetryReport: () -> Unit = {},
    val onSubmitReport: (AbuseReportSubmission) -> Unit = {},
    val onDismissReport: () -> Unit = {},
)

@Composable
internal fun RadioApp(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
    onSelectDestination: (MainDestination) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onRefreshQueue: () -> Unit,
    onRefreshFavorites: () -> Unit = {},
    onRefreshListenerActivity: () -> Unit = {},
    onRefreshChat: () -> Unit = {},
    onSendChatMessage: (String) -> Unit = {},
    onRefreshAuth: (StationId) -> Unit = {},
    onSignIn: (StationId, String, String, String) -> Unit = { _, _, _, _ -> },
    onSignOut: (StationId) -> Unit = {},
    onSearchRequests: (String, RequestSearchField) -> Unit = { _, _ -> },
    onSuggestRequest: (RequestSuggestionMode) -> Unit = {},
    onOpenRequestAlbum: (RequestSearchTarget) -> Unit = {},
    onPrepareRequest: (String) -> Unit = {},
    onPrepareFavoriteRequest: (FavoriteTrack) -> Unit = {},
    onCancelRequest: () -> Unit = {},
    onConfirmRequest: (String) -> Unit = {},
    onUseLastStationAtStartup: () -> Unit = {},
    onSetStartupStation: (StationId) -> Unit = {},
    onOpenStationPage: (StationPage) -> Unit = {},
    communitySafetyActions: CommunitySafetyActions = CommunitySafetyActions(),
    sleepTimerActions: SleepTimerActions = SleepTimerActions(),
    audioOutputActions: AudioOutputActions = AudioOutputActions(),
    diagnosticUi: DiagnosticUi = DiagnosticUi(),
) {
    var showTerms by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            TabletShell(state, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onConfirmRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions) { showTerms = true }
        } else {
            PhoneShell(state, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onConfirmRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions) { showTerms = true }
        }
    }
    if (showTerms) {
        CommunityTermsDialog(
            onAgree = {
                communitySafetyActions.onAcceptTerms()
                showTerms = false
            },
            onDecline = { showTerms = false },
        )
    }
    AbuseReportDialog(state, communitySafetyActions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneShell(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
    onSelectDestination: (MainDestination) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    diagnosticUi: DiagnosticUi,
    onRefreshQueue: () -> Unit,
    onRefreshFavorites: () -> Unit,
    onRefreshListenerActivity: () -> Unit,
    onRefreshChat: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onRefreshAuth: (StationId) -> Unit,
    onSignIn: (StationId, String, String, String) -> Unit,
    onSignOut: (StationId) -> Unit,
    onSearchRequests: (String, RequestSearchField) -> Unit,
    onSuggestRequest: (RequestSuggestionMode) -> Unit,
    onOpenRequestAlbum: (RequestSearchTarget) -> Unit,
    onPrepareRequest: (String) -> Unit,
    onPrepareFavoriteRequest: (FavoriteTrack) -> Unit,
    onCancelRequest: () -> Unit,
    onConfirmRequest: (String) -> Unit,
    onUseLastStationAtStartup: () -> Unit,
    onSetStartupStation: (StationId) -> Unit,
    onOpenStationPage: (StationPage) -> Unit,
    communitySafetyActions: CommunitySafetyActions,
    onReviewTerms: () -> Unit,
) {
    val showNavigationLabels = LocalDensity.current.fontScale <= 1.5f
    Scaffold(
        topBar = { StationTopBar(state, onSelectDestination) },
        bottomBar = {
            Column {
                if (state.destination != MainDestination.Player) {
                    PersistentMiniPlayer(state, onSelectDestination, onPlay, onPause)
                }
                NavigationBar(Modifier.testTag("phone_navigation_bar")) {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = state.destination == item.destination,
                            onClick = { onSelectDestination(item.destination) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = if (showNavigationLabels) {
                                {
                                    Text(
                                        item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            } else {
                                null
                            },
                            modifier = Modifier.semantics { contentDescription = item.label },
                        )
                    }
                }
            }
        },
    ) { padding ->
        DestinationContent(state, padding, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onConfirmRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, onReviewTerms)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletShell(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
    onSelectDestination: (MainDestination) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    diagnosticUi: DiagnosticUi,
    onRefreshQueue: () -> Unit,
    onRefreshFavorites: () -> Unit,
    onRefreshListenerActivity: () -> Unit,
    onRefreshChat: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onRefreshAuth: (StationId) -> Unit,
    onSignIn: (StationId, String, String, String) -> Unit,
    onSignOut: (StationId) -> Unit,
    onSearchRequests: (String, RequestSearchField) -> Unit,
    onSuggestRequest: (RequestSuggestionMode) -> Unit,
    onOpenRequestAlbum: (RequestSearchTarget) -> Unit,
    onPrepareRequest: (String) -> Unit,
    onPrepareFavoriteRequest: (FavoriteTrack) -> Unit,
    onCancelRequest: () -> Unit,
    onConfirmRequest: (String) -> Unit,
    onUseLastStationAtStartup: () -> Unit,
    onSetStartupStation: (StationId) -> Unit,
    onOpenStationPage: (StationPage) -> Unit,
    communitySafetyActions: CommunitySafetyActions,
    onReviewTerms: () -> Unit,
) {
    val showNavigationLabels = LocalDensity.current.fontScale <= 1.5f
    Row(Modifier.fillMaxSize()) {
        NavigationRail(Modifier.fillMaxHeight().testTag("tablet_navigation_rail")) {
            Spacer(Modifier.height(12.dp))
            navigationItems.forEach { item ->
                NavigationRailItem(
                    selected = state.destination == item.destination,
                    onClick = { onSelectDestination(item.destination) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = if (showNavigationLabels) {
                        {
                            Text(
                                item.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.semantics { contentDescription = item.label },
                )
            }
        }
        VerticalDivider(Modifier.fillMaxHeight())
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = { StationTopBar(state, onSelectDestination) },
            bottomBar = {
                if (state.destination != MainDestination.Player) {
                    PersistentMiniPlayer(state, onSelectDestination, onPlay, onPause)
                }
            },
        ) { padding ->
            DestinationContent(state, padding, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onConfirmRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, onReviewTerms)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationTopBar(
    state: MainUiState,
    onSelectDestination: (MainDestination) -> Unit,
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "24Seven.FM logo",
                modifier = Modifier.padding(start = 12.dp).size(40.dp).clip(RoundedCornerShape(10.dp)),
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("24Seven.FM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { onSelectDestination(MainDestination.Player) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(
                        state.selectedStation?.name ?: "Choose station",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { onSelectDestination(MainDestination.More) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Account and station options")
            }
        },
    )
}

@Composable
private fun DestinationContent(
    state: MainUiState,
    padding: PaddingValues,
    onSelectStation: (StationId) -> Unit,
    onSelectDestination: (MainDestination) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    diagnosticUi: DiagnosticUi,
    onRefreshQueue: () -> Unit,
    onRefreshFavorites: () -> Unit,
    onRefreshListenerActivity: () -> Unit,
    onRefreshChat: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onRefreshAuth: (StationId) -> Unit,
    onSignIn: (StationId, String, String, String) -> Unit,
    onSignOut: (StationId) -> Unit,
    onSearchRequests: (String, RequestSearchField) -> Unit,
    onSuggestRequest: (RequestSuggestionMode) -> Unit,
    onOpenRequestAlbum: (RequestSearchTarget) -> Unit,
    onPrepareRequest: (String) -> Unit,
    onPrepareFavoriteRequest: (FavoriteTrack) -> Unit,
    onCancelRequest: () -> Unit,
    onConfirmRequest: (String) -> Unit,
    onUseLastStationAtStartup: () -> Unit,
    onSetStartupStation: (StationId) -> Unit,
    onOpenStationPage: (StationPage) -> Unit,
    communitySafetyActions: CommunitySafetyActions,
    onReviewTerms: () -> Unit,
) {
    when (state.destination) {
        MainDestination.Player -> AdaptivePlayerScreen(
            state,
            padding,
            onSelectStation,
            onPlay,
            onPause,
            onStop,
            sleepTimerActions,
            audioOutputActions,
        )
        MainDestination.Favorites -> FavoriteTracksScreen(
            state = state,
            padding = padding,
            onRefresh = onRefreshFavorites,
            onPrepareRequest = onPrepareFavoriteRequest,
            onCancelRequest = onCancelRequest,
            onConfirmRequest = onConfirmRequest,
            onOpenAccount = { onSelectDestination(MainDestination.More) },
            onReviewTerms = onReviewTerms,
        )
        MainDestination.Chat -> ChatScreen(state, padding, onRefreshChat, onSendChatMessage, communitySafetyActions, onReviewTerms)
        MainDestination.Queue -> QueueScreen(state, padding, onRefreshQueue, communitySafetyActions)
        MainDestination.More -> MoreScreen(state, padding, onRefreshAuth, onSignIn, onSignOut, onRefreshListenerActivity, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onCancelRequest, onConfirmRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, diagnosticUi, onReviewTerms)
    }
}

@Composable
private fun ChatScreen(
    state: MainUiState,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onSendMessage: (String) -> Unit,
    communitySafetyActions: CommunitySafetyActions,
    onReviewTerms: () -> Unit,
) {
    val chat = state.chat
    when {
        state.selectedStation?.capabilities?.supportsChat != true -> FeatureScreen(
                title = "Chat",
                description = "No supported chat connection has been verified for this station yet.",
                icon = Icons.AutoMirrored.Filled.Chat,
                padding = padding,
            )
        !state.communitySafety.canViewCommunityContent -> CommunityAccessGate(
            state = state,
            padding = padding,
            actions = communitySafetyActions,
            onReviewTerms = onReviewTerms,
        )
        chat == null || chat.status == ChatLoadStatus.Unavailable -> FeatureScreen(
            title = "Chat",
            description = "The station chat is not available yet.",
            icon = Icons.AutoMirrored.Filled.Chat,
            padding = padding,
        )
        chat.status == ChatLoadStatus.Loading -> Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        chat.status == ChatLoadStatus.Error -> Column(
            Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Chat unavailable", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                chat.errorMessage ?: "The station chat could not be refreshed.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Try again")
            }
        }
        else -> ChatMessages(state, padding, onRefresh, onSendMessage, communitySafetyActions)
    }
}

@Composable
private fun ChatMessages(
    state: MainUiState,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onSendMessage: (String) -> Unit,
    communitySafetyActions: CommunitySafetyActions,
) {
    val chat = checkNotNull(state.chat)
    var draft by remember(state.selectedStation?.id) { mutableStateOf("") }
    var awaitingSend by remember(state.selectedStation?.id) { mutableStateOf(false) }
    LaunchedEffect(chat.isSending, chat.sendErrorMessage, chat.messages) {
        if (awaitingSend && !chat.isSending) {
            if (chat.sendErrorMessage != null) {
                awaitingSend = false
            } else if (chat.messages.any { it.messageText == draft }) {
                draft = ""
                awaitingSend = false
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Station chat", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh chat")
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (chat.messages.isEmpty()) {
                item { EmptyTrackList("No recent chat messages are available.") }
            } else {
                itemsIndexed(
                    chat.messages,
                    key = { index, message ->
                        "$index-${message.postedAtLabel}-${message.authorDisplayName}-${message.messageText}"
                    },
                ) { _, message ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    message.authorDisplayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                message.postedAtLabel?.let { timestamp ->
                                    Text(
                                        timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                CommunityMessageActions(
                                    author = message.authorDisplayName,
                                    onReportContent = {
                                        communitySafetyActions.onBeginReport(
                                            AbuseReportTarget(
                                                kind = AbuseReportKind.Content,
                                                source = AbuseReportSource.Chat,
                                                reportedUser = message.authorDisplayName,
                                                displayedTimestamp = message.postedAtLabel,
                                                contentSnapshot = message.messageText,
                                            ),
                                        )
                                    },
                                    onReportUser = {
                                        communitySafetyActions.onBeginReport(
                                            AbuseReportTarget(
                                                kind = AbuseReportKind.User,
                                                source = AbuseReportSource.Chat,
                                                reportedUser = message.authorDisplayName,
                                                displayedTimestamp = message.postedAtLabel,
                                            ),
                                        )
                                    },
                                    onBlockUser = {
                                        state.selectedStation?.id?.let { stationId ->
                                            communitySafetyActions.onBlockUser(stationId, message.authorDisplayName)
                                        }
                                    },
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            ChatMessageText(message)
                        }
                    }
                }
            }
        }
        if (state.auth?.status == AuthStatus.SignedIn) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                chat.sendErrorMessage?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 255) draft = it },
                        label = { Text("Message") },
                        supportingText = { Text("${draft.length}/255") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !chat.isSending,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            awaitingSend = true
                            onSendMessage(draft)
                        },
                        enabled = draft.isNotBlank() && !chat.isSending,
                    ) {
                        Text(if (chat.isSending) "Sending" else "Send")
                    }
                }
            }
        } else {
            Text(
                "Sign in from More to send messages.",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChatMessageText(message: ChatMessage) {
    val inlineContent = remember(message.parts) {
        message.parts.mapIndexedNotNull { index, part ->
            if (part !is ChatMessagePart.Emoticon) return@mapIndexedNotNull null
            val id = "chat-emoticon-$index"
            id to InlineTextContent(
                placeholder = Placeholder(1.15.em, 1.15.em, PlaceholderVerticalAlign.TextCenter),
            ) {
                AsyncImage(
                    model = part.imageUrl,
                    contentDescription = "${part.altText} emoticon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().testTag(id),
                )
            }
        }.toMap()
    }
    val annotated = remember(message.parts) {
        buildAnnotatedString {
            message.parts.forEachIndexed { index, part ->
                when (part) {
                    is ChatMessagePart.Text -> append(part.value)
                    is ChatMessagePart.Emoticon -> appendInlineContent("chat-emoticon-$index", part.altText)
                }
            }
        }
    }
    Text(annotated, inlineContent = inlineContent)
}

@Composable
private fun CommunityMessageActions(
    author: String,
    onReportContent: (() -> Unit)?,
    onReportUser: () -> Unit,
    onBlockUser: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Safety actions for $author")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            onReportContent?.let { reportContent ->
                DropdownMenuItem(
                    text = { Text("Report content") },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    onClick = {
                        expanded = false
                        reportContent()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Report user") },
                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                onClick = {
                    expanded = false
                    onReportUser()
                },
            )
            DropdownMenuItem(
                text = { Text("Block user") },
                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                onClick = {
                    expanded = false
                    onBlockUser()
                },
            )
        }
    }
}

@Composable
private fun CommunityAccessGate(
    state: MainUiState,
    padding: PaddingValues,
    actions: CommunitySafetyActions,
    onReviewTerms: () -> Unit,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(
        state.communitySafety.ageGateStatus,
        state.communitySafety.acceptedTermsVersion,
    ) {
        scrollState.scrollTo(0)
    }
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        when (state.communitySafety.ageGateStatus) {
            AgeGateStatus.NotCompleted -> {
                Text("Date of birth", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Community content is hidden. Enter your date of birth to determine access; the date itself is not saved.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                AgeScreenFields(state, actions.onSubmitAgeScreen)
            }
            AgeGateStatus.Underage -> {
                Text("Community features unavailable", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "The network's community features are restricted to adults. Playback and non-community station information remain available.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AgeGateStatus.Adult -> if (!state.communitySafety.hasAcceptedCurrentTerms) {
                Text("Terms required", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Review and accept the Terms of Participation before viewing or contributing community content.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onReviewTerms, modifier = Modifier.testTag("review_community_terms")) {
                    Text("Review terms")
                }
            } else {
                Text("Mature community content is hidden", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Chat and public request attribution may contain mature themes or explicit language. Choose separately whether to show it.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { actions.onSetCommunityContentVisible(true) },
                    modifier = Modifier.testTag("show_community_content"),
                ) { Text("Show community content") }
            }
        }
    }
}

@Composable
private fun AgeScreenFields(
    state: MainUiState,
    onSubmit: (Int, Int, Int) -> Unit,
) {
    var month by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = month,
            onValueChange = { month = it.filter(Char::isDigit).take(2) },
            label = { Text("Month") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("age_month"),
        )
        OutlinedTextField(
            value = day,
            onValueChange = { day = it.filter(Char::isDigit).take(2) },
            label = { Text("Day") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("age_day"),
        )
        OutlinedTextField(
            value = year,
            onValueChange = { year = it.filter(Char::isDigit).take(4) },
            label = { Text("Year") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1.3f).testTag("age_year"),
        )
    }
    state.communitySafety.ageGateErrorMessage?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            onSubmit(year.toIntOrNull() ?: 0, month.toIntOrNull() ?: 0, day.toIntOrNull() ?: 0)
        },
        enabled = year.length == 4 && month.isNotBlank() && day.isNotBlank(),
        modifier = Modifier.testTag("submit_age_screen"),
    ) { Text("Continue") }
}

@Composable
private fun CommunityTermsDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    val resources = LocalResources.current
    val terms = remember(resources) {
        resources.openRawResource(R.raw.terms_of_participation)
            .bufferedReader()
            .use { it.readText() }
    }
    var agreed by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Terms of Participation") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(terms, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = { agreed = it },
                        modifier = Modifier.testTag("agree_community_terms"),
                    )
                    Text("I Agree")
                }
            }
        },
        confirmButton = {
            Button(onClick = onAgree, enabled = agreed, modifier = Modifier.testTag("accept_community_terms")) {
                Text("I Agree")
            }
        },
        dismissButton = { TextButton(onClick = onDecline) { Text("I Decline") } },
    )
}

@Composable
private fun AbuseReportDialog(state: MainUiState, actions: CommunitySafetyActions) {
    val report = state.abuseReport
    if (report.status == AbuseReportStatus.Idle) return
    val target = report.target ?: return
    var reporterName by remember(target) { mutableStateOf(state.auth?.displayName.orEmpty()) }
    var reporterEmail by remember(target) { mutableStateOf("") }
    var category by remember(target) { mutableStateOf(AbuseReportCategory.Harassment) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var details by remember(target) { mutableStateOf("") }
    var securityCode by remember(target, report.captchaImageUrl) { mutableStateOf("") }
    val canDismiss = report.status != AbuseReportStatus.Submitting

    AlertDialog(
        onDismissRequest = { if (canDismiss) actions.onDismissReport() },
        title = { Text(target.kind.label) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Reported user: ${target.reportedUser}", fontWeight = FontWeight.SemiBold)
                target.contentSnapshot?.let {
                    Text("Content: “$it”", maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
                when (report.status) {
                    AbuseReportStatus.LoadingForm -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    AbuseReportStatus.Ready, AbuseReportStatus.Submitting -> {
                        Text(
                            "This sends a bounded report to the selected station's authorized administrators. Your contact information is required for the Contact Us form and is not saved by the app.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            reporterName,
                            { reporterName = it.take(100) },
                            label = { Text("Your name or station nickname") },
                            singleLine = true,
                            enabled = report.status == AbuseReportStatus.Ready,
                            modifier = Modifier.fillMaxWidth().testTag("reporter_name"),
                        )
                        OutlinedTextField(
                            reporterEmail,
                            { reporterEmail = it.take(254) },
                            label = { Text("Your email") },
                            singleLine = true,
                            enabled = report.status == AbuseReportStatus.Ready,
                            modifier = Modifier.fillMaxWidth().testTag("reporter_email"),
                        )
                        Box {
                            TextButton(onClick = { categoryMenuOpen = true }) {
                                Text("Category: ${category.label}")
                            }
                            DropdownMenu(
                                expanded = categoryMenuOpen,
                                onDismissRequest = { categoryMenuOpen = false },
                            ) {
                                AbuseReportCategory.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            category = option
                                            categoryMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            details,
                            { details = it.take(500) },
                            label = { Text("Optional details") },
                            supportingText = { Text("${details.length}/500") },
                            enabled = report.status == AbuseReportStatus.Ready,
                            modifier = Modifier.fillMaxWidth().testTag("report_details"),
                        )
                        AsyncImage(
                            model = report.captchaImageUrl,
                            contentDescription = "Report security code image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(96.dp).background(Color.White).padding(12.dp),
                        )
                        OutlinedTextField(
                            securityCode,
                            { securityCode = it.filter(Char::isLetterOrDigit).take(3) },
                            label = { Text("Three-character security code") },
                            supportingText = { Text("Case-sensitive") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                            enabled = report.status == AbuseReportStatus.Ready,
                            modifier = Modifier.fillMaxWidth().testTag("report_security_code"),
                        )
                        report.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (report.status == AbuseReportStatus.Submitting) {
                            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                    AbuseReportStatus.Submitted -> Text(
                        "Report sent to the selected station's administrators.",
                        modifier = Modifier.testTag("report_submitted"),
                    )
                    AbuseReportStatus.Error -> Text(
                        report.errorMessage ?: "The report could not be completed.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    AbuseReportStatus.Idle -> Unit
                }
            }
        },
        confirmButton = {
            when (report.status) {
                AbuseReportStatus.Ready -> Button(
                    onClick = {
                        actions.onSubmitReport(
                            AbuseReportSubmission(
                                reporterName = reporterName,
                                reporterEmail = reporterEmail,
                                category = category,
                                optionalDetails = details,
                                securityCode = securityCode,
                            ),
                        )
                    },
                    enabled = reporterName.isNotBlank() && reporterEmail.isNotBlank() && securityCode.length == 3,
                    modifier = Modifier.testTag("submit_abuse_report"),
                ) { Text("Send report") }
                AbuseReportStatus.Error -> if (report.retryAllowed) {
                    Button(onClick = actions.onRetryReport) { Text("Try again") }
                } else {
                    Button(onClick = actions.onDismissReport) { Text("Done") }
                }
                AbuseReportStatus.Submitted -> Button(onClick = actions.onDismissReport) { Text("Done") }
                else -> Unit
            }
        },
        dismissButton = {
            if (canDismiss && report.status != AbuseReportStatus.Submitted) {
                TextButton(onClick = actions.onDismissReport) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun QueueScreen(
    state: MainUiState,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    communitySafetyActions: CommunitySafetyActions,
) {
    val queue = state.queue
    val supportsQueueOrHistory = state.selectedStation?.capabilities?.run {
        supportsQueue || supportsHistory
    } == true
    when {
        !supportsQueueOrHistory ||
            queue == null || queue.status == QueueLoadStatus.Unavailable -> FeatureScreen(
                title = "Queue",
                description = "No supported queue or history source has been verified for this station yet.",
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                padding = padding,
            )
        queue.status == QueueLoadStatus.Loading -> Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        queue.status == QueueLoadStatus.Error -> Column(
            Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Queue unavailable", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                queue.errorMessage ?: "The station data could not be refreshed.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Try again")
            }
        }
        else -> QueueLists(
            queue.upcoming,
            queue.recentlyPlayed,
            queue.isStale,
            queue.errorMessage,
            padding,
            onRefresh,
            state.selectedStation?.id,
            communitySafetyActions,
        )
    }
}

@Composable
private fun QueueLists(
    upcoming: List<QueueTrack>,
    history: List<HistoryTrack>,
    isStale: Boolean,
    refreshMessage: String?,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    stationId: StationId?,
    communitySafetyActions: CommunitySafetyActions,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Up next", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh queue")
                }
            }
        }
        if (isStale) {
            item {
                Text(
                    refreshMessage ?: "Showing cached Queue data while a fresh copy is unavailable.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (upcoming.isEmpty()) {
            item { EmptyTrackList("The station queue is currently empty.") }
        } else {
            items(upcoming, key = { "queue-${it.position}-${it.displayTitle}" }) { track ->
                TrackCard(
                    track.position.toString(),
                    track.displayTitle,
                    track.artistName,
                    track.albumTitle,
                    track.durationLabel,
                    track.artworkUrl,
                    track.requesterName,
                    track.requestMessage,
                    communityActions = track.requesterName?.let { requester ->
                        {
                            CommunityMessageActions(
                                author = requester,
                                onReportContent = track.requestMessage?.let { message ->
                                    {
                                        communitySafetyActions.onBeginReport(
                                            AbuseReportTarget(
                                                kind = AbuseReportKind.Content,
                                                source = AbuseReportSource.Request,
                                                reportedUser = requester,
                                                contentSnapshot = message,
                                            ),
                                        )
                                    }
                                },
                                onReportUser = {
                                    communitySafetyActions.onBeginReport(
                                        AbuseReportTarget(
                                            kind = AbuseReportKind.User,
                                            source = AbuseReportSource.Request,
                                            reportedUser = requester,
                                        ),
                                    )
                                },
                                onBlockUser = { stationId?.let { communitySafetyActions.onBlockUser(it, requester) } },
                            )
                        }
                    },
                )
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Recently played", style = MaterialTheme.typography.headlineSmall)
        }
        if (history.isEmpty()) {
            item { EmptyTrackList("No recent history is available.") }
        } else {
            items(history) { track ->
                TrackCard(
                    null,
                    track.displayTitle,
                    track.artistName,
                    track.albumTitle,
                    track.durationLabel,
                    track.artworkUrl,
                    track.requesterName,
                    track.requestMessage,
                    communityActions = track.requesterName?.let { requester ->
                        {
                            CommunityMessageActions(
                                author = requester,
                                onReportContent = track.requestMessage?.let { message ->
                                    {
                                        communitySafetyActions.onBeginReport(
                                            AbuseReportTarget(
                                                kind = AbuseReportKind.Content,
                                                source = AbuseReportSource.Request,
                                                reportedUser = requester,
                                                contentSnapshot = message,
                                            ),
                                        )
                                    }
                                },
                                onReportUser = {
                                    communitySafetyActions.onBeginReport(
                                        AbuseReportTarget(
                                            kind = AbuseReportKind.User,
                                            source = AbuseReportSource.Request,
                                            reportedUser = requester,
                                        ),
                                    )
                                },
                                onBlockUser = { stationId?.let { communitySafetyActions.onBlockUser(it, requester) } },
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyTrackList(message: String) {
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun TrackCard(
    position: String?,
    title: String,
    artist: String?,
    album: String?,
    duration: String?,
    artworkUrl: String? = null,
    requesterName: String? = null,
    requestMessage: String? = null,
    communityActions: (@Composable () -> Unit)? = null,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            position?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
            }
            artworkUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                listOfNotNull(artist, album).takeIf(List<String>::isNotEmpty)?.let { details ->
                    Text(
                        details.joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                requesterName?.let {
                    Text(
                        "Requested by $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                requestMessage?.let {
                    Text(
                        "“$it”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            duration?.let {
                Spacer(Modifier.width(12.dp))
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            communityActions?.invoke()
        }
    }
}

@Composable
private fun FeatureScreen(title: String, description: String, icon: ImageVector, padding: PaddingValues) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MoreScreen(
    state: MainUiState,
    padding: PaddingValues,
    onRefreshAuth: (StationId) -> Unit,
    onSignIn: (StationId, String, String, String) -> Unit,
    onSignOut: (StationId) -> Unit,
    onRefreshListenerActivity: () -> Unit,
    onSearchRequests: (String, RequestSearchField) -> Unit,
    onSuggestRequest: (RequestSuggestionMode) -> Unit,
    onOpenRequestAlbum: (RequestSearchTarget) -> Unit,
    onPrepareRequest: (String) -> Unit,
    onCancelRequest: () -> Unit,
    onConfirmRequest: (String) -> Unit,
    onUseLastStationAtStartup: () -> Unit,
    onSetStartupStation: (StationId) -> Unit,
    onOpenStationPage: (StationPage) -> Unit,
    communitySafetyActions: CommunitySafetyActions,
    diagnosticUi: DiagnosticUi,
    onReviewTerms: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AccountSection(state, onRefreshAuth, onSignIn, onSignOut)
        CommunitySafetySection(state, communitySafetyActions, onReviewTerms)
        MoreDisclosure(
            title = "Song requests",
            summary = "Search or ask the station for an available track.",
            testTag = "more_song_requests",
        ) {
            SongRequestSection(state, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onCancelRequest, onConfirmRequest, onReviewTerms, showTitle = false)
        }
        if (state.selectedStation?.capabilities?.supportsListenerActivity == true) {
            MoreDisclosure(
                title = "Request activity",
                summary = "View membership, cooldown, and recent requests.",
                testTag = "more_request_activity",
            ) {
                ListenerActivitySection(state, onRefreshListenerActivity, showTitle = false)
            }
        }
        MoreDisclosure(
            title = "Device preferences",
            summary = "Choose which station opens at startup.",
            testTag = "more_device_preferences",
        ) {
            DevicePreferencesSection(state, onUseLastStationAtStartup, onSetStartupStation, showTitle = false)
        }
        DiagnosticsSection(state, diagnosticUi)
        SecondaryContentSection(state, onOpenStationPage)
        PrivacySection()
    }
}

@Composable
private fun DiagnosticsSection(
    state: MainUiState,
    diagnosticUi: DiagnosticUi,
) {
    val report = remember(
        diagnosticUi.environment,
        state.selectedStation?.name,
        state.playback,
        state.diagnosticTransitions,
    ) {
        buildDiagnosticReport(
            environment = diagnosticUi.environment,
            stationName = state.selectedStation?.name,
            playback = state.playback,
            transitions = state.diagnosticTransitions,
        )
    }
    MoreDisclosure(
        title = "In-app diagnostics",
        summary = "Preview and explicitly copy or share a privacy-safe support snapshot.",
        testTag = "more_diagnostics",
    ) {
        Card(Modifier.fillMaxWidth().testTag("diagnostics_card")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Review before sharing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "The snapshot uses a fixed allowlist. It does not include account details, messages, report content, URLs, device identifiers, raw errors, or logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("diagnostics_report"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    SelectionContainer {
                        Text(
                            report,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { diagnosticUi.actions.onCopy(report) },
                        modifier = Modifier.weight(1f).testTag("diagnostics_copy"),
                    ) { Text("Copy") }
                    Button(
                        onClick = { diagnosticUi.actions.onShare(report) },
                        modifier = Modifier.weight(1f).testTag("diagnostics_share"),
                    ) { Text("Share") }
                }
            }
        }
    }
}

@Composable
private fun CommunitySafetySection(
    state: MainUiState,
    actions: CommunitySafetyActions,
    onReviewTerms: () -> Unit,
) {
    val safety = state.communitySafety
    val selectedStation = state.selectedStation
    val status = when {
        safety.ageGateStatus == AgeGateStatus.Underage -> "Community features unavailable"
        safety.ageGateStatus == AgeGateStatus.NotCompleted -> "Age screen not completed"
        !safety.hasAcceptedCurrentTerms -> "Terms acceptance required"
        safety.communityContentVisible -> "Community content shown"
        else -> "Community content hidden"
    }
    MoreDisclosure(
        title = "Community safety",
        summary = status,
        testTag = "more_community_safety",
    ) {
        Card(Modifier.fillMaxWidth().testTag("community_safety_controls")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(status, fontWeight = FontWeight.SemiBold)
                when (safety.ageGateStatus) {
                    AgeGateStatus.NotCompleted -> {
                        Text(
                            "Enter your date of birth to determine community access. The date itself is not saved.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AgeScreenFields(state, actions.onSubmitAgeScreen)
                    }
                    AgeGateStatus.Underage -> Text(
                        "Playback remains available, but this adult network's Chat and public request attribution are unavailable.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AgeGateStatus.Adult -> {
                        TextButton(onClick = onReviewTerms, modifier = Modifier.testTag("open_terms_from_more")) {
                            Text(if (safety.hasAcceptedCurrentTerms) "Review Terms of Participation" else "Review and accept terms")
                        }
                        if (safety.hasAcceptedCurrentTerms) {
                            Button(
                                onClick = { actions.onSetCommunityContentVisible(!safety.communityContentVisible) },
                                modifier = Modifier.testTag("toggle_community_content"),
                            ) {
                                Text(if (safety.communityContentVisible) "Hide community content" else "Show community content")
                            }
                        }
                    }
                }
                val blocked = selectedStation?.let { station ->
                    safety.blockedUsers.filter { it.stationId == station.id }
                }.orEmpty()
                Text("Blocked users — ${selectedStation?.shortName ?: "station"}", style = MaterialTheme.typography.titleSmall)
                if (blocked.isEmpty()) {
                    Text("No users are blocked on this device for this station.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    blocked.forEach { user ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(user.displayName, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { actions.onUnblockUser(user.stationId, user.displayName) },
                                modifier = Modifier.testTag("unblock_${user.normalizedIdentity}"),
                            ) { Text("Unblock") }
                        }
                    }
                }
                Text(
                    "Blocks are stored only on this device and hide that user's Chat messages and request attribution in the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MoreDisclosure(
    title: String,
    summary: String,
    testTag: String,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(testTag) { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics {
                contentDescription = "$title, ${if (expanded) "expanded" else "collapsed"}"
            },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (expanded) "Hide" else "Open",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (expanded) content()
}

@Composable
private fun SecondaryContentSection(
    state: MainUiState,
    onOpenStationPage: (StationPage) -> Unit,
) {
    val station = state.selectedStation ?: return
    if (!station.capabilities.supportsSecondaryContent || station.secondaryPages.isEmpty()) {
        return
    }
    Text("Station links", style = MaterialTheme.typography.titleMedium)
    Text(
        "Opens securely in your browser.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(
        Modifier.fillMaxWidth().testTag("secondary_content_directory"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        station.secondaryPages.forEach { page ->
            Card(
                onClick = { onOpenStationPage(page) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("secondary_content_${page.kind.name.lowercase()}")
                    .semantics {
                        contentDescription = "Open ${page.title} for ${station.name} in browser"
                    },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(page.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            page.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun DevicePreferencesSection(
    state: MainUiState,
    onUseLastStationAtStartup: () -> Unit,
    onSetStartupStation: (StationId) -> Unit,
    showTitle: Boolean = true,
) {
    val preferences = state.stationPreferences
    val fixedStation = state.stations.firstOrNull { it.id == preferences.defaultStationId }
    val lastStation = state.stations.firstOrNull { it.id == preferences.lastStationId }
    val summary = when (preferences.startupMode) {
        StartupStationMode.LastSelected -> lastStation?.let { "Resume last station: ${it.name}" }
            ?: "Resume the last station selected on this device"
        StartupStationMode.Fixed -> fixedStation?.let { "Always start with ${it.name}" }
            ?: "Saved startup station is unavailable; using the safe catalog fallback"
    }

    if (showTitle) Text("Device preferences", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth().testTag("device_station_preferences")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                summary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { contentDescription = "Startup station preference: $summary" },
            )
            Button(
                onClick = onUseLastStationAtStartup,
                modifier = Modifier.testTag("startup_use_last_station"),
            ) { Text("Resume last station") }
            Button(
                onClick = { state.selectedStation?.id?.let(onSetStartupStation) },
                enabled = state.selectedStation != null,
                modifier = Modifier.testTag("startup_use_current_station"),
            ) { Text("Use current station at startup") }
        }
    }
}

@Composable
private fun AccountSection(
    state: MainUiState,
    onRefresh: (StationId) -> Unit,
    onSignIn: (StationId, String, String, String) -> Unit,
    onSignOut: (StationId) -> Unit,
) {
    val accountStates = state.accounts.ifEmpty {
        state.selectedStation?.let { selected ->
            listOf(StationAccountUiState(selected, state.auth ?: com.codeframe78.twentyfourseven.player.domain.AuthState(selected.id)))
        }.orEmpty()
    }
    val selectedAccount = accountStates.firstOrNull { it.station.id == state.selectedStation?.id }
        ?: accountStates.firstOrNull()
    val otherAccounts = accountStates.filterNot { it.station.id == selectedAccount?.station?.id }
    var showOtherAccounts by remember(state.selectedStation?.id) { mutableStateOf(false) }
    val visibleAccounts = listOfNotNull(selectedAccount) + if (showOtherAccounts) otherAccounts else emptyList()

    Text("Account", style = MaterialTheme.typography.titleMedium)
    Text(
        "Accounts and sign-in sessions are station-specific.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 720.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleAccounts.chunked(2).forEach { rowAccounts ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowAccounts.forEach { account ->
                            AccountCard(
                                account = account,
                                isSelectedStation = account.station.id == state.selectedStation?.id,
                                onRefresh = onRefresh,
                                onSignIn = onSignIn,
                                onSignOut = onSignOut,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowAccounts.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleAccounts.forEach { account ->
                    AccountCard(
                        account = account,
                        isSelectedStation = account.station.id == state.selectedStation?.id,
                        onRefresh = onRefresh,
                        onSignIn = onSignIn,
                        onSignOut = onSignOut,
                    )
                }
            }
        }
    }
    if (otherAccounts.isNotEmpty()) {
        TextButton(
            onClick = { showOtherAccounts = !showOtherAccounts },
            modifier = Modifier.testTag("toggle_other_station_accounts"),
        ) {
            Text(if (showOtherAccounts) "Hide other station accounts" else "Manage other station accounts")
        }
    }
}

@Composable
private fun ListenerActivitySection(
    state: MainUiState,
    onRefresh: () -> Unit,
    showTitle: Boolean = true,
) {
    val station = state.selectedStation ?: return
    if (!station.capabilities.supportsListenerActivity) return
    val activity = state.listenerActivity
    val membershipLabel = when (activity?.membershipTier) {
        MembershipTier.Standard -> "Standard member"
        MembershipTier.Vip -> "VIP member"
        MembershipTier.Rip -> "RIP member"
        MembershipTier.Unknown, null -> "Not reported by station"
    }
    val readinessLabel = when (activity?.requestReadiness) {
        RequestReadiness.Ready -> "Ready to request"
        RequestReadiness.Waiting -> activity.waitMinutes?.let { "Wait $it minutes" }
            ?: "Request cooldown active"
        RequestReadiness.Unknown, null -> "Not reported by station"
    }

    if (showTitle) Text("Request activity", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth().testTag("listener_activity_card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Authenticated history and station-reported request status",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = state.auth?.status == AuthStatus.SignedIn &&
                        activity?.status != ListenerActivityLoadStatus.Loading,
                    modifier = Modifier.testTag("refresh_listener_activity"),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh request activity")
                }
            }
            when {
                state.auth?.status != AuthStatus.SignedIn -> Text(
                    "Sign in to ${station.shortName} from its account card above to load this private activity.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                activity == null || activity.status == ListenerActivityLoadStatus.Idle -> Text(
                    "Request activity has not been loaded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                activity.status == ListenerActivityLoadStatus.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Loading request activity…")
                }
                activity.status == ListenerActivityLoadStatus.Error -> Text(
                    activity.errorMessage ?: "Request activity could not be loaded right now.",
                    color = MaterialTheme.colorScheme.error,
                )
                else -> {
                    ListenerStatusRow("Membership", membershipLabel)
                    ListenerStatusRow("Request status", readinessLabel)
                    Text("Your last requests", style = MaterialTheme.typography.titleSmall)
                    if (activity.recentRequests.isEmpty()) {
                        Text(
                            "No recent requests were reported by this station.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        activity.recentRequests.forEach { request ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("request_history_${request.position}")
                                    .semantics {
                                        contentDescription = "Request ${request.position}: ${request.trackSummary}; ${request.requestedAtLabel}"
                                    },
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    request.position.toString(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(request.trackSummary, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        request.requestedAtLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListenerStatusRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label: $value" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccountCard(
    account: StationAccountUiState,
    isSelectedStation: Boolean,
    onRefresh: (StationId) -> Unit,
    onSignIn: (StationId, String, String, String) -> Unit,
    onSignOut: (StationId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val station = account.station
    val auth = account.auth
    val palette = stationPalette(station.id)
    var username by remember(station.id) { mutableStateOf("") }
    var password by remember(station.id) { mutableStateOf("") }
    var securityCode by remember(station.id) { mutableStateOf("") }
    val useStackedHeader = LocalDensity.current.fontScale > 1.5f
    Card(modifier.fillMaxWidth().testTag("account_card_${station.id.value}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (useStackedHeader) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountStationIdentity(station, palette, isSelectedStation, Modifier.fillMaxWidth())
                    AccountStatusBadge(station.name, station.id, auth.status)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountStationIdentity(station, palette, isSelectedStation, Modifier.weight(1f))
                    AccountStatusBadge(station.name, station.id, auth.status)
                }
            }
            if (!station.capabilities.supportsAuthentication) {
                Text("Account sign in is unavailable for this station.")
                return@Column
            }
            when (auth.status) {
                AuthStatus.SignedIn -> {
                    Text("Signed in as ${auth.displayName.orEmpty()}", fontWeight = FontWeight.Medium)
                    Button(
                        onClick = { onSignOut(station.id) },
                        modifier = Modifier.testTag("account_sign_out_${station.id.value}"),
                    ) { Text("Sign out of ${station.shortName}") }
                }
                AuthStatus.LoadingChallenge, AuthStatus.SigningIn -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(if (auth.status == AuthStatus.SigningIn) "Signing in…" else "Loading secure sign in…")
                    }
                }
                AuthStatus.Unavailable -> {
                    Text("Not signed in", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = { onRefresh(station.id) },
                        modifier = Modifier.testTag("account_load_sign_in_${station.id.value}"),
                    ) { Text("Load ${station.shortName} sign in") }
                }
                AuthStatus.Expired -> {
                    Text(
                        auth.errorMessage ?: "Your saved station session expired. Sign in again.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = { onRefresh(station.id) },
                        modifier = Modifier.testTag("account_sign_in_again_${station.id.value}"),
                    ) { Text("Sign in to ${station.shortName} again") }
                }
                AuthStatus.SignedOut, AuthStatus.Error -> {
                    auth.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (auth.challengeImageUrl == null) {
                        Button(
                            onClick = { onRefresh(station.id) },
                            modifier = Modifier.testTag("account_retry_sign_in_${station.id.value}"),
                        ) { Text("Try loading ${station.shortName} sign in") }
                    } else {
                        OutlinedTextField(
                            username,
                            { username = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_username_${station.id.value}")
                                .semantics { contentDescription = "Username for ${station.name}" },
                        )
                        OutlinedTextField(
                            password,
                            { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_password_${station.id.value}")
                                .semantics { contentDescription = "Password for ${station.name}" },
                        )
                        AsyncImage(
                            model = auth.challengeImageUrl,
                            contentDescription = "${station.name} security code image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(12.dp),
                        )
                        OutlinedTextField(
                            securityCode,
                            { securityCode = it.filter(Char::isLetterOrDigit) },
                            label = { Text("Security code") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_security_code_${station.id.value}")
                                .semantics { contentDescription = "Security code for ${station.name}" },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onSignIn(station.id, username, password, securityCode)
                                    password = ""
                                    securityCode = ""
                                },
                                modifier = Modifier.testTag("account_sign_in_${station.id.value}"),
                            ) { Text("Sign in to ${station.shortName}") }
                            TextButton(onClick = { onRefresh(station.id) }) { Text("New code") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountStationIdentity(
    station: Station,
    palette: StationPalette,
    isSelectedStation: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(palette.accent)
                .semantics { contentDescription = "${station.name} station marker" },
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                station.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("account_station_name_${station.id.value}"),
            )
            if (isSelectedStation) {
                Text(
                    "Current playback station",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AccountStatusBadge(stationName: String, stationId: StationId, status: AuthStatus) {
    val (label, color) = when (status) {
        AuthStatus.SignedIn -> "Signed in" to MaterialTheme.colorScheme.primary
        AuthStatus.LoadingChallenge -> "Loading" to MaterialTheme.colorScheme.secondary
        AuthStatus.SigningIn -> "Signing in" to MaterialTheme.colorScheme.secondary
        AuthStatus.Expired -> "Expired" to MaterialTheme.colorScheme.error
        AuthStatus.Error -> "Attention" to MaterialTheme.colorScheme.error
        AuthStatus.SignedOut, AuthStatus.Unavailable -> "Signed out" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        modifier = Modifier
            .testTag("account_status_${stationId.value}")
            .semantics { contentDescription = "$stationName account status: $label" },
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SongRequestSection(
    state: MainUiState,
    onSearch: (String, RequestSearchField) -> Unit,
    onSuggest: (RequestSuggestionMode) -> Unit,
    onOpenAlbum: (RequestSearchTarget) -> Unit,
    onPrepareRequest: (String) -> Unit,
    onCancelRequest: () -> Unit,
    onConfirmRequest: (String) -> Unit,
    onReviewTerms: () -> Unit,
    showTitle: Boolean = true,
) {
    val requests = state.requests
    var query by remember(state.selectedStation?.id) { mutableStateOf("") }
    var field by remember(state.selectedStation?.id) { mutableStateOf(RequestSearchField.Title) }
    var fieldMenuOpen by remember { mutableStateOf(false) }
    var trackSortOrder by remember(state.selectedStation?.id) { mutableStateOf(TrackSortOrder.LibraryOrder) }
    var trackSortMenuOpen by remember { mutableStateOf(false) }
    val signedIn = state.auth?.status == AuthStatus.SignedIn

    RequestConfirmationDialog(state, onCancelRequest, onConfirmRequest, onReviewTerms)

    if (showTitle) Text("Song requests", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.selectedStation?.capabilities?.supportsRequests != true) {
                Text("Song requests have not been verified for this station.")
                return@Column
            }
            Text(
                "Search the station library. A request is only sent after you review and confirm one available track.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(onClick = { fieldMenuOpen = true }) {
                        Text("Search by ${field.name.lowercase()}")
                    }
                    DropdownMenu(expanded = fieldMenuOpen, onDismissRequest = { fieldMenuOpen = false }) {
                        RequestSearchField.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    field = option
                                    fieldMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(100) },
                label = { Text("Library search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSearch(query, field) },
                enabled = requests?.status != SongRequestLoadStatus.Loading &&
                    requests?.status != SongRequestLoadStatus.Submitting,
            ) { Text("Search") }

            Text(
                "Or let the station choose one available track.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onSuggest(RequestSuggestionMode.Random) },
                enabled = requests?.status != SongRequestLoadStatus.Loading &&
                    requests?.status != SongRequestLoadStatus.Submitting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Suggest a random track") }
            Button(
                onClick = { onSuggest(RequestSuggestionMode.LeastPlayed) },
                enabled = requests?.status != SongRequestLoadStatus.Loading &&
                    requests?.status != SongRequestLoadStatus.Submitting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Suggest a least-played random track") }

            if (requests?.status == SongRequestLoadStatus.Loading || requests?.status == SongRequestLoadStatus.Submitting) {
                CircularProgressIndicator()
                Text(if (requests.status == SongRequestLoadStatus.Submitting) "Sending one request…" else "Loading station library…")
            }
            requests?.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            requests?.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            requests?.searchResults?.takeIf { it.isNotEmpty() }?.let { results ->
                Text("Search results", style = MaterialTheme.typography.titleSmall)
                results.forEach { result ->
                    Surface(
                        onClick = { onOpenAlbum(result.target) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(result.title, fontWeight = FontWeight.Medium)
                            listOfNotNull(result.subtitle, result.year).takeIf { it.isNotEmpty() }?.let { details ->
                                Text(
                                    details.joinToString(" • "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                if (result.target is RequestSearchTarget.Artist) "View albums" else "View album",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            requests?.tracks?.takeIf { it.isNotEmpty() }?.let { tracks ->
                Text(requests.albumTitle ?: "Album tracks", style = MaterialTheme.typography.titleSmall)
                if (!signedIn) {
                    Text("Sign in to request a track. Library browsing remains available without an account.")
                }
                Box {
                    TextButton(
                        onClick = { trackSortMenuOpen = true },
                        modifier = Modifier.testTag("library_track_sort"),
                    ) {
                        Text("Sort: ${trackSortOrder.label}")
                    }
                    DropdownMenu(
                        expanded = trackSortMenuOpen,
                        onDismissRequest = { trackSortMenuOpen = false },
                    ) {
                        TrackSortOrder.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    trackSortOrder = option
                                    trackSortMenuOpen = false
                                },
                            )
                        }
                    }
                }
                tracks.sortedForDisplay(trackSortOrder) { it.availability }.forEach { track ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(track.title, fontWeight = FontWeight.Medium)
                            Text(
                                listOfNotNull(track.artist, track.duration).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            RequestStatusIndicator(
                                availability = track.availability,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            track.availability.detail?.let { detail ->
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (track.availability.canRequest) {
                            TextButton(
                                onClick = { onPrepareRequest(track.songId) },
                                enabled = requests.status == SongRequestLoadStatus.Ready,
                            ) { Text("Request Now") }
                        }
                    }
                }
            }
        }
    }
}
