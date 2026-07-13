package com.codeframe78.twentyfourseven.player

import android.app.Application
import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository
import com.codeframe78.twentyfourseven.player.playback.Media3PlaybackController

class RadioApplication : Application() {
    val appContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val stationRepository = BootstrapStationRepository()
    val playbackController = Media3PlaybackController(application)
}

