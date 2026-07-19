package com.codeframe78.twentyfourseven.player.data

import java.io.IOException
import java.net.URI

internal object TrustedStationNavigation {
    fun requireSameHttpsOrigin(candidate: URI, expectedOrigin: URI) {
        if (
            !candidate.scheme.equals("https", ignoreCase = true) ||
            !expectedOrigin.scheme.equals("https", ignoreCase = true) ||
            candidate.host.isNullOrBlank() ||
            !candidate.host.equals(expectedOrigin.host, ignoreCase = true) ||
            candidate.userInfo != null ||
            effectivePort(candidate) != effectivePort(expectedOrigin)
        ) {
            throw IOException("Untrusted station destination")
        }
    }

    fun resolveRedirect(current: URI, location: String?, expectedOrigin: URI): URI {
        val value = location?.takeIf(String::isNotBlank)
            ?: throw IOException("Station redirect was invalid")
        val resolved = runCatching { current.resolve(value) }
            .getOrElse { throw IOException("Station redirect was invalid") }
        requireSameHttpsOrigin(resolved, expectedOrigin)
        return resolved
    }

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> -1
    }
}
