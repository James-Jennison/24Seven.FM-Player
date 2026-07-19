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
)

enum class StationPageKind {
    Website,
    Forums,
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
    val websiteUrl: String,
    val streams: List<StreamVariant> = emptyList(),
    val capabilities: StationCapabilities = StationCapabilities(),
    val secondaryPages: List<StationPage> = emptyList(),
)
