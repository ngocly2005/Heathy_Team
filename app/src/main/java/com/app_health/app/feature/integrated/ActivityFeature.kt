package com.app_health.app.feature.integrated

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

fun activityFeature() = AppFeature("activity", "Vận động",load={vm,ctx ->
    val activities=vm.api.activities(ctx.owner)
    vm.publish(ctx){it.copy(activities=activities)}
}) {vm,navigate -> ActivityScreen(vm,navigate)}

@Composable
fun ActivityScreen(vm: AppViewModel,navigate:(String)->Unit) {
    val state by vm.state.collectAsState()
    var selected by rememberSaveable {mutableStateOf("")}
    var minutes by rememberSaveable {mutableStateOf("30")}
    val activity=state.activities.firstOrNull {it.id.toString()==selected}
    val duration=minutes.toIntOrNull()
    val enabled=!state.busy && !state.loading && !state.uncertain
    Page {
        DateStrip(vm)
        Choice("Loại vận động",selected,state.activities.map{it.id.toString() to it.name},enabled){selected=it}
        if(state.activities.isEmpty()) Text("Chưa có bài tập trong danh mục.")
        NumberInput("Thời gian hoạt động (phút)",minutes,enabled){minutes=it}
        val profile=state.profile
        if(profile==null) Button(onClick={navigate("profile")}){Text("Nhập hồ sơ trước khi ghi vận động")}
        if(activity!=null && duration!=null && duration>0 && profile!=null) {
            Text("Ước tính ${(activity.met*3.5*profile.weight_kg/200*duration).display()} kcal • MET ${activity.met.display()}")
        }
        Button(enabled=enabled && profile!=null && activity!=null && duration!=null && duration in 1..1440,onClick={
            vm.write {ctx -> vm.api.addActivity(ctx.owner,mapOf("activity_type_id" to activity?.id,"duration_minutes" to duration,"log_date" to ctx.date))}
        }){Text("Lưu vận động")}
        state.dashboard?.activity_logs?.forEach {log -> Panel(log.activity_name){Text("${log.duration_minutes} phút • ${log.calories_burned.display()} kcal")}}
    }
}
