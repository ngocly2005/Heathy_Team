package com.app_health.app.feature.integrated

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app_health.app.core.network.ApiFailure
import kotlinx.coroutines.CancellationException

fun chatFeature() = AppFeature("chat", "Tư vấn bữa ăn", load = { vm, ctx ->
    val messages = vm.api.chatHistory(ctx.owner)
    vm.publish(ctx) { it.copy(chat = messages) }
}) { vm, navigate -> ChatScreen(vm, navigate) }

@Composable
fun MessageBubble(text: String, user: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Surface(color = if (user) Color(0xFFD8F7EC) else Color.White,
            shape = RoundedCornerShape(19.dp), modifier = Modifier.widthIn(max = 330.dp),
            shadowElevation = if (user) 0.dp else 2.dp) {
            SelectionContainer { Text(text, Modifier.padding(14.dp), color = Ink) }
        }
    }
}

/** The answer and every meal detail are one assistant message in the scrolling history. */
@Composable
internal fun AssistantMealBubble(answer: ChatAnswerDto) {
    val details = chatMealDetails(answer)
    val mainText: String? = answer.answer
    val disclaimer: String? = answer.disclaimer
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(color = Color(0xFFEAF7F3), shape = RoundedCornerShape(20.dp),
            modifier = Modifier.widthIn(max = 330.dp), shadowElevation = 3.dp) {
            SelectionContainer {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!mainText.isNullOrBlank()) Text(mainText, color = Ink)
                    if (details.isNotEmpty()) {
                        Text("Gợi ý bữa ăn", color = Mint, fontWeight = FontWeight.Bold)
                        details.forEachIndexed { index, meal ->
                            if (index > 0) HorizontalDivider(color = Border)
                            Text(meal, color = Ink)
                        }
                    }
                    if (!disclaimer.isNullOrBlank()) Text(disclaimer, style = MaterialTheme.typography.bodySmall,
                        color = Muted)
                }
            }
        }
    }
}

@Composable
fun ChatScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var question by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    var observedLastId by remember { mutableStateOf<Long?>(null) }
    var readingOlder by remember(state.readRevision) { mutableStateOf(false) }
    var olderEnded by remember(state.readRevision) { mutableStateOf(false) }
    val newestAnswer = state.chat.lastOrNull { it.response != null }?.response
    val followUps = chatFollowUpQuestions(newestAnswer)
    val enabled = !state.busy && !state.loading && !state.uncertain && state.profile != null
    val hasOlderHeader = state.chat.size >= 50 && !olderEnded
    val pending = state.busy && vm.pendingChat != null
    val lastId = state.chat.lastOrNull()?.id

    // Prepending older rows does not change lastId, so it does not jump back to the latest reply.
    LaunchedEffect(lastId, pending) {
        if (pending || shouldScrollToNewestChat(observedLastId, lastId)) {
            list.animateScrollToItem(newestChatItemIndex(state.chat.size, hasOlderHeader, pending))
        }
        observedLastId = lastId
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF6FAFD)).imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = Color.White, shadowElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Tư vấn bữa ăn", color = Ink, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text("Tư vấn theo ngày ${uiDate(state.date.toString())}", color = Muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.profile == null) {
            Box(Modifier.padding(horizontal = 12.dp)) { HealthWarningPanel(listOf("Bạn cần nhập hồ sơ thể chất (tuổi, chiều cao, cân nặng và mức vận động) trước khi chat để AI đưa ra gợi ý phù hợp. Sau khi lưu hồ sơ, quay lại đây để đặt câu hỏi.")) }
            TextButton(enabled = !state.busy, onClick = { navigate("profile") }) { Text("Đi tới hồ sơ thể chất") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), state = list,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hasOlderHeader) item(key = "older-history") {
                TextButton(enabled = enabled && !readingOlder, onClick = {
                    if (!readingOlder) {
                        readingOlder = true
                        vm.read { ctx ->
                            try {
                                val older = vm.api.chatHistory(ctx.owner, state.chat.firstOrNull()?.id)
                                if (vm.valid(ctx)) {
                                    olderEnded = older.size < 50
                                    vm.readSucceeded(ctx) { it.copy(chat = (older + it.chat).distinctBy { row -> row.id }.sortedBy { row -> row.id }) }
                                }
                            } finally { readingOlder = false }
                        }
                    }
                }) { Text(if (readingOlder) "Đang tải lịch sử…" else "Tải tin nhắn cũ hơn") }
            }
            if (state.chat.isEmpty() && !pending) item(key = "chat-empty") {
                Text(if (state.loading) "Đang tải cuộc trò chuyện…" else "Đặt câu hỏi hoặc chọn một câu hỏi tiếp bên dưới.")
            }
            items(state.chat, key = { it.id }) { message ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    message.question?.let { MessageBubble(it, true) }
                    message.response?.let { AssistantMealBubble(it) }
                        ?: Text("Chưa có câu trả lời hợp lệ${message.error_code?.let { " (HTTP $it)" } ?: ""}.",
                            color = MaterialTheme.colorScheme.error)
                }
            }
            if (pending) item(key = "pending-chat") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.pendingChat?.let { MessageBubble(it, true) }
                    Text("Đang chờ AI trả lời…", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("Bạn có thể hỏi tiếp", Modifier.padding(horizontal = 14.dp), color = Ink,
            style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            followUps.forEach { prompt ->
                AssistChip(enabled = enabled, onClick = { question = prompt }, label = { Text(prompt) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color.White, labelColor = Ink))
            }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(question, { question = it.take(1000) }, label = { Text("Nhắn tin tư vấn") },
                modifier = Modifier.weight(1f), maxLines = 4, enabled = !state.busy,
                shape = RoundedCornerShape(16.dp))
            Button(enabled = enabled && question.trim().length >= 2, onClick = {
                val submitted = question.trim()
                vm.pendingChat = submitted
                var baselineId = 0L
                vm.write(success = { question = "" }, reconcile = { ctx ->
                    vm.api.chatHistory(ctx.owner).any { it.id > baselineId && it.question == submitted }
                }) { ctx ->
                    try {
                        baselineId = try { vm.api.chatHistory(ctx.owner).maxOfOrNull { it.id } ?: 0L }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { throw ApiFailure(503, "Chưa kiểm tra được lịch sử chat. Hãy thử lại.") }
                        vm.api.chat(ctx.owner, mapOf("question" to submitted, "date" to ctx.date))
                    }
                    finally { vm.pendingChat = null }
                }
            }) { Text("Gửi") }
        }
    }
}
