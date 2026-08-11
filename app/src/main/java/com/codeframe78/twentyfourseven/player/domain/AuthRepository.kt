package com.codeframe78.twentyfourseven.player.domain

import kotlinx.coroutines.flow.Flow

enum class AuthStatus { Unavailable, LoadingChallenge, SignedOut, SigningIn, SignedIn, Expired, Error }

data class AuthState(
    val stationId: StationId,
    val status: AuthStatus = AuthStatus.Unavailable,
    val displayName: String? = null,
    val challengeImageUrl: String? = null,
    val antiSpamPrompt: String? = null,
    val errorMessage: String? = null,
)

interface AuthRepository {
    fun observeAuth(stationId: StationId): Flow<AuthState>

    suspend fun restoreSession(stationId: StationId)

    suspend fun refreshChallenge(stationId: StationId)

    /** Credentials are transient inputs and must never be persisted or logged. */
    suspend fun signIn(stationId: StationId, username: String, password: String, securityCode: String)

    suspend fun signOut(stationId: StationId)
}
