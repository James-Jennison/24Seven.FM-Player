package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.MemberFavoritesStatus
import com.codeframe78.twentyfourseven.player.domain.MemberProfile
import com.codeframe78.twentyfourseven.player.domain.MemberProfileStatus
import com.codeframe78.twentyfourseven.player.domain.PLAYED_HISTORY_ARCHIVE_DAYS
import com.codeframe78.twentyfourseven.player.domain.PLAYED_HISTORY_BLOCK_HOURS
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryEntry
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryState
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryStatus
import com.codeframe78.twentyfourseven.player.domain.STATION_CLOCK_ZONE
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus
import com.codeframe78.twentyfourseven.player.domain.StationNewsStatus
import com.codeframe78.twentyfourseven.player.domain.StationNewsStory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
internal data class StationExtrasActions(
    val onOpenProfile: (String) -> Unit = {},
    val onCloseProfile: () -> Unit = {},
    val onOpenMemberFavorites: () -> Unit = {},
    val onCloseMemberFavorites: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onLoadHistory: (LocalDate, Int) -> Unit = { _, _ -> },
    val onCloseHistory: () -> Unit = {},
    val onRefreshNews: () -> Unit = {},
)

/** Lets any screen open the history archive or news without threading callbacks through every layout. */
internal val LocalStationExtrasActions = staticCompositionLocalOf { StationExtrasActions() }

/** Opens a member's profile card from wherever that member's name is shown; null where profiles are unavailable. */
internal val LocalMemberProfileOpener = staticCompositionLocalOf<((String) -> Unit)?> { null }

/** Makes a member's name open that member's profile card. It stays plain text where profiles are unavailable. */
@Composable
internal fun Modifier.opensMemberProfile(name: String?): Modifier {
    val open = LocalMemberProfileOpener.current
    val member = name?.takeIf(String::isNotBlank)
    return if (open == null || member == null) {
        this
    } else {
        clickable(onClickLabel = "View profile", role = Role.Button) { open(member) }
    }
}

