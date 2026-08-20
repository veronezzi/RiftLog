package com.veronezzi.riftlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.ProfileRepository
import com.veronezzi.riftlog.data.settings.RecentSearch
import com.veronezzi.riftlog.data.settings.SettingsRepository
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.veronezzi.riftlog.domain.model.PlayerProfile
import com.veronezzi.riftlog.ui.common.RiotIdValidator
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

private const val MAX_SUGGESTIONS = 5

data class HomeUiState(
    val selectedRegion: String = SettingsRepository.DEFAULT_PLATFORM_REGION,
    val recentSearch: RecentSearch? = null,
    val pinned: PinnedProfileState = PinnedProfileState.None,
    val inputError: String? = null,
    val suggestions: List<RecentSearch> = emptyList(),
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

    /** Every Riot ID successfully resolved on this device before - there's no Riot API to search
     * the whole player base by partial name, so "type a name, see options" can only ever suggest
     * from what this app has already looked up locally, not discover new people. */
    private var searchHistory: List<RecentSearch> = emptyList()
    private var currentQuery: String = ""

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
        viewModelScope.launch {
            settingsRepository.searchHistory.collectLatest { history ->
                searchHistory = history
                _uiState.value = _uiState.value.copy(suggestions = filteredSuggestions())
            }
        }
    }

    /** Called on every keystroke in the Riot ID field (not just on submit) so suggestions update
     * live. A "#" in the query means the user is already typing the tag, at which point matching
     * by name prefix stops being useful - suggestions clear rather than showing stale matches. */
    fun onQueryChanged(query: String) {
        currentQuery = query
        _uiState.value = _uiState.value.copy(suggestions = filteredSuggestions())
    }

    fun onSuggestionTapped(suggestion: RecentSearch) {
        currentQuery = ""
        _uiState.value = _uiState.value.copy(suggestions = emptyList())
        viewModelScope.launch {
            settingsRepository.setLastSearch(suggestion.gameName, suggestion.tagLine, suggestion.platformRegion)
            events.send(
                HomeEvent.NavigateToProfile(suggestion.gameName, suggestion.tagLine, suggestion.platformRegion)
            )
        }
    }

    private fun filteredSuggestions(): List<RecentSearch> {
        val query = currentQuery.trim()
        if (query.isBlank() || query.contains("#")) return emptyList()
        return searchHistory
            .filter { it.gameName.contains(query, ignoreCase = true) }
            .take(MAX_SUGGESTIONS)
    }

    fun onRegionSelected(region: String) {
        _uiState.value = _uiState.value.copy(selectedRegion = region)
    }

    fun onSearchSubmitted(rawRiotId: String) {
        val parsed = RiotIdValidator.parse(rawRiotId)
        if (parsed == null) {
            _uiState.value = _uiState.value.copy(inputError = INVALID_FORMAT)
            return
        }
        currentQuery = ""
        _uiState.value = _uiState.value.copy(inputError = null, suggestions = emptyList())
        val region = _uiState.value.selectedRegion
        viewModelScope.launch {
            settingsRepository.setLastSearch(parsed.gameName, parsed.tagLine, region)
            events.send(HomeEvent.NavigateToProfile(parsed.gameName, parsed.tagLine, region))
        }
    }

    fun onPinnedProfileTapped() {
        val recent = _uiState.value.recentSearch ?: return
        viewModelScope.launch {
            events.send(HomeEvent.NavigateToProfile(recent.gameName, recent.tagLine, recent.platformRegion))
        }
    }

    fun onUnpinClicked() {
        viewModelScope.launch { settingsRepository.clearPinnedProfile() }
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
