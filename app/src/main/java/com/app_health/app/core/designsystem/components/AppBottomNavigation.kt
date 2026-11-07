package com.app_health.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app_health.app.feature.home.components.AddCameraFood.AddFoodBottomSheet
import com.app_health.app.ui.theme.*

enum class BottomNavRoute { HOME, DIARY, PLAN, ACCOUNT }

@Composable
fun AppBottomNavigation(
    modifier: Modifier = Modifier,
    currentRoute: BottomNavRoute,
    onRouteSelected: (BottomNavRoute) -> Unit,
    onNavigateToSelectFood: () -> Unit,
    onNavigateToCaptureFood: () -> Unit,
    enabled: Boolean = true
) {
    var showAddSheet by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) showAddSheet = false }
    Row(modifier.fillMaxWidth()
        .shadow(8.dp, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
        .background(Color.White).navigationBarsPadding().padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        NavItem("Trang chủ", Icons.Default.Home, currentRoute == BottomNavRoute.HOME, enabled) { onRouteSelected(BottomNavRoute.HOME) }
        NavItem("Nhật ký", Icons.Default.Restaurant, currentRoute == BottomNavRoute.DIARY, enabled) { onRouteSelected(BottomNavRoute.DIARY) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Surface(onClick = { showAddSheet = true }, enabled = enabled, shape = CircleShape,
                color = AppPrimary, shadowElevation = 4.dp, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Thêm món ăn", tint = Color.White, modifier = Modifier.size(30.dp)) }
            }
        }
        NavItem("Thực đơn", Icons.AutoMirrored.Filled.Assignment, currentRoute == BottomNavRoute.PLAN, enabled) { onRouteSelected(BottomNavRoute.PLAN) }
        NavItem("Tài khoản", Icons.Default.Person, currentRoute == BottomNavRoute.ACCOUNT, enabled) { onRouteSelected(BottomNavRoute.ACCOUNT) }
    }
    if (showAddSheet && enabled) AddFoodBottomSheet(
        onDismiss = { showAddSheet = false },
        onNavigateToSelectFood = { showAddSheet = false; onNavigateToSelectFood() },
        onNavigateToCaptureFood = { showAddSheet = false; onNavigateToCaptureFood() }
    )
}

@Composable
private fun RowScope.NavItem(title: String, icon: ImageVector, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = if (selected) AppPrimary else TextSecondary
    Column(Modifier.weight(1f).selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
        .padding(horizontal = 2.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(title, color = color, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.heightIn(min = 16.dp))
        Box(Modifier.width(22.dp).height(3.dp).background(if (selected) AppPrimary else Color.Transparent, RoundedCornerShape(2.dp)))
    }
}

@Preview(showBackground = true)
@Composable
fun AppBottomNavigationPreview() {
    AppBottomNavigation(currentRoute = BottomNavRoute.HOME, onRouteSelected = {},
        onNavigateToSelectFood = {}, onNavigateToCaptureFood = {})
}
