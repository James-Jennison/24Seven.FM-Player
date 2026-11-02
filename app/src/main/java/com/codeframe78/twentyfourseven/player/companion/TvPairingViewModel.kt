package com.codeframe78.twentyfourseven.player.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codeframe78.twentyfourseven.player.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps the pairing controller across configuration changes such as rotation; ends any pairing when the activity ends. */
class TvPairingViewModel(container: AppContainer) : ViewModel() {
    val controller = CompanionPairingController(
        scope = viewModelScope,
        clockMillis = System::currentTimeMillis,
        connect = { qr -> withContext(Dispatchers.IO) { PinnedTlsConnector().connect(qr) } },
        probe = { host, port -> withContext(Dispatchers.IO) { PinnedTlsConnector().probe(host, port) } },
        handlerFor = { approve ->
            PlayerCompanionRequestHandler(
                stations = container.stationRepository,
                requests = container.songRequestRepository,
                auth = container.authRepository,
                queue = container.queueRepository,
                listenerActivity = container.listenerActivityRepository,
                communitySafety = container.communitySafetyRepository,
                approveOnPhone = approve,
            )
        },
    )

    override fun onCleared() {
        controller.close()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TvPairingViewModel(container) as T
    }
}
