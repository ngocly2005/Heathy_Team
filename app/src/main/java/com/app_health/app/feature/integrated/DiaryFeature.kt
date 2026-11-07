package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

fun diaryFeature() = AppFeature("diary", "Nhật ký", load = { vm, ctx ->
    val daily = vm.api.daily(ctx.owner, ctx.date)
    vm.publish(ctx) { it.copy(daily = daily, dashboard = daily.dashboard) }
}) { vm, navigate -> DiaryScreen(vm, navigate) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val day = state.daily
    var editing by rememberSaveable(state.date) { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable(state.date) { mutableStateOf<String?>(null) }
    var itemId by rememberSaveable(state.date) { mutableLongStateOf(0L) }
    var amount by rememberSaveable(state.date) { mutableStateOf("") }
    var meal by rememberSaveable(state.date) { mutableStateOf("snack") }
    var time by rememberSaveable(state.date) { mutableStateOf("") }
    var note by rememberSaveable(state.date, day?.diary?.content) { mutableStateOf(day?.diary?.content ?: "") }
    var mood by rememberSaveable(state.date, day?.diary?.mood) { mutableStateOf(day?.diary?.mood ?: "") }
    var waterDetails by rememberSaveable(state.date) { mutableStateOf(false) }
    val enabled = !state.busy && !state.loading && !state.uncertain
    val largeText = LocalDensity.current.fontScale > 1.3f

    HealthPage {
        HealthHeader("Nhật ký", "Ghi lại hành trình sức khỏe của bạn", Icons.Default.CalendarMonth, illustrated = true)
        DateStrip(vm)
        if (day == null) {
            HealthCard {
                Text(if (state.loading) "Đang tải nhật ký…" else "Chưa tải được dữ liệu ngày đã chọn.", color = Muted)
                if (!state.loading) OutlinedButton(onClick = { vm.refresh() }, enabled = !state.busy) { Text("Thử tải lại") }
            }
        } else {
            HealthCard(title = if (state.date == LocalDate.now()) "Tổng quan ngày hôm nay" else "Tổng quan ngày ${uiDate(state.date.toString())}") {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (largeText) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            StatMetric("Đã ăn", day.dashboard.calories_consumed.display(), "kcal", Icons.Default.Restaurant)
                            StatMetric("Vận động", day.dashboard.calories_burned.display(), "kcal", Icons.Default.DirectionsRun)
                            StatMetric("Nước uống", day.dashboard.water_ml.toString(), "ml", Icons.Default.WaterDrop)
                        }
                    } else if (maxWidth < 300.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatMetric("Đã ăn", day.dashboard.calories_consumed.display(), "kcal", Icons.Default.Restaurant, Modifier.weight(1f))
                                StatMetric("Vận động", day.dashboard.calories_burned.display(), "kcal", Icons.Default.DirectionsRun, Modifier.weight(1f))
                            }
                            StatMetric("Nước uống", day.dashboard.water_ml.toString(), "ml", Icons.Default.WaterDrop)
                        }
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatMetric("Đã ăn", day.dashboard.calories_consumed.display(), "kcal", Icons.Default.Restaurant, Modifier.weight(1f))
                        StatMetric("Vận động", day.dashboard.calories_burned.display(), "kcal", Icons.Default.DirectionsRun, Modifier.weight(1f))
                        StatMetric("Nước uống", day.dashboard.water_ml.toString(), "ml", Icons.Default.WaterDrop, Modifier.weight(1f))
                    }
                }
                day.weight?.let { Text("Số đo ngày này: ${it.weight_kg.display()} kg", color = Muted, style = MaterialTheme.typography.bodySmall) }
            }

            HealthCard(title = "Món ăn", actionLabel = if (!state.busy) "Thêm món" else null, onAction = { navigate("food") }) {
                if (day.dashboard.food_logs.isEmpty()) {
                    UiEmptyState(Icons.Default.Restaurant, "Chưa có món ăn trong ngày", "Thêm món ăn để theo dõi dinh dưỡng.",
                        "Thêm món ăn", enabled) { navigate("food") }
                } else {
                    day.dashboard.food_logs.sortedWith(compareBy<FoodLogDto> { it.meal_time ?: "99:99" }.thenBy { it.id }).forEachIndexed { index, food ->
                        if (index > 0) HorizontalDivider(color = Border)
                        Text(food.food_name, color = Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        val mealLabel = mealChoices.firstOrNull { it.first == food.meal_type }?.second ?: food.meal_type
                        Text("$mealLabel${food.meal_time?.let { " • $it" } ?: ""} • ${food.grams.display()} g", color = Muted)
                        Text("${food.calories_kcal.display()} kcal", color = Mint, fontWeight = FontWeight.Bold)
                        Text("Protein ${food.protein_g.display()} g • Carb ${food.carb_g.display()} g • Fat ${food.fat_g.display()} g",
                            color = Muted, style = MaterialTheme.typography.bodySmall)
                        UiEntryActions(enabled, onEdit = {
                            itemId = food.id; amount = uiEditNumber(food.grams); meal = food.meal_type
                            time = food.meal_time ?: ""; editing = "food"
                        }, onDelete = { itemId = food.id; deleting = "food" })
                    }
                    OutlinedButton(onClick = { navigate("food") }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Thêm món ăn")
                    }
                }
            }

            HealthCard(title = "Vận động", actionLabel = if (!state.busy) "Thêm" else null, onAction = { navigate("activity") }) {
                if (day.dashboard.activity_logs.isEmpty()) {
                    UiEmptyState(Icons.Default.FitnessCenter, "Chưa có hoạt động nào", "Ghi lại bài tập để theo dõi năng lượng vận động.",
                        "Thêm vận động", enabled) { navigate("activity") }
                } else {
                    day.dashboard.activity_logs.forEachIndexed { index, activity ->
                        if (index > 0) HorizontalDivider(color = Border)
                        Text(activity.activity_name, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${activity.duration_minutes} phút • ${activity.calories_burned.display()} kcal", color = Muted)
                        UiEntryActions(enabled, onEdit = {
                            itemId = activity.id; amount = activity.duration_minutes.toString(); editing = "activity"
                        }, onDelete = { itemId = activity.id; deleting = "activity" })
                    }
                }
            }

            val water = state.water
            val target = water?.effective_target_ml
            val total = day.dashboard.water_ml
            HealthCard(title = "Nước uống", actionLabel = if (day.water_logs.isNotEmpty()) if (waterDetails) "Thu gọn" else "Chi tiết" else null,
                onAction = { waterDetails = !waterDetails }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    UiIllustration(Icons.Default.WaterDrop)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$total ml", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(target?.let { "/ $it ml mục tiêu" } ?: "Chưa tải được mục tiêu nước", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                LinearProgressIndicator(
                    progress = { if (target != null && target > 0) (total.toFloat() / target).coerceIn(0f, 1f) else 0f },
                    color = Color(0xFF34A8F5), trackColor = Color(0xFFE2F3FF), modifier = Modifier.fillMaxWidth().height(10.dp))
                target?.takeIf { it > 0 }?.let {
                    Text("${(total.toDouble() * 100 / it).toInt()}% mục tiêu", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(enabled = enabled && water != null && total > 0, modifier = Modifier.weight(1f),
                        onClick = { water?.let { setting -> vm.write { ctx ->
                            vm.api.adjustWater(ctx.owner, mapOf("log_date" to ctx.date, "amount_ml" to -setting.serving_ml))
                        } } }) {
                        Icon(Icons.Default.Remove, null); Spacer(Modifier.width(5.dp)); Text("${water?.serving_ml ?: "—"} ml")
                    }
                    Button(enabled = enabled && water != null, modifier = Modifier.weight(1f),
                        onClick = { water?.let { setting -> vm.write { ctx ->
                            vm.api.adjustWater(ctx.owner, mapOf("log_date" to ctx.date, "amount_ml" to setting.serving_ml))
                        } } }) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("${water?.serving_ml ?: "—"} ml")
                    }
                }
                OutlinedButton(onClick = { navigate("water") }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Cài đặt & ghi thêm nước") }
                if (waterDetails) day.water_logs.forEach { log ->
                    HorizontalDivider(color = Border)
                    Text("${log.amount_ml} ml", color = Ink, fontWeight = FontWeight.SemiBold)
                    UiEntryActions(enabled, onEdit = { itemId = log.id; amount = log.amount_ml.toString(); editing = "water" },
                        onDelete = { itemId = log.id; deleting = "water" })
                }
            }

            HealthCard(title = "Ghi chú & tâm trạng") {
                OutlinedTextField(note, { note = it.take(10000) }, label = { Text("Hôm nay bạn có gì đặc biệt?") },
                    enabled = enabled, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
                Text("Tâm trạng hôm nay", color = Ink, fontWeight = FontWeight.SemiBold)
                val moods = listOf("Rất vui" to "😁", "Vui" to "😊", "Bình thường" to "😐", "Buồn" to "😔", "Mệt" to "😣")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    moods.forEach { (value, emoji) ->
                        FilterChip(selected = mood == value, onClick = { mood = if (mood == value) "" else value },
                            enabled = enabled, label = { Text("$emoji $value") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaleMint, selectedLabelColor = Mint))
                    }
                }
                if (mood.isNotBlank() && moods.none { it.first == mood }) {
                    OutlinedTextField(mood, { mood = it.take(30) }, label = { Text("Tâm trạng đã lưu") }, enabled = enabled, modifier = Modifier.fillMaxWidth())
                }
                if (note.isBlank()) Text("Viết ghi chú để lưu cùng tâm trạng.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Button(enabled = enabled && note.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                    vm.write { ctx -> vm.api.saveDiary(ctx.owner, ctx.date, mapOf("content" to note.trim(), "mood" to mood.trim().ifBlank { null })) }
                }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Lưu ghi chú") }
                if (day.diary != null) TextButton(enabled = enabled, onClick = { deleting = "note" },
                    colors = ButtonDefaults.textButtonColors(contentColor = UiDanger)) { Text("Xóa ghi chú") }
            }
        }
    }

    editing?.let { kind ->
        val number = inputNumber(amount)
        val valid = when (kind) {
            "food" -> number != null && number >= 0.01 && number <= 10000 && (time.isBlank() || validMealTime(time))
            "activity" -> amount.toIntOrNull()?.let { it in 1..1440 } == true
            else -> amount.toIntOrNull()?.let { it in 1..5000 } == true
        }
        AlertDialog(onDismissRequest = { if (!state.busy) editing = null }, title = { Text("Sửa mục nhật ký") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberInput(when (kind) { "food" -> "Khẩu phần (g)"; "activity" -> "Thời gian (phút)"; else -> "Lượng nước (ml)" }, amount, enabled) { amount = it }
                if (kind == "food") UiMealPicker(meal, time, enabled, { meal = it }, { time = it }, allowEmptyTime = true)
            } },
            confirmButton = { TextButton(enabled = enabled && valid, onClick = {
                vm.write(success = { editing = null }) { ctx -> when (kind) {
                    "food" -> vm.api.editFood(ctx.owner, itemId, mapOf("grams" to number, "meal_type" to meal, "meal_time" to time.ifBlank { null }))
                    "activity" -> vm.api.editActivity(ctx.owner, itemId, mapOf("duration_minutes" to amount.toInt()))
                    else -> vm.api.editWater(ctx.owner, itemId, mapOf("amount_ml" to amount.toInt()))
                } }
            }) { Text("Lưu") } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = { editing = null }) { Text("Hủy") } })
    }
    deleting?.let { kind ->
        AlertDialog(onDismissRequest = { if (!state.busy) deleting = null }, title = { Text("Xóa mục đã chọn?") },
            text = { Text("Dữ liệu tổng quan của ngày sẽ được cập nhật sau khi xóa.") },
            confirmButton = { TextButton(enabled = enabled, onClick = {
                vm.write(success = { deleting = null }) { ctx -> when (kind) {
                    "food" -> vm.api.deleteFood(ctx.owner, itemId)
                    "activity" -> vm.api.deleteActivity(ctx.owner, itemId)
                    "water" -> vm.api.deleteWater(ctx.owner, itemId)
                    else -> vm.api.deleteDiary(ctx.owner, ctx.date)
                } }
            }) { Text("Xóa", color = UiDanger) } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = { deleting = null }) { Text("Hủy") } })
    }
}
