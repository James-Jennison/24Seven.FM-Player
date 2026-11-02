package com.codeframe78.twentyfourseven.player.domain

/**
 * One cookie of the signed-in station session, as handed to the user's own TV. [toString] omits the value so it cannot
 * reach a log, a UI state or an exception message.
 */
data class SessionCookie(
    val name: String,
    val value: String,
    val maxAgeSeconds: Long,
    val secure: Boolean,
    val path: String,
) {
    override fun toString(): String = "SessionCookie(name=$name)"
}

/** The signed-in station session of the phone: [account] is the display label, [cookies] are the station origin's only. */
data class ExportedStationSession(
    val station: String,
    val account: String,
    val cookies: List<SessionCookie>,
) {
    override fun toString(): String = "ExportedStationSession(station=$station, cookies=${cookies.map { it.name }})"
}

/**
 * Reads the phone's station session so the listener can hand it to their own TV. Controllers and view models use this
 * interface and never touch the session store. Returns null when no one is signed in.
 */
interface StationSessionExporter {
    suspend fun export(stationId: StationId): ExportedStationSession?
}
