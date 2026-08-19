package com.example.riftlog.ui.builds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riftlog.data.repository.ChampionRepository
import com.example.riftlog.data.settings.SettingsRepository
import com.example.riftlog.domain.ApiResult
import com.example.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.example.riftlog.domain.model.ChampionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class BuildsUiState {
    object Loading : BuildsUiState()
    data class Error(val error: ApiResult.Error) : BuildsUiState()
    data class Success(val visible: List<ChampionInfo>) : BuildsUiState()
}

/**
 * Lets the user look up ANY champion's kit and build - not just the ones in their own match
 * history/mastery list (that's what Champion Stats is for). Reuses the same Champion Detail
 * screen; if the signed-in profile has recent games with the picked champion it shows the real
 * derived build same as Champion Stats would, otherwise it shows the "no recent games" state -
 * there's no invented meta-build data source here, only what the player's own matches say.
 */
class BuildsViewModel(
    private val championRepository: ChampionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BuildsUiState>(BuildsUiState.Loading)
    val uiState: StateFlow<BuildsUiState> = _uiState

    private var allChampions: List<ChampionInfo> = emptyList()
    private var query: String = ""

    init {
        load()
    }

    fun retry() = load()

    fun onQueryChanged(newQuery: String) {
        query = newQuery
        val current = _uiState.value
        if (current is BuildsUiState.Success || allChampions.isNotEmpty()) {
            _uiState.value = BuildsUiState.Success(filtered())
        }
    }

    private fun filtered(): List<ChampionInfo> {
        if (query.isBlank()) return allChampions
        return allChampions.filter { it.name.contains(query, ignoreCase = true) }
    }

    private fun load() {
        _uiState.value = BuildsUiState.Loading
        viewModelScope.launch {
            val lastProfile = settingsRepository.lastProfile.first()
            val versionResult = championRepository.getLatestVersion()
            val version = (versionResult as? ApiResult.Success)?.data ?: FALLBACK_DDRAGON_VERSION

            when (val result = championRepository.getChampions(version, lastProfile?.puuid)) {
                is ApiResult.Error -> _uiState.value = BuildsUiState.Error(result)
                is ApiResult.Success -> {
                    allChampions = result.data.sortedBy { it.name }
                    _uiState.value = BuildsUiState.Success(filtered())
                }
            }
        }
    }
}
