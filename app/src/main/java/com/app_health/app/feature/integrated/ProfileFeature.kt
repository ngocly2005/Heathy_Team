package com.app_health.app.feature.integrated

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.app_health.app.core.session.AuthState
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset

val activityChoices = listOf("sedentary" to "Ít vận động", "light" to "Vận động nhẹ",
    "moderate" to "Vận động vừa", "very_active" to "Vận động nhiều", "extra_active" to "Vận động rất nhiều")
private val profileGoals = listOf("lose" to "Giảm cân", "maintain" to "Duy trì", "gain" to "Tăng cân")
private val activityDescriptions = listOf("Làm việc văn phòng, không tập thể dục", "Tập luyện nhẹ nhàng 1–3 ngày/tuần",
    "Tập luyện đều đặn 3–5 ngày/tuần", "Tập luyện cường độ cao 6–7 ngày/tuần", "Tập luyện rất nhiều hoặc công việc thể lực")

fun profileFeature() = AppFeature("profile", "Tài khoản", load = { vm, ctx ->
    val profile = try { vm.api.profile(ctx.owner) } catch (e: HttpException) { if (e.code() == 404) null else throw e }
    vm.publish(ctx) { it.copy(profile = profile) }
}) { vm, navigate -> ProfileScreen(vm, navigate) }

