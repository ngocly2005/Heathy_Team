package com.app_health.app.feature.integrated

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app_health.app.R
import com.app_health.app.core.network.ApiFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

fun activityFeature() = AppFeature("activity", "Vận động", load = { vm, ctx ->
    val catalog = vm.api.activities(ctx.owner)
    vm.publish(ctx) { it.copy(activities = catalog) }
    val history = vm.api.activityHistory(ctx.owner, 30)
    vm.publish(ctx) { it.copy(activityHistory = history, activityHistoryHasMore = history.size == 30) }
}) { vm, navigate -> ActivityScreen(vm, navigate) }

@Composable
fun ActivityScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var diary by rememberSaveable { mutableStateOf(false) }
    var manual by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("30") }
    var manualName by rememberSaveable { mutableStateOf("") }
    var manualMet by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<ActivityLogDto?>(null) }
    var deleting by remember { mutableStateOf<ActivityLogDto?>(null) }
    var editMinutes by remember { mutableStateOf("") }
    var loadingMore by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val enabled = !state.busy && !state.loading && !state.uncertain
    val selected = state.activities.firstOrNull { it.id == selectedId }
    val duration = minutes.toIntOrNull()
    val metNumber = manualMet.trim().replace(',', '.').toDoubleOrNull()
    val validDuration = duration != null && duration in 1..1440
    val validManual = manualName.trim().length in 1..120 && metNumber != null && metNumber in 0.01..30.0

    key(diary, manual, selectedId) {
        HealthPage {
            if (diary) {
                HealthHeader("Nhật ký luyện tập", "Theo dõi hoạt động và năng lượng đã đốt",
                    icon = Icons.Default.History, onBack = { diary = false })
                OutlinedButton(onClick = { diary = false }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, Mint), shape = RoundedCornerShape(20.dp)) {
                    Icon(Icons.Default.Add, null, tint = Mint)
                    Spacer(Modifier.width(8.dp)); Text("Thêm vận động", color = Mint)
                }
                ExerciseDiary(vm, state, enabled, loadingMore,
                    onEdit = { editing = it; editMinutes = it.duration_minutes.toString() },
                    onDelete = { deleting = it },
                    onLoadMore = {
                        loadingMore = true
                        scope.launch {
                            try {
                                vm.read { ctx ->
                                    val older = vm.api.activityHistory(ctx.owner, 30, state.activityHistory.size)
                                    vm.publish(ctx) {
                                        it.copy(activityHistory = it.activityHistory + older,
                                            activityHistoryHasMore = older.size == 30)
                                    }
                                }.join()
                            } finally { loadingMore = false }
                        }
                    })
            } else {
                HealthHeader(
                    if (manual) "Thêm thủ công" else selected?.name ?: "Thêm vận động",
                    if (manual || selected != null) "Ghi thời gian tập vào nhật ký"
                        else "Chọn hoạt động hoặc tự nhập bài tập của bạn",
                    icon = Icons.Default.DirectionsWalk,
                    onBack = if (manual || selected != null) {
                        { manual = false; selectedId = null }
                    } else null
                )
                if (!manual && selected == null) {
                    WeekStrip(vm)
                    ExerciseCatalog(state.activities, search, enabled, state.loading,
                        onSearch = { search = it.take(100) },
                        onSelect = { selectedId = it.id }, onManual = { manual = true })
                    OutlinedButton(onClick = { diary = true }, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        border = BorderStroke(1.dp, Border), shape = RoundedCornerShape(20.dp)) {
                        Icon(Icons.Default.History, null, tint = Mint)
                        Spacer(Modifier.width(8.dp)); Text("Xem nhật ký luyện tập", color = Mint)
                    }
                } else {
                    if (selected != null) ExerciseArtwork(selected.name, Modifier.fillMaxWidth().height(190.dp))
                    WeekStrip(vm)
                    HealthCard(title = "Thông tin vận động") {
                        if (manual) {
                            OutlinedTextField(manualName, { manualName = it.take(120) },
                                label = { Text("Tên hoạt động") }, singleLine = true, enabled = enabled,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                                supportingText = { Text("Tên dài tối đa 120 ký tự.") })
                            OutlinedTextField(manualMet, { manualMet = it.take(12) },
                                label = { Text("Mức vận động (MET)") }, singleLine = true, enabled = enabled,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                                isError = manualMet.isNotBlank() && (metNumber == null || metNumber !in 0.01..30.0),
                                supportingText = { Text("MET từ 0,01 đến 30. Tham khảo cường độ trong danh mục nếu chưa rõ.") })
                        } else if (selected != null) Row(Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Cường độ", Modifier.weight(1f), color = Muted)
                            Text("${selected.met.uiNumber()} MET", color = Ink, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(4) },
                            label = { Text("Thời gian (phút)") }, singleLine = true, enabled = enabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            isError = minutes.isNotBlank() && !validDuration,
                            supportingText = { Text("Nhập từ 1 đến 1.440 phút.") })
                        val met = if (manual) metNumber else selected?.met
                        if (met != null && validDuration && state.profile != null) {
                            val estimate = met * 3.5 * state.profile!!.weight_kg / 200 * duration!!
                            HorizontalDivider(color = Border)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Calo tiêu thụ ước tính", Modifier.weight(1f), color = Muted)
                                Text("${estimate.uiNumber()} kcal", color = Mint,
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Text("Tính theo MET, thời gian và cân nặng trong hồ sơ.",
                                color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.profile == null) OutlinedButton(
                            onClick = { navigate("profile") }, enabled = enabled,
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)
                        ) { Text("Nhập hồ sơ trước khi ghi vận động") }
                        Button(
                            enabled = enabled && state.profile != null && validDuration &&
                                (if (manual) validManual else selected != null),
                            onClick = {
                                val payload: Map<String, Any?> = if (manual)
                                    mapOf("manual_name" to manualName.trim(), "manual_met" to metNumber!!,
                                        "duration_minutes" to duration!!, "log_date" to state.date.toString())
                                else mapOf("activity_type_id" to selected!!.id,
                                    "duration_minutes" to duration!!, "log_date" to state.date.toString())
                                var existingIds = emptySet<Long>()
                                var postStarted = false
                                val expectedName = if (manual) manualName.trim() else selected!!.name
                                val expectedMet = if (manual) metNumber!! else selected!!.met
                                vm.write(success = { navigate("home") }, reconcile = { ctx ->
                                    postStarted && vm.api.dashboard(ctx.owner, ctx.date).activity_logs.any {
                                        it.id !in existingIds && it.activity_name == expectedName &&
                                            it.duration_minutes == duration &&
                                            kotlin.math.abs(it.met_snapshot - expectedMet) < 0.01
                                    }
                                }) { ctx ->
                                    existingIds = try { vm.api.dashboard(ctx.owner, ctx.date).activity_logs.map { it.id }.toSet() }
                                        catch (cancelled: CancellationException) { throw cancelled }
                                        catch (_: Exception) { throw ApiFailure(503, "Chưa kiểm tra được nhật ký vận động. Hãy thử lại.") }
                                    postStarted = true
                                    vm.api.addActivity(ctx.owner, payload)
                                }
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Mint)
                        ) { Text("Lưu vận động") }
                    }
                }
            }
        }
    }
    editing?.let { log ->
        AlertDialog(onDismissRequest = { if (!state.busy) editing = null },
            title = { Text("Sửa thời gian tập") },
            text = { OutlinedTextField(editMinutes,
                { editMinutes = it.filter(Char::isDigit).take(4) }, label = { Text("Số phút") },
                singleLine = true, enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = {
                TextButton(enabled = enabled && (editMinutes.toIntOrNull() ?: 0) in 1..1440,
                    onClick = { vm.write(success = { editing = null }, reconcile = { ctx ->
                        vm.api.dashboard(ctx.owner, log.log_date).activity_logs.any {
                            it.id == log.id && it.duration_minutes == editMinutes.toInt()
                        }
                    }) { ctx ->
                        vm.api.editActivity(ctx.owner, log.id, mapOf("duration_minutes" to editMinutes.toInt()))
                    } }) { Text("Lưu") }
            }, dismissButton = { TextButton(enabled = !state.busy, onClick = { editing = null }) { Text("Hủy") } })
    }
    deleting?.let { log ->
        AlertDialog(onDismissRequest = { if (!state.busy) deleting = null },
            title = { Text("Xóa hoạt động?") },
            text = { Text("${log.activity_name} ngày ${uiDate(log.log_date)} sẽ bị xóa khỏi nhật ký.") },
            confirmButton = {
                TextButton(enabled = enabled, onClick = { vm.write(success = { deleting = null }, reconcile = { ctx ->
                    vm.api.dashboard(ctx.owner, log.log_date).activity_logs.none { it.id == log.id }
                }) { ctx ->
                    vm.api.deleteActivity(ctx.owner, log.id)
                } }) { Text("Xóa", color = UiDanger) }
            }, dismissButton = { TextButton(enabled = !state.busy, onClick = { deleting = null }) { Text("Hủy") } })
    }
}

@Composable
private fun ExerciseCatalog(
    activities: List<ActivityDto>, search: String, enabled: Boolean, loading: Boolean,
    onSearch: (String) -> Unit, onSelect: (ActivityDto) -> Unit, onManual: () -> Unit
) {
    HealthCard(title = "Chọn hoạt động") {
        OutlinedButton(onClick = onManual, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            border = BorderStroke(1.dp, Mint), shape = RoundedCornerShape(20.dp)) {
            Icon(Icons.Default.Add, null, tint = Mint)
            Spacer(Modifier.width(8.dp)); Text("Thêm thủ công", color = Mint)
        }
        OutlinedTextField(search, onSearch, placeholder = { Text("Tìm kiếm hoạt động") },
            singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) })
        val term = normalizedExercise(search)
        val shown = activities.filter { normalizedExercise(it.name).contains(term) }
            .sortedWith(compareBy<ActivityDto> { exerciseLetter(it.name) }
                .thenBy { normalizedExercise(it.name) })
        if (loading && activities.isEmpty()) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        else if (activities.isEmpty()) Text("Chưa có danh mục. Bạn vẫn có thể thêm thủ công.", color = Muted)
        else if (shown.isEmpty()) Text("Không tìm thấy hoạt động phù hợp.", color = Muted)
        shown.groupBy { exerciseLetter(it.name) }.forEach { (letter, rows) ->
            Text(letter, color = Mint, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            rows.forEach { activity ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = enabled) { onSelect(activity) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExerciseArtwork(activity.name, Modifier.size(64.dp))
                    Column(Modifier.weight(1f)) {
                        Text(activity.name, color = Ink, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${activity.met.uiNumber()} MET", color = Muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Chọn ${activity.name}",
                        tint = Mint, modifier = Modifier.size(20.dp))
                }
                HorizontalDivider(color = Border)
            }
        }
    }
}

