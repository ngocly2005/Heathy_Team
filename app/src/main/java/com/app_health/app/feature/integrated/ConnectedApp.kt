package com.app_health.app.feature.integrated

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app_health.app.core.network.AppContainer
import com.app_health.app.core.session.AuthState
import com.app_health.app.feature.auth.AuthViewModel
import com.app_health.app.core.designsystem.components.AppBottomNavigation
import com.app_health.app.core.designsystem.components.BottomNavRoute

@Composable
fun ConnectedApp() {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val auth: AuthViewModel = viewModel(factory = AuthViewModel.factory(container.authRepository))
    val session by auth.authState.collectAsState()
    val authUi by auth.uiState.collectAsState()
    when (val current = session) {
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        AuthState.SignedOut -> Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(76.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Chào mừng trở lại!", style = MaterialTheme.typography.headlineMedium)
            Text("Tiếp tục hành trình sức khỏe của bạn", Modifier.padding(vertical = 16.dp))
            authUi.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { auth.signIn(context) }, enabled = !authUi.busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (authUi.busy) "Đang đăng nhập…" else "Tiếp tục với Google")
            }
        }
        is AuthState.SignedIn -> key(current.user.id) {
            val vm: AppViewModel = viewModel(key = "connected-${current.user.id}")
            DisposableEffect(vm) { vm.activate(); onDispose { vm.releaseIfSessionChanged() } }
            ConnectedShell(vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectedShell(vm: AppViewModel) {
    val features = remember { installedFeatures() }
    var route by rememberSaveable { mutableStateOf(features.firstOrNull()?.route ?: "foundation") }
    val state by vm.state.collectAsState()
    val current = features.firstOrNull { it.route == route }
    val branded = route in setOf("home", "diary", "meals", "profile")
    LaunchedEffect(route) { vm.enter(route) }
    BackHandler(enabled = state.busy) { /* Keep a pending write on screen until its result is known. */ }
    BackHandler(enabled = route != (features.firstOrNull()?.route ?: "foundation") && !state.busy) {
        route = features.firstOrNull()?.route ?: "foundation"
    }
    Scaffold(containerColor = HealthBackground, topBar = {
        if (branded) Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { vm.refresh() }, enabled = !state.loading && !state.busy,
                modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text("Tải lại", style = MaterialTheme.typography.labelMedium)
            }
        } else TopAppBar(title = { Text(current?.title ?: "AppHealth") }, actions = {
            TextButton(onClick = { vm.refresh() }, enabled = !state.loading && !state.busy) { Text("Tải lại") }
        })
    }, bottomBar = {
        if(features.any{it.route=="diary"}) AppBottomNavigation(enabled = !state.busy,
            currentRoute=when(route){"diary"->BottomNavRoute.DIARY;"meals"->BottomNavRoute.PLAN;"profile"->BottomNavRoute.ACCOUNT;else->BottomNavRoute.HOME},
            onRouteSelected={target ->if(!state.busy)route=when(target){BottomNavRoute.HOME->"home";BottomNavRoute.DIARY->"diary";BottomNavRoute.PLAN->"meals";BottomNavRoute.ACCOUNT->"profile"}},
            onNavigateToSelectFood={if(!state.busy)route="food"},onNavigateToCaptureFood={if(!state.busy)route="photo"})
        else if (features.isNotEmpty()) NavigationBar {
            listOf("home","diary","meals","profile").mapNotNull{key->features.firstOrNull{it.route==key}}.forEach { feature ->
                NavigationBarItem(selected = route == feature.route, onClick = { if (!state.busy) route = feature.route },
                    icon = { Text(when(feature.route) { "home" -> "⌂"; "diary" -> "▤"; "meals" -> "♡"; else -> "●" }) },
                    label = { Text(feature.title) })
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.takeIf { state.route == route || state.uncertain }?.let { message ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(message, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            key(route) {
                current?.screen?.invoke(vm) { target -> if (features.any { it.route == target }) route = target }
                    ?: Text("Phiên Google đã sẵn sàng.", Modifier.padding(24.dp))
            }
        }
    }
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    HealthPage(content)
}

@Composable
fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    HealthCard(title = title, content = content)
}

@Composable
fun Choice(label: String, value: String, choices: List<Pair<String,String>>, enabled: Boolean = true, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(choices.firstOrNull { it.first == value }?.second ?: "Chọn…")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onChange(id); expanded = false }) }
        }
    }
}

val mealChoices = listOf("breakfast" to "Bữa sáng", "lunch" to "Bữa trưa", "dinner" to "Bữa tối", "snack" to "Bữa phụ")
fun Double.display(): String = "%.1f".format(java.util.Locale.US, this)
fun inputNumber(value: String): Double? = value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
