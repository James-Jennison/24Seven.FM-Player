package com.codeframe78.twentyfourseven.player

import android.app.Application
import com.codeframe78.twentyfourseven.player.data.BootstrapStationRepository

class RadioApplication : Application() {
    val appContainer by lazy { AppContainer() }
}

class AppContainer {
    val stationRepository = BootstrapStationRepository()
}

