package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.AudioOutputKind
import com.codeframe78.twentyfourseven.player.domain.AudioOutputState
import com.codeframe78.twentyfourseven.player.domain.PlaybackState
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {
    @Test
    fun `report uses an allowlist and excludes raw errors and route names`() {
        val report = buildDiagnosticReport(
            environment = DiagnosticEnvironment(
                appVersion = "0.1.0-alpha01-debug",
                versionCode = 2,
                buildType = "debug",
                androidRelease = "16",
                apiLevel = 36,
                deviceManufacturer = "motorola",
                deviceModel = "razr plus 2024",
            ),
            stationName = "StreamingSoundtracks.com",
            playback = PlaybackState(
                status = PlaybackStatus.Error,
                errorMessage = "https://private.example/?token=secret-value",
                networkAvailable = true,
                audioOutput = AudioOutputState("James's private JBL", AudioOutputKind.Bluetooth),
            ),
            transitions = listOf(
                DiagnosticTransition(PlaybackStatus.Buffering, true, AudioOutputKind.Device),
                DiagnosticTransition(PlaybackStatus.Error, true, AudioOutputKind.Bluetooth),
            ),
        )

        assertTrue(report.contains("Error category: Playback failure"))
        assertTrue(report.contains("Audio output: Bluetooth"))
        assertTrue(report.contains("StreamingSoundtracks.com"))
        assertFalse(report.contains("private.example"))
        assertFalse(report.contains("secret-value"))
        assertFalse(report.contains("James"))
    }

    @Test
    fun `report bounds and sanitizes externally supplied labels and transition history`() {
        val report = buildDiagnosticReport(
            environment = DiagnosticEnvironment(
                appVersion = "line one\nline two" + "x".repeat(100),
                deviceManufacturer = "Maker\r\nInjected",
            ),
            stationName = "Station\nInjected",
            playback = PlaybackState(),
            transitions = List(7) {
                DiagnosticTransition(PlaybackStatus.Idle, false, AudioOutputKind.Device)
            },
        )

        assertFalse(report.contains("\nline two"))
        assertFalse(report.contains("\nInjected"))
        assertFalse(report.contains("6. "))
        assertTrue(report.contains("5. Idle"))
    }
}
