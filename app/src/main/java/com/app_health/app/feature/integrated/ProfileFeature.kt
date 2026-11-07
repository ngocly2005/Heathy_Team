package com.app_health.app.feature.integrated

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.app_health.app.R
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.util.Locale

val activityChoices = listOf("sedentary" to "Ít vận động", "light" to "Vận động nhẹ",
    "moderate" to "Vận động vừa", "very_active" to "Vận động nhiều", "extra_active" to "Vận động rất nhiều")

private val profileGoals = listOf("lose" to "Giảm cân", "maintain" to "Duy trì", "gain" to "Tăng cân")

fun profileFeature() = AppFeature("profile", "Tài khoản", load = { vm, ctx ->
    val profile = try { vm.api.profile(ctx.owner) } catch (e: HttpException) { if (e.code() == 404) null else throw e }
    vm.publish(ctx) { it.copy(profile = profile) }
}) { vm, navigate -> ProfileScreen(vm, navigate) }

@Composable
fun NumberInput(label: String, value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val profile = state.profile
    val user = vm.container.sessionStore.snapshot()?.user
    var editing by rememberSaveable { mutableStateOf(false) }
    var birthPicker by rememberSaveable { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }
    var birth by remember(profile?.birth_date) { mutableStateOf(profile?.birth_date ?: "") }
    var height by remember(profile?.height_cm) { mutableStateOf(profile?.height_cm?.let(::uiEditNumber) ?: "") }
    var weight by remember(profile?.weight_kg) { mutableStateOf(profile?.weight_kg?.let(::uiEditNumber) ?: "") }
    var sex by remember(profile?.sex) { mutableStateOf(profile?.sex ?: "female") }
    var activity by remember(profile?.activity_level) { mutableStateOf(profile?.activity_level ?: "sedentary") }
    var goal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "maintain") }
    val birthday = runCatching { LocalDate.parse(birth) }.getOrNull()
    val h = inputNumber(height)
    val w = inputNumber(weight)
    val valid = birthday != null && birthday < LocalDate.now() && h != null && h > 80 && h <= 250 && w != null && w > 20 && w <= 400
    val enabled = !state.busy && !state.loading && !state.uncertain
    val largeText = LocalDensity.current.fontScale > 1.3f

    fun openEditor() {
        birth = profile?.birth_date ?: ""
        height = profile?.height_cm?.let(::uiEditNumber) ?: ""
        weight = profile?.weight_kg?.let(::uiEditNumber) ?: ""
        sex = profile?.sex ?: "female"
        activity = profile?.activity_level ?: "sedentary"
        goal = profile?.goal ?: "maintain"
        editing = true
    }

    HealthPage {
        HealthHeader("Tài khoản", "Quản lý hồ sơ và theo dõi sức khỏe của bạn", Icons.Default.PersonOutline)

        HealthCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(82.dp).clip(CircleShape).background(PaleMint), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.health_mascot_20261107), contentDescription = null,
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(3.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(user?.fullName?.ifBlank { "Tài khoản" } ?: "Tài khoản", color = Ink,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(user?.email ?: "", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    Surface(shape = RoundedCornerShape(16.dp), color = PaleMint) {
                        Text(if (user?.role == "admin") "Quản trị viên" else "Đăng nhập bằng Google",
                            Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = Mint, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        HealthCard(title = "Chỉ số sức khỏe nhanh") {
            if (largeText) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StatMetric("BMI", profile?.bmi?.display() ?: "—", icon = Icons.Default.MonitorHeart)
                StatMetric("TDEE", profile?.tdee_kcal?.display() ?: "—", "kcal/ngày", Icons.Default.LocalFireDepartment)
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatMetric("BMI", profile?.bmi?.display() ?: "—", icon = Icons.Default.MonitorHeart, modifier = Modifier.weight(1f))
                StatMetric("TDEE", profile?.tdee_kcal?.display() ?: "—", "kcal/ngày", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
            }
            Text("Các chỉ số được tính từ hồ sơ đã lưu và dùng để tham khảo.",
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }

        HealthCard(title = "Hồ sơ thể chất", actionLabel = if (enabled) if (profile == null) "Thiết lập" else "Chỉnh sửa" else null,
            onAction = { if (enabled) openEditor() }) {
            if (profile == null) {
                Text(when {
                    state.loading -> "Đang tải hồ sơ…"
                    state.error != null -> "Chưa tải được hồ sơ. Hãy thử tải lại dữ liệu."
                    else -> "Thêm hồ sơ thể chất để tính BMI, TDEE và mục tiêu dinh dưỡng."
                }, color = Muted)
                if (enabled && state.error == null) Button(onClick = ::openEditor, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text("Thiết lập hồ sơ")
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (largeText || maxWidth < 280.dp) 1 else 2
                FlowRow(maxItemsInEachRow = columns, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UiProfileValue("Ngày sinh", uiDate(profile.birth_date), Icons.Default.CalendarMonth, Modifier.weight(1f))
                    UiProfileValue("Chiều cao", String.format(Locale.ROOT, "%.2f m", profile.height_cm / 100), Icons.Default.Height, Modifier.weight(1f))
                    UiProfileValue("Cân nặng", "${profile.weight_kg.display()} kg", Icons.Default.MonitorWeight, Modifier.weight(1f))
                    UiProfileValue("Giới tính", if (profile.sex == "female") "Nữ" else "Nam", Icons.Default.PersonOutline, Modifier.weight(1f))
                    UiProfileValue("Mức vận động", activityChoices.firstOrNull { it.first == profile.activity_level }?.second ?: profile.activity_level,
                        Icons.Default.DirectionsRun, Modifier.weight(1f))
                    UiProfileValue("Mục tiêu", profileGoals.firstOrNull { it.first == profile.goal }?.second ?: profile.goal,
                        Icons.Default.Flag, Modifier.weight(1f))
                }
                }
            }
        }

        ActionGrid(listOf(
            HealthAction("Chụp món ăn", "Nhận diện và phân tích", Icons.Default.CameraAlt, "photo"),
            HealthAction("Tìm món ăn", "Tra cứu dinh dưỡng", Icons.Default.Search, "food"),
            HealthAction("Dinh dưỡng", "Mục tiêu & macro", Icons.Default.Restaurant, "macros"),
            HealthAction("Vận động", "Theo dõi bài tập", Icons.Default.DirectionsRun, "activity"),
            HealthAction("Uống nước", "Mục tiêu mỗi ngày", Icons.Default.WaterDrop, "water"),
            HealthAction("Cân nặng", "Theo dõi số đo", Icons.Default.MonitorWeight, "weight"),
            HealthAction("Tư vấn bữa ăn", "Hỏi đáp dinh dưỡng", Icons.Default.Forum, "chat"),
            HealthAction("Lịch sử ngày", "Xem lại nhật ký", Icons.Default.CalendarMonth, "calendar")
        ), onNavigate = navigate, enabled = !state.busy)

        OutlinedButton(onClick = { navigate("macros") }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Border), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Ink)) {
            Icon(Icons.Default.Settings, null, tint = Mint)
            Spacer(Modifier.width(12.dp))
            Text("Cài đặt mục tiêu dinh dưỡng", Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null)
        }
        OutlinedButton(onClick = {
            if (!loggingOut) {
                loggingOut = true
                vm.viewModelScope.launch {
                    try { vm.container.authRepository.logout() } finally { loggingOut = false }
                }
            }
        }, enabled = !state.busy && !loggingOut, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, UiDanger.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF2F5), contentColor = UiDanger)) {
            Icon(Icons.Default.Logout, null); Spacer(Modifier.width(9.dp)); Text(if (loggingOut) "Đang đăng xuất…" else "Đăng xuất")
        }
    }

    if (editing) {
        AlertDialog(onDismissRequest = { if (!state.busy) editing = false }, title = { Text("Hồ sơ thể chất") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { birthPicker = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (birth.isBlank()) "Chọn ngày sinh" else "Ngày sinh: ${uiDate(birth)}")
                    }
                    Choice("Giới tính", sex, listOf("female" to "Nữ", "male" to "Nam"), enabled) { sex = it }
                    NumberInput("Chiều cao (cm)", height, enabled) { height = it }
                    NumberInput("Cân nặng (kg)", weight, enabled) { weight = it }
                    Choice("Mức vận động", activity, activityChoices, enabled) { activity = it }
                    Choice("Mục tiêu", goal, profileGoals, enabled) { goal = it }
                    if (valid) {
                        val age = Period.between(birthday, LocalDate.now()).years
                        val bmi = w!! / ((h!! / 100) * (h / 100))
                        val bmr = 10 * w + 6.25 * h - 5 * age + if (sex == "male") 5 else -161
                        val factor = mapOf("sedentary" to 1.2, "light" to 1.375, "moderate" to 1.55, "very_active" to 1.725, "extra_active" to 1.9).getValue(activity)
                        Surface(shape = RoundedCornerShape(18.dp), color = PaleMint) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Kết quả tính trước khi lưu", color = Mint, fontWeight = FontWeight.Bold)
                                Text("BMI ${bmi.display()} • BMR ${bmr.display()} kcal", color = Ink)
                                Text("TDEE ${(bmr * factor).display()} kcal/ngày", color = Ink)
                            }
                        }
                    } else Text("Chọn ngày sinh trước hôm nay, chiều cao trên 80 đến 250 cm và cân nặng trên 20 đến 400 kg.",
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }, confirmButton = {
                TextButton(enabled = enabled && valid, onClick = {
                    vm.write(success = { editing = false }) { ctx ->
                        val saved = vm.api.saveProfile(ctx.owner, mapOf("birth_date" to birth, "height_cm" to h,
                            "weight_kg" to w, "sex" to sex, "activity_level" to activity, "goal" to goal))
                        vm.publish(ctx) { it.copy(profile = saved) }
                    }
                }) { Text("Lưu hồ sơ") }
            }, dismissButton = { TextButton(enabled = !state.busy, onClick = { editing = false }) { Text("Hủy") } })
    }
    if (birthPicker) {
        val datePicker = rememberDatePickerState(initialSelectedDateMillis = birthday?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        val selectedDate = datePicker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        DatePickerDialog(onDismissRequest = { if (!state.busy) birthPicker = false },
            confirmButton = { TextButton(enabled = enabled && selectedDate != null && selectedDate < LocalDate.now(), onClick = {
                selectedDate?.let { birth = it.toString() }; birthPicker = false
            }) { Text("Chọn") } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = { birthPicker = false }) { Text("Hủy") } }) {
            DatePicker(state = datePicker, title = { Text("Chọn ngày sinh", Modifier.padding(start = 24.dp, top = 18.dp)) })
        }
    }
}
