package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StartupStationMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesStationPreferencesRepositoryTest {
    @Test
    fun preferencesSurviveRepositoryRecreationAndRemainDeviceLocal() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "m14-station-preferences-test"
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        try {
            SharedPreferencesStationPreferencesRepository(context, name).apply {
                recordLastStation(StationId("afm"))
                setStartupPreference(StartupStationMode.Fixed, StationId("dfm"))
            }

            val restored = SharedPreferencesStationPreferencesRepository(context, name).current

            assertEquals(StationId("afm"), restored.lastStationId)
            assertEquals(StartupStationMode.Fixed, restored.startupMode)
            assertEquals(StationId("dfm"), restored.defaultStationId)
        } finally {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test
    fun unknownPersistedModeFallsBackToLastSelectedBehavior() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "m14-station-preferences-corrupt-test"
        val stored = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        stored.edit().clear().putString("startup_mode", "REMOVED_MODE").putString("last_station_id", "efm").commit()
        try {
            val restored = SharedPreferencesStationPreferencesRepository(context, name).current

            assertEquals(StartupStationMode.LastSelected, restored.startupMode)
            assertEquals(StationId("efm"), restored.lastStationId)
        } finally {
            stored.edit().clear().commit()
        }
    }

    @Test
    fun legacyStationIdsAreCanonicalizedAndRewritten() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "m32-station-id-migration-test"
        val stored = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        stored.edit().clear()
            .putString("startup_mode", StartupStationMode.Fixed.name)
            .putString("last_station_id", "entranced")
            .putString("default_station_id", "death")
            .commit()
        try {
            val restored = SharedPreferencesStationPreferencesRepository(context, name).current

            assertEquals(StationId("efm"), restored.lastStationId)
            assertEquals(StationId("dfm"), restored.defaultStationId)
            assertEquals("efm", stored.getString("last_station_id", null))
            assertEquals("dfm", stored.getString("default_station_id", null))

            stored.edit().putString("last_station_id", "unknown").remove("default_station_id").commit()
            val unknown = SharedPreferencesStationPreferencesRepository(context, name).current
            assertNull(unknown.lastStationId)
            assertNull(stored.getString("last_station_id", null))
        } finally {
            stored.edit().clear().commit()
        }
    }
}
