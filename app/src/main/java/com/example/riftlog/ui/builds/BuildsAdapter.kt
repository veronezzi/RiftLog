package com.example.riftlog.ui.builds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.riftlog.R
import com.example.riftlog.databinding.ItemChampionSearchRowBinding
import com.example.riftlog.domain.model.ChampionInfo

private class ChampionInfoDiffCallback : DiffUtil.ItemCallback<ChampionInfo>() {
    override fun areItemsTheSame(oldItem: ChampionInfo, newItem: ChampionInfo) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ChampionInfo, newItem: ChampionInfo) = oldItem == newItem
}

class BuildsAdapter(private val onClick: (ChampionInfo) -> Unit) :
    ListAdapter<ChampionInfo, BuildsAdapter.ChampionViewHolder>(ChampionInfoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChampionViewHolder {
        val binding = ItemChampionSearchRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChampionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChampionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class ChampionViewHolder(private val binding: ItemChampionSearchRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChampionInfo) {
            binding.championName.text = item.name
            binding.championIcon.load(item.squareImageUrl) {
                placeholder(R.drawable.bg_skeleton_block)
                error(R.drawable.bg_skeleton_block)
            }
        }
    }
}
