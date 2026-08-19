package com.example.riftlog.ui.matchhistory

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.riftlog.R
import com.example.riftlog.RiftLogApplication
import com.example.riftlog.data.remote.ddragon.FALLBACK_DDRAGON_VERSION
import com.example.riftlog.databinding.FragmentMatchHistoryBinding
import com.example.riftlog.ui.common.bindError
import com.example.riftlog.ui.common.setSkeletonVisible
import com.rifttracker.designsystem.databinding.ViewEmptyStateBinding
import kotlinx.coroutines.launch

class MatchHistoryFragment : Fragment(R.layout.fragment_match_history) {

    private var _binding: FragmentMatchHistoryBinding? = null
    private val binding get() = _binding!!
    private var _emptyStateBinding: ViewEmptyStateBinding? = null
    private val emptyStateBinding get() = _emptyStateBinding!!
    private var adapter: MatchAdapter? = null

    private val viewModel: MatchHistoryViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                MatchHistoryViewModel(app.matchRepository, app.championRepository, app.settingsRepository)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMatchHistoryBinding.bind(view)
        _emptyStateBinding = ViewEmptyStateBinding.bind(binding.emptyState)
        val matchAdapter = MatchAdapter(ddragonVersion = FALLBACK_DDRAGON_VERSION)
        adapter = matchAdapter
        binding.matchList.layoutManager = LinearLayoutManager(requireContext())
        binding.matchList.adapter = matchAdapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.retry() }
        binding.loadMoreButton.setOnClickListener { viewModel.loadMore() }
        emptyStateBinding.emptyStateRetryButton.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: MatchHistoryUiState) {
        binding.swipeRefresh.isRefreshing = false
        binding.skeletonContainer.setSkeletonVisible(state is MatchHistoryUiState.Loading)
        binding.swipeRefresh.visibility = if (state is MatchHistoryUiState.Success) View.VISIBLE else View.GONE
        binding.emptyState.visibility =
            if (state is MatchHistoryUiState.Error || state is MatchHistoryUiState.NoProfile) View.VISIBLE else View.GONE
        binding.loadMoreButton.visibility = View.GONE

        when (state) {
            is MatchHistoryUiState.Loading -> Unit
            is MatchHistoryUiState.NoProfile -> {
                emptyStateBinding.emptyStateTitle.text = getString(R.string.match_history_empty_title)
                emptyStateBinding.emptyStateMessage.text = getString(R.string.match_history_empty_message)
                emptyStateBinding.emptyStateRetryButton.visibility = View.GONE
            }
            is MatchHistoryUiState.Error -> emptyStateBinding.bindError(
                error = state.error,
                onRetry = { viewModel.retry() },
            )
            is MatchHistoryUiState.Success -> {
                adapter?.updateVersion(state.ddragonVersion)
                adapter?.submitList(state.matches)
                binding.loadMoreButton.visibility = if (state.canLoadMore) View.VISIBLE else View.GONE
                binding.loadMoreButton.isEnabled = !state.isLoadingMore
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _emptyStateBinding = null
        _binding = null
    }
}
