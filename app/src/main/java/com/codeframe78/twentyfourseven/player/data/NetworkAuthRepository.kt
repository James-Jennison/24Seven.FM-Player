package com.codeframe78.twentyfourseven.player.data

import android.content.Context
import com.codeframe78.twentyfourseven.player.domain.AuthRepository
import com.codeframe78.twentyfourseven.player.domain.AuthState
import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    constructor(context: Context) : this(
        StationAuthRemoteDataSource(sessionStore = AndroidKeystoreAuthSessionStore(context)),
    )

    override fun observeAuth(stationId: StationId): Flow<AuthState> {
        val canonical = stationId.canonicalized()
        return combine(
            state(canonical).asStateFlow(),
            remote.observeSessionValidity(canonical),
        ) { state, validity ->
            if (
                validity == ProtectedSessionValidity.Expired &&
                state.status in setOf(AuthStatus.Unavailable, AuthStatus.SignedIn)
            ) {
                AuthState(
                    stationId = canonical,
                    status = AuthStatus.Expired,
                    errorMessage = "Your saved station session expired. Sign in again.",
                )
            } else {
                state
            }
        }
    }

    override suspend fun restoreSession(stationId: StationId) {
        val canonical = stationId.canonicalized()
        lock(canonical).withLock {
            if (state(canonical).value.status != AuthStatus.Unavailable) return@withLock
            when (val restored = remote.restoredSession(canonical)) {
                RestoredAuthSession.None -> Unit
                RestoredAuthSession.Expired -> state(canonical).value = AuthState(
                    stationId = canonical,
                    status = AuthStatus.Expired,
                    errorMessage = "Your saved station session expired. Sign in again.",
                )
                is RestoredAuthSession.SignedIn -> state(canonical).value = AuthState(
                    canonical,
                    AuthStatus.SignedIn,
                    displayName = restored.displayName,
                )
            }
        }
    }

    override suspend fun refreshChallenge(stationId: StationId) {
        val canonical = stationId.canonicalized()
        lock(canonical).withLock { loadChallenge(canonical, errorMessage = null) }
    }

    override suspend fun signIn(
        stationId: StationId,
        username: String,
        password: String,
        securityCode: String,
    ) {
        val canonical = stationId.canonicalized()
        lock(canonical).withLock {
            val challenge = challenges[canonical]
            if (username.isBlank() || password.isBlank() || securityCode.isBlank() || challenge == null) {
                loadChallenge(canonical, "Enter your username, password, and security code.")
                return@withLock
            }
            state(canonical).value = AuthState(canonical, AuthStatus.SigningIn)
            runCatching {
                val page = remote.signIn(canonical, challenge, username, password, securityCode)
                val action = URI(challenge.actionUrl)
                val origin = "${action.scheme}://${action.authority}/"
                resultParser.parseSignedInDisplayName(page.html, origin, username)
            }.onSuccess { displayName ->
                remote.persistSession(canonical, displayName)
                challenges.remove(canonical)
                state(canonical).value = AuthState(canonical, AuthStatus.SignedIn, displayName = displayName)
            }.onFailure {
                loadChallenge(canonical, "Sign in failed. Check your details and the new security code.")
            }
        }
    }

    override suspend fun signOut(stationId: StationId) {
        val canonical = stationId.canonicalized()
        lock(canonical).withLock {
            runCatching { remote.signOut(canonical) }
            challenges.remove(canonical)
            loadChallenge(canonical, errorMessage = null)
        }
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
