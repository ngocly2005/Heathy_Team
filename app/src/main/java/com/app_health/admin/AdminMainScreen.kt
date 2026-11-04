
package com.app_health.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

// ================= COLOR THEME =================

private val AppBackground = Color(0xFFF5F7F9)
private val AppSurface = Color.White

private val AppPrimary = Color(0xFF10B981)
private val AppPrimaryDark = Color(0xFF047857)
private val AppPrimaryLight = Color(0xFFE8F8F1)

private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val BorderColor = Color(0xFFE8EEF2)

// ================= MAIN ADMIN SCREEN =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen() {

    val navController = rememberNavController()

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    val backStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry?.destination?.route ?: "admin_dashboard"

    val title = when (currentRoute) {
        "admin_dashboard" -> "Dashboard"
        "admin_users" -> "Quản lý người dùng"
        "admin_data" -> "Quản lý dữ liệu"
        else -> "Admin Panel"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.35f),

        drawerContent = {

            ModalDrawerSheet(
                drawerContainerColor = AppSurface,
                drawerShape = RoundedCornerShape(
                    topEnd = 24.dp,
                    bottomEnd = 24.dp
                ),
                modifier = Modifier
                    .width(290.dp)
                    .fillMaxHeight()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp)
                ) {

                    // ===== BRAND HEADER =====

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp, bottom = 24.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        AppPrimary,
                                        RoundedCornerShape(15.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "f",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {

                                Text(
                                    text = "fitChat",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppPrimaryDark
                                )

                                Text(
                                    text = "HEALTH & FITNESS PLATFORM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Surface(
                            color = AppPrimaryLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ADMIN PORTAL",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 7.dp
                                ),
                                color = AppPrimaryDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    Spacer(Modifier.height(20.dp))

                    // ===== ADMIN PROFILE =====

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3FAF7)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(43.dp)
                                    .background(
                                        AppPrimaryLight,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AD",
                                    color = AppPrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(Modifier.width(11.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "Administrator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )

                                Spacer(Modifier.height(3.dp))

                                Text(
                                    text = "Quản trị viên hệ thống",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(
                                        AppPrimary,
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // ===== NAVIGATION TITLE =====

                    Text(
                        text = "MENU CHÍNH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(
                            start = 12.dp,
                            bottom = 12.dp
                        )
                    )

                    // ===== NAVIGATION ITEMS =====

                    DrawerItem(
                        text = "Dashboard",
                        subtitle = "Tổng quan hệ thống",
                        icon = Icons.Default.Analytics,
                        selected = currentRoute == "admin_dashboard"
                    ) {
                        navController.navigate("admin_dashboard") {
                            launchSingleTop = true
                        }

                        scope.launch {
                            drawerState.close()
                        }
                    }

                    DrawerItem(
                        text = "Quản lý người dùng",
                        subtitle = "Tài khoản và quyền truy cập",
                        icon = Icons.Default.People,
                        selected = currentRoute == "admin_users"
                    ) {
                        navController.navigate("admin_users") {
                            launchSingleTop = true
                        }

                        scope.launch {
                            drawerState.close()
                        }
                    }

                    DrawerItem(
                        text = "Quản lý dữ liệu",
                        subtitle = "Thực phẩm và bài tập",
                        icon = Icons.Default.Fastfood,
                        selected = currentRoute == "admin_data"
                    ) {
                        navController.navigate("admin_data") {
                            launchSingleTop = true
                        }

                        scope.launch {
                            drawerState.close()
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // ===== SYSTEM STATUS =====

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF7F9FB)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(
                                            AppPrimary,
                                            CircleShape
                                        )
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    text = "Hệ thống hoạt động",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppPrimaryDark
                                )
                            }

                            Spacer(Modifier.height(7.dp))

                            Text(
                                text = "fitChat Admin Dashboard",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )

                            Spacer(Modifier.height(10.dp))

                            HorizontalDivider(
                                color = BorderColor
                            )

                            Spacer(Modifier.height(9.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Text(
                                    text = "Phiên bản",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )

                                Text(
                                    text = "v1.0.0",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

    ) {

        // ===== MAIN CONTENT =====

        Scaffold(
            containerColor = AppBackground,

            topBar = {

                TopAppBar(

                    title = {

                        Column {

                            Text(
                                text = title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = "fitChat / Admin Portal",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    },

                    navigationIcon = {

                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = TextPrimary
                            )
                        }
                    },

                    actions = {

                        Surface(
                            color = AppPrimaryLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 16.dp)
                        ) {

                            Text(
                                text = "ADMIN",
                                modifier = Modifier.padding(
                                    horizontal = 11.dp,
                                    vertical = 7.dp
                                ),
                                color = AppPrimaryDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppSurface
                    )
                )
            }

        ) { padding ->

            NavHost(
                navController = navController,
                startDestination = "admin_dashboard",
                modifier = Modifier.padding(padding)
            ) {

                composable("admin_dashboard") {
                    AdminDashboardScreen()
                }

                composable("admin_users") {
                    AdminUsersScreen()
                }

                composable("admin_data") {
                    AdminDataScreen()
                }
            }
        }
    }
}

// ================= DRAWER ITEM =================

@Composable
private fun DrawerItem(
    text: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {

    NavigationDrawerItem(

        icon = {

            Box(
                modifier = Modifier.size(25.dp),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) {
                        Color.White
                    } else {
                        TextSecondary
                    },
                    modifier = Modifier.size(21.dp)
                )
            }
        },

        label = {

            Column(
                modifier = Modifier.padding(vertical = 4.dp)
            ) {

                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    },
                    color = if (selected) {
                        Color.White
                    } else {
                        TextPrimary
                    }
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = if (selected) {
                        Color.White.copy(alpha = 0.8f)
                    } else {
                        TextSecondary
                    }
                )
            }
        },

        selected = selected,

        onClick = onClick,

        colors = NavigationDrawerItemDefaults.colors(

            selectedContainerColor = AppPrimary,

            unselectedContainerColor = Color.Transparent,

            selectedIconColor = Color.White,

            unselectedIconColor = TextSecondary,

            selectedTextColor = Color.White,

            unselectedTextColor = TextPrimary
        ),

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),

        shape = RoundedCornerShape(13.dp)
    )
}
