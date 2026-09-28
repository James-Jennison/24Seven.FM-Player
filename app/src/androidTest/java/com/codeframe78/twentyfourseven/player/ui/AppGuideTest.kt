package com.codeframe78.twentyfourseven.player.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
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
                    onDismiss = { dismissed = true },
                    onComplete = {},
                )
            }
        }

        composeRule.onNodeWithTag("app_guide_overlay").assertIsDisplayed()
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
                    onDismiss = {},
                    onComplete = { completed = true },
                )
            }
        }

        composeRule.onNodeWithTag("app_guide_next").performClick()
        composeRule.onNodeWithTag("app_guide_back").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("app_guide_skip").assertIsDisplayed()
        assertFalse(completed)

        repeat(2) {
            composeRule.onNodeWithTag("app_guide_next").performClick()
        }
        composeRule.onNodeWithTag("app_guide_complete").assertIsDisplayed().performClick()
        assertTrue(completed)
    }

    @Test
    fun guideTitleAndBodyUseReadableContentColorInDarkTheme() {
        assertTextUsesReadableContentColor(darkTheme = true)
    }

    @Test
    fun guideTitleAndBodyUseReadableContentColorInLightTheme() {
        assertTextUsesReadableContentColor(darkTheme = false)
    }

    private fun assertTextUsesReadableContentColor(darkTheme: Boolean) {
        var expectedTextColor = Color.Unspecified
        var expectedSurfaceColor = Color.Unspecified
        composeRule.setContent {
            TwentyFourSevenTheme(darkTheme = darkTheme) {
                expectedTextColor = MaterialTheme.colorScheme.onSurface
                expectedSurfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
                AppGuideDialog(
                    onDismiss = {},
                    onComplete = {},
                )
            }
        }

        composeRule.waitForIdle()
        assertUsesExpectedTextColor("app_guide_title", expectedTextColor)
        assertUsesExpectedTextColor("app_guide_body", expectedTextColor)
        assertMinimumContrast(expectedTextColor, expectedSurfaceColor)
    }

    private fun assertUsesExpectedTextColor(tag: String, expected: Color) {
        val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
        val pixelCount = pixels.width * pixels.height
        val expectedPixelCount = (pixelCount * 0.01f).toInt().coerceAtLeast(12)
        var matchingPixelCount = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val pixel = pixels[x, y]
                if (
                    kotlin.math.abs(pixel.red - expected.red) <= 0.08f &&
                    kotlin.math.abs(pixel.green - expected.green) <= 0.08f &&
                    kotlin.math.abs(pixel.blue - expected.blue) <= 0.08f
                ) {
                    matchingPixelCount += 1
                }
            }
        }
        assertTrue(
            "Expected $tag to render $expected; matched $matchingPixelCount of $pixelCount pixels",
            matchingPixelCount >= expectedPixelCount,
        )
    }

    private fun assertMinimumContrast(foreground: Color, surface: Color) {
        val worstCaseBackdrop = if (foreground.luminance() > 0.5f) Color.White else Color.Black
        val compositedSurface = surface.copy(alpha = 0.94f).compositeOver(worstCaseBackdrop)
        val lighter = maxOf(foreground.luminance(), compositedSurface.luminance())
        val darker = minOf(foreground.luminance(), compositedSurface.luminance())
        val contrastRatio = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue(
            "Expected text-to-surface contrast >= 4.5:1, was $contrastRatio:1",
            contrastRatio >= 4.5f,
        )
    }
}
