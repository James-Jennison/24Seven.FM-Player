package com.codeframe78.twentyfourseven.player

import android.app.Application
import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
import com.codeframe78.twentyfourseven.player.data.InMemoryNowPlayingRepository
import com.codeframe78.twentyfourseven.player.data.PollingQueueRepository
import com.codeframe78.twentyfourseven.player.data.NetworkAuthRepository
import com.codeframe78.twentyfourseven.player.data.AndroidKeystoreAuthSessionStore
import com.codeframe78.twentyfourseven.player.data.PollingChatRepository
import com.codeframe78.twentyfourseven.player.data.StationAuthRemoteDataSource
import com.codeframe78.twentyfourseven.player.data.StationChatRemoteDataSource
import com.codeframe78.twentyfourseven.player.data.NetworkSongRequestRepository
import com.codeframe78.twentyfourseven.player.data.StationSongRequestRemoteDataSource
import com.codeframe78.twentyfourseven.player.domain.NowPlayingPublisher
import com.codeframe78.twentyfourseven.player.domain.NowPlayingRepository
import com.codeframe78.twentyfourseven.player.domain.SongRequestRepository
import com.codeframe78.twentyfourseven.player.playback.Media3PlaybackController

class RadioApplication : Application() {
    val appContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val nowPlayingStore = InMemoryNowPlayingRepository()
    private val authSessionStore = AndroidKeystoreAuthSessionStore(application)

    val stationRepository = BootstrapStationRepository()
    val playbackController by lazy { Media3PlaybackController(application) }
    val nowPlayingRepository: NowPlayingRepository = nowPlayingStore
    val nowPlayingPublisher: NowPlayingPublisher = nowPlayingStore
    val queueRepository = PollingQueueRepository()
    val authRepository = NetworkAuthRepository(
        StationAuthRemoteDataSource(sessionStore = authSessionStore),
    )
    val chatRepository = PollingChatRepository(
        StationChatRemoteDataSource(sessionStore = authSessionStore),
    )
    val songRequestRepository: SongRequestRepository = NetworkSongRequestRepository(
        StationSongRequestRemoteDataSource(sessionStore = authSessionStore),
    )
}

