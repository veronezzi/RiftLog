package com.example.riftlog.ui.common

import com.example.riftlog.R
import com.example.riftlog.domain.ApiResult
import com.rifttracker.designsystem.databinding.ViewEmptyStateBinding

/**
 * Binds an [ApiResult.Error] to the local empty-state layout with copy specific to each error
 * type. InvalidApiKey means the developer running this build hasn't set RIOT_API_KEY in their
 * local.properties (or Riot rotated/expired it) - it's a build setup issue, not something the
 * end user can fix from within the app, so it just offers retry like the other error cases.
 */
fun ViewEmptyStateBinding.bindError(
    error: ApiResult.Error,
    onRetry: () -> Unit,
) {
    val context = root.context
    when (error) {
        is ApiResult.Error.InvalidApiKey -> {
            emptyStateTitle.text = context.getString(R.string.error_invalid_api_key_title)
            emptyStateMessage.text = context.getString(R.string.error_invalid_api_key_message)
            emptyStateRetryButton.text = context.getString(R.string.action_retry)
            emptyStateRetryButton.setOnClickListener { onRetry() }
        }
        is ApiResult.Error.SummonerNotFound -> {
            emptyStateTitle.text = context.getString(R.string.error_summoner_not_found_title)
            emptyStateMessage.text = context.getString(R.string.error_summoner_not_found_message)
            emptyStateRetryButton.text = context.getString(R.string.action_retry)
            emptyStateRetryButton.setOnClickListener { onRetry() }
        }
        is ApiResult.Error.Network -> {
            emptyStateTitle.text = context.getString(R.string.error_network_title)
            emptyStateMessage.text = context.getString(R.string.error_network_message)
            emptyStateRetryButton.text = context.getString(R.string.action_retry)
            emptyStateRetryButton.setOnClickListener { onRetry() }
        }
        is ApiResult.Error.RateLimited -> {
            emptyStateTitle.text = context.getString(R.string.error_rate_limited_title)
            emptyStateMessage.text = context.getString(R.string.error_rate_limited_message)
            emptyStateRetryButton.text = context.getString(R.string.action_retry)
            emptyStateRetryButton.setOnClickListener { onRetry() }
        }
        is ApiResult.Error.Unknown -> {
            emptyStateTitle.text = context.getString(R.string.error_unknown_title)
            emptyStateMessage.text = error.message ?: context.getString(R.string.error_unknown_message)
            emptyStateRetryButton.text = context.getString(R.string.action_retry)
            emptyStateRetryButton.setOnClickListener { onRetry() }
        }
    }
    emptyStateRetryButton.visibility = android.view.View.VISIBLE
}
