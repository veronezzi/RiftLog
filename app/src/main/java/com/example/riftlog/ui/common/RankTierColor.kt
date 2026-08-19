package com.example.riftlog.ui.common

import android.content.Context
import androidx.core.content.ContextCompat
import com.rifttracker.designsystem.R

/** rift-tracker-design-system v1.0.1 only ships colors for silver/gold/platinum/emerald/diamond.
 * Other tiers fall back to the accent color rather than inventing new palette entries. */
object RankTierColor {
    fun colorFor(context: Context, tier: String): Int {
        val resId = when (tier.uppercase()) {
            "SILVER" -> R.color.rank_silver
            "GOLD" -> R.color.rank_gold
            "PLATINUM" -> R.color.rank_platinum
            "EMERALD" -> R.color.rank_emerald
            "DIAMOND" -> R.color.rank_diamond
            else -> R.color.rift_accent
        }
        return ContextCompat.getColor(context, resId)
    }
}
