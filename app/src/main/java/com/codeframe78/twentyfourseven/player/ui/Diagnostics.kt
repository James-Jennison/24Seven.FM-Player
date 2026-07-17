package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.runtime.Immutable
import com.codeframe78.twentyfourseven.player.domain.AudioOutputKind
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus

private const val MaximumDiagnosticValueLength = 80
private const val MaximumDiagnosticTransitions = 5

@Immutable
internal data class DiagnosticEnvironment(
    val appVersion: String = "Unknown",
    val versionCode: Long = 0L,
    val buildType: String = "Unknown",
    val androidRelease: String = "Unknown",
    val apiLevel: Int = 0,
    val deviceManufacturer: String = "Unknown",
    val deviceModel: String = "Unknown",
)

@Immutable
internal data class DiagnosticActions(
    val onCopy: (String) -> Unit = {},
    val onShare: (String) -> Unit = {},
)

@Immutable
internal data class DiagnosticUi(
    val environment: DiagnosticEnvironment = DiagnosticEnvironment(),
    val actions: DiagnosticActions = DiagnosticActions(),
)

@Immutable
data class DiagnosticTransition(
    val playbackStatus: PlaybackStatus,
    val networkAvailable: Boolean,
    val audioOutputKind: AudioOutputKind,
)

internal fun buildDiagnosticReport(
    environment: DiagnosticEnvironment,
    stationName: String?,
    playback: PlaybackState,
    transitions: List<DiagnosticTransition>,
): String = buildString {
    appendLine("24Seven.FM Player diagnostics")
    appendLine("App version: ${safeDiagnosticValue(environment.appVersion)} (${environment.versionCode.coerceAtLeast(0L)})")
    appendLine("Build type: ${safeDiagnosticValue(environment.buildType)}")
    appendLine("Android: ${safeDiagnosticValue(environment.androidRelease)} (API ${environment.apiLevel.coerceAtLeast(0)})")
    appendLine(
        "Device: ${safeDiagnosticValue(environment.deviceManufacturer)} ${safeDiagnosticValue(environment.deviceModel)}",
    )
    appendLine("Station: ${safeDiagnosticValue(stationName ?: "None selected")}")
    appendLine("Playback: ${playback.status.diagnosticLabel()}")
    appendLine("Error category: ${playback.errorCategoryLabel()}")
    appendLine("Network: ${if (playback.networkAvailable) "Available" else "Unavailable"}")
    appendLine("Audio output: ${playback.audioOutput.kind.diagnosticLabel()}")
    appendLine("Recent playback transitions (oldest to newest):")
    transitions.takeLast(MaximumDiagnosticTransitions).forEachIndexed { index, transition ->
        appendLine(
            "${index + 1}. ${transition.playbackStatus.diagnosticLabel()} | " +
                "${if (transition.networkAvailable) "Network available" else "Network unavailable"} | " +
                transition.audioOutputKind.diagnosticLabel(),
        )
    }
    if (transitions.isEmpty()) appendLine("None recorded")
    append("Privacy: This snapshot excludes account data, messages, content, URLs, identifiers, raw errors, and logs.")
}

private fun safeDiagnosticValue(value: String): String = value
    .asSequence()
    .filterNot(Char::isISOControl)
    .joinToString("")
    .trim()
    .ifEmpty { "Unknown" }
    .take(MaximumDiagnosticValueLength)

private fun PlaybackState.errorCategoryLabel(): String = when (status) {
    PlaybackStatus.WaitingForNetwork -> "Network unavailable"
    PlaybackStatus.Error -> "Playback failure"
    else -> "None"
}

private fun PlaybackStatus.diagnosticLabel(): String = when (this) {
    PlaybackStatus.Idle -> "Idle"
    PlaybackStatus.Connecting -> "Connecting"
    PlaybackStatus.Buffering -> "Buffering"
    PlaybackStatus.Playing -> "Playing"
    PlaybackStatus.Paused -> "Paused"
    PlaybackStatus.Retrying -> "Retrying alternate stream"
    PlaybackStatus.WaitingForNetwork -> "Waiting for network"
    PlaybackStatus.Error -> "Error"
}

private fun AudioOutputKind.diagnosticLabel(): String = when (this) {
    AudioOutputKind.Device -> "This device"
    AudioOutputKind.Bluetooth -> "Bluetooth"
    AudioOutputKind.Wired -> "Wired or USB"
    AudioOutputKind.Remote -> "System-managed remote output"
}
