package com.codeframe78.twentyfourseven.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.codeframe78.twentyfourseven.player.domain.StationId

private val Night = Color(0xFF0E0B14)
private val DeepNavy = Color(0xFF131522)
private val Charcoal = Color(0xFF1C1C27)
private val RaisedCharcoal = Color(0xFF282734)
private val WarmWhite = Color(0xFFF8F2F4)
private val MutedLavender = Color(0xFFCEC3D4)
private val RadioLavender = Color(0xFFC9A8FF)
private val RadioViolet = Color(0xFF8B5CF6)

@Composable
fun requestAvailableGreen(): Color = if (isSystemInDarkTheme()) Color(0xFF47C96B) else Color(0xFF146C36)

@Composable
fun requestUnavailableRed(): Color = if (isSystemInDarkTheme()) Color(0xFFFF6B76) else Color(0xFFB3261E)

private val DarkColors = darkColorScheme(
    primary = RadioLavender,
    onPrimary = Color(0xFF281342),
    primaryContainer = Color(0xFF3B2457),
    onPrimaryContainer = Color(0xFFEEDCFF),
    secondary = Color(0xFFFFC56C),
    onSecondary = Color(0xFF3E2800),
    background = Night,
    onBackground = WarmWhite,
    surface = DeepNavy,
    onSurface = WarmWhite,
    surfaceVariant = RaisedCharcoal,
    onSurfaceVariant = MutedLavender,
    surfaceContainer = Charcoal,
    surfaceContainerHigh = RaisedCharcoal,
    outline = Color(0xFF918A98),
    outlineVariant = Color(0xFF4C4652),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6942A0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECDDFF),
    onPrimaryContainer = Color(0xFF25113F),
    secondary = Color(0xFF805600),
    onSecondary = Color.White,
    background = Color(0xFFFFF8FC),
    onBackground = Color(0xFF201A20),
    surface = Color(0xFFFFF8FC),
    onSurface = Color(0xFF201A20),
    surfaceVariant = Color(0xFFF1E7F1),
    onSurfaceVariant = Color(0xFF4D454F),
    surfaceContainer = Color(0xFFF7EDF6),
    surfaceContainerHigh = Color(0xFFEFE5EE),
    outline = Color(0xFF7E747F),
    outlineVariant = Color(0xFFD1C3D0),
    error = Color(0xFFBA1A1A),
)

@Composable
fun TwentyFourSevenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/**
 * A station's colours. [accent] is tuned for dark surfaces; [accentOnLight] is the same hue deep enough to read on
 * the light theme. [themedAccent] picks the right one, and [onAccent] is what sits on a fill of it.
 */
@Immutable
data class StationPalette(
    val accent: Color,
    val secondary: Color,
    val glow: Color,
    val accentOnLight: Color = accent,
)

@Composable
fun StationPalette.themedAccent(): Color = if (isSystemInDarkTheme()) accent else accentOnLight

@Composable
fun StationPalette.onAccent(): Color = if (isSystemInDarkTheme()) Color(0xFF1C1424) else Color.White

/**
 * Each accent is the station website's own brand colour (its `theme-color`), so the app, the TV app and the sites agree.
 * Death.FM's pure red is lifted slightly so it stays readable on the dark surfaces.
 */
fun stationPalette(stationId: StationId?): StationPalette = when (stationId?.value) {
    "sst" -> StationPalette(
        accent = Color(0xFF99CCFF),
        secondary = Color(0xFF5B8FC7),
        glow = Color(0xFF12253D),
        accentOnLight = Color(0xFF1F5E99),
    )
    "1980s" -> StationPalette(
        accent = Color(0xFFFF99CC),
        secondary = Color(0xFFEE88BB),
        glow = Color(0xFF3A1530),
        accentOnLight = Color(0xFFB8337A),
    )
    "afm" -> StationPalette(
        accent = Color(0xFFFFCC99),
        secondary = Color(0xFFEEBB88),
        glow = Color(0xFF3D2A14),
        accentOnLight = Color(0xFF9A5A12),
    )
    "dfm" -> StationPalette(
        accent = Color(0xFFFF4D4D),
        secondary = Color(0xFFCC0000),
        glow = Color(0xFF3A0F12),
        accentOnLight = Color(0xFFB00020),
    )
    "efm" -> StationPalette(
        accent = Color(0xFFFFFF66),
        secondary = Color(0xFFB5B545),
        glow = Color(0xFF3A3A10),
        accentOnLight = Color(0xFF6B6B00),
    )
    else -> StationPalette(
        accent = RadioLavender,
        secondary = RadioViolet,
        glow = Color(0xFF2B1B3F),
        accentOnLight = Color(0xFF6942A0),
    )
}
