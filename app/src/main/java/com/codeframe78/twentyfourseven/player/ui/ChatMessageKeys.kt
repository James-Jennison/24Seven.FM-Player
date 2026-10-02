package com.codeframe78.twentyfourseven.player.ui

import com.codeframe78.twentyfourseven.player.domain.ChatMessage

/**
 * List keys for a newest-first chat page that stay the same when newer messages arrive.
 *
 * A key built from the list position changes for every row on each new message, which closes an open message menu
 * and moves the reading position. Identical messages are told apart by counting from the oldest one.
 */
internal fun chatMessageKeys(messages: List<ChatMessage>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return messages.asReversed().map { message ->
        val identity = "${message.postedAtLabel}-${message.authorDisplayName}-${message.messageText}"
        val occurrence = seen.merge(identity, 1, Int::plus)
        "$identity#$occurrence"
    }.asReversed()
}
