package com.veronezzi.riftlog.ui.championdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import coil.load
import com.veronezzi.riftlog.R
import com.veronezzi.riftlog.RiftLogApplication
import com.veronezzi.riftlog.data.remote.ddragon.DDragonUrls
import com.veronezzi.riftlog.databinding.FragmentChampionDetailBinding
import com.veronezzi.riftlog.databinding.ItemAbilityRowBinding
import com.veronezzi.riftlog.databinding.ItemStatRowBinding
import com.veronezzi.riftlog.domain.model.AbilityInfo
import com.veronezzi.riftlog.domain.model.ChampionDetail
import com.veronezzi.riftlog.domain.model.ProBuild
import kotlinx.coroutines.launch

class ChampionDetailFragment : Fragment(R.layout.fragment_champion_detail) {

    private var _binding: FragmentChampionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChampionDetailViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                ChampionDetailViewModel(
                    championId = requireArguments().getString("championId")!!,
                    championRepository = app.championRepository,
                    matchRepository = app.matchRepository,
                    settingsRepository = app.settingsRepository,
                    proBuildRepository = app.proBuildRepository,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentChampionDetailBinding.bind(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ChampionDetailUiState) {
        binding.loadingIndicator.visibility = if (state is ChampionDetailUiState.Loading) View.VISIBLE else View.GONE
        binding.contentScroll.visibility = if (state is ChampionDetailUiState.Success) View.VISIBLE else View.GONE
        if (state is ChampionDetailUiState.Success) bindDetail(state)
    }

    private fun bindDetail(state: ChampionDetailUiState.Success) {
        val detail = state.detail
        binding.championName.text = detail.info.name
        binding.championTitle.text = detail.info.title
        binding.splashImage.load(detail.info.splashImageUrl)

        bindStats(detail)
        bindAbilities(detail)
        bindBuild(state.recommendedBuild, state.ddragonVersion)
        bindProBuild(state.proBuild, state.proBuildFailed)
    }

    private fun bindStats(detail: ChampionDetail) {
        binding.statsGrid.removeAllViews()
        val stats = listOf(
            getString(R.string.champion_stat_hp) to detail.hp.toInt().toString(),
            getString(R.string.champion_stat_mp) to detail.mp.toInt().toString(),
            getString(R.string.champion_stat_armor) to detail.armor.toInt().toString(),
            getString(R.string.champion_stat_mr) to detail.magicResist.toInt().toString(),
            getString(R.string.champion_stat_ad) to detail.attackDamage.toInt().toString(),
            getString(R.string.champion_stat_as) to "%.2f".format(detail.attackSpeed),
            getString(R.string.champion_stat_ms) to detail.moveSpeed.toInt().toString(),
        )
        stats.chunked(2).forEach { pair ->
            val row = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            }
            pair.forEach { (label, value) ->
                val rowBinding = ItemStatRowBinding.inflate(LayoutInflater.from(requireContext()), row, false)
                rowBinding.statLabel.text = label
                rowBinding.statValue.text = value
                row.addView(rowBinding.root)
            }
            binding.statsGrid.addView(row)
        }
    }

    private fun bindAbilities(detail: ChampionDetail) {
        binding.abilitiesContainer.removeAllViews()
        val keys = listOf("P", "Q", "W", "E", "R")
        val abilities: List<AbilityInfo> = listOf(detail.passive) + detail.spells
        abilities.forEachIndexed { index, ability ->
            val rowBinding = ItemAbilityRowBinding.inflate(
                LayoutInflater.from(requireContext()), binding.abilitiesContainer, false
            )
            rowBinding.abilityKey.text = keys.getOrElse(index) { "" }
            rowBinding.abilityName.text = ability.name
            rowBinding.abilityDescription.text = android.text.Html.fromHtml(
                ability.description, android.text.Html.FROM_HTML_MODE_COMPACT
            )
            rowBinding.abilityIcon.load(ability.imageUrl) {
                placeholder(R.drawable.bg_skeleton_block)
                error(R.drawable.bg_skeleton_block)
            }
            binding.abilitiesContainer.addView(rowBinding.root)
        }
    }

    private fun bindBuild(recommendedBuild: List<Int>?, ddragonVersion: String) {
        binding.buildItemRow.removeAllViews()
        val items = recommendedBuild?.filter { it != 0 }.orEmpty()
        binding.noBuildText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.buildItemRow.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        items.forEach { itemId ->
            val icon = android.widget.ImageView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(40.dpToPx(), 40.dpToPx()).apply {
                    setMargins(0, 0, 8.dpToPx(), 0)
                }
                load(DDragonUrls.itemIcon(ddragonVersion, itemId)) {
                    placeholder(R.drawable.bg_skeleton_block)
                    error(R.drawable.bg_skeleton_block)
                }
            }
            binding.buildItemRow.addView(icon)
        }
    }

    private fun bindProBuild(proBuild: ProBuild?, proBuildFailed: Boolean) {
        binding.proBuildItemRow.removeAllViews()
        if (proBuild == null) {
            binding.proBuildLabel.text = getString(R.string.champion_pro_build_header)
            binding.proBuildItemRow.visibility = View.GONE
            binding.noProBuildText.visibility = View.VISIBLE
            binding.noProBuildText.text = getString(
                if (proBuildFailed) R.string.champion_pro_build_failed else R.string.champion_pro_build_unavailable
            )
            return
        }
        binding.noProBuildText.visibility = View.GONE
        binding.proBuildItemRow.visibility = View.VISIBLE
        binding.proBuildLabel.text = getString(R.string.champion_pro_build_label, proBuild.sampleGames)
        proBuild.items.forEach { item ->
            val icon = android.widget.ImageView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(40.dpToPx(), 40.dpToPx()).apply {
                    setMargins(0, 0, 8.dpToPx(), 0)
                }
                load(item.iconUrl) {
                    placeholder(R.drawable.bg_skeleton_block)
                    error(R.drawable.bg_skeleton_block)
                }
            }
            binding.proBuildItemRow.addView(icon)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
