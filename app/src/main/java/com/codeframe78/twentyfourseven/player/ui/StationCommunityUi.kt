package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.codeframe78.twentyfourseven.player.domain.ALBUM_REVIEW_RATINGS
import com.codeframe78.twentyfourseven.player.domain.AlbumReview
import com.codeframe78.twentyfourseven.player.domain.AlbumReviewSendStatus
import com.codeframe78.twentyfourseven.player.domain.AlbumReviewsStatus
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.CalendarDay
import com.codeframe78.twentyfourseven.player.domain.CalendarEntryKind
import com.codeframe78.twentyfourseven.player.domain.CalendarStatus
import com.codeframe78.twentyfourseven.player.domain.EditableProfile
import com.codeframe78.twentyfourseven.player.domain.FavoriteChangeStatus
import com.codeframe78.twentyfourseven.player.domain.MAX_ALBUM_REVIEW_BODY_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.MAX_ALBUM_REVIEW_TITLE_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.MemberListSort
import com.codeframe78.twentyfourseven.player.domain.MemberSummary
import com.codeframe78.twentyfourseven.player.domain.MembersStatus
import com.codeframe78.twentyfourseven.player.domain.ProfileEditStatus
import com.codeframe78.twentyfourseven.player.domain.RecentlyAddedStatus
import com.codeframe78.twentyfourseven.player.domain.STATION_CLOCK_ZONE
import java.time.LocalDate
import java.time.MonthDay

