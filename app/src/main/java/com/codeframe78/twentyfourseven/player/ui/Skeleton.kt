package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * The shape of a list that is still loading: a few rows of soft blocks that breathe, so the screen reads as
 * "content on its way" rather than an empty page with a spinner. Each row stands for one item.
 */
@Composable
internal fun SkeletonList(
    modifier: Modifier = Modifier,
    rows: Int = 6,
    showsArtwork: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    description: String = "Loading",
) {
    val breath by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeleton_alpha",
    )
    val block = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Column(
        modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .alpha(breath)
            .semantics { contentDescription = description }
            .testTag("skeleton_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(rows) { index ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showsArtwork) {
                    Box(Modifier.size(52.dp).background(block, RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(14.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Rows differ in length so the block does not look like a table.
                    val titleFraction = listOf(0.72f, 0.58f, 0.8f, 0.64f)[index % 4]
                    Box(Modifier.fillMaxWidth(titleFraction).height(16.dp).background(block, RoundedCornerShape(8.dp)))
                    Box(Modifier.fillMaxWidth(titleFraction * 0.6f).height(12.dp).background(block, RoundedCornerShape(6.dp)))
                }
            }
        }
    }
}
