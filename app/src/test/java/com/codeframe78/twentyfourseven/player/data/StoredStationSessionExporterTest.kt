package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.LocalStationPreferences
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import java.net.HttpCookie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoredStationSessionExporterTest {
    private val id = StationId("sst")
    private val station = Station(
        id = id, name = "SST", shortName = "SST", description = "d", websiteUrl = "https://sst.example/",
    )

    private val stations = object : StationRepository {
        override fun availableStations() = listOf(station)
        override fun observeStations(): Flow<List<Station>> = TODO()
        override fun observeSelectedStation(): Flow<Station> = TODO()
        override fun observeStationPreferences(): Flow<LocalStationPreferences> = TODO()
        override suspend fun selectStation(stationId: StationId) = TODO()
        override suspend fun useLastStationAtStartup() = TODO()
        override suspend fun setStartupStation(stationId: StationId) = TODO()
    }

    private class FakeStore(val name: String?, val cookies: List<HttpCookie>) : AuthSessionStore {
        var askedDomain: String? = null
        override fun load(stationId: StationId, expectedDomain: String): List<HttpCookie> {
            askedDomain = expectedDomain
            return cookies
        }
        override fun loadDisplayName(stationId: StationId) = name
        override fun save(stationId: StationId, expectedDomain: String, cookies: List<HttpCookie>, displayName: String) = Unit
        override fun updateCookies(stationId: StationId, expectedDomain: String, cookies: List<HttpCookie>, refreshedCookies: List<HttpCookie>) = Unit
        override fun clear(stationId: StationId) = Unit
    }

    private fun cookie(name: String, maxAge: Long) = HttpCookie(name, "v-$name").also {
        it.secure = true
        it.path = "/"
        it.maxAge = maxAge
    }

    @Test
    fun `signed out returns nothing`() = runBlocking {
        assertNull(StoredStationSessionExporter(FakeStore(null, emptyList()), stations).export(id))
        assertNull(StoredStationSessionExporter(FakeStore("James", emptyList()), stations).export(id))
        assertNull(StoredStationSessionExporter(FakeStore(null, listOf(cookie("a", -1))), stations).export(id))
    }

    @Test
    fun `signed in returns the station's cookies with their lifetimes`() = runBlocking {
        val store = FakeStore("James", listOf(cookie("PHPSESSID", -1), cookie("user", 5000)))
        val exported = StoredStationSessionExporter(store, stations).export(id)!!
        assertEquals("sst.example", store.askedDomain)
        assertEquals("James", exported.account)
        assertEquals(listOf("PHPSESSID", "user"), exported.cookies.map { it.name })
        assertEquals(listOf(-1L, 5000L), exported.cookies.map { it.maxAgeSeconds })
        assertEquals(listOf(true, true), exported.cookies.map { it.secure })
    }

    @Test
    fun `a www website host still reads the cookies stored for the bare origin`() = runBlocking {
        val wwwStations = object : StationRepository by stations {
            override fun availableStations() = listOf(station.copy(websiteUrl = "https://www.sst.example/"))
        }
        val store = FakeStore("James", listOf(cookie("PHPSESSID", -1)))
        StoredStationSessionExporter(store, wwwStations).export(id)!!
        assertEquals("sst.example", store.askedDomain)
    }

    @Test
    fun `exportAll returns every signed-in station and skips the others`() = runBlocking {
        val sst = station.copy(shortName = "SST")
        val other = station.copy(id = StationId("1980s"), shortName = "1980s.FM", websiteUrl = "https://1980s.example/")
        val two = object : StationRepository by stations {
            override fun availableStations() = listOf(sst, other, station.copy(id = StationId("afm"), websiteUrl = "https://afm.example/"))
        }
        val store = object : AuthSessionStore {
            override fun load(stationId: StationId, expectedDomain: String) =
                if (stationId.value == "afm") emptyList() else listOf(cookie("c-${stationId.value}", -1))
            override fun loadDisplayName(stationId: StationId) = if (stationId.value == "afm") null else "user-${stationId.value}"
            override fun save(stationId: StationId, expectedDomain: String, cookies: List<HttpCookie>, displayName: String) = Unit
            override fun updateCookies(stationId: StationId, expectedDomain: String, cookies: List<HttpCookie>, refreshedCookies: List<HttpCookie>) = Unit
            override fun clear(stationId: StationId) = Unit
        }
        val all = StoredStationSessionExporter(store, two).exportAll()
        assertEquals(listOf("sst", "1980s"), all.map { it.station })
        assertEquals(listOf("SST", "1980s.FM"), all.map { it.stationName })
        assertEquals(listOf("user-sst", "user-1980s"), all.map { it.account })
    }

    @Test
    fun `an unknown station returns nothing`() = runBlocking {
        assertNull(StoredStationSessionExporter(FakeStore("James", listOf(cookie("a", -1))), stations).export(StationId("other")))
    }
}
