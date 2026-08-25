package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.runtime.Immutable
import com.codeframe78.twentyfourseven.player.domain.FeedbackSubmission

@Immutable
internal data class FeedbackActions(
    val onReviewEmail: (FeedbackSubmission, String?) -> Unit = { _, _ -> },
)

@Immutable
internal data class FeedbackUi(
    val actions: FeedbackActions = FeedbackActions(),
)
