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
import com.app_health.app.core.network.ApiFailure
import com.app_health.app.core.network.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.math.abs

private fun savedMealMatches(saved: SavedMealDto, name: String, payloadItems: List<Map<String, Any?>>): Boolean =
    saved.name == name && saved.items.size == payloadItems.size &&
        saved.items.zip(payloadItems).all { (actual, expected) ->
            actual.food_id == expected["food_id"] &&
                abs(actual.grams - (expected["grams"] as Double)) < 0.03 &&
                (if (actual.food_id != null) true else actual.food_name == expected["food_name"] &&
                    listOf(
                        actual.calories_per_100g to "calories_per_100g",
                        actual.protein_per_100g to "protein_per_100g",
                        actual.carb_per_100g to "carb_per_100g",
                        actual.fat_per_100g to "fat_per_100g",
                    ).all { (number, field) -> abs(number - (expected[field] as Double)) < 0.03 })
        }

private suspend fun mealLogIdsBeforeWrite(vm: AppViewModel, ctx: RequestContext): Set<Long> = try {
    retryTransientRead(checkCurrent = { if (!vm.valid(ctx)) throw CancellationException("Account changed") }) {
        vm.api.dashboard(ctx.owner, ctx.date).food_logs.map { it.id }.toSet()
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    throw ApiFailure(503, "Chưa kiểm tra được nhật ký trước khi thêm bữa. Hãy thử lại.")
}

private suspend fun savedMealIdsBeforeWrite(vm: AppViewModel, ctx: RequestContext): Set<Long> = try {
    retryTransientRead(checkCurrent = { if (!vm.valid(ctx)) throw CancellationException("Account changed") }) {
        vm.api.meals(ctx.owner).map { it.id }.toSet()
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    throw ApiFailure(503, "Chưa kiểm tra được bữa đã lưu trước khi tạo. Hãy thử lại.")
}

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
        Button(enabled = enabled, onClick = { creating = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Tạo thực đơn dinh dưỡng mới")
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

        if (state.meals.isNotEmpty()) Text("Bữa ăn đã lưu", color = Ink,
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        state.meals.forEach { saved ->
            HealthCard(title = saved.name) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    UiIllustration(Icons.Default.RestaurantMenu, Modifier.size(58.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("${saved.items.size} món • ${saved.items.sumOf { it.grams }.display()} g", color = Muted)
                        Text("${saved.items.sumOf { it.calories_per_100g * it.grams / 100 }.display()} kcal",
                            color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = RoundedCornerShape(18.dp), color = PaleMint) {
                        Text("Đã lưu", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Mint,
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                saved.items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        val catalog = item.food_id?.let { id ->
                            state.foods.firstOrNull { it.id == id && !it.is_custom } ?: FoodDto(
                                id, item.food_name, item.calories_per_100g, item.protein_per_100g,
                                item.carb_per_100g, item.fat_per_100g, has_image = true)
                        }
                        if (catalog != null) FoodPhoto(vm, catalog, Modifier.size(42.dp))
                        else Icon(Icons.Default.Restaurant, null, Modifier.size(28.dp), tint = Mint)
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
                Button(enabled = enabled && validMealTime(time), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint), onClick = {
                    val chosenMeal = meal
                    val chosenTime = time
                    var beforeIds: Set<Long>? = null
                    var postStarted = false
                    vm.write(reconcile = { ctx ->
                        val previous = beforeIds
                        if (!postStarted || previous == null) false else {
                            val newLogs = vm.api.dashboard(ctx.owner, ctx.date).food_logs.filter {
                                it.id !in previous && it.meal_type == chosenMeal && it.meal_time == chosenTime
                            }.toMutableList()
                            saved.items.all { item ->
                                val index = newLogs.indexOfFirst { log ->
                                    log.food_name == item.food_name && abs(log.grams - item.grams) < 0.02 &&
                                        abs(log.calories_kcal - item.calories_per_100g * item.grams / 100) < 0.06
                                }
                                if (index < 0) false else { newLogs.removeAt(index); true }
                            }
                        }
                    }) { ctx ->
                        beforeIds = mealLogIdsBeforeWrite(vm, ctx)
                        postStarted = true
                        vm.api.logMeal(ctx.owner, saved.id,
                            mapOf("log_date" to ctx.date, "meal_type" to chosenMeal, "meal_time" to chosenTime))
                    }
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
                        val food = state.foods.firstOrNull { it.name == log.food_name &&
                            (log.food_id == null || it.id == log.food_id) }
                        if (food != null) FoodPhoto(vm, food, Modifier.size(42.dp))
                        else Icon(Icons.Default.Restaurant, null, Modifier.size(32.dp), tint = Mint)
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint), onClick = {
                    val items = selected.map { log ->
                        mapOf("food_id" to log.food_id, "food_name" to log.food_name, "grams" to log.grams,
                            "calories_per_100g" to log.calories_kcal * 100 / log.grams,
                            "protein_per_100g" to log.protein_g * 100 / log.grams,
                            "carb_per_100g" to log.carb_g * 100 / log.grams,
                            "fat_per_100g" to log.fat_g * 100 / log.grams)
                    }
                    val mealName = name.trim()
                    var beforeIds: Set<Long>? = null
                    var postStarted = false
                    vm.write(success = { name = "" }, reconcile = { ctx ->
                        val found = postStarted && beforeIds != null && vm.api.meals(ctx.owner).any {
                            it.id !in beforeIds!! && savedMealMatches(it, mealName, items)
                        }
                        if (found) name = ""
                        found
                    }) { ctx ->
                        beforeIds = savedMealIdsBeforeWrite(vm, ctx)
                        postStarted = true
                        vm.api.saveMeal(ctx.owner, mapOf("name" to mealName, "items" to items))
                    }
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
                vm.write(success = { deleting = null }, reconcile = { ctx ->
                    val gone = vm.api.meals(ctx.owner).none { it.id == id }
                    if (gone) deleting = null
                    gone
                }) { ctx -> vm.api.deleteMeal(ctx.owner, id) }
            }) { Text("Xóa", color = UiDanger) } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = { deleting = null }) { Text("Hủy") } })
    }
}

private data class MealDraft(val foodId: Long?, val name: String, val grams: String, val calories: Double,
    val protein: Double, val carbs: Double, val fat: Double) {
    fun payload(): Map<String, Any?> = mapOf("food_id" to foodId, "food_name" to name,
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
        saved?.items?.forEach { item -> add(MealDraft(item.food_id, item.food_name, uiEditNumber(item.grams),
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
            Button(enabled = enabled && valid, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint), onClick = {
                val mealName = name.trim()
                val items = rows.map { it.payload() }
                val payload = mapOf("name" to mealName, "items" to items)
                var beforeIds: Set<Long>? = null
                var postStarted = false
                vm.write(success = onClose, reconcile = { ctx ->
                    val found = vm.api.meals(ctx.owner).any { item ->
                        (if (saved == null) postStarted && beforeIds?.contains(item.id) == false
                            else item.id == saved.id) && savedMealMatches(item, mealName, items)
                    }
                    if (found) onClose()
                    found
                }) { ctx ->
                    if (saved == null) {
                        beforeIds = savedMealIdsBeforeWrite(vm, ctx)
                        postStarted = true
                        vm.api.saveMeal(ctx.owner, payload)
                    }
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
                        rows.add(MealDraft(if (food.is_custom) null else food.id, food.name, "100", food.calories_per_100g,
                            food.protein_per_100g, food.carb_per_100g, food.fat_per_100g))
                    }) { Icon(Icons.Default.Add, "Thêm ${food.name}") }
                }
            }
        }
        Text("Muốn tạo món riêng? Vào Trang chủ → Tìm món ăn → Tạo món, rồi quay lại đây.",
            color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}
