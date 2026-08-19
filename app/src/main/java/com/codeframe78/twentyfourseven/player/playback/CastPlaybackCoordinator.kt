package com.codeframe78.twentyfourseven.player.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import com.codeframe78.twentyfourseven.player.domain.PlaybackRoute
import com.codeframe78.twentyfourseven.player.domain.PlaybackStatus
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StreamFormat
import com.codeframe78.twentyfourseven.player.domain.StreamVariant
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.CastStatusCodes
import com.google.android.gms.cast.MediaError
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.api.ResultCallback

internal data class CastPlaybackSnapshot(
    val route: PlaybackRoute = PlaybackRoute.Local,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val deviceName: String? = null,
    val errorMessage: String? = null,
)

/**
 * Keeps Google Cast as a sender route. The local Media3 player remains the only local playback owner.
 */
internal class CastPlaybackCoordinator(
    context: Context,
    private val onSnapshotChanged: (CastPlaybackSnapshot) -> Unit,
    private val onRemotePlaybackAccepted: () -> Unit,
) {
    private companion object {
        const val LogTag = "CastPlayback"
    }

    private val appContext = context.applicationContext
    private val castContext = CastContext.getSharedInstance(appContext)
    private var selectedStation: Station? = null
    private var shouldPlay = false
    private var castSession: CastSession? = null
    private var remoteClient: RemoteMediaClient? = null
    private var snapshot = CastPlaybackSnapshot()
    private var localPlaybackSuspendedForCast = false

    private val remoteMediaCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() = publishRemoteStatus()
        override fun onMetadataUpdated() = publishRemoteStatus()
        override fun onQueueStatusUpdated() = Unit
        override fun onPreloadStatusUpdated() = Unit
        override fun onSendingRemoteMediaRequest() = Unit
        override fun onAdBreakStatusUpdated() = Unit
        override fun onMediaError(error: MediaError) {
            Log.w(LogTag, "receiver media error code=${error.detailedErrorCode}")
            publish(
                CastPlaybackSnapshot(
                    route = PlaybackRoute.CastConnected,
                    status = PlaybackStatus.Error,
                    deviceName = deviceName(),
                    errorMessage = "Cast receiver reported a playback error",
                ),
            )
        }
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) = Unit

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            attach(session)
            if (shouldPlay) loadSelectedStation(autoplay = true)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Log.w(LogTag, "session start failed code=$error")
            detach()
            publish(CastPlaybackSnapshot(errorMessage = "Unable to start Cast session"))
        }

        override fun onSessionEnding(session: CastSession) = Unit

        override fun onSessionEnded(session: CastSession, error: Int) {
            detach()
            publish(CastPlaybackSnapshot())
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            attach(session)
            publishRemoteStatus()
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            Log.w(LogTag, "session resume failed code=$error")
            detach()
            publish(CastPlaybackSnapshot(errorMessage = "Unable to resume Cast session"))
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
    }

    init {
        castContext.sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
        castContext.sessionManager.currentCastSession?.takeIf(CastSession::isConnected)?.let(::attach)
    }

    fun selectStation(station: Station) {
        val changed = selectedStation?.id != station.id
        selectedStation = station
        if (changed && snapshot.route == PlaybackRoute.Casting) loadSelectedStation(autoplay = shouldPlay)
    }

    fun recordLocalPlaybackIntent(isPlaying: Boolean) {
        if (snapshot.route == PlaybackRoute.Local) shouldPlay = isPlaying
    }

    fun play(): Boolean {
        shouldPlay = true
        if (castSession?.isConnected != true) return false
        loadSelectedStation(autoplay = true)
        return true
    }

    fun pause(): Boolean {
        val client = remoteClient ?: return false
        if (snapshot.route != PlaybackRoute.Casting) return false
        shouldPlay = false
        client.pause()
        return true
    }

    fun stop(): Boolean {
        val client = remoteClient ?: return false
        if (snapshot.route == PlaybackRoute.Local) return false
        shouldPlay = false
        if (snapshot.route == PlaybackRoute.Casting) client.stop()
        publish(CastPlaybackSnapshot(PlaybackRoute.CastConnected, PlaybackStatus.Idle, deviceName()))
        return true
    }

    fun isCastSessionActive(): Boolean = snapshot.route != PlaybackRoute.Local

    private fun attach(session: CastSession) {
        if (castSession === session) {
            publishRemoteStatus()
            return
        }
        detach()
        castSession = session
        remoteClient = session.remoteMediaClient?.also { it.registerCallback(remoteMediaCallback) }
        publishRemoteStatus()
    }

    private fun detach() {
        remoteClient?.unregisterCallback(remoteMediaCallback)
        remoteClient = null
        castSession = null
        localPlaybackSuspendedForCast = false
    }

    private fun loadSelectedStation(autoplay: Boolean) {
        val station = selectedStation ?: return
        val client = remoteClient ?: return
        val session = castSession ?: return
        val stream = station.streams.minByOrNull(StreamVariant::priority) ?: return
        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(CastMediaInfoFactory.create(station, stream))
            .setAutoplay(autoplay)
            .build()
        publish(CastPlaybackSnapshot(PlaybackRoute.CastConnected, PlaybackStatus.Connecting, deviceName()))
        client.load(request).setResultCallback(ResultCallback { result ->
            // A remote load can complete after the user has stopped casting.  In that
            // case its result belongs to the old session and must not stop the local
            // Media3 player or overwrite the local playback state.
            if (remoteClient !== client || castSession !== session || !session.isConnected) {
                Log.i(LogTag, "ignoring stale remote media load result")
                return@ResultCallback
            }
            Log.i(LogTag, "remote media load result code=${result.status.statusCode}")
            if (result.status.statusCode == CastStatusCodes.SUCCESS) {
                publishRemoteStatus()
            } else {
                publish(
                    CastPlaybackSnapshot(
                        route = PlaybackRoute.CastConnected,
                        status = PlaybackStatus.Error,
                        deviceName = deviceName(),
                        errorMessage = "Cast receiver could not load this station",
                    ),
                )
            }
        })
    }

    private fun publishRemoteStatus() {
        val session = castSession ?: return publish(CastPlaybackSnapshot())
        val client = remoteClient
        val status = client?.mediaStatus
        val playbackStatus = when (status?.playerState) {
            MediaStatus.PLAYER_STATE_PLAYING -> PlaybackStatus.Playing
            MediaStatus.PLAYER_STATE_PAUSED -> PlaybackStatus.Paused
            MediaStatus.PLAYER_STATE_BUFFERING -> PlaybackStatus.Buffering
            else -> PlaybackStatus.Idle
        }
        remotePlaybackIntentFor(status?.playerState)?.let { shouldPlay = it }
        if (playbackStatus == PlaybackStatus.Playing && !localPlaybackSuspendedForCast) {
            localPlaybackSuspendedForCast = true
            onRemotePlaybackAccepted()
        }
        val route = if (status == null || status.playerState == MediaStatus.PLAYER_STATE_IDLE) {
            PlaybackRoute.CastConnected
        } else {
            PlaybackRoute.Casting
        }
        publish(CastPlaybackSnapshot(route, playbackStatus, session.castDevice?.friendlyName))
    }

    private fun publish(next: CastPlaybackSnapshot) {
        snapshot = next
        onSnapshotChanged(next)
    }

    private fun deviceName(): String? = castSession?.castDevice?.friendlyName
}

