package com.codeframe78.twentyfourseven.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.codeframe78.twentyfourseven.player.domain.Station
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(val stations: List<Station> = emptyList(), val selectedStation: Station? = null)

class MainViewModel(private val stations: StationRepository) : ViewModel() {
    val uiState: StateFlow<MainUiState> = combine(
        stations.observeStations(), stations.observeSelectedStation()
    ) { all, selected -> MainUiState(all, selected) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun selectStation(id: StationId) = viewModelScope.launch { stations.selectStation(id) }

    class Factory(private val stations: StationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(stations) as T
    }
}

