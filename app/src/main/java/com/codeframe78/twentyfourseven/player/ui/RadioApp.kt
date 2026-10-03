package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import com.codeframe78.twentyfourseven.player.domain.RequestHistoryEntry
import com.codeframe78.twentyfourseven.player.ui.theme.requestAvailableGreen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import com.codeframe78.twentyfourseven.player.ui.theme.themedAccent
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.codeframe78.twentyfourseven.player.domain.FeedbackCategory
import com.codeframe78.twentyfourseven.player.domain.FeedbackSubmission
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityLoadStatus
import com.codeframe78.twentyfourseven.player.domain.MembershipTier
import com.codeframe78.twentyfourseven.player.domain.QueueLoadStatus
import com.codeframe78.twentyfourseven.player.domain.QueueTrack
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationPage
import com.codeframe78.twentyfourseven.player.domain.StationPageKind
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

/** The More tab carries the unread private message count, since that is where messages are read. */
@Composable
private fun NavigationItemIcon(item: NavigationItem, unreadMessages: Int) {
    if (item.destination == MainDestination.More && unreadMessages > 0) {
        BadgedBox(
            badge = {
                Badge(Modifier.testTag("unread_messages_badge")) {
                    Text(if (unreadMessages > 99) "99+" else unreadMessages.toString())
                }
            },
        ) { Icon(item.icon, contentDescription = null) }
    } else {
        Icon(item.icon, contentDescription = null)
    }
}

internal fun messagesButtonDescription(unreadMessages: Int): String = when {
    unreadMessages <= 0 -> "Private messages"
    unreadMessages == 1 -> "Private messages, 1 unread"
    else -> "Private messages, $unreadMessages unread"
}

internal fun navigationItemDescription(destination: MainDestination, label: String, unreadMessages: Int): String =
    when {
        destination != MainDestination.More || unreadMessages <= 0 -> label
        unreadMessages == 1 -> "$label, 1 unread private message"
        else -> "$label, $unreadMessages unread private messages"
    }

private fun navigationItemDescription(item: NavigationItem, unreadMessages: Int): String =
    navigationItemDescription(item.destination, item.label, unreadMessages)

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
    val onSetChatMentionsEnabled: (StationId, Boolean) -> Unit = { _, _ -> },
    val onSetForegroundChatMentionMonitorEnabled: (StationId, Boolean) -> Unit = { _, _ -> },
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
    feedbackUi: FeedbackUi = FeedbackUi(),
    onOpenAppGuide: () -> Unit = {},
    trackActions: TrackActions = TrackActions(),
    privateMessageActions: PrivateMessageActions = PrivateMessageActions(),
    stationExtrasActions: StationExtrasActions = StationExtrasActions(),
    albumActions: AlbumActions = AlbumActions(),
) {
    val profileOpener = stationExtrasActions.onOpenProfile.takeIf {
        state.selectedStation?.capabilities?.supportsMemberProfiles == true &&
            state.communitySafety.canViewCommunityContent
    }
    val albumOpener = albumActions.onOpen.takeIf { state.selectedStation?.capabilities?.supportsRequests == true }
    var showMessages by rememberSaveable(state.selectedStation?.id?.value) { mutableStateOf(false) }
    val messagesOpener: (() -> Unit)? = if (state.selectedStation?.capabilities?.supportsPrivateMessages == true) {
        { showMessages = true }
    } else {
        null
    }
    CompositionLocalProvider(
        LocalStationExtrasActions provides stationExtrasActions,
        LocalMemberProfileOpener provides profileOpener,
        LocalAlbumOpener provides albumOpener,
        LocalMessagesOpener provides messagesOpener,
    ) {
        RadioAppContent(state, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, feedbackUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onConfirmRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, onOpenAppGuide, trackActions)
        if (showMessages && messagesOpener != null) {
            PrivateMessagesScreen(state, privateMessageActions, communitySafetyActions) { showMessages = false }
        }
        PlayedHistoryDialog(state, stationExtrasActions)
        AlbumDialog(state, albumActions)
        MemberProfileDialog(state, stationExtrasActions, privateMessageActions.onNewMessage)
        PrivateMessageComposeDialog(state.privateMessages?.compose, privateMessageActions)
    }
}

