package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

fun diaryFeature()=AppFeature("diary","Nhật ký",load={vm,ctx ->
    val daily=vm.api.daily(ctx.owner,ctx.date)
    vm.publish(ctx){it.copy(daily=daily,dashboard=daily.dashboard)}
}){vm,navigate ->DiaryScreen(vm,navigate)}

@Composable
fun DiaryScreen(vm:AppViewModel,navigate:(String)->Unit) {
    val state by vm.state.collectAsState()
    val day=state.daily
    var editing by rememberSaveable(state.date){mutableStateOf<String?>(null)}
    var deleting by rememberSaveable(state.date){mutableStateOf<String?>(null)}
    var itemId by rememberSaveable(state.date){mutableLongStateOf(0L)}
    var amount by rememberSaveable(state.date){mutableStateOf("")}
    var meal by rememberSaveable(state.date){mutableStateOf("snack")}
    var time by rememberSaveable(state.date){mutableStateOf("")}
    var note by rememberSaveable(state.date,day?.diary?.content){mutableStateOf(day?.diary?.content ?: "")}
    var mood by rememberSaveable(state.date,day?.diary?.mood){mutableStateOf(day?.diary?.mood ?: "")}
    val enabled=!state.busy && !state.loading && !state.uncertain
    Page {
        DateStrip(vm)
        if(day==null)Text("Chưa tải được nhật ký ngày này.") else {
            Panel("Tổng ngày") {
                Text("Đã ăn ${day.dashboard.calories_consumed.display()} kcal • Vận động ${day.dashboard.calories_burned.display()} kcal")
                Text("Nước ${day.dashboard.water_ml} ml")
                day.weight?.let{Text("Cân nặng ${it.weight_kg.display()} kg")}
            }
            Text("Món ăn",style=MaterialTheme.typography.titleLarge)
            if(day.dashboard.food_logs.isEmpty())Text("Chưa ghi món ăn.")
            day.dashboard.food_logs.sortedWith(compareBy<FoodLogDto>{it.meal_time ?: "99:99"}.thenBy{it.id}).forEach{food ->Panel(food.food_name){
                Text("${food.meal_time ?: mealChoices.firstOrNull{it.first==food.meal_type}?.second ?: food.meal_type} • ${food.grams.display()} g • ${food.calories_kcal.display()} kcal")
                Text("P ${food.protein_g.display()} • C ${food.carb_g.display()} • F ${food.fat_g.display()} g")
                Row {
                    TextButton(enabled=enabled,onClick={itemId=food.id;amount=food.grams.display();meal=food.meal_type;time=food.meal_time ?: "";editing="food"}){Text("Sửa")}
                    TextButton(enabled=enabled,onClick={itemId=food.id;deleting="food"}){Text("Xóa")}
                }
            }}
            OutlinedButton(onClick={navigate("food")},enabled=enabled){Text("Thêm món ăn")}
            Text("Vận động",style=MaterialTheme.typography.titleLarge)
            day.dashboard.activity_logs.forEach{activity ->Panel(activity.activity_name){
                Text("${activity.duration_minutes} phút • ${activity.calories_burned.display()} kcal")
                Row {
                    TextButton(enabled=enabled,onClick={itemId=activity.id;amount=activity.duration_minutes.toString();editing="activity"}){Text("Sửa")}
                    TextButton(enabled=enabled,onClick={itemId=activity.id;deleting="activity"}){Text("Xóa")}
                }
            }}
            OutlinedButton(onClick={navigate("activity")},enabled=enabled){Text("Thêm vận động")}
            Text("Nước uống",style=MaterialTheme.typography.titleLarge)
            day.water_logs.forEach{water ->Panel("${water.amount_ml} ml"){
                Row {
                    TextButton(enabled=enabled,onClick={itemId=water.id;amount=water.amount_ml.toString();editing="water"}){Text("Sửa")}
                    TextButton(enabled=enabled,onClick={itemId=water.id;deleting="water"}){Text("Xóa")}
                }
            }}
            OutlinedButton(onClick={navigate("water")},enabled=enabled){Text("Ghi thêm nước")}
            Panel("Ghi chú trong ngày") {
                OutlinedTextField(note,{note=it.take(10000)},label={Text("Nội dung nhật ký")},enabled=enabled,minLines=3,maxLines=6)
                OutlinedTextField(mood,{mood=it.take(30)},label={Text("Tâm trạng (không bắt buộc)")},enabled=enabled)
                Button(enabled=enabled && note.isNotBlank(),onClick={vm.write{ctx ->vm.api.saveDiary(ctx.owner,ctx.date,mapOf("content" to note.trim(),"mood" to mood.trim().ifBlank{null}))}}){Text("Lưu ghi chú")}
                if(day.diary!=null)TextButton(enabled=enabled,onClick={deleting="note"}){Text("Xóa ghi chú")}
            }
        }
    }
    editing?.let{kind ->
        val number=inputNumber(amount)
        val valid=when(kind){"food" ->number!=null && number>=0.01 && number<=10000 && (time.isBlank()||validMealTime(time));"activity" ->amount.toIntOrNull()?.let{it in 1..1440}==true;else ->amount.toIntOrNull()?.let{it in 1..5000}==true}
        AlertDialog(onDismissRequest={if(!state.busy)editing=null},title={Text("Sửa mục nhật ký")},text={Column {
            NumberInput(when(kind){"food" ->"Khẩu phần (g)";"activity" ->"Thời gian (phút)";else ->"Lượng nước (ml)"},amount,enabled){amount=it}
            if(kind=="food")MealInputs(meal,time,enabled,{meal=it},{time=it})
        }},confirmButton={TextButton(enabled=enabled && valid,onClick={vm.write(success={editing=null}){ctx ->when(kind){
            "food" ->vm.api.editFood(ctx.owner,itemId,mapOf("grams" to number,"meal_type" to meal,"meal_time" to time.ifBlank{null}))
            "activity" ->vm.api.editActivity(ctx.owner,itemId,mapOf("duration_minutes" to amount.toInt()))
            else ->vm.api.editWater(ctx.owner,itemId,mapOf("amount_ml" to amount.toInt()))
        }}}){Text("Lưu")}},dismissButton={TextButton(enabled=!state.busy,onClick={editing=null}){Text("Hủy")}})
    }
    deleting?.let{kind ->AlertDialog(onDismissRequest={if(!state.busy)deleting=null},title={Text("Xóa mục đã chọn?")},
        confirmButton={TextButton(enabled=enabled,onClick={vm.write(success={deleting=null}){ctx ->when(kind){
            "food" ->vm.api.deleteFood(ctx.owner,itemId)
            "activity" ->vm.api.deleteActivity(ctx.owner,itemId)
            "water" ->vm.api.deleteWater(ctx.owner,itemId)
            else ->vm.api.deleteDiary(ctx.owner,ctx.date)
        }}}){Text("Xóa")}},dismissButton={TextButton(enabled=!state.busy,onClick={deleting=null}){Text("Hủy")}})}
}
