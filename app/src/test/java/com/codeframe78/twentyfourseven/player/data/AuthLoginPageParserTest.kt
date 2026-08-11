package com.codeframe78.twentyfourseven.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class AuthLoginPageParserTest {
    private val parser = AuthLoginPageParser()

    @Test
    fun `verified form produces same-origin challenge`() {
        val challenge = parser.parse(loginPage(), "https://adagio.fm/") as LoginChallenge.Image

        assertEquals("https://adagio.fm/modules.php?name=Your_Account", challenge.actionUrl)
        assertEquals(
            "https://adagio.fm/modules.php?name=Your_Account&gfx=gfx&random_num=123456",
            challenge.imageUrl,
        )
        assertEquals("gfx_check", challenge.answerFieldName)
        assertEquals(
            listOf(LoginFormField("random_num", "123456"), LoginFormField("op", "login")),
            challenge.hiddenFields,
        )
    }

    @Test
    fun `cross-origin security image is rejected`() {
        assertThrows(IOException::class.java) {
            parser.parse(loginPage(imageUrl = "https://example.com/code.png"), "https://adagio.fm/")
        }
    }

    @Test
    fun `unexpected challenge format is rejected`() {
        assertThrows(IOException::class.java) {
            parser.parse(loginPage(token = "not-a-code"), "https://adagio.fm/")
        }
    }

    @Test
    fun `unexpected operation is rejected`() {
        assertThrows(IOException::class.java) {
            parser.parse(loginPage(operation = "register"), "https://adagio.fm/")
        }
    }

    @Test
    fun `text anti-spam form produces native prompt and verified answer field`() {
        val challenge = parser.parse(textAntiSpamLoginPage(), "https://adagio.fm/") as LoginChallenge.Text

        assertEquals("https://adagio.fm/modules.php?name=Your_Account", challenge.actionUrl)
        assertEquals("Anti-spam check: Type the word “sound” below.", challenge.prompt)
        assertEquals("anti_spam_answer", challenge.answerFieldName)
        assertEquals(listOf(LoginFormField("op", "login")), challenge.hiddenFields)
    }

    @Test
    fun `text anti-spam form accepts a sole answer field with an unfamiliar name`() {
        val challenge = parser.parse(textAntiSpamLoginPage(answerFieldName = "captcha"), "https://adagio.fm/") as LoginChallenge.Text

        assertEquals("captcha", challenge.answerFieldName)
    }

    @Test
    fun `text anti-spam form keeps the legacy answer field name without requiring an image`() {
        val challenge = parser.parse(
            textAntiSpamLoginPage(answerFieldName = "gfx_check", token = "sound"),
            "https://adagio.fm/",
        ) as LoginChallenge.Text

        assertEquals("gfx_check", challenge.answerFieldName)
        assertEquals(
            listOf(LoginFormField("random_num", "sound"), LoginFormField("op", "login")),
            challenge.hiddenFields,
        )
    }

    private fun loginPage(
        token: String = "123456",
        operation: String = "login",
        imageUrl: String = "/modules.php?name=Your_Account&gfx=gfx&random_num=$token",
    ) = """
        <form method="post" action="/modules.php?name=Your_Account">
          <input name="username" type="text">
          <input name="user_password" type="password">
          <input name="gfx_check" type="text">
          <input name="random_num" type="hidden" value="$token">
          <input name="op" type="hidden" value="$operation">
          <img src="$imageUrl" alt="Security Code">
        </form>
    """.trimIndent()

    private fun textAntiSpamLoginPage(
        answerFieldName: String = "anti_spam_answer",
        token: String? = null,
    ) = """
        <form method="post" action="/modules.php?name=Your_Account">
          <label>Nickname: <input name="username" type="text"></label>
          <label>Password: <input name="user_password" type="password"></label>
          <p>Anti-Spam Check: Type the word <strong>sound</strong> below:</p>
          <input name="$answerFieldName" type="text">
          ${token?.let { "<input name=\"random_num\" type=\"hidden\" value=\"$it\">" }.orEmpty()}
          <input name="op" type="hidden" value="login">
        </form>
    """.trimIndent()
}
