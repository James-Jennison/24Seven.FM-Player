package com.codeframe78.twentyfourseven.player.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioPlaybackServiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test
    fun sessionHidesFallbackNavigationAndReconnectsAfterServiceStop() {
        val serviceIntent = Intent(context, RadioPlaybackService::class.java)
        assertNotNull(context.startService(serviceIntent))

        connectController().useOnMainThread { controller ->
            val commands = controller.availableCommands
            assertNotNull(controller.sessionActivity)
            assertTrue(commands.contains(Player.COMMAND_PLAY_PAUSE))
            assertFalse(commands.contains(Player.COMMAND_SEEK_TO_NEXT))
            assertFalse(commands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
            assertFalse(commands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))
            assertFalse(commands.contains(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
        }

        assertTrue(context.stopService(serviceIntent))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        connectController().useOnMainThread { controller ->
            assertTrue(controller.availableCommands.contains(Player.COMMAND_PLAY_PAUSE))
        }
    }

    @Test
    fun customCommandStartsTimerAndManualStopCancelsIt() {
        context.getSharedPreferences(SLEEP_TIMER_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        val controller = connectController()
        try {
            var supportsTimerCommands = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                supportsTimerCommands =
                    controller.availableSessionCommands.contains(SleepTimerSessionContract.setCommand) &&
                    controller.availableSessionCommands.contains(SleepTimerSessionContract.cancelCommand)
            }
            assertTrue(supportsTimerCommands)

            lateinit var commandResult: ListenableFuture<SessionResult>
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                commandResult = controller.sendCustomCommand(
                    SleepTimerSessionContract.setCommand,
                    SleepTimerSessionContract.setArguments(60_000L),
                )
            }
            assertTrue(commandResult.get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS).resultCode == SessionResult.RESULT_SUCCESS)
            assertTrue(awaitTimerState(controller) { it.isActive })

            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.stop() }
            assertTrue(awaitTimerState(controller) { !it.isActive })
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.release() }
            context.getSharedPreferences(SLEEP_TIMER_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test
    fun activeTimerRestoresWhenPlaybackServiceIsRecreated() {
        val serviceIntent = Intent(context, RadioPlaybackService::class.java)
        val preferences = context.getSharedPreferences(SLEEP_TIMER_PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        assertNotNull(context.startService(serviceIntent))
        val firstController = connectController()
        try {
            lateinit var commandResult: ListenableFuture<SessionResult>
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                commandResult = firstController.sendCustomCommand(
                    SleepTimerSessionContract.setCommand,
                    SleepTimerSessionContract.setArguments(60_000L),
                )
            }
            assertTrue(commandResult.get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS).resultCode == SessionResult.RESULT_SUCCESS)
            assertTrue(awaitTimerState(firstController) { it.isActive })
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { firstController.release() }
        }

        assertTrue(context.stopService(serviceIntent))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val restoredController = connectController()
        try {
            assertTrue(awaitTimerState(restoredController) { it.isActive && it.remainingMillis <= 60_000L })
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                restoredController.sendCustomCommand(SleepTimerSessionContract.cancelCommand, Bundle.EMPTY)
            }
            assertTrue(awaitTimerState(restoredController) { !it.isActive })
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { restoredController.release() }
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun timerExpiryPublishesExpiryAndClearsActiveState() {
        val preferences = context.getSharedPreferences(SLEEP_TIMER_PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val expired = CountDownLatch(1)
        val controller = connectController(
            object : MediaController.Listener {
                @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
                override fun onCustomCommand(
                    controller: MediaController,
                    command: androidx.media3.session.SessionCommand,
                    args: Bundle,
                ) = if (command == SleepTimerSessionContract.expiredCommand) {
                    expired.countDown()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                } else {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                }
            },
        )
        try {
            lateinit var commandResult: ListenableFuture<SessionResult>
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                commandResult = controller.sendCustomCommand(
                    SleepTimerSessionContract.setCommand,
                    SleepTimerSessionContract.setArguments(60_000L),
                )
            }
            assertTrue(commandResult.get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS).resultCode == SessionResult.RESULT_SUCCESS)
            assertTrue(awaitTimerState(controller) { it.isActive })
            assertTrue(expired.await(65L, TimeUnit.SECONDS))
            assertTrue(awaitTimerState(controller) { !it.isActive })
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.release() }
            preferences.edit().clear().commit()
        }
    }

    private fun connectController(
        listener: MediaController.Listener? = null,
    ): MediaController {
        val token = SessionToken(context, ComponentName(context, RadioPlaybackService::class.java))
        val builder = MediaController.Builder(context, token)
        if (listener != null) builder.setListener(listener)
        return builder.buildAsync()
            .get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun MediaController.useOnMainThread(block: (MediaController) -> Unit) {
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { block(this) }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { release() }
        }
    }

    private fun awaitTimerState(
        controller: MediaController,
        predicate: (com.codeframe78.twentyfourseven.player.domain.SleepTimerState) -> Boolean,
    ): Boolean {
        repeat(50) {
            var matches = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                matches = predicate(SleepTimerSessionContract.state(controller.sessionExtras))
            }
            if (matches) return true
            Thread.sleep(100L)
        }
        return false
    }

    private companion object {
        const val CONNECTION_TIMEOUT_SECONDS = 10L
    }
}
