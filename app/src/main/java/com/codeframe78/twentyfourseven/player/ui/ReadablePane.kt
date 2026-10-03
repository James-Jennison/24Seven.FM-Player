package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Fills a full-screen view on a phone; on a tablet keeps its content in a centred column narrow enough to read,
 * rather than stretching a list across the whole display.
 */
internal fun Modifier.readablePane(): Modifier = fillMaxSize()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = 760.dp)
    .fillMaxWidth()
