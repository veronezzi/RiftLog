package com.veronezzi.riftlog.ui.comparison

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.load
import com.veronezzi.riftlog.R
import com.veronezzi.riftlog.RiftLogApplication
import com.veronezzi.riftlog.data.remote.ddragon.DDragonUrls
import com.veronezzi.riftlog.data.settings.RecentSearch
import com.veronezzi.riftlog.databinding.FragmentComparisonBinding
import com.veronezzi.riftlog.databinding.ViewCompareStatRowBinding
import com.veronezzi.riftlog.domain.model.PlayerProfile
import com.veronezzi.riftlog.domain.model.RankEntry
import com.veronezzi.riftlog.ui.common.bindError
import com.veronezzi.riftlog.ui.profile.RecentFormAggregate
import com.rifttracker.designsystem.databinding.ViewEmptyStateBinding
import com.rifttracker.designsystem.R as DesignR
import kotlinx.coroutines.launch

class ComparisonFragment : Fragment(R.layout.fragment_comparison) {

    private var _binding: FragmentComparisonBinding? = null
    private val binding get() = _binding!!
    private var _playerAEmptyStateBinding: ViewEmptyStateBinding? = null
    private val playerAEmptyStateBinding get() = _playerAEmptyStateBinding!!
    private var _playerBEmptyStateBinding: ViewEmptyStateBinding? = null
    private val playerBEmptyStateBinding get() = _playerBEmptyStateBinding!!

