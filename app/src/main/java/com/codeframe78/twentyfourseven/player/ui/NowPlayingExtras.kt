package com.codeframe78.twentyfourseven.player.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.AlbumRatingStatus
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.FavoriteActionStatus
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import kotlinx.coroutines.delay

@Immutable
internal data class TrackActions(
    val onAddFavorite: () -> Unit = {},
    val onOpenRating: () -> Unit = {},
    val onSubmitRating: (String) -> Unit = {},
    val onCloseRating: () -> Unit = {},
)

internal val MinimumOverlayArtworkSize = 200.dp
// Two 48 dp touch targets and the gap between them.
private val MinimumActionArtworkSize = 104.dp
// Artwork often carries its own lettering near the bottom edge, so the scrim reaches full strength above the first line.
private val OverlayScrim = Brush.verticalGradient(
    0f to Color.Transparent,
    0.2f to Color.Black.copy(alpha = 0.8f),
    1f to Color.Black.copy(alpha = 0.88f),
)

/**
 * What the station reports about the current track beyond its title, drawn over the artwork so no player layout
 * loses height: who requested it, how many people are listening, how far it has played, and the signed-in
 * listener's favorite and rating actions. Each part appears only when the station supplied it. Layouts whose artwork
 * is too small for the caption pass [showsCaption] false and place [NowPlayingInlineExtras] beside the artwork.
 */
@Composable
internal fun NowPlayingArtworkOverlay(
    state: MainUiState,
    actions: TrackActions,
    modifier: Modifier = Modifier,
    showsCaption: Boolean = true,
) {
    BoxWithConstraints(modifier.testTag("now_playing_extras")) {
        if (maxWidth < MinimumActionArtworkSize) return@BoxWithConstraints
        val actionPadding = if (maxWidth < MinimumActionArtworkSize + 16.dp) 0.dp else 8.dp
        TrackActionButtons(state, actions, Modifier.align(Alignment.TopEnd).padding(actionPadding))
        if (!showsCaption || maxWidth < MinimumOverlayArtworkSize) return@BoxWithConstraints
        val nowPlaying = state.nowPlaying
        val request = requestLine(nowPlaying)
        val caption = listOfNotNull(
            favoriteResultLine(state),
            request,
            nowPlaying.listenerCount?.let(::listenerCountLabel),
        )
        val showsProgress = state.playback.status.followsStationTrack &&
            nowPlaying.trackLengthMillis != null &&
            nowPlaying.trackStartedElapsedRealtimeMillis != null
        if (caption.isNotEmpty() || showsProgress) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(OverlayScrim)
                    .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                caption.forEachIndexed { index, line ->
                    Text(
                        line,
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .testTag("now_playing_caption_$index")
                            .then(if (line === request) Modifier.opensMemberProfile(nowPlaying.requesterName) else Modifier),
                    )
                }
                if (showsProgress) TrackProgress(nowPlaying)
            }
        }
    }
}

