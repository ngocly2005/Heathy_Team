package com.app_health.app.feature.integrated

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun macroFeature() = AppFeature("macros", "Dinh dưỡng") { vm, navigate -> MacroScreen(vm, navigate) }

private val nutrientColors = listOf(Color(0xFFF144A7), Color(0xFF2DC4F4), Color(0xFFFFB715))

@Composable
private fun MacroRing(parts: List<Double>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 11.dp.toPx()
        val edge = minOf(size.width, size.height) - stroke
        val origin = Offset((size.width - edge) / 2, (size.height - edge) / 2)
        drawArc(Color(0xFFE9EBF0), 0f, 360f, false, origin, Size(edge, edge), style = Stroke(stroke))
        var angle = -90f
        parts.forEachIndexed { index, part ->
            val sweep = (part * 3.6).toFloat()
            if (sweep > 0) drawArc(nutrientColors[index], angle, sweep, false, origin, Size(edge, edge), style = Stroke(stroke))
            angle += sweep
        }
    }
}

/** Saved nutrition settings only; the reference's illustrative numbers are never used. */
@Composable
fun NutritionGoalSummary(target: NutritionDto, enabled: Boolean, onEdit: (() -> Unit)? = null) {
    FigmaCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(94.dp), contentAlignment = Alignment.Center) {
                MacroRing(listOf(target.protein_pct.toDouble(), target.carb_pct.toDouble(), target.fat_pct.toDouble()), Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(target.effective_calorie_target?.uiNumber(0) ?: "—", color = FigmaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("kcal", color = FigmaMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                listOf(Triple("Chất đạm", target.protein_pct, target.protein_g), Triple("Đường bột", target.carb_pct, target.carb_g),
                    Triple("Chất béo", target.fat_pct, target.fat_g)).forEachIndexed { index, (name, percent, grams) ->
                    Text("$name  $percent% (${grams.uiNumber()}g)", color = FigmaInk, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = nutrientColors[index], trackColor = Color(0xFFE8EAF0))
                }
            }
        }
        if (onEdit != null) FilledTonalButton(onClick = onEdit, enabled = enabled, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFF0F1F5), contentColor = FigmaInk)) {
            Text("Tùy chỉnh mục tiêu", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MacroOverview(state: AppState) {
    val macros = state.dashboard?.macros ?: return
    val target = state.nutrition ?: return
    FigmaCard(title = "Dinh dưỡng trong ngày") {
        listOf(Triple("Chất đạm", macros.protein_g, target.protein_g), Triple("Đường bột", macros.carb_g, target.carb_g),
            Triple("Chất béo", macros.fat_g, target.fat_g)).forEachIndexed { index, (name, amount, goal) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, color = FigmaInk)
                Text("${amount.display()} / ${goal.display()} g", color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(progress = { if (goal > 0) (amount / goal).toFloat().coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(5.dp), color = nutrientColors[index], trackColor = Color(0xFFE8EAF0))
        }
        MacroRing(listOf(macros.protein_pct, macros.carb_pct, macros.fat_pct), Modifier.fillMaxWidth().height(140.dp))
        Text("Chất đạm ${macros.protein_pct.display()}% • Đường bột ${macros.carb_pct.display()}% • Chất béo ${macros.fat_pct.display()}%", color = FigmaInk,
            style = MaterialTheme.typography.bodySmall)
        Text("Tỷ lệ năng lượng tính theo 4/4/9 kcal trên mỗi gram.", color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MacroScreen(vm: AppViewModel, navigate: (String) -> Unit = {}) {
    val state by vm.state.collectAsState()
    val target = state.nutrition
    var calories by rememberSaveable(target?.calorie_target) { mutableStateOf(target?.calorie_target?.display() ?: "") }
    var protein by rememberSaveable(target?.protein_pct) { mutableStateOf((target?.protein_pct ?:25).toString()) }
    var carbs by rememberSaveable(target?.carb_pct) { mutableStateOf((target?.carb_pct ?:50).toString()) }
    var fat by rememberSaveable(target?.fat_pct) { mutableStateOf((target?.fat_pct ?:25).toString()) }
    var weight by rememberSaveable(target?.target_weight_kg) { mutableStateOf(target?.target_weight_kg?.display() ?: "") }
    val p = protein.toIntOrNull(); val c = carbs.toIntOrNull(); val f = fat.toIntOrNull()
    val cal = inputNumber(calories); val w = inputNumber(weight)
    val valid = p != null && c != null && f != null && p in 0..100 && c in 0..100 && f in 0..100 && p+c+f ==100 &&
        (calories.isBlank() || cal != null && cal in 500.0..10000.0) && (weight.isBlank() || w != null && w>20 && w<=400)
    val enabled = !state.busy && !state.loading && !state.uncertain
    FigmaPage {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !state.busy, onClick = { navigate("profile") }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Về hồ sơ", tint = FigmaInk) }
            Text("Thiết lập mục tiêu", fontSize = 20.sp, color = FigmaInk, fontWeight = FontWeight.Bold)
        }
        FigmaTitle("1. Thông tin của bạn")
        FigmaCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cân nặng hiện tại", color = FigmaMuted)
                Text(state.profile?.weight_kg?.let { "${it.display()} kg" } ?: "Chưa cập nhật", color = FigmaInk, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = Border)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BMI", color = FigmaMuted)
                Text(state.profile?.bmi?.display() ?: "Chưa đủ dữ liệu", color = FigmaInk, fontWeight = FontWeight.Bold)
            }
            TextButton(enabled = !state.busy, onClick = { navigate("profile") }) { Text("Chỉnh sửa hồ sơ thể chất") }
        }
        FigmaTitle("2. Cân nặng mong muốn")
        NumberInput("Cân nặng mục tiêu (kg, không bắt buộc)",weight,enabled){weight=it}
        FigmaTitle("3. Calo mục tiêu hàng ngày")
        NumberInput("Calo/ngày (để trống dùng TDEE)",calories,enabled){calories=it}
        Text("Để trống để dùng mục tiêu được tính từ hồ sơ đã lưu.", color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
        FigmaTitle("4. Mục tiêu dinh dưỡng & đa lượng")
        FigmaCard {
            NumberInput("Chất đạm (%)",protein,enabled){protein=it}
            NumberInput("Đường bột (%)",carbs,enabled){carbs=it}
            NumberInput("Chất béo (%)",fat,enabled){fat=it}
            Text("Tổng tỷ lệ phải bằng 100%.", color = FigmaMuted, style = MaterialTheme.typography.bodySmall)
        }
        Button(enabled=enabled && valid,onClick={ vm.write {ctx ->
            vm.api.saveNutrition(ctx.owner,mapOf("calorie_target" to cal,"protein_pct" to p,"carb_pct" to c,"fat_pct" to f,"target_weight_kg" to w))
        } },modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(28.dp)) { Text("Lưu mục tiêu", fontWeight = FontWeight.Bold) }
        target?.let { FigmaTitle("Mục tiêu đã lưu"); NutritionGoalSummary(it, enabled) }
        DateStrip(vm)
        MacroOverview(state)
    }
}
