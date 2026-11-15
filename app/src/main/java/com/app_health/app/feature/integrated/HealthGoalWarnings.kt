package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Adult reference ranges are prompts for review, not a diagnosis or a save restriction. */
internal fun macroGoalWarnings(protein: Int?, carbs: Int?, fat: Int?, age: Int?): List<String> {
    if (protein == null && carbs == null && fat == null) return emptyList()
    if (age != null && age < 19) return listOf("Khung tỷ lệ đa lượng dưới đây dành cho người từ 19 tuổi. Hãy hỏi chuyên gia dinh dưỡng về mục tiêu phù hợp với lứa tuổi của bạn.")
    return buildList {
        if (protein != null && protein !in 10..35) add("Chất đạm $protein% nằm ngoài khoảng tham khảo 10–35% năng lượng cho người lớn.")
        if (carbs != null && carbs !in 45..65) add("Đường bột $carbs% nằm ngoài khoảng tham khảo 45–65% năng lượng cho người lớn.")
        if (fat != null && fat !in 20..35) add("Chất béo $fat% nằm ngoài khoảng tham khảo 20–35% năng lượng cho người lớn.")
    }
}

internal fun weightGoalWarnings(targetKg: Double?, profile: ProfileDto?): List<String> {
    if (targetKg == null || profile == null) return emptyList()
    if (profile.age < 19) return listOf("Tốc độ giảm cân và BMI của người lớn không áp dụng trực tiếp cho người dưới 19 tuổi. Hãy cùng chuyên gia đánh giá mục tiêu cân nặng.")
    return buildList {
        val lossKg = profile.weight_kg - targetKg
        if (lossKg > 4) {
            val shortestWeeks = ceil(lossKg).toInt()
            add("Bạn muốn giảm ${lossKg.display()} kg. Tốc độ thường được khuyến nghị là khoảng 0,5–1 kg/tuần (2–4 kg/tháng); mục tiêu này cần ít nhất khoảng $shortestWeeks tuần, không phải trong một tháng.")
        }
        if (lossKg < -4) add("Bạn muốn tăng ${(-lossKg).display()} kg. Hãy đặt thời gian phù hợp và trao đổi với chuyên gia nếu muốn tăng cân nhanh; ứng dụng không suy ra tốc độ tăng cân an toàn chỉ từ một số đo.")
        if (profile.age >= 20 && profile.height_cm > 0) {
            val heightM = profile.height_cm / 100
            val targetBmi = targetKg / (heightM * heightM)
            if (targetBmi < 18.5) add("Cân nặng này tương ứng BMI khoảng ${targetBmi.display()}, dưới ngưỡng 18,5 của người lớn. Hãy trao đổi với chuyên gia trước khi theo đuổi mục tiêu.")
            if (targetBmi >= 30 && targetBmi > profile.bmi) add("Cân nặng này tương ứng BMI khoảng ${targetBmi.display()}. BMI chỉ là chỉ số sàng lọc; hãy xem xét mục tiêu tăng cân cùng chuyên gia.")
        }
    }
}

internal fun calorieGoalWarnings(calories: Double?, profile: ProfileDto?): List<String> {
    if (calories == null || profile == null) return emptyList()
    if (profile.age < 19) return listOf("Các mốc calo tham khảo cho người lớn không áp dụng trực tiếp cho người dưới 19 tuổi. Hãy hỏi chuyên gia trước khi đặt mục tiêu ăn kiêng.")
    val communityFloor = if (profile.sex == "male") 1500.0 else 1200.0
    val reviewFloor = max(communityFloor, profile.bmr_kcal)
    return buildList {
        if (calories < reviewFloor) add("Cảnh báo calo thấp: ${calories.display()} kcal/ngày thấp hơn mốc rà soát ${reviewFloor.display()} kcal/ngày (BMR ước tính ${profile.bmr_kcal.display()} và mốc tham khảo ${communityFloor.display()}). Không tự duy trì chế độ này khi chưa hỏi chuyên gia.")
        if (profile.tdee_kcal > 0 && calories > profile.tdee_kcal + 500) add("Calo mục tiêu cao hơn TDEE ước tính ${profile.tdee_kcal.display()} quá 500 kcal/ngày. Hãy kiểm tra mục tiêu và mức vận động; nhu cầu thực tế có thể khác.")
    }
}

/** No population-wide upper limit exists for water; 4.5 L is an app review trigger. */
internal fun waterGoalWarnings(targetMl: Int?, profile: ProfileDto? = null): List<String> {
    if (targetMl == null) return emptyList()
    val activityExtra = when (profile?.activity_level) {
        "moderate" -> 300; "very_active" -> 500; "extra_active" -> 700; else -> 0
    }
    val automaticMl = profile?.let { (it.weight_kg * 35).roundToInt() + activityExtra }
        ?.coerceIn(1500, 4500) ?: 2000
    return if (targetMl > 4500 || targetMl > automaticMl * 1.5) listOf(
        "Mục tiêu ${targetMl / 1000.0} lít/ngày cao hơn mức tự tính khoảng ${automaticMl / 1000.0} lít/ngày. Đây không phải ngưỡng độc hại cố định; nhu cầu phụ thuộc vận động, thời tiết và bệnh lý. Hãy hỏi chuyên gia nếu bạn cần uống nhiều như vậy."
    ) else emptyList()
}

internal fun waterIntakeWarnings(consumedMl: Int, targetMl: Int): List<String> = when {
    consumedMl > 4500 -> listOf("Đã ghi ${(consumedMl / 1000.0).display()} lít nước hôm nay. Tránh uống một lượng lớn trong thời gian ngắn; uống quá nhanh có thể gây hạ natri máu. Ứng dụng chỉ có tổng theo ngày nên không đánh giá được tốc độ uống.")
    targetMl > 0 && consumedMl > targetMl * 1.1 -> listOf("Bạn đã uống ${(consumedMl / 1000.0).display()} lít, vượt hơn 10% mục tiêu ${(targetMl / 1000.0).display()} lít hôm nay. Đây không tự động là mức nguy hiểm; hãy uống theo nhu cầu và tránh uống dồn quá nhanh.")
    else -> emptyList()
}

@Composable
internal fun HealthWarningPanel(messages: List<String>) {
    if (messages.isEmpty()) return
    Surface(color = Color(0xFFFFF3D6), contentColor = Color(0xFF6B4700),
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.WarningAmber, null, modifier = Modifier.size(20.dp))
                Text("Lưu ý về mục tiêu sức khỏe", style = MaterialTheme.typography.titleSmall)
            }
            messages.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
