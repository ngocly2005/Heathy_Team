package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.app_health.app.feature.home.components.Home.Water.WaterIntakeCard
import kotlin.math.ceil

fun waterFeature() = AppFeature("water","Uống nước",load={vm,ctx ->
    val settings=vm.api.waterSettings(ctx.owner)
    vm.publish(ctx){it.copy(water=settings)}
}) {vm,_ -> WaterScreen(vm)}

@Composable
fun WaterOverview(vm:AppViewModel) {
    val state by vm.state.collectAsState()
    val settings=state.water ?: return
    val day=state.dashboard ?: return
    val enabled=!state.busy && !state.loading && !state.uncertain
    WaterIntakeCard(day.water_ml,settings.effective_target_ml,
        onIncrease={if(enabled)vm.write {ctx -> vm.api.adjustWater(ctx.owner,mapOf("log_date" to ctx.date,"amount_ml" to settings.serving_ml))}},
        onDecrease={if(enabled)vm.write {ctx -> vm.api.adjustWater(ctx.owner,mapOf("log_date" to ctx.date,"amount_ml" to -settings.serving_ml))}})
    val glasses=ceil(settings.effective_target_ml.toDouble()/settings.serving_ml).toInt()
    val filled=day.water_ml/settings.serving_ml
    Text("$filled ly đầy • ${settings.serving_ml} ml/ly • mục tiêu $glasses ly")
    (0 until glasses).chunked(8).forEach {row -> Text(row.joinToString(" "){if(it<filled)"💧" else "○"})}
}

@Composable
fun WaterScreen(vm:AppViewModel) {
    val state by vm.state.collectAsState()
    val settings=state.water
    var target by rememberSaveable(settings?.target_ml){mutableStateOf(settings?.target_ml?.toString() ?: "")}
    var serving by rememberSaveable(settings?.serving_ml){mutableStateOf((settings?.serving_ml ?:200).toString())}
    val amount=target.toIntOrNull()
    val enabled=!state.busy && !state.loading && !state.uncertain
    Page {
        DateStrip(vm)
        WaterOverview(vm)
        Panel("Mục tiêu uống nước") {
            NumberInput("Mục tiêu (ml, để trống để tự tính)",target,enabled){target=it}
            Choice("Dung tích mỗi ly",serving,listOf("200" to "Ly 200 ml","500" to "Chai 500 ml"),enabled){serving=it}
            Text("Tự động: 35 ml/kg, cộng theo mức vận động; giới hạn 1.5–4.5 lít.")
            Button(enabled=enabled && (target.isBlank() || amount!=null && amount in 500..10000),onClick={
                vm.write {ctx -> vm.api.saveWater(ctx.owner,mapOf("target_ml" to amount,"serving_ml" to serving.toInt()))}
            }){Text("Lưu cài đặt nước")}
        }
    }
}
