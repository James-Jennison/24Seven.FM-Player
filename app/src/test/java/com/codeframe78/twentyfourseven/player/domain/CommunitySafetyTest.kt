package com.codeframe78.twentyfourseven.player.domain

import com.codeframe78.twentyfourseven.player.data.InMemoryCommunitySafetyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CommunitySafetyTest {
    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `age boundary requires the eighteenth birthday and rejects invalid dates`() {
        assertTrue(isAdultOnDate(LocalDate.of(2008, 7, 15), today))
        assertFalse(isAdultOnDate(LocalDate.of(2008, 7, 16), today))
        assertNull(validatedBirthDate(2026, 2, 30, today))
        assertNull(validatedBirthDate(2027, 1, 1, today))
    }

    @Test
    fun `identity normalization is case whitespace and compatibility stable`() {
        assertEquals("morg hubby", "  MorG   Hubby ".normalizedCommunityIdentity())
        assertEquals("morg", "ＭｏｒＧ".normalizedCommunityIdentity())
    }

    @Test
    fun `access requires adult screen current terms and separate visibility choice`() = runTest {
        val repository = InMemoryCommunitySafetyRepository(today = { today })

        repository.submitAgeScreen(1990, 3, 10)
        assertFalse(repository.observeSafety().first().canViewCommunityContent)

        repository.acceptTerms()
        assertFalse(repository.observeSafety().first().canViewCommunityContent)

        repository.setCommunityContentVisible(true)
        val enabled = repository.observeSafety().first()
        assertTrue(enabled.canViewCommunityContent)
        assertEquals(CURRENT_COMMUNITY_TERMS_VERSION, enabled.acceptedTermsVersion)

        repository.setCommunityContentVisible(false)
        assertFalse(repository.observeSafety().first().canViewCommunityContent)
    }

    @Test
    fun `blocks are normalized and remain station scoped`() = runTest {
        val repository = InMemoryCommunitySafetyRepository()
        val sst = StationId("sst")
        val adagio = StationId("afm")

        repository.blockUser(sst, " MorG  Hubby ")
        repository.blockUser(sst, "morg hubby")
        val blocked = repository.observeSafety().first()

        assertEquals(1, blocked.blockedUsers.size)
        assertTrue(blocked.isBlocked(sst, "MORG HUBBY"))
        assertFalse(blocked.isBlocked(adagio, "MORG HUBBY"))

        repository.unblockUser(sst, "morg hubby")
        assertFalse(repository.observeSafety().first().isBlocked(sst, "MorG Hubby"))
    }
}
