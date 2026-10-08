package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.codeframe78.twentyfourseven.player.R
import com.codeframe78.twentyfourseven.player.domain.ChatMessage
import com.codeframe78.twentyfourseven.player.domain.ChatMessagePart
import com.codeframe78.twentyfourseven.player.domain.ChatRole

/** The day and the clock of a station chat timestamp: "02 Oct 26 - 15:04:41" is day "02 Oct", clock "15:04". */
internal data class ChatStamp(val day: String, val clock: String)

internal fun chatStamp(raw: String?): ChatStamp? =
    raw?.let { CHAT_TIMESTAMP.matchEntire(it.trim()) }?.let { ChatStamp(it.groupValues[1], it.groupValues[2].padStart(5, '0')) }

private val CHAT_TIMESTAMP = Regex("""(\d{1,2} \p{L}{3}) \d{2} - (\d{1,2}:\d{2}):\d{2}""")

/** True when [text] names [nick] as a whole word, so "Morgan" does not highlight for "Morg". */
internal fun chatMentions(text: String, nick: String?): Boolean {
    val name = nick?.trim().orEmpty()
    if (name.isEmpty()) return false
    return Regex("(?<![\\p{L}\\p{N}_])${Regex.escape(name)}(?![\\p{L}\\p{N}_])", RegexOption.IGNORE_CASE).containsMatchIn(text)
}

/**
 * The colour the station's legend gives a role. On a dark surface these are the station stylesheet's own values; on a
 * light one the same hues are darkened until they can be read. Members without a role take the theme's accent.
 */
internal fun chatRoleColor(role: ChatRole, onDark: Boolean): Color? = when (role) {
    ChatRole.Member -> null
    ChatRole.Proprietor -> if (onDark) Color(0xFFCC66FF) else Color(0xFF8E24C9)
    ChatRole.Administrator -> if (onDark) Color(0xFFFFCC66) else Color(0xFF8F6200)
    ChatRole.Moderator -> if (onDark) Color(0xFF66FF66) else Color(0xFF1B7F1B)
    ChatRole.Ambassador -> if (onDark) Color(0xFFCC5100) else Color(0xFFB84800)
    ChatRole.VisitorMod -> if (onDark) Color(0xFF0094FF) else Color(0xFF006FC4)
    ChatRole.Composer -> if (onDark) Color(0xFFFFFF00) else Color(0xFF6E6A00)
}

/** One chat line the way an IRC client writes it: `[15:04] <Nick> message`, in a fixed-width face. */
@Composable
internal fun ChatLine(message: ChatMessage, ownNick: String?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val nickColor = chatRoleColor(message.authorRole, onDark = scheme.surface.luminance() < 0.5f) ?: scheme.primary
    val dim = scheme.onSurfaceVariant
    val inlineContent = remember(message.parts) {
        message.parts.mapIndexedNotNull { index, part ->
            if (part !is ChatMessagePart.Emoticon) return@mapIndexedNotNull null
            val id = "chat-emoticon-$index"
            id to InlineTextContent(
                placeholder = Placeholder(1.15.em, 1.15.em, PlaceholderVerticalAlign.TextCenter),
            ) {
                AsyncImage(
                    model = part.imageUrl,
                    contentDescription = "${part.altText} emoticon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().testTag(id),
                )
            }
        }.toMap()
    }
    val line = remember(message, nickColor, dim) {
        buildAnnotatedString {
            val clock = chatStamp(message.postedAtLabel)?.clock ?: message.postedAtLabel
            clock?.let { withStyle(SpanStyle(color = dim)) { append("[$it] ") } }
            withStyle(SpanStyle(color = dim)) { append("<") }
            withStyle(SpanStyle(color = nickColor, fontWeight = FontWeight.Bold)) { append(message.authorDisplayName) }
            withStyle(SpanStyle(color = dim)) { append("> ") }
            message.parts.forEachIndexed { index, part ->
                when (part) {
                    is ChatMessagePart.Text -> append(part.value)
                    is ChatMessagePart.Emoticon -> appendInlineContent("chat-emoticon-$index", part.altText)
                }
            }
        }
    }
    val mentioned = remember(message.messageText, ownNick) { chatMentions(message.messageText, ownNick) }
    Text(
        line,
        inlineContent = inlineContent,
        style = ChatLineStyle(),
        modifier = modifier
            .fillMaxWidth()
            .then(if (mentioned) Modifier.background(scheme.primary.copy(alpha = 0.16f)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 3.dp),
    )
}

/** "— 02 Oct —" above the first line of a day. */
@Composable
internal fun ChatDayRule(day: String) {
    Text(
        "\u2014 $day \u2014",
        style = ChatLineStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

@Composable
internal fun ChatLineStyle() =
    MaterialTheme.typography.bodyMedium.copy(fontFamily = ChatFontFamily, fontSize = 13.sp, lineHeight = 19.sp)

/** Bundled, because a phone's font theme can replace the system's fixed-width face with a proportional one. */
private val ChatFontFamily = FontFamily(
    Font(R.font.dejavu_sans_mono, FontWeight.Normal),
    Font(R.font.dejavu_sans_mono_bold, FontWeight.Bold),
)
