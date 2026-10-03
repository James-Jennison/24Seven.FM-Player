package com.codeframe78.twentyfourseven.player.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommands

internal enum class ControllerAccess {
    LocalApp,
    Automotive,
    TrustedSystem,
    Foreign,
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal object MediaSessionControllerPolicy {
    fun access(
        controller: MediaSession.ControllerInfo,
        applicationPackageName: String,
    ): ControllerAccess = access(
        controller.packageName,
        controller.isTrusted,
        controller.isPackageNameVerified,
        applicationPackageName,
    )

    internal fun access(
        controllerPackageName: String,
        isTrusted: Boolean,
        isPackageNameVerified: Boolean,
        applicationPackageName: String,
    ): ControllerAccess = when {
        // A Media3 controller supplies its own package name, so only a verified name earns full control.
        isPackageNameVerified && controllerPackageName == applicationPackageName -> ControllerAccess.LocalApp
        // Media3 verifies a platform session controller's package name only on Android 13 and later, so Android
        // Auto's play requests arrive unverified on older releases. A caller the system trusts for media control
        // may use the Android Auto name there: the role only selects stations from the Player's own catalog.
        controllerPackageName in ANDROID_AUTO_HOST_PACKAGES && (isPackageNameVerified || isTrusted) ->
            ControllerAccess.Automotive
        isTrusted -> ControllerAccess.TrustedSystem
        else -> ControllerAccess.Foreign
    }

    fun playerCommands(base: Player.Commands, access: ControllerAccess): Player.Commands {
        if (access == ControllerAccess.LocalApp) return base
        return Player.Commands.Builder().apply {
            APPROVED_EXTERNAL_PLAYER_COMMANDS.forEach { command ->
                if (base.contains(command)) add(command)
            }
            if (access == ControllerAccess.Automotive) {
                AUTOMOTIVE_CATALOG_PLAYER_COMMANDS.forEach { command ->
                    if (base.contains(command)) add(command)
                }
            }
        }.build()
    }

    /**
     * The playlist holds one station's stream and its fallbacks, which is not a queue a listener can use. Media3
     * publishes the platform session's queue, which a car display offers as a Queue button, only while its
     * notification controller may read the timeline.
     */
    fun withoutPublishedQueue(commands: Player.Commands): Player.Commands =
        commands.buildUpon().remove(Player.COMMAND_GET_TIMELINE).build()

    fun sessionCommands(base: SessionCommands, access: ControllerAccess): SessionCommands =
        base.buildUpon().apply {
            when (access) {
                ControllerAccess.LocalApp -> {
                    add(SleepTimerSessionContract.setCommand)
                    add(SleepTimerSessionContract.cancelCommand)
                    add(CastHandoffSessionContract.stopLocalPlaybackCommand)
                }

                ControllerAccess.Automotive -> Unit
                ControllerAccess.TrustedSystem -> add(SleepTimerSessionContract.cancelCommand)
                ControllerAccess.Foreign -> Unit
            }
        }.build()

    fun maySetSleepTimer(access: ControllerAccess) = access == ControllerAccess.LocalApp

    fun mayCancelSleepTimer(access: ControllerAccess) =
        access == ControllerAccess.LocalApp || access == ControllerAccess.TrustedSystem

    fun mayHandOffToCast(access: ControllerAccess) = access == ControllerAccess.LocalApp

    fun mayChangeMedia(access: ControllerAccess) =
        access == ControllerAccess.LocalApp || access == ControllerAccess.Automotive

    private val ANDROID_AUTO_HOST_PACKAGES = setOf(
        "com.google.android.projection.gearhead",
    )

    private val APPROVED_EXTERNAL_PLAYER_COMMANDS = listOf(
        Player.COMMAND_PLAY_PAUSE,
        Player.COMMAND_PREPARE,
        Player.COMMAND_STOP,
        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
        Player.COMMAND_GET_TIMELINE,
        Player.COMMAND_GET_METADATA,
        Player.COMMAND_GET_AUDIO_ATTRIBUTES,
        Player.COMMAND_GET_VOLUME,
        Player.COMMAND_GET_DEVICE_VOLUME,
        Player.COMMAND_GET_TEXT,
        Player.COMMAND_GET_TRACKS,
    )

    private val AUTOMOTIVE_CATALOG_PLAYER_COMMANDS = listOf(
        Player.COMMAND_SET_MEDIA_ITEM,
        Player.COMMAND_CHANGE_MEDIA_ITEMS,
    )
}
