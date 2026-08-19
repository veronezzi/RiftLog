package com.veronezzi.riftlog.ui.championstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.MatchRepository
import com.veronezzi.riftlog.data.settings.SettingsRepository
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.veronezzi.riftlog.domain.model.ChampionAggregate
import com.veronezzi.riftlog.domain.model.ChampionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ChampionSort { MASTERY, WINRATE, GAMES }

data class ChampionStatsItem(val info: ChampionInfo, val aggregate: ChampionAggregate?)

sealed class ChampionStatsUiState {
    object Loading : ChampionStatsUiState()
    object NoProfile : ChampionStatsUiState()
    data class Error(val error: ApiResult.Error) : ChampionStatsUiState()
    data class Success(
        val items: List<ChampionStatsItem>,
        val ddragonVersion: String,
        val sort: ChampionSort,
    ) : ChampionStatsUiState()
}

class ChampionStatsViewModel(
    private val championRepository: ChampionRepository,
    private val matchRepository: MatchRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChampionStatsUiState>(ChampionStatsUiState.Loading)
    val uiState: StateFlow<ChampionStatsUiState> = _uiState

    private var unsortedItems: List<ChampionStatsItem> = emptyList()
    private var ddragonVersion: String = FALLBACK_DDRAGON_VERSION

    init {
        load()
    }

    fun retry() = load()

    fun onSortSelected(sort: ChampionSort) {
        val current = _uiState.value
        if (current is ChampionStatsUiState.Success) {
            _uiState.value = current.copy(items = sorted(unsortedItems, sort), sort = sort)
        }
    }

    private fun load() {
        _uiState.value = ChampionStatsUiState.Loading
        viewModelScope.launch {
            val lastProfile = settingsRepository.lastProfile.first()
            if (lastProfile == null) {
                _uiState.value = ChampionStatsUiState.NoProfile
                return@launch
            }
            val (puuid, platformRegion) = lastProfile

            val versionResult = championRepository.getLatestVersion()
            val version = (versionResult as? ApiResult.Success)?.data ?: FALLBACK_DDRAGON_VERSION
            ddragonVersion = version

            championRepository.refreshChampionMasteries(puuid, platformRegion)

            when (val championsResult = championRepository.getChampions(version, puuid)) {
                is ApiResult.Error -> _uiState.value = ChampionStatsUiState.Error(championsResult)
                is ApiResult.Success -> {
                    val played = championsResult.data.filter { it.mastery != null }
                    val items = played.map { info ->
                        ChampionStatsItem(info, matchRepository.getChampionAggregate(puuid, info.id))
                    }
                    unsortedItems = items
                    _uiState.value = ChampionStatsUiState.Success(
                        items = sorted(items, ChampionSort.MASTERY),
                        ddragonVersion = version,
                        sort = ChampionSort.MASTERY,
                    )
                }
            }
        }
    }

    private fun sorted(items: List<ChampionStatsItem>, sort: ChampionSort): List<ChampionStatsItem> = when (sort) {
        ChampionSort.MASTERY -> items.sortedByDescending { it.info.mastery?.championPoints ?: 0L }
        ChampionSort.WINRATE -> items.sortedByDescending { it.aggregate?.winRate ?: 0.0 }
        ChampionSort.GAMES -> items.sortedByDescending { it.aggregate?.gamesPlayed ?: 0 }
    }
}
