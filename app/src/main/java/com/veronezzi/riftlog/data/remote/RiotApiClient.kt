package com.veronezzi.riftlog.data.remote

import com.veronezzi.riftlog.data.remote.riot.RiotPlatformApi
import com.veronezzi.riftlog.data.remote.riot.RiotRegionalApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds Retrofit instances on demand, keyed by base URL, since the base URL depends on the
 * user-selected region/platform which is only known at runtime (read from DataStore). Instances
 * are cached so switching between the same couple of regions doesn't rebuild Retrofit each call.
 */
class RiotApiClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(RiotApiKeyInterceptor())
        .addInterceptor(RetryAfterInterceptor())
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    private val regionalApiCache = mutableMapOf<String, RiotRegionalApi>()
    private val platformApiCache = mutableMapOf<String, RiotPlatformApi>()

    private fun retrofitFor(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** [regionalRouting] is one of "americas" / "europe" / "asia". */
    fun regionalApi(regionalRouting: String): RiotRegionalApi =
        regionalApiCache.getOrPut(regionalRouting) {
            retrofitFor("https://$regionalRouting.api.riotgames.com/")
                .create(RiotRegionalApi::class.java)
        }

    /** [platformId] is one of "na1" / "euw1" / "kr" / etc. */
    fun platformApi(platformId: String): RiotPlatformApi =
        platformApiCache.getOrPut(platformId) {
            retrofitFor("https://$platformId.api.riotgames.com/")
                .create(RiotPlatformApi::class.java)
        }
}
