package com.veronezzi.riftlog.ui.matchhistory

import android.content.res.ColorStateList
import android.text.format.DateUtils
import androidx.core.graphics.ColorUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veronezzi.riftlog.R
import com.veronezzi.riftlog.data.remote.ddragon.DDragonUrls
import com.veronezzi.riftlog.databinding.ItemMatchRowBinding
import com.veronezzi.riftlog.domain.model.MatchSummary
import com.rifttracker.designsystem.R as DesignSystemR
import com.rifttracker.designsystem.databinding.ViewMatchHistoryItemBinding

private class MatchDiffCallback : DiffUtil.ItemCallback<MatchSummary>() {
    override fun areItemsTheSame(oldItem: MatchSummary, newItem: MatchSummary) = oldItem.matchId == newItem.matchId
    override fun areContentsTheSame(oldItem: MatchSummary, newItem: MatchSummary) = oldItem == newItem
}

class MatchAdapter(private var ddragonVersion: String) : ListAdapter<MatchSummary, MatchAdapter.MatchViewHolder>(MatchDiffCallback()) {

    private val expandedMatchIds = mutableSetOf<String>()

    fun updateVersion(version: String) {
        ddragonVersion = version
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = getItem(position)
        holder.bind(match, ddragonVersion, expandedMatchIds.contains(match.matchId)) {
            if (!expandedMatchIds.add(match.matchId)) expandedMatchIds.remove(match.matchId)
            notifyItemChanged(position)
        }
    }

    class MatchViewHolder(private val binding: ItemMatchRowBinding) : RecyclerView.ViewHolder(binding.root) {

        private val card = ViewMatchHistoryItemBinding.bind(binding.matchHistoryItem)

        private val itemSlots = listOf(
            card.matchItem1,
            card.matchItem2,
            card.matchItem3,
            card.matchItem4,
            card.matchItem5,
            card.matchItem6,
        )

        fun bind(match: MatchSummary, ddragonVersion: String, expanded: Boolean, onToggle: () -> Unit) {
            val context = binding.root.context

            card.matchChampionIcon.imageTintList = null
            card.matchChampionIcon.load(DDragonUrls.championSquare(ddragonVersion, "${match.championName}.png")) {
                placeholder(R.drawable.bg_skeleton_block)
                error(R.drawable.bg_skeleton_block)
            }
            card.matchChampionName.text = match.championName
            val kda = if (match.deaths == 0) {
                (match.kills + match.assists).toDouble()
            } else {
                (match.kills + match.assists).toDouble() / match.deaths
            }
            card.matchKda.text = "${match.kills} / ${match.deaths} / ${match.assists} · %.2f KDA".format(kda)
            card.matchTimeAgo.text = DateUtils.getRelativeTimeSpanString(
                match.gameCreationMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )

            val resultColor = ContextCompat.getColor(
                context, if (match.win) DesignSystemR.color.rift_win else DesignSystemR.color.rift_loss
            )
            val surfaceColor = ContextCompat.getColor(context, DesignSystemR.color.rift_surface)
            card.matchResultStrip.setBackgroundColor(resultColor)
            card.matchHistoryCard.setStrokeColor(ColorStateList.valueOf(resultColor))
            // Wash the whole card in a faint win/loss tint (blended into the flat surface color,
            // no alpha/glassmorphism) - readers scan a match list by result first, and a full-card
            // hint reads faster than the 4dp edge strip alone (this is what OP.GG's match list
            // leans on too, per user feedback when they briefly dropped it).
            card.matchHistoryCard.setCardBackgroundColor(ColorUtils.blendARGB(surfaceColor, resultColor, 0.10f))

            itemSlots.forEachIndexed { index, slot ->
                val itemId = match.items.getOrElse(index) { 0 }
                if (itemId != 0) {
                    slot.load(DDragonUrls.itemIcon(ddragonVersion, itemId)) {
                        placeholder(R.drawable.bg_skeleton_block)
                        error(R.drawable.bg_skeleton_block)
                    }
                } else {
                    slot.setImageDrawable(null)
                }
            }

            binding.detailRow.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.detailCs.text = "${match.totalMinionsKilled} CS"
            binding.detailGold.text = "%.1fk gold".format(match.goldEarned / 1000.0)
            val minutes = match.gameDurationSeconds / 60
            val seconds = match.gameDurationSeconds % 60
            binding.detailDuration.text = "${minutes}m ${seconds}s"

            binding.root.setOnClickListener { onToggle() }
        }
    }
}
