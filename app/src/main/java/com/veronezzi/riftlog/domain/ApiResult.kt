package com.veronezzi.riftlog.domain

/** Outcome of a repository call, with distinct error cases the UI can branch on directly. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    sealed class Error : ApiResult<Nothing>() {
        object InvalidApiKey : Error()
        object SummonerNotFound : Error()
        data class RateLimited(val retryAfterSeconds: Int?) : Error()
        data class Network(val message: String?) : Error()
        data class Unknown(val message: String?) : Error()
    }
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onError(action: (ApiResult.Error) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) action(this)
    return this
}