@Composable
internal fun MemberProfileDialog(state: MainUiState, actions: StationExtrasActions, onSendMessage: (String) -> Unit) {
    val card = state.extras?.profile ?: return
    if (card.status == MemberProfileStatus.Closed) return
    val station = state.selectedStation ?: return
    val profile = card.profile
    val canMessage = profile != null &&
        station.capabilities.supportsPrivateMessageSending &&
        state.auth?.status == AuthStatus.SignedIn &&
        state.communitySafety.canContributeCommunityContent &&
        !profile.username.equals(state.auth.displayName, ignoreCase = true)
    val signedIn = state.auth?.status == AuthStatus.SignedIn
    AlertDialog(
        onDismissRequest = actions.onCloseProfile,
        modifier = Modifier.testTag("member_profile_dialog"),
        title = { Text(profile?.username ?: card.requestedName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            when (card.status) {
                MemberProfileStatus.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("Loading profile…")
                }
                MemberProfileStatus.NotFound -> Text("${station.shortName} has no member profile under that name.")
                MemberProfileStatus.Ready -> profile?.let {
                    MemberProfileBody(
                        profile = it,
                        stationName = station.shortName,
                        showsFavorites = station.capabilities.supportsMemberFavorites && it.memberNumber != null,
                        signedIn = signedIn,
                        onOpenFavorites = actions.onOpenMemberFavorites,
                    )
                }
                else -> Text("This profile could not be loaded right now.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            if (canMessage && profile != null) {
                Button(
                    onClick = {
                        actions.onCloseProfile()
                        onSendMessage(profile.username)
                    },
                    modifier = Modifier.testTag("member_profile_message"),
                ) { Text("Send message") }
            }
        },
        dismissButton = { TextButton(onClick = actions.onCloseProfile) { Text("Close") } },
    )
}

@Composable
private fun MemberProfileBody(
    profile: MemberProfile,
    stationName: String,
    showsFavorites: Boolean,
    signedIn: Boolean,
    onOpenFavorites: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    profile.username.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                profile.avatarUrl?.let { avatar ->
                    AsyncImage(
                        model = avatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                profile.rankTitle?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                Text(
                    if (profile.isOnline) "Online now" else "Offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (profile.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        memberProfileLines(profile, stationName).forEach { line -> Text(line, style = MaterialTheme.typography.bodyMedium) }
        profile.publicFavoritesBadge?.let { badge ->
            // The badge opens the list it stands for. The stations show that list to signed-in members only.
            AssistChip(
                onClick = onOpenFavorites,
                enabled = showsFavorites && signedIn,
                label = { Text(badge) },
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, Modifier.size(18.dp)) },
                modifier = Modifier
                    .testTag("member_profile_favorites")
                    .semantics { contentDescription = "$badge, view ${profile.username}'s favorites" },
            )
            if (showsFavorites && !signedIn) {
                Text(
                    "Sign in to $stationName to see ${profile.username}'s favorites.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun MemberFavoritesDialog(
    state: MainUiState,
    actions: StationExtrasActions,
    onPrepareRequest: (FavoriteTrack) -> Unit,
) {
    val favorites = state.extras?.memberFavorites ?: return
    if (favorites.status == MemberFavoritesStatus.Closed) return
    val station = state.selectedStation ?: return
    var filter by rememberSaveable(favorites.memberName) { mutableStateOf("") }
    val visibleTracks = remember(favorites.tracks, filter) {
        val query = filter.trim()
        if (query.isBlank()) favorites.tracks else favorites.tracks.filter { it.matchesFilter(query) }
    }
    Dialog(
        onDismissRequest = actions.onCloseMemberFavorites,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize().testTag("member_favorites_dialog")) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${favorites.memberName}'s favorites",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            station.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = actions.onCloseMemberFavorites) {
                        Icon(Icons.Default.Close, contentDescription = "Close ${favorites.memberName}'s favorites")
                    }
                }
                when (favorites.status) {
                    MemberFavoritesStatus.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    MemberFavoritesStatus.SignInRequired -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sign in to ${station.shortName} again to see this list.")
                    }
                    MemberFavoritesStatus.Ready -> LazyColumn(
                        Modifier.fillMaxSize().testTag("member_favorites_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            OutlinedTextField(
                                value = filter,
                                onValueChange = { filter = it.take(100) },
                                label = { Text("Filter favorites") },
                                supportingText = {
                                    Text(
                                        if (filter.isBlank()) {
                                            "${visibleTracks.size} tracks"
                                        } else {
                                            "${visibleTracks.size} of ${favorites.tracks.size} tracks"
                                        },
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (visibleTracks.isEmpty()) {
                            item {
                                Text(
                                    if (filter.isBlank()) "This list has no tracks." else "No favorites match this filter.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(
                            items = visibleTracks,
                            key = { track -> "${track.position}-${track.requestTrack?.songId ?: track.title}" },
                        ) { track ->
                            FavoriteTrackCard(
                                track = track,
                                canRequest = station.capabilities.supportsRequests &&
                                    state.requests?.status != SongRequestLoadStatus.Submitting,
                                onPrepareRequest = onPrepareRequest,
                            )
                        }
                    }
                    else -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("This list could not be loaded right now.", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = actions.onOpenMemberFavorites) { Text("Try again") }
                    }
                }
            }
        }
    }
}

/**
 * The plain facts of a profile card, one per line, leaving out whatever the station did not supply. The public
 * favorites badge is left out because the card shows it as a button of its own.
 */
internal fun memberProfileLines(profile: MemberProfile, stationName: String): List<String> = listOfNotNull(
    profile.memberSince?.let { "$stationName member since $it" },
    profile.location?.let { "Location: $it" },
    profile.membership?.let { "Membership: $it" },
    (profile.badges - listOfNotNull(profile.publicFavoritesBadge).toSet())
        .takeIf(List<String>::isNotEmpty)?.joinToString(prefix = "Badges: "),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayedHistoryDialog(state: MainUiState, actions: StationExtrasActions) {
    val history = state.extras?.history ?: return
    val date = history.date ?: return
    if (history.status == PlayedHistoryStatus.Closed) return
    val station = state.selectedStation ?: return
    var choosingDate by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now(STATION_CLOCK_ZONE)
    val earliest = today.minusDays(PLAYED_HISTORY_ARCHIVE_DAYS)
    val previous = previousHistoryBlock(date, history.startHour).takeIf { it.first >= earliest }
    val next = nextHistoryBlock(date, history.startHour).takeIf { it.first <= today }
    Dialog(onDismissRequest = actions.onCloseHistory, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("played_history_dialog")) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${station.shortName} played history", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Times are on the station's clock (US Eastern).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = actions.onCloseHistory) {
                        Icon(Icons.Default.Close, contentDescription = "Close played history")
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { previous?.let { actions.onLoadHistory(it.first, it.second) } },
                        enabled = previous != null,
                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Earlier two hours") }
                    TextButton(
                        onClick = { choosingDate = true },
                        modifier = Modifier.weight(1f).testTag("played_history_date"),
                    ) {
                        Text(
                            "${date.format(HISTORY_DATE)} · ${historyBlockLabel(history.startHour)}",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = { next?.let { actions.onLoadHistory(it.first, it.second) } },
                        enabled = next != null,
                    ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Later two hours") }
                }
                PlayedHistoryList(history, onRetry = { actions.onLoadHistory(date, history.startHour) })
            }
        }
    }
    if (choosingDate) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return day in earliest..today
                }

                override fun isSelectableYear(year: Int): Boolean = year in earliest.year..today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { choosingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        choosingDate = false
                        picker.selectedDateMillis?.let { millis ->
                            val chosen = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            actions.onLoadHistory(chosen, history.startHour)
                        }
                    },
                ) { Text("Show") }
            },
            dismissButton = { TextButton(onClick = { choosingDate = false }) { Text("Cancel") } },
        ) { DatePicker(picker) }
    }
}

