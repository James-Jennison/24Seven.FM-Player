package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.StationId
import java.net.URI
import java.nio.charset.Charset

/** Serves scripted station pages by path and records every request, so a test can prove what was sent. */
internal class FakeStationPages(
    private var signedIn: Boolean = true,
    private val respond: (method: String, path: String, fields: List<Pair<String, String>>) -> String,
) : StationPages {
    val requests = mutableListOf<String>()
    val posted = mutableListOf<List<Pair<String, String>>>()
    var expired = false
        private set

    override fun origin(stationId: StationId): String = ORIGIN

    override fun hasSession(stationId: StationId): Boolean = signedIn

    override fun expireSession(stationId: StationId) {
        expired = true
        signedIn = false
    }

    override fun get(stationId: StationId, path: String, maxCharacters: Int): StationPageResponse {
        requests += "GET $path"
        return StationPageResponse(respond("GET", path, emptyList()), URI(ORIGIN).resolve(path))
    }

    override fun postForm(
        stationId: StationId,
        path: String,
        fields: List<Pair<String, String>>,
        maxCharacters: Int,
        charset: Charset,
    ): StationPageResponse {
        requests += "POST $path"
        posted += fields
        return StationPageResponse(respond("POST", path, fields), URI(ORIGIN).resolve(path))
    }

    companion object {
        const val ORIGIN = "https://streamingsoundtracks.com/"
    }
}
