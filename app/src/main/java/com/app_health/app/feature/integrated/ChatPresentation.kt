package com.app_health.app.feature.integrated

/** All meal details belong to their assistant message, including legacy history records. */
internal fun chatMealDetails(answer: ChatAnswerDto): List<String> {
    val mainText: String? = answer.answer
    return answer.suggestions.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
        .distinct().filterNot { mainText.orEmpty().contains(it) }
}

/** Never turn meal descriptions from the legacy suggestions field into user questions. */
internal fun chatFollowUpQuestions(answer: ChatAnswerDto?): List<String> =
    answer?.follow_up_questions.orEmpty().map { it.trim() }
        .filter { it.length in 2..120 }.distinct().take(4)
        .ifEmpty { listOf("Chi tiết bữa trưa", "Đổi sang món chay") }

/** Stable message keys preserve position when older history is prepended. */
internal fun shouldScrollToNewestChat(previousLastId: Long?, newLastId: Long?): Boolean =
    newLastId != null && previousLastId != newLastId

internal fun newestChatItemIndex(messageCount: Int, hasOlderHeader: Boolean, pending: Boolean): Int =
    ((if (hasOlderHeader) 1 else 0) + messageCount - (if (pending) 0 else 1)).coerceAtLeast(0)
