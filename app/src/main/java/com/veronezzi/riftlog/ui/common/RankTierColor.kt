package com.veronezzi.riftlog.ui.common

import android.content.Context
import androidx.core.content.ContextCompat
import com.veronezzi.riftlog.R as AppR
import com.rifttracker.designsystem.R as DesignR

/** rift-tracker-design-system v1.1.0 only ships colors for silver/gold/platinum/emerald/diamond.
 * Iron/Bronze/Master/Grandmaster/Challenger are filled in locally in app colors.xml, matched to
 * the same muted/pastel style as the design system's own rank colors (see colors.xml for notes).
 * If the design system adds these upstream, switch these four entries over to it. */
object RankTierColor {
    fun colorFor(context: Context, tier: String): Int {
        val resId = when (tier.uppercase()) {
            "IRON" -> AppR.color.rank_iron
            "BRONZE" -> AppR.color.rank_bronze
            "SILVER" -> DesignR.color.rank_silver
            "GOLD" -> DesignR.color.rank_gold
            "PLATINUM" -> DesignR.color.rank_platinum
            "EMERALD" -> DesignR.color.rank_emerald
            "DIAMOND" -> DesignR.color.rank_diamond
            "MASTER" -> AppR.color.rank_master
            "GRANDMASTER" -> AppR.color.rank_grandmaster
            "CHALLENGER" -> AppR.color.rank_challenger
            else -> DesignR.color.rift_accent
        }
        return ContextCompat.getColor(context, resId)
    }
}
