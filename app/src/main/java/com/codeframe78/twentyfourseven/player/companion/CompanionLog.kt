package com.codeframe78.twentyfourseven.player.companion

import android.util.Log

/** Diagnostics for TV pairing. Frame types and failures only: never codes, proofs, secrets or track text. */
object CompanionLog {
    private const val TAG = "TvCompanion"

    fun d(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    fun e(message: String, failure: Throwable? = null) {
        runCatching { Log.e(TAG, message, failure) }
    }
}
