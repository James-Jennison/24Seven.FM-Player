package com.codeframe78.twentyfourseven.player.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

/**
 * Lets the Player hand playback to a Cast device without ending the listening session.
 *
 * An ordinary Stop is a listener decision and cancels the sleep timer. Stopping the local player because the audio
 * moved to a Cast device is not, so it travels as its own command and leaves the timer running.
 */
internal object CastHandoffSessionContract {
    val stopLocalPlaybackCommand = SessionCommand(
        "com.codeframe78.twentyfourseven.player.cast_handoff.STOP_LOCAL_PLAYBACK",
        Bundle.EMPTY,
    )
}
