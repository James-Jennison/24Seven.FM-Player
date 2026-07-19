package com.codeframe78.twentyfourseven.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.NowPlayingState
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationCapabilities
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
import com.codeframe78.twentyfourseven.player.domain.StreamVariant
import com.codeframe78.twentyfourseven.player.ui.theme.TwentyFourSevenTheme

@Preview(name = "Compact phone · playing", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun CompactPlayingPreview() = PlayerPreview(PlaybackStatus.Playing)

@Preview(name = "Phone landscape · paused", widthDp = 760, heightDp = 400, showBackground = true)
@Composable
private fun LandscapePausedPreview() = PlayerPreview(PlaybackStatus.Paused)

@Preview(name = "Expanded · buffering", widthDp = 1200, heightDp = 800, showBackground = true)
@Composable
private fun ExpandedBufferingPreview() = PlayerPreview(PlaybackStatus.Buffering)

@Preview(name = "Reconnecting · missing artwork", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun MissingArtworkPreview() = PlayerPreview(PlaybackStatus.Retrying, displayTitle = null)

@Preview(name = "Offline · automatic recovery", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun OfflineRecoveryPreview() = PlayerPreview(PlaybackStatus.WaitingForNetwork)

@Preview(name = "Long metadata · large type", widthDp = 430, heightDp = 930, fontScale = 1.5f, showBackground = true)
@Composable
private fun LongMetadataPreview() = PlayerPreview(
    PlaybackStatus.Playing,
    displayTitle = "The International Orchestra for Motion Pictures - A Remarkably Long Movement Title That Must Remain Readable on a Narrow Display",
)

@Preview(
    name = "Light mode · playback error",
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Composable
private fun LightErrorPreview() = PlayerPreview(PlaybackStatus.Error, darkTheme = false)

@Composable
private fun PlayerPreview(
    status: PlaybackStatus,
    displayTitle: String? = "John Williams - The Adventure Continues",
    darkTheme: Boolean = true,
) {
    val stations = previewStations
    val selected = stations.first()
    TwentyFourSevenTheme(darkTheme = darkTheme) {
        AdaptivePlayerScreen(
            state = MainUiState(
                stations = stations,
                selectedStation = selected,
                playback = PlaybackState(selected.id, status),
                nowPlaying = NowPlayingState(selected.id, displayTitle),
            ),
            padding = PaddingValues(0.dp),
            onSelectStation = {},
            onPlay = {},
            onPause = {},
            onStop = {},
        )
    }
}

private val previewStations = listOf(
    previewStation("sst", "StreamingSoundtracks.com", "SST", "Movie, game, TV and anime scores"),
    previewStation("1980s", "1980s.FM", "1980s", "Music from the 1980s"),
    previewStation("afm", "Adagio.FM", "Adagio", "Classical and light music"),
    previewStation("dfm", "Death.FM", "Death", "Extreme metal"),
    previewStation("efm", "Entranced.FM", "Entranced", "Trance and electronic music"),
)

private fun previewStation(id: String, name: String, shortName: String, description: String) = Station(
    id = StationId(id),
    name = name,
    shortName = shortName,
    description = description,
    websiteUrl = "https://example.invalid/",
    streams = listOf(StreamVariant("https://example.invalid/live", "Preview", 0, StreamFormat.Aac, 128)),
    capabilities = StationCapabilities(
        supportsAuthentication = true,
        supportsChat = true,
        supportsRequests = true,
        supportsRequestMessages = id == "sst",
        supportsQueue = true,
        supportsHistory = true,
    ),
)
