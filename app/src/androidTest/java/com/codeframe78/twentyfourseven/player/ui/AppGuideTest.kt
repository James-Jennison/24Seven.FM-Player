package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.codeframe78.twentyfourseven.player.ui.theme.TwentyFourSevenTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppGuideTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun guideCanBeSkippedFromFirstStep() {
        var dismissed = false
        composeRule.setContent {
            TwentyFourSevenTheme {
                AppGuideDialog(
                    station = null,
                    onDismiss = { dismissed = true },
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithTag("app_guide_title").assertIsDisplayed()
        composeRule.onNodeWithTag("app_guide_skip").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun guideAdvancesBacksAndCompletes() {
        var completed = false
        composeRule.setContent {
            TwentyFourSevenTheme {
                AppGuideDialog(
                    station = null,
                    onDismiss = {},
                    onComplete = { completed = true },
                )
            }
        }

        composeRule.onNodeWithTag("app_guide_next").performClick()
        composeRule.onNodeWithTag("app_guide_back").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("app_guide_skip").assertIsDisplayed()
        assertFalse(completed)

        repeat(5) {
            composeRule.onNodeWithTag("app_guide_next").performClick()
        }
        composeRule.onNodeWithTag("app_guide_complete").assertIsDisplayed().performClick()
        assertTrue(completed)
    }
}
