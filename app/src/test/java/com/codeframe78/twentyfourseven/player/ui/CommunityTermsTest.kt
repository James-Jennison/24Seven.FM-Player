package com.codeframe78.twentyfourseven.player.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityTermsTest {
    @Test
    fun `terms are read as headings paragraphs and bullets without the title or form lines`() {
        val blocks = termsBlocks(
            """
            Terms of Participation: 24seven.FM Network

            1. Age Verification and Access

            18+ Requirement: This is a strictly 18+ environment.

            Users may not post:

            • spam; or
            • threats.

            Agreement of Terms

            By clicking "I Agree" below, you confirm that you have read these terms.

            [ ] I Agree
            [ ] I Decline
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                TermsBlock.Heading("1. Age Verification and Access"),
                TermsBlock.Paragraph("18+ Requirement", "This is a strictly 18+ environment."),
                TermsBlock.Paragraph(null, "Users may not post:"),
                TermsBlock.Bullet("spam; or"),
                TermsBlock.Bullet("threats."),
                TermsBlock.Heading("Agreement of Terms"),
                TermsBlock.Paragraph(null, "By clicking \"I Agree\" below, you confirm that you have read these terms."),
            ),
            blocks,
        )
    }
}
