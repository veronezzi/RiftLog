package com.veronezzi.riftlog.domain.model

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
    val championPointsSinceLastLevel: Long,
    val championPointsUntilNextLevel: Long, // 0 once Riot reports this champion as max level
    val tokensEarned: Int,
) {
    val isMaxLevel: Boolean get() = championPointsUntilNextLevel <= 0

    /** 0f..1f progress within the current level, for a progress-bar fill weight. */
    val progressFraction: Float
        get() {
            if (isMaxLevel) return 1f
            val total = championPointsSinceLastLevel + championPointsUntilNextLevel
            if (total <= 0) return 1f
            return (championPointsSinceLastLevel.toFloat() / total).coerceIn(0f, 1f)
        }
}

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

/** A single rune or rune-tree icon + display name, resolved from runesReforged.json. */
data class RuneIconInfo(
    val id: Int,
    val name: String,
    val iconUrl: String,
)

/** Flattened lookup over every rune tree/keystone/minor rune, keyed by id, for turning the
 * (styleId, keystoneId) ints stored per match into displayable icons. */
data class RuneCatalog(
    val stylesById: Map<Int, RuneIconInfo>,
    val runesById: Map<Int, RuneIconInfo>,
)

/** The recommended rune page for a champion, resolved against [RuneCatalog]. Null when a rune id
 * from the aggregate has no match in the catalog (e.g. stale cached version). */
data class RunePageInfo(
    val primaryStyle: RuneIconInfo,
    val subStyle: RuneIconInfo,
    val keystone: RuneIconInfo,
)
