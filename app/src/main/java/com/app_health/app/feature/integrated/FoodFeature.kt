package com.app_health.app.feature.integrated

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    FigmaPage {
        Text("${uiDate(state.date.toString())}  •  $time", modifier = Modifier.align(Alignment.CenterHorizontally),
            color = FigmaInk, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(query,{query=it.take(100);selectedId=null},placeholder={Text("Tìm thực phẩm hoặc món ăn")},
            leadingIcon={Icon(Icons.Default.Search,null,tint=FigmaMuted)},singleLine=true,enabled=enabled,
            shape=RoundedCornerShape(28.dp),colors=OutlinedTextFieldDefaults.colors(
                focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,
                focusedBorderColor=FigmaGreen,unfocusedBorderColor=Color.Transparent),modifier=Modifier.fillMaxWidth())
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!searching && state.foods.isEmpty()) Text("Không tìm thấy thực phẩm phù hợp.")
        if (selected == null && state.foods.isNotEmpty()) Text("Danh sách thực phẩm", color=FigmaMuted,
            style=MaterialTheme.typography.labelLarge, modifier=Modifier.padding(horizontal=12.dp))
        if (selected == null) Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { state.foods.forEach { food ->
            Card(onClick={selectedId=food.id},enabled=enabled && !searching,modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White),
                elevation=CardDefaults.cardElevation(defaultElevation=0.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=16.dp),
                    horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(Color(0xFFF3F4F6),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                        Icon(Icons.Default.Restaurant,null,tint=Color(0xFFE8A02F),modifier=Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text(food.name,color=FigmaInk,fontSize=15.sp,fontWeight=FontWeight.Bold)
                        Text("100 g • ${food.calories_per_100g.display()} kcal",color=FigmaMuted,fontSize=12.sp)
                        FoodMacroLine(food.protein_per_100g,food.carb_per_100g,food.fat_per_100g)
                    }
                    FilledTonalIconButton(onClick={selectedId=food.id},enabled=enabled && !searching,
                        colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=Color(0xFFD5FAEB),contentColor=Mint)) {
                        Icon(Icons.Default.Add,"Chọn ${food.name} và điều chỉnh khẩu phần")
                    }
                }
            }
        } } else FigmaCard(selected.name) {
            NumberInput("Khẩu phần (gram)",grams,enabled) {grams=it}
            if (amount != null && amount > 0) {
                val factor = amount/100
                Text("${(selected.calories_per_100g*factor).display()} kcal")
                FoodMacroLine(selected.protein_per_100g*factor,selected.carb_per_100g*factor,selected.fat_per_100g*factor)
            }
            MealInputs(meal,time,enabled,{meal=it},{time=it})
            Button(enabled=enabled && amount != null && amount >= 0.01 && amount <= 10000 && validMealTime(time),onClick={
                vm.write(success={navigate("home")}) {ctx ->
                    vm.api.addFood(ctx.owner,mapOf("food_id" to selected.id,"grams" to amount,"meal_type" to meal,"meal_time" to time,"log_date" to ctx.date))
                }
            },modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)) {Text("Thêm vào nhật ký")}
            TextButton(onClick={selectedId=null},enabled=enabled) {Text("Chọn món khác")}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodMacroLine(protein: Double, carbs: Double, fat: Double) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Text("P ${protein.display()} g",fontSize=12.sp,color=Color(0xFFD82C83))
        Text("C ${carbs.display()} g",fontSize=12.sp,color=Color(0xFF138CB8))
        Text("F ${fat.display()} g",fontSize=12.sp,color=Color(0xFFB47400))
    }
}
