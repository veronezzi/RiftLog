package com.veronezzi.riftlog.ui.championstats

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veronezzi.riftlog.R
import com.veronezzi.riftlog.databinding.ItemChampionMasteryCardBinding
import com.veronezzi.riftlog.domain.model.ChampionMastery
import com.rifttracker.designsystem.databinding.ViewChampionMasteryCardBinding

private class ChampionDiffCallback : DiffUtil.ItemCallback<ChampionStatsItem>() {
    override fun areItemsTheSame(oldItem: ChampionStatsItem, newItem: ChampionStatsItem) =
        oldItem.info.id == newItem.info.id
    override fun areContentsTheSame(oldItem: ChampionStatsItem, newItem: ChampionStatsItem) = oldItem == newItem
}

class ChampionAdapter(private val onClick: (ChampionStatsItem) -> Unit) :
    ListAdapter<ChampionStatsItem, ChampionAdapter.ChampionViewHolder>(ChampionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChampionViewHolder {
        val binding = ItemChampionMasteryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChampionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChampionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class ChampionViewHolder(private val binding: ItemChampionMasteryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChampionStatsItem) {
            val card = ViewChampionMasteryCardBinding.bind(binding.masteryCard)
            card.masteryChampionIcon.load(item.info.squareImageUrl) {
                placeholder(R.drawable.bg_skeleton_block)
                error(R.drawable.bg_skeleton_block)
            }
            card.masteryChampionName.text = item.info.name
            val mastery = item.info.mastery
            val context = binding.root.context
            card.masteryLevelBadge.text = "M${mastery?.championLevel ?: 0}"
            card.masteryPoints.text = context.getString(
                R.string.champion_mastery_points_format, mastery?.championPoints ?: 0L
            )

            val aggregate = item.aggregate
            binding.aggregateSummary.text = if (aggregate == null) {
                context.getString(R.string.champion_mastery_no_recent_games)
            } else {
                val winRatePercent = (aggregate.winRate * 100).toInt()
                val kda = "%.2f".format(aggregate.avgKda)
                context.getString(
                    R.string.champion_mastery_summary_format, aggregate.gamesPlayed, winRatePercent, kda
                )
            }

            bindProgress(mastery, context)
        }

        private fun bindProgress(mastery: ChampionMastery?, context: Context) {
            val fraction = mastery?.progressFraction
            if (mastery == null || fraction == null) {
                binding.masteryProgressBar.visibility = View.GONE
                binding.masteryProgressLabel.visibility = View.GONE
                return
            }
            binding.masteryProgressBar.visibility = View.VISIBLE
            binding.masteryProgressLabel.visibility = View.VISIBLE

            binding.masteryProgressFill.layoutParams =
                (binding.masteryProgressFill.layoutParams as LinearLayout.LayoutParams).apply { weight = fraction }
            binding.masteryProgressRemainder.layoutParams =
                (binding.masteryProgressRemainder.layoutParams as LinearLayout.LayoutParams).apply { weight = 1f - fraction }

            binding.masteryProgressLabel.text = if (mastery.isMaxLevel) {
                context.getString(R.string.champion_mastery_max_level)
            } else {
                // fraction != null guarantees championPointsUntilNextLevel is known here too -
                // see ChampionMastery.progressFraction, which returns null unless both are set.
                context.getString(
                    R.string.champion_mastery_progress_format,
                    mastery.championPointsUntilNextLevel ?: 0,
                    mastery.championLevel + 1,
                )
            }
        }
    }
}