@Composable
fun NumberInput(label: String, value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        enabled = enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ProfileHeading(title: String, onBack: (() -> Unit)? = null, onSettings: (() -> Unit)? = null, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack, enabled = enabled) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = FigmaInk) }
        Text(title, Modifier.weight(1f), color = FigmaInk, fontSize = if (onBack == null) 24.sp else 20.sp, fontWeight = FontWeight.Bold)
        if (onSettings != null) IconButton(onClick = onSettings, enabled = enabled) { Icon(Icons.Default.Settings, "Bảo mật tài khoản", tint = FigmaMuted) }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), color = FigmaMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, Modifier.weight(1f), color = FigmaInk, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val profile = state.profile
    val identity by vm.container.sessionStore.state.collectAsState()
    val user = (identity as? AuthState.SignedIn)?.user
    var section by rememberSaveable { mutableStateOf("home") }
    var birthPicker by rememberSaveable { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }
    var birth by remember(profile?.birth_date) { mutableStateOf(profile?.birth_date ?: "") }
    var height by remember(profile?.height_cm) { mutableStateOf(profile?.height_cm?.let(::uiEditNumber) ?: "") }
    var weight by remember(profile?.weight_kg) { mutableStateOf(profile?.weight_kg?.let(::uiEditNumber) ?: "") }
    var sex by remember(profile?.sex) { mutableStateOf(profile?.sex ?: "female") }
    var activity by remember(profile?.activity_level) { mutableStateOf(profile?.activity_level ?: "sedentary") }
    var goal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "maintain") }
    val birthday = runCatching { LocalDate.parse(birth) }.getOrNull()
    val h = inputNumber(height); val w = inputNumber(weight)
    val valid = birthday != null && birthday < LocalDate.now() && h != null && h > 80 && h <= 250 && w != null && w > 20 && w <= 400
    val enabled = !state.busy && !state.loading && !state.uncertain
    val largeText = LocalDensity.current.fontScale > 1.3f
    fun openEditor() {
        birth = profile?.birth_date ?: ""; height = profile?.height_cm?.let(::uiEditNumber) ?: ""
        weight = profile?.weight_kg?.let(::uiEditNumber) ?: ""; sex = profile?.sex ?: "female"
        activity = profile?.activity_level ?: "sedentary"; goal = profile?.goal ?: "maintain"
        section = "edit"
    }
    BackHandler(enabled = section != "home" && !state.busy) { section = "home" }
    FigmaPage {
        when (section) {
            "edit" -> {
                ProfileHeading("Cập nhật chỉ số", onBack = { section = "home" }, enabled = !state.busy)
                FigmaTitle("Thông tin cơ bản")
                OutlinedButton(onClick = { birthPicker = true }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(if (birth.isBlank()) "Chọn ngày sinh" else "Ngày sinh: ${uiDate(birth)}")
                }
                Choice("Giới tính", sex, listOf("female" to "Nữ", "male" to "Nam"), enabled) { sex = it }
                NumberInput("Chiều cao (cm)", height, enabled) { height = it }
                FigmaTitle("Cân nặng hiện tại")
                NumberInput("Cân nặng (kg)", weight, enabled) { weight = it }
                FigmaTitle("Mức độ vận động")
                activityChoices.forEachIndexed { index, (id, title) ->
                    val selected = activity == id
                    Surface(onClick = { activity = id }, enabled = enabled, shape = RoundedCornerShape(16.dp),
                        color = if (selected) Color(0xFFCFFBE8) else Color.White,
                        border = if (selected) BorderStroke(1.dp, FigmaGreen) else null) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(title, color = if (selected) Color(0xFF007A56) else FigmaInk, fontWeight = FontWeight.Bold)
                                Text(activityDescriptions[index], color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            if (selected) Icon(Icons.Default.Check, null, tint = Color(0xFF007A56), modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Choice("Mục tiêu", goal, profileGoals, enabled) { goal = it }
                if (valid) {
                    val age = Period.between(birthday, LocalDate.now()).years
                    val bmi = w!! / ((h!! / 100) * (h / 100))
                    val bmr = 10 * w + 6.25 * h - 5 * age + if (sex == "male") 5 else -161
                    val factor = mapOf("sedentary" to 1.2, "light" to 1.375, "moderate" to 1.55, "very_active" to 1.725, "extra_active" to 1.9).getValue(activity)
                    FigmaCard(title = "Kết quả tính trước khi lưu") {
                        ProfileDetailRow("BMI", bmi.display()); ProfileDetailRow("BMR", "${bmr.display()} kcal")
                        ProfileDetailRow("TDEE", "${(bmr * factor).display()} kcal/ngày")
                    }
                } else Text("Chọn ngày sinh trước hôm nay, chiều cao trên 80 đến 250 cm và cân nặng trên 20 đến 400 kg.",
                    color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
                Button(enabled = enabled && valid, onClick = {
                    vm.write(success = { section = "home" }) { ctx ->
                        val saved = vm.api.saveProfile(ctx.owner, mapOf("birth_date" to birth, "height_cm" to h,
                            "weight_kg" to w, "sex" to sex, "activity_level" to activity, "goal" to goal))
                        vm.publish(ctx) { it.copy(profile = saved) }
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(28.dp)) { Text("Lưu thay đổi", fontWeight = FontWeight.Bold) }
            }
            "physical" -> {
                ProfileHeading("Hồ sơ thể chất", onBack = { section = "home" }, enabled = !state.busy)
                FigmaTitle("Thông tin cơ bản")
                FigmaCard {
                    Text("HỌ VÀ TÊN", color = FigmaMuted, style = MaterialTheme.typography.labelSmall)
                    Text(user?.fullName?.ifBlank { "Tài khoản" } ?: "Tài khoản", fontWeight = FontWeight.Bold, color = FigmaInk)
                    HorizontalDivider(color = Border)
                    ProfileDetailRow("Giới tính", profile?.let { if (it.sex == "female") "Nữ" else "Nam" } ?: "Chưa cập nhật")
                    ProfileDetailRow("Tuổi", profile?.age?.toString() ?: "—")
                    ProfileDetailRow("Chiều cao", profile?.let { "${it.height_cm.display()} cm" } ?: "—")
                    TextButton(enabled = enabled, onClick = ::openEditor) { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("Chỉnh sửa thông tin") }
                }
                FigmaTitle("Mục tiêu cân nặng & Chỉ số")
                FigmaCard {
                    ProfileDetailRow(profileGoals.firstOrNull { it.first == profile?.goal }?.second ?: "Mục tiêu", profile?.let { "${it.weight_kg.display()} kg" } ?: "—")
                    HorizontalDivider(color = Border)
                    ProfileDetailRow("Cân nặng mục tiêu", state.nutrition?.target_weight_kg?.let { "${it.display()} kg" } ?: "Chưa thiết lập")
                    HorizontalDivider(color = Border)
                    ProfileDetailRow("Cường độ vận động", activityChoices.firstOrNull { it.first == profile?.activity_level }?.second ?: "Chưa cập nhật")
                    HorizontalDivider(color = Border)
                    ProfileDetailRow("Chỉ số BMI của bạn", profile?.bmi?.display() ?: "Chưa đủ dữ liệu")
                    HorizontalDivider(color = Border)
                    ProfileDetailRow("Calo mục tiêu hàng ngày", state.nutrition?.effective_calorie_target?.let { "${it.display()} kcal" } ?: "Chưa tải mục tiêu")
                }
                FilledTonalButton(enabled = !state.busy, onClick = { navigate("macros") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFCFFBE8), contentColor = Color(0xFF007A56))) { Text("Thiết lập mục tiêu", fontWeight = FontWeight.Bold) }
            }
            else -> {
                ProfileHeading("Hồ sơ cá nhân", onSettings = { navigate("security") }, enabled = !state.busy)
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(80.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                        Text((user?.fullName?.trim()?.firstOrNull() ?: user?.email?.firstOrNull() ?: 'A').uppercaseChar().toString(), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(user?.fullName?.ifBlank { "Tài khoản" } ?: "Tài khoản", fontWeight = FontWeight.Bold, color = FigmaInk, fontSize = 18.sp)
                    Text(user?.email ?: "", color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
                    if (user?.role == "admin") Text("Quản trị viên", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                }
                FigmaCard {
                    if (largeText) {
                        ProfileDetailRow("BMI", profile?.bmi?.display() ?: "—")
                        ProfileDetailRow("TDEE", profile?.tdee_kcal?.let { "${it.uiNumber(0)} kcal" } ?: "—")
                        ProfileDetailRow("Cân nặng", profile?.weight_kg?.let { "${it.display()} kg" } ?: "—")
                    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        listOf("BMI" to (profile?.bmi?.display() ?: "—"), "TDEE" to (profile?.tdee_kcal?.uiNumber(0) ?: "—"), "Cân nặng" to (profile?.weight_kg?.let { "${it.display()} kg" } ?: "—")).forEachIndexed { index, (label, value) ->
                            if (index > 0) VerticalDivider(Modifier.height(32.dp), color = Border)
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(value, color = FigmaInk, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(label, color = FigmaMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(enabled = enabled, onClick = ::openEditor, modifier = Modifier.weight(1f).heightIn(min = 46.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFCFFBE8), contentColor = Color(0xFF007A56))) { Text("Cập nhật chỉ số", fontWeight = FontWeight.Bold) }
                    FilledTonalButton(enabled = !state.busy, onClick = { section = "physical" }, modifier = Modifier.weight(1f).heightIn(min = 46.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFCFFBE8), contentColor = Color(0xFF007A56))) { Text("Hồ sơ thể chất", fontWeight = FontWeight.Bold) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Hành trình của bạn", Modifier.weight(1f), color = FigmaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { navigate("weight") }, enabled = !state.busy) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Lịch sử cân nặng", tint = FigmaInk) }
                }
                FigmaCard {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.TrackChanges, null, tint = Color(0xFFF24A4D), modifier = Modifier.size(38.dp))
                        Text(profile?.let { "Cân nặng hiện tại: ${it.weight_kg.display()} kg" } ?: "Chưa có dữ liệu cân nặng", color = FigmaInk, fontWeight = FontWeight.Bold)
                        Text(if (profile == null) "Hãy cập nhật chỉ số để xem tiến trình" else "Theo dõi số đo thực tế trong lịch sử cân nặng", color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = !state.busy, onClick = { navigate("weight") }) { Text("Xem biểu đồ cân nặng") }
                    }
                    state.nutrition?.target_weight_kg?.let { ProfileDetailRow("Mục tiêu", "${it.display()} kg") }
                }
                FigmaTitle("Mục tiêu dinh dưỡng & đa lượng")
                state.nutrition?.let { NutritionGoalSummary(it, !state.busy) { navigate("macros") } }
                    ?: FigmaCard { Text("Thiết lập mục tiêu dinh dưỡng để theo dõi calo và đa lượng.", color = FigmaMuted); TextButton(enabled = !state.busy, onClick = { navigate("macros") }) { Text("Tùy chỉnh mục tiêu") } }
                if (user?.role == "admin") FigmaCard(title = "Quản trị viên") {
                    Button(enabled = !state.busy, onClick = { navigate("admin") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AdminPanelSettings, null); Spacer(Modifier.width(8.dp)); Text("Mở quản trị") }
                }
                FigmaTitle("Tiện ích sức khỏe")
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
                OutlinedButton(onClick = { navigate("security") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(9.dp)); Text("Bảo mật tài khoản") }
                OutlinedButton(onClick = {
                    if (!loggingOut) {
                        loggingOut = true
                        vm.viewModelScope.launch { try { vm.container.authRepository.logout() } finally { loggingOut = false } }
                    }
                }, enabled = !state.busy && !loggingOut, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, UiDanger.copy(alpha = 0.3f)), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF2F5), contentColor = UiDanger)) {
                    Icon(Icons.Default.Logout, null); Spacer(Modifier.width(9.dp)); Text(if (loggingOut) "Đang đăng xuất…" else "Đăng xuất")
                }
            }
        }
    }
    if (birthPicker) {
        val datePicker = rememberDatePickerState(initialSelectedDateMillis = birthday?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        val selectedDate = datePicker.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        DatePickerDialog(onDismissRequest = { if (!state.busy) birthPicker = false }, confirmButton = { TextButton(enabled = enabled && selectedDate != null && selectedDate < LocalDate.now(), onClick = {
            selectedDate?.let { birth = it.toString() }; birthPicker = false
        }) { Text("Chọn") } }, dismissButton = { TextButton(enabled = !state.busy, onClick = { birthPicker = false }) { Text("Hủy") } }) {
            DatePicker(state = datePicker, title = { Text("Chọn ngày sinh", Modifier.padding(start = 24.dp, top = 18.dp)) })
        }
    }
}
