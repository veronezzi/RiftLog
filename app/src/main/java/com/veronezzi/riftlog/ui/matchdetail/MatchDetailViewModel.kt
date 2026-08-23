package com.veronezzi.riftlog.ui.matchdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veronezzi.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.MatchRepository
import com.veronezzi.riftlog.data.repository.toMatchDetail
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.domain.model.MatchDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MatchDetailUiState {
    object Loading : MatchDetailUiState()
    data class Error(val error: ApiResult.Error) : MatchDetailUiState()
    data class Success(val detail: MatchDetail, val ddragonVersion: String) : MatchDetailUiState()
}

class MatchDetailViewModel(
    private val matchId: String,
    private val platformRegion: String,
    private val viewerPuuid: String,
    private val matchRepository: MatchRepository,
    private val championRepository: ChampionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = MatchDetailUiState.Loading
        viewModelScope.launch {
            val versionResult = championRepository.getLatestVersion()
            val version = (versionResult as? ApiResult.Success)?.data ?: FALLBACK_DDRAGON_VERSION
            when (val result = matchRepository.getMatchDetail(matchId, platformRegion)) {
                is ApiResult.Error -> _uiState.value = MatchDetailUiState.Error(result)
                is ApiResult.Success -> {
                    _uiState.value = MatchDetailUiState.Success(
                        detail = result.data.toMatchDetail(matchId, viewerPuuid),
                        ddragonVersion = version,
                    )
                }
            }
        }
    }
}
