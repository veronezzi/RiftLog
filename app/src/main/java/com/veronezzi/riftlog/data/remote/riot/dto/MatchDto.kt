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
    // Absent on very old matches (Riot backfilled `teams` later); the full-match screen falls
    // back to an empty roster of objectives rather than crashing on those.
    val teams: List<TeamDto> = emptyList(),
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
    // Added for the full match-detail screen (needed to show/identify the other 9 participants -
    // the viewer-only path above never reads these). Real Riot Games' match-v5 field names.
    /** The player's current Riot ID name at fetch time. Can be null/blank on some older or
     * edge-case matches (Riot's docs don't guarantee it's always populated) - callers must fall
     * back to something else (e.g. puuid) rather than assume it's always usable. */
    val riotIdGameName: String? = null,
    val riotIdTagline: String? = null,
    val teamId: Int = 0,
    val champLevel: Int = 0,
    val totalDamageDealtToChampions: Int = 0,
    val visionScore: Int = 0,
    val wardsPlaced: Int = 0,
)

/** One of the two 5-player sides. `teamId` is Riot's fixed 100 (blue) / 200 (red) convention. */
@Serializable
data class TeamDto(
    val teamId: Int,
    val win: Boolean,
    val objectives: ObjectivesDto,
)

/** Riot's real shape is one of these per objective type, all with just a `kills` count (plus a
 * `first` flag this app doesn't use). Field names below match match-v5 exactly. */
@Serializable
data class ObjectivesDto(
    val baron: ObjectiveDto,
    val dragon: ObjectiveDto,
    val tower: ObjectiveDto,
    val inhibitor: ObjectiveDto,
    val riftHerald: ObjectiveDto,
    val champion: ObjectiveDto,
)

@Serializable
data class ObjectiveDto(
    val kills: Int,
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
