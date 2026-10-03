package com.codeframe78.twentyfourseven.player.ui

import android.view.ContextThemeWrapper
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import com.codeframe78.twentyfourseven.player.ui.theme.onAccent
import com.codeframe78.twentyfourseven.player.ui.theme.themedAccent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import coil3.compose.AsyncImage
import com.codeframe78.twentyfourseven.player.R
import com.codeframe78.twentyfourseven.player.domain.AudioOutputKind
import com.codeframe78.twentyfourseven.player.domain.PlaybackRoute
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
import com.codeframe78.twentyfourseven.player.playback.setUpCastRouteButton
import com.codeframe78.twentyfourseven.player.ui.theme.StationPalette
import com.codeframe78.twentyfourseven.player.ui.theme.stationPalette

private val ExpandedPlayerBreakpoint = 840.dp
private val CompactPlayerReservedHeight = 420.dp
private val CompactPlayerHorizontalPadding = 12.dp
private val MinimumCompactArtworkSize = 140.dp
private val MaximumCompactArtworkSize = 384.dp
private val CompactPlayerNoScrollHeight = 720.dp
private val LandscapePlayerMinimumHeight = 300.dp
private val LandscapeArtworkMinimumSize = 120.dp
private val LandscapeArtworkMaximumSize = 200.dp
private val LandscapeStationSelectorWidth = 56.dp
private val ExpandedLandscapeMinimumWidth = 1000.dp
private val ExpandedLandscapeMinimumHeight = 640.dp
private const val ExpandedLandscapeMaximumFontScale = 1.15f
private val ArtworkFramePadding = 8.dp
private val StationSwipeThreshold = 64.dp
private val SleepTimerPresetsMinutes = listOf(15, 30, 45, 60, 90)

@Immutable
internal data class SleepTimerActions(
    val onSet: (Long) -> Unit = {},
    val onCancel: () -> Unit = {},
)

@Immutable
internal data class AudioOutputActions(
    val onOpenChooser: () -> Unit = {},
)

@Composable
internal fun AdaptivePlayerScreen(
    state: MainUiState,
    padding: PaddingValues,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions = SleepTimerActions(),
    audioOutputActions: AudioOutputActions = AudioOutputActions(),
    isCoverDisplay: Boolean = false,
    trackActions: TrackActions = TrackActions(),
) {
    val palette = stationPalette(state.selectedStation?.id)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.glow.copy(alpha = 0.62f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
        ),
    ) {
        if (isCoverDisplay) {
            CoverPlayerContent(state, palette, onSelectStation, onPlay, onStop, sleepTimerActions, audioOutputActions)
        } else if (usesExpandedLandscapePlayerLayout(maxWidth, maxHeight, LocalDensity.current.fontScale)) {
            ExpandedLandscapePlayerContent(
                state,
                palette,
                onSelectStation,
                onPlay,
                onStop,
                sleepTimerActions,
                audioOutputActions,
                trackActions,
            )
        } else if (usesLandscapePlayerLayout(maxWidth, maxHeight)) {
            LandscapePlayerContent(state, palette, maxWidth, maxHeight, onSelectStation, onPlay, onStop, sleepTimerActions, audioOutputActions, trackActions)
        } else if (maxWidth >= ExpandedPlayerBreakpoint) {
            ExpandedPlayerContent(state, palette, onSelectStation, onPlay, onStop, sleepTimerActions, audioOutputActions, trackActions)
        } else {
            val compactPlayerCanFitWithoutScroll =
                maxHeight >= CompactPlayerNoScrollHeight && LocalDensity.current.fontScale <= 1.3f
            val availableArtworkWidth = maxWidth - (CompactPlayerHorizontalPadding * 2)
            val availableArtworkHeight = (maxHeight - CompactPlayerReservedHeight)
                .coerceIn(MinimumCompactArtworkSize, MaximumCompactArtworkSize)
            val artworkSize = minOf(
                availableArtworkWidth,
                availableArtworkHeight,
                MaximumCompactArtworkSize,
            )
            CompactPlayerContent(state, palette, artworkSize, onSelectStation, onPlay, onStop, sleepTimerActions, audioOutputActions, trackActions, isScrollable = !compactPlayerCanFitWithoutScroll)
        }
    }
}

