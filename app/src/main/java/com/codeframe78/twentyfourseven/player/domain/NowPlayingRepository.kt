package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

data class NowPlayingState(
    val stationId: StationId? = null,
    val displayTitle: String? = null,
    val artworkUrl: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val track: String? = null,
    val albumId: String? = null,
    val requesterName: String? = null,
    val requestMessage: String? = null,
    val listenerCount: Int? = null,
    val trackLengthMillis: Long? = null,
    /** Device uptime, as SystemClock.elapsedRealtime reports it, at which the station started this track. */
    val trackStartedElapsedRealtimeMillis: Long? = null,
) {
    /** The station's own details for the track named by [displayTitle], or this state when they describe another. */
    fun withStationDetails(details: NowPlayingState?): NowPlayingState {
        if (details == null) return this
        val sameTrack = details.track?.trackMatchKey()?.takeIf(String::isNotEmpty)
            ?.let { displayTitle?.trackMatchKey()?.contains(it) } == true
        return copy(
            artworkUrl = details.artworkUrl ?: artworkUrl,
            artist = if (sameTrack) details.artist else artist,
            album = if (sameTrack) details.album else album,
            track = if (sameTrack) details.track else track,
            albumId = if (sameTrack) details.albumId else albumId,
            requesterName = if (sameTrack) details.requesterName else requesterName,
            requestMessage = if (sameTrack) details.requestMessage else requestMessage,
            listenerCount = details.listenerCount ?: listenerCount,
            trackLengthMillis = if (sameTrack) details.trackLengthMillis else trackLengthMillis,
            trackStartedElapsedRealtimeMillis = if (sameTrack) {
                details.trackStartedElapsedRealtimeMillis
            } else {
                trackStartedElapsedRealtimeMillis
            },
        )
    }
}

interface NowPlayingRepository {
    fun observeNowPlaying(): Flow<NowPlayingState>
}

interface NowPlayingPublisher {
    fun publish(state: NowPlayingState)
    fun clear(stationId: StationId? = null)
}

interface NowPlayingArtworkRepository {
    suspend fun fetchArtwork(stationId: StationId): String?
}

interface NowPlayingDetailsRepository {
    suspend fun fetchNowPlaying(stationId: StationId): NowPlayingState?
}

fun String.normalizeTrailingTheArticle(): String {
    val match = TrailingTheArticle.matchEntire(trim()) ?: return this
    return "The ${match.groupValues[1].trim()}"
}

/** Letters and digits only, without "the", so "Story, The" and "The Story" compare equal. */
private fun String.trackMatchKey(): String = lowercase()
    .replace(TheArticle, " ")
    .filter(Char::isLetterOrDigit)

private val TheArticle = Regex("\\bthe\\b")
private val TrailingTheArticle = Regex("^(.+),\\s*the$", RegexOption.IGNORE_CASE)
