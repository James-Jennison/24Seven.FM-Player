package com.codeframe78.twentyfourseven.player.data

import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI

internal sealed class LoginChallenge {
    abstract val actionUrl: String
    abstract val answerFieldName: String
    abstract val hiddenFields: List<LoginFormField>

    data class Image(
        override val actionUrl: String,
        val imageUrl: String,
        override val answerFieldName: String,
        override val hiddenFields: List<LoginFormField>,
    ) : LoginChallenge()

    data class Text(
        override val actionUrl: String,
        val prompt: String,
        override val answerFieldName: String,
        override val hiddenFields: List<LoginFormField>,
    ) : LoginChallenge()
}

internal data class LoginFormField(val name: String, val value: String)

internal class AuthLoginPageParser {
    fun parse(html: String, origin: String): LoginChallenge {
        val originUri = URI(origin)
        require(originUri.scheme == "https" && originUri.path == "/")
        val document = Jsoup.parse(html, origin)
        val form = document.select("form").firstOrNull { candidate ->
            candidate.selectFirst("input[name=username]") != null &&
                candidate.selectFirst("input[name=user_password]") != null
        } ?: throw IOException("Login form was not found")

        val operation = form.selectFirst("input[name=op]")?.attr("value")
        if (operation != "login") throw IOException("Login operation was not recognized")

        val action = sameOriginHttpsUrl(form.absUrl("action"), originUri, "login action")
        val hiddenFields = form.select("input[type=hidden][name]").map { input ->
            val name = input.attr("name")
            val value = input.attr("value")
            if (!name.matches(FIELD_NAME) || value.length > MAX_HIDDEN_FIELD_VALUE_LENGTH) {
                throw IOException("Login form included an invalid hidden field")
            }
            LoginFormField(name, value)
        }
        if (
            hiddenFields.map(LoginFormField::name).toSet().size != hiddenFields.size ||
            hiddenFields.any { it.name == "username" || it.name == "user_password" }
        ) {
            throw IOException("Login form included duplicate credential fields")
        }

        val imageAnswerField = form.selectFirst("input[name=gfx_check]")
        val imageElement = form.selectFirst("img[alt=Security Code], img[alt=Security Check]")
        if (imageAnswerField != null && imageElement != null) {
            val token = form.selectFirst("input[name=random_num]")?.attr("value").orEmpty()
            if (!token.matches(SIX_DIGIT_CHALLENGE)) throw IOException("Login challenge was not recognized")
            val image = sameOriginHttpsUrl(imageElement.absUrl("src"), originUri, "security code image")
            return LoginChallenge.Image(action, image, imageAnswerField.attr("name"), hiddenFields)
        }

        val antiSpamMatch = ANTI_SPAM_PROMPT.find(form.text().replace(Regex("\\s+"), " "))
            ?: throw IOException("Anti-spam check was not recognized")
        val textFields = form.select("input[name]").filter { input ->
            val type = input.attr("type").lowercase()
            val name = input.attr("name")
            (type.isEmpty() || type == "text") &&
                name != "username"
        }
        val answerField = textFields.firstOrNull { it.attr("name").matches(ANTI_SPAM_FIELD_NAME) }
            ?: textFields.singleOrNull()
            ?: throw IOException("Anti-spam answer field was not recognized")
        val answerFieldName = answerField.attr("name")
        if (!answerFieldName.matches(FIELD_NAME) || hiddenFields.any { it.name == answerFieldName }) {
            throw IOException("Login form included a duplicate anti-spam field")
        }
        val word = antiSpamMatch.groupValues[1]
        return LoginChallenge.Text(
            actionUrl = action,
            prompt = "Anti-spam check: Type the word “$word” below.",
            answerFieldName = answerFieldName,
            hiddenFields = hiddenFields,
        )
    }

    private fun sameOriginHttpsUrl(value: String, origin: URI, label: String): String {
        val uri = runCatching { URI(value) }.getOrNull()
            ?: throw IOException("Invalid $label")
        if (
            uri.scheme != "https" ||
            !uri.host.equals(origin.host, ignoreCase = true) ||
            uri.port != origin.port
        ) {
            throw IOException("Untrusted $label")
        }
        return uri.toASCIIString()
    }

    private companion object {
        val SIX_DIGIT_CHALLENGE = Regex("^[0-9]{6}$")
        val FIELD_NAME = Regex("^[A-Za-z][A-Za-z0-9_-]{0,63}$")
        val ANTI_SPAM_FIELD_NAME = Regex("(?i).*(anti|spam|check|answer|code).*")
        val ANTI_SPAM_PROMPT = Regex(
            "(?i)anti[- ]?spam check\\s*:\\s*type the word\\s+([A-Za-z0-9]{1,32})\\s+below",
        )
        const val MAX_HIDDEN_FIELD_VALUE_LENGTH = 4_096
    }
}
