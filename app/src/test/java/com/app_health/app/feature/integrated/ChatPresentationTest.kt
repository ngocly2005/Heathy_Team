package com.app_health.app.feature.integrated

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ChatPresentationTest {
    private val gson = Gson()

    @Test fun legacyHistoryKeepsEveryMealWithoutTruncation() {
        val meals = (1..6).map { "Bữa $it: " + "Chi tiết khẩu phần và dinh dưỡng. ".repeat(50) }
        val json = gson.toJson(mapOf("answer" to "Thực đơn của bạn", "suggestions" to meals, "disclaimer" to "Ước tính"))
        val answer = gson.fromJson(json, ChatAnswerDto::class.java)
        assertNull(answer.follow_up_questions)
        // Presentation trims surrounding whitespace but must keep every complete meal body.
        assertEquals(meals.map { it.trim() }, chatMealDetails(answer))
        assertTrue(chatMealDetails(answer).last().length > 1000)
        assertEquals(listOf("Chi tiết bữa trưa", "Đổi sang món chay"), chatFollowUpQuestions(answer))
    }

    @Test fun newContractSeparatesMealContentFromFollowUpPrompts() {
        val answer = gson.fromJson("""{"answer":"Bạn có thể tham khảo:","suggestions":["Bữa trưa: 150 g thịt, 100 g cơm, ước tính 500 kcal"],"disclaimer":"Ước tính","follow_up_questions":["Chi tiết bữa trưa","Đổi sang món chay"]}""", ChatAnswerDto::class.java)
        assertEquals(listOf("Bữa trưa: 150 g thịt, 100 g cơm, ước tính 500 kcal"), chatMealDetails(answer))
        assertEquals(listOf("Chi tiết bữa trưa", "Đổi sang món chay"), chatFollowUpQuestions(answer))
        assertFalse(chatFollowUpQuestions(answer).contains(answer.suggestions.single()))
    }

    @Test fun mealAlreadyFullyPresentInAnswerIsNotRepeated() {
        val meal = "Bữa trưa: cơm và rau, ước tính 500 kcal"
        val answer = ChatAnswerDto("Thực đơn:\n$meal", listOf(meal, "Bữa tối: súp"), "Ước tính")
        assertEquals(listOf("Bữa tối: súp"), chatMealDetails(answer))
    }

    @Test fun emptyAndInvalidFollowUpsUseShortEditableFallbacks() {
        val answer = ChatAnswerDto("", listOf("Bữa trưa chứa nội dung món ăn"), "", listOf(" ", "x".repeat(121)))
        assertEquals(listOf("Chi tiết bữa trưa", "Đổi sang món chay"), chatFollowUpQuestions(answer))
        assertEquals(chatFollowUpQuestions(answer), chatFollowUpQuestions(null))
    }

    @Test fun followUpsAreDistinctAndShortWithoutModifyingMealDetails() {
        val answer = ChatAnswerDto("", listOf("Bữa ăn đầy đủ"), "",
            listOf("  Chi tiết bữa trưa  ", "Chi tiết bữa trưa", "Món chay?", "Bữa tối?", "Đổi khẩu phần?", "Protein?"))
        assertEquals(listOf("Chi tiết bữa trưa", "Món chay?", "Bữa tối?", "Đổi khẩu phần?"), chatFollowUpQuestions(answer))
        assertEquals(listOf("Bữa ăn đầy đủ"), answer.suggestions)
    }

    @Test fun prependingOlderMessagesDoesNotTriggerScrollToNewest() {
        assertTrue(shouldScrollToNewestChat(null, 100L))
        assertFalse(shouldScrollToNewestChat(100L, 100L))
        assertTrue(shouldScrollToNewestChat(100L, 101L))
        assertFalse(shouldScrollToNewestChat(100L, null))
    }

    @Test fun scrollTargetAccountsForOlderHistoryHeaderAndPendingMessage() {
        assertEquals(0, newestChatItemIndex(1, false, false))
        assertEquals(50, newestChatItemIndex(50, true, false))
        assertEquals(51, newestChatItemIndex(50, true, true))
        assertEquals(0, newestChatItemIndex(0, false, true))
    }
}
