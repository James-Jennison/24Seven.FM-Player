package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AuthStatus
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class NetworkAuthRepositoryTest {
    private val stationId = StationId("sst")
    private val challenge = LoginChallenge.Image(
        actionUrl = "https://streamingsoundtracks.com/modules.php?name=Your_Account",
        imageUrl = "https://streamingsoundtracks.com/security-code.png",
        answerFieldName = "gfx_check",
        hiddenFields = listOf(LoginFormField("random_num", "123456"), LoginFormField("op", "login")),
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
        assertEquals("Sign in failed. Check your details and the new anti-spam check.", state.errorMessage)
    }

    @Test
    fun `text anti-spam challenge is exposed without an image`() = runTest {
        val textChallenge = LoginChallenge.Text(
            actionUrl = "https://streamingsoundtracks.com/modules.php?name=Your_Account",
            prompt = "Anti-spam check: Type the word “sound” below.",
            answerFieldName = "anti_spam_answer",
            hiddenFields = listOf(LoginFormField("op", "login")),
        )
        val repository = NetworkAuthRepository(FakeAuthRemoteDataSource(textChallenge))

        repository.refreshChallenge(stationId)

        val state = repository.observeAuth(stationId).first()
        assertNull(state.challengeImageUrl)
        assertEquals("Anti-spam check: Type the word “sound” below.", state.antiSpamPrompt)
    }

    @Test
    fun `logout clears remote session and returns to signed out`() = runTest {
        val remote = FakeAuthRemoteDataSource(challenge)
        val repository = NetworkAuthRepository(remote)
        repository.refreshChallenge(stationId)
        repository.signIn(stationId, "Listener", "secret", "654321")

        repository.signOut(stationId)

        assertEquals(listOf(stationId), remote.signedOutStations)
        assertEquals(AuthStatus.SignedOut, repository.observeAuth(stationId).first().status)
    }

    @Test
    fun `stored identity restores signed-in state without credentials`() = runTest {
        val repository = NetworkAuthRepository(
            FakeAuthRemoteDataSource(
                challenge,
                restoredSessions = mapOf(stationId to RestoredAuthSession.SignedIn("Listener")),
            ),
        )

        repository.restoreSession(stationId)

        val state = repository.observeAuth(stationId).first()
        assertEquals(AuthStatus.SignedIn, state.status)
        assertEquals("Listener", state.displayName)
    }

    @Test
    fun `expired restored session is explicit and isolated from another station`() = runTest {
        val adagio = StationId("afm")
        val repository = NetworkAuthRepository(
            FakeAuthRemoteDataSource(
                challenge,
                restoredSessions = mapOf(
                    stationId to RestoredAuthSession.Expired,
                    adagio to RestoredAuthSession.SignedIn("Adagio listener"),
                ),
            ),
        )

        repository.restoreSession(stationId)
        repository.restoreSession(adagio)

        val expired = repository.observeAuth(stationId).first()
        val signedIn = repository.observeAuth(adagio).first()
        assertEquals(AuthStatus.Expired, expired.status)
        assertEquals("Your saved station session expired. Sign in again.", expired.errorMessage)
        assertEquals(AuthStatus.SignedIn, signedIn.status)
        assertEquals("Adagio listener", signedIn.displayName)
    }

    @Test
    fun `signing out one station preserves another restored station`() = runTest {
        val adagio = StationId("afm")
        val remote = FakeAuthRemoteDataSource(
            challenge,
            restoredSessions = mapOf(
                stationId to RestoredAuthSession.SignedIn("SST listener"),
                adagio to RestoredAuthSession.SignedIn("Adagio listener"),
            ),
        )
        val repository = NetworkAuthRepository(remote)
        repository.restoreSession(stationId)
        repository.restoreSession(adagio)

        repository.signOut(stationId)

        assertEquals(AuthStatus.SignedOut, repository.observeAuth(stationId).first().status)
        assertEquals(AuthStatus.SignedIn, repository.observeAuth(adagio).first().status)
        assertEquals("Adagio listener", repository.observeAuth(adagio).first().displayName)
        assertEquals(listOf(stationId), remote.signedOutStations)
    }

    @Test
    fun `runtime session invalidation becomes explicit without changing another station`() = runTest {
        val adagio = StationId("afm")
        val remote = FakeAuthRemoteDataSource(
            challenge,
            restoredSessions = mapOf(
                stationId to RestoredAuthSession.SignedIn("SST listener"),
                adagio to RestoredAuthSession.SignedIn("Adagio listener"),
            ),
        )
        val repository = NetworkAuthRepository(remote)
        repository.restoreSession(stationId)
        repository.restoreSession(adagio)

        remote.emitValidity(stationId, ProtectedSessionValidity.Expired)

        assertEquals(AuthStatus.Expired, repository.observeAuth(stationId).first().status)
        assertEquals(AuthStatus.SignedIn, repository.observeAuth(adagio).first().status)
    }

    private class FakeAuthRemoteDataSource(
        private val challenge: LoginChallenge,
        private val failSignIn: Boolean = false,
        private val restoredSessions: Map<StationId, RestoredAuthSession> = emptyMap(),
    ) : AuthRemoteDataSource {
        var challengeCalls = 0
        val signedOutStations = mutableListOf<StationId>()
        var persistCalls = 0
        var submittedInputs: List<String>? = null
        private val validities = mutableMapOf<StationId, MutableStateFlow<ProtectedSessionValidity>>()

        override fun observeSessionValidity(stationId: StationId): Flow<ProtectedSessionValidity> =
            validity(stationId)

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
            signedOutStations += stationId
        }

        override suspend fun restoredSession(stationId: StationId): RestoredAuthSession =
            restoredSessions[stationId] ?: RestoredAuthSession.None

        override fun persistSession(stationId: StationId, displayName: String) {
            persistCalls++
            validity(stationId).value = ProtectedSessionValidity.Active
        }

        fun emitValidity(stationId: StationId, value: ProtectedSessionValidity) {
            validity(stationId).value = value
        }

        private fun validity(stationId: StationId) = validities.getOrPut(stationId) {
            MutableStateFlow(ProtectedSessionValidity.Unknown)
        }
    }
}
