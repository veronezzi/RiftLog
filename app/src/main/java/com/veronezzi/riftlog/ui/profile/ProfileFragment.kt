package com.veronezzi.riftlog.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.navigation.fragment.findNavController
import coil.load
import com.veronezzi.riftlog.R
import com.veronezzi.riftlog.RiftLogApplication
import com.veronezzi.riftlog.data.remote.ddragon.DDragonUrls
import com.veronezzi.riftlog.databinding.FragmentProfileBinding
import com.veronezzi.riftlog.domain.model.PlayerProfile
import com.veronezzi.riftlog.domain.model.RankEntry
import com.veronezzi.riftlog.ui.common.RankTierColor
import com.veronezzi.riftlog.ui.common.bindError
import com.veronezzi.riftlog.ui.common.setSkeletonVisible
import com.rifttracker.designsystem.databinding.ViewEmptyStateBinding
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var _emptyStateBinding: ViewEmptyStateBinding? = null
    private val emptyStateBinding get() = _emptyStateBinding!!

    private val viewModel: ProfileViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                ProfileViewModel(
                    gameName = requireArguments().getString("gameName")!!,
                    tagLine = requireArguments().getString("tagLine")!!,
                    platformRegion = requireArguments().getString("platformRegion")!!,
                    profileRepository = app.profileRepository,
                    matchRepository = app.matchRepository,
                    championRepository = app.championRepository,
                    settingsRepository = app.settingsRepository,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentProfileBinding.bind(view)
        _emptyStateBinding = ViewEmptyStateBinding.bind(binding.emptyState)
        binding.matchHistoryButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_matchHistory)
        }
        binding.championStatsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_championStats)
        }
        emptyStateBinding.emptyStateRetryButton.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ProfileUiState) {
        binding.skeletonContainer.setSkeletonVisible(state is ProfileUiState.Loading)
        binding.contentScroll.visibility = if (state is ProfileUiState.Success) View.VISIBLE else View.GONE
        binding.emptyState.visibility = if (state is ProfileUiState.Error) View.VISIBLE else View.GONE

        when (state) {
            is ProfileUiState.Loading -> Unit
            is ProfileUiState.Error -> emptyStateBinding.bindError(
                error = state.error,
                onRetry = { viewModel.retry() },
            )
            is ProfileUiState.Success -> bindProfile(state)
        }
    }

    private fun bindProfile(state: ProfileUiState.Success) {
        val profile = state.profile
        binding.riotIdText.text = "${profile.gameName}#${profile.tagLine}"
        binding.levelText.text = getString(R.string.profile_level_format, profile.summonerLevel.toInt())
        binding.profileIcon.load(DDragonUrls.profileIcon(state.ddragonVersion, profile.profileIconId)) {
            placeholder(R.drawable.bg_skeleton_block)
            error(R.drawable.bg_skeleton_block)
        }

        bindRankCard(binding.soloDuoRankCard, getString(R.string.profile_solo_duo), findRank(profile, "RANKED_SOLO_5x5"))
        bindRankCard(binding.flexRankCard, getString(R.string.profile_flex), findRank(profile, "RANKED_FLEX_SR"))

        val form = state.recentForm
        binding.recentGamesLabel.text = getString(R.string.profile_recent_games_label, form?.gamesPlayed ?: 0)
        binding.winrateStatCard.statValue.text = "${form?.winRatePercent ?: 0}%"
        binding.winrateStatCard.statLabel.text = getString(R.string.profile_stat_winrate)
        binding.kdaStatCard.statValue.text = form?.let { "%.2f".format(it.avgKda) } ?: "-"
        binding.kdaStatCard.statLabel.text = getString(R.string.profile_stat_kda)
        binding.gamesStatCard.statValue.text = "${form?.gamesPlayed ?: 0}"
        binding.gamesStatCard.statLabel.text = getString(R.string.profile_stat_games)
    }

    private fun findRank(profile: PlayerProfile, queueType: String): RankEntry? =
        profile.rankEntries.firstOrNull { it.queueType == queueType }

    private fun bindRankCard(
        rankCardBinding: com.veronezzi.riftlog.databinding.ViewRankCardBinding,
        queueLabel: String,
        rank: RankEntry?,
    ) {
        rankCardBinding.queueLabel.text = queueLabel
        val context = rankCardBinding.root.context
        if (rank == null) {
            rankCardBinding.tierChip.text = getString(R.string.profile_unranked)
            rankCardBinding.tierChip.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(RankTierColor.colorFor(context, ""))
            rankCardBinding.lpText.text = ""
            rankCardBinding.recordText.text = ""
            rankCardBinding.winrateBar.progress = 0
            return
        }
        rankCardBinding.tierChip.text = "${rank.tier.lowercase().replaceFirstChar { it.uppercase() }} ${rank.rank}"
        rankCardBinding.tierChip.chipBackgroundColor =
            android.content.res.ColorStateList.valueOf(RankTierColor.colorFor(context, rank.tier))
        rankCardBinding.lpText.text = getString(R.string.profile_lp_format, rank.leaguePoints)
        val total = rank.wins + rank.losses
        val winRate = if (total == 0) 0 else ((rank.wins * 100.0) / total).roundToInt()
        rankCardBinding.recordText.text = getString(R.string.profile_record_format, rank.wins, rank.losses, winRate)
        rankCardBinding.winrateBar.progress = winRate
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _emptyStateBinding = null
        _binding = null
    }
}
