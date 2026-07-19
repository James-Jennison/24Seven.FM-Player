package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import com.codeframe78.twentyfourseven.player.domain.LocalStationPreferences
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationPreferencesRepository
import com.codeframe78.twentyfourseven.player.domain.StartupStationMode
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import com.codeframe78.twentyfourseven.player.domain.toSupportedStationIdOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesStationPreferencesRepository(
    context: Context,
    preferencesName: String = PREFERENCES_NAME,
) : StationPreferencesRepository {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(readPreferences())

    override val current: LocalStationPreferences
        get() = state.value

    override fun observePreferences(): Flow<LocalStationPreferences> = state.asStateFlow()

    override suspend fun recordLastStation(stationId: StationId) {
        val canonical = stationId.canonicalized()
        preferences.edit().putString(KEY_LAST_STATION, canonical.value).apply()
        state.value = state.value.copy(lastStationId = canonical)
    }

    override suspend fun setStartupPreference(mode: StartupStationMode, defaultStationId: StationId?) {
        val canonicalDefault = defaultStationId?.canonicalized()
        preferences.edit()
            .putString(KEY_STARTUP_MODE, mode.name)
            .apply {
                if (canonicalDefault == null) remove(KEY_DEFAULT_STATION)
                else putString(KEY_DEFAULT_STATION, canonicalDefault.value)
            }
            .apply()
        state.value = state.value.copy(startupMode = mode, defaultStationId = canonicalDefault)
    }

    private fun readPreferences() = LocalStationPreferences(
        startupMode = preferences.getString(KEY_STARTUP_MODE, null)
            ?.let { stored -> StartupStationMode.entries.firstOrNull { it.name == stored } }
            ?: StartupStationMode.LastSelected,
        defaultStationId = preferences.getString(KEY_DEFAULT_STATION, null)?.toSupportedStationIdOrNull(),
        lastStationId = preferences.getString(KEY_LAST_STATION, null)?.toSupportedStationIdOrNull(),
    ).also(::persistCanonicalIds)

    private fun persistCanonicalIds(value: LocalStationPreferences) {
        preferences.edit().apply {
            value.defaultStationId?.let { putString(KEY_DEFAULT_STATION, it.value) } ?: remove(KEY_DEFAULT_STATION)
            value.lastStationId?.let { putString(KEY_LAST_STATION, it.value) } ?: remove(KEY_LAST_STATION)
        }.apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "local_station_preferences"
        const val KEY_STARTUP_MODE = "startup_mode"
        const val KEY_DEFAULT_STATION = "default_station_id"
        const val KEY_LAST_STATION = "last_station_id"
    }
}

class InMemoryStationPreferencesRepository(
    initial: LocalStationPreferences = LocalStationPreferences(),
) : StationPreferencesRepository {
    private val state = MutableStateFlow(initial)

    override val current: LocalStationPreferences
        get() = state.value

    override fun observePreferences(): Flow<LocalStationPreferences> = state.asStateFlow()

    override suspend fun recordLastStation(stationId: StationId) {
        state.value = state.value.copy(lastStationId = stationId.canonicalized())
    }

    override suspend fun setStartupPreference(mode: StartupStationMode, defaultStationId: StationId?) {
        state.value = state.value.copy(startupMode = mode, defaultStationId = defaultStationId?.canonicalized())
    }
}
