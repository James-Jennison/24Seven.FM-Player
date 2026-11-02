package com.codeframe78.twentyfourseven.player.companion

import android.util.Log

/** Diagnostics for TV pairing. Frame types and failures only: never codes, proofs, secrets or track text. */
object CompanionLog {
    private const val TAG = "TvCompanion"

    /** Receives every message too; set only by tests, to assert nothing sensitive is logged. */
    @Volatile internal var sink: ((String) -> Unit)? = null

    fun d(message: String) {
        sink?.invoke(message)
        runCatching { Log.d(TAG, message) }
    }

    fun e(message: String, failure: Throwable? = null) {
        sink?.invoke(message + (failure?.let { " " + it } ?: ""))
        runCatching { Log.e(TAG, message, failure) }
    }
}
