package com.app_health.app.feature.integrated

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app_health.app.core.network.ApiFailure
import com.app_health.app.core.network.toUserMessage
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val WeightMint = Color(0xFF007D68)
private val WeightPale = Color(0xFFE5F8F1)
private val WeightInk = Color(0xFF162D3B)
private val WeightMuted = Color(0xFF657187)
private val WeightBorder = Color(0xFFE3EEE9)
private val WeightDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
private const val LastWeightMessage = "Hồ sơ cần giữ ít nhất một số đo. Hãy thêm số đo khác trước khi xóa số đo này."

fun weightFeature() = AppFeature("weight", "Cân nặng", load = { vm, ctx ->
    val weights = vm.api.weights(ctx.owner)
    vm.publish(ctx) { it.copy(weights = weights.sortedBy { row -> row.measured_on }) }
}) { vm, _ -> WeightScreen(vm) }

@Composable
private fun WeightCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, WeightBorder), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun WeightHeading(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = WeightMint, modifier = Modifier.size(25.dp))
        Text(title, color = WeightInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeightMonthSelector(month: YearMonth, today: LocalDate, enabled: Boolean, change: (YearMonth) -> Unit) {
    Row(Modifier.fillMaxWidth().background(WeightPale, RoundedCornerShape(18.dp)).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(enabled = enabled && month > YearMonth.of(1, 1), onClick = { change(month.minusMonths(1)) }) {
            Icon(Icons.Outlined.ChevronLeft, "Tháng trước", tint = WeightMint)
        }
        Text("Tháng ${month.monthValue}, ${month.year}", color = WeightInk,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(enabled = enabled && month < YearMonth.from(today), onClick = { change(month.plusMonths(1)) }) {
            Icon(Icons.Outlined.ChevronRight, "Tháng sau", tint = if (month < YearMonth.from(today)) WeightMint else WeightMuted.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun MonthlyWeightTrend(points: List<MonthlyWeightPoint>, month: YearMonth, enabled: Boolean,
    loading: Boolean, failed: Boolean, edit: (WeightDto) -> Unit) {
    if (points.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.MonitorWeight, null, tint = WeightMint.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
            Text(when { loading -> "Đang tải số đo…"; failed -> "Chưa tải được dữ liệu cân nặng."; else -> "Chưa có số đo trong tháng này." },
                color = WeightMuted, textAlign = TextAlign.Center)
            if (!loading && !failed) Text("Chọn ngày và ghi số đo bên dưới để bắt đầu theo dõi.",
                color = WeightMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        return
    }
    var selectedDate by rememberSaveable(month.toString()) { mutableStateOf<String?>(null) }
    val selected = points.firstOrNull { it.entry.measured_on == selectedDate } ?: points.last()
    val bounds = remember(points) { weightChartRange(points) }
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.width(45.dp).height(180.dp), verticalArrangement = Arrangement.SpaceBetween) {
            listOf(bounds.maxKg, (bounds.maxKg + bounds.minKg) / 2, bounds.minKg).forEach {
                Text(weightLabel(it), color = WeightMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.fillMaxWidth().height(180.dp)
                .semantics { contentDescription = "Biểu đồ tháng ${month.monthValue}/${month.year}, ${points.size} số đo. Chọn số đo trong danh sách để chỉnh sửa." }
                .pointerInput(points, month) {
                    detectTapGestures { tap ->
                        val inset = 10.dp.toPx()
                        val fraction = ((tap.x - inset) / (size.width - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        selectedDate = points.minByOrNull { abs(weightDayFraction(it.date, month) - fraction) }?.entry?.measured_on
                    }
                }) {
                val inset = 10.dp.toPx()
                val plotWidth = (size.width - 2 * inset).coerceAtLeast(1f)
                val plotHeight = (size.height - 2 * inset).coerceAtLeast(1f)
                fun position(point: MonthlyWeightPoint) = Offset(
                    inset + weightDayFraction(point.date, month) * plotWidth,
                    size.height - inset - ((point.entry.weight_kg - bounds.minKg) / (bounds.maxKg - bounds.minKg)).toFloat() * plotHeight)
                repeat(3) { row ->
                    val y = inset + plotHeight * row / 2
                    drawLine(WeightBorder, Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx())
                }
                val line = Path().apply {
                    val first = position(points.first()); moveTo(first.x, first.y)
                    points.drop(1).forEach { point -> val p = position(point); lineTo(p.x, p.y) }
                }
                if (points.size > 1) {
                    val fill = Path().apply {
                        val first = position(points.first()); moveTo(first.x, size.height - inset); lineTo(first.x, first.y)
                        points.drop(1).forEach { point -> val p = position(point); lineTo(p.x, p.y) }
                        lineTo(position(points.last()).x, size.height - inset); close()
                    }
                    drawPath(fill, Brush.verticalGradient(listOf(WeightMint.copy(alpha = 0.18f), Color.Transparent)))
                    drawPath(line, WeightMint, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                points.forEach { point ->
                    val p = position(point)
                    if (point.date == selected.date) drawCircle(WeightMint.copy(alpha = 0.15f), 11.dp.toPx(), p)
                    drawCircle(Color.White, 6.dp.toPx(), p)
                    drawCircle(WeightMint, 4.dp.toPx(), p)
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().height(24.dp)) {
                listOf(1, 8, 15, 22, month.lengthOfMonth()).distinct().forEach { day ->
                    val fraction = (day - 1).toFloat() / (month.lengthOfMonth() - 1)
                    Text(day.toString(), Modifier.width(22.dp).offset(x = 10.dp + (maxWidth - 20.dp) * fraction - 11.dp),
                        color = WeightMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
    Text("Ngày trong tháng • đơn vị kg", color = WeightMuted, style = MaterialTheme.typography.labelSmall)
    Row(Modifier.fillMaxWidth().background(WeightPale, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(selected.date.format(WeightDateFormat), color = WeightMuted, style = MaterialTheme.typography.labelMedium)
            Text("${weightLabel(selected.entry.weight_kg)} kg", color = WeightMint,
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        TextButton(enabled = enabled, onClick = { edit(selected.entry) }) {
            Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = WeightMint)
            Spacer(Modifier.width(5.dp)); Text("Sửa số đo", color = WeightMint)
        }
    }
    if (points.size > 1) {
        val delta = points.last().entry.weight_kg - points.first().entry.weight_kg
        Text("Thay đổi trong tháng: ${if (delta > 0) "+" else ""}${weightLabel(delta)} kg",
            color = WeightMuted, style = MaterialTheme.typography.bodySmall)
    } else Text("Cần thêm số đo để thể hiện xu hướng.", color = WeightMuted, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun WeightValueInput(value: String, enabled: Boolean, change: (String) -> Unit) {
    val error = weightInputError(value)
    OutlinedTextField(value, change, modifier = Modifier.fillMaxWidth(), label = { Text("Cân nặng (kg)") },
        singleLine = true, enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp), isError = value.isNotBlank() && error != null,
        supportingText = { Text(if (value.isNotBlank() && error != null) error else "Trên 20 đến 400 kg · tối đa 2 chữ số thập phân") },
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WeightMint, unfocusedBorderColor = WeightBorder,
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, cursorColor = WeightMint,
            focusedTextColor = WeightInk, unfocusedTextColor = WeightInk, disabledTextColor = WeightMuted,
            focusedLabelColor = WeightMint, unfocusedLabelColor = WeightMuted,
            focusedSupportingTextColor = WeightMuted, unfocusedSupportingTextColor = WeightMuted))
}

private fun measurementError(error: Exception): String = if (error is IOException && error !is ApiFailure)
    "Chưa rõ kết quả thao tác. Bấm Tải lại và kiểm tra số đo trước khi gửi tiếp."
else error.toUserMessage()

private fun saveMeasurement(vm: AppViewModel, date: LocalDate, value: Double, success: () -> Unit, failure: (String) -> Unit) {
    vm.write(success = success) { ctx ->
        try { vm.api.saveWeight(ctx.owner, date.toString(), mapOf("weight_kg" to value)) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { failure(measurementError(error)); throw error }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightDatePicker(initial: LocalDate, today: LocalDate, dismiss: () -> Unit, selected: (LocalDate) -> Unit) {
    val picker = rememberDatePickerState(initialSelectedDateMillis = weightPickerMillis(initial),
        yearRange = minOf(1900, initial.year)..today.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = allowedWeightDate(weightPickerDate(utcTimeMillis), today)
            override fun isSelectableYear(year: Int) = year <= today.year
        })
    val colors = DatePickerDefaults.colors(containerColor = Color.White,
        titleContentColor = WeightMuted, headlineContentColor = WeightInk, weekdayContentColor = WeightMuted,
        subheadContentColor = WeightInk, dayContentColor = WeightInk,
        selectedDayContainerColor = WeightMint, selectedDayContentColor = Color.White,
        todayContentColor = WeightMint, todayDateBorderColor = WeightMint)
    DatePickerDialog(onDismissRequest = dismiss, colors = colors, confirmButton = {
        TextButton(enabled = picker.selectedDateMillis != null, onClick = {
            picker.selectedDateMillis?.let { selected(weightPickerDate(it)) }
        }) { Text("Chọn ngày", color = WeightMint) }
    }, dismissButton = { TextButton(onClick = dismiss) { Text("Hủy", color = WeightMuted) } }) {
        DatePicker(state = picker, colors = colors, showModeToggle = false)
    }
}

@Composable
fun WeightScreen(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val today = LocalDate.now()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(state.date.coerceAtMost(today)).toString()) }
    val month = YearMonth.parse(monthText)
    val points = remember(state.weights, month) { monthlyWeightPoints(state.weights, month) }
    var newDateText by rememberSaveable { mutableStateOf(state.date.coerceAtMost(today).toString()) }
    val newDate = LocalDate.parse(newDateText)
    var value by rememberSaveable { mutableStateOf("") }
    var choosingDate by rememberSaveable { mutableStateOf(false) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<WeightDto?>(null) }
    var editValue by rememberSaveable { mutableStateOf("") }
    var editError by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<WeightDto?>(null) }
    var deleteError by rememberSaveable { mutableStateOf<String?>(null) }
    val enabled = !state.busy && !state.loading && !state.uncertain
    fun edit(entry: WeightDto) { editing = entry; editValue = weightInputText(entry.weight_kg); editError = null }

    Column(Modifier.fillMaxSize().background(Color(0xFFF7FBF9)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ghi số đo và theo dõi thay đổi theo từng tháng.", color = WeightMuted,
            style = MaterialTheme.typography.bodyMedium)
        WeightCard {
            WeightHeading("Xu hướng theo tháng", Icons.Outlined.ShowChart)
            WeightMonthSelector(month, today, enabled) { monthText = it.toString() }
            MonthlyWeightTrend(points, month, enabled, state.loading, state.error != null, ::edit)
        }
        WeightCard {
            WeightHeading("Ghi số đo", Icons.Outlined.MonitorWeight)
            Text("Bạn có thể thêm và chỉnh số đo của ngày quá khứ.", color = WeightMuted,
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = enabled, onClick = { choosingDate = true }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, WeightBorder)) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = WeightMint)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("Ngày đo", color = WeightMuted, style = MaterialTheme.typography.labelSmall)
                    Text(newDate.format(WeightDateFormat), color = WeightInk, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Outlined.ExpandMore, "Chọn ngày đo", tint = WeightMint)
            }
            WeightValueInput(value, enabled) { value = it; formError = null }
            formError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(enabled = enabled && allowedWeightDate(newDate, today) && weightInputAmount(value) != null,
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WeightMint), onClick = {
                    val amount = weightInputAmount(value) ?: return@Button
                    val submittedDate = newDate
                    formError = null
                    saveMeasurement(vm, submittedDate, amount, success = {
                        value = ""; monthText = YearMonth.from(submittedDate).toString()
                    }, failure = { formError = it })
                }) {
                Icon(Icons.Outlined.Save, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp)); Text("Lưu số đo", fontWeight = FontWeight.SemiBold)
            }
            Text("Ngày đã có số đo sẽ được cập nhật, không tạo thêm bản ghi trùng ngày.",
                color = WeightMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (points.isNotEmpty()) WeightCard {
            WeightHeading("Số đo tháng ${month.monthValue}/${month.year}", Icons.Outlined.History)
            Text("${points.size} số đo · chọn Sửa để chỉnh trực tiếp ngày đã ghi",
                color = WeightMuted, style = MaterialTheme.typography.bodySmall)
            points.asReversed().forEachIndexed { index, point ->
                if (index > 0) HorizontalDivider(color = WeightBorder)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(point.date.format(WeightDateFormat), color = WeightMuted, style = MaterialTheme.typography.bodySmall)
                        Text("${weightLabel(point.entry.weight_kg)} kg", color = WeightInk,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(enabled = enabled, onClick = { edit(point.entry) }) {
                        Icon(Icons.Outlined.Edit, "Sửa số đo ngày ${point.date.format(WeightDateFormat)}", tint = WeightMint)
                    }
                    IconButton(enabled = enabled, onClick = { deleting = point.entry; deleteError = null }) {
                        Icon(Icons.Outlined.DeleteOutline, "Xóa số đo ngày ${point.date.format(WeightDateFormat)}",
                            tint = Color(0xFFD64C61))
                    }
                }
            }
        }
    }
    if (choosingDate) WeightDatePicker(newDate, today, dismiss = { choosingDate = false }) {
        newDateText = it.toString(); choosingDate = false; formError = null
    }
    editing?.let { target ->
        val date = LocalDate.parse(target.measured_on)
        AlertDialog(onDismissRequest = { if (!state.busy) editing = null }, title = { Text("Chỉnh số đo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ngày đo: ${date.format(WeightDateFormat)}", color = WeightInk, fontWeight = FontWeight.SemiBold)
                    Text("Thay đổi này chỉ áp dụng cho số đo của ngày đã chọn.", color = WeightMuted,
                        style = MaterialTheme.typography.bodySmall)
                    WeightValueInput(editValue, enabled) { editValue = it; editError = null }
                    editError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }, confirmButton = {
                TextButton(enabled = enabled && allowedWeightDate(date, today) && weightInputAmount(editValue) != null,
                    onClick = {
                        val amount = weightInputAmount(editValue) ?: return@TextButton
                        editError = null
                        saveMeasurement(vm, date, amount, success = {
                            editing = null; monthText = YearMonth.from(date).toString()
                        }, failure = { editError = it })
                    }) { Text("Lưu thay đổi", color = WeightMint) }
            }, dismissButton = {
                TextButton(enabled = !state.busy, onClick = { editing = null }) { Text("Hủy", color = WeightMuted) }
            }, containerColor = Color.White, titleContentColor = WeightInk, textContentColor = WeightInk)
    }
    deleting?.let { target ->
        val date = LocalDate.parse(target.measured_on)
        AlertDialog(onDismissRequest = { if (!state.busy) deleting = null }, title = { Text("Xóa số đo?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${date.format(WeightDateFormat)} · ${weightLabel(target.weight_kg)} kg")
                    Text("Những số đo của ngày khác được giữ nguyên.", color = WeightMuted)
                    deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }, confirmButton = {
                TextButton(enabled = enabled && deleteError != LastWeightMessage, onClick = {
                    deleteError = null
                    vm.write(success = { deleting = null }) { ctx ->
                        try { vm.api.deleteWeight(ctx.owner, target.measured_on) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) {
                            if (error is HttpException && error.code() == 409) {
                                deleteError = LastWeightMessage
                                throw ApiFailure(409, LastWeightMessage)
                            }
                            deleteError = measurementError(error)
                            throw error
                        }
                    }
                }) { Text("Xóa số đo", color = Color(0xFFD64C61)) }
            }, dismissButton = {
                TextButton(enabled = !state.busy, onClick = { deleting = null }) { Text("Đóng", color = WeightMuted) }
            }, containerColor = Color.White, titleContentColor = WeightInk, textContentColor = WeightInk)
    }
}
