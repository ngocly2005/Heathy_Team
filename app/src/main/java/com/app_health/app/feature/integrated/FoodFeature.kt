package com.app_health.app.feature.integrated

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app_health.app.core.network.retryTransientRead
import com.app_health.app.core.network.ApiFailure
import com.app_health.app.core.network.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

fun foodFeature() = AppFeature("food", "Tìm món ăn") { vm, navigate -> FoodScreen(vm, navigate) }

private fun sameFoodNutrition(food: FoodDto, payload: Map<String, Any?>): Boolean =
    food.name == payload["name"] && listOf(
        food.calories_per_100g to "calories_per_100g",
        food.protein_per_100g to "protein_per_100g",
        food.carb_per_100g to "carb_per_100g",
        food.fat_per_100g to "fat_per_100g",
    ).all { (actual, field) -> abs(actual - (payload[field] as Double)) < 0.02 }

private suspend fun foodLogIdsBeforeWrite(vm: AppViewModel, ctx: RequestContext): Set<Long> = try {
    retryTransientRead(checkCurrent = { if (!vm.valid(ctx)) throw CancellationException("Account changed") }) {
        vm.api.dashboard(ctx.owner, ctx.date).food_logs.map { it.id }.toSet()
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    // The POST has not been sent. Mark this as a known read failure, not an uncertain write.
    throw ApiFailure(503, "Chưa kiểm tra được nhật ký trước khi thêm món. Hãy thử lại.")
}

private suspend fun customFoodIdsBeforeWrite(vm: AppViewModel, ctx: RequestContext): Set<Long> = try {
    retryTransientRead(checkCurrent = { if (!vm.valid(ctx)) throw CancellationException("Account changed") }) {
        vm.api.customFoods(ctx.owner).map { it.id }.toSet()
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    throw ApiFailure(503, "Chưa kiểm tra được món cá nhân trước khi tạo. Hãy thử lại.")
}

/** The backend resizes and re-encodes JPEG, so byte hashes cannot verify the requested image. */
private fun sameRenderedFoodImage(candidate: ByteArray, served: ByteArray): Boolean {
    val first = BitmapFactory.decodeByteArray(candidate, 0, candidate.size) ?: return false
    val second = BitmapFactory.decodeByteArray(served, 0, served.size)
    if (second == null) { first.recycle(); return false }
    try {
        val firstRatio = first.width.toDouble() / first.height
        val secondRatio = second.width.toDouble() / second.height
        if (abs(firstRatio - secondRatio) > 0.02) return false
        val a = Bitmap.createScaledBitmap(first, 64, 64, true)
        val b = Bitmap.createScaledBitmap(second, 64, 64, true)
        try {
            val pixelsA = IntArray(64 * 64)
            val pixelsB = IntArray(64 * 64)
            a.getPixels(pixelsA, 0, 64, 0, 0, 64, 64)
            b.getPixels(pixelsB, 0, 64, 0, 0, 64, 64)
            var difference = 0L
            var changedPixels = 0
            for (i in pixelsA.indices) {
                val x = pixelsA[i]
                val y = pixelsB[i]
                val delta = abs((x shr 16 and 255) - (y shr 16 and 255)) +
                    abs((x shr 8 and 255) - (y shr 8 and 255)) +
                    abs((x and 255) - (y and 255))
                difference += delta
                if (delta > 60) changedPixels++
            }
            return difference <= 64L * 64 * 3 * 12 && changedPixels <= 64 * 64 / 6
        } finally {
            if (a !== first) a.recycle()
            if (b !== second) b.recycle()
        }
    } finally {
        first.recycle()
        second.recycle()
    }
}

@Composable
fun FoodScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var personalOnly by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedCustom by rememberSaveable { mutableStateOf(false) }
    var grams by rememberSaveable { mutableStateOf("100") }
    var meal by rememberSaveable { mutableStateOf("snack") }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var searching by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FoodDto?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var createdId by remember { mutableStateOf<Long?>(null) }
    var pendingHomeAfterReconcile by remember { mutableStateOf(false) }
    val selected = state.foods.firstOrNull { it.id == selectedId && it.is_custom == selectedCustom }
    val amount = inputNumber(grams)
    val enabled = !state.busy && !state.loading && !state.uncertain

    LaunchedEffect(pendingHomeAfterReconcile, state.uncertain, state.loading) {
        if (pendingHomeAfterReconcile && !state.uncertain && !state.loading) {
            pendingHomeAfterReconcile = false
            navigate("home")
        }
    }

    LaunchedEffect(query, personalOnly, state.date, state.loading, state.busy, state.readRevision, state.route) {
        if (state.loading || state.busy || state.route != "food") return@LaunchedEffect
        searching = true
        delay(300)
        val ctx = vm.context()
        try {
            val results = retryTransientRead(checkCurrent = {
                if (!vm.valid(ctx)) throw CancellationException("Screen changed")
            }) {
                if (personalOnly) vm.api.customFoods(ctx.owner).filter { it.name.contains(query.trim(), true) }
                else vm.api.foods(ctx.owner, query.trim())
            }
            vm.readSucceeded(ctx) { it.copy(foods = results) }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            vm.showError(e.toUserMessage(), ctx)
        } finally { searching = false }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val foodId = selected?.takeIf { it.is_custom }?.id ?: return@rememberLauncherForActivityResult
        if (uri != null) scope.launch {
            val ctx = vm.context()
            try {
                val bytes = withContext(Dispatchers.IO) { normalizedPhoto(context, uri) }
                if (!vm.valid(ctx)) return@launch
                val part = MultipartBody.Part.createFormData("image", "food.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaType()))
                var putStarted = false
                vm.write(reconcile = { writeCtx ->
                    if (!putStarted) false else {
                        val hasImage = vm.api.customFoods(writeCtx.owner).firstOrNull { it.id == foodId }?.has_image == true
                        if (!hasImage) false else {
                            val served = vm.api.customFoodImage(writeCtx.owner, foodId).use { it.bytes() }
                            withContext(Dispatchers.Default) { sameRenderedFoodImage(bytes, served) }
                        }
                    }
                }) { writeCtx ->
                    putStarted = true
                    vm.api.uploadCustomFoodImage(writeCtx.owner, foodId, part)
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { vm.showError(e.message ?: "Không đọc được ảnh món ăn.", ctx) }
        }
    }

    HealthPage {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = enabled, onClick = { navigate("home") }) {
                Icon(Icons.Default.ArrowBack, "Quay lại", tint = Ink)
            }
            Text("Tìm món ăn", Modifier.weight(1f), color = Ink,
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, null, tint = Mint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("${uiDate(state.date.toString())} • $time", Modifier.weight(1f), color = Muted,
                style = MaterialTheme.typography.bodySmall)
            FilledTonalButton(enabled = enabled, onClick = { createdId = null; creating = true },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = PaleMint, contentColor = Mint),
                shape = RoundedCornerShape(22.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(3.dp)); Text("Tạo món")
            }
        }
        OutlinedTextField(query, { query = it.take(100); selectedId = null },
            placeholder = { Text("Tìm thực phẩm hoặc món ăn") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = FigmaMuted) },
            singleLine = true, enabled = enabled, shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White, focusedBorderColor = FigmaGreen,
                unfocusedBorderColor = Color.Transparent), modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !personalOnly, enabled = enabled, onClick = { personalOnly = false; selectedId = null },
                label = { Text("Tất cả") }, shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(containerColor = Color.White,
                    selectedContainerColor = PaleMint, selectedLabelColor = Mint))
            FilterChip(selected = personalOnly, enabled = enabled, onClick = { personalOnly = true; selectedId = null },
                label = { Text("Tạo bởi tôi") }, shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(containerColor = Color.White,
                    selectedContainerColor = PaleMint, selectedLabelColor = Mint))
        }
        if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!searching && state.foods.isEmpty()) Text(if (personalOnly) "Bạn chưa tạo món ăn nào." else
            "Không tìm thấy thực phẩm phù hợp.", color = FigmaMuted)
        if (selected == null) {
            state.foods.forEach { food ->
                Card(onClick = { selectedId = food.id; selectedCustom = food.is_custom },
                    enabled = enabled && !searching, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Border), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FoodPhoto(vm, food, Modifier.size(58.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(food.name, color = FigmaInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("100 g • ${food.calories_per_100g.display()} kcal", color = FigmaMuted, fontSize = 12.sp)
                            FoodMacroLine(food.protein_per_100g, food.carb_per_100g, food.fat_per_100g)
                        }
                        Icon(Icons.Default.Add, "Chọn ${food.name}", tint = Mint)
                    }
                }
            }
        } else FigmaCard(selected.name) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FoodPhoto(vm, selected, Modifier.size(88.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (selected.is_custom) "Món ăn của tôi" else "Thực phẩm trong danh mục", color = FigmaMuted)
                    if (selected.is_custom) TextButton(enabled = enabled, onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (selected.has_image) "Đổi ảnh" else "Thêm ảnh") }
                }
            }
            if (selected.is_custom) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = enabled, onClick = { editing = selected }) { Text("Sửa món") }
                TextButton(enabled = enabled, onClick = { deleting = true }) { Text("Xóa món", color = UiDanger) }
                if (selected.has_image) TextButton(enabled = enabled, onClick = {
                    val foodId = selected.id
                    vm.write(reconcile = { ctx ->
                        vm.api.customFoods(ctx.owner).firstOrNull { it.id == foodId }?.has_image == false
                    }) { ctx -> vm.api.deleteCustomFoodImage(ctx.owner, foodId) }
                }) { Text("Xóa ảnh") }
            }
            NumberInput("Khẩu phần (gram)", grams, enabled) { grams = it }
            if (amount != null && amount > 0) {
                val factor = amount / 100
                Text("${(selected.calories_per_100g * factor).display()} kcal")
                FoodMacroLine(selected.protein_per_100g * factor, selected.carb_per_100g * factor,
                    selected.fat_per_100g * factor)
            }
            MealInputs(meal, time, enabled, { meal = it }, { time = it })
            Button(enabled = enabled && amount != null && amount in 0.01..10000.0 && validMealTime(time),
                onClick = {
                    val food = selected
                    val portion = amount ?: return@Button
                    val chosenMeal = meal
                    val chosenTime = time
                    var beforeIds: Set<Long>? = null
                    var postStarted = false
                    vm.write(success = { navigate("home") }, reconcile = { ctx ->
                        val before = beforeIds
                        val found = postStarted && before != null && vm.api.dashboard(ctx.owner, ctx.date).food_logs.any { log ->
                            log.id !in before && log.meal_type == chosenMeal && log.meal_time == chosenTime &&
                                abs(log.grams - portion) < 0.02 &&
                                (if (food.is_custom) log.food_name == food.name else log.food_id == food.id)
                        }
                        if (found) pendingHomeAfterReconcile = true
                        found
                    }) { ctx ->
                        beforeIds = foodLogIdsBeforeWrite(vm, ctx)
                        postStarted = true
                        vm.api.addFood(ctx.owner, mapOf(
                            (if (food.is_custom) "custom_food_id" else "food_id") to food.id,
                            "grams" to portion, "meal_type" to chosenMeal, "meal_time" to chosenTime, "log_date" to ctx.date))
                    }
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Text("Thêm vào nhật ký")
            }
            TextButton(onClick = { selectedId = null }, enabled = enabled) { Text("Chọn món khác") }
        }
    }
    if (creating || editing != null) CustomFoodEditor(editing, enabled, onDismiss = {
        creating = false; editing = null
    }, onSave = { payload ->
        val old = editing
        var beforeIds: Set<Long>? = null
        var postStarted = false
        vm.write(success = {
            creating = false; editing = null
            if (old == null && createdId != null) {
                query = ""; personalOnly = true; selectedId = createdId; selectedCustom = true
            }
        }, reconcile = { ctx ->
            val result = vm.api.customFoods(ctx.owner).firstOrNull {
                if (old == null) it.name == payload["name"] && postStarted &&
                    beforeIds?.contains(it.id) == false else it.id == old.id
            }
            val found = result != null && sameFoodNutrition(result, payload)
            if (found) {
                creating = false; editing = null
                if (old == null) {
                    createdId = result!!.id
                    query = ""; personalOnly = true; selectedId = result.id; selectedCustom = true
                }
            }
            found
        }) { ctx ->
            if (old == null) {
                beforeIds = customFoodIdsBeforeWrite(vm, ctx)
                postStarted = true
                createdId = vm.api.createCustomFood(ctx.owner, payload).id
            }
            else vm.api.editCustomFood(ctx.owner, old.id, payload)
        }
    })
    if (deleting && selected?.is_custom == true) AlertDialog(onDismissRequest = { deleting = false },
        title = { Text("Xóa món ăn của tôi?") },
        text = { Text("Nhật ký và bữa ăn đã lưu trước đây vẫn giữ số liệu cũ.") },
        confirmButton = { TextButton(enabled = enabled, onClick = {
            val foodId = selected.id
            vm.write(success = { deleting = false; selectedId = null }, reconcile = { ctx ->
                val gone = vm.api.customFoods(ctx.owner).none { it.id == foodId }
                if (gone) { deleting = false; selectedId = null }
                gone
            }) { ctx ->
                vm.api.deleteCustomFood(ctx.owner, foodId)
            }
        }) { Text("Xóa", color = UiDanger) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Hủy") } })
}

@Composable
private fun CustomFoodEditor(food: FoodDto?, enabled: Boolean, onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit) {
    var name by remember(food?.id) { mutableStateOf(food?.name ?: "") }
    var serving by remember(food?.id) { mutableStateOf("100") }
    var protein by remember(food?.id) { mutableStateOf(food?.protein_per_100g?.let(::uiEditNumber) ?: "0") }
    var carbs by remember(food?.id) { mutableStateOf(food?.carb_per_100g?.let(::uiEditNumber) ?: "0") }
    var fat by remember(food?.id) { mutableStateOf(food?.fat_per_100g?.let(::uiEditNumber) ?: "0") }
    val grams = inputNumber(serving); val p = inputNumber(protein)
    val c = inputNumber(carbs); val f = inputNumber(fat)
    val macrosValid = grams != null && grams in 0.01..10000.0 && p != null && c != null && f != null &&
        p >= 0 && c >= 0 && f >= 0 && p + c + f <= grams + 0.05
    val estimatedCalories = if (macrosValid) 4 * p!! + 4 * c!! + 9 * f!! else null
    fun perHundred(amount: Double, portion: Double): Double = BigDecimal.valueOf(amount * 100 / portion)
        .setScale(2, RoundingMode.HALF_UP).toDouble()
    val converted = if (macrosValid) {
        listOf(estimatedCalories!!, p!!, c!!, f!!).map { perHundred(it, grams!!) }
    } else null
    val valid = name.trim().isNotEmpty() && converted != null && converted[0] in 0.0..20000.0 &&
        converted[1] in 0.0..2000.0 && converted[2] in 0.0..3000.0 && converted[3] in 0.0..2000.0
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (food == null) "Tạo món ăn của tôi" else "Sửa món ăn") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nhập dinh dưỡng của khẩu phần bạn ăn. Bạn có thể thêm ảnh sau khi lưu.", color = FigmaMuted,
                style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it.take(160) }, label = { Text("Tên món") },
                singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
            NumberInput("Khẩu phần (g)", serving, enabled) { serving = it }
            Text("Thành phần dinh dưỡng cho khẩu phần trên", color = FigmaInk,
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            NumberInput("Protein (g)", protein, enabled) { protein = it }
            NumberInput("Carb (g)", carbs, enabled) { carbs = it }
            NumberInput("Fat (g)", fat, enabled) { fat = it }
            OutlinedTextField(estimatedCalories?.display() ?: "—", {},
                label = { Text("Tổng calories (kcal)") }, readOnly = true, enabled = false,
                modifier = Modifier.fillMaxWidth())
            Text("Ước tính từ đạm/đường bột/chất béo theo 4/4/9 kcal/g.",
                color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
            if (grams != null && p != null && c != null && f != null && p + c + f > grams + 0.05)
                Text("Tổng gram đạm, đường bột và chất béo không thể lớn hơn khẩu phần.",
                    color = UiDanger, style = MaterialTheme.typography.bodySmall)
            else if (converted != null && !valid)
                Text("Khẩu phần hoặc thành phần dinh dưỡng vượt giới hạn lưu trữ. Hãy kiểm tra lại.",
                    color = UiDanger, style = MaterialTheme.typography.bodySmall)
        } }, confirmButton = { TextButton(enabled = enabled && valid, onClick = {
            onSave(mapOf("name" to name.trim(), "calories_per_100g" to converted!![0],
                "protein_per_100g" to converted[1], "carb_per_100g" to converted[2],
                "fat_per_100g" to converted[3]))
        }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}

@Composable
fun FoodPhoto(vm: AppViewModel, food: FoodDto, modifier: Modifier = Modifier.size(58.dp)) {
    val imageRevision = vm.state.collectAsState().value.readRevision
    val bitmap by produceState<Bitmap?>(null, food.id, food.is_custom, food.has_image, imageRevision) {
        if (!food.has_image) return@produceState
        val ctx = vm.context()
        try {
            value = withContext(Dispatchers.IO) {
                val body = if (food.is_custom) vm.api.customFoodImage(ctx.owner, food.id)
                    else vm.api.catalogFoodImage(ctx.owner, food.id)
                body.use { response ->
                    val bytes = response.bytes()
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 256) sample *= 2
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { /* Fall back to the icon if a photo is unavailable. */ }
    }
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFF0F5F2)), contentAlignment = Alignment.Center) {
        val current = bitmap
        if (current != null) Image(current.asImageBitmap(), food.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.Restaurant, null, tint = Mint, modifier = Modifier.size(28.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FoodMacroLine(protein: Double, carbs: Double, fat: Double) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("P ${protein.display()} g", fontSize = 12.sp, color = Color(0xFFD82C83))
        Text("C ${carbs.display()} g", fontSize = 12.sp, color = Color(0xFF138CB8))
        Text("F ${fat.display()} g", fontSize = 12.sp, color = Color(0xFFB47400))
    }
}
