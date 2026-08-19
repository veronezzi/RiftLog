package com.example.riftlog.data.remote

import com.example.riftlog.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the developer's own Riot API key (from local.properties -> BuildConfig, never
 * committed) as the X-Riot-Token header. Never hardcode a key here - Riot's ToS forbids shipping
 * a personal key inside a distributed app.
 */
class RiotApiKeyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-Riot-Token", BuildConfig.RIOT_API_KEY)
            .build()
        return chain.proceed(request)
    }
}
