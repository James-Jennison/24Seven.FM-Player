package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.StateFlow

const val CURRENT_APP_GUIDE_VERSION = 1

data class AppGuideState(
    val completedVersion: Int = 0,
    val shouldShowAutomatically: Boolean = false,
)

interface AppGuideRepository {
    val state: StateFlow<AppGuideState>

    suspend fun markCurrentVersionComplete()
}
