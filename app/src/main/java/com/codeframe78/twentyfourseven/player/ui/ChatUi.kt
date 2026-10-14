package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
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
import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonCatalog
import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonChoice
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

private const val CHAT_EMOTICON_HEIGHT_EM = 1.15f

/** Wide station emoticons keep their declared shape up to this width, so one cannot span the row. */
private const val CHAT_EMOTICON_MAX_WIDTH_EM = 4f

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
            val width = (CHAT_EMOTICON_HEIGHT_EM * part.aspectRatio)
                .coerceIn(CHAT_EMOTICON_HEIGHT_EM, CHAT_EMOTICON_MAX_WIDTH_EM)
            id to InlineTextContent(
                placeholder = Placeholder(width.em, CHAT_EMOTICON_HEIGHT_EM.em, PlaceholderVerticalAlign.TextCenter),
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

/**
 * Inserts a smiley [code] at the draft's cursor, with the space the station's `-code-` syntax needs when a
 * letter or digit would otherwise touch it. Returns null when the result would pass [limit] characters.
 */
internal fun insertChatEmoticon(draft: TextFieldValue, code: String, limit: Int = CHAT_MESSAGE_LIMIT): TextFieldValue? {
    val text = draft.text
    val start = draft.selection.min.coerceIn(0, text.length)
    val end = draft.selection.max.coerceIn(0, text.length)
    val before = text.substring(0, start)
    val after = text.substring(end)
    val spaceBefore = code.startsWith("-") && before.lastOrNull()?.isLetterOrDigit() == true
    val spaceAfter = code.endsWith("-") && after.firstOrNull()?.isLetterOrDigit() == true
    val insertion = buildString {
        if (spaceBefore) append(' ')
        append(code)
        if (spaceAfter) append(' ')
    }
    val next = before + insertion + after
    if (next.length > limit) return null
    return TextFieldValue(next, TextRange(before.length + insertion.length))
}

internal const val CHAT_MESSAGE_LIMIT = 255

/** The station's smiley catalog as a sheet over the composer: its tabs, its groups, and a tap inserts the code. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatEmoticonPicker(
    stationName: String?,
    catalog: ChatEmoticonCatalog?,
    isLoading: Boolean,
    errorMessage: String?,
    draft: String,
    notice: String?,
    onRetry: () -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag("chat_emoticon_picker")) {
        Column(Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 560.dp).padding(bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Smilies", style = MaterialTheme.typography.titleMedium)
                    stationName?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
            Text(
                text = if (draft.isBlank()) "Your message is empty." else draft,
                style = ChatLineStyle(),
                color = if (draft.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("chat_emoticon_draft"),
            )
            notice?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when {
                catalog != null -> ChatEmoticonCatalogContent(catalog, onPick)
                isLoading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> Column(
                    Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        errorMessage ?: "The station's smilies could not be loaded.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ChatEmoticonCatalogContent(
    catalog: ChatEmoticonCatalog,
    onPick: (String) -> Unit,
) {
    var tabIndex by rememberSaveable(catalog) { mutableIntStateOf(0) }
    val tab = catalog.tabs[tabIndex.coerceIn(0, catalog.tabs.lastIndex)]
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        catalog.tabs.forEachIndexed { index, candidate ->
            FilterChip(
                selected = index == tabIndex,
                onClick = { tabIndex = index },
                label = { Text(candidate.label) },
                modifier = Modifier.testTag("chat_emoticon_tab_${candidate.id}"),
            )
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 52.dp),
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        tab.groups.forEach { group ->
            item(key = "${tab.id}/${group.label}", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    group.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
                )
            }
            items(group.choices.size, key = { "${tab.id}/${group.label}/${group.choices[it].code}" }) { index ->
                ChatEmoticonChoiceCell(group.choices[index], onPick)
            }
        }
    }
}

@Composable
private fun ChatEmoticonChoiceCell(choice: ChatEmoticonChoice, onPick: (String) -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Insert ${choice.label}") { onPick(choice.code) }
            .semantics { contentDescription = choice.label }
            .testTag("chat_emoticon_choice"),
        contentAlignment = Alignment.Center,
    ) {
        val image = choice.image
        if (image == null) {
            Text(choice.code, fontSize = 24.sp)
        } else {
            val height = (44.dp / image.aspectRatio).coerceAtMost(30.dp)
            AsyncImage(
                model = image.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = height * image.aspectRatio, height = height),
            )
        }
    }
}
