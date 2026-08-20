package com.veronezzi.riftlog.data.remote.ddragon

import com.veronezzi.riftlog.data.remote.ddragon.dto.ChampionDetailListDto
import com.veronezzi.riftlog.data.remote.ddragon.dto.ChampionListDto
import com.veronezzi.riftlog.data.remote.ddragon.dto.ItemListDto
import com.veronezzi.riftlog.data.remote.ddragon.dto.RuneTreeDto
import retrofit2.http.GET
import retrofit2.http.Path

/** Static game data, no API key required. Base URL: https://ddragon.leagueoflegends.com/ */
interface DDragonApi {

    @GET("api/versions.json")
    suspend fun getVersions(): List<String>

    @GET("cdn/{version}/data/en_US/champion.json")
    suspend fun getChampionList(@Path("version") version: String): ChampionListDto

    @GET("cdn/{version}/data/en_US/champion/{championId}.json")
    suspend fun getChampionDetail(
        @Path("version") version: String,
        @Path("championId") championId: String,
    ): ChampionDetailListDto

    @GET("cdn/{version}/data/en_US/item.json")
    suspend fun getItemList(@Path("version") version: String): ItemListDto

    @GET("cdn/{version}/data/en_US/runesReforged.json")
    suspend fun getRunesReforged(@Path("version") version: String): List<RuneTreeDto>
}
