package com.veronezzi.riftlog.data.remote

/**
 * Maps a Riot "platform" routing value (per-server, e.g. na1) to its "regional" routing value
 * (continent-level, e.g. americas) used by the account/match-v5 endpoints.
 */
object RegionMapper {

    private val platformToRegional = mapOf(
        "na1" to "americas",
        "br1" to "americas",
        "la1" to "americas",
        "la2" to "americas",
        "oc1" to "americas",
        "euw1" to "europe",
        "eun1" to "europe",
        "tr1" to "europe",
        "ru" to "europe",
        "kr" to "asia",
        "jp1" to "asia",
    )

    val platformIds: List<String> = platformToRegional.keys.toList()

    fun regionalRoutingFor(platformId: String): String =
        platformToRegional[platformId]
            ?: throw IllegalArgumentException("Unknown platform id: $platformId")
}