/** A note under the favorites list when a move or removal did not go through. */
@Composable
internal fun FavoriteChangeNotice(state: MainUiState) {
    val change = state.favorites?.change ?: return
    val message = when (change.status) {
        FavoriteChangeStatus.SignInRequired -> "Sign in to the station again to change your favorites."
        FavoriteChangeStatus.Failed -> "That change could not be made. The list shows what the station has now."
        else -> return
    }
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

/** The reviews on the album page, with the write-a-review control the station offers a signed-in member. */
@Composable
internal fun AlbumReviewsSection(state: MainUiState, albumId: String, actions: StationExtrasActions) {
    val station = state.selectedStation ?: return
    if (!station.capabilities.supportsAlbumReviews) return
    val reviews = state.extras?.albumReviews?.takeIf { it.albumId == albumId } ?: return
    val signedIn = state.auth?.status == AuthStatus.SignedIn
    val canContribute = state.communitySafety.canContributeCommunityContent
    var composing by rememberSaveable(albumId) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp).testTag("album_reviews"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Member reviews", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (reviews.status == AlbumReviewsStatus.Ready && reviews.canWrite && signedIn) {
                TextButton(
                    onClick = { composing = true },
                    enabled = reviews.sendStatus != AlbumReviewSendStatus.Sending,
                    modifier = Modifier.testTag("album_write_review"),
                ) { Text("Write a review") }
            }
        }
        when (reviews.status) {
            AlbumReviewsStatus.Loading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(Modifier.size(20.dp))
                Text("Loading reviews…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AlbumReviewsStatus.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Reviews could not be loaded right now.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onReloadAlbumReviews) { Text("Try again") }
            }
            AlbumReviewsStatus.Ready -> {
                if (reviews.reviews.isEmpty()) {
                    Text("No member has reviewed this album yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                reviews.reviews.forEach { review -> AlbumReviewCard(review) }
                if (reviews.canWrite && !signedIn) {
                    Text(
                        "Sign in to ${station.shortName} from the More tab to write a review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AlbumReviewsStatus.Closed -> Unit
        }
        reviews.sendMessage?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = when (reviews.sendStatus) {
                    AlbumReviewSendStatus.Sent -> MaterialTheme.colorScheme.primary
                    AlbumReviewSendStatus.Unconfirmed -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.testTag("album_review_result"),
            )
        }
    }
    if (composing) {
        AlbumReviewComposer(
            stationName = station.shortName,
            canContribute = canContribute,
            sending = reviews.sendStatus == AlbumReviewSendStatus.Sending,
            onSend = { title, body, rating ->
                actions.onSubmitAlbumReview(title, body, rating)
                composing = false
            },
            onDismiss = { composing = false },
        )
    }
}

@Composable
private fun AlbumReviewCard(review: AlbumReview) {
    Card(Modifier.fillMaxWidth().testTag("album_review")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    review.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                review.rating?.let { rating ->
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Star, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "$rating / 5",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.semantics { contentDescription = "Rated $rating out of 5" },
                    )
                }
            }
            if (review.author != null || review.dateLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    review.author?.let { author ->
                        Text(
                            author,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.opensMemberProfile(author).padding(vertical = 4.dp),
                        )
                    }
                    review.dateLabel?.let { date ->
                        Text(
                            if (review.author == null) date else " · $date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(review.body, style = MaterialTheme.typography.bodyMedium)
            review.helpfulLabel?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Writes one review, then shows it once more before it is sent. */
@Composable
private fun AlbumReviewComposer(
    stationName: String,
    canContribute: Boolean,
    sending: Boolean,
    onSend: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableStateOf(ALBUM_REVIEW_RATINGS.first().value) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    val ready = title.isNotBlank() && body.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("album_review_composer"),
        title = { Text(if (reviewing) "Send this review?" else "Write a review") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!canContribute) {
                    Text(
                        "Turn on community content in More to write reviews.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (reviewing) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        ALBUM_REVIEW_RATINGS.first { it.value == rating }.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(body.trim(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "It is posted on $stationName under your member name and cannot be edited from the Player.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(MAX_ALBUM_REVIEW_TITLE_CHARACTERS) },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("album_review_title"),
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it.take(MAX_ALBUM_REVIEW_BODY_CHARACTERS) },
                        label = { Text("Your review") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("album_review_body"),
                    )
                    Text("Rating", style = MaterialTheme.typography.labelLarge)
                    RatingChips(selected = rating, onSelect = { rating = it })
                }
            }
        },
        confirmButton = {
            if (!canContribute) {
                TextButton(onClick = onDismiss) { Text("Close") }
            } else if (reviewing) {
                Button(
                    onClick = { onSend(title.trim(), body.trim(), rating) },
                    enabled = !sending,
                    modifier = Modifier.testTag("album_review_send"),
                ) { Text("Send review") }
            } else {
                Button(
                    onClick = { reviewing = true },
                    enabled = ready,
                    modifier = Modifier.testTag("album_review_next"),
                ) { Text("Review") }
            }
        },
        dismissButton = {
            if (reviewing) {
                TextButton(onClick = { reviewing = false }) { Text("Edit") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingChips(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        ALBUM_REVIEW_RATINGS.forEach { option ->
            FilterChip(
                selected = option.value == selected,
                onClick = { onSelect(option.value) },
                label = { Text(option.label.substringBefore(" - ")) },
                modifier = Modifier.semantics { contentDescription = option.label },
            )
        }
    }
}

/** The station's Recently Added page: each day's new albums, which open the album browser. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecentlyAddedSection(state: MainUiState, actions: StationExtrasActions) {
    val station = state.selectedStation ?: return
    val recent = state.extras?.recentlyAdded ?: return
    LaunchedEffect(station.id) {
        if (recent.status == RecentlyAddedStatus.Idle) actions.onRefreshRecentlyAdded()
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("recently_added")) {
        when {
            recent.batches.isEmpty() && recent.status == RecentlyAddedStatus.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recently added albums could not be loaded right now.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onRefreshRecentlyAdded) { Text("Try again") }
            }
            recent.batches.isEmpty() && recent.status == RecentlyAddedStatus.Ready ->
                Text("${station.shortName} lists no recent additions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            recent.batches.isEmpty() -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text("Loading recent additions…")
            }
            else -> {
                recent.batches.forEach { batch ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                batch.dateLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            batch.author?.let { author ->
                                Text(
                                    " · added by ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    author,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.opensMemberProfile(author).padding(vertical = 6.dp),
                                )
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            batch.albums.forEach { album ->
                                Column(
                                    Modifier
                                        .width(96.dp)
                                        .opensAlbum(album.albumId, album.title, album.coverUrl)
                                        .testTag("recently_added_album"),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .size(96.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (album.coverUrl == null) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            AsyncImage(
                                                model = crossfadingImage(album.coverUrl),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                    Text(
                                        album.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = actions.onRefreshRecentlyAdded,
                    enabled = recent.status != RecentlyAddedStatus.Loading,
                ) { Icon(Icons.Default.Refresh, contentDescription = "Refresh recently added albums") }
            }
        }
    }
}

/** The station's events and birthdays, from today onwards on the station's calendar. */
@Composable
internal fun CalendarSection(state: MainUiState, actions: StationExtrasActions) {
    val station = state.selectedStation ?: return
    val calendar = state.extras?.calendar ?: return
    LaunchedEffect(station.id) {
        if (calendar.status == CalendarStatus.Idle) actions.onRefreshCalendar()
    }
    val today = remember { MonthDay.from(LocalDate.now(STATION_CLOCK_ZONE)) }
    var shownDays by rememberSaveable(station.id.value) { mutableIntStateOf(INITIAL_CALENDAR_DAYS) }
    val ordered = remember(calendar.days, today) { calendar.days.startingAt(today) }
    val showsMembers = state.communitySafety.canViewCommunityContent
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("station_calendar")) {
        when {
            calendar.days.isEmpty() && calendar.status == CalendarStatus.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "The calendar could not be loaded right now.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onRefreshCalendar) { Text("Try again") }
            }
            calendar.days.isEmpty() && calendar.status == CalendarStatus.Ready ->
                Text("${station.shortName} has no events listed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            calendar.days.isEmpty() -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text("Loading the calendar…")
            }
            else -> {
                ordered.take(shownDays).forEach { day -> CalendarDayCard(day, isToday = day.day == today, showsMembers) }
                if (shownDays < ordered.size) {
                    OutlinedButton(
                        onClick = { shownDays += MORE_CALENDAR_DAYS },
                        modifier = Modifier.testTag("station_calendar_more"),
                    ) { Text("Show more days") }
                }
            }
        }
    }
}

/** The calendar in station order, beginning with today or the first listed day after it. */
internal fun List<CalendarDay>.startingAt(today: MonthDay): List<CalendarDay> {
    val sorted = sortedBy { it.day }
    val index = sorted.indexOfFirst { it.day >= today }.takeIf { it >= 0 } ?: 0
    return sorted.drop(index) + sorted.take(index)
}

@Composable
private fun CalendarDayCard(day: CalendarDay, isToday: Boolean, showsMembers: Boolean) {
    val entries = if (showsMembers) day.entries else day.entries.filter { it.kind != CalendarEntryKind.Member }
    if (entries.isEmpty()) return
    Card(Modifier.fillMaxWidth().testTag("station_calendar_day")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(day.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (isToday) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            entries.forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val icon = when (entry.kind) {
                        CalendarEntryKind.Event -> Icons.Default.Celebration
                        CalendarEntryKind.Composer -> Icons.Default.MusicNote
                        CalendarEntryKind.Member -> Icons.Default.Cake
                    }
                    Icon(icon, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (entry.kind == CalendarEntryKind.Member) {
                        Text(
                            entry.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.opensMemberProfile(entry.username).padding(vertical = 4.dp),
                        )
                    } else {
                        Text(
                            entry.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (entry.kind == CalendarEntryKind.Event) FontWeight.Medium else null,
                        )
                    }
                }
            }
        }
    }
}

/** The members list with the Online Now block above it, opened over the More tab. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MembersDialog(state: MainUiState, actions: StationExtrasActions) {
    val members = state.extras?.members ?: return
    if (members.status == MembersStatus.Closed) return
    val station = state.selectedStation ?: return
    var query by rememberSaveable(station.id.value) { mutableStateOf(members.query) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = actions.onCloseMembers,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize().testTag("members_dialog")) {
            Column(Modifier.readablePane().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Members", style = MaterialTheme.typography.titleLarge)
                        Text(station.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = actions.onCloseMembers) { Icon(Icons.Default.Close, contentDescription = "Close members") }
                }
                LazyColumn(
                    Modifier.fillMaxSize().testTag("members_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Online now", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            val counts = listOfNotNull(
                                members.visitors?.let { "$it listening" },
                                "${members.online.size} ${if (members.online.size == 1) "member" else "members"} online",
                                members.totalMembers?.let { "$it members overall" },
                            ).joinToString(" · ")
                            Text(counts, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (members.online.isEmpty() && members.status == MembersStatus.Ready) {
                                Text("No member is signed in right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                members.online.forEach { member ->
                                    val opener = LocalMemberProfileOpener.current
                                    AssistChip(
                                        onClick = { opener?.invoke(member.username) },
                                        enabled = opener != null,
                                        label = { Text(member.username) },
                                        leadingIcon = {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(OnlineGreen))
                                        },
                                        modifier = Modifier.semantics {
                                            contentDescription = listOfNotNull(member.username, member.countryName).joinToString(", ") + ", online"
                                        },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it.take(60) },
                            label = { Text("Find a member") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                TextButton(
                                    onClick = { actions.onSearchMembers(query, members.sort) },
                                    enabled = members.status != MembersStatus.Loading,
                                    modifier = Modifier.testTag("members_search"),
                                ) { Text("Search") }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            MemberListSort.entries.forEach { sort ->
                                FilterChip(
                                    selected = sort == members.sort,
                                    onClick = { if (sort != members.sort) actions.onSearchMembers(query, sort) },
                                    enabled = members.status != MembersStatus.Loading,
                                    label = { Text(sort.label) },
                                )
                            }
                        }
                    }
                    when {
                        members.status == MembersStatus.Error -> item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "The members list could not be loaded right now.",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = actions.onOpenMembers) { Text("Try again") }
                            }
                        }
                        members.members.isEmpty() && members.status == MembersStatus.Loading -> item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        members.members.isEmpty() -> item {
                            Text(
                                if (members.query.isBlank()) "No members were listed." else "No member matches \"${members.query}\".",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(members.members, key = { "${it.memberNumber ?: it.username}" }) { member -> MemberRow(member) }
                    if (members.nextStart != null || (members.members.isNotEmpty() && members.status == MembersStatus.Loading)) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (members.status == MembersStatus.Loading) {
                                    CircularProgressIndicator(Modifier.size(28.dp))
                                } else {
                                    OutlinedButton(
                                        onClick = actions.onLoadMoreMembers,
                                        modifier = Modifier.testTag("members_more"),
                                    ) { Text("Show more members") }
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
private fun MemberRow(member: MemberSummary) {
    Card(Modifier.fillMaxWidth().testTag("member_row")) {
        Row(
            Modifier
                .fillMaxWidth()
                .opensMemberProfile(member.username)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (member.isOnline) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(OnlineGreen)
                            .semantics { contentDescription = "Online now" },
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        member.username,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (member.isVip) {
                        Text(
                            "VIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                val details = listOfNotNull(
                    member.rankTitle,
                    listOfNotNull(member.location, member.countryName).joinToString(", ").takeIf(String::isNotBlank),
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val facts = listOfNotNull(
                    member.joinedLabel?.let { "Joined $it" },
                    member.posts?.let { if (it == 1) "1 post" else "$it posts" },
                ).joinToString(" · ")
                if (facts.isNotBlank()) {
                    Text(facts, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** The member's own station profile, edited field by field and sent back as the station's whole form. */
@Composable
internal fun ProfileEditDialog(state: MainUiState, actions: StationExtrasActions) {
    val edit = state.extras?.profileEdit ?: return
    if (edit.status == ProfileEditStatus.Closed) return
    val station = state.selectedStation ?: return
    val form = edit.form
    var draft by remember(form) { mutableStateOf(form?.profile ?: EditableProfile()) }
    var choosingFlag by rememberSaveable { mutableStateOf(false) }
    val busy = edit.status == ProfileEditStatus.Saving || edit.status == ProfileEditStatus.Loading
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!busy) actions.onCloseProfileEditor() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize().testTag("profile_edit_dialog")) {
            Column(Modifier.readablePane().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Edit profile", style = MaterialTheme.typography.titleLarge)
                        Text(station.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = actions.onCloseProfileEditor, enabled = !busy) {
                        Icon(Icons.Default.Close, contentDescription = "Close edit profile")
                    }
                }
                when {
                    edit.status == ProfileEditStatus.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    edit.status == ProfileEditStatus.SignInRequired -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sign in to ${station.shortName} again to edit your profile.")
                    }
                    form == null -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(edit.message ?: "Your profile could not be loaded right now.", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = actions.onOpenProfileEditor) { Text("Try again") }
                    }
                    else -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        edit.message?.let { message ->
                            Text(
                                message,
                                color = if (edit.status == ProfileEditStatus.Saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("profile_edit_message"),
                            )
                        }
                        Text(
                            "These details are shown on your ${station.shortName} profile. Email, password, and forum settings stay on the station's own page.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ProfileField("Real name", draft.realName, 60, "profile_edit_real_name") { draft = draft.copy(realName = it) }
                        ProfileField("Location", draft.location, 100, "profile_edit_location") { draft = draft.copy(location = it) }
                        val flagLabel = form.flags.firstOrNull { it.value == draft.flag }?.label ?: "Not chosen"
                        Surface(
                            onClick = { choosingFlag = true },
                            enabled = !busy,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().testTag("profile_edit_country"),
                        ) {
                            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text("Country", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(flagLabel, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        ProfileField("Occupation", draft.occupation, 100, "profile_edit_occupation") { draft = draft.copy(occupation = it) }
                        ProfileField("Interests", draft.interests, 100, "profile_edit_interests") { draft = draft.copy(interests = it) }
                        ProfileField("Website", draft.website, 255, "profile_edit_website") { draft = draft.copy(website = it) }
                        ProfileField("Signature", draft.signature, 500, "profile_edit_signature", minLines = 2) { draft = draft.copy(signature = it) }
                        ProfileField("About you", draft.bio, 1024, "profile_edit_bio", minLines = 3) { draft = draft.copy(bio = it) }
                        ProfileSwitch("Station newsletter", draft.newsletter, "profile_edit_newsletter") { draft = draft.copy(newsletter = it) }
                        ProfileSwitch("Hide my online status", draft.hideOnlineStatus, "profile_edit_hide_online") { draft = draft.copy(hideOnlineStatus = it) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
                            Button(
                                onClick = { actions.onSaveProfile(draft) },
                                enabled = !busy && draft != form.profile,
                                modifier = Modifier.testTag("profile_edit_save"),
                            ) { Text(if (edit.status == ProfileEditStatus.Saving) "Saving…" else "Save to ${station.shortName}") }
                            TextButton(onClick = { draft = form.profile }, enabled = !busy && draft != form.profile) { Text("Reset") }
                        }
                    }
                }
            }
        }
    }
    if (choosingFlag && form != null) {
        var filter by rememberSaveable { mutableStateOf("") }
        val options = remember(form.flags, filter) {
            form.flags.filter { filter.isBlank() || it.label.contains(filter.trim(), ignoreCase = true) }
        }
        AlertDialog(
            onDismissRequest = { choosingFlag = false },
            title = { Text("Country") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it.take(40) },
                        label = { Text("Filter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(Modifier.heightIn(max = 360.dp).testTag("profile_edit_country_list")) {
                        items(options, key = { it.value }) { option ->
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (option.value == draft.flag) FontWeight.SemiBold else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) {
                                        draft = draft.copy(flag = option.value)
                                        choosingFlag = false
                                    }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingFlag = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    maxCharacters: Int,
    testTag: String,
    minLines: Int = 1,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(maxCharacters)) },
        label = { Text(label) },
        singleLine = minLines == 1,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
}

@Composable
private fun ProfileSwitch(label: String, checked: Boolean, testTag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(testTag))
    }
}

private val OnlineGreen = Color(0xFF2E9E4F)
private const val INITIAL_CALENDAR_DAYS = 14
private const val MORE_CALENDAR_DAYS = 30
