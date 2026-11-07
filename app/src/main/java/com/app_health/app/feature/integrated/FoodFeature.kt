package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.app_health.app.core.network.toUserMessage

fun foodFeature() = AppFeature("food", "Tìm món ăn") { vm, navigate -> FoodScreen(vm, navigate) }

@Composable
fun FoodScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var grams by rememberSaveable { mutableStateOf("100") }
    var meal by rememberSaveable { mutableStateOf("snack") }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query, state.date, state.loading, state.busy, state.readRevision, state.route) {
        if(state.loading || state.busy || state.route!="food")return@LaunchedEffect
        searching = true
        delay(300)
        val ctx = vm.context()
        try {
            val results = vm.api.foods(ctx.owner,query.trim())
            vm.readSucceeded(ctx) { it.copy(foods=results) }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { currentCoroutineContext().ensureActive(); vm.showError(e.toUserMessage(),ctx)
        } finally { searching = false }
    }
    val selected = state.foods.firstOrNull { it.id == selectedId }
    val amount = inputNumber(grams)
    val enabled = !state.busy && !state.loading && !state.uncertain
    Page {
        Text("Thêm món vào ngày ${state.date}")
        OutlinedTextField(query,{query=it.take(100);selectedId=null},label={Text("Tìm thực phẩm")},singleLine=true,enabled=enabled,modifier=Modifier.fillMaxWidth())
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!searching && state.foods.isEmpty()) Text("Không tìm thấy thực phẩm phù hợp.")
        if (selected == null) state.foods.forEach { food ->
            OutlinedCard(onClick={selectedId=food.id},enabled=enabled && !searching,modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(food.name,style=MaterialTheme.typography.titleMedium)
                    Text("${food.calories_per_100g.display()} kcal / 100 g")
                }
            }
        } else Panel(selected.name) {
            NumberInput("Khẩu phần (gram)",grams,enabled) {grams=it}
            if (amount != null && amount > 0) {
                val factor = amount/100
                Text("${(selected.calories_per_100g*factor).display()} kcal")
                Text("P ${(selected.protein_per_100g*factor).display()} g • C ${(selected.carb_per_100g*factor).display()} g • F ${(selected.fat_per_100g*factor).display()} g")
            }
            MealInputs(meal,time,enabled,{meal=it},{time=it})
            Button(enabled=enabled && amount != null && amount >= 0.01 && amount <= 10000 && validMealTime(time),onClick={
                vm.write(success={navigate("home")}) {ctx ->
                    vm.api.addFood(ctx.owner,mapOf("food_id" to selected.id,"grams" to amount,"meal_type" to meal,"meal_time" to time,"log_date" to ctx.date))
                }
            }) {Text("Thêm vào nhật ký")}
            TextButton(onClick={selectedId=null}) {Text("Chọn món khác")}
        }
    }
}