@Composable
private fun ExerciseDiary(
    vm: AppViewModel, state: AppState, enabled: Boolean, loadingMore: Boolean,
    onEdit: (ActivityLogDto) -> Unit, onDelete: (ActivityLogDto) -> Unit, onLoadMore: () -> Unit
) {
    WeekStrip(vm)
    val logs = (state.activityHistory + (state.dashboard?.activity_logs ?: emptyList()))
        .distinctBy { it.id }
        .sortedWith(compareByDescending<ActivityLogDto> { it.log_date }.thenByDescending { it.id })
    val selectedDay = logs.filter { it.log_date == state.date.toString() }
    HealthCard(title = "Ngày ${uiDate(state.date.toString())}") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${selectedDay.size} hoạt động", Modifier.weight(1f), color = Muted)
            Text("${selectedDay.sumOf { it.calories_burned }.uiNumber()} kcal",
                color = Mint, fontWeight = FontWeight.Bold)
        }
        if (selectedDay.isEmpty()) Text("Chưa ghi vận động cho ngày đã chọn.", color = Muted)
    }
    Text("Calo đốt khi tập được tính riêng với calo đã ăn.",
        color = Muted, style = MaterialTheme.typography.bodySmall)
    if (logs.isEmpty() && !state.loading) HealthCard { Text("Chưa có hoạt động nào trong nhật ký.", color = Muted) }
    logs.groupBy { it.log_date.take(7) }.forEach { (month, entries) ->
        Text("Tháng ${month.takeLast(2)}/${month.take(4)}", color = Ink,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        entries.forEach { log ->
            HealthCard {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExerciseArtwork(log.activity_name, Modifier.size(68.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(log.activity_name, color = Ink, fontWeight = FontWeight.Bold)
                        Text(uiDate(log.log_date), color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text("${log.duration_minutes} phút · ${log.calories_burned.uiNumber()} kcal",
                            color = Mint, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(color = Border)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = enabled, onClick = { onEdit(log) },
                        border = BorderStroke(1.dp, Border)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp)); Text("Sửa thời gian")
                    }
                    TextButton(enabled = enabled, onClick = { onDelete(log) }) {
                        Icon(Icons.Default.DeleteOutline, null, Modifier.size(16.dp), tint = UiDanger)
                        Spacer(Modifier.width(5.dp)); Text("Xóa", color = UiDanger)
                    }
                }
            }
        }
    }
    if (state.activityHistoryHasMore) OutlinedButton(enabled = enabled && !loadingMore,
        onClick = onLoadMore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        border = BorderStroke(1.dp, Border), shape = RoundedCornerShape(20.dp)) {
        Text(if (loadingMore) "Đang tải…" else "Xem hoạt động cũ hơn")
    }
}

