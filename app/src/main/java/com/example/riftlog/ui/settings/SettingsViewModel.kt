package com.example.riftlog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riftlog.data.local.RiftLogDbHelper
import com.example.riftlog.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(val platformRegion: String = SettingsRepository.DEFAULT_PLATFORM_REGION)

sealed class SettingsEvent {
    object Saved : SettingsEvent()
    object CacheCleared : SettingsEvent()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val dbHelper: RiftLogDbHelper,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.platformRegion
        .map { region -> SettingsUiState(region) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val eventFlow: Flow<SettingsEvent> = events.receiveAsFlow()

    fun save(platformRegion: String) {
        viewModelScope.launch {
            settingsRepository.setPlatformRegion(platformRegion)
            events.send(SettingsEvent.Saved)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dbHelper.clearAll() }
            settingsRepository.clearCachedData()
            events.send(SettingsEvent.CacheCleared)
        }
    }
}
