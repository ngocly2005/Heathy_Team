package com.app_health.app.feature.integrated

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

fun homeFeature() = AppFeature("home", "Trang chủ", load = { vm, ctx ->
    val dashboard = vm.api.dashboard(ctx.owner, ctx.date)
    val nutrition = vm.api.nutrition(ctx.owner)
    vm.publish(ctx) { it.copy(dashboard = dashboard, nutrition = nutrition) }
}) { vm, navigate -> HomeScreen(vm, navigate) }

@Composable
fun DateStrip(vm: AppViewModel) = WeekStrip(vm)

@Composable
fun HomeScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val name = vm.container.sessionStore.snapshot()?.user?.fullName?.ifBlank { null } ?: "bạn"
    HealthPage {
        HealthHeader("Chào ${name}!", "Chăm sóc sức khỏe từ những điều nhỏ mỗi ngày.", illustrated = true)
        DateStrip(vm)
        if (!state.loading && state.error == null && state.profile == null) HealthCard("Thiết lập hồ sơ") {
            Text("Nhập hồ sơ để tính BMI, TDEE và mục tiêu dinh dưỡng.", color = Muted)
            Button(enabled = !state.busy, onClick = { navigate("profile") }) { Text("Nhập hồ sơ") }
        }
        state.dashboard?.let { day ->
            CalorieSummaryCard(day)
            HomeMacroCard(state) { navigate("macros") }
            HomeWaterCard(vm)
        }
        Text("Tác vụ nhanh", color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalorieSummaryCard(day: DashboardDto) {
    val model = caloriePresentation(day)
    val accent = if (model.remaining != null && model.remaining < 0) Color(0xFFCF5F35) else Mint
    Surface(shape = RoundedCornerShape(26.dp), color = Color.White, shadowElevation = 3.dp) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFD8F8E9), Color.White)))) {
            Icon(Icons.Default.LocalFireDepartment, null, tint = Mint.copy(alpha = 0.08f),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(130.dp))
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFFA632))
                    Text(if (model.remaining != null && model.remaining < 0) "Vượt ngân sách calo" else "Calo còn lại",
                        color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(model.remaining?.let { abs(it).uiNumber() } ?: "—", color = accent,
                        fontSize = 46.sp, lineHeight = 54.sp, fontWeight = FontWeight.ExtraBold)
                    Text("kcal", Modifier.padding(top = 21.dp), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Mục tiêu: ${day.calorie_target?.uiNumber() ?: "Chưa có"} kcal", color = Ink)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tiến độ đã ăn", Modifier.weight(1f), color = Muted, style = MaterialTheme.typography.labelMedium)
                    val pct = model.percent
                    Text(pct?.let { "${it.uiNumber(if (it > 0 && it < 1) 2 else 0)}%" } ?: "Chưa có mục tiêu",
                        color = Ink, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { model.progress ?: 0f }, modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = if (model.aboveFoodTarget) Color(0xFFF29B4A) else Mint, trackColor = Color(0xFFE8EDF4))
                HorizontalDivider(color = Border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatMetric("Đã ăn", day.calories_consumed.uiNumber(), "kcal", Icons.Default.Restaurant, Modifier.weight(1f))
                    StatMetric("Vận động", day.calories_burned.uiNumber(), "kcal", Icons.Default.DirectionsRun, Modifier.weight(1f))
                }
                Text("Còn lại = mục tiêu − đã ăn + vận động. Thanh tiến độ theo lượng đã ăn.",
                    color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun HomeMacroCard(state: AppState, onDetails: () -> Unit) {
    val macro = state.dashboard?.macros ?: return
    val goals = state.nutrition ?: return
    HealthCard("Tổng quan dinh dưỡng", "Chi tiết ›", if (state.busy) null else onDetails) {
        val rows = listOf(
            Triple("Protein", macro.protein_g to goals.protein_g, Color(0xFF2496EA)),
            Triple("Carb", macro.carb_g to goals.carb_g, Color(0xFFFF9B34)),
            Triple("Fat", macro.fat_g to goals.fat_g, Color(0xFFA86CF1))
        )
        val largeText = LocalDensity.current.fontScale > 1.3f
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (largeText || maxWidth < 280.dp) 1 else 3
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (label, amounts, color) ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(label, color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(if (amounts.second > 0) "${amounts.first.uiNumber()} / ${amounts.second.uiNumber()} g"
                                    else "${amounts.first.uiNumber()} g", color = Muted, fontSize = 12.sp)
                                LinearProgressIndicator(progress = { if (amounts.second > 0) (amounts.first / amounts.second).toFloat().coerceIn(0f, 1f) else 0f },
                                    modifier = Modifier.fillMaxWidth().height(7.dp), color = color, trackColor = Border)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeWaterCard(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val settings = state.water ?: return
    val day = state.dashboard ?: return
    val enabled = !state.busy && !state.loading && !state.uncertain
    HealthCard("Uống nước") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(42.dp).background(Color(0xFFE5F6FF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.WaterDrop, null, tint = Color(0xFF35A9EE), modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("${(day.water_ml / 1000.0).uiNumber(2)} / ${(settings.effective_target_ml / 1000.0).uiNumber(2)} L",
                    color = Ink, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { (day.water_ml.toFloat() / settings.effective_target_ml.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(9.dp), color = Color(0xFF35A9EE), trackColor = Color(0xFFE3F2FD))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${settings.serving_ml} ml / lần", Modifier.weight(1f), color = Muted, style = MaterialTheme.typography.bodySmall)
            FilledTonalIconButton(enabled = enabled && day.water_ml > 0, onClick = {
                vm.write { ctx -> vm.api.adjustWater(ctx.owner, mapOf("log_date" to ctx.date, "amount_ml" to -settings.serving_ml)) }
            }) { Icon(Icons.Default.Remove, "Giảm một ly nước") }
            FilledTonalIconButton(enabled = enabled, onClick = {
                vm.write { ctx -> vm.api.adjustWater(ctx.owner, mapOf("log_date" to ctx.date, "amount_ml" to settings.serving_ml)) }
            }) { Icon(Icons.Default.Add, "Thêm một ly nước") }
        }
    }
}
