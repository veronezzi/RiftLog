package com.example.riftlog.domain.model

data class ChampionInfo(
    val id: String, // ddragon string id, e.g. "Ahri"
    val championId: Long, // numeric key, matches mastery/match data championId
    val name: String,
    val title: String,
    val squareImageUrl: String,
    val splashImageUrl: String,
    val mastery: ChampionMastery?,
)

data class ChampionMastery(
    val championLevel: Int,
    val championPoints: Long,
)

data class ChampionDetail(
    val info: ChampionInfo,
    val hp: Double,
    val mp: Double,
    val armor: Double,
    val magicResist: Double,
    val attackDamage: Double,
    val attackSpeed: Double,
    val moveSpeed: Double,
    val passive: AbilityInfo,
    val spells: List<AbilityInfo>, // Q, W, E, R in order
)

data class AbilityInfo(
    val name: String,
    val description: String,
    val imageUrl: String,
)

data class ItemInfo(
    val itemId: Int,
    val name: String,
    val iconUrl: String,
)

/** Aggregated from real, recent professional matches (Leaguepedia) - not a fabricated "meta"
 * guess. sampleGames is how many of those matches actually had a usable item list, so the UI can
 * be upfront about how small or large the sample is. */
data class ProBuild(
    val items: List<ItemInfo>,
    val sampleGames: Int,
)
