package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun mealsFeature()=AppFeature("meals","Thực đơn đã lưu",load={vm,ctx ->
    val meals=vm.api.meals(ctx.owner)
    vm.publish(ctx){it.copy(meals=meals)}
}){vm,navigate -> MealsScreen(vm,navigate)}

private fun snapshot(item:SavedItemDto,grams:Double=item.grams):Map<String,Any?> = mapOf(
    "food_id" to null,"food_name" to item.food_name,"grams" to grams,
    "calories_per_100g" to item.calories_per_100g,"protein_per_100g" to item.protein_per_100g,
    "carb_per_100g" to item.carb_per_100g,"fat_per_100g" to item.fat_per_100g)

@Composable
fun MealsScreen(vm:AppViewModel,navigate:(String)->Unit) {
    val state by vm.state.collectAsState()
    var name by rememberSaveable{mutableStateOf("")}
    var meal by rememberSaveable{mutableStateOf("snack")}
    var time by rememberSaveable{mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))}
    var deleting by rememberSaveable{mutableStateOf<Long?>(null)}
    var editing by remember{mutableStateOf<SavedMealDto?>(null)}
    var editName by rememberSaveable{mutableStateOf("")}
    var editGrams by remember{mutableStateOf<List<String>>(emptyList())}
    val logs=state.dashboard?.food_logs ?: emptyList()
    var chosen by remember(state.date,logs.map{it.id}){mutableStateOf(logs.map{it.id}.toSet())}
    val enabled=!state.busy && !state.loading && !state.uncertain
    Page {
        DateStrip(vm)
        MealInputs(meal,time,enabled,{meal=it},{time=it})
        if(state.meals.isEmpty()) Text("Chưa có bữa ăn đã lưu. Bạn có thể lưu các món trong nhật ký bên dưới.")
        state.meals.forEach {saved -> Panel(saved.name){
            saved.items.forEach{Text("${it.food_name} • ${it.grams.display()} g")}
            val total=saved.items.sumOf{it.calories_per_100g*it.grams/100}
            Text("${total.display()} kcal / bữa")
            Button(enabled=enabled && validMealTime(time),onClick={vm.write{ctx ->
                vm.api.logMeal(ctx.owner,saved.id,mapOf("log_date" to ctx.date,"meal_type" to meal,"meal_time" to time))
            }}){Text("Thêm nhanh vào ngày ${state.date}")}
            Row {
                TextButton(enabled=enabled,onClick={editing=saved;editName=saved.name;editGrams=saved.items.map{it.grams.display()}}){Text("Sửa bữa")}
                TextButton(enabled=enabled,onClick={deleting=saved.id}){Text("Xóa")}
            }
        }}
        Panel("Lưu bữa từ nhật ký ngày đã chọn") {
            OutlinedTextField(name,{name=it.take(120)},label={Text("Tên bữa ăn")},enabled=enabled)
            logs.forEach {log -> Row {
                Checkbox(checked=log.id in chosen,onCheckedChange={checked -> chosen=if(checked)chosen+log.id else chosen-log.id},enabled=enabled)
                Text("${log.food_name} • ${log.grams.display()} g")
            }}
            if(logs.isEmpty()) OutlinedButton(onClick={navigate("food")}){Text("Thêm món vào nhật ký trước")}
            Button(enabled=enabled && name.isNotBlank() && chosen.isNotEmpty() && chosen.size<=30,onClick={
                val items=logs.filter{it.id in chosen}.map{log ->
                    mapOf("food_id" to null,"food_name" to log.food_name,"grams" to log.grams,
                        "calories_per_100g" to log.calories_kcal*100/log.grams,"protein_per_100g" to log.protein_g*100/log.grams,
                        "carb_per_100g" to log.carb_g*100/log.grams,"fat_per_100g" to log.fat_g*100/log.grams)
                }
                vm.write(success={name=""}){ctx -> vm.api.saveMeal(ctx.owner,mapOf("name" to name.trim(),"items" to items))}
            }){Text("Lưu bữa ăn")}
        }
    }
    deleting?.let{id -> AlertDialog(onDismissRequest={if(!state.busy)deleting=null},title={Text("Xóa bữa đã lưu?")},
        text={Text("Các món đã ghi vào nhật ký vẫn được giữ lại.")},confirmButton={TextButton(enabled=enabled,onClick={vm.write(success={deleting=null}){ctx ->vm.api.deleteMeal(ctx.owner,id)}}){Text("Xóa")}},
        dismissButton={TextButton(enabled=!state.busy,onClick={deleting=null}){Text("Hủy")}})}
    editing?.let{saved -> AlertDialog(onDismissRequest={if(!state.busy)editing=null},title={Text("Sửa bữa đã lưu")},text={
        Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(editName,{editName=it.take(120)},label={Text("Tên bữa")},enabled=enabled)
            saved.items.forEachIndexed {i,item -> NumberInput("${item.food_name} (g)",editGrams.getOrElse(i){""},enabled){value -> editGrams=editGrams.toMutableList().apply{set(i,value)}} }
        }
    },confirmButton={TextButton(enabled=enabled && editName.isNotBlank() && editGrams.all{inputNumber(it)?.let{n ->n>0&&n<=10000}==true},onClick={
        val items=saved.items.mapIndexed{i,item ->snapshot(item,inputNumber(editGrams[i])!!)}
        vm.write(success={editing=null}){ctx ->vm.api.editMeal(ctx.owner,saved.id,mapOf("name" to editName.trim(),"items" to items))}
    }){Text("Lưu")}},dismissButton={TextButton(enabled=!state.busy,onClick={editing=null}){Text("Hủy")}})}
}
