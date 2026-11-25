package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ExportedStationSession
import com.codeframe78.twentyfourseven.player.domain.SessionCookie
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import com.codeframe78.twentyfourseven.player.domain.StationSessionExporter
import com.codeframe78.twentyfourseven.player.companion.CompanionLog
import java.net.URI

/** Exports the cookies the session store holds for the station's own origin, and nothing else. */
internal class StoredStationSessionExporter(
    private val store: AuthSessionStore,
    private val stations: StationRepository,
) : StationSessionExporter {
    override suspend fun export(stationId: StationId): ExportedStationSession? {
        val station = stations.availableStations().firstOrNull { it.id == stationId }
            ?: return null.also { CompanionLog.d("session export: unknown station ${stationId.value}") }
        // The session store is keyed by the origin the data sources use, which has no "www." (SST's website host does).
        val host = runCatching { URI(station.websiteUrl).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() }
            ?: return null.also { CompanionLog.d("session export: no host for ${stationId.value}") }
        val account = store.loadDisplayName(stationId)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null.also { CompanionLog.d("session export: no stored account label for ${stationId.value}") }
        val stored = store.load(stationId, host)
        CompanionLog.d("session export: ${stored.size} stored cookies for ${stationId.value} on $host ${stored.map { it.name }}")
        val cookies = stored.filter { it.name.isNotEmpty() }.map {
            SessionCookie(
                name = it.name,
                value = it.value,
                maxAgeSeconds = if (it.maxAge > 0) it.maxAge else -1L,
                secure = it.secure,
                path = it.path ?: "/",
            )
        }
        if (cookies.isEmpty()) return null.also { CompanionLog.d("session export: no cookies for ${stationId.value}") }
        return ExportedStationSession(stationId.value, account, cookies, station.shortName)
    }

    override suspend fun exportAll(): List<ExportedStationSession> =
        stations.availableStations().mapNotNull { export(it.id) }
}