@Composable
private fun PlayedHistoryList(history: PlayedHistoryState, onRetry: () -> Unit) {
    when {
        history.status == PlayedHistoryStatus.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        history.status == PlayedHistoryStatus.Error -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("The history could not be loaded right now.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Try again") }
        }
        history.entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing was played in these two hours.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            Modifier.fillMaxSize().testTag("played_history_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(history.entries) { entry -> PlayedHistoryRow(entry) }
        }
    }
}

@Composable
private fun PlayedHistoryRow(entry: PlayedHistoryEntry) {
    val openProfile = LocalMemberProfileOpener.current
    Card(
        Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .opensAlbum(entry.albumId, entry.albumTitle, entry.artworkUrl),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                historyTimeLabel(entry.playedAtLabel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            entry.artworkUrl?.let { artwork ->
                AsyncImage(
                    model = crossfadingImage(artwork),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                listOfNotNull(entry.artistName, entry.albumTitle).takeIf(List<String>::isNotEmpty)?.let { details ->
                    Text(
                        details.joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                entry.requesterName?.let { requester ->
                    val label = "Requested by $requester"
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (openProfile == null) {
                            Modifier
                        } else {
                            // Padding inside the clickable area keeps the name a comfortable touch target.
                            Modifier
                                .clickable(onClickLabel = "View profile") { openProfile(requester) }
                                .padding(vertical = 10.dp)
                                .semantics { contentDescription = "$label, view profile" }
                        },
                    )
                }
                entry.requestMessage?.let { message ->
                    Text(
                        "“$message”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            entry.lengthLabel?.let { length ->
                Spacer(Modifier.width(12.dp))
                Text(length, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

internal fun previousHistoryBlock(date: LocalDate, startHour: Int): Pair<LocalDate, Int> =
    if (startHour >= PLAYED_HISTORY_BLOCK_HOURS) {
        date to startHour - PLAYED_HISTORY_BLOCK_HOURS
    } else {
        date.minusDays(1) to 24 - PLAYED_HISTORY_BLOCK_HOURS
    }

internal fun nextHistoryBlock(date: LocalDate, startHour: Int): Pair<LocalDate, Int> =
    if (startHour + PLAYED_HISTORY_BLOCK_HOURS < 24) {
        date to startHour + PLAYED_HISTORY_BLOCK_HOURS
    } else {
        date.plusDays(1) to 0
    }

/** "1:50 PM" for the station's "13:50:12", so a row reads like the block label above it. */
internal fun historyTimeLabel(playedAt: String): String {
    val parts = playedAt.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 } ?: return playedAt
    val minute = parts.getOrNull(1)?.takeIf { it.length == 2 } ?: return playedAt
    return "${if (hour % 12 == 0) 12 else hour % 12}:$minute ${if (hour < 12) "AM" else "PM"}"
}

/** "2:00 – 4:00 AM" for the two hours starting at [startHour]. */
internal fun historyBlockLabel(startHour: Int): String {
    val end = (startHour + PLAYED_HISTORY_BLOCK_HOURS) % 24
    fun hour(value: Int) = if (value % 12 == 0) 12 else value % 12
    fun meridiem(value: Int) = if (value < 12) "AM" else "PM"
    val startMeridiem = meridiem(startHour)
    val endMeridiem = meridiem(end)
    return if (startMeridiem == endMeridiem) {
        "${hour(startHour)}:00 – ${hour(end)}:00 $endMeridiem"
    } else {
        "${hour(startHour)}:00 $startMeridiem – ${hour(end)}:00 $endMeridiem"
    }
}

@Composable
internal fun StationNewsSection(state: MainUiState, actions: StationExtrasActions) {
    val station = state.selectedStation ?: return
    val news = state.extras?.news ?: return
    LaunchedEffect(station.id) {
        if (news.status == StationNewsStatus.Idle) actions.onRefreshNews()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("station_news")) {
        when {
            news.stories.isEmpty() && news.status == StationNewsStatus.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${station.shortName} news could not be loaded right now.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onRefreshNews) { Text("Try again") }
            }
            news.stories.isEmpty() && news.status == StationNewsStatus.Ready ->
                Text("${station.shortName} has no news to show.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            news.stories.isEmpty() -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text("Loading news…")
            }
            else -> {
                news.stories.forEach { story -> StationNewsCard(story) }
                IconButton(
                    onClick = actions.onRefreshNews,
                    enabled = news.status != StationNewsStatus.Loading,
                ) { Icon(Icons.Default.Refresh, contentDescription = "Refresh ${station.shortName} news") }
            }
        }
    }
}

@Composable
private fun StationNewsCard(story: StationNewsStory) {
    var expanded by rememberSaveable(story.id) { mutableStateOf(false) }
    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().testTag("station_news_story")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(story.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (story.publishedLabel != null || story.author != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    story.publishedLabel?.let { published ->
                        Text(
                            if (story.author == null) published else "$published · ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    story.author?.let { author ->
                        Text(
                            "by $author",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.opensMemberProfile(author).padding(vertical = 6.dp),
                        )
                    }
                }
            }
            if (story.coverUrls.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    story.coverUrls.forEach { cover ->
                        AsyncImage(
                            model = crossfadingImage(cover),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .opensAlbum(albumIdFromCoverUrl(cover), null, cover),
                        )
                    }
                }
            }
            Text(
                story.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 0.dp),
            )
            Text(
                if (expanded) "Show less" else "Read more",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private val HISTORY_DATE = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
