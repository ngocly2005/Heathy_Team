package com.app_health.app.feature.adminconnected

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app_health.app.feature.integrated.*
import kotlin.math.roundToInt

internal data class AdminSeries(val label: String, val color: Color, val value: (AdminDay) -> Long)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminDashboard(report: AdminReport?) {
    if (report == null) { Text("Chưa có báo cáo được tải từ máy chủ.", color = Muted); return }
    Text("Theo ngày dữ liệu: ${report.date_from} → ${report.date_to}", color = Muted)
    val metrics = listOf("total_users" to "Tổng người dùng", "active_accounts" to "Tài khoản đang mở",
        "active_users" to "Có hoạt động trong kỳ", "food_logs" to "Lượt ghi món", "ai_requests" to "Yêu cầu AI")
    val columns = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
    metrics.chunked(columns).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        row.forEach { (key, label) -> HealthCard(modifier = Modifier.weight(1f)) {
            StatMetric(label, (report.totals[key] ?: 0).toString())
        } }
        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
    } }
    AdminTrend("Hoạt động theo ngày", report.daily, listOf(
        AdminSeries("Món ăn", Mint) { it.food_logs }, AdminSeries("Vận động", Color(0xFF357DD9)) { it.activity_logs },
        AdminSeries("Yêu cầu AI", Color(0xFFB47613)) { it.ai_requests }))
    AdminTrend("Người dùng mới", report.daily, listOf(AdminSeries("Tài khoản mới", Mint) { it.new_users }))
    AdminTrend("Lượng nước ghi nhận (ml)", report.daily, listOf(AdminSeries("Nước (ml)", Color(0xFF258AC8)) { it.water_ml }))
    var details by remember { mutableStateOf(false) }
    HealthCard("Bảng số liệu theo ngày", if (details) "Thu gọn" else "Xem bảng", { details = !details }) {
        if (report.daily.isEmpty()) Text("Không có dữ liệu ngày.", color = Muted)
        else if (details) report.daily.forEach { day ->
            Text(day.date, style = MaterialTheme.typography.titleSmall)
            Text("Người mới ${day.new_users} • món ${day.food_logs} • vận động ${day.activity_logs}\nNước ${day.water_ml} ml • AI ${day.ai_requests}", color = Muted)
            HorizontalDivider(color = Border)
        } else Text("Mở bảng để đọc đầy đủ số liệu tương ứng với các biểu đồ.", color = Muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminTrend(title: String, days: List<AdminDay>, series: List<AdminSeries>) {
    HealthCard(title) {
        if (days.isEmpty()) { Text("Không có số liệu.", color = Muted); return@HealthCard }
        var selected by remember(days) { mutableIntStateOf(days.lastIndex) }
        val maximum = maxOf(1L, days.maxOf { day -> series.maxOf { it.value(day).coerceAtLeast(0) } })
        Text("Thang dọc: 0–$maximum", style = MaterialTheme.typography.labelSmall, color = Muted)
        Canvas(Modifier.fillMaxWidth().height(170.dp)
            .semantics { contentDescription = "$title, ${days.size} ngày. Số liệu đầy đủ có trong bảng theo ngày bên dưới." }
            .pointerInput(days) { detectTapGestures { tap ->
                val inset = 10.dp.toPx()
                val fraction = ((tap.x - inset) / (size.width - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)
                selected = (fraction * days.lastIndex).roundToInt()
            } }) {
            val inset = 10.dp.toPx()
            val width = (size.width - inset * 2).coerceAtLeast(1f)
            val height = (size.height - inset * 2).coerceAtLeast(1f)
            repeat(3) { row -> val y = inset + height * row / 2
                drawLine(Border, Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx()) }
            series.forEach { item ->
                fun point(index: Int) = Offset(inset + if (days.size == 1) width / 2 else width * index / days.lastIndex,
                    size.height - inset - height * item.value(days[index]).coerceAtLeast(0).toFloat() / maximum)
                val line = Path().apply { val start = point(0); moveTo(start.x, start.y)
                    for (i in 1..days.lastIndex) { val p = point(i); lineTo(p.x, p.y) } }
                if (days.size > 1) drawPath(line, item.color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(item.color, 4.dp.toPx(), point(selected.coerceIn(0, days.lastIndex)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(days.first().date, color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(days.last().date, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        Text("${days[selected.coerceIn(0, days.lastIndex)].date} • chạm biểu đồ để chọn ngày", color = Muted,
            style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            series.forEach { item -> Text("${item.label}: ${item.value(days[selected.coerceIn(0, days.lastIndex)])}", color = item.color,
                style = MaterialTheme.typography.bodySmall) }
        }
    }
}
