package com.veronezzi.riftlog.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Simple 429 handling: on rate limit, sleep for the server-provided Retry-After
 * (seconds) and retry exactly once. No exponential backoff / retry library -
 * Riot always sends Retry-After on 429 so a single honoring of it is enough.
 */
class RetryAfterInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)

        if (response.code == 429) {
            val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: 1L
            response.close()
            Thread.sleep(retryAfterSeconds.coerceAtLeast(1L) * 1000)
            response = chain.proceed(request)
        }

        return response
    }
}
