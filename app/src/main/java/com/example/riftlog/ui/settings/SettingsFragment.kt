package com.example.riftlog.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.example.riftlog.R
import com.example.riftlog.RiftLogApplication
import com.example.riftlog.data.remote.RegionMapper
import com.example.riftlog.data.settings.SettingsRepository
import com.example.riftlog.databinding.FragmentSettingsBinding
import com.example.riftlog.ui.common.RegionDisplay
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                SettingsViewModel(app.settingsRepository, app.dbHelper)
            }
        }
    }

    private var selectedRegion: String = SettingsRepository.DEFAULT_PLATFORM_REGION

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsBinding.bind(view)
        setUpRegionChips()

        binding.saveButton.setOnClickListener {
            viewModel.save(selectedRegion)
        }
        binding.clearCacheButton.setOnClickListener { viewModel.clearCache() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    val messageRes = when (event) {
                        is SettingsEvent.Saved -> R.string.settings_saved
                        is SettingsEvent.CacheCleared -> R.string.settings_clear_cache_done
                    }
                    Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setUpRegionChips() {
        binding.regionChipGroup.removeAllViews()
        RegionMapper.platformIds.forEach { region ->
            val chip = layoutInflater.inflate(R.layout.item_region_chip, binding.regionChipGroup, false) as Chip
            chip.text = RegionDisplay.labelFor(region)
            chip.tag = region
            chip.setOnClickListener {
                selectedRegion = region
            }
            binding.regionChipGroup.addView(chip)
        }
    }

    private fun render(state: SettingsUiState) {
        selectedRegion = state.platformRegion
        for (i in 0 until binding.regionChipGroup.childCount) {
            val chip = binding.regionChipGroup.getChildAt(i) as Chip
            chip.isChecked = chip.tag == state.platformRegion
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
