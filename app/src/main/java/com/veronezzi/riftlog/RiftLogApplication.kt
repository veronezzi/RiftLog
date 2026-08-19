package com.veronezzi.riftlog

import android.app.Application
import com.veronezzi.riftlog.data.local.MasteryDao
import com.veronezzi.riftlog.data.local.MatchDao
import com.veronezzi.riftlog.data.local.ProfileDao
import com.veronezzi.riftlog.data.local.RiftLogDbHelper
import com.veronezzi.riftlog.data.local.StaticDataDao
import com.veronezzi.riftlog.data.remote.RiotApiClient
import com.veronezzi.riftlog.data.remote.ddragon.DDragonApi
import com.veronezzi.riftlog.data.remote.leaguepedia.LeaguepediaApi
import com.veronezzi.riftlog.data.repository.ChampionRepository
import com.veronezzi.riftlog.data.repository.MatchRepository
import com.veronezzi.riftlog.data.repository.ProBuildRepository
import com.veronezzi.riftlog.data.repository.ProfileRepository
import com.veronezzi.riftlog.data.settings.SettingsRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual DI container: exposes lazily-built singletons. No Hilt/Dagger - this app is small
 * enough that a few `by lazy` properties are simpler and avoid KSP/Hilt version-compat risk
 * with a brand-new AGP/Kotlin toolchain.
 */
class RiftLogApplication : Application() {

    private val json = Json { ignoreUnknownKeys = true }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    val dbHelper: RiftLogDbHelper by lazy { RiftLogDbHelper(this) }
    private val profileDao: ProfileDao by lazy { ProfileDao(dbHelper) }
    private val matchDao: MatchDao by lazy { MatchDao(dbHelper) }
    private val masteryDao: MasteryDao by lazy { MasteryDao(dbHelper) }
    private val staticDataDao: StaticDataDao by lazy { StaticDataDao(dbHelper) }

    private val riotApiClient: RiotApiClient by lazy { RiotApiClient() }

    private val dDragonApi: DDragonApi by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        Retrofit.Builder()
            .baseUrl("https://ddragon.leagueoflegends.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DDragonApi::class.java)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(riotApiClient, profileDao)
    }

    val matchRepository: MatchRepository by lazy {
        MatchRepository(riotApiClient, matchDao)
    }

    val championRepository: ChampionRepository by lazy {
        ChampionRepository(dDragonApi, riotApiClient, staticDataDao, masteryDao)
    }

    private val leaguepediaApi: LeaguepediaApi by lazy {
        // Public Cargo query API, no key - MediaWiki's usage guidelines ask for an identifying
        // User-Agent on every request, so add one instead of relying on OkHttp's default.
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", "RiftLog/1.0").build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
        Retrofit.Builder()
            .baseUrl("https://lol.fandom.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LeaguepediaApi::class.java)
    }

    val proBuildRepository: ProBuildRepository by lazy {
        ProBuildRepository(leaguepediaApi, championRepository, staticDataDao)
    }
}
