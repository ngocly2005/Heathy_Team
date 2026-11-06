package com.app_health.app.feature.integrated

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

fun macroFeature() = AppFeature("macros", "Dinh dưỡng") { vm, _ -> MacroScreen(vm) }

@Composable
fun MacroOverview(state: AppState) {
    val macros = state.dashboard?.macros ?: return
    val target = state.nutrition ?: return
    Panel("Dinh dưỡng trong ngày") {
        listOf(Triple("Protein",macros.protein_g,target.protein_g),Triple("Carb",macros.carb_g,target.carb_g),Triple("Fat",macros.fat_g,target.fat_g)).forEach { (name, amount, goal) ->
            Text("$name: ${amount.display()} / ${goal.display()} g")
            LinearProgressIndicator(progress={if(goal>0)(amount/goal).toFloat().coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth())
        }
        val parts = listOf(macros.protein_pct,macros.carb_pct,macros.fat_pct)
        val colors = listOf(Color(0xFF119B72),Color(0xFF4895EF),Color(0xFFF4B942))
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val edge = size.height-16
            val origin = Offset((size.width-edge)/2,8f)
            var angle = -90f
            if (parts.sum() == 0.0) drawArc(Color.LightGray,0f,360f,false,origin,Size(edge,edge),style=Stroke(24f))
            parts.forEachIndexed { i, part ->
                val sweep = (part*3.6).toFloat()
                if (sweep > 0) drawArc(colors[i],angle,sweep,false,origin,Size(edge,edge),style=Stroke(24f))
                angle += sweep
            }
        }
        Text("Protein ${macros.protein_pct.display()}% • Carb ${macros.carb_pct.display()}% • Fat ${macros.fat_pct.display()}%")
        Text("Tỷ lệ năng lượng tính theo 4/4/9 kcal trên mỗi gram.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MacroScreen(vm: AppViewModel) {
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
    Page {
        DateStrip(vm)
        MacroOverview(state)
        Panel("Tùy chỉnh mục tiêu") {
            NumberInput("Calo/ngày (để trống dùng TDEE)",calories,enabled){calories=it}
            NumberInput("Protein (%)",protein,enabled){protein=it}
            NumberInput("Carb (%)",carbs,enabled){carbs=it}
            NumberInput("Fat (%)",fat,enabled){fat=it}
            Text("Tổng tỷ lệ phải bằng 100%.")
            NumberInput("Cân nặng mục tiêu (kg, không bắt buộc)",weight,enabled){weight=it}
            Button(enabled=enabled && valid,onClick={ vm.write {ctx ->
                vm.api.saveNutrition(ctx.owner,mapOf("calorie_target" to cal,"protein_pct" to p,"carb_pct" to c,"fat_pct" to f,"target_weight_kg" to w))
            } }) {Text("Lưu mục tiêu")}
        }
    }
}
