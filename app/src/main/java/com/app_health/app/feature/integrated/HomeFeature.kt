package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app_health.app.feature.home.components.Home.CalorieRing
import com.app_health.app.feature.home.components.Home.WeeklyCalendarSwipeable
import java.time.LocalDate

fun homeFeature() = AppFeature("home", "Trang chủ", load = { vm, ctx ->
    val dashboard = vm.api.dashboard(ctx.owner, ctx.date)
    val nutrition = vm.api.nutrition(ctx.owner)
    vm.publish(ctx) { it.copy(dashboard = dashboard, nutrition = nutrition) }
}) { vm, navigate -> HomeScreen(vm, navigate) }

@Composable
fun DateStrip(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val enabled = !state.busy && !state.uncertain
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { vm.chooseDate(state.date.minusDays(1)) }, enabled = enabled) { Text("‹") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (state.date == LocalDate.now()) "Hôm nay" else "Ngày đã chọn")
            Text(state.date.toString(), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = { vm.chooseDate(state.date.plusDays(1)) }, enabled = enabled && state.date < LocalDate.now()) { Text("›") }
    }
    WeeklyCalendarSwipeable(selectedDate=state.date,onDateSelected={if(enabled)vm.chooseDate(it)})
}

@Composable
fun HomeScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    Page {
        Text("Chào ${vm.container.sessionStore.snapshot()?.user?.fullName ?: "bạn"}!", style = MaterialTheme.typography.headlineSmall)
        DateStrip(vm)
        if (state.profile == null) Panel("Thiết lập hồ sơ") {
            Text("Lưu hồ sơ để tính mục tiêu và năng lượng vận động.")
            Button(onClick = { navigate("profile") }) { Text("Nhập hồ sơ") }
        }
        state.dashboard?.let { day ->
            Panel("Tiến độ calo thuần") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if(day.calorie_target!=null) CalorieRing(day.net_calories.toInt(), day.calorie_target.toInt())
                    else Text("Chưa thiết lập mục tiêu calo",Modifier.padding(24.dp))
                }
                Text("Calo thuần = đã ăn − vận động")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Đã ăn\n${day.calories_consumed.display()} kcal")
                    Text("Vận động\n${day.calories_burned.display()} kcal")
                }
                Text("Mục tiêu: ${day.calorie_target?.display() ?: "Chưa có"} kcal")
                Text("Còn lại: ${day.calories_remaining?.display() ?: "Chưa có mục tiêu"} kcal")
            }
            Panel("Tổng quan ngày") {
                Text("${day.food_logs.size} món ăn • ${day.activity_logs.size} hoạt động")
                Text("Nước: ${day.water_ml} ml")
            }
            MacroOverview(state)
            WaterOverview(vm)
            if (day.food_logs.isNotEmpty()) Panel("Bữa ăn gần đây") {
                day.food_logs.takeLast(4).forEach { Text("${it.food_name} • ${it.grams.display()} g • ${it.calories_kcal.display()} kcal") }
            }
        }
        installedFeatures().filter { it.route !in setOf("home","profile","diary","meals") }.forEach { feature ->
            OutlinedButton(onClick = { navigate(feature.route) }, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text(feature.title) }
        }
    }
}
