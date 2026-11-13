package com.app_health.app.feature.integrated

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Scoped to the screens present in the supplied exports; other screens keep their theme.
val FigmaGreen = Color(0xFF0DBD88)
val FigmaBackground = Color(0xFFF5F6FA)
val FigmaInk = Color(0xFF232D3C)
val FigmaMuted = Color(0xFF6C7485)

@Composable
fun FigmaPage(background: Color = FigmaBackground, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(background).verticalScroll(rememberScrollState())
        .imePadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}

@Composable
fun FigmaCard(title: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null || actionLabel != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                title?.let { Text(it, Modifier.weight(1f), color = FigmaInk, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium) }
                actionLabel?.let { TextButton(onClick = { onAction?.invoke() }, enabled = onAction != null) { Text(it) } }
            }
            content()
        }
    }
}

@Composable
fun FigmaTitle(text: String) {
    Text(text, color = FigmaInk, fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun FigmaMacroStrip(state: AppState, onDetails: () -> Unit) {
    val macros = state.dashboard?.macros ?: return
    val goals = state.nutrition
    val rows = listOf(Triple("Chất đạm", macros.protein_g, goals?.protein_g),
        Triple("Đường bột", macros.carb_g, goals?.carb_g), Triple("Chất béo", macros.fat_g, goals?.fat_g))
    val colors = listOf(Color(0xFFED45A1), Color(0xFF35BDED), Color(0xFFF5B40D))
    val large = LocalDensity.current.fontScale > 1.3f
    FigmaCard {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (large || maxWidth < 280.dp) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                rows.forEachIndexed { index, (name, amount, target) ->
                    FigmaMacroItem(name, amount, target, colors[index], Modifier.fillMaxWidth())
                }
            } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEachIndexed { index, (name, amount, target) ->
                    FigmaMacroItem(name, amount, target, colors[index], Modifier.weight(1f))
                }
            }
        }
        TextButton(onClick = onDetails, enabled = !state.busy, modifier = Modifier.align(Alignment.End),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("Chi tiết dinh dưỡng", fontSize = 12.sp) }
    }
}

@Composable
private fun FigmaMacroItem(name: String, amount: Double, target: Double?, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, color = FigmaInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        LinearProgressIndicator(progress = { if (target != null && target > 0) (amount / target).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(5.dp), color = color, trackColor = Color(0xFFE5E7EB))
        Text(if (target != null) "${amount.uiNumber()} / ${target.uiNumber()} g" else "${amount.uiNumber()} g",
            color = FigmaMuted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}
