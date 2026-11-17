package com.app_health.app.feature.integrated

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.app_health.app.core.network.retryTransientRead
import com.app_health.app.core.network.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

fun mealsFeature() = AppFeature("meals", "Thực đơn đã lưu", load = { vm, ctx ->
    val meals = vm.api.meals(ctx.owner)
    vm.publish(ctx) { it.copy(meals = meals) }
}) { vm, navigate -> MealsScreen(vm, navigate) }

@Composable
fun MealsScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var meal by rememberSaveable { mutableStateOf("snack") }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<SavedMealDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    val logs = state.dashboard?.food_logs ?: emptyList()
    var chosen by remember(state.date, logs.map { it.id }) { mutableStateOf(logs.map { it.id }.toSet()) }
    val selected = logs.filter { it.id in chosen }
    val enabled = !state.busy && !state.loading && !state.uncertain
    val largeText = LocalDensity.current.fontScale > 1.3f

    if (creating || editing != null) {
        MealComposerScreen(vm, editing, onClose = { creating = false; editing = null })
        return
    }

    HealthPage {
        HealthHeader("Thực đơn đã lưu", "Lưu và quản lý các bữa ăn yêu thích của bạn", Icons.Default.Bookmarks,
            onBack = { if (!state.busy) navigate("home") })
        Button(enabled = enabled, onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Tạo bữa ăn từ các món của tôi")
        }
        DateStrip(vm)
        HealthCard(title = "Chọn bữa ăn & giờ ăn") {
            Text("Dùng cho lần thêm nhanh vào nhật ký ngày ${uiDate(state.date.toString())}.",
                color = Muted, style = MaterialTheme.typography.bodySmall)
            UiMealPicker(meal, time, enabled, { meal = it }, { time = it })
        }

        if (!state.loading && state.error == null && state.meals.isEmpty()) {
            HealthCard(title = "Bữa ăn yêu thích") {
                UiEmptyState(Icons.Default.BookmarkBorder, "Chưa có bữa ăn đã lưu",
                    "Tự chọn món từ danh mục hoặc lưu các món trong nhật ký bên dưới.", "Tạo bữa ăn", enabled) { creating = true }
            }
        }

        state.meals.forEach { saved ->
            HealthCard(title = saved.name) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    UiIllustration(Icons.Default.RestaurantMenu)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("${saved.items.size} món • ${saved.items.sumOf { it.grams }.display()} g", color = Muted)
                        Text("${saved.items.sumOf { it.calories_per_100g * it.grams / 100 }.display()} kcal",
                            color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
                saved.items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.food_name, Modifier.weight(1f), color = Ink)
                        Text("${item.grams.display()} g", color = Muted)
                    }
                }
                val protein = saved.items.sumOf { it.protein_per_100g * it.grams / 100 }
                val carbs = saved.items.sumOf { it.carb_per_100g * it.grams / 100 }
                val fat = saved.items.sumOf { it.fat_per_100g * it.grams / 100 }
                Text("Protein ${protein.display()} g • Carb ${carbs.display()} g • Fat ${fat.display()} g",
                    color = Muted, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = Border)
                Button(enabled = enabled && validMealTime(time), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                    vm.write { ctx -> vm.api.logMeal(ctx.owner, saved.id,
                        mapOf("log_date" to ctx.date, "meal_type" to meal, "meal_time" to time)) }
                }) { Icon(Icons.Default.Bolt, null); Spacer(Modifier.width(6.dp)); Text("Thêm nhanh vào nhật ký") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(enabled = enabled, modifier = Modifier.weight(1f), onClick = {
                        editing = saved
                    }, border = BorderStroke(1.dp, Border)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Sửa")
                    }
                    OutlinedButton(enabled = enabled, modifier = Modifier.weight(1f), onClick = { deleting = saved.id },
                        border = BorderStroke(1.dp, Border), colors = ButtonDefaults.outlinedButtonColors(contentColor = UiDanger)) {
                        Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Xóa")
                    }
                }
            }
        }

        HealthCard(title = "Lưu bữa từ nhật ký ngày đã chọn") {
            Text("Lưu các món của ngày ${uiDate(state.date.toString())} để dùng lại vào lần sau.",
                color = Muted, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(name, { name = it.take(120) }, label = { Text("Tên bữa ăn") },
                placeholder = { Text("Ví dụ: Bữa sáng yêu thích") }, enabled = enabled,
                singleLine = true, modifier = Modifier.fillMaxWidth())
            logs.forEach { log ->
                val checked = log.id in chosen
                Surface(shape = RoundedCornerShape(16.dp), color = if (checked) PaleMint else Color.White,
                    border = BorderStroke(1.dp, if (checked) Mint.copy(alpha = 0.25f) else Border)) {
                    Row(Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Checkbox,
                        onValueChange = { value -> chosen = if (value) chosen + log.id else chosen - log.id })
                        .padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, onCheckedChange = null, enabled = enabled,
                            colors = CheckboxDefaults.colors(checkedColor = Mint))
                        Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(log.food_name, color = Ink, fontWeight = FontWeight.SemiBold)
                            Text("${log.grams.display()} g • ${log.calories_kcal.display()} kcal",
                                color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (logs.isEmpty() && !state.loading && state.error == null) {
                Text("Ngày này chưa có món ăn để lưu thành bữa.", color = Muted)
                OutlinedButton(onClick = { navigate("food") }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("Thêm món vào nhật ký")
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = PaleMint) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
                    val total = selected.sumOf { it.calories_kcal }.display()
                    if (largeText) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatMetric("Đã chọn", selected.size.toString(), "món", Icons.Default.Restaurant)
                        StatMetric("Tổng calo", total, "kcal", Icons.Default.LocalFireDepartment)
                        Text("Giờ thêm vào nhật ký: $time", color = Muted, style = MaterialTheme.typography.bodySmall)
                    } else if (maxWidth < 280.dp) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatMetric("Đã chọn", selected.size.toString(), "món", Icons.Default.Restaurant, Modifier.weight(1f))
                            StatMetric("Tổng calo", total, "kcal", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
                        }
                        Text("Giờ thêm vào nhật ký: $time", color = Muted, style = MaterialTheme.typography.bodySmall)
                    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatMetric("Đã chọn", selected.size.toString(), "món", Icons.Default.Restaurant, Modifier.weight(1f))
                        StatMetric("Tổng calo", total, "kcal", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
                        StatMetric("Giờ ăn", time, icon = Icons.Default.AccessTime, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (selected.size > 30) Text("Mỗi bữa tối đa 30 món. Bỏ chọn một số món để tiếp tục.", color = UiDanger)
            Button(enabled = enabled && name.isNotBlank() && selected.isNotEmpty() && selected.size <= 30,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), onClick = {
                    val items = selected.map { log ->
                        mapOf("food_id" to null, "food_name" to log.food_name, "grams" to log.grams,
                            "calories_per_100g" to log.calories_kcal * 100 / log.grams,
                            "protein_per_100g" to log.protein_g * 100 / log.grams,
                            "carb_per_100g" to log.carb_g * 100 / log.grams,
                            "fat_per_100g" to log.fat_g * 100 / log.grams)
                    }
                    vm.write(success = { name = "" }) { ctx -> vm.api.saveMeal(ctx.owner, mapOf("name" to name.trim(), "items" to items)) }
                }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Lưu bữa ăn") }
        }

        HealthCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                UiIllustration(Icons.Default.Lightbulb)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mẹo nhỏ", color = Mint, fontWeight = FontWeight.Bold)
                    Text("Lưu những bữa ăn thường dùng để ghi nhật ký nhanh hơn mỗi ngày.",
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    deleting?.let { id ->
        AlertDialog(onDismissRequest = { if (!state.busy) deleting = null }, title = { Text("Xóa bữa đã lưu?") },
            text = { Text("Các món đã ghi vào nhật ký vẫn được giữ lại.") },
            confirmButton = { TextButton(enabled = enabled, onClick = {
                vm.write(success = { deleting = null }) { ctx -> vm.api.deleteMeal(ctx.owner, id) }
            }) { Text("Xóa", color = UiDanger) } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = { deleting = null }) { Text("Hủy") } })
    }
}

private data class MealDraft(val name: String, val grams: String, val calories: Double,
    val protein: Double, val carbs: Double, val fat: Double) {
    fun payload(): Map<String, Any?> = mapOf("food_id" to null, "food_name" to name,
        "grams" to inputNumber(grams)!!, "calories_per_100g" to calories,
        "protein_per_100g" to protein, "carb_per_100g" to carbs, "fat_per_100g" to fat)
}

@Composable
private fun MealComposerScreen(vm: AppViewModel, saved: SavedMealDto?, onClose: () -> Unit) {
    val state by vm.state.collectAsState()
    var name by remember(saved?.id) { mutableStateOf(saved?.name ?: "") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodDto>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val rows = remember(saved?.id) { mutableStateListOf<MealDraft>().apply {
        saved?.items?.forEach { item -> add(MealDraft(item.food_name, uiEditNumber(item.grams),
            item.calories_per_100g, item.protein_per_100g, item.carb_per_100g, item.fat_per_100g)) }
    } }
    val enabled = !state.busy && !state.loading && !state.uncertain
    LaunchedEffect(query, state.readRevision, state.route, state.busy, state.loading) {
        if (state.busy || state.loading || state.route != "meals") return@LaunchedEffect
        searching = true
        delay(250)
        val ctx = vm.context()
        try {
            val found = retryTransientRead(checkCurrent = {
                if (!vm.valid(ctx)) throw CancellationException("Screen changed")
            }) { vm.api.foods(ctx.owner, query.trim()) }
            if (vm.valid(ctx)) results = found
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { currentCoroutineContext().ensureActive(); vm.showError(e.toUserMessage(), ctx)
        } finally { searching = false }
    }
    val valid = name.isNotBlank() && rows.size in 1..30 && rows.all {
        inputNumber(it.grams)?.let { n -> n in 0.01..10000.0 } == true
    }
    HealthPage {
        HealthHeader(if (saved == null) "Tạo bữa ăn" else "Sửa bữa ăn", "Chọn món và khẩu phần cho công thức của bạn",
            Icons.Default.RestaurantMenu, onBack = { if (enabled) onClose() })
        OutlinedTextField(name, { name = it.take(120) }, label = { Text("Tên bữa ăn") },
            singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
        HealthCard(title = "Các món đã chọn (${rows.size}/30)") {
            if (rows.isEmpty()) Text("Tìm món bên dưới rồi bấm Thêm.", color = Muted)
            rows.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, color = Ink, fontWeight = FontWeight.SemiBold)
                        NumberInput("Khẩu phần (g)", item.grams, enabled) { value ->
                            rows[index] = item.copy(grams = value)
                        }
                    }
                    IconButton(onClick = { rows.removeAt(index) }, enabled = enabled) {
                        Icon(Icons.Default.DeleteOutline, "Bỏ ${item.name}", tint = UiDanger)
                    }
                }
                Text("${(item.calories * (inputNumber(item.grams) ?: 0.0) / 100).display()} kcal", color = Muted)
                HorizontalDivider(color = Border)
            }
            Button(enabled = enabled && valid, modifier = Modifier.fillMaxWidth(), onClick = {
                val payload = mapOf("name" to name.trim(), "items" to rows.map { it.payload() })
                vm.write(success = onClose) { ctx ->
                    if (saved == null) vm.api.saveMeal(ctx.owner, payload)
                    else vm.api.editMeal(ctx.owner, saved.id, payload)
                }
            }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Lưu bữa ăn") }
        }
        HealthCard(title = "Thêm món từ danh mục") {
            OutlinedTextField(query, { query = it.take(100) }, label = { Text("Tìm món") },
                enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!searching && results.isEmpty()) Text("Không tìm thấy món phù hợp.", color = Muted)
            results.forEach { food ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodPhoto(vm, food, Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(food.name, color = Ink)
                        Text("${food.calories_per_100g.display()} kcal / 100 g", color = Muted)
                    }
                    IconButton(enabled = enabled && rows.size < 30, onClick = {
                        rows.add(MealDraft(food.name, "100", food.calories_per_100g,
                            food.protein_per_100g, food.carb_per_100g, food.fat_per_100g))
                    }) { Icon(Icons.Default.Add, "Thêm ${food.name}") }
                }
            }
        }
        Text("Muốn tạo món riêng? Vào Trang chủ → Tìm món ăn → Tạo món, rồi quay lại đây.",
            color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}
