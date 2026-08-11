package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codeframe78.twentyfourseven.player.domain.TrackRequestAvailability
import com.codeframe78.twentyfourseven.player.domain.TrackRequestStatus
import com.codeframe78.twentyfourseven.player.ui.theme.requestAvailableGreen
import com.codeframe78.twentyfourseven.player.ui.theme.requestUnavailableRed

@Composable
internal fun RequestStatusIndicator(
    availability: TrackRequestAvailability,
    modifier: Modifier = Modifier,
) {
    val presentation = availability.presentation()
    val color = when (presentation.colorRole) {
        RequestStatusColorRole.Available -> requestAvailableGreen()
        RequestStatusColorRole.Recent -> requestUnavailableRed()
        RequestStatusColorRole.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        RequestStatusColorRole.Error -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = modifier
            .testTag(
                when (presentation.colorRole) {
                    RequestStatusColorRole.Available -> "request_status_green"
                    RequestStatusColorRole.Recent -> "request_status_red"
                    RequestStatusColorRole.Neutral, RequestStatusColorRole.Error -> "request_status_neutral"
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = presentation.description
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (presentation.iconRole) {
                RequestStatusIconRole.Stoplight -> Icons.Default.Circle
                RequestStatusIconRole.Lock -> Icons.Default.Lock
                RequestStatusIconRole.Block -> Icons.Default.Block
            },
            contentDescription = null,
            tint = color,
        )
        Text(
            presentation.label,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun TrackRequestAvailability.visibleLabel(): String = presentation().label

private fun TrackRequestAvailability.presentation(): RequestStatusPresentation = when (status) {
    TrackRequestStatus.Available -> RequestStatusPresentation(
        label = "Request Now",
        description = "Request Now — track is currently available to request",
        colorRole = RequestStatusColorRole.Available,
        iconRole = RequestStatusIconRole.Stoplight,
    )
    TrackRequestStatus.InCurrentQueue -> RequestStatusPresentation(
        label = "Track Recently Played",
        description = "Track Recently Played — track is currently in the station queue and cannot be requested again",
        colorRole = RequestStatusColorRole.Recent,
        iconRole = RequestStatusIconRole.Stoplight,
    )
    TrackRequestStatus.RecentlyPlayed -> RequestStatusPresentation(
        label = "Track Recently Played",
        description = "Track Recently Played — track is not currently available to request",
        colorRole = RequestStatusColorRole.Recent,
        iconRole = RequestStatusIconRole.Stoplight,
    )
    TrackRequestStatus.AuthenticationRequired -> RequestStatusPresentation(
        label = "Sign In to Request",
        description = "Sign In to Request — this station account is signed out",
        colorRole = RequestStatusColorRole.Neutral,
        iconRole = RequestStatusIconRole.Lock,
    )
    TrackRequestStatus.UserCooldown -> blocked("Request Cooldown Active")
    TrackRequestStatus.MembershipRequired -> blocked("VIP Membership Required")
    TrackRequestStatus.RequestLimitReached -> blocked("Request Limit Reached")
    TrackRequestStatus.StationUnavailable -> blocked("Station Unavailable", error = true)
    TrackRequestStatus.RequestsUnavailable, TrackRequestStatus.Unknown ->
        blocked("Requests Temporarily Unavailable")
}

private fun blocked(label: String, error: Boolean = false) = RequestStatusPresentation(
    label = label,
    description = "$label — track cannot currently be requested",
    colorRole = if (error) RequestStatusColorRole.Error else RequestStatusColorRole.Neutral,
    iconRole = RequestStatusIconRole.Block,
)

private data class RequestStatusPresentation(
    val label: String,
    val description: String,
    val colorRole: RequestStatusColorRole,
    val iconRole: RequestStatusIconRole,
)

private enum class RequestStatusColorRole { Available, Recent, Neutral, Error }
private enum class RequestStatusIconRole { Stoplight, Lock, Block }
