package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import android.content.pm.PackageInfo
import com.codeframe78.twentyfourseven.player.domain.AppGuideRepository
import com.codeframe78.twentyfourseven.player.domain.AppGuideState
import com.codeframe78.twentyfourseven.player.domain.CURRENT_APP_GUIDE_VERSION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class SharedPreferencesAppGuideRepository(
    context: Context,
    packageInfo: PackageInfo,
    private val currentVersion: Int = CURRENT_APP_GUIDE_VERSION,
) : AppGuideRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(readState(packageInfo))

    override val state: StateFlow<AppGuideState> = mutableState.asStateFlow()

    override suspend fun markCurrentVersionComplete() {
        preferences.edit().putInt(KEY_COMPLETED_VERSION, currentVersion).apply()
        mutableState.value = AppGuideState(
            completedVersion = currentVersion,
            shouldShowAutomatically = false,
        )
    }

    private fun readState(packageInfo: PackageInfo): AppGuideState {
        val hasStoredCompletion = preferences.contains(KEY_COMPLETED_VERSION)
        val completed = if (hasStoredCompletion) {
            preferences.getInt(KEY_COMPLETED_VERSION, 0)
        } else if (isExistingInstallation(packageInfo)) {
            currentVersion.also {
                preferences.edit().putInt(KEY_COMPLETED_VERSION, it).apply()
            }
        } else {
            0
        }
        return AppGuideState(
            completedVersion = completed,
            shouldShowAutomatically = shouldAutoShowAppGuide(
                completedVersion = completed,
                currentVersion = currentVersion,
                hasStoredCompletion = hasStoredCompletion,
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime,
            ),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "app_guide_preferences"
        const val KEY_COMPLETED_VERSION = "completed_app_guide_version"
    }
}

internal fun shouldAutoShowAppGuide(
    completedVersion: Int,
    currentVersion: Int,
    hasStoredCompletion: Boolean,
    firstInstallTime: Long,
    lastUpdateTime: Long,
): Boolean {
    if (completedVersion >= currentVersion) return false
    return hasStoredCompletion || !isExistingInstallation(firstInstallTime, lastUpdateTime)
}

private fun isExistingInstallation(packageInfo: PackageInfo): Boolean =
    isExistingInstallation(packageInfo.firstInstallTime, packageInfo.lastUpdateTime)

private fun isExistingInstallation(firstInstallTime: Long, lastUpdateTime: Long): Boolean =
    abs(lastUpdateTime - firstInstallTime) > 5_000L