@Composable
private fun ExerciseArtwork(name: String, modifier: Modifier = Modifier) {
    val image = when (normalizedExercise(name)) {
        "bai tap the chat" -> R.drawable.exercise_04
        "boi loi", "swimming" -> R.drawable.exercise_05
        "dap xe", "cycling" -> R.drawable.exercise_06
        "di bo", "walking" -> R.drawable.exercise_07
        "chay bo", "running" -> R.drawable.exercise_08
        "tap ta" -> R.drawable.exercise_09
        "the duc dung cu" -> R.drawable.exercise_10
        "bong da trong nha" -> R.drawable.exercise_12
        "bong chay" -> R.drawable.exercise_13
        "bong da" -> R.drawable.exercise_14
        "ban cung" -> R.drawable.exercise_15
        "bong ro" -> R.drawable.exercise_16
        "bong chuyen" -> R.drawable.exercise_17
        "bong ban" -> R.drawable.exercise_18
        "cu ta" -> R.drawable.exercise_20
        "cau long" -> R.drawable.exercise_21
        "dau vat" -> R.drawable.exercise_22
        "nhay day" -> R.drawable.exercise_25
        "pilates" -> R.drawable.exercise_26
        "pickleball" -> R.drawable.exercise_27
        "quan vot" -> R.drawable.exercise_28
        "quyen anh" -> R.drawable.exercise_29
        "yoga" -> R.drawable.exercise_30
        else -> null
    }
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(PaleMint),
        contentAlignment = Alignment.Center) {
        if (image != null) Image(painterResource(image), "Ảnh ${name}",
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.DirectionsWalk, null, Modifier.size(32.dp), tint = Mint)
    }
}

private fun normalizedExercise(name: String): String =
    Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace('Đ', 'D').replace('đ', 'd')
        .lowercase(Locale.ROOT)

private fun exerciseLetter(name: String): String =
    normalizedExercise(name).firstOrNull()?.uppercaseChar()?.toString() ?: "#"
