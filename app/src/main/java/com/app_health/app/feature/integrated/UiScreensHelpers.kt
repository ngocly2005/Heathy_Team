package com.app_health.app.feature.integrated

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val UiDanger = Color(0xFFD94260)
internal fun uiDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT))
}.getOrDefault(value)

/** Keep server precision in editable fields rather than rounding a valid 0.01 g to 0.0 g. */
internal fun uiEditNumber(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

@Composable
internal fun UiIllustration(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(64.dp), shape = RoundedCornerShape(22.dp), color = PaleMint) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(34.dp), tint = Mint) }
    }
}

@Composable
internal fun UiEmptyState(
    icon: ImageVector, title: String, description: String, action: String,
    enabled: Boolean, onAction: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        UiIllustration(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Ink, style = MaterialTheme.typography.titleSmall)
            Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
    Button(onClick = onAction, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(action)
    }
}

@Composable
internal fun UiEntryActions(enabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onEdit, enabled = enabled) {
            Icon(Icons.Default.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Sửa")
        }
        TextButton(onClick = onDelete, enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = UiDanger)) {
            Icon(Icons.Default.DeleteOutline, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Xóa")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UiMealPicker(
    meal: String, time: String, enabled: Boolean,
    onMeal: (String) -> Unit, onTime: (String) -> Unit, allowEmptyTime: Boolean = false
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf("snack" to "Bữa phụ", "breakfast" to "Sáng", "lunch" to "Trưa", "dinner" to "Tối").forEach { (id, label) ->
            FilterChip(selected = meal == id, onClick = { onMeal(id) }, enabled = enabled,
                label = { Text(label) }, shape = RoundedCornerShape(24.dp),
                colors = FilterChipDefaults.filterChipColors(containerColor = Color.White,
                    labelColor = Ink, selectedContainerColor = Mint, selectedLabelColor = Color.White))
        }
    }
    OutlinedButton(onClick = { picking = true }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), border = BorderStroke(1.dp, Border)) {
        Icon(Icons.Default.AccessTime, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Giờ ăn", color = Ink)
        Spacer(Modifier.weight(1f))
        Text(time.ifBlank { "Chọn giờ" }, fontWeight = FontWeight.Bold)
        Icon(Icons.Default.KeyboardArrowDown, null)
    }
    if (allowEmptyTime && time.isNotBlank()) TextButton(enabled = enabled, onClick = { onTime("") }) { Text("Bỏ giờ ăn") }
    if (picking) {
        val initial = runCatching { LocalTime.parse(time) }.getOrElse { LocalTime.now() }
        var hours by remember { mutableStateOf(initial.hour.toString().padStart(2, '0')) }
        var minutes by remember { mutableStateOf(initial.minute.toString().padStart(2, '0')) }
        val h = hours.toIntOrNull(); val m = minutes.toIntOrNull()
        AlertDialog(onDismissRequest = { if (enabled) picking = false },
            title = { Text("Chọn giờ ăn") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(hours, { hours = it.filter(Char::isDigit).take(2) },
                        label = { Text("Giờ (0–23)") }, singleLine = true, enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(2) },
                        label = { Text("Phút (0–59)") }, singleLine = true, enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }, confirmButton = {
                TextButton(enabled = enabled && h != null && h in 0..23 && m != null && m in 0..59, onClick = {
                    onTime(String.format(Locale.ROOT, "%02d:%02d", h, m)); picking = false
                }) { Text("Chọn") }
            }, dismissButton = { TextButton(enabled = enabled, onClick = { picking = false }) { Text("Hủy") } })
    }
}

@Composable
internal fun UiProfileValue(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.heightIn(min = 84.dp), shape = RoundedCornerShape(18.dp),
        color = Color.White, border = BorderStroke(1.dp, Border)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, Modifier.size(23.dp), tint = Mint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(value, color = Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
