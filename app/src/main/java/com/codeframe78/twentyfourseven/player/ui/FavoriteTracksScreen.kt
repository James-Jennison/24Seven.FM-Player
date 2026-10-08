package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.Station
import coil3.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Immutable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteChange
import com.codeframe78.twentyfourseven.player.domain.FavoriteChangeStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksLoadStatus
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus

@Composable
internal fun FavoriteTracksScreen(
    state: MainUiState,
    padding: PaddingValues,
    onRefresh: () -> Unit,
    onPrepareRequest: (FavoriteTrack) -> Unit,
    onCancelRequest: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    val favorites = state.favorites
    val signedIn = state.auth?.status == AuthStatus.SignedIn
    var filter by rememberSaveable(state.selectedStation?.id?.value) { mutableStateOf("") }
    var sortOrder by remember(state.selectedStation?.id) { mutableStateOf(FavoriteTrackSortOrder.Position) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val allTracks = favorites?.tracks.orEmpty()
    val visibleTracks = remember(allTracks, filter, sortOrder) {
        val query = filter.trim()
        val matchingTracks = if (query.isBlank()) {
            allTracks
        } else {
            allTracks.filter { track -> track.matchesFilter(query) }
        }
        matchingTracks.sortedForFavorites(sortOrder)
    }

    Box(Modifier.fillMaxSize()) {
        RefreshableBox(
            onRefresh = { if (signedIn) onRefresh() },
            modifier = Modifier.fillMaxSize().padding(padding),
            isLoading = favorites?.status == FavoriteTracksLoadStatus.Loading,
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("favorite_tracks_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Favorites", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    if (signedIn) {
                        IconButton(onClick = onRefresh, enabled = favorites?.status != FavoriteTracksLoadStatus.Loading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh favorite tracks")
                        }
                    }
                }
            }

            if (state.selectedStation?.capabilities?.supportsFavorites != true) {
                item { Text("Favorite tracks have not been verified for this station.") }
                return@LazyColumn
            }

            if (!signedIn) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Text("Sign in to see your favorites", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Your ${state.selectedStation?.shortName.orEmpty()} favorite tracks appear here once you sign in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onOpenAccount) { Text("Sign in") }
                    }
                }
                return@LazyColumn
            }

            when (favorites?.status ?: FavoriteTracksLoadStatus.Idle) {
                FavoriteTracksLoadStatus.Idle, FavoriteTracksLoadStatus.Loading -> item {
                    SkeletonList(
                        rows = 6,
                        showsArtwork = false,
                        contentPadding = PaddingValues(vertical = 4.dp),
                        description = "Loading your favorite tracks",
                    )
                }
                FavoriteTracksLoadStatus.Error -> item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(favorites?.errorMessage ?: "Favorite tracks could not be loaded.", color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRefresh) { Text("Try again") }
                        }
                    }
                }
                FavoriteTracksLoadStatus.Ready -> {
                    item { FavoriteChangeNotice(state) }
                    item {
                        OutlinedTextField(
                            value = filter,
                            onValueChange = { filter = it.take(100) },
                            label = { Text("Filter favorites") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (filter.isNotEmpty()) {
                                    IconButton(onClick = { filter = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear filter")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (filter.isBlank()) "${visibleTracks.size} tracks" else "${visibleTracks.size} of ${favorites?.tracks?.size ?: 0} tracks",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Box {
                                TextButton(
                                    onClick = { sortMenuOpen = true },
                                    modifier = Modifier.testTag("favorite_track_sort"),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Sort: ${sortOrder.label}")
                                }
                                DropdownMenu(
                                    expanded = sortMenuOpen,
                                    onDismissRequest = { sortMenuOpen = false },
                                ) {
                                    FavoriteTrackSortOrder.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                sortOrder = option
                                                sortMenuOpen = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (visibleTracks.isEmpty()) {
                        item {
                            Text(
                                if (filter.isBlank()) "No favorite tracks were found." else "No favorites match this filter.",
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
                            canRequest = state.selectedStation?.capabilities?.supportsRequests == true &&
                                state.requests?.status != SongRequestLoadStatus.Submitting,
                            onPrepareRequest = onPrepareRequest,
                            coverUrl = favoriteCoverUrl(state.selectedStation, track.albumId),
                            manage = favoriteManagement(state, track, allTracks),
                        )
                    }
                }
            }
        }
        }

        FavoriteRequestFeedback(
            notice = state.requests?.notice,
            errorMessage = state.requests?.errorMessage,
            onDismiss = onCancelRequest,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(padding)
                .padding(20.dp),
        )
    }
}

internal fun FavoriteTrack.matchesFilter(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        album.contains(query, ignoreCase = true) ||
        artist.contains(query, ignoreCase = true) ||
        genre?.contains(query, ignoreCase = true) == true

@Composable
private fun FavoriteRequestFeedback(
    notice: String?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = errorMessage ?: notice ?: return
    val isError = errorMessage != null
    Card(
        modifier = modifier.fillMaxWidth().testTag(if (isError) "favorite_request_error" else "favorite_request_notice"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (isError) "Request status" else "Request sent",
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(message)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_favorite_request_result"),
            ) { Text("Dismiss") }
        }
    }
}

/**
 * The station's small cover for a favorite's album, from the station's own image host. The small size keeps a list
 * of well over a thousand favorites light to scroll.
 */
internal fun favoriteCoverUrl(station: Station?, albumId: String?): String? {
    val id = albumId?.takeIf { it.matches(COVER_ALBUM_ID) } ?: return null
    val host = runCatching { java.net.URI(station?.logoUrl.orEmpty()).host }.getOrNull()?.takeIf(String::isNotBlank)
        ?: return null
    return "https://$host/images/cover/040/$id.jpg"
}

private val COVER_ALBUM_ID = Regex("[A-Za-z0-9_.-]{1,64}")

/** What the member may do to one row of their own favorites list, and whether a change is on its way. */
@Immutable
internal data class FavoriteManagement(
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
    val busy: Boolean,
    val onChange: (FavoriteChange) -> Unit,
)

/** The controls for a row of the signed-in member's own list; null for another member's list or a visitor. */
@Composable
internal fun favoriteManagement(state: MainUiState, track: FavoriteTrack, allTracks: List<FavoriteTrack>): FavoriteManagement? {
    val station = state.selectedStation ?: return null
    val favorites = state.favorites ?: return null
    val songId = track.songId ?: return null
    if (
        !station.capabilities.supportsFavoriteManagement || state.auth?.status != AuthStatus.SignedIn ||
        favorites.status != FavoriteTracksLoadStatus.Ready
    ) {
        return null
    }
    val onChange = LocalStationExtrasActions.current.onChangeFavorite
    val index = allTracks.indexOfFirst { it.songId == songId }
    return FavoriteManagement(
        canMoveUp = index > 0,
        canMoveDown = index >= 0 && index < allTracks.lastIndex,
        busy = favorites.change.status == FavoriteChangeStatus.Working,
        onChange = { change -> onChange(track, change) },
    )
}

/** One favorite as a row: its cover, what it is, whether it can be requested, and the request button when it can. */
@Composable
internal fun FavoriteTrackCard(
    track: FavoriteTrack,
    canRequest: Boolean,
    onPrepareRequest: (FavoriteTrack) -> Unit,
    coverUrl: String? = null,
    manage: FavoriteManagement? = null,
) {
    val available = track.availability.canRequest
    var menuOpen by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag("favorite_track_${track.position}")) {
        Row(
            Modifier
                .fillMaxWidth()
                .opensAlbum(track.albumId, track.album.takeIf(String::isNotBlank), null)
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
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
                if (coverUrl == null) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    AsyncImage(
                        model = crossfadingImage(coverUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val facts = listOfNotNull(track.genre, track.year, track.duration).joinToString(" · ")
                if (available) {
                    // The button already says a track can be requested, so the light alone marks it.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RequestStatusIndicator(track.availability, compact = true, showsLabel = false)
                        FavoriteFacts(facts)
                    }
                } else {
                    FavoriteFacts(facts)
                    RequestStatusIndicator(track.availability, compact = true)
                }
                track.availability.detail?.let {
                    Text(
                        availabilityDetailLabel(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (available) {
                FilledTonalButton(
                    onClick = { onPrepareRequest(track) },
                    enabled = canRequest,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) { Text("Request Now") }
            }
            if (manage != null) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        enabled = !manage.busy,
                        modifier = Modifier.testTag("favorite_track_menu_${track.position}"),
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More actions for ${track.title}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Move up") },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                            enabled = manage.canMoveUp,
                            onClick = {
                                menuOpen = false
                                manage.onChange(FavoriteChange.MoveUp)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Move down") },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                            enabled = manage.canMoveDown,
                            onClick = {
                                menuOpen = false
                                manage.onChange(FavoriteChange.MoveDown)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from favorites") },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                confirmRemove = true
                            },
                        )
                    }
                }
            }
        }
    }
    if (confirmRemove && manage != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove from favorites?") },
            text = { Text("\"${track.title}\" will be removed from your favorites on the station.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        manage.onChange(FavoriteChange.Remove)
                    },
                    modifier = Modifier.testTag("favorite_track_remove_confirm"),
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun FavoriteFacts(text: String) {
    if (text.isBlank()) return
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
