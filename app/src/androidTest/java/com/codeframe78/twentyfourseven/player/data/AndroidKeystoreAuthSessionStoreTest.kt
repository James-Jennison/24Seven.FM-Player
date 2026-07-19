package com.codeframe78.twentyfourseven.player.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codeframe78.twentyfourseven.player.domain.StationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpCookie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreAuthSessionStoreTest {
    @Test
    fun encryptedSessionRoundTripsAndClears() {
        val store = AndroidKeystoreAuthSessionStore(ApplicationProvider.getApplicationContext())
        val stationId = StationId("keystore-test")
        store.clear(stationId)
        val cookie = HttpCookie("session", "instrumentation-only-value").apply {
            path = "/"
            isHttpOnly = true
        }

        store.save(stationId, "streamingsoundtracks.com", listOf(cookie), "Listener")
        val restored = store.load(stationId, "streamingsoundtracks.com")

        assertEquals(1, restored.size)
        assertEquals("session", restored.single().name)
        assertEquals("instrumentation-only-value", restored.single().value)
        assertEquals("streamingsoundtracks.com", restored.single().domain)
        assertTrue(restored.single().secure)
        assertEquals("Listener", store.loadDisplayName(stationId))
        assertTrue(store.load(stationId, "example.com").isEmpty())
        store.clear(stationId)
        assertTrue(store.load(stationId, "streamingsoundtracks.com").isEmpty())
    }

    @Test
    fun clearingOneEncryptedStationSessionPreservesAnother() {
        val store = AndroidKeystoreAuthSessionStore(ApplicationProvider.getApplicationContext())
        val first = StationId("keystore-isolation-first")
        val second = StationId("keystore-isolation-second")
        store.clear(first)
        store.clear(second)
        try {
            store.save(first, "first.example", listOf(HttpCookie("session", "first-value")), "First listener")
            store.save(second, "second.example", listOf(HttpCookie("session", "second-value")), "Second listener")

            store.clear(first)

            assertTrue(store.load(first, "first.example").isEmpty())
            assertEquals("second-value", store.load(second, "second.example").single().value)
            assertEquals("Second listener", store.loadDisplayName(second))
        } finally {
            store.clear(first)
            store.clear(second)
        }
    }

    @Test
    fun encryptedLegacyKeysMigrateWithoutDecryptingOrExtendingTheSession() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferences = context.getSharedPreferences("protected_auth_sessions", android.content.Context.MODE_PRIVATE)
        val canonical = StationId("afm")
        val store = AndroidKeystoreAuthSessionStore(context)
        store.clear(canonical)
        preferences.edit().remove("station_adagio").remove("identity_adagio").commit()
        try {
            store.save(
                canonical,
                "adagio.fm",
                listOf(HttpCookie("session", "legacy-encrypted").apply { domain = "adagio.fm" }),
                "Legacy listener",
            )
            preferences.edit()
                .putString("station_adagio", preferences.getString("station_afm", null))
                .putString("identity_adagio", preferences.getString("identity_afm", null))
                .remove("station_afm")
                .remove("identity_afm")
                .commit()

            val migrated = AndroidKeystoreAuthSessionStore(context)

            assertEquals("legacy-encrypted", migrated.load(canonical, "adagio.fm").single().value)
            assertEquals("Legacy listener", migrated.loadDisplayName(canonical))
            assertTrue(!preferences.contains("station_adagio"))
            assertTrue(!preferences.contains("identity_adagio"))
        } finally {
            store.clear(canonical)
            preferences.edit().remove("station_adagio").remove("identity_adagio").commit()
        }
    }

    @Test
    fun encryptedCookieKeepsAbsoluteExpiryAcrossReloadAndUpdate() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val station = StationId("keystore-expiry-test")
        var now = 1_000L
        val store = AndroidKeystoreAuthSessionStore(context) { now }
        store.clear(station)
        try {
            store.save(
                station,
                "streamingsoundtracks.com",
                listOf(HttpCookie("session", "expiring").apply {
                    domain = "streamingsoundtracks.com"
                    path = "/"
                    maxAge = 10L
                }),
                "Listener",
            )
            now += 5_000L
            val halfway = store.load(station, "streamingsoundtracks.com")
            assertEquals(5L, halfway.single().maxAge)
            store.updateCookies(
                station,
                "streamingsoundtracks.com",
                halfway,
                refreshedCookies = emptyList(),
            )

            now += 5_001L
            val coordinator = StationAuthSessionCoordinator(store)
            assertTrue(coordinator.cookieManager(station, "https://streamingsoundtracks.com/").cookieStore.cookies.isEmpty())
            assertEquals(ProtectedSessionValidity.Expired, coordinator.observeValidity(station).first())
            assertEquals(null, store.loadDisplayName(station))
        } finally {
            store.clear(station)
        }
    }
}
