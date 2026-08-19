package com.veronezzi.riftlog.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.MatchRepository
import com.veronezzi.riftlog.data.repository.ProfileRepository
import com.veronezzi.riftlog.data.settings.SettingsRepository
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.domain.model.MatchSummary
import com.veronezzi.riftlog.domain.model.PlayerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val RECENT_MATCH_COUNT = 20
private const val FALLBACK_DDRAGON_VERSION = "14.1.1"

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
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            when (val result = profileRepository.getProfile(gameName, tagLine, platformRegion)) {
                is ApiResult.Error -> _uiState.value = ProfileUiState.Error(result)
                is ApiResult.Success -> {
                    val profile = result.data
                    settingsRepository.setLastProfile(profile.puuid, profile.platformRegion)
                    val matchesResult = matchRepository.getRecentMatches(
                        profile.puuid, profile.platformRegion, count = RECENT_MATCH_COUNT
                    )
                    val recentForm = (matchesResult as? ApiResult.Success)?.data?.let(::toAggregate)
                    val version = (championRepository.getLatestVersion() as? ApiResult.Success)?.data
                        ?: FALLBACK_DDRAGON_VERSION
                    _uiState.value = ProfileUiState.Success(profile, recentForm, version)
                }
            }
        }
    }

    private fun toAggregate(matches: List<MatchSummary>): RecentFormAggregate? {
        if (matches.isEmpty()) return null
        val wins = matches.count { it.win }
        val avgKda = matches.map {
            val deaths = it.deaths.coerceAtLeast(1)
            (it.kills + it.assists).toDouble() / deaths
        }.average()
        return RecentFormAggregate(matches.size, wins, avgKda)
    }
}
