package com.veronezzi.riftlog.ui.builds

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.veronezzi.riftlog.R
import com.veronezzi.riftlog.RiftLogApplication
import com.veronezzi.riftlog.databinding.FragmentBuildsBinding
import com.veronezzi.riftlog.ui.common.bindError
import com.veronezzi.riftlog.ui.common.setSkeletonVisible
import com.rifttracker.designsystem.databinding.ViewEmptyStateBinding
import kotlinx.coroutines.launch

class BuildsFragment : Fragment(R.layout.fragment_builds) {

    private var _binding: FragmentBuildsBinding? = null
    private val binding get() = _binding!!
    private var _emptyStateBinding: ViewEmptyStateBinding? = null
    private val emptyStateBinding get() = _emptyStateBinding!!

    private val viewModel: BuildsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                BuildsViewModel(app.championRepository, app.settingsRepository)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentBuildsBinding.bind(view)
        _emptyStateBinding = ViewEmptyStateBinding.bind(binding.emptyState)
        val adapter = BuildsAdapter { champion ->
            findNavController().navigate(
                R.id.action_builds_to_championDetail,
                bundleOf("championId" to champion.id),
            )
        }
        binding.championList.layoutManager = LinearLayoutManager(requireContext())
        binding.championList.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onQueryChanged(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        emptyStateBinding.emptyStateRetryButton.setOnClickListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state, adapter) }
            }
        }
    }

    private fun render(state: BuildsUiState, adapter: BuildsAdapter) {
        binding.skeletonContainer.setSkeletonVisible(state is BuildsUiState.Loading)
        binding.championList.visibility = if (state is BuildsUiState.Success) View.VISIBLE else View.GONE
        binding.emptyState.visibility = when (state) {
            is BuildsUiState.Error -> View.VISIBLE
            is BuildsUiState.Success -> if (state.visible.isEmpty()) View.VISIBLE else View.GONE
            else -> View.GONE
        }

        when (state) {
            is BuildsUiState.Loading -> Unit
            is BuildsUiState.Error -> emptyStateBinding.bindError(
                error = state.error,
                onRetry = { viewModel.retry() },
            )
            is BuildsUiState.Success -> {
                if (state.visible.isEmpty()) {
                    emptyStateBinding.emptyStateTitle.text = getString(R.string.builds_empty_title)
                    emptyStateBinding.emptyStateMessage.text = getString(R.string.builds_empty_message)
                    emptyStateBinding.emptyStateRetryButton.visibility = View.GONE
                } else {
                    adapter.submitList(state.visible)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _emptyStateBinding = null
        _binding = null
    }
}
