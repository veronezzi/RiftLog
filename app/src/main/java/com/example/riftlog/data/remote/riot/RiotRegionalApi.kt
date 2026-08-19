package com.example.riftlog.data.remote.riot

import com.example.riftlog.data.remote.riot.dto.AccountDto
import com.example.riftlog.data.remote.riot.dto.MatchDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints served from continent-level routing: americas / europe / asia. */
interface RiotRegionalApi {

    @GET("riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
    suspend fun getAccountByRiotId(
        @Path("gameName") gameName: String,
        @Path("tagLine") tagLine: String,
    ): AccountDto

    @GET("lol/match/v5/matches/by-puuid/{puuid}/ids")
    suspend fun getMatchIds(
        @Path("puuid") puuid: String,
        @Query("start") start: Int = 0,
        @Query("count") count: Int = 20,
    ): List<String>

    @GET("lol/match/v5/matches/{matchId}")
    suspend fun getMatchDetail(
        @Path("matchId") matchId: String,
    ): MatchDto
}