internal fun usesLandscapePlayerLayout(width: Dp, height: Dp): Boolean =
    width > height && height >= LandscapePlayerMinimumHeight

internal fun usesExpandedLandscapePlayerLayout(width: Dp, height: Dp, fontScale: Float): Boolean =
    width >= ExpandedLandscapeMinimumWidth &&
        width > height &&
        height >= ExpandedLandscapeMinimumHeight &&
        fontScale <= ExpandedLandscapeMaximumFontScale

@Composable
private fun CoverPlayerContent(
    state: MainUiState,
    palette: StationPalette,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The folded cover window is height-constrained by the camera cutout. Keep the
            // station controls in the visible window instead of allowing the selector to clip.
            NowPlayingArtwork(state, palette, Modifier.size(112.dp), onSelectStation = onSelectStation)
            Column(Modifier.weight(1f)) {
                CoverNowPlayingDetails(state, palette)
            }
        }
        PrimaryPlayerControls(
            state,
            onPlay,
            onStop,
            sleepTimerActions,
            audioOutputActions,
            isCompact = true,
        )
        Spacer(Modifier.weight(1f))
        CoverStationSelector(state, onSelectStation)
    }
}

@Composable
private fun CoverStationSelector(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().testTag("cover_station_selector"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.stations.forEach { station ->
            val selected = station.id == state.selectedStation?.id
            val palette = stationPalette(station.id)
            Card(
                onClick = { onSelectStation(station.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) palette.glow else MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) palette.accent else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .semantics {
                        this.selected = selected
                        role = Role.RadioButton
                        contentDescription = if (selected) "${station.name}, selected" else station.name
                    }
                    .testTag("cover_station_${station.id.value}"),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(stationSelectorLogoResource(station.id)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverNowPlayingDetails(
    state: MainUiState,
    palette: StationPalette,
) {
    val metadata = parseNowPlayingMetadata(state.nowPlaying.displayTitle)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            state.selectedStation?.shortName ?: "24Seven.FM",
            color = palette.themedAccent(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            metadata.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("now_playing_title"),
        )
        if (state.playback.status != PlaybackStatus.Playing) {
            Text(
                playbackStatusMessage(state.playback.status, hasTrack = state.nowPlaying.displayTitle != null),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { stateDescription = state.playback.status.accessibleName },
            )
        }
    }
}

@Composable
private fun CompactPlayerContent(
    state: MainUiState,
    palette: StationPalette,
    artworkSize: androidx.compose.ui.unit.Dp,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    trackActions: TrackActions,
    isScrollable: Boolean,
) {
    val scrollModifier = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(
        Modifier
            .fillMaxSize()
            .then(scrollModifier)
            .testTag("compact_player_scroll")
            .padding(horizontal = CompactPlayerHorizontalPadding, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NowPlayingArtwork(state, palette, Modifier.size(artworkSize), trackActions, onSelectStation = onSelectStation)
        NowPlayingDetails(state, palette, trackActions = trackActions)
        PrimaryPlayerControls(state, onPlay, onStop, sleepTimerActions, audioOutputActions, isCompact = true)
        if (!isScrollable) Spacer(Modifier.weight(1f))
        StationSelector(state, onSelectStation, isCompact = true)
        if (!isScrollable) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ExpandedLandscapePlayerContent(
    state: MainUiState,
    palette: StationPalette,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    trackActions: TrackActions,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        palette.secondary.copy(alpha = 0.26f),
                        palette.glow.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                ),
            )
            .testTag("expanded_landscape_player"),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("tablet_player_hero"),
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.84f),
                border = BorderStroke(1.dp, palette.secondary.copy(alpha = 0.52f)),
                tonalElevation = 8.dp,
            ) {
                BoxWithConstraints(Modifier.fillMaxSize().padding(28.dp)) {
                    val artworkSize = minOf(maxHeight, maxWidth * 0.34f, 320.dp)
                    // Artwork too small for the caption shows it with the track details instead.
                    val captionOverArtwork = artworkSize - (ArtworkFramePadding * 2) >= MinimumOverlayArtworkSize
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NowPlayingArtwork(state, palette, Modifier.size(artworkSize), trackActions, captionOverArtwork, onSelectStation)
                        Column(
                            Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "YOUR LIVE RADIO NETWORK",
                                color = palette.themedAccent(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(10.dp))
                            NowPlayingDetails(state, palette, Alignment.Start, trackActions)
                            if (!captionOverArtwork) NowPlayingInlineExtras(state, Modifier.padding(top = 8.dp))
                            // The tagline stands in for the title until a track is known, so it is not repeated then.
                            state.selectedStation?.description
                                ?.takeIf { it.isNotBlank() && !state.nowPlaying.displayTitle.isNullOrBlank() }
                                ?.let { description ->
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(28.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            ) {
                                PrimaryPlayerControls(
                                    state,
                                    onPlay,
                                    onStop,
                                    sleepTimerActions,
                                    audioOutputActions,
                                )
                            }
                        }
                    }
                }
            }
            ExpandedLandscapeStationSelector(state, onSelectStation)
        }
    }
}

@Composable
private fun ExpandedLandscapeStationSelector(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 138.dp)
            .testTag("tablet_station_selector"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        tonalElevation = 4.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Explore the network",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${state.stations.size} live stations",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.stations.forEach { station ->
                    val selected = station.id == state.selectedStation?.id
                    val stationPalette = stationPalette(station.id)
                    Card(
                        onClick = { onSelectStation(station.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                stationPalette.glow.copy(alpha = 0.94f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) stationPalette.accent else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .semantics {
                                this.selected = selected
                                role = Role.RadioButton
                                contentDescription = if (selected) "${station.name}, selected" else station.name
                            }
                            .testTag("tablet_station_${station.id.value}"),
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(stationSelectorLogoResource(station.id)),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    station.shortName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) stationPalette.accent else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    station.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) {
                                        Color.White.copy(alpha = 0.88f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapePlayerContent(
    state: MainUiState,
    palette: StationPalette,
    maxWidth: Dp,
    maxHeight: Dp,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    trackActions: TrackActions,
) {
    val artworkSize = minOf(
        LandscapeArtworkMaximumSize,
        (maxWidth - 400.dp).coerceIn(LandscapeArtworkMinimumSize, LandscapeArtworkMaximumSize),
        (maxHeight - 32.dp).coerceAtLeast(LandscapeArtworkMinimumSize),
    )
    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("landscape_player"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The landscape artwork is too small for the caption, so it sits with the track details instead.
        NowPlayingArtwork(state, palette, Modifier.size(artworkSize), trackActions, showsOverlayCaption = false, onSelectStation = onSelectStation)
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            LandscapeNowPlayingDetails(state, palette, trackActions)
            NowPlayingInlineExtras(state, Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            PrimaryPlayerControls(
                state,
                onPlay,
                onStop,
                sleepTimerActions,
                audioOutputActions,
                isCompact = true,
            )
        }
        LandscapeStationSelector(state, onSelectStation)
    }
}

@Composable
private fun LandscapeNowPlayingDetails(
    state: MainUiState,
    palette: StationPalette,
    trackActions: TrackActions,
) {
    val metadata = parseNowPlayingMetadata(state.nowPlaying.displayTitle)
    val hasTrack = !state.nowPlaying.displayTitle.isNullOrBlank()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            state.selectedStation?.name ?: "24Seven.FM",
            color = palette.themedAccent(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (hasTrack) metadata.title else stationTagline(state.selectedStation),
            style = if (hasTrack) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("now_playing_title"),
        )
        metadata.artist?.let { artist ->
            Text(
                artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PlaybackStatusPill(state, palette)
            TrackActionButtons(state, trackActions, onArtwork = false)
        }
    }
}

@Composable
private fun LandscapeStationSelector(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
) {
    Column(
        Modifier
            .width(LandscapeStationSelectorWidth)
            .fillMaxHeight()
            .testTag("landscape_station_selector"),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.stations.forEach { station ->
            val selected = station.id == state.selectedStation?.id
            val palette = stationPalette(station.id)
            Card(
                onClick = { onSelectStation(station.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) palette.glow else MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) palette.accent else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        this.selected = selected
                        role = Role.RadioButton
                        contentDescription = if (selected) "${station.name}, selected" else station.name
                    }
                    .testTag("landscape_station_${station.id.value}"),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(stationSelectorLogoResource(station.id)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
    }
}

@DrawableRes
internal fun stationSelectorLogoResource(stationId: StationId): Int = when (stationId.value) {
    "1980s" -> R.drawable.station_logo_1980s
    "afm" -> R.drawable.station_logo_adagio
    "dfm" -> R.drawable.station_logo_death
    "efm" -> R.drawable.station_logo_entranced
    "sst" -> R.drawable.station_logo_sst
    else -> R.drawable.app_logo
}

@Composable
private fun ExpandedPlayerContent(
    state: MainUiState,
    palette: StationPalette,
    onSelectStation: (StationId) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    trackActions: TrackActions,
) {
    Row(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NowPlayingArtwork(state, palette, Modifier.fillMaxWidth().aspectRatio(1f), trackActions, onSelectStation = onSelectStation)
        }
        Column(
            Modifier.weight(1.15f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            NowPlayingDetails(state, palette, Alignment.Start, trackActions)
            Spacer(Modifier.height(28.dp))
            PrimaryPlayerControls(state, onPlay, onStop, sleepTimerActions, audioOutputActions)
            Spacer(Modifier.height(32.dp))
            Text("Choose a station", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            StationSelector(state, onSelectStation, edgePadding = 0.dp)
            state.selectedStation?.let { station ->
                Spacer(Modifier.height(20.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(station.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            station.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingArtwork(
    state: MainUiState,
    palette: StationPalette,
    modifier: Modifier = Modifier,
    trackActions: TrackActions? = null,
    showsOverlayCaption: Boolean = true,
    onSelectStation: ((StationId) -> Unit)? = null,
) {
    val artworkUrl = preferredPlayerArtworkUrl(
        nowPlayingArtworkUrl = state.nowPlaying.artworkUrl,
        station = state.selectedStation,
    )
    val hasAlbumArtwork = !state.nowPlaying.artworkUrl.isNullOrBlank()
    val swipe = if (onSelectStation != null && state.stations.size > 1) {
        Modifier.switchesStationOnSwipe(state.stations, state.selectedStation?.id, onSelectStation)
    } else {
        Modifier
    }
    Box(
        modifier
            .then(swipe)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(palette.glow, palette.secondary.copy(alpha = 0.48f)),
                ),
            )
            .padding(ArtworkFramePadding),
    ) {
        ArtworkImage(
            url = artworkUrl,
            fallback = painterResource(R.drawable.app_logo),
            contentDescription = if (hasAlbumArtwork) {
                "Album artwork"
            } else {
                "Selected station artwork"
            },
            contentScale = if (hasAlbumArtwork) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .testTag("now_playing_artwork")
                .opensAlbum(state.nowPlaying.albumId, state.nowPlaying.album, state.nowPlaying.artworkUrl),
        )
        // The favorite and rating buttons sit with the track details, so the cover stays uncovered.
        if (trackActions != null) {
            NowPlayingArtworkOverlay(
                state,
                actions = null,
                Modifier.matchParentSize().clip(RoundedCornerShape(22.dp)),
                showsOverlayCaption,
            )
        }
    }
}

/**
 * Dragging the artwork sideways turns the dial to the next or previous station, the way the removed arrows did.
 * The same two moves are offered as accessibility actions.
 */
@Composable
private fun Modifier.switchesStationOnSwipe(
    stations: List<Station>,
    selectedId: StationId?,
    onSelectStation: (StationId) -> Unit,
): Modifier {
    val threshold = with(LocalDensity.current) { StationSwipeThreshold.toPx() }
    val haptics = LocalHapticFeedback.current
    var dragged by remember { mutableFloatStateOf(0f) }
    fun turn(offset: Int) {
        adjacentStationId(stations, selectedId, offset)?.let {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onSelectStation(it)
        }
    }
    return this
        .draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta -> dragged += delta },
            onDragStarted = { dragged = 0f },
            onDragStopped = {
                // Dragging left pulls the next station in from the right.
                when {
                    dragged <= -threshold -> turn(1)
                    dragged >= threshold -> turn(-1)
                }
                dragged = 0f
            },
        )
        .semantics {
            customActions = listOf(
                CustomAccessibilityAction("Next station") { turn(1); true },
                CustomAccessibilityAction("Previous station") { turn(-1); true },
            )
        }
}

/** A list cover that fades in when it arrives instead of popping. */
@Composable
internal fun crossfadingImage(url: String?): ImageRequest =
    ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(ARTWORK_CROSSFADE_MILLIS).build()

/** Uses a station's verified identity when no track artwork has arrived yet. */
internal fun preferredPlayerArtworkUrl(
    nowPlayingArtworkUrl: String?,
    station: Station?,
): String? = nowPlayingArtworkUrl
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?: station?.logoUrl
        ?.trim()
        ?.takeIf(String::isNotEmpty)

@Composable
private fun NowPlayingDetails(
    state: MainUiState,
    palette: StationPalette,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    trackActions: TrackActions? = null,
) {
    val metadata = parseNowPlayingMetadata(state.nowPlaying.displayTitle)
    val hasTrack = !state.nowPlaying.displayTitle.isNullOrBlank()
    val textAlign = if (alignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start
    Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            state.selectedStation?.name ?: "24Seven.FM",
            color = palette.themedAccent(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Until a track is known the station's own tagline fills the title's place. A new title rises into place.
        AnimatedContent(
            targetState = if (hasTrack) metadata.title else stationTagline(state.selectedStation),
            transitionSpec = {
                (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 3 })
                    .togetherWith(fadeOut(tween(140)))
            },
            label = "now_playing_title",
        ) { title ->
            Text(
                title,
                style = if (hasTrack) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().testTag("now_playing_title"),
            )
        }
        metadata.artist?.let { artist ->
            Text(
                artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PlaybackStatusPill(state, palette)
            trackActions?.let { TrackActionButtons(state, it, onArtwork = false) }
        }
        CastRouteLabel(state)
    }
}

/** What the player says about a station before it knows the track. */
internal fun stationTagline(station: Station?): String =
    station?.description?.trim()?.takeIf(String::isNotEmpty) ?: "Live radio"

@Composable
internal fun CastRouteButton() {
    val context = LocalContext.current
    AndroidView(
        factory = { viewContext ->
            MediaRouteButton(
                ContextThemeWrapper(viewContext, R.style.Theme_TwentyFourSevenPlayer_Cast),
            ).apply {
                contentDescription = "Cast to a device"
                setUpCastRouteButton(context, this)
            }
        },
        modifier = Modifier.size(48.dp).testTag("cast_route_button"),
    )
}

@Composable
private fun CastRouteLabel(state: MainUiState) {
    if (state.playback.route == PlaybackRoute.Local) return
    Text(
        text = if (state.playback.route == PlaybackRoute.Casting) {
            "Casting to ${state.playback.castDeviceName ?: "device"}"
        } else {
            "Connected to ${state.playback.castDeviceName ?: "Cast device"}"
        },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("cast_route_status"),
    )
}

@Composable
private fun PlaybackStatusPill(state: MainUiState, palette: StationPalette) {
    if (state.playback.status == PlaybackStatus.Playing) return
    Surface(
        color = palette.glow.copy(alpha = 0.88f),
        contentColor = palette.accent,
        shape = RoundedCornerShape(100.dp),
        modifier = Modifier
            .semantics { stateDescription = state.playback.status.accessibleName }
            .testTag("playback_status"),
    ) {
        Text(
            playbackStatusMessage(state.playback.status, hasTrack = state.nowPlaying.displayTitle != null),
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Playback only: the sleep timer, Play in the station's colour, and the audio output. Stations are changed from the
 * station cards or by swiping the artwork, so the controls row no longer mixes the two.
 */
@Composable
private fun PrimaryPlayerControls(
    state: MainUiState,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    sleepTimerActions: SleepTimerActions,
    audioOutputActions: AudioOutputActions,
    isCompact: Boolean = false,
) {
    val isActive = state.playback.status.isActive
    val palette = stationPalette(state.selectedStation?.id)
    val supportingControlSize = if (isCompact) 48.dp else 56.dp
    val primaryControlSize = if (isCompact) 80.dp else 92.dp
    val haptics = LocalHapticFeedback.current
    val pressInteraction = remember { MutableInteractionSource() }
    val pressed by pressInteraction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "play_press")
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SleepTimerControl(state, sleepTimerActions)
        Spacer(Modifier.width(24.dp))
        val playbackDescription = if (isActive) "Stop radio" else "Play live radio"
        FilledIconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                if (isActive) onStop() else onPlay()
            },
            enabled = state.selectedStation?.streams?.isNotEmpty() == true,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = palette.themedAccent(),
                contentColor = palette.onAccent(),
            ),
            interactionSource = pressInteraction,
            modifier = Modifier
                .size(primaryControlSize)
                .scale(pressScale)
                .semantics { contentDescription = playbackDescription }
                .testTag("primary_play_pause"),
            shape = CircleShape,
        ) {
            // The glyph turns over from Play to Stop rather than swapping.
            AnimatedContent(
                targetState = isActive,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.6f))
                        .togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.6f))
                },
                label = "play_glyph",
            ) { active ->
                Icon(
                    if (active) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(if (isCompact) 40.dp else 46.dp),
                )
            }
        }
        Spacer(Modifier.width(24.dp))
        PlaybackAudioOutputControl(state, audioOutputActions, supportingControlSize)
    }
}

@Composable
private fun PlaybackAudioOutputControl(
    state: MainUiState,
    actions: AudioOutputActions,
    controlSize: androidx.compose.ui.unit.Dp,
) {
    val output = state.playback.audioOutput
    IconButton(
        onClick = actions.onOpenChooser,
        modifier = Modifier
            .size(controlSize)
            .semantics {
                contentDescription = "Choose audio output"
                stateDescription = "Current output: ${output.displayName}"
            }
            .testTag("audio_output_open"),
    ) {
        Icon(output.kind.icon, contentDescription = null)
    }
}

private val AudioOutputKind.icon: ImageVector
    get() = when (this) {
        AudioOutputKind.Device -> Icons.Default.Speaker
        AudioOutputKind.Bluetooth -> Icons.Default.BluetoothAudio
        AudioOutputKind.Wired -> Icons.Default.Headphones
        AudioOutputKind.Remote -> Icons.Default.CastConnected
    }

@Composable
private fun SleepTimerControl(state: MainUiState, actions: SleepTimerActions) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val sleepTimer = state.playback.sleepTimer
    IconButton(
            onClick = { showDialog = true },
            enabled = state.selectedStation?.streams?.isNotEmpty() == true,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = if (sleepTimer.isActive) "Adjust sleep timer" else "Set sleep timer"
                    if (sleepTimer.isActive) stateDescription = "Sleep timer active"
                }
                .testTag("sleep_timer_open"),
    ) {
            Icon(Icons.Default.Bedtime, contentDescription = null)
    }
    if (showDialog) {
        SleepTimerDialog(
            isAdjusting = sleepTimer.isActive,
            onSet = { durationMillis ->
                actions.onSet(durationMillis)
                showDialog = false
            },
            onCancel = if (sleepTimer.isActive) {
                {
                    actions.onCancel()
                    showDialog = false
                }
            } else {
                null
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun SleepTimerDialog(
    isAdjusting: Boolean,
    onSet: (Long) -> Unit,
    onCancel: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var customMinutes by rememberSaveable { mutableStateOf("") }
    val parsedMinutes = customMinutes.toLongOrNull()
    val customIsValid = parsedMinutes != null && parsedMinutes in 1L..720L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAdjusting) "Adjust sleep timer" else "Set sleep timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Playback stops when the countdown ends, even if the app is in the background.")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SleepTimerPresetsMinutes) { minutes ->
                        Button(
                            onClick = { onSet(minutes * 60_000L) },
                            modifier = Modifier.testTag("sleep_timer_preset_$minutes"),
                        ) {
                            Text("$minutes min")
                        }
                    }
                }
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { input -> customMinutes = input.filter(Char::isDigit).take(3) },
                    label = { Text("Custom minutes") },
                    supportingText = { Text("Enter 1–720 minutes") },
                    isError = customMinutes.isNotEmpty() && !customIsValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sleep_timer_custom_minutes"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedMinutes?.let { onSet(it * 60_000L) } },
                enabled = customIsValid,
                modifier = Modifier.testTag("sleep_timer_confirm_custom"),
            ) {
                Text(if (isAdjusting) "Update" else "Start")
            }
        },
        dismissButton = {
            Row {
                if (onCancel != null) {
                    TextButton(onClick = onCancel, modifier = Modifier.testTag("sleep_timer_cancel")) {
                        Text("Cancel timer")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Not now") }
            }
        },
    )
}

internal fun formatSleepTimerRemaining(remainingMillis: Long): String {
    val totalSeconds = ((remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

@Composable
private fun StationSelector(
    state: MainUiState,
    onSelectStation: (StationId) -> Unit,
    edgePadding: androidx.compose.ui.unit.Dp = 2.dp,
    isCompact: Boolean = false,
) {
    if (isCompact) {
        Row(
            Modifier.fillMaxWidth().testTag("station_selector"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.stations.forEach { station ->
                val selected = station.id == state.selectedStation?.id
                val palette = stationPalette(station.id)
                Card(
                    onClick = { onSelectStation(station.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) palette.glow else MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.accent else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .semantics {
                            this.selected = selected
                            role = Role.RadioButton
                            contentDescription = if (selected) "${station.name}, selected" else station.name
                        }
                        .testTag("station_card_${station.id.value}"),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(stationSelectorLogoResource(station.id)),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
            }
        }
    } else {
        LazyRow(
            Modifier.fillMaxWidth().testTag("station_selector"),
            contentPadding = PaddingValues(horizontal = edgePadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.stations, key = { it.id.value }) { station ->
            val selected = station.id == state.selectedStation?.id
            val palette = stationPalette(station.id)
            Card(
                onClick = { onSelectStation(station.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) palette.glow else MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.accent else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .width(152.dp)
                    .heightIn(min = 86.dp)
                    .semantics {
                        this.selected = selected
                        role = Role.RadioButton
                        contentDescription = if (selected) "${station.name}, selected" else station.name
                    }
                    .testTag("station_card_${station.id.value}"),
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        station.shortName,
                        color = palette.themedAccent(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        station.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("station_card_description_${station.id.value}"),
                    )
                }
            }
        }
        }
    }
}

@Composable
internal fun PersistentMiniPlayer(
    state: MainUiState,
    onSelectDestination: (MainDestination) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val palette = stationPalette(state.selectedStation?.id)
    val artworkUrl = preferredPlayerArtworkUrl(
        nowPlayingArtworkUrl = state.nowPlaying.artworkUrl,
        station = state.selectedStation,
    )
    val hasAlbumArtwork = !state.nowPlaying.artworkUrl.isNullOrBlank()
    val tint = palette.glow.copy(alpha = if (isSystemInDarkTheme()) 0.55f else 0.14f)
    Surface(
        onClick = { onSelectDestination(MainDestination.Player) },
        modifier = Modifier.fillMaxWidth().testTag("persistent_mini_player"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 5.dp,
    ) {
        Row(
            Modifier
                .background(Brush.horizontalGradient(listOf(tint, Color.Transparent)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkImage(
                url = artworkUrl,
                fallback = painterResource(R.drawable.app_logo),
                contentDescription = if (hasAlbumArtwork) "Now playing album artwork" else "Selected station artwork",
                contentScale = if (hasAlbumArtwork) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.nowPlaying.displayTitle ?: stationTagline(state.selectedStation),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.selectedStation?.shortName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.themedAccent(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledIconButton(
                onClick = if (state.playback.status.isActive) onPause else onPlay,
                enabled = state.selectedStation?.streams?.isNotEmpty() == true,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = palette.themedAccent(),
                    contentColor = palette.onAccent(),
                ),
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        contentDescription = if (state.playback.status.isActive) {
                            "Pause live radio"
                        } else {
                            "Play live radio"
                        }
                    },
            ) {
                Icon(
                    if (state.playback.status.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
            }
        }
    }
}

internal fun adjacentStationId(
    stations: List<Station>,
    selectedId: StationId?,
    offset: Int,
): StationId? {
    if (stations.isEmpty()) return null
    val selectedIndex = stations.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    val destinationIndex = Math.floorMod(selectedIndex + offset, stations.size)
    return stations[destinationIndex].id
}

@Immutable
private data class NowPlayingMetadata(val title: String, val artist: String?)

private fun parseNowPlayingMetadata(raw: String?): NowPlayingMetadata {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return NowPlayingMetadata("Live radio", null)
    val separator = value.indexOf(" - ")
    return if (separator > 0 && separator < value.lastIndex - 2) {
        NowPlayingMetadata(
            title = value.substring(separator + 3).trim(),
            artist = value.substring(0, separator).trim(),
        )
    } else {
        NowPlayingMetadata(value, null)
    }
}

private val PlaybackStatus.isActive: Boolean
    get() = this in setOf(
        PlaybackStatus.Connecting,
        PlaybackStatus.Buffering,
        PlaybackStatus.Playing,
        PlaybackStatus.Retrying,
        PlaybackStatus.WaitingForNetwork,
    )

private val PlaybackStatus.accessibleName: String
    get() = when (this) {
        PlaybackStatus.Idle -> "Ready"
        PlaybackStatus.Connecting -> "Connecting"
        PlaybackStatus.Buffering -> "Buffering"
        PlaybackStatus.Playing -> "Playing"
        PlaybackStatus.Paused -> "Paused"
        PlaybackStatus.Retrying -> "Reconnecting with fallback"
        PlaybackStatus.WaitingForNetwork -> "Waiting for network"
        PlaybackStatus.Error -> "Playback error"
    }

/** Before Play is pressed the Player already shows the station's current track, so it says so instead of "Not connected". */
internal fun playbackStatusMessage(status: PlaybackStatus, hasTrack: Boolean): String =
    if (status == PlaybackStatus.Idle && hasTrack) "On air now" else status.userMessage

private val PlaybackStatus.userMessage: String
    get() = when (this) {
        PlaybackStatus.Idle -> "Tap Play to listen"
        PlaybackStatus.Connecting -> "Connecting…"
        PlaybackStatus.Buffering -> "Buffering…"
        PlaybackStatus.Playing -> "Live"
        PlaybackStatus.Paused -> "Paused"
        PlaybackStatus.Retrying -> "Trying the backup stream…"
        PlaybackStatus.WaitingForNetwork -> "Waiting for network · resumes by itself"
        PlaybackStatus.Error -> "Couldn't play this station · try again"
    }
