package com.codeframe78.twentyfourseven.player.domain

@JvmInline value class StationId(val value: String)

data class StationCapabilities(
    val supportsChat: Boolean = false,
    val supportsRequests: Boolean = false,
    val supportsQueue: Boolean = false,
    val supportsHistory: Boolean = false,
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
)