    private val viewModel: ComparisonViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                ComparisonViewModel(
                    playerAGameName = requireArguments().getString("gameName")!!,
                    playerATagLine = requireArguments().getString("tagLine")!!,
                    playerAPlatformRegion = requireArguments().getString("platformRegion")!!,
                    profileRepository = app.profileRepository,
                    matchRepository = app.matchRepository,
                    championRepository = app.championRepository,
                    settingsRepository = app.settingsRepository,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentComparisonBinding.bind(view)
        _playerAEmptyStateBinding = ViewEmptyStateBinding.bind(binding.playerAEmptyState)
        _playerBEmptyStateBinding = ViewEmptyStateBinding.bind(binding.playerBEmptyState)

        binding.playerBSearchButton.setOnClickListener {
            viewModel.onPlayerBSearchSubmitted(binding.playerBInput.text?.toString().orEmpty())
        }
        binding.playerBInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onPlayerBQueryChanged(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        binding.playerBChangeButton.setOnClickListener { viewModel.onPlayerBChangeRequested() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ComparisonUiState) {
        binding.playerBSearchSection.visibility = if (state.playerB is ComparisonSlotState.Empty) View.VISIBLE else View.GONE
        binding.playerBInputErrorText.visibility = if (state.playerBInputError) View.VISIBLE else View.GONE
        renderSuggestions(state.playerBSuggestions)

        renderPlayerA(state)
        renderPlayerB(state)
        renderStats(state)
    }

    private fun renderPlayerA(state: ComparisonUiState) {
        val slot = state.playerA
        binding.playerASkeleton.visibility = if (slot is ComparisonSlotState.Loading) View.VISIBLE else View.GONE
        binding.playerAContent.visibility = if (slot is ComparisonSlotState.Success) View.VISIBLE else View.GONE
        binding.playerAEmptyState.visibility = if (slot is ComparisonSlotState.Error) View.VISIBLE else View.GONE
        when (slot) {
            is ComparisonSlotState.Success -> bindHeader(
                slot.profile, state.ddragonVersion, binding.playerAAvatar, binding.playerAName, binding.playerALevel
            )
            is ComparisonSlotState.Error -> playerAEmptyStateBinding.bindError(slot.error, onRetry = { viewModel.retryPlayerA() })
            else -> Unit
        }
    }

    private fun renderPlayerB(state: ComparisonUiState) {
        val slot = state.playerB
        binding.playerBPlaceholder.visibility = if (slot is ComparisonSlotState.Empty) View.VISIBLE else View.GONE
        binding.playerBSkeleton.visibility = if (slot is ComparisonSlotState.Loading) View.VISIBLE else View.GONE
        binding.playerBContent.visibility = if (slot is ComparisonSlotState.Success) View.VISIBLE else View.GONE
        binding.playerBEmptyState.visibility = if (slot is ComparisonSlotState.Error) View.VISIBLE else View.GONE
        when (slot) {
            is ComparisonSlotState.Success -> bindHeader(
                slot.profile, state.ddragonVersion, binding.playerBAvatar, binding.playerBName, binding.playerBLevel
            )
            is ComparisonSlotState.Error -> playerBEmptyStateBinding.bindError(slot.error, onRetry = { viewModel.retryPlayerB() })
            else -> Unit
        }
    }

    private fun bindHeader(
        profile: PlayerProfile,
        ddragonVersion: String,
        avatar: com.google.android.material.imageview.ShapeableImageView,
        name: android.widget.TextView,
        level: android.widget.TextView,
    ) {
        name.text = "${profile.gameName}#${profile.tagLine}"
        level.text = getString(R.string.profile_level_format, profile.summonerLevel.toInt())
        avatar.load(DDragonUrls.profileIcon(ddragonVersion, profile.profileIconId)) {
            placeholder(R.drawable.bg_skeleton_block)
            error(R.drawable.bg_skeleton_block)
        }
    }

    private fun renderSuggestions(suggestions: List<RecentSearch>) {
        binding.playerBSuggestionsContainer.removeAllViews()
        binding.playerBSuggestionsCard.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE
        val paddingPx = resources.getDimensionPixelSize(DesignR.dimen.spacing_md)
        suggestions.forEach { suggestion ->
            val row = android.widget.TextView(requireContext()).apply {
                text = "${suggestion.gameName}#${suggestion.tagLine}"
                setTextAppearance(DesignR.style.TextAppearance_RiftTracker_Body)
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                isClickable = true
                isFocusable = true
                val outValue = android.util.TypedValue()
                requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { viewModel.onPlayerBSuggestionTapped(suggestion) }
            }
            binding.playerBSuggestionsContainer.addView(row)
        }
    }

    private fun renderStats(state: ComparisonUiState) {
        val a = state.playerA as? ComparisonSlotState.Success
        val b = state.playerB as? ComparisonSlotState.Success
        binding.compareStatsContainer.removeAllViews()
        if (a == null || b == null) {
            binding.compareStatsContainer.visibility = View.GONE
            return
        }
        binding.compareStatsContainer.visibility = View.VISIBLE

        addStatRow(
            getString(R.string.comparison_stat_level),
            a.profile.summonerLevel.toString(),
            b.profile.summonerLevel.toString(),
            (a.profile.summonerLevel - b.profile.summonerLevel).let { if (it > 0) 1 else if (it < 0) -1 else 0 },
        )

        val soloA = findRank(a.profile, "RANKED_SOLO_5x5")
        val soloB = findRank(b.profile, "RANKED_SOLO_5x5")
        addStatRow(
            getString(R.string.profile_solo_duo), formatRank(soloA), formatRank(soloB),
            RankComparator.compare(soloA, soloB),
        )

        val flexA = findRank(a.profile, "RANKED_FLEX_SR")
        val flexB = findRank(b.profile, "RANKED_FLEX_SR")
        addStatRow(
            getString(R.string.profile_flex), formatRank(flexA), formatRank(flexB),
            RankComparator.compare(flexA, flexB),
        )

        addStatRow(
            getString(R.string.profile_stat_winrate),
            "${a.recentForm?.winRatePercent ?: 0}%",
            "${b.recentForm?.winRatePercent ?: 0}%",
            compareRecentForm(a.recentForm, b.recentForm) { it.winRatePercent },
        )

        addStatRow(
            getString(R.string.profile_stat_kda),
            a.recentForm?.let { "%.2f".format(it.avgKda) } ?: "-",
            b.recentForm?.let { "%.2f".format(it.avgKda) } ?: "-",
            compareRecentForm(a.recentForm, b.recentForm) { it.avgKda },
        )
    }

    /** Only compares recent-form stats when both sides actually have games in the window - a
     * side with zero recent games isn't "worse", it's just missing data, so neither gets
     * highlighted rather than one side winning on an empty 0%/"-" default. */
    private fun <T : Comparable<T>> compareRecentForm(
        a: RecentFormAggregate?,
        b: RecentFormAggregate?,
        selector: (RecentFormAggregate) -> T,
    ): Int {
        if (a == null || b == null) return 0
        return selector(a).compareTo(selector(b))
    }

    private fun findRank(profile: PlayerProfile, queueType: String): RankEntry? =
        profile.rankEntries.firstOrNull { it.queueType == queueType }

    private fun formatRank(rank: RankEntry?): String {
        if (rank == null) return getString(R.string.profile_unranked)
        val tier = rank.tier.lowercase().replaceFirstChar { it.uppercase() }
        return getString(R.string.comparison_rank_format, tier, rank.rank, rank.leaguePoints)
    }

    private fun addStatRow(label: String, valueA: String, valueB: String, comparison: Int) {
        val rowBinding = ViewCompareStatRowBinding.inflate(
            LayoutInflater.from(requireContext()), binding.compareStatsContainer, false
        )
        rowBinding.rowLabel.text = label
        rowBinding.valueA.text = valueA
        rowBinding.valueB.text = valueB
        val winColor = ContextCompat.getColor(requireContext(), DesignR.color.rift_win)
        when {
            comparison > 0 -> highlight(rowBinding.valueA, winColor)
            comparison < 0 -> highlight(rowBinding.valueB, winColor)
        }
        binding.compareStatsContainer.addView(rowBinding.root)
    }

    private fun highlight(textView: android.widget.TextView, color: Int) {
        textView.setTextColor(color)
        textView.setTypeface(textView.typeface, Typeface.BOLD)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _playerAEmptyStateBinding = null
        _playerBEmptyStateBinding = null
        _binding = null
    }
}
