package com.codeframe78.twentyfourseven.player.companion

import com.codeframe78.twentyfourseven.player.domain.SongRequestOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompanionTrackRefTest {
    @Test
    fun `a reference splits at the last colon and album ids cannot contain one`() {
        assertEquals(CompanionTrackRef("B000ABC123", "4567"), CompanionTrackRef.parse("B000ABC123:4567"))
        assertNull(CompanionTrackRef.parse("a:b:9"))
    }

    @Test
    fun `bad references are rejected`() {
        listOf("", "nocolon", ":123", "album:", "album:12x", "al bum:1", "album/../x:1", "a".repeat(65) + ":1", "album:1234567890123")
            .forEach { assertNull(it, CompanionTrackRef.parse(it)) }
    }

    @Test
    fun `outcomes map conservatively`() {
        assertEquals(CompanionSubmitOutcome.Accepted, SongRequestOutcome.Submitted.toCompanionOutcome())
        assertEquals(CompanionSubmitOutcome.Refused("No"), SongRequestOutcome.Rejected("No").toCompanionOutcome())
        assertEquals(CompanionSubmitOutcome.Refused("Sign in"), SongRequestOutcome.NotSent("Sign in").toCompanionOutcome())
        assertEquals(CompanionSubmitOutcome.Uncertain, SongRequestOutcome.Unconfirmed.toCompanionOutcome())
        assertEquals(CompanionSubmitOutcome.Uncertain, (null as SongRequestOutcome?).toCompanionOutcome())
    }
}
