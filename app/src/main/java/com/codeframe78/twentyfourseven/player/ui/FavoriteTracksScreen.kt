package com.codeframe78.twentyfourseven.player.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
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
    onConfirmRequest: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onReviewTerms: () -> Unit,
) {
    RequestConfirmationDialog(state, onCancelRequest, onConfirmRequest, onReviewTerms)
    val favorites = state.favorites
    val signedIn = state.auth?.status == AuthStatus.SignedIn
    var filter by remember(state.selectedStation?.id) { mutableStateOf("") }
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("favorite_tracks_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Favorite tracks", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            state.selectedStation?.name.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Station sign-in required", fontWeight = FontWeight.Bold)
                            Text("Sign in to this station to discover and browse your own favorite-track list.")
                            Button(onClick = onOpenAccount) { Text("Open account") }
                        }
                    }
                }
                return@LazyColumn
            }

            when (favorites?.status ?: FavoriteTracksLoadStatus.Idle) {
                FavoriteTracksLoadStatus.Idle, FavoriteTracksLoadStatus.Loading -> item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Text("Loading your favorite tracks…")
                    }
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
                    item {
                        OutlinedTextField(
                            value = filter,
                            onValueChange = { filter = it.take(100) },
                            label = { Text("Filter favorites") },
                            supportingText = {
                                Text(if (filter.isBlank()) "${visibleTracks.size} tracks" else "${visibleTracks.size} of ${favorites?.tracks?.size ?: 0} tracks")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Box {
                            TextButton(
                                onClick = { sortMenuOpen = true },
                                modifier = Modifier.testTag("favorite_track_sort"),
                            ) {
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
                    if (visibleTracks.isEmpty()) {
                        item { Text(if (filter.isBlank()) "No favorite tracks were found." else "No favorites match this filter.") }
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
                        )
                    }
                }
            }
        }

        FavoriteRequestFeedback(
            notice = state.requests?.notice,
            errorMessage = state.requests?.errorMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(padding)
                .padding(20.dp),
        )
    }
}

private fun FavoriteTrack.matchesFilter(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        album.contains(query, ignoreCase = true) ||
        artist.contains(query, ignoreCase = true) ||
        genre?.contains(query, ignoreCase = true) == true

@Composable
private fun FavoriteRequestFeedback(
    notice: String?,
    errorMessage: String?,
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
        }
    }
}

@Composable
private fun FavoriteTrackCard(
    track: FavoriteTrack,
    canRequest: Boolean,
    onPrepareRequest: (FavoriteTrack) -> Unit,
) {
    val available = track.availability.canRequest
    Card(Modifier.fillMaxWidth().testTag("favorite_track_${track.position}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RequestStatusIndicator(track.availability)
                Spacer(Modifier.weight(1f))
                Text("#${track.position}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" • "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                listOfNotNull(track.genre, track.year, track.duration).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            track.availability.detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (available) {
                Button(
                    onClick = { onPrepareRequest(track) },
                    enabled = canRequest,
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Request Now") }
            }
        }
    }
}
