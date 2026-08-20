package com.veronezzi.riftlog.ui.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veronezzi.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.MatchRepository
import com.veronezzi.riftlog.data.repository.ProfileRepository
import com.veronezzi.riftlog.data.settings.RecentSearch
import com.veronezzi.riftlog.data.settings.SettingsRepository
import com.veronezzi.riftlog.domain.ApiResult
import com.veronezzi.riftlog.domain.model.PlayerProfile
import com.veronezzi.riftlog.ui.common.RiotIdValidator
import com.veronezzi.riftlog.ui.profile.RecentFormAggregate
import com.veronezzi.riftlog.ui.profile.toRecentFormAggregate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val RECENT_MATCH_COUNT = 20
private const val MAX_SUGGESTIONS = 5

sealed class ComparisonSlotState {
    /** Player B only: search box shown, nothing looked up yet. */
    object Empty : ComparisonSlotState()
    object Loading : ComparisonSlotState()
    data class Success(val profile: PlayerProfile, val recentForm: RecentFormAggregate?) : ComparisonSlotState()
    data class Error(val error: ApiResult.Error) : ComparisonSlotState()
}

data class ComparisonUiState(
    val playerA: ComparisonSlotState = ComparisonSlotState.Loading,
    val playerB: ComparisonSlotState = ComparisonSlotState.Empty,
    val ddragonVersion: String = FALLBACK_DDRAGON_VERSION,
    val playerBInputError: Boolean = false,
    val playerBSuggestions: List<RecentSearch> = emptyList(),
)

/** Player A is whatever profile the user was already viewing, passed in as nav args. Player B is
 * searched independently on this screen, reusing the same Riot ID format + local search-history
 * suggestions as Home. Player B is always looked up on Player A's platform region - there's no
 * second region picker, since a rift/friend comparison is almost always same-region and adding a
 * second region selector would double the input surface for a case that barely comes up. */
class ComparisonViewModel(
    private val playerAGameName: String,
    private val playerATagLine: String,
    private val playerAPlatformRegion: String,
    private val profileRepository: ProfileRepository,
    private val matchRepository: MatchRepository,
    private val championRepository: ChampionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState

    private var searchHistory: List<RecentSearch> = emptyList()
    private var playerBQuery: String = ""
    private var lastPlayerBRequest: RiotIdRequest? = null

    init {
        loadPlayerA(forceRefresh = false)
        loadDdragonVersion()
        viewModelScope.launch {
            settingsRepository.searchHistory.collectLatest { history ->
                searchHistory = history
                _uiState.value = _uiState.value.copy(playerBSuggestions = filteredSuggestions())
            }
        }
    }

    fun retryPlayerA() = loadPlayerA(forceRefresh = true)

    fun retryPlayerB() {
        lastPlayerBRequest?.let { searchPlayerB(it.gameName, it.tagLine, forceRefresh = true) }
    }

    fun onPlayerBQueryChanged(query: String) {
        playerBQuery = query
        _uiState.value = _uiState.value.copy(playerBSuggestions = filteredSuggestions())
    }

    fun onPlayerBSuggestionTapped(suggestion: RecentSearch) {
        playerBQuery = ""
        searchPlayerB(suggestion.gameName, suggestion.tagLine)
    }

    fun onPlayerBSearchSubmitted(rawRiotId: String) {
        val parsed = RiotIdValidator.parse(rawRiotId)
        if (parsed == null) {
            _uiState.value = _uiState.value.copy(playerBInputError = true)
            return
        }
        playerBQuery = ""
        searchPlayerB(parsed.gameName, parsed.tagLine)
    }

    /** Drops the loaded Player B back to the search box so the user can compare against someone
     * else without leaving the screen. */
    fun onPlayerBChangeRequested() {
        lastPlayerBRequest = null
        _uiState.value = _uiState.value.copy(playerB = ComparisonSlotState.Empty, playerBInputError = false)
    }

    private fun searchPlayerB(gameName: String, tagLine: String, forceRefresh: Boolean = false) {
        lastPlayerBRequest = RiotIdRequest(gameName, tagLine)
        _uiState.value = _uiState.value.copy(
            playerB = ComparisonSlotState.Loading,
            playerBInputError = false,
            playerBSuggestions = emptyList(),
        )
        viewModelScope.launch {
            val slot = loadSlot(gameName, tagLine, playerAPlatformRegion, forceRefresh, updatePinnedProfile = false)
            _uiState.value = _uiState.value.copy(playerB = slot)
        }
    }

    private fun loadPlayerA(forceRefresh: Boolean) {
        _uiState.value = _uiState.value.copy(playerA = ComparisonSlotState.Loading)
        viewModelScope.launch {
            val slot = loadSlot(
                playerAGameName, playerATagLine, playerAPlatformRegion, forceRefresh, updatePinnedProfile = true
            )
            _uiState.value = _uiState.value.copy(playerA = slot)
        }
    }

    private fun loadDdragonVersion() {
        viewModelScope.launch {
            val version = (championRepository.getLatestVersion() as? ApiResult.Success)?.data
                ?: FALLBACK_DDRAGON_VERSION
            _uiState.value = _uiState.value.copy(ddragonVersion = version)
        }
    }

    // updatePinnedProfile is only true for Player A: Player A is the profile the user was already
    // viewing (and already pinned it, via ProfileViewModel, before navigating here). Player B must
    // NOT touch settingsRepository.setLastProfile - that's the puuid the Match History and Champion
    // Stats tabs read as "the current profile", and letting a comparison search silently repoint
    // those tabs at whoever Player B is would be a much worse bug than the comparison screen itself.
    private suspend fun loadSlot(
        gameName: String,
        tagLine: String,
        platformRegion: String,
        forceRefresh: Boolean,
        updatePinnedProfile: Boolean,
    ): ComparisonSlotState {
        val profileResult = profileRepository.getProfile(gameName, tagLine, platformRegion, forceRefresh)
        val profile = when (profileResult) {
            is ApiResult.Error -> return ComparisonSlotState.Error(profileResult)
            is ApiResult.Success -> profileResult.data
        }
        if (updatePinnedProfile) settingsRepository.setLastProfile(profile.puuid, profile.platformRegion)
        settingsRepository.addToSearchHistory(profile.gameName, profile.tagLine, profile.platformRegion)

        val matchesResult = matchRepository.getRecentMatches(
            profile.puuid, profile.platformRegion, count = RECENT_MATCH_COUNT, forceRefresh = forceRefresh
        )
        val recentForm = (matchesResult as? ApiResult.Success)?.data?.matches?.toRecentFormAggregate()
        return ComparisonSlotState.Success(profile, recentForm)
    }

    // Player B is always looked up on Player A's region (see the class doc), so a suggestion from
    // a different region would 404 with no explanation if it were tappable - filter those out
    // rather than let the user hit a dead end.
    private fun filteredSuggestions(): List<RecentSearch> {
        val query = playerBQuery.trim()
        if (query.isBlank() || query.contains("#")) return emptyList()
        return searchHistory
            .filter { it.platformRegion == playerAPlatformRegion && it.gameName.contains(query, ignoreCase = true) }
            .take(MAX_SUGGESTIONS)
    }

    private data class RiotIdRequest(val gameName: String, val tagLine: String)
}
