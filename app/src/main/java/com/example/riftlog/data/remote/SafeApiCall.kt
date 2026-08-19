package com.example.riftlog.data.remote

import com.example.riftlog.domain.ApiResult
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs a Riot API suspend call and maps common failure modes to a distinct ApiResult.Error so
 * the UI can show the right empty/error state, instead of one generic "something went wrong".
 */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        when (e.code()) {
            401, 403 -> ApiResult.Error.InvalidApiKey
            404 -> ApiResult.Error.SummonerNotFound
            429 -> ApiResult.Error.RateLimited(
                e.response()?.headers()?.get("Retry-After")?.toIntOrNull()
            )
            else -> ApiResult.Error.Unknown(e.message())
        }
    } catch (e: IOException) {
        ApiResult.Error.Network(e.message)
    } catch (e: Exception) {
        ApiResult.Error.Unknown(e.message)
    }
}
