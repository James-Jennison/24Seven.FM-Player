package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Pull down to refresh. The indicator stays for as long as [isLoading] reports the refresh is running, and at least
 * long enough to be seen when the screen refreshes without ever reporting that it is loading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RefreshableBox(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    var pulled by remember { mutableStateOf(false) }
    LaunchedEffect(pulled, isLoading) {
        if (pulled && !isLoading) {
            delay(MINIMUM_REFRESH_INDICATOR_MILLIS)
            pulled = false
        }
    }
    PullToRefreshBox(
        isRefreshing = pulled,
        onRefresh = {
            pulled = true
            onRefresh()
        },
        modifier = modifier,
        content = content,
    )
}

private const val MINIMUM_REFRESH_INDICATOR_MILLIS = 700L
