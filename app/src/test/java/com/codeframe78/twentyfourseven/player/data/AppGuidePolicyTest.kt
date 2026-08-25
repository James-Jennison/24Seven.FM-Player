package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppGuidePolicyTest {
    @Test
    fun freshInstallWithNoCompletionShowsGuide() {
        assertTrue(
            shouldAutoShowAppGuide(
                completedVersion = 0,
                currentVersion = 1,
                firstInstallTime = 10_000L,
                lastUpdateTime = 10_000L,
            ),
        )
    }

    @Test
    fun upgradedExistingInstallDoesNotForceGuide() {
        assertFalse(
            shouldAutoShowAppGuide(
                completedVersion = 0,
                currentVersion = 1,
                firstInstallTime = 10_000L,
                lastUpdateTime = 20_000L,
            ),
        )
    }

    @Test
    fun completedCurrentVersionDoesNotShowGuide() {
        assertFalse(
            shouldAutoShowAppGuide(
                completedVersion = 1,
                currentVersion = 1,
                firstInstallTime = 10_000L,
                lastUpdateTime = 10_000L,
            ),
        )
    }

    @Test
    fun olderCompletionCanShowOnFreshInstallState() {
        assertTrue(
            shouldAutoShowAppGuide(
                completedVersion = 1,
                currentVersion = 2,
                firstInstallTime = 10_000L,
                lastUpdateTime = 10_000L,
            ),
        )
    }
}
