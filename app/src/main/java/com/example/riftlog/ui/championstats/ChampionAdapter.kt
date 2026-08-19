package com.example.riftlog.ui.championstats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.riftlog.R
import com.example.riftlog.databinding.ItemChampionMasteryCardBinding
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
        }
    }
}
