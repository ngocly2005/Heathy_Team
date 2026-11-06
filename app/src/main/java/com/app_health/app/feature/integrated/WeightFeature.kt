package com.app_health.app.feature.integrated

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate

fun weightFeature()=AppFeature("weight","Cân nặng",load={vm,ctx ->
    val weights=vm.api.weights(ctx.owner)
    vm.publish(ctx){it.copy(weights=weights.sortedBy{row ->row.measured_on})}
}){vm,_ -> WeightScreen(vm)}

@Composable
fun WeightTrend(weights:List<WeightDto>) {
    if(weights.isEmpty()){Text("Chưa có dữ liệu cân nặng.");return}
    var selected by remember(weights){mutableFloatStateOf((weights.size-1).toFloat())}
    val color=MaterialTheme.colorScheme.primary
    val from=LocalDate.parse(weights.first().measured_on).toEpochDay()
    val to=LocalDate.parse(weights.last().measured_on).toEpochDay()
    val low=weights.minOf{it.weight_kg}-1
    val high=weights.maxOf{it.weight_kg}+1
    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val inset=20f
        fun position(row:WeightDto):Offset {
            val x=if(to==from)0.5f else ((LocalDate.parse(row.measured_on).toEpochDay()-from).toDouble()/(to-from)).toFloat()
            val y=((row.weight_kg-low)/(high-low)).toFloat()
            return Offset(inset+x*(size.width-2*inset),size.height-inset-y*(size.height-2*inset))
        }
        drawLine(Color.LightGray,Offset(inset,size.height-inset),Offset(size.width-inset,size.height-inset),2f)
        weights.zipWithNext().forEach{(a,b)->drawLine(color,position(a),position(b),5f)}
        weights.forEachIndexed{i,row ->drawCircle(color,if(i==selected.toInt())9f else 5f,position(row))}
    }
    Text("${weights.first().measured_on} → ${weights.last().measured_on} • ${low.display()}–${high.display()} kg")
    if(weights.size>1) Slider(value=selected,onValueChange={selected=it},valueRange=0f..(weights.size-1).toFloat(),steps=(weights.size-2).coerceAtLeast(0))
    weights.getOrNull(selected.toInt())?.let{Text("${it.measured_on}: ${it.weight_kg.display()} kg",style=MaterialTheme.typography.titleMedium)}
}

@Composable
fun WeightScreen(vm:AppViewModel) {
    val state by vm.state.collectAsState()
    var date by rememberSaveable(state.date){mutableStateOf(state.date.toString())}
    var value by rememberSaveable{mutableStateOf("")}
    var deleting by rememberSaveable{mutableStateOf<String?>(null)}
    val day=runCatching{LocalDate.parse(date)}.getOrNull()
    val weight=inputNumber(value)
    val enabled=!state.busy && !state.loading && !state.uncertain
    Page {
        Panel("Xu hướng cân nặng"){WeightTrend(state.weights)}
        Panel("Ghi số đo") {
            OutlinedTextField(date,{date=it},label={Text("Ngày đo (YYYY-MM-DD)")},singleLine=true,enabled=enabled)
            NumberInput("Cân nặng (kg)",value,enabled){value=it}
            Button(enabled=enabled && day!=null && day<=LocalDate.now() && weight!=null && weight>20 && weight<=400,onClick={
                vm.write(success={value=""}){ctx ->vm.api.saveWeight(ctx.owner,date,mapOf("weight_kg" to weight))}
            }){Text("Lưu cân nặng")}
        }
        state.weights.asReversed().forEach{entry ->Panel(entry.measured_on){
            Text("${entry.weight_kg.display()} kg")
            Row {
                TextButton(enabled=enabled,onClick={date=entry.measured_on;value=entry.weight_kg.display()}){Text("Sửa")}
                TextButton(enabled=enabled,onClick={deleting=entry.measured_on}){Text("Xóa")}
            }
        }}
    }
    deleting?.let{target ->AlertDialog(onDismissRequest={if(!state.busy)deleting=null},title={Text("Xóa số đo ngày $target?")},
        confirmButton={TextButton(enabled=enabled,onClick={vm.write(success={deleting=null}){ctx ->vm.api.deleteWeight(ctx.owner,target)}}){Text("Xóa")}},
        dismissButton={TextButton(enabled=!state.busy,onClick={deleting=null}){Text("Hủy")}})}
}
