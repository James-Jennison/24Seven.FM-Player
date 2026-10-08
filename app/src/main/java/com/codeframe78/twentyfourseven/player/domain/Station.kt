package com.codeframe78.twentyfourseven.player.domain

@JvmInline value class StationId(val value: String)

val SST_STATION_ID = StationId("sst")
val EIGHTIES_STATION_ID = StationId("1980s")
val ADAGIO_STATION_ID = StationId("afm")
val DEATH_STATION_ID = StationId("dfm")
val ENTRANCED_STATION_ID = StationId("efm")

val SUPPORTED_STATION_IDS = setOf(
    SST_STATION_ID,
    EIGHTIES_STATION_ID,
    ADAGIO_STATION_ID,
    DEATH_STATION_ID,
    ENTRANCED_STATION_ID,
)

fun StationId.canonicalized(): StationId = when (value.trim().lowercase()) {
    "sst" -> SST_STATION_ID
    "1980s" -> EIGHTIES_STATION_ID
    "adagio", "afm" -> ADAGIO_STATION_ID
    "death", "dfm" -> DEATH_STATION_ID
    "entranced", "efm" -> ENTRANCED_STATION_ID
    else -> this
}

fun String.toSupportedStationIdOrNull(): StationId? = StationId(this)
    .canonicalized()
    .takeIf(SUPPORTED_STATION_IDS::contains)

data class StationCapabilities(
    val supportsAuthentication: Boolean = false,
    val supportsChat: Boolean = false,
    val supportsFavorites: Boolean = false,
    val supportsRequests: Boolean = false,
    val supportsRequestMessages: Boolean = false,
    val supportsListenerActivity: Boolean = false,
    val supportsQueue: Boolean = false,
    val supportsHistory: Boolean = false,
    val supportsSecondaryContent: Boolean = false,
    val supportsNowPlayingFavorite: Boolean = false,
    val supportsAlbumRating: Boolean = false,
    val supportsPrivateMessages: Boolean = false,
    val supportsPrivateMessageSending: Boolean = false,
    val supportsMemberProfiles: Boolean = false,
    val supportsMemberFavorites: Boolean = false,
    val supportsPlayedHistoryArchive: Boolean = false,
    val supportsStationNews: Boolean = false,
    /** Moving and removing tracks in the signed-in member's own favorites list. */
    val supportsFavoriteManagement: Boolean = false,
    /** Reading an album's member reviews and, when signed in, writing one. */
    val supportsAlbumReviews: Boolean = false,
    val supportsRecentlyAdded: Boolean = false,
    /** The members list and the station's Online Now block. */
    val supportsMembersList: Boolean = false,
    /** The station's events and birthdays calendar. */
    val supportsCalendar: Boolean = false,
    /** Editing the signed-in member's own station profile. */
    val supportsProfileEditing: Boolean = false,
)

enum class StationPageKind {
    Website,
    Members,
    Statistics,
    TopTracks,
    Contact,
    Membership,
    SoundtrackOfTheMonth,
    Games,
    Awards,
}

data class StationPage(
    val kind: StationPageKind,
    val title: String,
    val description: String,
    val url: String,
)

data class StreamVariant(
    val url: String,
    val label: String,
    val priority: Int,
    val format: StreamFormat = StreamFormat.Unknown,
    val bitrateKbps: Int? = null,
)

enum class StreamFormat { Mp3, Aac, Hls, Unknown }

data class Station(
    val id: StationId,
    val name: String,
    val shortName: String,
    val description: String,
    val logoUrl: String? = null,
    val websiteUrl: String,
    val streams: List<StreamVariant> = emptyList(),
    val capabilities: StationCapabilities = StationCapabilities(),
    val secondaryPages: List<StationPage> = emptyList(),
)
