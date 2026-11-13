package com.app_health.app.feature.integrated

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer
import java.util.Locale

fun activityFeature() = AppFeature("activity", "Vận động",load={vm,ctx ->
    val activities=vm.api.activities(ctx.owner)
    vm.publish(ctx){it.copy(activities=activities)}
}) {vm,navigate -> ActivityScreen(vm,navigate)}

@Composable
fun ActivityScreen(vm: AppViewModel,navigate:(String)->Unit) {
    val state by vm.state.collectAsState()
    var selected by rememberSaveable {mutableStateOf("")}
    var minutes by rememberSaveable {mutableStateOf("30")}
    var query by rememberSaveable {mutableStateOf("")}
    val activity=state.activities.firstOrNull {it.id.toString()==selected}
    val duration=minutes.toIntOrNull()
    val enabled=!state.busy && !state.loading && !state.uncertain
    val profile=state.profile
    FigmaPage {
        DateStrip(vm)
        if (activity == null) {
            OutlinedTextField(query,{query=it.take(100)},placeholder={Text("Tìm kiếm hoạt động")},singleLine=true,
                enabled=enabled,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),
                leadingIcon={Icon(Icons.Default.Search,null,tint=FigmaMuted)},
                colors=OutlinedTextFieldDefaults.colors(focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,
                    focusedBorderColor=FigmaGreen,unfocusedBorderColor=Color(0xFFD6DAE1)))
            if(state.activities.isEmpty()) Text("Chưa có bài tập trong danh mục.",color=FigmaMuted)
            val shown=state.activities.filter { it.name.contains(query.trim(),ignoreCase=true) }
                .sortedWith(compareBy<ActivityDto> { activityLetter(it.name) }.thenBy { it.name.lowercase(Locale.ROOT) })
            if(state.activities.isNotEmpty() && shown.isEmpty()) Text("Không tìm thấy hoạt động phù hợp.",color=FigmaMuted)
            shown.groupBy { activityLetter(it.name) }.forEach { (letter,rows) ->
                Text(letter,color=FigmaMuted,style=MaterialTheme.typography.labelLarge,
                    fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
                Column {
                    rows.forEach { row ->
                        Row(Modifier.fillMaxWidth().heightIn(min=54.dp)
                            .clickable(enabled=enabled){selected=row.id.toString()}.padding(vertical=14.dp),
                            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Text(row.name,Modifier.weight(1f),color=FigmaInk,fontSize=15.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,"Chọn ${row.name}",tint=FigmaInk,modifier=Modifier.size(20.dp))
                        }
                        HorizontalDivider(color=Color(0xFFE1E4EB))
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(activity.name,Modifier.weight(1f),color=FigmaInk,fontSize=20.sp,fontWeight=FontWeight.Bold)
                TextButton(enabled=enabled,onClick={selected=""}) {Text("Chọn lại",color=FigmaMuted)}
            }
            Surface(Modifier.fillMaxWidth().height(150.dp),shape=RoundedCornerShape(24.dp),color=Color(0xFF2B2B3C)) {
                Box(contentAlignment=Alignment.Center) {
                    Icon(Icons.Default.DirectionsWalk,null,tint=Color.White,modifier=Modifier.size(62.dp))
                }
            }
        }
        FigmaCard {
            Choice("Loại vận động",selected,state.activities.map{it.id.toString() to it.name},enabled){selected=it}
            NumberInput("Thời gian hoạt động (phút)",minutes,enabled){minutes=it}
            if(activity!=null && duration!=null && duration>0 && profile!=null) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Calo tiêu thụ ước tính",Modifier.weight(1f),color=FigmaMuted)
                    Text("${(activity.met*3.5*profile.weight_kg/200*duration).display()} kcal",color=FigmaInk,fontWeight=FontWeight.Bold)
                }
                HorizontalDivider(color=Color(0xFFD6DAE1))
                Text("MET ${activity.met.display()} • Cân nặng ${profile.weight_kg.display()} kg",color=FigmaMuted,
                    style=MaterialTheme.typography.bodySmall)
            }
            if(profile==null) OutlinedButton(onClick={navigate("profile")},enabled=enabled,modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(24.dp)){Text("Nhập hồ sơ trước khi ghi vận động")}
            Button(enabled=enabled && profile!=null && activity!=null && duration!=null && duration in 1..1440,onClick={
                vm.write {ctx -> vm.api.addActivity(ctx.owner,mapOf("activity_type_id" to activity?.id,"duration_minutes" to duration,"log_date" to ctx.date))}
            },modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Text("Lưu vận động")}
        }
        state.dashboard?.activity_logs?.forEach {log -> FigmaCard(log.activity_name){Text("${log.duration_minutes} phút • ${log.calories_burned.display()} kcal",color=FigmaMuted)}}
    }
}

private fun activityLetter(name: String): String {
    val plain=Normalizer.normalize(name.trim(),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").replace('Đ','D').replace('đ','d')
    return plain.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
}
