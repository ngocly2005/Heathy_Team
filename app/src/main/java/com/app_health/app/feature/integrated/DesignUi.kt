package com.app_health.app.feature.integrated

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app_health.app.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// A deeper mint keeps small labels and white button text readable.
val Mint = Color(0xFF007D68)
val Ink = Color(0xFF172139)
val Muted = Color(0xFF657187)
val PaleMint = Color(0xFFE4F8F1)
val Border = Color(0xFFE9EDF3)
val HealthBackground = Color(0xFFF7F9FD)

fun Double.uiNumber(decimals: Int = 1): String = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = decimals
}.format(this)

@Composable
fun HealthPage(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFFF1FBF8), HealthBackground, HealthBackground)))
        .verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable
fun HealthHeader(
    title: String, subtitle: String, icon: ImageVector? = null,
    onBack: (() -> Unit)? = null, illustrated: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onBack != null) FilledTonalIconButton(onClick = onBack,
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = Mint)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = Mint, modifier = Modifier.size(30.dp))
            Text(title, color = Ink, fontSize = 26.sp, lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        if (illustrated && LocalDensity.current.fontScale <= 1.3f) Image(
            painterResource(R.drawable.health_mascot_20261107), null,
            Modifier.width(106.dp).height(138.dp))
    }
}

@Composable
fun HealthCard(
    title: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            if (title != null || actionLabel != null) Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                if (title != null) Text(title, Modifier.weight(1f), color = Ink,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (actionLabel != null) TextButton(onClick = { onAction?.invoke() }, enabled = onAction != null) {
                    Text(actionLabel, color = Mint)
                }
            }
            content()
        }
    }
}

data class HealthAction(val title: String, val subtitle: String, val icon: ImageVector, val route: String)

@Composable
fun ActionGrid(actions: List<HealthAction>, onNavigate: (String) -> Unit, enabled: Boolean = true) {
    val columns = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { action ->
                    Surface(onClick = { onNavigate(action.route) }, enabled = enabled,
                        modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 88.dp),
                        shape = RoundedCornerShape(20.dp), color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border), shadowElevation = 1.dp) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(Modifier.size(32.dp).background(PaleMint, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(action.icon, null, tint = Mint, modifier = Modifier.size(21.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(action.title, color = Ink, fontSize = 13.sp, lineHeight = 18.sp,
                                    fontWeight = FontWeight.SemiBold)
                                Text(action.subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatMetric(label: String, value: String, unit: String = "", icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (icon != null) Icon(icon, null, tint = Mint, modifier = Modifier.size(19.dp))
            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotEmpty()) Text(unit, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun WeekStrip(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val today = LocalDate.now()
    var weekText by rememberSaveable { mutableStateOf(weekBeginning(state.date).toString()) }
    val week = LocalDate.parse(weekText)
    LaunchedEffect(state.date) {
        if (state.date < week || state.date > week.plusDays(6)) weekText = weekBeginning(state.date).toString()
    }
    val enabled = !state.busy && !state.uncertain
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), shadowElevation = 2.dp) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { weekText = week.minusWeeks(1).toString() }, enabled = enabled) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tuần trước", tint = Mint, modifier = Modifier.size(18.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.date == today) "Hôm nay" else "Ngày đã chọn", color = Muted, style = MaterialTheme.typography.labelSmall)
                    Text(state.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), color = Ink,
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { weekText = week.plusWeeks(1).toString() }, enabled = enabled && week < weekBeginning(today)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Tuần sau", tint = Mint, modifier = Modifier.size(21.dp))
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val minimum = if (LocalDensity.current.fontScale > 1.3f) 50.dp else 35.dp
                val dayWidth = ((maxWidth - 24.dp) / 7).coerceAtLeast(minimum)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { i ->
                        val date = week.plusDays(i.toLong())
                        val selected = date == state.date
                        Column(Modifier.width(dayWidth).heightIn(min = 66.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Mint else Color(0xFFF8FAFC))
                            .clickable(enabled = enabled && date <= today) { vm.chooseDate(date) }
                            .padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")[i],
                                color = if (selected) Color.White else Muted, fontSize = 11.sp)
                            Text("${date.dayOfMonth}/${date.monthValue}", fontSize = 11.sp,
                                color = if (selected) Color.White else if (date > today) Color.LightGray else Ink,
                                fontWeight = FontWeight.SemiBold)
                            Box(Modifier.size(4.dp).background(if (selected) Color.White else if (date == today) Mint else Color.Transparent, CircleShape))
                        }
                    }
                }
            }
        }
    }
}
