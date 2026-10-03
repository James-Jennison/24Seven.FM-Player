package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.RequestableTrack
import com.codeframe78.twentyfourseven.player.domain.SongRequestLoadStatus

/** An album as the item that was tapped knows it: its station identifier, and whatever title and cover it showed. */
@Immutable
internal data class AlbumLink(val albumId: String, val title: String? = null, val artworkUrl: String? = null)

@Immutable
internal data class AlbumActions(
    val onOpen: (AlbumLink) -> Unit = {},
    val onClose: () -> Unit = {},
    val onRate: (String) -> Unit = {},
    val onPrepareRequest: (String) -> Unit = {},
)

/** Opens an album's page from wherever the album is shown; null where the station library cannot be browsed. */
internal val LocalAlbumOpener = staticCompositionLocalOf<((AlbumLink) -> Unit)?> { null }

/** Makes an element open the album it shows. It stays as it was when the album is unknown or cannot be opened. */
@Composable
internal fun Modifier.opensAlbum(albumId: String?, title: String?, artworkUrl: String?): Modifier {
    val open = LocalAlbumOpener.current
    val id = albumId?.takeIf { it.matches(AlbumIdentifier) }
    return if (open == null || id == null) {
        this
    } else {
        clickable(onClickLabel = "View album", role = Role.Button) { open(AlbumLink(id, title, artworkUrl)) }
    }
}

/** The stations name a cover image after its album, as in "/images/cover/500/B000KNB1IM.jpg". */
internal fun albumIdFromCoverUrl(url: String?): String? = url
    ?.substringBefore('?')
    ?.takeIf { "/images/cover/" in it }
    ?.substringAfterLast('/')
    ?.substringBeforeLast('.')
    ?.takeIf { it.matches(AlbumIdentifier) }

private val AlbumIdentifier = Regex("[A-Za-z0-9_.-]{1,64}")

/** An album's tracks with their request status, opened over whatever screen showed the album. */
@Composable
internal fun AlbumDialog(state: MainUiState, actions: AlbumActions) {
    val album = state.album ?: return
    val station = state.selectedStation ?: return
    val requests = state.requests
    val signedIn = state.auth?.status == AuthStatus.SignedIn
    val tracks = requests?.tracks.orEmpty().filter { it.albumId == album.albumId }
    val title = album.title ?: requests?.albumTitle?.takeIf { tracks.isNotEmpty() } ?: "Album"
    Dialog(onDismissRequest = actions.onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("album_dialog")) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    album.artworkUrl?.let { artwork ->
                        AsyncImage(
                            model = crossfadingImage(artwork),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("album_title"),
                        )
                        Text(
                            station.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = actions.onClose) { Icon(Icons.Default.Close, contentDescription = "Close album") }
                }
                if (station.capabilities.supportsAlbumRating && signedIn) {
                    OutlinedButton(
                        onClick = { actions.onRate(album.albumId) },
                        modifier = Modifier.padding(top = 8.dp).testTag("album_rate"),
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Rate this album")
                    }
                }
                when {
                    requests == null || requests.status == SongRequestLoadStatus.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    tracks.isEmpty() -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val failed = requests.errorMessage != null
                        Text(
                            requests.errorMessage ?: requests.notice ?: "No track listing was found for this album.",
                            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (failed) {
                            TextButton(onClick = { actions.onOpen(AlbumLink(album.albumId, album.title, album.artworkUrl)) }) {
                                Text("Try again")
                            }
                        }
                    }
                    else -> LazyColumn(
                        Modifier.fillMaxSize().padding(top = 8.dp).testTag("album_tracks"),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (!signedIn) {
                            item {
                                Text(
                                    "Sign in to ${station.shortName} from the More tab to request a track.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        requests.errorMessage?.let { error ->
                            item { Text(error, color = MaterialTheme.colorScheme.error) }
                        }
                        // A track that cannot be requested has no song number, so its place in the list names it.
                        itemsIndexed(tracks, key = { index, track -> track.songId.ifBlank { "track-$index" } }) { _, track ->
                            RequestableTrackRow(
                                track = track,
                                canRequest = requests.status == SongRequestLoadStatus.Ready,
                                onPrepareRequest = actions.onPrepareRequest,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One library track with its request status and, when the station allows it, the button that starts a request. */
@Composable
internal fun RequestableTrackRow(track: RequestableTrack, canRequest: Boolean, onPrepareRequest: (String) -> Unit) {
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
                compact = true,
                showsLabel = !track.availability.canRequest,
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
            FilledTonalButton(
                onClick = { onPrepareRequest(track.songId) },
                enabled = canRequest,
            ) { Text("Request Now") }
        }
    }
}
