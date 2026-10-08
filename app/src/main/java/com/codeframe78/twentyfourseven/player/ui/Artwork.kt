package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size

/**
 * Cover art that changes without a gap: the picture on screen stays until the next one has actually loaded, then
 * the two crossfade. A plain AsyncImage drops back to its placeholder the moment the address changes, which made
 * every track change flash the app logo. [fallback] shows before the first picture and when a load fails.
 */
@Composable
internal fun ArtworkImage(
    url: String?,
    fallback: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalPlatformContext.current
    // The loading painter is never drawn itself, so it must be told what size to fetch.
    val request = remember(url, context) { ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build() }
    val loader = rememberAsyncImagePainter(request)
    val state by loader.state.collectAsState()
    var settled by remember { mutableStateOf<Painter?>(null) }
    LaunchedEffect(state) {
        when (val current = state) {
            is AsyncImagePainter.State.Success -> settled = current.painter
            is AsyncImagePainter.State.Error -> settled = null
            else -> Unit
        }
    }
    Crossfade(targetState = settled, animationSpec = tween(ARTWORK_CROSSFADE_MILLIS), modifier = modifier, label = "artwork") { shown ->
        Image(
            painter = shown ?: fallback,
            contentDescription = contentDescription,
            contentScale = if (shown == null) ContentScale.Fit else contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal const val ARTWORK_CROSSFADE_MILLIS = 350
