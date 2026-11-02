package com.codeframe78.twentyfourseven.player.companion

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import org.json.JSONException
import org.json.JSONObject

const val COMPANION_PROTOCOL_VERSION = 1
const val COMPANION_MAX_FRAME_BYTES = 16 * 1024

enum class CompanionResultCode(val wire: String) {
    Accepted("accepted"),
    Refused("refused"),
    Uncertain("uncertain"),
}

enum class PairRefusal(val wire: String) {
    Expired("expired"),
    BadProof("bad_proof"),
    AlreadyPaired("already_paired"),
}

sealed interface CompanionFrame {
    data class Pair(val proof: String, val phone: String, val account: String, val station: String) : CompanionFrame
    data class PairOk(val session: String) : CompanionFrame
    data class PairRefused(val reason: PairRefusal) : CompanionFrame
    data class Propose(
        val op: String,
        val station: String,
        val trackRef: String,
        val title: String,
        val artist: String,
        val album: String,
    ) : CompanionFrame
    data class Awaiting(val op: String) : CompanionFrame
    data class Declined(val op: String) : CompanionFrame
    data class Submitting(val op: String) : CompanionFrame
    data class Result(val op: String, val code: CompanionResultCode, val reason: String? = null) : CompanionFrame
    data class StateChanged(val station: String, val account: String) : CompanionFrame
    data object Revoke : CompanionFrame
    data object Ping : CompanionFrame
    data object Pong : CompanionFrame
}

sealed interface DecodedFrame {
    data class Frame(val frame: CompanionFrame) : DecodedFrame
    /** The peer speaks a different protocol version; the connection must close. */
    data class UnsupportedVersion(val version: Int) : DecodedFrame
    /** A well-formed frame of a type this version does not know; ignored. */
    data object Unknown : DecodedFrame
    /** Not valid JSON, missing or oversized fields; the connection must close. */
    data object Malformed : DecodedFrame
}

object CompanionFrameCodec {
    private const val MAX_LABEL = 64
    private const val MAX_TEXT = 200
    private const val MAX_TRACK_REF = 512
    private const val MAX_REASON = 300
    private val OP = Regex("[0-9a-f]{32}")
    private val STATION = Regex("[a-z0-9]{1,16}")

    fun encode(frame: CompanionFrame): ByteArray {
        val payload = toJson(frame).toString().toByteArray(Charsets.UTF_8)
        require(payload.size <= COMPANION_MAX_FRAME_BYTES) { "Frame too large" }
        val out = ByteArrayOutputStream(payload.size + 4)
        out.write(payload.size ushr 24)
        out.write(payload.size ushr 16)
        out.write(payload.size ushr 8)
        out.write(payload.size)
        out.write(payload)
        return out.toByteArray()
    }

    /** Reads one frame payload; throws [MalformedFrameException] for a bad or oversized length. */
    fun readPayload(input: InputStream): ByteArray {
        val data = DataInputStream(input)
        val length = try {
            data.readInt()
        } catch (_: EOFException) {
            throw MalformedFrameException("Connection closed")
        }
        if (length < 2 || length > COMPANION_MAX_FRAME_BYTES) throw MalformedFrameException("Bad frame length")
        val payload = ByteArray(length)
        try {
            data.readFully(payload)
        } catch (_: EOFException) {
            throw MalformedFrameException("Truncated frame")
        }
        return payload
    }

    fun decode(payload: ByteArray): DecodedFrame {
        if (payload.size > COMPANION_MAX_FRAME_BYTES) return DecodedFrame.Malformed
        val json = try {
            JSONObject(String(payload, Charsets.UTF_8))
        } catch (_: JSONException) {
            return DecodedFrame.Malformed
        }
        val version = json.optInt("v", -1)
        if (version != COMPANION_PROTOCOL_VERSION) {
            return if (version == -1) DecodedFrame.Malformed else DecodedFrame.UnsupportedVersion(version)
        }
        return try {
            fromJson(json)?.let { DecodedFrame.Frame(it) } ?: DecodedFrame.Unknown
        } catch (_: IllegalArgumentException) {
            DecodedFrame.Malformed
        }
    }

