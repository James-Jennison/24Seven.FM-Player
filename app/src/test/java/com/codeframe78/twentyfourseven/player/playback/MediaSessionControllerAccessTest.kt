package com.codeframe78.twentyfourseven.player.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSessionControllerAccessTest {
    private val player = "com.codeframe78.twentyfourseven.player"
    private val androidAuto = "com.google.android.projection.gearhead"

    @Test
    fun `verified Player package is the local app`() {
        assertEquals(ControllerAccess.LocalApp, access(player, isTrusted = false, isPackageNameVerified = true))
    }

    @Test
    fun `unverified Player package name earns no local authority`() {
        assertEquals(ControllerAccess.Foreign, access(player, isTrusted = false, isPackageNameVerified = false))
        assertEquals(ControllerAccess.TrustedSystem, access(player, isTrusted = true, isPackageNameVerified = false))
    }

    @Test
    fun `Android Auto is recognized when verified or when unverified but trusted for media control`() {
        assertEquals(ControllerAccess.Automotive, access(androidAuto, isTrusted = false, isPackageNameVerified = true))
        assertEquals(ControllerAccess.Automotive, access(androidAuto, isTrusted = true, isPackageNameVerified = false))
    }

    @Test
    fun `unverified and untrusted Android Auto name stays foreign`() {
        assertEquals(ControllerAccess.Foreign, access(androidAuto, isTrusted = false, isPackageNameVerified = false))
    }

    @Test
    fun `other trusted controllers are trusted system and the rest are foreign`() {
        assertEquals(ControllerAccess.TrustedSystem, access("com.android.systemui", isTrusted = true, isPackageNameVerified = true))
        assertEquals(ControllerAccess.Foreign, access("com.example.other", isTrusted = false, isPackageNameVerified = true))
    }

    private fun access(packageName: String, isTrusted: Boolean, isPackageNameVerified: Boolean) =
        MediaSessionControllerPolicy.access(packageName, isTrusted, isPackageNameVerified, player)
}
