package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.FavoriteTrack

internal enum class FavoriteTrackSortOrder(val label: String) {
    Position("#"),
    TrackName("Track Name"),
    Album("Album"),
    Artist("Artist"),
    Genre("Genre"),
    Year("Year"),
    Length("Length"),
    PlayState("Play state"),
}

internal fun List<FavoriteTrack>.sortedForFavorites(order: FavoriteTrackSortOrder): List<FavoriteTrack> = when (order) {
    FavoriteTrackSortOrder.Position -> if (isSortedByPosition()) this else sortedBy(FavoriteTrack::position)
    FavoriteTrackSortOrder.TrackName -> sortedWith(textComparator(FavoriteTrack::title))
    FavoriteTrackSortOrder.Album -> sortedWith(textComparator(FavoriteTrack::album))
    FavoriteTrackSortOrder.Artist -> sortedWith(textComparator(FavoriteTrack::artist))
    FavoriteTrackSortOrder.Genre -> sortedWith(textComparator(FavoriteTrack::genre))
    FavoriteTrackSortOrder.Year -> sortedWith(
        compareBy<FavoriteTrack> { it.year?.trim()?.toIntOrNull() ?: Int.MAX_VALUE }
            .thenBy(FavoriteTrack::position),
    )
    FavoriteTrackSortOrder.Length -> sortedWith(
        compareBy<FavoriteTrack> { durationSeconds(it.duration) ?: Int.MAX_VALUE }
            .thenBy(FavoriteTrack::position),
    )
    FavoriteTrackSortOrder.PlayState -> sortedForDisplay(TrackSortOrder.PlayState, FavoriteTrack::availability)
}

private fun List<FavoriteTrack>.isSortedByPosition(): Boolean {
    for (index in 1 until size) {
        if (this[index - 1].position > this[index].position) return false
    }
    return true
}

private fun textComparator(selector: (FavoriteTrack) -> String?): Comparator<FavoriteTrack> =
    compareBy<FavoriteTrack> { selector(it).isNullOrBlank() }
        .thenComparator { left, right ->
            String.CASE_INSENSITIVE_ORDER.compare(selector(left).orEmpty(), selector(right).orEmpty())
        }
        .thenBy(FavoriteTrack::position)

private fun durationSeconds(value: String?): Int? {
    val parts = value?.trim()?.split(':')?.map { it.toIntOrNull() ?: return null } ?: return null
    if (parts.size !in 2..3 || parts.first() < 0 || parts.drop(1).any { it !in 0..59 }) return null
    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> null
    }
}
