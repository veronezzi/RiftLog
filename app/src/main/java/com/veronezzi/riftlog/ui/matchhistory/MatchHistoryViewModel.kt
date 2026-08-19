package com.veronezzi.riftlog.ui.matchhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.MatchRepository
import com.veronezzi.riftlog.data.settings.SettingsRepository
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.veronezzi.riftlog.domain.model.MatchSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20
sealed class MatchHistoryUiState {
    object Loading : MatchHistoryUiState()
    object NoProfile : MatchHistoryUiState()
    data class Error(val error: ApiResult.Error) : MatchHistoryUiState()
    data class Success(
        val matches: List<MatchSummary>,
        val ddragonVersion: String,
        val isLoadingMore: Boolean,
        val canLoadMore: Boolean,
    ) : MatchHistoryUiState()
}

class MatchHistoryViewModel(
    private val matchRepository: MatchRepository,
    private val championRepository: ChampionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchHistoryUiState>(MatchHistoryUiState.Loading)
    val uiState: StateFlow<MatchHistoryUiState> = _uiState

    private var puuid: String? = null
    private var platformRegion: String? = null
    private var requestedCount = PAGE_SIZE

    init {
        load(forceRefresh = false)
    }

    // Always bypasses the cache: a retry after an error can otherwise land on a TTL-fresh but
    // partial page (a mid-fetch failure persists whatever it got before propagating the error),
    // silently "succeeding" with an incomplete match list for up to 5 minutes instead of
    // actually trying the network again.
    fun retry() = load(forceRefresh = true)

    fun loadMore() {
        val state = _uiState.value
        if (state !is MatchHistoryUiState.Success || state.isLoadingMore || !state.canLoadMore) return
        requestedCount += PAGE_SIZE
        fetchMatches(forceRefresh = true, isLoadingMore = true)
    }

    private fun load(forceRefresh: Boolean) {
        _uiState.value = MatchHistoryUiState.Loading
        requestedCount = PAGE_SIZE
        viewModelScope.launch {
            val lastProfile = settingsRepository.lastProfile.first()
            if (lastProfile == null) {
                _uiState.value = MatchHistoryUiState.NoProfile
                return@launch
            }
            puuid = lastProfile.puuid
            platformRegion = lastProfile.platformRegion
            fetchMatches(forceRefresh = forceRefresh, isLoadingMore = false)
        }
    }

    private fun fetchMatches(forceRefresh: Boolean, isLoadingMore: Boolean) {
        val puuid = puuid ?: return
        val platformRegion = platformRegion ?: return
        if (isLoadingMore) {
            val current = _uiState.value
            if (current is MatchHistoryUiState.Success) {
                _uiState.value = current.copy(isLoadingMore = true)
            }
        }
        viewModelScope.launch {
            val versionResult = championRepository.getLatestVersion()
            val version = (versionResult as? ApiResult.Success)?.data ?: FALLBACK_DDRAGON_VERSION
            when (val result = matchRepository.getRecentMatches(puuid, platformRegion, requestedCount, forceRefresh)) {
                is ApiResult.Error -> _uiState.value = MatchHistoryUiState.Error(result)
                is ApiResult.Success -> {
                    val page = result.data
                    _uiState.value = MatchHistoryUiState.Success(
                        matches = page.matches,
                        ddragonVersion = version,
                        isLoadingMore = false,
                        canLoadMore = page.hasMore,
                    )
                }
            }
        }
    }
}
