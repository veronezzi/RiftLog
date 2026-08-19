package com.veronezzi.riftlog.data.remote.ddragon.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChampionDetailListDto(
    val data: Map<String, ChampionDetailDto>,
)

@Serializable
data class ChampionDetailDto(
    val id: String,
    val name: String,
    val title: String,
    val image: ImageDto,
    val stats: ChampionStatsDto,
    val passive: PassiveDto,
    val spells: List<SpellDto>,
)

@Serializable
data class ChampionStatsDto(
    val hp: Double,
    val mp: Double,
    val armor: Double,
    val spellblock: Double,
    val attackdamage: Double,
    val attackspeed: Double,
    val movespeed: Double,
)

@Serializable
data class PassiveDto(
    val name: String,
    val description: String,
    val image: ImageDto,
)

@Serializable
data class SpellDto(
    val id: String,
    val name: String,
    val description: String,
    val image: ImageDto,
)