@Composable
private fun RadioAppContent(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
    onSelectDestination: (MainDestination) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    diagnosticUi: DiagnosticUi,
    feedbackUi: FeedbackUi,
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
    onOpenAppGuide: () -> Unit,
    trackActions: TrackActions,
) {
    var showTerms by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (usesNavigationRail(maxWidth, maxHeight)) {
            TabletShell(state, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, feedbackUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onConfirmRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, onOpenAppGuide, trackActions = trackActions) { showTerms = true }
        } else {
            PhoneShell(state, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, feedbackUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onConfirmRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, isCoverDisplay = isCoverDisplayWindow(maxWidth, maxHeight), onOpenAppGuide = onOpenAppGuide, trackActions = trackActions) { showTerms = true }
        }
    }
    if (showTerms) {
        CommunityTermsDialog(
            alreadyAccepted = state.communitySafety.hasAcceptedCurrentTerms,
            onAgree = {
                communitySafetyActions.onAcceptTerms()
                showTerms = false
            },
            onDecline = { showTerms = false },
        )
    }
    AbuseReportDialog(state, communitySafetyActions)
    AlbumRatingDialog(state, trackActions)
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
    feedbackUi: FeedbackUi,
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
    isCoverDisplay: Boolean,
    onOpenAppGuide: () -> Unit,
    trackActions: TrackActions,
    onReviewTerms: () -> Unit,
) {
    val showNavigationLabels = LocalDensity.current.fontScale <= 1.5f
    Scaffold(
        topBar = {
            if (!isCoverDisplay) {
                StationTopBar(state)
            }
        },
        bottomBar = {
            if (isCoverDisplay) {
                if (state.destination != MainDestination.Player) {
                    PersistentMiniPlayer(state, onSelectDestination, onPlay, onPause)
                }
            } else {
                if (state.destination != MainDestination.Player) {
                    PersistentMiniPlayer(state, onSelectDestination, onPlay, onPause)
                }
                NavigationBar(Modifier.testTag("phone_navigation_bar")) {
                    val unreadMessages = state.privateMessages?.unreadCount ?: 0
                    val accent = stationPalette(state.selectedStation?.id).themedAccent()
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = state.destination == item.destination,
                            onClick = { onSelectDestination(item.destination) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = accent,
                                indicatorColor = accent.copy(alpha = 0.22f),
                            ),
                            icon = { NavigationItemIcon(item, unreadMessages) },
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
                            modifier = Modifier.semantics { contentDescription = navigationItemDescription(item, unreadMessages) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        DestinationContent(state, padding, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, feedbackUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, onOpenAppGuide, onReviewTerms, isCoverDisplay, trackActions)
    }
    if (state.destination != MainDestination.Favorites && state.album == null) {
        RequestResultDialog(state, onCancelRequest)
    }
    MemberFavoritesDialog(state, LocalStationExtrasActions.current, onPrepareFavoriteRequest)
    // One confirmation for every place a request can start from; it opens last, so it sits above those screens.
    RequestConfirmationDialog(state, onCancelRequest, onConfirmRequest, onReviewTerms)
}

/**
 * A full-app flip-phone cover display is small and close to square. The cover camera cutout can
 * make its usable app window short and wide, so the window contract accommodates that inset.
 * This remains a window contract rather than a device check.
 */
internal fun usesNavigationRail(maxWidth: androidx.compose.ui.unit.Dp, maxHeight: androidx.compose.ui.unit.Dp): Boolean =
    maxWidth >= 600.dp || (
        usesLandscapePlayerLayout(maxWidth, maxHeight) &&
            !isCoverDisplayWindow(maxWidth, maxHeight)
        )

private fun isCoverDisplayWindow(maxWidth: androidx.compose.ui.unit.Dp, maxHeight: androidx.compose.ui.unit.Dp): Boolean {
    if (maxWidth > 480.dp || maxHeight > 480.dp) return false
    val aspectRatio = maxWidth.value / maxHeight.value
    return aspectRatio in 0.8f..1.5f
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
    feedbackUi: FeedbackUi,
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
    onOpenAppGuide: () -> Unit,
    trackActions: TrackActions,
    onReviewTerms: () -> Unit,
) {
    val showNavigationLabels = LocalDensity.current.fontScale <= 1.5f
    Row(Modifier.fillMaxSize()) {
        NavigationRail(Modifier.fillMaxHeight().testTag("tablet_navigation_rail")) {
            Spacer(Modifier.height(12.dp))
            val unreadMessages = state.privateMessages?.unreadCount ?: 0
            val accent = stationPalette(state.selectedStation?.id).themedAccent()
            navigationItems.forEach { item ->
                NavigationRailItem(
                    selected = state.destination == item.destination,
                    onClick = { onSelectDestination(item.destination) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = accent,
                        indicatorColor = accent.copy(alpha = 0.22f),
                    ),
                    icon = { NavigationItemIcon(item, unreadMessages) },
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
                    modifier = Modifier.semantics { contentDescription = navigationItemDescription(item, unreadMessages) },
                )
            }
        }
        VerticalDivider(Modifier.fillMaxHeight())
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = { StationTopBar(state) },
            bottomBar = {
                if (state.destination != MainDestination.Player) {
                    PersistentMiniPlayer(state, onSelectDestination, onPlay, onPause)
                }
            },
        ) { padding ->
            DestinationContent(state, padding, onSelectStation, onSelectDestination, onPlay, onPause, onStop, sleepTimerActions, audioOutputActions, diagnosticUi, feedbackUi, onRefreshQueue, onRefreshFavorites, onRefreshListenerActivity, onRefreshChat, onSendChatMessage, onRefreshAuth, onSignIn, onSignOut, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onPrepareFavoriteRequest, onCancelRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, onOpenAppGuide, onReviewTerms, trackActions = trackActions)
        }
        if (state.destination != MainDestination.Favorites && state.album == null) {
            RequestResultDialog(state, onCancelRequest)
        }
        MemberFavoritesDialog(state, LocalStationExtrasActions.current, onPrepareFavoriteRequest)
        RequestConfirmationDialog(state, onCancelRequest, onConfirmRequest, onReviewTerms)
    }
}

/** The station is the headline: its logo and its full name, which always stays on one line and is never broken up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationTopBar(state: MainUiState) {
    val station = state.selectedStation
    val openMessages = LocalMessagesOpener.current.takeIf { state.auth?.status == AuthStatus.SignedIn }
    TopAppBar(
        navigationIcon = {
            Image(
                painter = painterResource(station?.let { stationSelectorLogoResource(it.id) } ?: R.drawable.app_logo),
                contentDescription = if (station == null) "24Seven.FM logo" else "${station.name} logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(start = 12.dp).size(38.dp).clip(RoundedCornerShape(10.dp)),
            )
        },
        title = {
            Text(
                station?.name ?: "24Seven.FM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 20.sp, stepSize = 1.sp),
                modifier = Modifier.padding(start = 4.dp).testTag("top_bar_station_name"),
            )
        },
        actions = {
            openMessages?.let { open ->
                val unread = state.privateMessages?.unreadCount ?: 0
                IconButton(
                    onClick = open,
                    modifier = Modifier.testTag("open_private_messages").semantics {
                        contentDescription = messagesButtonDescription(unread)
                    },
                ) {
                    BadgedBox(
                        badge = {
                            if (unread > 0) {
                                Badge(Modifier.testTag("unread_messages_top_badge")) {
                                    Text(if (unread > 99) "99+" else unread.toString())
                                }
                            }
                        },
                    ) { Icon(Icons.Default.Mail, contentDescription = null) }
                }
            }
            CastRouteButton()
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
    feedbackUi: FeedbackUi,
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
    onUseLastStationAtStartup: () -> Unit,
    onSetStartupStation: (StationId) -> Unit,
    onOpenStationPage: (StationPage) -> Unit,
    communitySafetyActions: CommunitySafetyActions,
    onOpenAppGuide: () -> Unit,
    onReviewTerms: () -> Unit,
    isCoverDisplay: Boolean = false,
    trackActions: TrackActions = TrackActions(),
) {
    when (state.destination) {
        MainDestination.Player -> AdaptivePlayerScreen(
            state,
            padding,
            onSelectStation,
            onPlay,
            onStop,
            sleepTimerActions,
            audioOutputActions,
            isCoverDisplay,
            trackActions,
        )
        MainDestination.Favorites -> FavoriteTracksScreen(
            state = state,
            padding = padding,
            onRefresh = onRefreshFavorites,
            onPrepareRequest = onPrepareFavoriteRequest,
            onCancelRequest = onCancelRequest,
            onOpenAccount = { onSelectDestination(MainDestination.More) },
        )
        MainDestination.Chat -> ChatScreen(state, padding, onRefreshChat, onSendChatMessage, communitySafetyActions, onReviewTerms)
        MainDestination.Queue -> QueueScreen(state, padding, onRefreshQueue, communitySafetyActions)
        MainDestination.More -> MoreScreen(state, padding, onRefreshAuth, onSignIn, onSignOut, onRefreshListenerActivity, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onCancelRequest, onUseLastStationAtStartup, onSetStartupStation, onOpenStationPage, communitySafetyActions, diagnosticUi, feedbackUi, onOpenAppGuide, onReviewTerms)
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
        chat.status == ChatLoadStatus.Loading -> SkeletonList(
            Modifier.fillMaxSize().padding(padding),
            rows = 8,
            showsArtwork = false,
            description = "Loading chat",
        )
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
    var draft by rememberSaveable(state.selectedStation?.id?.value) { mutableStateOf("") }
    var awaitingSend by remember(state.selectedStation?.id) { mutableStateOf(false) }
    val messageKeys = remember(chat.messages) { chatMessageKeys(chat.messages) }
    val signedIn = state.auth?.status == AuthStatus.SignedIn
    val ownNick = state.auth?.displayName?.takeIf { signedIn }
    LaunchedEffect(chat.isSending, chat.sendErrorMessage, chat.messages) {
        if (awaitingSend && !chat.isSending) {
            if (chat.sendErrorMessage != null) {
                awaitingSend = false
            } else {
                draft = ""
                awaitingSend = false
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        // The channel bar: the room and its station, as an IRC client titles a window.
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("#", style = ChatLineStyle(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(
                "Station chat",
                style = ChatLineStyle(),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                state.selectedStation?.name?.let { " · $it" }.orEmpty(),
                style = ChatLineStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh chat")
            }
        }
        RefreshableBox(onRefresh = onRefresh, modifier = Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            if (chat.messages.isEmpty()) {
                item { EmptyTrackList("No recent chat messages are available.") }
            } else {
                itemsIndexed(
                    chat.messages,
                    key = { index, _ -> messageKeys[index] },
                ) { index, message ->
                    // Newest first: a day opens above its oldest line, which is the one before an older day.
                    val day = chatStamp(message.postedAtLabel)?.day
                    val olderDay = chat.messages.getOrNull(index + 1)?.let { chatStamp(it.postedAtLabel)?.day }
                    var menuOpen by remember(messageKeys[index]) { mutableStateOf(false) }
                    Column {
                        if (day != null && day != olderDay) ChatDayRule(day)
                        Box {
                            ChatLine(
                                message,
                                ownNick = ownNick,
                                modifier = Modifier.clickable(
                                    onClickLabel = "Safety actions for ${message.authorDisplayName}",
                                ) { menuOpen = true },
                            )
                            CommunityMessageMenu(
                                expanded = menuOpen,
                                onDismiss = { menuOpen = false },
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
                    }
                }
            }
        }
        }
        HorizontalDivider()
        if (signedIn) {
            val send = {
                if (draft.isNotBlank() && !chat.isSending) {
                    awaitingSend = true
                    onSendMessage(draft)
                }
            }
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
                chat.sendErrorMessage?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 255) draft = it },
                        placeholder = { Text("Message", style = ChatLineStyle()) },
                        textStyle = ChatLineStyle(),
                        // The limit only matters once it is close.
                        supportingText = if (draft.length >= 200) {
                            { Text("${draft.length}/255") }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !chat.isSending,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                    )
                    IconButton(onClick = send, enabled = draft.isNotBlank() && !chat.isSending) {
                        if (chat.isSending) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        } else {
            Text(
                "Sign in from More to send messages.",
                style = ChatLineStyle(),
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
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
        CommunityMessageMenu(expanded, { expanded = false }, author, onReportContent, onReportUser, onBlockUser)
    }
}

@Composable
private fun CommunityMessageMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    author: String,
    onReportContent: (() -> Unit)?,
    onReportUser: () -> Unit,
    onBlockUser: () -> Unit,
) {
    val openProfile = LocalMemberProfileOpener.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        openProfile?.let { viewProfile ->
            DropdownMenuItem(
                text = { Text("View profile") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                onClick = {
                    onDismiss()
                    viewProfile(author)
                },
            )
        }
        onReportContent?.let { reportContent ->
            DropdownMenuItem(
                text = { Text("Report content") },
                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                onClick = {
                    onDismiss()
                    reportContent()
                },
            )
        }
        DropdownMenuItem(
            text = { Text("Report user") },
            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
            onClick = {
                onDismiss()
                onReportUser()
            },
        )
        DropdownMenuItem(
            text = { Text("Block user") },
            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
            onClick = {
                onDismiss()
                onBlockUser()
            },
        )
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
private fun AbuseReportDialog(state: MainUiState, actions: CommunitySafetyActions) {
    val report = state.abuseReport
    if (report.status == AbuseReportStatus.Idle) return
    val target = report.target ?: return
    var reporterName by rememberSaveable(target) { mutableStateOf(state.auth?.displayName.orEmpty()) }
    var category by rememberSaveable(target) { mutableStateOf(AbuseReportCategory.Harassment) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var details by rememberSaveable(target) { mutableStateOf("") }
    val canDismiss = report.status !in setOf(
        AbuseReportStatus.PreparingEmail,
        AbuseReportStatus.EmailReady,
    )

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
                    AbuseReportStatus.Ready -> {
                        Text(
                            "The Player will prepare a bounded report addressed to the monitored moderation mailbox. Your email app will open so you can review, edit, and explicitly send it. The Player cannot read your email account or confirm delivery.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            reporterName,
                            { reporterName = it.take(100) },
                            label = { Text("Your name or station nickname") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("reporter_name"),
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
                            modifier = Modifier.fillMaxWidth().testTag("report_details"),
                        )
                    }
                    AbuseReportStatus.PreparingEmail, AbuseReportStatus.EmailReady -> {
                        Text("Opening your email app for review…")
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    }
                    AbuseReportStatus.EmailHandoffStarted -> Text(
                        "Android opened the email handoff. The report is not sent unless you choose an email app, review the draft, and send it there; the Player cannot confirm delivery.",
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
                                category = category,
                                optionalDetails = details,
                            ),
                        )
                    },
                    enabled = reporterName.trim().length >= 2,
                    modifier = Modifier.testTag("submit_abuse_report"),
                ) { Text("Review email") }
                AbuseReportStatus.Error -> if (report.retryAllowed) {
                    Button(onClick = actions.onRetryReport) { Text("Try again") }
                } else {
                    Button(onClick = actions.onDismissReport) { Text("Done") }
                }
                AbuseReportStatus.EmailHandoffStarted -> Button(onClick = actions.onDismissReport) { Text("Done") }
                else -> Unit
            }
        },
        dismissButton = {
            if (canDismiss && report.status != AbuseReportStatus.EmailHandoffStarted) {
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
        queue.status == QueueLoadStatus.Loading -> SkeletonList(
            Modifier.fillMaxSize().padding(padding),
            rows = 7,
            description = "Loading queue",
        )
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
            LocalStationExtrasActions.current.onOpenHistory.takeIf {
                state.selectedStation?.capabilities?.supportsPlayedHistoryArchive == true
            },
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
    onOpenHistory: (() -> Unit)? = null,
) {
    RefreshableBox(onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("queue_lists"),
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
                    albumId = track.albumId,
                )
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recently played", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                onOpenHistory?.let { openHistory ->
                    TextButton(onClick = openHistory, modifier = Modifier.testTag("open_played_history")) {
                        Text("Browse earlier")
                    }
                }
            }
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
                    albumId = track.albumId,
                )
            }
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
    albumId: String? = null,
) {
    Card(Modifier.fillMaxWidth().clip(CardDefaults.shape).opensAlbum(albumId, album, artworkUrl)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            position?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
            }
            artworkUrl?.let {
                AsyncImage(
                    model = crossfadingImage(it),
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
                        modifier = Modifier.opensMemberProfile(it).padding(vertical = 6.dp),
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
    onUseLastStationAtStartup: () -> Unit,
    onSetStartupStation: (StationId) -> Unit,
    onOpenStationPage: (StationPage) -> Unit,
    communitySafetyActions: CommunitySafetyActions,
    diagnosticUi: DiagnosticUi,
    feedbackUi: FeedbackUi,
    onOpenAppGuide: () -> Unit,
    onReviewTerms: () -> Unit,
) {
    val station = state.selectedStation
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SettingsGroup("Account") {
            AccountSection(state, onRefreshAuth, onSignIn, onSignOut)
        }
        SettingsGroup(station?.shortName ?: "Station") {
            MoreDisclosure(
                title = "Song requests",
                summary = "Search the library or ask the station for a track.",
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                testTag = "more_song_requests",
            ) {
                SongRequestSection(state, onSearchRequests, onSuggestRequest, onOpenRequestAlbum, onPrepareRequest, onCancelRequest, showTitle = false)
            }
            if (station?.capabilities?.supportsListenerActivity == true) {
                MoreDisclosure(
                    title = "Request activity",
                    summary = "Membership, request status, and your recent requests.",
                    icon = Icons.Default.History,
                    testTag = "more_request_activity",
                ) {
                    ListenerActivitySection(state, onRefreshListenerActivity, showTitle = false)
                }
            }
            if (station?.capabilities?.supportsStationNews == true) {
                MoreDisclosure(
                    title = "Station news",
                    summary = "Playlist updates and announcements.",
                    icon = Icons.Default.Newspaper,
                    testTag = "more_station_news",
                ) {
                    StationNewsSection(state, LocalStationExtrasActions.current)
                }
            }
            LocalMessagesOpener.current?.let { openMessages ->
                MoreLink(
                    title = "Private messages",
                    summary = privateMessagesSummary(state.privateMessages),
                    icon = Icons.Default.Mail,
                    testTag = "more_private_messages",
                    onClick = openMessages,
                )
            }
            SecondaryContentSection(state, onOpenStationPage)
        }
        SettingsGroup("Community") {
            CommunitySafetySection(state, communitySafetyActions, onReviewTerms)
            CommunityNotificationSection(
                state,
                communitySafetyActions.onSetChatMentionsEnabled,
                communitySafetyActions.onSetForegroundChatMentionMonitorEnabled,
            )
        }
        SettingsGroup("App") {
            MoreDisclosure(
                title = "Startup station",
                summary = startupStationSummary(state),
                icon = Icons.Default.PlayCircle,
                testTag = "more_device_preferences",
            ) {
                DevicePreferencesSection(state, onUseLastStationAtStartup, onSetStartupStation, showTitle = false)
            }
            MoreLink(
                title = "App guide",
                summary = "A short tour of the Player.",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                testTag = "open_app_guide",
                onClick = onOpenAppGuide,
            )
            FeedbackSection(state, diagnosticUi, feedbackUi)
            DiagnosticsSection(state, diagnosticUi)
            PrivacySection()
        }
        AboutFooter()
    }
}

private fun startupStationSummary(state: MainUiState): String {
    val preferences = state.stationPreferences
    if (preferences.startupMode == StartupStationMode.LastSelected) return "Resumes where you left off."
    val fixed = state.stations.firstOrNull { it.id == preferences.defaultStationId }
    return fixed?.let { "Always starts with ${it.name}." } ?: "Choose which station opens at startup."
}

/** A titled run of settings rows on one rounded surface, the way system settings group related items. */
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun AboutFooter() {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    Text(
        listOfNotNull("24Seven.FM Player", version).joinToString(" ") + "\nAn unofficial listener app for the 24Seven.FM stations",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
private fun CommunityNotificationSection(
    state: MainUiState,
    onSetChatMentionsEnabled: (StationId, Boolean) -> Unit,
    onSetForegroundChatMentionMonitorEnabled: (StationId, Boolean) -> Unit,
) {
    val station = state.selectedStation ?: return
    val enabled = state.communityNotifications.chatMentionsEnabled(station.id)
    val monitorEnabled = state.communityNotifications.foregroundMonitorEnabled(station.id)
    val eligible = state.auth?.status == AuthStatus.SignedIn && state.communitySafety.canViewCommunityContent
    val canChangeSetting = eligible || enabled
    val canChangeMonitor = (eligible && enabled) || monitorEnabled
    MoreDisclosure(
        title = "Community notifications",
        summary = if (enabled) "Chat mentions enabled for ${station.shortName}" else "Off for ${station.shortName}",
        icon = Icons.Default.Notifications,
        testTag = "more_community_notifications",
    ) {
        Card(Modifier.fillMaxWidth().testTag("community_notification_controls")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingSwitchRow(
                    title = "Mention notifications",
                    description = "Tells you when your ${station.shortName} name comes up in chat. Members you blocked are ignored.",
                    checked = enabled,
                    enabled = canChangeSetting,
                    toggleTag = "chat_mention_notifications_toggle",
                    toggleDescription = "Notify when my station name is mentioned",
                    onCheckedChange = { onSetChatMentionsEnabled(station.id, it) },
                )
                if (!eligible) {
                    Footnote(
                        if (enabled) {
                            "Paused until you sign in and show community content for this station. You can still turn it off."
                        } else {
                            "Sign in and show community content for this station to turn this on."
                        },
                    )
                }
                SettingSwitchRow(
                    title = "Keep watching when the app is closed",
                    description = "Checks ${station.shortName} chat about once a minute. Android shows an ongoing notification with a Stop action.",
                    checked = monitorEnabled,
                    enabled = canChangeMonitor,
                    toggleTag = "foreground_chat_mention_monitor_toggle",
                    toggleDescription = "Monitor chat mentions while the app is closed",
                    onCheckedChange = { onSetForegroundChatMentionMonitorEnabled(station.id, it) },
                )
                if (!enabled && eligible) Footnote("Turn on mention notifications first.")
                Footnote(
                    if (monitorEnabled) {
                        "Watching stops if you sign out or hide community content. Message text is never shown in notifications."
                    } else {
                        "With the app open, mentions are noticed while Chat refreshes. Message text is never shown in notifications."
                    },
                )
            }
        }
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
        summary = "Technical details you can preview, copy, or share with support.",
        icon = Icons.Default.BugReport,
        testTag = "more_diagnostics",
    ) {
        Card(Modifier.fillMaxWidth().testTag("diagnostics_card")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Footnote("A fixed list of technical details. No account details, messages, addresses, device identifiers, or logs.")
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
                    OutlinedButton(
                        onClick = { diagnosticUi.actions.onCopy(report) },
                        modifier = Modifier.weight(1f).testTag("diagnostics_copy"),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = { diagnosticUi.actions.onShare(report) },
                        modifier = Modifier.weight(1f).testTag("diagnostics_share"),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    state: MainUiState,
    diagnosticUi: DiagnosticUi,
    feedbackUi: FeedbackUi,
) {
    var categoryName by rememberSaveable { mutableStateOf(FeedbackCategory.Playback.name) }
    var description by rememberSaveable { mutableStateOf("") }
    var includeDiagnostics by rememberSaveable { mutableStateOf(false) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    val category = FeedbackCategory.entries.firstOrNull { it.name == categoryName }
        ?: FeedbackCategory.Other
    val diagnosticReport = remember(
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
        title = "Report a problem",
        summary = "Tell the Player team what went wrong. You review the email before it is sent.",
        icon = Icons.Default.Feedback,
        testTag = "more_feedback",
    ) {
        Card(Modifier.fillMaxWidth().testTag("feedback_card")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { categoryMenuOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_category")
                            .semantics { contentDescription = "Category: ${category.label}" },
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("About", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(category.label)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = categoryMenuOpen,
                        onDismissRequest = { categoryMenuOpen = false },
                    ) {
                        FeedbackCategory.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    categoryName = option.name
                                    categoryMenuOpen = false
                                },
                                modifier = Modifier.testTag("feedback_category_${option.name}"),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(1_000) },
                    modifier = Modifier.fillMaxWidth().testTag("feedback_description"),
                    label = { Text("What happened? (optional)") },
                    supportingText = { Text("Leave out passwords, codes, and private messages. ${description.length}/1,000") },
                    minLines = 3,
                    maxLines = 6,
                )
                SettingSwitchRow(
                    title = "Attach diagnostics",
                    description = "App and Android version, device model, station, and recent playback states. Never account data, messages, or logs.",
                    checked = includeDiagnostics,
                    toggleTag = "feedback_include_diagnostics",
                    toggleDescription = "Include privacy-safe diagnostics",
                    onCheckedChange = { includeDiagnostics = it },
                )
                Button(
                    onClick = {
                        feedbackUi.actions.onReviewEmail(
                            FeedbackSubmission(
                                category = category,
                                optionalDescription = description,
                            ),
                            diagnosticReport.takeIf { includeDiagnostics },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("feedback_review_email"),
                ) { Text("Review email draft") }
                Footnote("Opens a draft in your email app. Nothing is sent until you send it there, and the Player cannot confirm delivery.")
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
        icon = Icons.Default.Shield,
        testTag = "more_community_safety",
    ) {
        Card(Modifier.fillMaxWidth().testTag("community_safety_controls")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val canToggle = safety.ageGateStatus == AgeGateStatus.Adult && safety.hasAcceptedCurrentTerms
                val shown = canToggle && safety.communityContentVisible
                StatusTile(
                    icon = if (shown) Icons.Default.Visibility else if (canToggle) Icons.Default.VisibilityOff else Icons.Default.Shield,
                    tint = when {
                        shown -> MaterialTheme.colorScheme.primary
                        canToggle || safety.ageGateStatus == AgeGateStatus.Underage -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    headline = if (canToggle) "Community content" else status,
                    detail = when {
                        safety.ageGateStatus == AgeGateStatus.Underage ->
                            "Playback stays available. This adult network's chat and request credits are not."
                        safety.ageGateStatus == AgeGateStatus.NotCompleted ->
                            "Enter your date of birth to decide access. The date itself is not saved."
                        !safety.hasAcceptedCurrentTerms -> "Accept the Terms of Participation to see chat and request credits."
                        shown -> "Shown. Chat and request credits are visible."
                        else -> "Hidden. Chat and request credits are not shown."
                    },
                ) {
                    if (canToggle) {
                        Switch(
                            checked = safety.communityContentVisible,
                            onCheckedChange = { actions.onSetCommunityContentVisible(!safety.communityContentVisible) },
                            modifier = Modifier
                                .testTag("toggle_community_content")
                                .semantics { contentDescription = "Show community content" },
                        )
                    }
                }
                when (safety.ageGateStatus) {
                    AgeGateStatus.NotCompleted -> AgeScreenFields(state, actions.onSubmitAgeScreen)
                    AgeGateStatus.Underage -> Unit
                    AgeGateStatus.Adult -> if (safety.hasAcceptedCurrentTerms) {
                        TextButton(onClick = onReviewTerms, modifier = Modifier.testTag("open_terms_from_more")) {
                            Text("Terms of Participation")
                        }
                    } else {
                        Button(onClick = onReviewTerms, modifier = Modifier.testTag("open_terms_from_more")) {
                            Text("Review and accept terms")
                        }
                    }
                }
                val blocked = selectedStation?.let { station ->
                    safety.blockedUsers.filter { it.stationId == station.id }
                }.orEmpty()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Blocked on ${selectedStation?.shortName ?: "this station"}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (blocked.isNotEmpty()) {
                        Text(
                            blocked.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (blocked.isEmpty()) {
                    Text(
                        "Nobody is blocked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                Footnote("Blocks stay on this device and hide that member's chat messages and request credits.")
            }
        }
    }
}

/** A tinted tile for the one state a card exists to show, with room for the control that changes it. */
@Composable
private fun StatusTile(
    icon: ImageVector,
    tint: Color,
    headline: String,
    detail: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = tint.copy(alpha = 0.14f), modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

/** A setting that is on or off: what it does on the left, the switch on the right. */
@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    toggleTag: String,
    toggleDescription: String,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(toggleTag).semantics { contentDescription = toggleDescription },
        )
    }
}

/** One of a set of choices where exactly one applies. */
@Composable
private fun SettingChoiceRow(
    title: String,
    description: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Footnote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A More entry that opens a screen of its own. */
/** One settings row: an optional leading icon, a title, a one-line summary, and whatever sits at the end. */
@Composable
private fun MoreRow(
    title: String,
    summary: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            icon?.let {
                Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing()
        }
    }
}

@Composable
internal fun MoreLink(
    title: String,
    summary: String,
    testTag: String,
    icon: ImageVector? = null,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    onClick: () -> Unit,
) {
    MoreRow(title, summary, icon, Modifier.testTag(testTag), onClick) {
        Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A settings row that opens in place; its content sits directly under it on the same surface. */
@Composable
internal fun MoreDisclosure(
    title: String,
    summary: String,
    testTag: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(testTag) { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "disclosure")
    MoreRow(
        title,
        summary,
        icon,
        Modifier
            .testTag(testTag)
            .semantics { contentDescription = "$title, ${if (expanded) "expanded" else "collapsed"}" },
        onClick = { expanded = !expanded },
    ) {
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(chevron),
        )
    }
    if (expanded) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
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
    Column(
        Modifier.fillMaxWidth().testTag("secondary_content_directory"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        station.secondaryPages.forEach { page ->
            val opensEmail = page.kind == StationPageKind.Contact
            MoreRow(
                title = page.title,
                summary = if (opensEmail) {
                    "${page.description.trimEnd().trimEnd('.')}. Opens a reviewed draft in your email app."
                } else {
                    page.description
                },
                icon = if (opensEmail) Icons.Default.Email else Icons.AutoMirrored.Filled.OpenInNew,
                modifier = Modifier
                    .testTag("secondary_content_${page.kind.name.lowercase()}")
                    .semantics {
                        contentDescription = if (opensEmail) {
                            "Email ${page.title} for ${station.name}"
                        } else {
                            "Open ${page.title} for ${station.name} in browser"
                        }
                    },
                onClick = { onOpenStationPage(page) },
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val current = state.selectedStation
    val resumes = preferences.startupMode == StartupStationMode.LastSelected

    if (showTitle) Text("Device preferences", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth().testTag("device_station_preferences")) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingChoiceRow(
                title = "Resume where I left off",
                description = lastStation?.let { "Last played: ${it.name}" } ?: "The last station selected on this device",
                selected = resumes,
                modifier = Modifier.testTag("startup_use_last_station"),
                onClick = onUseLastStationAtStartup,
            )
            // A startup station other than the one on screen stays listed, so choosing the current one is a visible change.
            if (!resumes && fixedStation != null && fixedStation.id != current?.id) {
                SettingChoiceRow(
                    title = "Always start with ${fixedStation.name}",
                    description = null,
                    selected = true,
                    onClick = {},
                )
            }
            SettingChoiceRow(
                title = "Always start with ${current?.name ?: "the selected station"}",
                description = "The station selected now",
                selected = !resumes && fixedStation != null && fixedStation.id == current?.id,
                enabled = current != null,
                modifier = Modifier.testTag("startup_use_current_station"),
                onClick = { current?.id?.let(onSetStartupStation) },
            )
            if (!resumes && fixedStation == null) {
                Footnote("Your saved startup station is no longer available, so the Player picks one for you.")
            }
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

    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    val membershipLabel = membershipLabel(activity?.membershipTier, activity?.rankTitle)
    val readinessLabel = when (activity?.requestReadiness) {
        RequestReadiness.Ready -> "Ready to request"
        RequestReadiness.Waiting -> activity.waitMinutes?.let { if (it == 1) "Wait 1 minute" else "Wait $it minutes" }
            ?: "Request cooldown active"
        RequestReadiness.Unknown, null -> "Not reported by station"
    }
    var showAllRequests by rememberSaveable(station.id.value) { mutableStateOf(false) }

    if (showTitle) Text("Request activity", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth().testTag("listener_activity_card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val loaded = state.auth?.status == AuthStatus.SignedIn &&
                activity?.status == ListenerActivityLoadStatus.Ready
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (loaded) {
                        MembershipLine(activity?.membershipTier, activity?.rankTitle, membershipLabel, station.id)
                    } else {
                        Text(
                            "As reported by ${station.shortName}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
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
                    "Sign in to ${station.shortName} above to see your request activity.",
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
                    RequestClockTile(activity.requestReadiness, activity.waitMinutes, readinessLabel)
                    activity.queuedRequestWaitSeconds?.let { seconds ->
                        ListenerStatusRow("Your queued request", queuedRequestLabel(seconds))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Recent requests",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        if (activity.recentRequests.isNotEmpty()) {
                            Text(
                                activity.recentRequests.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (activity.recentRequests.isEmpty()) {
                        Text(
                            "No recent requests were reported by this station.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val shown = if (showAllRequests) {
                            activity.recentRequests
                        } else {
                            activity.recentRequests.take(COLLAPSED_REQUEST_HISTORY_ROWS)
                        }
                        shown.forEach { request -> RequestHistoryRow(request) }
                        if (activity.recentRequests.size > COLLAPSED_REQUEST_HISTORY_ROWS) {
                            TextButton(
                                onClick = { showAllRequests = !showAllRequests },
                                modifier = Modifier.testTag("request_history_toggle"),
                            ) {
                                Text(if (showAllRequests) "Show fewer" else "Show all ${activity.recentRequests.size}")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The member's tier as a badge in the station's colour, then their rank as the station words it. */
@Composable
private fun MembershipLine(tier: MembershipTier?, rankTitle: String?, description: String, stationId: StationId) {
    val accent = stationPalette(stationId).themedAccent()
    val badge = when (tier) {
        MembershipTier.Vip -> "VIP"
        MembershipTier.Rip -> "RIP"
        MembershipTier.Standard -> "Member"
        MembershipTier.Unknown, null -> null
    }
    Row(
        Modifier.clearAndSetSemantics { contentDescription = "Membership: $description" },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        badge?.let { label ->
            Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.18f), contentColor = accent) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        val rank = rankTitle?.takeIf(String::isNotBlank)
        Text(
            rank ?: if (badge == null) description else "",
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The request clock as the one thing to read at a glance: whether a request can be sent now, or how long is left. */
@Composable
private fun RequestClockTile(readiness: RequestReadiness, waitMinutes: Int?, description: String) {
    val tint = when (readiness) {
        RequestReadiness.Ready -> requestAvailableGreen()
        RequestReadiness.Waiting -> MaterialTheme.colorScheme.secondary
        RequestReadiness.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (readiness) {
        RequestReadiness.Ready -> Icons.Default.CheckCircle
        RequestReadiness.Waiting -> Icons.Default.Schedule
        RequestReadiness.Unknown -> Icons.Default.HelpOutline
    }
    val headline = when (readiness) {
        RequestReadiness.Ready -> "Ready to request"
        RequestReadiness.Waiting -> waitMinutes?.let { if (it == 1) "Next request in 1 minute" else "Next request in $it minutes" }
            ?: "Request clock running"
        RequestReadiness.Unknown -> "Request clock unavailable"
    }
    val detail = when (readiness) {
        RequestReadiness.Ready -> "You can send a request now."
        RequestReadiness.Waiting -> "Your request clock is counting down."
        RequestReadiness.Unknown -> "The station did not report it."
    }
    StatusTile(
        icon = icon,
        tint = tint,
        headline = headline,
        detail = detail,
        modifier = Modifier
            .testTag("request_clock")
            .clearAndSetSemantics { contentDescription = "Request status: $description" },
    )
}

@Composable
private fun RequestHistoryRow(request: RequestHistoryEntry) {
    val requestedAt = requestTimeLabel(request.requestedAtLabel)
    val (title, artist) = splitTrackSummary(request.trackSummary)
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("request_history_${request.position}")
            .opensAlbum(request.albumId, request.albumTitle, request.artworkUrl)
            .semantics(mergeDescendants = true) {
                contentDescription = "Request ${request.position}: ${request.trackSummary}; $requestedAt"
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (request.artworkUrl.isNullOrBlank()) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AsyncImage(
                    model = crossfadingImage(request.artworkUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            artist?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                listOfNotNull(request.albumTitle, requestedAt).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The station writes a request as "Track — Artist"; the track may itself contain a dash, so the last one divides. */
internal fun splitTrackSummary(summary: String): Pair<String, String?> {
    val divider = summary.lastIndexOf(TRACK_SUMMARY_DIVIDER)
    if (divider <= 0) return summary to null
    val artist = summary.substring(divider + TRACK_SUMMARY_DIVIDER.length).trim()
    return summary.substring(0, divider).trim() to artist.ifEmpty { null }
}

private const val TRACK_SUMMARY_DIVIDER = " \u2014 "

@Composable
private fun ListenerStatusRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label: $value" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

private const val COLLAPSED_REQUEST_HISTORY_ROWS = 10

/** "VIP member · Admiral (Administrator)": the membership the station reports, then the member's rank when it has one. */
internal fun membershipLabel(tier: MembershipTier?, rankTitle: String?): String {
    val membership = when (tier) {
        MembershipTier.Standard -> "Standard member"
        MembershipTier.Vip -> "VIP member"
        MembershipTier.Rip -> "RIP member"
        MembershipTier.Unknown, null -> null
    }
    return listOfNotNull(membership, rankTitle?.takeIf(String::isNotBlank)).joinToString(" · ")
        .ifEmpty { "Not reported by station" }
}

/** How long until the listener's earliest queued request should start, as the station estimates it. */
internal fun queuedRequestLabel(seconds: Int): String {
    val minutes = (seconds + 30) / 60
    return when {
        seconds < 60 -> "Due shortly"
        minutes < 60 -> if (minutes == 1) "Plays in about 1 minute" else "Plays in about $minutes minutes"
        else -> "Plays in about ${minutes / 60} hr ${minutes % 60} min"
    }
}

/**
 * "Oct 2 · 4:16 PM" for the station's "2026-10-02 16:16:02", with the year only when it is not this one; anything
 * else is shown as the station wrote it.
 */
internal fun requestTimeLabel(raw: String, currentYear: Int = java.time.Year.now().value): String = runCatching {
    val time = java.time.LocalDateTime.parse(raw.trim(), STATION_TIMESTAMP)
    time.format(if (time.year == currentYear) REQUEST_TIME_THIS_YEAR else REQUEST_TIME_LABEL)
}.getOrDefault(raw)

private val STATION_TIMESTAMP = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
private val REQUEST_TIME_LABEL = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", java.util.Locale.US)
private val REQUEST_TIME_THIS_YEAR = java.time.format.DateTimeFormatter.ofPattern("MMM d · h:mm a", java.util.Locale.US)

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
    var username by rememberSaveable(station.id.value) { mutableStateOf("") }
    // The password is deliberately not saved: saved state can be written to disk.
    var password by remember(station.id) { mutableStateOf("") }
    var passwordVisible by remember(station.id) { mutableStateOf(false) }
    var securityCode by rememberSaveable(station.id.value) { mutableStateOf("") }
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
                    ) { Text("Sign out") }
                }
                AuthStatus.LoadingChallenge, AuthStatus.SigningIn -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(if (auth.status == AuthStatus.SigningIn) "Signing in…" else "Loading sign-in…")
                    }
                }
                AuthStatus.Unavailable -> {
                    Button(
                        onClick = { onRefresh(station.id) },
                        modifier = Modifier.testTag("account_load_sign_in_${station.id.value}"),
                    ) { Text("Sign in") }
                }
                AuthStatus.Expired -> {
                    Text(
                        auth.errorMessage ?: "Your saved station session expired. Sign in again.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = { onRefresh(station.id) },
                        modifier = Modifier.testTag("account_sign_in_again_${station.id.value}"),
                    ) { Text("Sign in again") }
                }
                AuthStatus.SignedOut, AuthStatus.Error -> {
                    auth.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (auth.challengeImageUrl == null && auth.antiSpamPrompt == null) {
                        Button(
                            onClick = { onRefresh(station.id) },
                            modifier = Modifier.testTag("account_retry_sign_in_${station.id.value}"),
                        ) { Text("Try again") }
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.testTag("account_toggle_password_${station.id.value}"),
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) {
                                            "Hide password for ${station.shortName}"
                                        } else {
                                            "Show password for ${station.shortName}"
                                        },
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_password_${station.id.value}")
                                .semantics { contentDescription = "Password for ${station.name}" },
                        )
                        auth.challengeImageUrl?.let { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "${station.name} security code image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(112.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                            )
                        }
                        auth.antiSpamPrompt?.let { prompt ->
                            Text(prompt, fontWeight = FontWeight.Medium)
                        }
                        OutlinedTextField(
                            securityCode,
                            { securityCode = it.take(MAX_ANTI_SPAM_ANSWER_LENGTH) },
                            label = { Text(if (auth.antiSpamPrompt == null) "Security code" else "Anti-spam answer") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_security_code_${station.id.value}")
                                .semantics {
                                    contentDescription = if (auth.antiSpamPrompt == null) {
                                        "Security code for ${station.name}"
                                    } else {
                                        "Anti-spam answer for ${station.name}"
                                    }
                                },
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
                            TextButton(onClick = { onRefresh(station.id) }) { Text("New check") }
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_ANTI_SPAM_ANSWER_LENGTH = 64

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
                    "Selected station",
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
    showTitle: Boolean = true,
) {
    val requests = state.requests
    var query by rememberSaveable(state.selectedStation?.id?.value) { mutableStateOf("") }
    var field by rememberSaveable(state.selectedStation?.id?.value) { mutableStateOf(RequestSearchField.Title) }
    var trackSortOrder by rememberSaveable(state.selectedStation?.id?.value) { mutableStateOf(TrackSortOrder.LibraryOrder) }
    var trackSortMenuOpen by remember { mutableStateOf(false) }
    val signedIn = state.auth?.status == AuthStatus.SignedIn

    if (showTitle) Text("Song requests", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.selectedStation?.capabilities?.supportsRequests != true) {
                Text("Song requests have not been verified for this station.")
                return@Column
            }
            val busy = requests?.status == SongRequestLoadStatus.Loading ||
                requests?.status == SongRequestLoadStatus.Submitting
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(100) },
                    label = { Text("Search the library") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (!busy) onSearch(query, field) }),
                    modifier = Modifier.weight(1f).testTag("library_search_query"),
                )
                FilledIconButton(
                    onClick = { onSearch(query, field) },
                    enabled = !busy,
                    modifier = Modifier.padding(top = 8.dp).size(52.dp).testTag("library_search_submit"),
                ) { Icon(Icons.Default.Search, contentDescription = "Search") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RequestSearchField.entries.forEach { option ->
                    FilterChip(
                        selected = field == option,
                        onClick = { field = option },
                        label = { Text(option.name) },
                        modifier = Modifier.testTag("library_search_field_${option.name.lowercase()}"),
                    )
                }
            }
            Text("Or let the station pick", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onSuggest(RequestSuggestionMode.Random) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).testTag("suggest_random_track"),
                ) { Text("Random track") }
                OutlinedButton(
                    onClick = { onSuggest(RequestSuggestionMode.LeastPlayed) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).testTag("suggest_least_played_track"),
                ) { Text("Least played") }
            }
            Footnote("Nothing is sent until you review and confirm one track.")

            if (busy) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(if (requests?.status == SongRequestLoadStatus.Submitting) "Sending one request…" else "Loading station library…")
                }
            }
            requests?.errorMessage?.let { error ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(
                        onClick = onCancelRequest,
                        modifier = Modifier.testTag("dismiss_song_request_result"),
                    ) { Text("Dismiss") }
                }
            }
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
                        Row(
                            Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(result.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                listOfNotNull(result.subtitle, result.year).takeIf { it.isNotEmpty() }?.let { details ->
                                    Text(
                                        details.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    RequestableTrackRow(
                        track = track,
                        canRequest = requests.status == SongRequestLoadStatus.Ready,
                        onPrepareRequest = onPrepareRequest,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestResultDialog(
    state: MainUiState,
    onDismiss: () -> Unit,
) {
    val requests = state.requests ?: return
    val error = requests.errorMessage
    val message = error ?: requests.notice ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request status") },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_song_request_result_dialog"),
            ) { Text("Dismiss") }
        },
    )
}
