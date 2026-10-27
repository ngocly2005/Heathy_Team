package com.app_health.app.core.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app_health.app.core.designsystem.components.BottomNavRoute
import com.app_health.app.core.designsystem.components.PendingFeatureScreen
import com.app_health.app.feature.auth.register.RegisterScreen
import com.app_health.app.feature.home.components.Account.AccountScreen
import com.app_health.app.feature.home.components.Account.GoalSetupViewModel
import com.app_health.app.feature.home.components.Account.PhysicalProfileScreen
import com.app_health.app.feature.home.components.Account.UpdateMetricsScreen
import com.app_health.app.feature.home.components.Diary.DiaryScreen
import com.app_health.app.feature.home.components.Plan.MenuScreen
import com.app_health.app.feature.home.components.Home.HomeTabContent
import com.app_health.app.feature.home.components.FoodSelectScreen
import com.app_health.app.feature.home.components.model.HealthViewModel
import com.app_health.app.feature.auth.login.LoginScreen
import com.app_health.app.feature.home.components.Account.CustomizeGoal
import com.app_health.app.feature.home.components.LoginLogout.DataAuthManager
import com.app_health.app.feature.home.components.LoginLogout.CurrentUserManager
import java.time.LocalDate

@Composable
fun AppNavigation(viewModel: HealthViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val currentUserManager = remember { CurrentUserManager(context) }

    val currentEmail by currentUserManager.currentEmailFlow.collectAsState(initial = "loading")


    if (currentEmail == "loading") {
        return
    }
    LaunchedEffect(key1 = currentEmail) {
        currentEmail?.let { email ->
            viewModel.loadDataForUser(email)
        }
    }

    val startDestination = if (currentEmail.isNullOrBlank()) "login" else "home"

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val currentBottomRoute = when (currentDestination) {
        "home" -> BottomNavRoute.HOME
        "diary" -> BottomNavRoute.DIARY
        "plan" -> BottomNavRoute.PLAN
        "account" -> BottomNavRoute.ACCOUNT
        else -> BottomNavRoute.HOME
    }

    val onNavigateBottomNav: (BottomNavRoute) -> Unit = { route ->
        val routeString = when (route) {
            BottomNavRoute.HOME -> "home"
            BottomNavRoute.DIARY -> "diary"
            BottomNavRoute.PLAN -> "plan"
            BottomNavRoute.ACCOUNT -> "account"
        }
        navController.navigate(routeString) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ================= NHÁNH XÁC THỰC =================

        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // ================= NHÁNH CHỨC NĂNG CHÍNH (BOTTOM NAV) =================

        composable("home") {
            HomeTabContent(
                viewModel = viewModel,
                currentRoute = currentBottomRoute,
                onNavigate = onNavigateBottomNav,
                onNavigateToSelectFood = { navController.navigate("food_select") },
                onNavigateToCaptureFood = { navController.navigate("camera_capture") }
            )
        }

        composable("diary") {
            DiaryScreen(
                currentRoute = currentBottomRoute,
                viewModel = viewModel,
                onNavigate = onNavigateBottomNav
            )
        }

        composable("plan") {
            MenuScreen(
                viewModel = viewModel,
                currentRoute = currentBottomRoute,
                onNavigate = onNavigateBottomNav
            )
        }
//
//        composable("account") {
//            AccountScreen(
//                currentRoute = currentBottomRoute,
//                onNavigate = onNavigateBottomNav,
//                onNavigateToUpdateMetrics = { navController.navigate("update_metrics_screen") },
//                onNavigateToPhysicalProfile = { navController.navigate("physical_profile") },
//                onNavigateToCustomGoal = { navController.navigate("custom_goal_screen") }
////                onNavigateToLogin = {
////                    viewModel.clearAllSessionData()
////                    navController.navigate("login") {
////                        popUpTo(navController.graph.id) {
////                            inclusive = true
////                        }
////                    }
////                }
//            )
//        }
        // ================= NHÁNH TÍNH NĂNG PHỤ / MÀN HÌNH CHI TIẾT =================

        composable("food_select") {
            FoodSelectScreen(
                viewModel = viewModel,
                selectedDate = LocalDate.now().toString(), // ĐÃ SỬA: Bổ sung tham số ngày đang thiếu
                timeSlot = String.format("%02d:00", java.time.LocalTime.now().hour),
                onBack = { navController.popBackStack() }
            )
        }

        composable("camera_capture") {
            PendingFeatureScreen("Chụp ảnh món ăn", onBack = { navController.popBackStack() })
        }

        composable("physical_profile") {
            PhysicalProfileScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToGoalSetup = { navController.navigate("goal_setup_screen") }
            )
        }

        composable("goal_setup_screen") {
            GoalSetupViewModel(
                authManager = DataAuthManager(context, currentEmail),
                onBackToProfile = { navController.popBackStack() }
            )
        }

        composable("update_metrics_screen") {
            UpdateMetricsScreen(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() }
            )
        }
        composable("custom_goal_screen") {
            CustomizeGoal(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}