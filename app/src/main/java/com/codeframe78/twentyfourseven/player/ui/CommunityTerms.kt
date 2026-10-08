package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codeframe78.twentyfourseven.player.R

/** One piece of the terms as it is laid out: a section heading, a paragraph with an optional lead-in, or a list item. */
internal sealed interface TermsBlock {
    data class Heading(val text: String) : TermsBlock
    data class Paragraph(val label: String?, val text: String) : TermsBlock
    data class Bullet(val text: String) : TermsBlock
}

/**
 * Reads the terms document into blocks. The document's own title and its "[ ] I Agree" form lines are left out: the
 * screen carries the title, and its buttons are the form.
 */
internal fun termsBlocks(document: String): List<TermsBlock> = document.lines()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .filterIndexed { index, line -> !(index == 0 && line.startsWith("Terms of Participation")) }
    .filterNot { it.startsWith("[ ]") }
    .map { line ->
        val labelled = TERMS_LABELLED_PARAGRAPH.matchEntire(line)
        when {
            TERMS_SECTION.containsMatchIn(line) || line == "Agreement of Terms" -> TermsBlock.Heading(line)
            line.startsWith("•") -> TermsBlock.Bullet(line.removePrefix("•").trim())
            labelled != null -> TermsBlock.Paragraph(labelled.groupValues[1], labelled.groupValues[2])
            else -> TermsBlock.Paragraph(null, line)
        }
    }

private val TERMS_SECTION = Regex("""^\d+\.\s""")
private val TERMS_LABELLED_PARAGRAPH = Regex("""^([A-Z0-9][^:.]{2,40}): (.+)$""")

/**
 * The Terms of Participation as a page to read, with one way to accept them. Terms that are already accepted are
 * shown to be read again and closed.
 */
@Composable
internal fun CommunityTermsDialog(
    alreadyAccepted: Boolean,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    val resources = LocalResources.current
    val blocks = remember(resources) {
        termsBlocks(resources.openRawResource(R.raw.terms_of_participation).bufferedReader().use { it.readText() })
    }
    Dialog(onDismissRequest = onDecline, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().testTag("community_terms")) {
            Column(Modifier.readablePane()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Terms of Participation", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "24Seven.FM network",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDecline) {
                        Icon(Icons.Default.Close, contentDescription = "Close terms")
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("community_terms_text"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    blocks.forEach { block -> TermsBlockText(block) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (alreadyAccepted) {
                        Text(
                            "You accepted these terms.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = onDecline, modifier = Modifier.testTag("close_community_terms")) { Text("Close") }
                    } else {
                        TextButton(onClick = onDecline, modifier = Modifier.testTag("decline_community_terms")) {
                            Text("Decline")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onAgree, modifier = Modifier.testTag("accept_community_terms")) {
                            Text("I Agree")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermsBlockText(block: TermsBlock) {
    when (block) {
        is TermsBlock.Heading -> Text(
            block.text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp),
        )
        is TermsBlock.Paragraph -> Text(
            buildAnnotatedString {
                block.label?.let { label ->
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("$label. ") }
                }
                append(block.text)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        is TermsBlock.Bullet -> Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(block.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
