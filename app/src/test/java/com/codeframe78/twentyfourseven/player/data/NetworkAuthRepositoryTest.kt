package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class NetworkAuthRepositoryTest {
    private val stationId = StationId("sst")
    private val challenge = LoginChallenge(
        "https://streamingsoundtracks.com/modules.php?name=Your_Account",
        "https://streamingsoundtracks.com/security-code.png",
        "123456",
    )

    @Test
    fun `challenge and successful login produce immutable states`() = runTest {
        val remote = FakeAuthRemoteDataSource(challenge)
        val repository = NetworkAuthRepository(remote)

        repository.refreshChallenge(stationId)
        assertEquals(AuthStatus.SignedOut, repository.observeAuth(stationId).first().status)
        assertEquals(challenge.imageUrl, repository.observeAuth(stationId).first().challengeImageUrl)

        repository.signIn(stationId, "Listener", "secret", "654321")
        val state = repository.observeAuth(stationId).first()
        assertEquals(AuthStatus.SignedIn, state.status)
        assertEquals("Listener", state.displayName)
        assertNull(state.challengeImageUrl)
        assertEquals(listOf("Listener", "secret", "654321"), remote.submittedInputs)
        assertEquals(1, remote.persistCalls)
    }

    @Test
    fun `failed login loads a fresh challenge and exposes no credentials`() = runTest {
        val remote = FakeAuthRemoteDataSource(challenge, failSignIn = true)
        val repository = NetworkAuthRepository(remote)
        repository.refreshChallenge(stationId)

        repository.signIn(stationId, "Listener", "secret", "654321")
        val state = repository.observeAuth(stationId).first()

        assertEquals(AuthStatus.Error, state.status)
        assertEquals(2, remote.challengeCalls)
        assertEquals(challenge.imageUrl, state.challengeImageUrl)
        assertEquals("Sign in failed. Check your details and the new security code.", state.errorMessage)
    }

    @Test
    fun `logout clears remote session and returns to signed out`() = runTest {
        val remote = FakeAuthRemoteDataSource(challenge)
        val repository = NetworkAuthRepository(remote)
        repository.refreshChallenge(stationId)
        repository.signIn(stationId, "Listener", "secret", "654321")

        repository.signOut(stationId)

        assertEquals(1, remote.signOutCalls)
        assertEquals(AuthStatus.SignedOut, repository.observeAuth(stationId).first().status)
    }

    private class FakeAuthRemoteDataSource(
        private val challenge: LoginChallenge,
        private val failSignIn: Boolean = false,
    ) : AuthRemoteDataSource {
        var challengeCalls = 0
        var signOutCalls = 0
        var persistCalls = 0
        var submittedInputs: List<String>? = null

        override suspend fun fetchChallenge(stationId: StationId): LoginChallenge {
            challengeCalls++
            return challenge
        }

        override suspend fun signIn(
            stationId: StationId,
            challenge: LoginChallenge,
            username: String,
            password: String,
            securityCode: String,
        ): AuthenticatedPage {
            submittedInputs = listOf(username, password, securityCode)
            if (failSignIn) throw IOException("sanitized")
            return AuthenticatedPage(
                "<p>Welcome, Listener.</p><a href='/modules.php?name=Your_Account&op=logout'>Logout</a>",
                "https://streamingsoundtracks.com/",
            )
        }

        override suspend fun signOut(stationId: StationId) {
            signOutCalls++
        }

        override fun persistSession(stationId: StationId) {
            persistCalls++
        }
    }
}
