package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun chatFeature()=AppFeature("chat","Tư vấn bữa ăn",load={vm,ctx ->
    val messages=vm.api.chatHistory(ctx.owner)
    vm.publish(ctx){it.copy(chat=messages)}
}){vm,navigate ->ChatScreen(vm,navigate)}

@Composable
fun MessageBubble(text:String,user:Boolean) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=if(user)Arrangement.End else Arrangement.Start) {
        Surface(color=if(user)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape=MaterialTheme.shapes.large,modifier=Modifier.widthIn(max=330.dp)) {
            Text(text,Modifier.padding(14.dp))
        }
    }
}

@Composable
fun ChatScreen(vm:AppViewModel,navigate:(String)->Unit) {
    val state by vm.state.collectAsState()
    var question by rememberSaveable{mutableStateOf("")}
    val list=rememberLazyListState()
    val suggestions=state.chat.lastOrNull{it.response!=null}?.response?.suggestions?.take(4)
        ?:listOf("Gợi ý bữa tối hôm nay","Tôi có thể ăn gì cho đủ protein?")
    val enabled=!state.busy && !state.loading && !state.uncertain && state.profile!=null
    LaunchedEffect(state.chat.size,state.busy){if(state.chat.isNotEmpty())list.animateScrollToItem((state.chat.size-1).coerceAtLeast(0))}
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Tư vấn theo ngày ${state.date}")
        if(state.profile==null) TextButton(onClick={navigate("profile")}){Text("Nhập hồ sơ để nhận tư vấn")}
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),state=list,verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(state.chat.size>=50) item {
                TextButton(enabled=!state.busy,onClick={vm.read{ctx ->
                    val older=vm.api.chatHistory(ctx.owner,state.chat.firstOrNull()?.id)
                    vm.publish(ctx){it.copy(chat=(older+it.chat).distinctBy{row ->row.id})}
                }}){Text("Tải tin nhắn cũ hơn")}
            }
            if(state.chat.isEmpty())item{Text("Đặt câu hỏi hoặc chọn một gợi ý bên dưới.")}
            items(state.chat,key={it.id}){message ->Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                message.question?.let{MessageBubble(it,true)}
                message.response?.let{answer ->
                    MessageBubble(answer.answer,false)
                    Text(answer.disclaimer,style=MaterialTheme.typography.bodySmall)
                } ?: Text("Chưa có câu trả lời hợp lệ${message.error_code?.let{" (HTTP $it)"} ?: ""}.",color=MaterialTheme.colorScheme.error)
            }}
            if(state.busy && vm.pendingChat!=null)item{Text("Đang gửi câu hỏi và chờ trả lời…")}
        }
        suggestions.forEach{suggestion ->OutlinedButton(enabled=enabled,onClick={question=suggestion.take(1000)},modifier=Modifier.fillMaxWidth()){Text(suggestion,maxLines=2)}}
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(question,{question=it.take(1000)},label={Text("Nhắn tin tư vấn")},modifier=Modifier.weight(1f),maxLines=4,enabled=!state.busy)
            Button(enabled=enabled && question.trim().length>=2,onClick={
                val submitted=question.trim()
                vm.pendingChat=submitted
                vm.write(success={question=""}){ctx ->
                    try { vm.api.chat(ctx.owner,mapOf("question" to submitted,"date" to ctx.date)) }
                    finally {vm.pendingChat=null}
                }
            }){Text("Gửi")}
        }
    }
}
