package com.codeframe78.twentyfourseven.player

import android.app.Application
import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
import com.codeframe78.twentyfourseven.player.data.InMemoryNowPlayingRepository
import com.codeframe78.twentyfourseven.player.domain.NowPlayingPublisher
import com.codeframe78.twentyfourseven.player.domain.NowPlayingRepository
import com.codeframe78.twentyfourseven.player.playback.Media3PlaybackController

class RadioApplication : Application() {
    val appContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val nowPlayingStore = InMemoryNowPlayingRepository()

    val stationRepository = BootstrapStationRepository()
    val playbackController by lazy { Media3PlaybackController(application) }
    val nowPlayingRepository: NowPlayingRepository = nowPlayingStore
    val nowPlayingPublisher: NowPlayingPublisher = nowPlayingStore
}

