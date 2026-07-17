package com.codeframe78.twentyfourseven.player

import android.app.Application
import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
import com.codeframe78.twentyfourseven.player.data.InMemoryNowPlayingRepository
import com.codeframe78.twentyfourseven.player.data.PollingQueueRepository
import com.codeframe78.twentyfourseven.player.data.NetworkAuthRepository
import com.codeframe78.twentyfourseven.player.data.AndroidKeystoreAuthSessionStore
import com.codeframe78.twentyfourseven.player.data.AndroidCommunityNotificationRepository
import com.codeframe78.twentyfourseven.player.data.PollingChatRepository
import com.codeframe78.twentyfourseven.player.data.StationAuthRemoteDataSource
import com.codeframe78.twentyfourseven.player.data.StationChatRemoteDataSource
import com.codeframe78.twentyfourseven.player.data.NetworkSongRequestRepository
import com.codeframe78.twentyfourseven.player.data.NetworkFavoriteTracksRepository
import com.codeframe78.twentyfourseven.player.data.StationFavoriteTracksRemoteDataSource
import com.codeframe78.twentyfourseven.player.data.StationSongRequestRemoteDataSource
import com.codeframe78.twentyfourseven.player.data.StationNowPlayingArtworkRepository
import com.codeframe78.twentyfourseven.player.data.SharedPreferencesStationPreferencesRepository
import com.codeframe78.twentyfourseven.player.data.SharedPreferencesCommunitySafetyRepository
import com.codeframe78.twentyfourseven.player.data.NetworkListenerActivityRepository
import com.codeframe78.twentyfourseven.player.data.StationListenerActivityRemoteDataSource
import com.codeframe78.twentyfourseven.player.domain.NowPlayingArtworkRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingPublisher
import com.codeframe78.twentyfourseven.player.domain.NowPlayingRepository
import com.codeframe78.twentyfourseven.player.domain.SongRequestRepository
import com.codeframe78.twentyfourseven.player.domain.FavoriteTracksRepository
import com.codeframe78.twentyfourseven.player.domain.ListenerActivityRepository
import com.codeframe78.twentyfourseven.player.playback.Media3PlaybackController

class RadioApplication : Application() {
    val appContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val nowPlayingStore = InMemoryNowPlayingRepository()
    private val authSessionStore = AndroidKeystoreAuthSessionStore(application)
    private val stationPreferences = SharedPreferencesStationPreferencesRepository(application)
    val communitySafetyRepository = SharedPreferencesCommunitySafetyRepository(application)
    val communityNotificationRepository = AndroidCommunityNotificationRepository(application)

    val stationRepository = BootstrapStationRepository(stationPreferences)
    val playbackController by lazy { Media3PlaybackController(application) }
    val nowPlayingRepository: NowPlayingRepository = nowPlayingStore
    val nowPlayingPublisher: NowPlayingPublisher = nowPlayingStore
    val nowPlayingArtworkRepository: NowPlayingArtworkRepository = StationNowPlayingArtworkRepository()
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
    val favoriteTracksRepository: FavoriteTracksRepository = NetworkFavoriteTracksRepository(
        StationFavoriteTracksRemoteDataSource(sessionStore = authSessionStore),
    )
    val listenerActivityRepository: ListenerActivityRepository = NetworkListenerActivityRepository(
        StationListenerActivityRemoteDataSource(sessionStore = authSessionStore),
    )
}
