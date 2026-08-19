package com.veronezzi.riftlog.data.remote.riot

import com.veronezzi.riftlog.data.remote.riot.dto.ChampionMasteryDto
import com.veronezzi.riftlog.data.remote.riot.dto.LeagueEntryDto
import com.veronezzi.riftlog.data.remote.riot.dto.SummonerDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Endpoints served from per-server platform routing: na1 / euw1 / kr / etc. */
interface RiotPlatformApi {

    @GET("lol/summoner/v4/summoners/by-puuid/{puuid}")
    suspend fun getSummonerByPuuid(@Path("puuid") puuid: String): SummonerDto

    @GET("lol/league/v4/entries/by-puuid/{puuid}")
    suspend fun getLeagueEntriesByPuuid(@Path("puuid") puuid: String): List<LeagueEntryDto>

    @GET("lol/champion-mastery/v4/champion-masteries/by-puuid/{puuid}")
    suspend fun getChampionMasteries(@Path("puuid") puuid: String): List<ChampionMasteryDto>
}
