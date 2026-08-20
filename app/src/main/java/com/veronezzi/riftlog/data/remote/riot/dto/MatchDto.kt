package com.veronezzi.riftlog.data.remote.riot.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchDto(
    val info: MatchInfoDto,
)

@Serializable
data class MatchInfoDto(
    val gameDuration: Long,
    val gameCreation: Long,
    val participants: List<ParticipantDto>,
)

@Serializable
data class ParticipantDto(
    val puuid: String,
    val championName: String,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val win: Boolean,
    val item0: Int,
    val item1: Int,
    val item2: Int,
    val item3: Int,
    val item4: Int,
    val item5: Int,
    val item6: Int,
    val summoner1Id: Int,
    val summoner2Id: Int,
    val teamPosition: String,
    val totalMinionsKilled: Int,
    val goldEarned: Int,
    val perks: PerksDto? = null,
)

@Serializable
data class PerksDto(
    val styles: List<PerkStyleDto>,
)

/** One rune tree pick. `description` is "primaryStyle" or "subStyle" - the keystone is
 * `selections[0].perk` of the "primaryStyle" style, the rest are the minor rune picks in that tree. */
@Serializable
data class PerkStyleDto(
    val description: String,
    val style: Int,
    val selections: List<PerkStyleSelectionDto>,
)

@Serializable
data class PerkStyleSelectionDto(
    val perk: Int,
)
