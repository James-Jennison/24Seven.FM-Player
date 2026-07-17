package com.codeframe78.twentyfourseven.player.playback

import android.content.Context
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.codeframe78.twentyfourseven.player.domain.AudioOutputKind
import com.codeframe78.twentyfourseven.player.domain.AudioOutputState

internal class AndroidAudioOutputMonitor(
    context: Context,
    private val onOutputChanged: (AudioOutputState) -> Unit,
) {
    private val mediaRouter = MediaRouter.getInstance(context.applicationContext)
    private val selector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
        .build()
    private val callback = object : MediaRouter.Callback() {
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
            publish(router.selectedRoute)
        }

        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) {
            publish(router.selectedRoute)
        }

        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
            if (route.isSelected) publish(route)
        }
    }

    init {
        mediaRouter.addCallback(
            selector,
            callback,
            MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS,
        )
        publish(mediaRouter.selectedRoute)
    }

    private fun publish(route: MediaRouter.RouteInfo) {
        onOutputChanged(
            audioOutputState(
                routeName = route.name,
                deviceType = route.deviceType,
                playbackType = route.playbackType,
                isSystemRoute = route.isSystemRoute,
            ),
        )
    }
}

internal fun audioOutputState(
    routeName: CharSequence?,
    deviceType: Int,
    playbackType: Int,
    isSystemRoute: Boolean,
): AudioOutputState {
    val kind = when {
        playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE || !isSystemRoute -> AudioOutputKind.Remote
        deviceType in bluetoothDeviceTypes -> AudioOutputKind.Bluetooth
        deviceType in wiredDeviceTypes -> AudioOutputKind.Wired
        deviceType in localDeviceTypes -> AudioOutputKind.Device
        else -> AudioOutputKind.Device
    }
    val displayName = routeName?.toString()?.trim().orEmpty().ifEmpty { "This device" }
    return AudioOutputState(
        displayName = displayName,
        kind = kind,
    )
}

private val bluetoothDeviceTypes = setOf(
    MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH_A2DP,
    MediaRouter.RouteInfo.DEVICE_TYPE_BLE_HEADSET,
    MediaRouter.RouteInfo.DEVICE_TYPE_HEARING_AID,
    MediaRouter.RouteInfo.DEVICE_TYPE_CAR,
)

private val wiredDeviceTypes = setOf(
    MediaRouter.RouteInfo.DEVICE_TYPE_WIRED_HEADSET,
    MediaRouter.RouteInfo.DEVICE_TYPE_WIRED_HEADPHONES,
    MediaRouter.RouteInfo.DEVICE_TYPE_USB_DEVICE,
    MediaRouter.RouteInfo.DEVICE_TYPE_USB_ACCESSORY,
    MediaRouter.RouteInfo.DEVICE_TYPE_USB_HEADSET,
    MediaRouter.RouteInfo.DEVICE_TYPE_HDMI,
    MediaRouter.RouteInfo.DEVICE_TYPE_HDMI_ARC,
    MediaRouter.RouteInfo.DEVICE_TYPE_HDMI_EARC,
    MediaRouter.RouteInfo.DEVICE_TYPE_DOCK,
    MediaRouter.RouteInfo.DEVICE_TYPE_AUDIO_VIDEO_RECEIVER,
)

private val localDeviceTypes = setOf(
    MediaRouter.RouteInfo.DEVICE_TYPE_BUILTIN_SPEAKER,
    MediaRouter.RouteInfo.DEVICE_TYPE_SMARTPHONE,
    MediaRouter.RouteInfo.DEVICE_TYPE_TABLET,
    MediaRouter.RouteInfo.DEVICE_TYPE_TABLET_DOCKED,
)
