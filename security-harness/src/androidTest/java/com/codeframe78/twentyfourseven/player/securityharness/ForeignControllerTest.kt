package com.codeframe78.twentyfourseven.player.securityharness

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForeignControllerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun foreignPackageGetsTransportWithoutMediaOrSleepTimerMutation() {
        val token = SessionToken(context, ComponentName(PLAYER_PACKAGE, PLAYBACK_SERVICE))
        val controller = MediaController.Builder(context, token).buildAsync()
            .get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertTrue(controller.availableCommands.contains(Player.COMMAND_PLAY_PAUSE))
                assertTrue(controller.availableCommands.contains(Player.COMMAND_STOP))
                assertFalse(controller.availableCommands.contains(Player.COMMAND_SET_MEDIA_ITEM))
                assertFalse(controller.availableCommands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
                assertFalse(controller.availableSessionCommands.contains(SET_SLEEP_TIMER))
                assertFalse(controller.availableSessionCommands.contains(CANCEL_SLEEP_TIMER))
            }

            var originalMediaId: String? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                originalMediaId = controller.currentMediaItem?.mediaId
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId("foreign:injected")
                        .setUri("https://example.invalid/injected")
                        .build(),
                )
            }
            var resultingMediaId: String? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                resultingMediaId = controller.currentMediaItem?.mediaId
            }
            assertEquals(originalMediaId, resultingMediaId)

            lateinit var setFuture: ListenableFuture<SessionResult>
            lateinit var cancelFuture: ListenableFuture<SessionResult>
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                setFuture = controller.sendCustomCommand(SET_SLEEP_TIMER, Bundle.EMPTY)
                cancelFuture = controller.sendCustomCommand(CANCEL_SLEEP_TIMER, Bundle.EMPTY)
            }
            val setResult = setFuture.get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            val cancelResult = cancelFuture.get(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            assertTrue(setResult.resultCode != SessionResult.RESULT_SUCCESS)
            assertTrue(cancelResult.resultCode != SessionResult.RESULT_SUCCESS)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.release() }
        }
    }

    private companion object {
        const val PLAYER_PACKAGE = "com.codeframe78.twentyfourseven.player.debug"
        const val PLAYBACK_SERVICE = "com.codeframe78.twentyfourseven.player.playback.RadioPlaybackService"
        const val CONNECTION_TIMEOUT_SECONDS = 10L
        val SET_SLEEP_TIMER = SessionCommand(
            "com.codeframe78.twentyfourseven.player.sleep_timer.SET",
            Bundle.EMPTY,
        )
        val CANCEL_SLEEP_TIMER = SessionCommand(
            "com.codeframe78.twentyfourseven.player.sleep_timer.CANCEL",
            Bundle.EMPTY,
        )
    }
}
