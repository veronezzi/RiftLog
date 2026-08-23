package com.veronezzi.riftlog.ui.matchdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import com.veronezzi.riftlog.databinding.FragmentMatchDetailBinding
import com.veronezzi.riftlog.databinding.ItemMatchParticipantRowBinding
import com.veronezzi.riftlog.domain.model.MatchParticipantDetail
import com.veronezzi.riftlog.domain.model.MatchTeamDetail
import com.veronezzi.riftlog.ui.common.bindError
import com.rifttracker.designsystem.databinding.ViewEmptyStateBinding
import kotlinx.coroutines.launch

class MatchDetailFragment : Fragment(R.layout.fragment_match_detail) {

    private var _binding: FragmentMatchDetailBinding? = null
    private val binding get() = _binding!!
    private var _emptyStateBinding: ViewEmptyStateBinding? = null
    private val emptyStateBinding get() = _emptyStateBinding!!

    private val viewModel: MatchDetailViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = requireActivity().application as RiftLogApplication
                MatchDetailViewModel(
                    matchId = requireArguments().getString("matchId")!!,
                    platformRegion = requireArguments().getString("platformRegion")!!,
                    viewerPuuid = requireArguments().getString("puuid")!!,
                    matchRepository = app.matchRepository,
                    championRepository = app.championRepository,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMatchDetailBinding.bind(view)
        _emptyStateBinding = ViewEmptyStateBinding.bind(binding.emptyState)
        emptyStateBinding.emptyStateRetryButton.setOnClickListener { viewModel.retry() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: MatchDetailUiState) {
        binding.loadingIndicator.visibility = if (state is MatchDetailUiState.Loading) View.VISIBLE else View.GONE
        binding.contentScroll.visibility = if (state is MatchDetailUiState.Success) View.VISIBLE else View.GONE
        binding.emptyState.visibility = if (state is MatchDetailUiState.Error) View.VISIBLE else View.GONE
        if (state is MatchDetailUiState.Error) {
            emptyStateBinding.bindError(error = state.error, onRetry = { viewModel.retry() })
        }
        if (state is MatchDetailUiState.Success) bindDetail(state)
    }

    private fun bindDetail(state: MatchDetailUiState.Success) {
        val detail = state.detail
        val minutes = detail.gameDurationSeconds / 60
        val seconds = detail.gameDurationSeconds % 60
        binding.matchDuration.text = "${minutes}m ${seconds}s"

        // Riot's team ordering isn't guaranteed - show the viewer's own side first so the player
        // doesn't have to scroll to find themselves.
        val orderedTeams = detail.teams.sortedByDescending { team -> team.participants.any { it.isViewer } }
        val teamA = orderedTeams.getOrNull(0)
        val teamB = orderedTeams.getOrNull(1)
        bindTeam(teamA, binding.teamAHeader, binding.teamAContainer, state.ddragonVersion)
        bindTeam(teamB, binding.teamBHeader, binding.teamBContainer, state.ddragonVersion)
    }

    private fun bindTeam(
        team: MatchTeamDetail?,
        header: android.widget.TextView,
        container: android.widget.LinearLayout,
        ddragonVersion: String,
    ) {
        container.removeAllViews()
        if (team == null) {
            header.visibility = View.GONE
            return
        }
        header.visibility = View.VISIBLE
        val resultLabel = getString(if (team.win) R.string.match_detail_victory else R.string.match_detail_defeat)
        header.text = getString(
            R.string.match_detail_team_header_format,
            resultLabel, team.baronKills, team.dragonKills, team.towerKills,
        )
        team.participants.forEach { participant ->
            val rowBinding = ItemMatchParticipantRowBinding.inflate(LayoutInflater.from(requireContext()), container, false)
            bindParticipant(rowBinding, participant, ddragonVersion)
            container.addView(rowBinding.root)
        }
    }

    private fun bindParticipant(
        rowBinding: ItemMatchParticipantRowBinding,
        participant: MatchParticipantDetail,
        ddragonVersion: String,
    ) {
        rowBinding.participantChampionIcon.load(
            DDragonUrls.championSquare(ddragonVersion, "${participant.championName}.png")
        ) {
            placeholder(R.drawable.bg_skeleton_block)
            error(R.drawable.bg_skeleton_block)
        }
        rowBinding.participantName.text = participant.displayName
        rowBinding.participantName.setTypeface(null, if (participant.isViewer) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        val kda = if (participant.deaths == 0) {
            (participant.kills + participant.assists).toDouble()
        } else {
            (participant.kills + participant.assists).toDouble() / participant.deaths
        }
        rowBinding.participantKda.text = getString(
            R.string.match_detail_kda_format, participant.kills, participant.deaths, participant.assists, kda,
        )
        rowBinding.participantStats.text = getString(
            R.string.match_detail_stats_format,
            participant.totalMinionsKilled,
            participant.totalDamageDealtToChampions / 1000.0,
            participant.visionScore,
        )
        rowBinding.participantItemRow.removeAllViews()
        participant.items.filter { it != 0 }.forEach { itemId ->
            val icon = android.widget.ImageView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(20.dpToPx(), 20.dpToPx()).apply {
                    setMargins(2.dpToPx(), 0, 0, 0)
                }
                load(DDragonUrls.itemIcon(ddragonVersion, itemId)) {
                    placeholder(R.drawable.bg_skeleton_block)
                    error(R.drawable.bg_skeleton_block)
                }
            }
            rowBinding.participantItemRow.addView(icon)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _emptyStateBinding = null
        _binding = null
    }
}
