package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AuthLoginResultParserTest {
    private val parser = AuthLoginResultParser()

    @Test
    fun `welcome identity and same-origin logout identify signed-in account`() {
        assertEquals(
            "Listener",
            parser.parseSignedInDisplayName(signedInPage(), "https://streamingsoundtracks.com/", "Listener"),
        )
    }

    @Test
    fun `remaining password field rejects response`() {
        assertThrows(IOException::class.java) {
            parser.parseSignedInDisplayName(
                signedInPage(extra = "<input name=user_password type=password>"),
                "https://streamingsoundtracks.com/",
                "Listener",
            )
        }
    }

    @Test
    fun `cross-origin logout is rejected`() {
        assertThrows(IOException::class.java) {
            parser.parseSignedInDisplayName(
                signedInPage(logout = "https://example.com/modules.php?name=Your_Account&op=logout"),
                "https://streamingsoundtracks.com/",
                "Listener",
            )
        }
    }

    @Test
    fun `unrelated account-derived links are ignored`() {
        assertEquals(
            "Listener",
            parser.parseSignedInDisplayName(
                signedInPage(extra = "<a href='/unrelated?account_derived=sensitive'>Other</a>"),
                "https://streamingsoundtracks.com/",
                "Listener",
            ),
        )
    }

    @Test
    fun `account menu label and same-origin logout identify signed-in account without a welcome line`() {
        assertEquals(
            "listener",
            parser.parseSignedInDisplayName(accountMenuPage(), "https://streamingsoundtracks.com/", "listener"),
        )
    }

    @Test
    fun `account menu naming another account is rejected`() {
        assertThrows(IOException::class.java) {
            parser.parseSignedInDisplayName(accountMenuPage(), "https://streamingsoundtracks.com/", "Someone")
        }
    }

    @Test
    fun `account menu page that still shows the login form is rejected`() {
        assertThrows(IOException::class.java) {
            parser.parseSignedInDisplayName(
                accountMenuPage(extra = "<input name=user_password type=password>"),
                "https://streamingsoundtracks.com/",
                "Listener",
            )
        }
    }

    @Test
    fun `same-origin logout without any account name still identifies a signed-in page`() {
        assertEquals(
            "Listener",
            parser.parseSignedInDisplayName(
                "<html><body><a href='/modules.php?name=Your_Account&op=logout'>Logout</a></body></html>",
                "https://streamingsoundtracks.com/",
                "Listener",
            ),
        )
    }

    @Test
    fun `page without a same-origin logout is rejected`() {
        assertThrows(IOException::class.java) {
            parser.parseSignedInDisplayName(
                "<html><body><p>Welcome, Listener.</p></body></html>",
                "https://streamingsoundtracks.com/",
                "Listener",
            )
        }
    }

    @Test
    fun `session evidence separates signed-out pages from pages that say nothing`() {
        val origin = "https://streamingsoundtracks.com/"
        val visitor = "<nav><a href='modules.php?name=Your_Account'>Login</a>" +
            "<a href='modules.php?name=Your_Account&op=new_user'>Register</a></nav>"

        assertEquals(SignedInEvidence.Confirmed, parser.signedInEvidence(accountMenuPage(), origin, "Listener"))
        assertEquals(SignedInEvidence.SignedOut, parser.signedInEvidence(accountMenuPage(), origin, "Someone"))
        assertEquals(SignedInEvidence.SignedOut, parser.signedInEvidence(visitor, origin, "Listener"))
        assertEquals(
            SignedInEvidence.SignedOut,
            parser.signedInEvidence("<input name=user_password type=password>", origin, "Listener"),
        )
        assertEquals(
            SignedInEvidence.Unknown,
            parser.signedInEvidence("<p>The station is being upgraded.</p>", origin, "Listener"),
        )
        assertTrue(parser.showsSignedOutVisitor(visitor, origin))
        assertFalse(parser.showsSignedOutVisitor("<p>The station is being upgraded.</p>", origin))
        assertFalse(parser.showsSignedOutVisitor(accountMenuPage(), origin))
    }

    // Mirrors the station navigation observed on October 1, 2026: the account menu is labelled with the member
    // name, the signed-in landing page is the member profile, and no welcome line is present.
    private fun accountMenuPage(extra: String = "") = """
        <html><head><title>Listener - User Profile |</title></head><body class="station-sst">
          <nav class="modern-nav"><ul>
            <li>
              <a href="modules.php?name=Your_Account" target="_self">
                <span id="auth-dot">🟢</span> Listener
                <span class="pm-badge">0</span>
              </a>
              <ul>
                <li><a href="modules.php?name=Your_Account&amp;op=logout">🚪 Logout</a></li>
                <li><a href="modules.php?name=Your_Account">👤 Your Account</a></li>
              </ul>
            </li>
            <li><a href="modules.php?name=Forums">💬 Forums</a></li>
          </ul></nav>
          <form action="modules.php" method="get">
            <input type="hidden" name="name" value="Forums">
            <input type="text" name="username"><input type="submit" value="Search">
          </form>
          $extra
        </body></html>
    """.trimIndent()

    private fun signedInPage(
        logout: String = "/modules.php?name=Your_Account&op=logout",
        extra: String = "",
    ) = """
        <html><body>
          <p>Welcome, Listener.</p>
          <a href="$logout">Logout</a>
          $extra
        </body></html>
    """.trimIndent()
}