/**
 * An idle receiver has not expressed a playback preference. In particular, a newly
 * launched receiver starts idle, and must not erase the sender's local-play intent
 * before [SessionManagerListener.onSessionStarted] can load the selected station.
 */
internal fun remotePlaybackIntentFor(playerState: Int?): Boolean? = when (playerState) {
    MediaStatus.PLAYER_STATE_PLAYING,
    MediaStatus.PLAYER_STATE_BUFFERING,
    -> true

    MediaStatus.PLAYER_STATE_PAUSED -> false
    else -> null
}

internal object CastMediaInfoFactory {
    fun create(station: Station, stream: StreamVariant): MediaInfo {
        val descriptor = CastMediaDescriptorFactory.create(station, stream)
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, descriptor.title)
            putString(MediaMetadata.KEY_ARTIST, "24Seven.FM")
            putString(MediaMetadata.KEY_ALBUM_TITLE, descriptor.stationName)
            descriptor.artworkUrl.safeHttpsUri()?.let { addImage(com.google.android.gms.common.images.WebImage(it)) }
        }
        return MediaInfo.Builder(descriptor.contentId)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(descriptor.contentType)
            .setMetadata(metadata)
            .build()
    }
}

internal data class CastMediaDescriptor(
    val contentId: String,
    val contentType: String,
    val title: String,
    val stationName: String,
    val artworkUrl: String?,
)

internal object CastMediaDescriptorFactory {
    fun create(station: Station, stream: StreamVariant) = CastMediaDescriptor(
        contentId = stream.url,
        contentType = stream.format.castContentType(),
        title = station.name,
        stationName = station.name,
        artworkUrl = null,
    )
}

internal fun StreamFormat.castContentType(): String = when (this) {
    // Preserve the station relay's declared media type. A Cast-compatible
    // HLS variant is required for remote playback of this raw AAC+ transport.
    StreamFormat.Aac -> "audio/aacp"
    StreamFormat.Mp3 -> "audio/mpeg"
    StreamFormat.Hls -> "application/x-mpegURL"
    StreamFormat.Unknown -> "audio/aacp"
}

private fun String?.safeHttpsUri(): Uri? = this
    ?.let(Uri::parse)
    ?.takeIf { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() && it.userInfo == null }
