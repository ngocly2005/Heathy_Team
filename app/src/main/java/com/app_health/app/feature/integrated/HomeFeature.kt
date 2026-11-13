package com.app_health.app.feature.integrated

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.abs

fun homeFeature() = AppFeature("home", "Trang chủ", load = { vm, ctx ->
    val dashboard = vm.api.dashboard(ctx.owner, ctx.date)
    val nutrition = vm.api.nutrition(ctx.owner)
    vm.publish(ctx) { it.copy(dashboard = dashboard, nutrition = nutrition) }
}) { vm, navigate -> HomeScreen(vm, navigate) }

@Composable fun DateStrip(vm: AppViewModel) = WeekStrip(vm)

@Composable
private fun HomeDates(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    var weekText by rememberSaveable { mutableStateOf(weekBeginning(state.date).toString()) }
    val week = LocalDate.parse(weekText)
    val today = LocalDate.now()
    val enabled = !state.busy && !state.uncertain
    LaunchedEffect(state.date) {
        if (state.date < week || state.date > week.plusDays(6)) weekText = weekBeginning(state.date).toString()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${if (state.date == today) "HÔM NAY" else "NGÀY ĐÃ CHỌN"}, ${state.date}", Modifier.weight(1f), color = FigmaMuted, fontSize = 11.sp)
            IconButton(enabled = enabled, onClick = { weekText = week.minusWeeks(1).toString() }) { Icon(Icons.Default.ChevronLeft, "Tuần trước", tint = FigmaMuted) }
            IconButton(enabled = enabled && week < weekBeginning(today), onClick = { weekText = week.plusWeeks(1).toString() }) { Icon(Icons.Default.ChevronRight, "Tuần sau", tint = FigmaMuted) }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = (maxWidth / 7).coerceAtLeast(44.dp)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                repeat(7) { i ->
                    val date = week.plusDays(i.toLong())
                    val selected = date == state.date
                    Column(Modifier.width(width).heightIn(min = 76.dp).clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = enabled && date <= today) { vm.chooseDate(date) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")[i], color = FigmaMuted, fontSize = 11.sp)
                        Box(Modifier.size(38.dp).background(if (selected) Mint else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
                            Text(date.dayOfMonth.toString(), color = if (selected) Color.White else FigmaInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    FigmaPage(background = Color(0xFFE4F4EE)) {
        HomeDates(vm)
        if (!state.loading && state.error == null && state.profile == null) FigmaCard("Thiết lập hồ sơ") {
            Text("Nhập hồ sơ để tính BMI, TDEE và mục tiêu dinh dưỡng.", color = FigmaMuted)
            Button(enabled = !state.busy, onClick = { navigate("profile") }) { Text("Nhập hồ sơ") }
        }
        state.dashboard?.let { day ->
            CalorieSummaryCard(day)
            FigmaMacroStrip(state) { navigate("macros") }
            FigmaTitle("Nhật ký gần đây")
            FigmaCard {
                if (day.food_logs.isEmpty()) Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, null, tint = FigmaMuted, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(12.dp)); Text("Chưa có dữ liệu", color = FigmaMuted)
                } else day.food_logs.sortedByDescending { it.id }.take(3).forEach { food ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, null, tint = FigmaGreen)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(food.food_name, color = FigmaInk, fontWeight = FontWeight.SemiBold)
                            Text("${food.grams.uiNumber()} g • ${food.calories_kcal.uiNumber()} kcal", color = FigmaMuted, fontSize = 12.sp)
                        }
                    }
                }
                TextButton(enabled = !state.busy, onClick = { navigate("diary") }, modifier = Modifier.align(Alignment.End)) { Text("Mở nhật ký") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Mục tiêu" to day.calorie_target, "Đã nạp" to day.calories_consumed, "Tập luyện" to day.calories_burned).forEachIndexed { i, (label, value) ->
                    FigmaCard(modifier = Modifier.weight(1f)) {
                        Text(value?.uiNumber() ?: "—", color = if (i == 1) Mint else FigmaInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(label, color = FigmaMuted, fontSize = 11.sp)
                    }
                }
            }
            FigmaTitle("Hoạt động tập luyện")
            FigmaCard {
                Text("Chọn bài tập và ghi thời gian hoạt động từ danh mục.", color = FigmaMuted, fontSize = 13.sp)
                Button(enabled = !state.busy, onClick = { navigate("activity") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DirectionsRun, null); Spacer(Modifier.width(8.dp)); Text("Ghi nhận vận động")
                }
            }
            HomeWaterCard(vm)
        }
        FigmaTitle("Tác vụ nhanh")
        ActionGrid(listOf(
            HealthAction("Chụp món ăn", "AI Camera", Icons.Default.PhotoCamera, "photo"),
            HealthAction("Tìm món ăn", "Tra cứu thực phẩm", Icons.Default.Search, "food"),
            HealthAction("Dinh dưỡng", "Mục tiêu và macro", Icons.Default.Restaurant, "macros"),
            HealthAction("Vận động", "Ghi nhận bài tập", Icons.Default.DirectionsRun, "activity"),
            HealthAction("Uống nước", "Theo dõi mỗi ngày", Icons.Default.WaterDrop, "water"),
            HealthAction("Cân nặng", "Xu hướng theo tháng", Icons.Default.MonitorWeight, "weight")
        ), onNavigate = navigate, enabled = !state.busy)
    }
}

@Composable
fun CalorieSummaryCard(day: DashboardDto) {
    val model = caloriePresentation(day)
    val exceeded = model.remaining != null && model.remaining < 0
    val large = LocalDensity.current.fontScale > 1.3f
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mục tiêu calo", Modifier.align(Alignment.Start), color = FigmaInk, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        Box(Modifier.size(if (large) 244.dp else 224.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { model.progress ?: 0f }, modifier = Modifier.fillMaxSize(),
                strokeWidth = 14.dp, color = if (model.aboveFoodTarget) Color(0xFFF5A13C) else FigmaGreen, trackColor = Color(0xFFE5E7EB))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(model.remaining?.let { abs(it).uiNumber() } ?: "—", color = FigmaInk,
                    fontSize = if (large) 32.sp else 42.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (exceeded) "Calo vượt mục tiêu" else "Calo còn lại", color = FigmaMuted, fontSize = 14.sp)
            }
        }
        Text("Đã ăn ${day.calories_consumed.uiNumber()} / ${day.calorie_target?.uiNumber() ?: "—"} kcal", color = FigmaMuted, fontSize = 12.sp)
        if (exceeded) Text("Đã vượt mục tiêu của ngày đã chọn.", color = Color(0xFFB94433))
    }
}

@Composable
private fun HomeWaterCard(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val settings = state.water ?: return
    val day = state.dashboard ?: return
    val enabled = !state.busy && !state.loading && !state.uncertain
    FigmaCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Uống nước", color = FigmaMuted, fontSize = 12.sp)
                Text("${(day.water_ml / 1000.0).uiNumber(2)} / ${(settings.effective_target_ml / 1000.0).uiNumber(2)} Lít", color = FigmaInk, fontWeight = FontWeight.Bold)
            }
            FilledTonalIconButton(enabled = enabled && day.water_ml > 0, onClick = {
                vm.write { ctx -> vm.api.adjustWater(ctx.owner, mapOf("log_date" to ctx.date, "amount_ml" to -settings.serving_ml)) }
            }) { Icon(Icons.Default.Remove, "Giảm một ly nước") }
            FilledTonalIconButton(enabled = enabled, onClick = {
                vm.write { ctx -> vm.api.adjustWater(ctx.owner, mapOf("log_date" to ctx.date, "amount_ml" to settings.serving_ml)) }
            }) { Icon(Icons.Default.Add, "Thêm một ly nước") }
        }
    }
}
