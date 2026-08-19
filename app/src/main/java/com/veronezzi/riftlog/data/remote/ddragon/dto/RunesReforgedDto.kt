package com.veronezzi.riftlog.data.remote.ddragon.dto

import kotlinx.serialization.Serializable

/** `runesReforged.json` is a top-level JSON array, not an object - one entry per rune tree
 * (Precision, Domination, Sorcery, Resolve, Inspiration). */
@Serializable
data class RuneTreeDto(
    val id: Int,
    val key: String,
    val icon: String,
    val name: String,
    val slots: List<RuneSlotDto>,
)

@Serializable
data class RuneSlotDto(
    val runes: List<RuneDto>,
)

@Serializable
data class RuneDto(
    val id: Int,
    val key: String,
    val icon: String,
    val name: String,
)
