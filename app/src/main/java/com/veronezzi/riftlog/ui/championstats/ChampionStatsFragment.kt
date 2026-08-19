package com.veronezzi.riftlog.ui.championstats

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.veronezzi.riftlog.R
import com.veronezzi.riftlog.RiftLogApplication
import com.veronezzi.riftlog.databinding.FragmentChampionStatsBinding
import com.veronezzi.riftlog.ui.common.bindError
import com.veronezzi.riftlog.ui.common.setSkeletonVisible
import com.rifttracker.designsystem.databinding.ViewEmptyStateBinding
import kotlinx.coroutines.launch

class ChampionStatsFragment : Fragment(R.layout.fragment_champion_stats) {

    private var _binding: FragmentChampionStatsBinding? = null
    private val binding get() = _binding!!
    private var _emptyStateBinding: ViewEmptyStateBinding? = null
    private val emptyStateBinding get() = _emptyStateBinding!!

    private val viewModel: ChampionStatsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                ChampionStatsViewModel(app.championRepository, app.matchRepository, app.settingsRepository)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentChampionStatsBinding.bind(view)
        _emptyStateBinding = ViewEmptyStateBinding.bind(binding.emptyState)
        val adapter = ChampionAdapter { item ->
            findNavController().navigate(
                R.id.action_championStats_to_championDetail,
                bundleOf("championId" to item.info.id),
            )
        }
        binding.championList.layoutManager = LinearLayoutManager(requireContext())
        binding.championList.adapter = adapter

        binding.sortByMastery.setOnClickListener { viewModel.onSortSelected(ChampionSort.MASTERY) }
        binding.sortByWinrate.setOnClickListener { viewModel.onSortSelected(ChampionSort.WINRATE) }
        binding.sortByGames.setOnClickListener { viewModel.onSortSelected(ChampionSort.GAMES) }
        emptyStateBinding.emptyStateRetryButton.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state, adapter) }
            }
        }
    }

    private fun render(state: ChampionStatsUiState, adapter: ChampionAdapter) {
        binding.skeletonContainer.setSkeletonVisible(state is ChampionStatsUiState.Loading)
        binding.championList.visibility = if (state is ChampionStatsUiState.Success) View.VISIBLE else View.GONE
        binding.sortChipGroup.visibility = if (state is ChampionStatsUiState.Success) View.VISIBLE else View.GONE
        binding.emptyState.visibility =
            if (state is ChampionStatsUiState.Error || state is ChampionStatsUiState.NoProfile) View.VISIBLE else View.GONE

        when (state) {
            is ChampionStatsUiState.Loading -> Unit
            is ChampionStatsUiState.NoProfile -> {
                emptyStateBinding.emptyStateTitle.text = getString(R.string.champion_stats_empty_title)
                emptyStateBinding.emptyStateMessage.text = getString(R.string.champion_stats_empty_message)
                emptyStateBinding.emptyStateRetryButton.visibility = View.GONE
            }
            is ChampionStatsUiState.Error -> emptyStateBinding.bindError(
                error = state.error,
                onRetry = { viewModel.retry() },
            )
            is ChampionStatsUiState.Success -> adapter.submitList(state.items)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _emptyStateBinding = null
        _binding = null
    }
}
