package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.MAX_PRIVATE_MESSAGE_BODY_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.MAX_PRIVATE_MESSAGE_SUBJECT_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.PrivateMessage
import com.codeframe78.twentyfourseven.player.domain.PrivateMessageFolder
import com.codeframe78.twentyfourseven.player.domain.StationId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal class PrivateMessagesSignInRequiredException : IOException("Station sign-in is required")

internal enum class PrivateMessageSendOutcome { Sent, Unconfirmed }

internal interface PrivateMessagesRemoteDataSource {
    suspend fun loadFolder(stationId: StationId, folder: PrivateMessageFolder, page: Int): PrivateMessagesPage
    suspend fun loadMessage(stationId: StationId, folder: PrivateMessageFolder, messageId: String): PrivateMessage
    suspend fun replyForm(stationId: StationId, messageId: String): PrivateMessageForm?
    suspend fun messageForm(stationId: StationId, recipient: String): PrivateMessageForm?
    suspend fun send(
        stationId: StationId,
        form: PrivateMessageForm,
        subject: String,
        body: String,
    ): PrivateMessageSendOutcome
}

internal class StationPrivateMessagesRemoteDataSource(
    private val pages: StationPages,
    private val parser: PrivateMessagesPageParser = PrivateMessagesPageParser(),
    private val sessionEvidence: AuthLoginResultParser = AuthLoginResultParser(),
) : PrivateMessagesRemoteDataSource {
    override suspend fun loadFolder(
        stationId: StationId,
        folder: PrivateMessageFolder,
        page: Int,
    ): PrivateMessagesPage = withContext(Dispatchers.IO) {
        require(page in 1..MAX_PAGE)
        val body = memberPage(stationId, "$MODULE_PATH&folder=${folder.key}&p=$page")
        parser.parseFolder(body, pages.origin(stationId))
    }

    override suspend fun loadMessage(
        stationId: StationId,
        folder: PrivateMessageFolder,
        messageId: String,
    ): PrivateMessage = withContext(Dispatchers.IO) {
        requireMessageId(messageId)
        val body = memberPage(stationId, "$MODULE_PATH&folder=${folder.key}&mode=read&id=$messageId")
        parser.parseMessage(body, pages.origin(stationId), folder, messageId)
    }

    override suspend fun replyForm(stationId: StationId, messageId: String): PrivateMessageForm? =
        withContext(Dispatchers.IO) {
            requireMessageId(messageId)
            parser.parseForm(
                memberPage(stationId, "$MODULE_PATH&mode=post&reply=1&id=$messageId"),
                pages.origin(stationId),
            )
        }

    override suspend fun messageForm(stationId: StationId, recipient: String): PrivateMessageForm? =
        withContext(Dispatchers.IO) {
            val name = recipient.trim()
            if (name.isEmpty() || name.length > MAX_RECIPIENT_CHARACTERS) return@withContext null
            val origin = pages.origin(stationId)
            // The station addresses a message by member number, which only the member's profile page gives.
            val profile = memberPage(
                stationId,
                "$PROFILE_PATH${URLEncoder.encode(name, StandardCharsets.UTF_8.name())}",
            )
            val memberNumber = parser.parseProfileMemberNumber(profile, origin) ?: return@withContext null
            parser.parseForm(memberPage(stationId, "$MODULE_PATH&file=index&mode=post&u=$memberNumber"), origin)
                ?.takeIf { it.recipient.equals(name, ignoreCase = true) }
        }

    override suspend fun send(
        stationId: StationId,
        form: PrivateMessageForm,
        subject: String,
        body: String,
    ): PrivateMessageSendOutcome = withContext(Dispatchers.IO) {
        require(subject.isNotBlank() && subject.length <= MAX_PRIVATE_MESSAGE_SUBJECT_CHARACTERS)
        require(body.isNotBlank() && body.length <= MAX_PRIVATE_MESSAGE_BODY_CHARACTERS)
        if (!pages.hasSession(stationId)) throw PrivateMessagesSignInRequiredException()
        val origin = pages.origin(stationId)
        val response = try {
            pages.postForm(
                stationId,
                MODULE_PATH,
                // A saved copy is what lets the Sent folder confirm the message afterwards.
                listOf("subject" to subject, "message" to body, "savecopy" to form.saveCopyValue) + form.hiddenFields,
                PAGE_LIMIT,
            )
        } catch (failure: StationHttpException) {
            if (failure.status == HTTP_FORBIDDEN) throw PrivateMessagesSignInRequiredException()
            throw failure
        }
        if (sessionEvidence.showsSignedOutVisitor(response.body, origin)) {
            pages.expireSession(stationId)
            throw PrivateMessagesSignInRequiredException()
        }
        // The message is never sent twice. One read of the Sent folder asks the station whether it has it.
        val sent = runCatching {
            parser.parseFolder(memberPage(stationId, "$MODULE_PATH&folder=${PrivateMessageFolder.Sent.key}&p=1"), origin)
        }.getOrNull()
        val confirmed = sent?.messages?.take(CONFIRMATION_ROWS)?.any { message ->
            message.subject.trim() == subject.trim() && message.correspondent.equals(form.recipient, ignoreCase = true)
        } == true
        if (confirmed) PrivateMessageSendOutcome.Sent else PrivateMessageSendOutcome.Unconfirmed
    }

    private fun memberPage(stationId: StationId, path: String): String {
        if (!pages.hasSession(stationId)) throw PrivateMessagesSignInRequiredException()
        val response = try {
            pages.get(stationId, path, PAGE_LIMIT)
        } catch (failure: StationHttpException) {
            // The station answers a visitor with 403 for this members-only module.
            if (failure.status == HTTP_FORBIDDEN) throw PrivateMessagesSignInRequiredException()
            throw failure
        }
        if (sessionEvidence.showsSignedOutVisitor(response.body, pages.origin(stationId))) {
            pages.expireSession(stationId)
            throw PrivateMessagesSignInRequiredException()
        }
        return response.body
    }

    private fun requireMessageId(messageId: String) {
        if (!messageId.matches(MESSAGE_ID)) throw IOException("Message identifier was not recognized")
    }

    private companion object {
        const val MODULE_PATH = "/modules.php?name=Private_Messages"
        const val PROFILE_PATH = "/modules.php?name=Your_Account&op=userinfo&username="
        const val PAGE_LIMIT = 1_000_000
        const val MAX_PAGE = 10_000
        const val MAX_RECIPIENT_CHARACTERS = 60
        const val CONFIRMATION_ROWS = 3
        const val HTTP_FORBIDDEN = 403
        val MESSAGE_ID = Regex("\\d{1,12}")
    }
}
