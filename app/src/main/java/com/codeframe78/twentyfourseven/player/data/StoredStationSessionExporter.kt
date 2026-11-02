package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ExportedStationSession
import com.codeframe78.twentyfourseven.player.domain.SessionCookie
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import com.codeframe78.twentyfourseven.player.domain.StationSessionExporter
import java.net.URI

/** Exports the cookies the session store holds for the station's own origin, and nothing else. */
internal class StoredStationSessionExporter(
    private val store: AuthSessionStore,
    private val stations: StationRepository,
) : StationSessionExporter {
    override suspend fun export(stationId: StationId): ExportedStationSession? {
        val station = stations.availableStations().firstOrNull { it.id == stationId } ?: return null
        val host = runCatching { URI(station.websiteUrl).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val account = store.loadDisplayName(stationId)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val cookies = store.load(stationId, host).filter { it.name.isNotEmpty() }.map {
            SessionCookie(
                name = it.name,
                value = it.value,
                maxAgeSeconds = if (it.maxAge > 0) it.maxAge else -1L,
                secure = it.secure,
                path = it.path ?: "/",
            )
        }
        if (cookies.isEmpty()) return null
        return ExportedStationSession(stationId.value, account, cookies)
    }
}
