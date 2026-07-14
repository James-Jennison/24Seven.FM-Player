package com.codeframe78.twentyfourseven.player.data

import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal class ChatPostCredentials(
    val username: String,
    val token: String,
) {
    override fun toString(): String = "ChatPostCredentials([redacted])"
}

internal class ChatPostFormParser {
    fun parse(html: String, origin: String): ChatPostCredentials {
        val frames = Jsoup.parse(html, origin).select("iframe[name=inputframe][src]")
        if (frames.size != 1) throw IOException("Station chat form was unavailable")
        val source = frames.single().absUrl("src")
        val uri = URI(source)
        val expected = URI(origin)
        if (
            uri.scheme != "https" ||
            !uri.host.equals(expected.host, ignoreCase = true) ||
            uri.port != expected.port ||
            uri.path != POST_PATH
        ) {
            throw IOException("Untrusted chat form destination")
        }
        val parameters = parseQuery(uri.rawQuery)
        val usernames = parameters["username"].orEmpty()
        val tokens = parameters["userpw"].orEmpty()
        if (usernames.size != 1 || tokens.size != 1 || usernames.single().isBlank() || tokens.single().isBlank()) {
            throw IOException("Station chat form credentials were unavailable")
        }
        return ChatPostCredentials(usernames.single(), tokens.single())
    }

    private fun parseQuery(query: String?): Map<String, List<String>> = query.orEmpty()
        .split('&')
        .filter(String::isNotBlank)
        .map { part ->
            val pieces = part.split('=', limit = 2)
            decode(pieces[0]) to decode(pieces.getOrElse(1) { "" })
        }
        .groupBy({ it.first }, { it.second })

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    internal companion object {
        const val POST_PATH = "/modules/ClearChat/block-files/input.php"
    }
}
