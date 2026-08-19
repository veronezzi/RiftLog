package com.example.riftlog.ui.home

import android.os.Bundle
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
import coil.load
import com.example.riftlog.R
import com.example.riftlog.RiftLogApplication
import com.example.riftlog.data.remote.RegionMapper
import com.example.riftlog.data.remote.ddragon.DDragonUrls
import com.example.riftlog.databinding.FragmentHomeBinding
import com.example.riftlog.ui.common.RegionDisplay
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                HomeViewModel(app.settingsRepository, app.profileRepository, app.championRepository)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)
        setUpRegionChips()
        setUpListeners()
        observeState()
        observeEvents()
    }

    private fun setUpRegionChips() {
        binding.regionChipGroup.removeAllViews()
        RegionMapper.platformIds.forEach { region ->
            val chip = layoutInflater.inflate(R.layout.item_region_chip, binding.regionChipGroup, false) as Chip
            chip.text = RegionDisplay.labelFor(region)
            chip.tag = region
            chip.setOnClickListener { viewModel.onRegionSelected(region) }
            binding.regionChipGroup.addView(chip)
        }
    }

    private fun setUpListeners() {
        binding.searchButton.setOnClickListener {
            val riotId = binding.riotIdInput.text?.toString().orEmpty()
            viewModel.onSearchSubmitted(riotId)
        }
        binding.pinnedProfileCard.setOnClickListener { viewModel.onPinnedProfileTapped() }
        binding.unpinButton.setOnClickListener { viewModel.onUnpinClicked() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    for (i in 0 until binding.regionChipGroup.childCount) {
                        val chip = binding.regionChipGroup.getChildAt(i) as Chip
                        chip.isChecked = chip.tag == state.selectedRegion
                    }
                    binding.errorText.visibility = if (state.inputError != null) View.VISIBLE else View.GONE
                    if (state.inputError != null) {
                        binding.errorText.text = getString(R.string.home_error_invalid_riot_id)
                    }
                    renderPinned(state.pinned)
                }
            }
        }
    }

    private fun renderPinned(pinned: PinnedProfileState) {
        when (pinned) {
            is PinnedProfileState.None -> {
                binding.pinnedProfileCard.visibility = View.GONE
            }
            is PinnedProfileState.Loading -> {
                // Keep whatever was showing (usually the cached copy) until the refresh resolves,
                // instead of flashing the card away and back.
            }
            is PinnedProfileState.Loaded -> {
                val profile = pinned.profile
                binding.pinnedProfileCard.visibility = View.VISIBLE
                binding.pinnedName.text = "${profile.gameName}#${profile.tagLine}"
                binding.pinnedLevel.text = getString(R.string.profile_level_format, profile.summonerLevel.toInt())
                binding.pinnedAvatar.load(DDragonUrls.profileIcon(pinned.ddragonVersion, profile.profileIconId)) {
                    placeholder(R.drawable.bg_skeleton_block)
                    error(R.drawable.bg_skeleton_block)
                }
            }
            is PinnedProfileState.Error -> {
                val recent = pinned.recentSearch
                binding.pinnedProfileCard.visibility = View.VISIBLE
                binding.pinnedName.text = "${recent.gameName}#${recent.tagLine} (${RegionDisplay.labelFor(recent.platformRegion)})"
                binding.pinnedLevel.text = null
                binding.pinnedAvatar.setImageResource(R.drawable.bg_skeleton_block)
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    when (event) {
                        is HomeEvent.NavigateToProfile -> {
                            findNavController().navigate(
                                R.id.action_home_to_profile,
                                bundleOf(
                                    "gameName" to event.gameName,
                                    "tagLine" to event.tagLine,
                                    "platformRegion" to event.platformRegion,
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
