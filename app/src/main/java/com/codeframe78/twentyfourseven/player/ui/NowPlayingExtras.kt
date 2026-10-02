package com.codeframe78.twentyfourseven.player.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import kotlinx.coroutines.delay

/**
 * What the station reports about the current track beyond its title: how far it has played, who requested it,
 * and how many people are listening. Each line appears only when the station supplied it.
 */
@Composable
internal fun NowPlayingTrackExtras(
    state: MainUiState,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
    val nowPlaying = state.nowPlaying
    val textAlign = if (alignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start
    Column(
        Modifier.testTag("now_playing_extras"),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state.playback.status == PlaybackStatus.Playing) TrackProgress(nowPlaying)
        requestLine(nowPlaying)?.let { line ->
            Text(
                line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("now_playing_requester"),
            )
        }
        nowPlaying.listenerCount?.let { count ->
            Text(
                listenerCountLabel(count),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                modifier = Modifier.testTag("now_playing_listeners"),
            )
        }
    }
}

@Composable
private fun TrackProgress(nowPlaying: NowPlayingState) {
    val length = nowPlaying.trackLengthMillis ?: return
    val started = nowPlaying.trackStartedElapsedRealtimeMillis ?: return
    val played by produceState(playedMillis(started, length), started, length) {
        while (true) {
            value = playedMillis(started, length)
            delay(1_000)
        }
    }
    Column(
        Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .testTag("now_playing_progress")
            .clearAndSetSemantics {
                contentDescription = "Track length ${formatTrackTime(length)}"
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LinearProgressIndicator(
            progress = { trackProgressFraction(played, length) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTrackTime(played), style = MaterialTheme.typography.labelSmall)
            Text(formatTrackTime(length), style = MaterialTheme.typography.labelSmall)
        }
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