/** The caption and track progress for layouts that show them beside the artwork instead of over it. */
@Composable
internal fun NowPlayingInlineExtras(state: MainUiState, modifier: Modifier = Modifier) {
    val nowPlaying = state.nowPlaying
    val favoriteResult = favoriteResultLine(state)
    val caption = favoriteResult
        ?: listOfNotNull(requestLine(nowPlaying), nowPlaying.listenerCount?.let(::listenerCountLabel))
            .joinToString(" • ")
            .takeIf(String::isNotEmpty)
    val showsProgress = state.playback.status.followsStationTrack &&
        nowPlaying.trackLengthMillis != null &&
        nowPlaying.trackStartedElapsedRealtimeMillis != null
    if (caption == null && !showsProgress) return
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.testTag("now_playing_inline_extras"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        caption?.let { line ->
            Text(
                line,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .testTag("now_playing_caption_0")
                    .then(if (favoriteResult == null) Modifier.opensMemberProfile(nowPlaying.requesterName) else Modifier),
            )
        }
        if (showsProgress) {
            TrackProgress(nowPlaying, contentColor, MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

/**
 * True when the shown track is the one the station is playing right now, so its progress can be drawn: while this
 * device is playing it, and while the device is idle and the Player shows what is on air.
 */
internal val PlaybackStatus.followsStationTrack: Boolean
    get() = this == PlaybackStatus.Playing || showsOnAirPreview

@Composable
private fun TrackActionButtons(state: MainUiState, actions: TrackActions, modifier: Modifier) {
    val station = state.selectedStation ?: return
    val nowPlaying = state.nowPlaying
    if (state.auth?.status != AuthStatus.SignedIn || nowPlaying.track.isNullOrBlank()) return
    val favorite = state.trackActions?.favorite?.takeIf { it.trackTitle == nowPlaying.track }
    val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
        disabledContainerColor = Color.Black.copy(alpha = 0.6f),
        disabledContentColor = Color.White,
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (station.capabilities.supportsNowPlayingFavorite) {
            val isFavorite = favorite?.status in setOf(FavoriteActionStatus.Added, FavoriteActionStatus.AlreadyFavorite)
            val isWorking = favorite?.status == FavoriteActionStatus.Working
            FilledIconButton(
                onClick = actions.onAddFavorite,
                enabled = !isWorking && !isFavorite,
                colors = colors,
                modifier = Modifier
                    .testTag("now_playing_add_favorite")
                    .semantics {
                        contentDescription = if (isFavorite) {
                            "This track is in your ${station.shortName} favorites"
                        } else {
                            "Add this track to your ${station.shortName} favorites"
                        }
                    },
            ) {
                when {
                    isWorking -> CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    isFavorite -> Icon(Icons.Default.Favorite, contentDescription = null)
                    else -> Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                }
            }
        }
        if (station.capabilities.supportsAlbumRating && nowPlaying.albumId != null) {
            FilledIconButton(
                onClick = actions.onOpenRating,
                colors = colors,
                modifier = Modifier
                    .testTag("now_playing_rate_album")
                    .semantics { contentDescription = "Rate this album" },
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TrackProgress(
    nowPlaying: NowPlayingState,
    contentColor: Color = Color.White,
    trackColor: Color = Color.White.copy(alpha = 0.3f),
) {
    val length = nowPlaying.trackLengthMillis ?: return
    val started = nowPlaying.trackStartedElapsedRealtimeMillis ?: return
    val played by produceState(playedMillis(started, length), started, length) {
        while (true) {
            value = playedMillis(started, length)
            delay(1_000)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("now_playing_progress")
            .clearAndSetSemantics { contentDescription = "Track length ${formatTrackTime(length)}" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(formatTrackTime(played), color = contentColor, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { trackProgressFraction(played, length) },
            modifier = Modifier.weight(1f),
            trackColor = trackColor,
        )
        Text(formatTrackTime(length), color = contentColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun AlbumRatingDialog(state: MainUiState, actions: TrackActions) {
    val rating = state.trackActions?.rating ?: return
    if (rating.status == AlbumRatingStatus.Idle) return
    var selected by rememberSaveable(rating.albumId) { mutableStateOf<String?>(null) }
    val submitting = rating.status == AlbumRatingStatus.Submitting
    AlertDialog(
        onDismissRequest = { if (!submitting) actions.onCloseRating() },
        modifier = Modifier.testTag("album_rating_dialog"),
        title = { Text("Rate this album") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rating.albumTitle?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                rating.artist?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                currentRatingLine(rating.currentRating, rating.voteCount)?.let { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
                when (rating.status) {
                    AlbumRatingStatus.Loading, AlbumRatingStatus.Submitting -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Text(if (submitting) "Sending your rating…" else "Loading…")
                    }
                    AlbumRatingStatus.Ready -> Column(Modifier.selectableGroup()) {
                        rating.options.forEach { option ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selected == option.value,
                                        onClick = { selected = option.value },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 6.dp)
                                    .testTag("album_rating_option_${option.value}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(selected = selected == option.value, onClick = null)
                                Text(option.label)
                            }
                        }
                    }
                    else -> albumRatingMessage(rating.status)?.let { message ->
                        Text(
                            message,
                            color = if (rating.status in AlbumRatingProblems) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (rating.status == AlbumRatingStatus.Ready) {
                Button(
                    onClick = { selected?.let(actions.onSubmitRating) },
                    enabled = selected != null,
                    modifier = Modifier.testTag("album_rating_submit"),
                ) { Text("Rate") }
            } else {
                TextButton(onClick = actions.onCloseRating, enabled = !submitting) { Text("Close") }
            }
        },
        dismissButton = {
            if (rating.status == AlbumRatingStatus.Ready) {
                TextButton(onClick = actions.onCloseRating) { Text("Cancel") }
            }
        },
    )
}

private val AlbumRatingProblems = setOf(
    AlbumRatingStatus.SignInRequired,
    AlbumRatingStatus.Unconfirmed,
    AlbumRatingStatus.Error,
)

internal fun albumRatingMessage(status: AlbumRatingStatus): String? = when (status) {
    AlbumRatingStatus.AlreadyRated -> "You have already rated this album."
    AlbumRatingStatus.Rated -> "The station recorded your rating."
    AlbumRatingStatus.SignInRequired -> "Sign in to this station to rate albums."
    AlbumRatingStatus.Unconfirmed ->
        "The station did not confirm your rating. Open this again later to see whether it was counted before rating again."
    AlbumRatingStatus.Error -> "The rating could not be loaded right now."
    AlbumRatingStatus.Idle, AlbumRatingStatus.Loading, AlbumRatingStatus.Ready, AlbumRatingStatus.Submitting -> null
}

internal fun currentRatingLine(currentRating: String?, voteCount: String?): String? {
    val rating = currentRating?.takeIf(String::isNotBlank) ?: return null
    return if (voteCount.isNullOrBlank()) "Current rating: $rating" else "Current rating: $rating ($voteCount votes)"
}

/** The outcome of the favorite action, shown only while it still describes the track that is playing. */
internal fun favoriteResultLine(state: MainUiState): String? {
    val favorite = state.trackActions?.favorite?.takeIf { it.trackTitle == state.nowPlaying.track } ?: return null
    val station = state.selectedStation?.shortName ?: "station"
    return when (favorite.status) {
        FavoriteActionStatus.Added -> "Added to your $station favorites"
        FavoriteActionStatus.AlreadyFavorite -> "Already in your $station favorites"
        FavoriteActionStatus.TrackChanged -> "The track changed before it could be added"
        FavoriteActionStatus.SignInRequired -> "Sign in to $station to add favorites"
        FavoriteActionStatus.Failed -> "The favorite could not be added right now"
        FavoriteActionStatus.Idle, FavoriteActionStatus.Working -> null
    }
}

private fun playedMillis(startedElapsedRealtimeMillis: Long, lengthMillis: Long): Long =
    (SystemClock.elapsedRealtime() - startedElapsedRealtimeMillis).coerceIn(0L, lengthMillis)

internal fun trackProgressFraction(playedMillis: Long, lengthMillis: Long): Float =
    if (lengthMillis <= 0L) 0f else (playedMillis.toFloat() / lengthMillis).coerceIn(0f, 1f)

internal fun formatTrackTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

internal fun requestLine(nowPlaying: NowPlayingState): String? {
    val requester = nowPlaying.requesterName?.takeIf(String::isNotBlank) ?: return null
    val message = nowPlaying.requestMessage?.takeIf(String::isNotBlank)
    return if (message == null) "Requested by $requester" else "Requested by $requester: “$message”"
}

internal fun listenerCountLabel(count: Int): String =
    if (count == 1) "1 listener" else "%,d listeners".format(count)
