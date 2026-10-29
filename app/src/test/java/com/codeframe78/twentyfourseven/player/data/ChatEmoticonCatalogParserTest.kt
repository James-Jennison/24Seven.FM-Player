package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.ChatEmoticonChoice
import com.codeframe78.twentyfourseven.player.domain.ChatMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ChatEmoticonCatalogParserTest {
    private val parser = ChatEmoticonCatalogParser()
    private val baseUrl = "https://streamingsoundtracks.com/modules/ClearChat/common/smilies.php"

    @Test
    fun `reads tabs, groups and choices the way the station's picker lists them`() {
        val catalog = parser.parse(
            """
                <body class="smiley-picker station-sst">
                <h1>StreamingSoundtracks Smilies</h1>
                <nav class="smiley-tabs" role="tablist">
                  <button type="button" role="tab" id="tab-faces" aria-controls="panel-faces" data-smiley-tab="faces">Common</button>
                  <button type="button" role="tab" id="tab-custom" aria-controls="panel-custom" data-smiley-tab="custom">Extra</button>
                  <button type="button" role="tab" id="tab-native" aria-controls="panel-native" data-smiley-tab="native">Emoji</button>
                  <button type="button" role="tab" id="tab-ghost" aria-controls="panel-ghost" data-smiley-tab="ghost">Ghost</button>
                </nav>
                <section id="panel-faces" class="smiley-panel"><h2>Common</h2>
                  <h3>Everyday</h3>
                  <button type="button" class="smiley-choice" data-smiley-code="-smile-" title="Smile -smile- (also: :))" aria-label="Smile"><picture class="station-smiley"><source media="(prefers-reduced-motion: reduce)" srcset="/modules/ClearChat/common/station-smilies/assets/sst/regular/smile.svg"><img src="/modules/ClearChat/common/station-smilies/assets/sst/regular/smile.gif" alt="" width="20" height="20"></picture></button>
                  <button type="button" class="smiley-choice" data-smiley-code="-tracker-" aria-label="Tracker"><img src="https://example.com/t.gif" alt="" width="20" height="20"></button>
                  <button type="button" class="smiley-choice" data-smiley-code="" aria-label="Blank"></button>
                  <h3>Stations</h3>
                  <button type="button" class="smiley-choice" data-smiley-code="-station80s-" title="1980s.FM -station80s-" aria-label="1980s.FM"><picture><img src="/modules/ClearChat/common/station-smilies/station/station80s.ico" alt="" width="20" height="20"></picture></button>
                  <h3>Empty group</h3>
                </section>
                <section id="panel-custom" class="smiley-panel"><h2>Extra</h2>
                  <h3>Community Favorites</h3>
                  <button type="button" class="smiley-choice" data-smiley-code="-fisher-" title="Fisher -fisher-" aria-label="Fisher"><picture class="station-smiley station-smiley-wide"><img src="/modules/ClearChat/common/station-smilies/originals/fisher.gif" alt="" width="72" height="32"></picture></button>
                </section>
                <section id="panel-native" class="smiley-panel"><h2>Emoji</h2>
                  <button type="button" class="smiley-choice smiley-emoji" data-smiley-code="📻" title="📻" aria-label="📻">📻</button>
                  <h3>Faces</h3>
                  <button type="button" class="smiley-choice smiley-emoji" data-smiley-code="😀" title="😀" aria-label="😀">😀</button>
                </section>
                </body>
            """.trimIndent(),
            baseUrl,
        )

        assertEquals(listOf("faces" to "Common", "custom" to "Extra", "native" to "Emoji"), catalog.tabs.map { it.id to it.label })
        val faces = catalog.tabs[0]
        assertEquals(listOf("Everyday", "Stations"), faces.groups.map { it.label })
        assertEquals(
            listOf(
                ChatEmoticonChoice(
                    "-smile-",
                    "Smile",
                    ChatMessagePart.Emoticon(
                        "Smile",
                        "https://streamingsoundtracks.com/modules/ClearChat/common/station-smilies/assets/sst/regular/smile.gif",
                    ),
                ),
            ),
            faces.groups[0].choices,
        )
        assertEquals("-station80s-", faces.groups[1].choices.single().code)
        val fisher = catalog.tabs[1].groups.single().choices.single()
        assertEquals(72, fisher.image?.widthPx)
        assertEquals(32, fisher.image?.heightPx)
        val emoji = catalog.tabs[2]
        assertEquals(listOf("Emoji", "Faces"), emoji.groups.map { it.label })
        assertEquals("📻", emoji.groups[0].choices.single().code)
        assertNull(emoji.groups[1].choices.single().image)
    }

    @Test
    fun `fails when the page holds no catalog`() {
        assertThrows(IOException::class.java) { parser.parse("<html><body><p>Sign in</p></body></html>", baseUrl) }
    }

    @Test
    fun `repeated codes, group headings and tab ids cannot produce duplicate picker keys`() {
        val catalog = parser.parse(
            """
                <nav>
                  <button data-smiley-tab="faces" aria-controls="panel-faces">Common</button>
                  <button data-smiley-tab="faces" aria-controls="panel-faces">Again</button>
                </nav>
                <section id="panel-faces"><h2>Common</h2>
                  <h3>Everyday</h3>
                  <button data-smiley-code="a" aria-label="A"></button>
                  <button data-smiley-code="a" aria-label="A again"></button>
                  <h3>Everyday</h3>
                  <button data-smiley-code="a" aria-label="A third"></button>
                  <button data-smiley-code="b" aria-label="B"></button>
                </section>
            """.trimIndent(),
            baseUrl,
        )

        assertEquals(listOf("faces"), catalog.tabs.map { it.id })
        assertEquals(listOf("Everyday"), catalog.tabs.single().groups.map { it.label })
        assertEquals(listOf("a", "b"), catalog.tabs.single().groups.single().choices.map { it.code })
    }
}
