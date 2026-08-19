package com.veronezzi.riftlog.ui.championdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.MatchRepository
import com.veronezzi.riftlog.data.repository.ProBuildRepository
import com.veronezzi.riftlog.data.settings.SettingsRepository
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.veronezzi.riftlog.domain.model.ChampionAggregate
import com.veronezzi.riftlog.domain.model.ChampionDetail
import com.veronezzi.riftlog.domain.model.ProBuild
import com.veronezzi.riftlog.domain.model.RuneCatalog
import com.veronezzi.riftlog.domain.model.RunePageInfo
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
        val recommendedRunes: RunePageInfo?,
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
                    val aggregate = puuid?.let { matchRepository.getChampionAggregate(it, championId) }
                    val runeCatalogResult = championRepository.getRunes(version)
                    val runeCatalog = (runeCatalogResult as? ApiResult.Success)?.data
                    val recommendedRunes = if (aggregate != null && runeCatalog != null) {
                        buildRunePageInfo(aggregate, runeCatalog)
                    } else {
                        null
                    }
                    val proBuildResult = proBuildRepository.getProBuild(championInfo.name, version)
                    _uiState.value = ChampionDetailUiState.Success(
                        detail = detailResult.data,
                        recommendedBuild = aggregate?.recommendedBuild,
                        recommendedRunes = recommendedRunes,
                        proBuild = (proBuildResult as? ApiResult.Success)?.data,
                        proBuildFailed = proBuildResult is ApiResult.Error,
                        ddragonVersion = version,
                    )
                }
            }
        }
    }

    /** Null when the aggregate has no games (nothing to recommend) or a rune id it stored doesn't
     * resolve against the current catalog - a version mismatch between cached match rows and the
     * live ddragon version, which self-heals once matches get re-fetched. */
    private fun buildRunePageInfo(aggregate: ChampionAggregate, catalog: RuneCatalog): RunePageInfo? {
        if (aggregate.recommendedKeystoneId == 0) return null
        val primaryStyle = catalog.stylesById[aggregate.recommendedPrimaryStyleId] ?: return null
        val subStyle = catalog.stylesById[aggregate.recommendedSubStyleId] ?: return null
        val keystone = catalog.runesById[aggregate.recommendedKeystoneId] ?: return null
        return RunePageInfo(primaryStyle, subStyle, keystone)
    }
}
