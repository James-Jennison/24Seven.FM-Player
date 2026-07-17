package com.codeframe78.twentyfourseven.player.playback

import androidx.mediarouter.media.MediaRouter
import com.codeframe78.twentyfourseven.player.domain.AudioOutputKind
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidAudioOutputMonitorTest {
    @Test
    fun `system route types map to stable user-facing categories`() {
        assertEquals(
            AudioOutputKind.Device,
            outputKind(MediaRouter.RouteInfo.DEVICE_TYPE_BUILTIN_SPEAKER),
        )
        assertEquals(
            AudioOutputKind.Bluetooth,
            outputKind(MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP),
        )
        assertEquals(
            AudioOutputKind.Wired,
            outputKind(MediaRouter.RouteInfo.DEVICE_TYPE_USB_HEADSET),
        )
    }

    @Test
    fun `remote playback takes precedence over receiver device type`() {
        val state = audioOutputState(
            routeName = "Living room",
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_REMOTE_SPEAKER,
            playbackType = MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE,
            isSystemRoute = false,
        )

        assertEquals("Living room", state.displayName)
        assertEquals(AudioOutputKind.Remote, state.kind)
    }

    @Test
    fun `blank route names use a privacy-safe device fallback`() {
        val state = audioOutputState(
            routeName = "  ",
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            playbackType = MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL,
            isSystemRoute = true,
        )

        assertEquals("This device", state.displayName)
        assertEquals(AudioOutputKind.Device, state.kind)
    }

    private fun outputKind(deviceType: Int) = audioOutputState(
        routeName = "Output",
        deviceType = deviceType,
        playbackType = MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL,
        isSystemRoute = true,
    ).kind
}
