package com.codeframe78.twentyfourseven.player.companion

/**
 * Master switch for the TV companion pairing feature. It is off in 1.0.3: nothing in this
 * package is reachable from the UI, opens a socket or requests a permission until the Owner
 * approves the privacy wording and this constant is turned on in a later release.
 */
object TvCompanionFeature {
    const val ENABLED: Boolean = false
}