    private fun toJson(frame: CompanionFrame): JSONObject {
        val json = JSONObject().put("v", COMPANION_PROTOCOL_VERSION)
        return when (frame) {
            is CompanionFrame.Pair -> json.put("type", "pair").put("proof", frame.proof)
                .put("phone", frame.phone).put("account", frame.account).put("station", frame.station)
            is CompanionFrame.PairOk -> json.put("type", "pair_ok").put("session", frame.session)
            is CompanionFrame.PairRefused -> json.put("type", "pair_refused").put("reason", frame.reason.wire)
            is CompanionFrame.Propose -> json.put("type", "propose").put("op", frame.op).put("station", frame.station)
                .put("trackRef", frame.trackRef).put("title", frame.title).put("artist", frame.artist)
                .put("album", frame.album)
            is CompanionFrame.Awaiting -> json.put("type", "awaiting").put("op", frame.op)
            is CompanionFrame.Declined -> json.put("type", "declined").put("op", frame.op)
            is CompanionFrame.Submitting -> json.put("type", "submitting").put("op", frame.op)
            is CompanionFrame.Result -> json.put("type", "result").put("op", frame.op).put("code", frame.code.wire)
                .also { if (frame.reason != null) it.put("reason", frame.reason) }
            is CompanionFrame.StateChanged -> json.put("type", "state_changed").put("station", frame.station)
                .put("account", frame.account)
            CompanionFrame.Revoke -> json.put("type", "revoke")
            CompanionFrame.Ping -> json.put("type", "ping")
            CompanionFrame.Pong -> json.put("type", "pong")
        }
    }

    private fun fromJson(json: JSONObject): CompanionFrame? = when (json.optString("type")) {
        "pair" -> CompanionFrame.Pair(
            proof = hex(json, "proof", 64),
            phone = text(json, "phone", MAX_LABEL),
            account = text(json, "account", MAX_LABEL),
            station = station(json),
        )
        "pair_ok" -> CompanionFrame.PairOk(session = text(json, "session", MAX_LABEL, allowEmpty = false))
        "pair_refused" -> CompanionFrame.PairRefused(
            PairRefusal.entries.firstOrNull { it.wire == json.optString("reason") }
                ?: throw IllegalArgumentException("reason"),
        )
        "propose" -> CompanionFrame.Propose(
            op = op(json),
            station = station(json),
            trackRef = text(json, "trackRef", MAX_TRACK_REF, allowEmpty = false),
            title = text(json, "title", MAX_TEXT),
            artist = text(json, "artist", MAX_TEXT),
            album = text(json, "album", MAX_TEXT),
        )
        "awaiting" -> CompanionFrame.Awaiting(op(json))
        "declined" -> CompanionFrame.Declined(op(json))
        "submitting" -> CompanionFrame.Submitting(op(json))
        "result" -> CompanionFrame.Result(
            op = op(json),
            code = CompanionResultCode.entries.firstOrNull { it.wire == json.optString("code") }
                ?: throw IllegalArgumentException("code"),
            reason = if (json.has("reason")) text(json, "reason", MAX_REASON) else null,
        )
        "state_changed" -> CompanionFrame.StateChanged(station = station(json), account = text(json, "account", MAX_LABEL))
        "revoke" -> CompanionFrame.Revoke
        "ping" -> CompanionFrame.Ping
        "pong" -> CompanionFrame.Pong
        else -> null
    }

    private fun text(json: JSONObject, name: String, max: Int, allowEmpty: Boolean = true): String {
        val value = json.opt(name) as? String ?: throw IllegalArgumentException(name)
        require(value.length <= max && (allowEmpty || value.isNotEmpty())) { name }
        return value
    }

    private fun op(json: JSONObject): String =
        (json.opt("op") as? String)?.takeIf { OP.matches(it) } ?: throw IllegalArgumentException("op")

    private fun station(json: JSONObject): String =
        (json.opt("station") as? String)?.takeIf { STATION.matches(it) } ?: throw IllegalArgumentException("station")

    private fun hex(json: JSONObject, name: String, length: Int): String =
        (json.opt(name) as? String)?.takeIf { it.length == length && it.all { c -> c in "0123456789abcdef" } }
            ?: throw IllegalArgumentException(name)
}

class MalformedFrameException(message: String) : Exception(message)
