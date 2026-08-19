package com.example.riftlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riftlog.data.repository.ChampionRepository
import com.example.riftlog.data.repository.ProfileRepository
import com.example.riftlog.data.settings.RecentSearch
import com.example.riftlog.data.settings.SettingsRepository
import com.example.riftlog.domain.ApiResult
import com.example.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.example.riftlog.domain.model.PlayerProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class PinnedProfileState {
    object None : PinnedProfileState()
    object Loading : PinnedProfileState()
    data class Loaded(val profile: PlayerProfile, val ddragonVersion: String) : PinnedProfileState()
    data class Error(val recentSearch: RecentSearch) : PinnedProfileState()
}

data class HomeUiState(
    val selectedRegion: String = SettingsRepository.DEFAULT_PLATFORM_REGION,
    val recentSearch: RecentSearch? = null,
    val pinned: PinnedProfileState = PinnedProfileState.None,
    val inputError: String? = null,
)

sealed class HomeEvent {
    data class NavigateToProfile(val gameName: String, val tagLine: String, val platformRegion: String) : HomeEvent()
}

/** Home only validates the Riot ID format and remembers the last search - the actual lookup
 * for a fresh search happens in ProfileViewModel. The pinned-profile preview shown here reuses
 * ProfileRepository's own 5-minute cache, so it's cheap even though it's fetched every time the
 * Home tab comes back into view. */
class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val championRepository: ChampionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow: Flow<HomeEvent> = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.platformRegion.collectLatest { region ->
                _uiState.value = _uiState.value.copy(selectedRegion = region)
            }
        }
        viewModelScope.launch {
            settingsRepository.lastSearch.collectLatest { recent ->
                _uiState.value = _uiState.value.copy(recentSearch = recent)
                loadPinnedPreview(recent)
            }
        }
    }

    fun onRegionSelected(region: String) {
        _uiState.value = _uiState.value.copy(selectedRegion = region)
    }

    fun onSearchSubmitted(rawRiotId: String) {
        val parts = rawRiotId.trim().split("#")
        if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            _uiState.value = _uiState.value.copy(inputError = INVALID_FORMAT)
            return
        }
        _uiState.value = _uiState.value.copy(inputError = null)
        val gameName = parts[0].trim()
        val tagLine = parts[1].trim()
        val region = _uiState.value.selectedRegion
        viewModelScope.launch {
            settingsRepository.setLastSearch(gameName, tagLine, region)
            events.send(HomeEvent.NavigateToProfile(gameName, tagLine, region))
        }
    }

    fun onPinnedProfileTapped() {
        val recent = _uiState.value.recentSearch ?: return
        viewModelScope.launch {
            events.send(HomeEvent.NavigateToProfile(recent.gameName, recent.tagLine, recent.platformRegion))
        }
    }

    fun onUnpinClicked() {
        viewModelScope.launch { settingsRepository.clearCachedData() }
    }

    private fun loadPinnedPreview(recent: RecentSearch?) {
        if (recent == null) {
            _uiState.value = _uiState.value.copy(pinned = PinnedProfileState.None)
            return
        }
        _uiState.value = _uiState.value.copy(pinned = PinnedProfileState.Loading)
        viewModelScope.launch {
            when (val result = profileRepository.getProfile(recent.gameName, recent.tagLine, recent.platformRegion)) {
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(pinned = PinnedProfileState.Error(recent))
                is ApiResult.Success -> {
                    val version = (championRepository.getLatestVersion() as? ApiResult.Success)?.data
                        ?: FALLBACK_DDRAGON_VERSION
                    _uiState.value = _uiState.value.copy(
                        pinned = PinnedProfileState.Loaded(result.data, version)
                    )
                }
            }
        }
    }

    companion object {
        const val INVALID_FORMAT = "invalid_format"
    }
}
