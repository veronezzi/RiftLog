package com.veronezzi.riftlog.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.MatchRepository
import com.veronezzi.riftlog.data.repository.ProfileRepository
import com.veronezzi.riftlog.data.settings.SettingsRepository
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.veronezzi.riftlog.domain.model.MatchSummary
import com.veronezzi.riftlog.domain.model.PlayerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val RECENT_MATCH_COUNT = 20
data class RecentFormAggregate(
    val gamesPlayed: Int,
    val wins: Int,
    val avgKda: Double,
) {
    val winRatePercent: Int get() = if (gamesPlayed == 0) 0 else (wins * 100) / gamesPlayed
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(
        val profile: PlayerProfile,
        val recentForm: RecentFormAggregate?,
        val ddragonVersion: String,
    ) : ProfileUiState()
    data class Error(val error: ApiResult.Error) : ProfileUiState()
}

class ProfileViewModel(
    private val gameName: String,
    private val tagLine: String,
    private val platformRegion: String,
    private val profileRepository: ProfileRepository,
    private val matchRepository: MatchRepository,
    private val championRepository: ChampionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        load(forceRefresh = false)
    }

    // Bypasses the cache: otherwise a retry right after a mid-fetch match-list failure can land
    // on the partial page that failure already persisted (still TTL-fresh) instead of actually
    // hitting the network again. See MatchHistoryViewModel.retry() for the same fix.
    fun retry() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean) {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            when (val result = profileRepository.getProfile(gameName, tagLine, platformRegion, forceRefresh)) {
                is ApiResult.Error -> _uiState.value = ProfileUiState.Error(result)
                is ApiResult.Success -> {
                    val profile = result.data
                    settingsRepository.setLastProfile(profile.puuid, profile.platformRegion)
                    settingsRepository.addToSearchHistory(profile.gameName, profile.tagLine, profile.platformRegion)
                    val matchesResult = matchRepository.getRecentMatches(
                        profile.puuid, profile.platformRegion, count = RECENT_MATCH_COUNT, forceRefresh = forceRefresh
                    )
                    val recentForm = (matchesResult as? ApiResult.Success)?.data?.matches?.toRecentFormAggregate()
                    val version = (championRepository.getLatestVersion() as? ApiResult.Success)?.data
                        ?: FALLBACK_DDRAGON_VERSION
                    _uiState.value = ProfileUiState.Success(profile, recentForm, version)
                }
            }
        }
    }
}

/** Shared with ComparisonViewModel, which computes the same recent-form summary per side. */
fun List<MatchSummary>.toRecentFormAggregate(): RecentFormAggregate? {
    if (isEmpty()) return null
    val wins = count { it.win }
    val avgKda = map {
        val deaths = it.deaths.coerceAtLeast(1)
        (it.kills + it.assists).toDouble() / deaths
    }.average()
    return RecentFormAggregate(size, wins, avgKda)
}
