package com.example.riftlog.ui.championdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riftlog.data.repository.ChampionRepository
import com.example.riftlog.data.repository.MatchRepository
import com.example.riftlog.data.repository.ProBuildRepository
import com.example.riftlog.data.settings.SettingsRepository
import com.example.riftlog.domain.ApiResult
import com.example.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.example.riftlog.domain.model.ChampionDetail
import com.example.riftlog.domain.model.ProBuild
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ChampionDetailUiState {
    object Loading : ChampionDetailUiState()
    data class Error(val error: ApiResult.Error) : ChampionDetailUiState()
    data class Success(
        val detail: ChampionDetail,
        val recommendedBuild: List<Int>?,
        val proBuild: ProBuild?,
        val proBuildFailed: Boolean,
        val ddragonVersion: String,
    ) : ChampionDetailUiState()
}

class ChampionDetailViewModel(
    private val championId: String,
    private val championRepository: ChampionRepository,
    private val matchRepository: MatchRepository,
    private val settingsRepository: SettingsRepository,
    private val proBuildRepository: ProBuildRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChampionDetailUiState>(ChampionDetailUiState.Loading)
    val uiState: StateFlow<ChampionDetailUiState> = _uiState

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = ChampionDetailUiState.Loading
        viewModelScope.launch {
            val versionResult = championRepository.getLatestVersion()
            val version = (versionResult as? ApiResult.Success)?.data ?: FALLBACK_DDRAGON_VERSION

            val puuid = settingsRepository.lastProfile.first()?.puuid
            val championsResult = championRepository.getChampions(version, puuid)
            val championInfo = (championsResult as? ApiResult.Success)?.data?.firstOrNull { it.id == championId }
            if (championInfo == null) {
                _uiState.value = ChampionDetailUiState.Error(
                    (championsResult as? ApiResult.Error) ?: ApiResult.Error.Unknown("Champion not found")
                )
                return@launch
            }

            when (val detailResult = championRepository.getChampionDetail(version, championId, championInfo)) {
                is ApiResult.Error -> _uiState.value = ChampionDetailUiState.Error(detailResult)
                is ApiResult.Success -> {
                    val recommendedBuild = puuid?.let { matchRepository.getChampionAggregate(it, championId) }
                        ?.recommendedBuild
                    val proBuildResult = proBuildRepository.getProBuild(championInfo.name, version)
                    _uiState.value = ChampionDetailUiState.Success(
                        detail = detailResult.data,
                        recommendedBuild = recommendedBuild,
                        proBuild = (proBuildResult as? ApiResult.Success)?.data,
                        proBuildFailed = proBuildResult is ApiResult.Error,
                        ddragonVersion = version,
                    )
                }
            }
        }
    }
}
