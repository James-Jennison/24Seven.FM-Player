package com.codeframe78.twentyfourseven.player.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaSessionControllerPolicyTest {
    private val playerCommands = Player.Commands.Builder()
        .add(Player.COMMAND_PLAY_PAUSE)
        .add(Player.COMMAND_PREPARE)
        .add(Player.COMMAND_STOP)
        .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
        .add(Player.COMMAND_SET_MEDIA_ITEM)
        .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
        .build()

    @Test
    fun foreignControllerReceivesTransportButNoMutationOrTimerAuthority() {
        val commands = MediaSessionControllerPolicy.playerCommands(playerCommands, ControllerAccess.Foreign)
        val sessionCommands = MediaSessionControllerPolicy.sessionCommands(
            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
            ControllerAccess.Foreign,
        )

        assertTrue(commands.contains(Player.COMMAND_PLAY_PAUSE))
        assertTrue(commands.contains(Player.COMMAND_STOP))
        assertFalse(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertFalse(commands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
        assertFalse(sessionCommands.contains(SleepTimerSessionContract.setCommand))
        assertFalse(sessionCommands.contains(SleepTimerSessionContract.cancelCommand))
        assertFalse(MediaSessionControllerPolicy.mayChangeMedia(ControllerAccess.Foreign))
    }

    @Test
    fun trustedSystemCanCancelActiveTimerButCannotSetTimerOrMedia() {
        val commands = MediaSessionControllerPolicy.playerCommands(playerCommands, ControllerAccess.TrustedSystem)
        val sessionCommands = MediaSessionControllerPolicy.sessionCommands(
            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
            ControllerAccess.TrustedSystem,
        )

        assertFalse(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertFalse(commands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
        assertFalse(sessionCommands.contains(SleepTimerSessionContract.setCommand))
        assertTrue(sessionCommands.contains(SleepTimerSessionContract.cancelCommand))
        assertTrue(MediaSessionControllerPolicy.mayCancelSleepTimer(ControllerAccess.TrustedSystem))
    }

    @Test
    fun localAppRetainsStationAndTimerCommands() {
        val commands = MediaSessionControllerPolicy.playerCommands(playerCommands, ControllerAccess.LocalApp)
        val sessionCommands = MediaSessionControllerPolicy.sessionCommands(
            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
            ControllerAccess.LocalApp,
        )

        assertTrue(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
        assertTrue(sessionCommands.contains(SleepTimerSessionContract.setCommand))
        assertTrue(sessionCommands.contains(SleepTimerSessionContract.cancelCommand))
    }
}
