package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AuthRepository
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

class NetworkAuthRepository internal constructor(
    private val remote: AuthRemoteDataSource,
    private val resultParser: AuthLoginResultParser = AuthLoginResultParser(),
) : AuthRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<AuthState>>()
    private val challenges = ConcurrentHashMap<StationId, LoginChallenge>()
    private val locks = ConcurrentHashMap<StationId, Mutex>()

    constructor() : this(StationAuthRemoteDataSource())

    override fun observeAuth(stationId: StationId): Flow<AuthState> = state(stationId).asStateFlow()

    override suspend fun refreshChallenge(stationId: StationId) = lock(stationId).withLock {
        loadChallenge(stationId, errorMessage = null)
    }

    override suspend fun signIn(
        stationId: StationId,
        username: String,
        password: String,
        securityCode: String,
    ) = lock(stationId).withLock {
        val challenge = challenges[stationId]
        if (username.isBlank() || password.isBlank() || securityCode.isBlank() || challenge == null) {
            loadChallenge(stationId, "Enter your username, password, and security code.")
            return@withLock
        }
        state(stationId).value = AuthState(stationId, AuthStatus.SigningIn)
        runCatching {
            val page = remote.signIn(stationId, challenge, username, password, securityCode)
            val action = URI(challenge.actionUrl)
            val origin = "${action.scheme}://${action.authority}/"
            resultParser.parseSignedInDisplayName(page.html, origin, username)
        }.onSuccess { displayName ->
            remote.persistSession(stationId)
            challenges.remove(stationId)
            state(stationId).value = AuthState(stationId, AuthStatus.SignedIn, displayName = displayName)
        }.onFailure {
            loadChallenge(stationId, "Sign in failed. Check your details and the new security code.")
        }
    }

    override suspend fun signOut(stationId: StationId) = lock(stationId).withLock {
        runCatching { remote.signOut(stationId) }
        challenges.remove(stationId)
        loadChallenge(stationId, errorMessage = null)
    }

    private suspend fun loadChallenge(stationId: StationId, errorMessage: String?) {
        state(stationId).value = AuthState(stationId, AuthStatus.LoadingChallenge)
        runCatching { remote.fetchChallenge(stationId) }
            .onSuccess { challenge ->
                challenges[stationId] = challenge
                state(stationId).value = AuthState(
                    stationId,
                    if (errorMessage == null) AuthStatus.SignedOut else AuthStatus.Error,
                    challengeImageUrl = challenge.imageUrl,
                    errorMessage = errorMessage,
                )
            }
            .onFailure {
                challenges.remove(stationId)
                state(stationId).value = AuthState(
                    stationId,
                    AuthStatus.Error,
                    errorMessage = "The station login could not be loaded.",
                )
            }
    }

    private fun state(stationId: StationId) = states.getOrPut(stationId) {
        MutableStateFlow(AuthState(stationId))
    }

    private fun lock(stationId: StationId) = locks.getOrPut(stationId, ::Mutex)
}
