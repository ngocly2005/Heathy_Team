package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.Period

val activityChoices = listOf("sedentary" to "Ít vận động", "light" to "Vận động nhẹ",
    "moderate" to "Vận động vừa", "very_active" to "Vận động nhiều", "extra_active" to "Vận động rất nhiều")

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
fun ProfileScreen(vm: AppViewModel, navigate: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val profile = state.profile
    var birth by remember(profile?.birth_date) { mutableStateOf(profile?.birth_date ?: "") }
    var height by remember(profile?.height_cm) { mutableStateOf(profile?.height_cm?.display() ?: "") }
    var weight by remember(profile?.weight_kg) { mutableStateOf(profile?.weight_kg?.display() ?: "") }
    var sex by remember(profile?.sex) { mutableStateOf(profile?.sex ?: "female") }
    var activity by remember(profile?.activity_level) { mutableStateOf(profile?.activity_level ?: "sedentary") }
    var goal by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "maintain") }
    val birthday = runCatching { LocalDate.parse(birth) }.getOrNull()
    val h = inputNumber(height)
    val w = inputNumber(weight)
    val valid = birthday != null && birthday < LocalDate.now() && h != null && h > 80 && h <= 250 && w != null && w > 20 && w <= 400
    val enabled = !state.busy && !state.loading && !state.uncertain
    Page {
        Text(vm.container.sessionStore.snapshot()?.user?.fullName ?: "Tài khoản", style = MaterialTheme.typography.headlineSmall)
        Text("Hồ sơ thể chất", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(birth, { birth = it }, label = { Text("Ngày sinh (YYYY-MM-DD)") }, singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth())
        Choice("Giới tính", sex, listOf("female" to "Nữ", "male" to "Nam"), enabled) { sex = it }
        NumberInput("Chiều cao (cm)", height, enabled) { height = it }
        NumberInput("Cân nặng (kg)", weight, enabled) { weight = it }
        Choice("Mức vận động", activity, activityChoices, enabled) { activity = it }
        Choice("Mục tiêu", goal, listOf("lose" to "Giảm cân", "maintain" to "Duy trì", "gain" to "Tăng cân"), enabled) { goal = it }
        if (valid) {
            val age = Period.between(birthday, LocalDate.now()).years
            val bmi = w!! / ((h!! / 100) * (h / 100))
            val bmr = 10 * w + 6.25 * h - 5 * age + if (sex == "male") 5 else -161
            val factor = mapOf("sedentary" to 1.2,"light" to 1.375,"moderate" to 1.55,"very_active" to 1.725,"extra_active" to 1.9).getValue(activity)
            Panel("Kết quả tính trước khi lưu") {
                Text("BMI ${bmi.display()} • BMR ${bmr.display()} kcal")
                Text("TDEE ${(bmr * factor).display()} kcal/ngày")
            }
        } else Text("Nhập ngày sinh hợp lệ, chiều cao trên 80–250 cm và cân nặng trên 20–400 kg.")
        Button(enabled = enabled && valid, onClick = {
            vm.write { ctx ->
                val saved = vm.api.saveProfile(ctx.owner, mapOf("birth_date" to birth, "height_cm" to h,
                    "weight_kg" to w, "sex" to sex, "activity_level" to activity, "goal" to goal))
                vm.publish(ctx) { it.copy(profile = saved) }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Lưu hồ sơ") }
        profile?.let { Text("Đã lưu trên máy chủ • BMI ${it.bmi.display()} • TDEE ${it.tdee_kcal.display()} kcal") }
        installedFeatures().filter { it.route !in setOf("home","profile","diary","meals") }.forEach { feature ->
            OutlinedButton(onClick = { navigate(feature.route) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(feature.title) }
        }
        OutlinedButton(onClick = { vm.viewModelScope.launch { vm.container.authRepository.logout() } }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth()) { Text("Đăng xuất") }
    }
}
