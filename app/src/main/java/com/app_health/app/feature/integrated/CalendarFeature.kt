package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app_health.app.core.network.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.LocalDate
import java.time.YearMonth

fun calendarFeature()=AppFeature("calendar","Lịch sử theo ngày"){vm,navigate ->CalendarScreen(vm,navigate)}

@Composable
fun CalendarScreen(vm:AppViewModel,navigate:(String)->Unit) {
    val state by vm.state.collectAsState()
    var monthText by rememberSaveable{mutableStateOf(YearMonth.from(state.date).toString())}
    val month=YearMonth.parse(monthText)
    var rows by remember(month){mutableStateOf<List<CalendarDto>>(emptyList())}
    var loading by remember{mutableStateOf(false)}
    LaunchedEffect(month,state.loading,state.busy,state.readRevision,state.route) {
        if(state.loading || state.busy || state.route!="calendar")return@LaunchedEffect
        val ctx=vm.context()
        loading=true
        try{val result=vm.api.calendar(ctx.owner,month.toString());if(vm.valid(ctx))rows=result}
        catch(e:CancellationException){throw e}
        catch(e:Exception){currentCoroutineContext().ensureActive();vm.showError(e.toUserMessage(),ctx)}
        finally{loading=false}
    }
    val enabled=!state.busy && !state.uncertain
    Page {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(enabled=enabled,onClick={monthText=month.minusMonths(1).toString()}){Text("‹")}
            Text(month.toString(),style=MaterialTheme.typography.headlineSmall)
            TextButton(enabled=enabled && month<YearMonth.now(),onClick={monthText=month.plusMonths(1).toString()}){Text("›")}
        }
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth()){listOf("T2","T3","T4","T5","T6","T7","CN").forEach{Text(it,Modifier.weight(1f))}}
        val offset=month.atDay(1).dayOfWeek.value-1
        val cells=List(offset){null}+List(month.lengthOfMonth()){month.atDay(it+1)}
        cells.chunked(7).forEach{week ->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
            week.forEach{date ->Box(Modifier.weight(1f).height(70.dp)) {
                if(date!=null){val record=rows.firstOrNull{it.date==date.toString()}
                    val color=when(record?.status){"over"->Color(0xFFFFDAD6);"on_target"->Color(0xFFB7EFD5);"under"->Color(0xFFE1F3E8);"logged"->Color(0xFFDCEBFF);else->MaterialTheme.colorScheme.surfaceVariant}
                    FilledTonalButton(enabled=enabled && !loading && date<=LocalDate.now(),onClick={vm.chooseDate(date);navigate("diary")},
                        modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=color)) {
                        Column {
                            Text(date.dayOfMonth.toString())
                            if(record?.has_diary==true)Text("✎",style=MaterialTheme.typography.labelSmall)
                            if(record?.weight_kg!=null)Text("●",style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }}
            repeat(7-week.size){Spacer(Modifier.weight(1f))}
        }}
        Text("Xanh: có dữ liệu calo • Đỏ: trên mục tiêu • Lam: ghi nhận khác • ✎ ghi chú • ● cân nặng")
        Button(enabled=enabled,onClick={vm.chooseDate(LocalDate.now());navigate("diary")}){Text("Về hôm nay")}
    }
}
