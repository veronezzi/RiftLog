package com.example.riftlog.data.remote.leaguepedia

import com.example.riftlog.data.remote.leaguepedia.dto.CargoQueryResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Leaguepedia's public Cargo query API (lol.fandom.com) - the community esports wiki that backs
 * most third-party pro-play stat sites. No API key, no auth: it's the same public endpoint any
 * fan tool queries. Base URL: https://lol.fandom.com/
 *
 * This is what actually has real professional-player item builds - the standard Riot API only
 * covers normal/ranked games on public accounts, not LCK/LEC/LCS/LPL tournament matches, which
 * are played on separate accounts the public API can't see.
 */
interface LeaguepediaApi {

    @GET("api.php")
    suspend fun queryItemBuilds(
        @Query("where") where: String,
        @Query("action") action: String = "cargoquery",
        @Query("tables") tables: String = "ScoreboardPlayers",
        @Query("fields") fields: String = "ScoreboardPlayers.Champion,ScoreboardPlayers.Items",
        @Query("order_by") orderBy: String = "ScoreboardPlayers.DateTime_UTC DESC",
        @Query("limit") limit: Int = 20,
        @Query("format") format: String = "json",
    ): CargoQueryResponseDto
}
