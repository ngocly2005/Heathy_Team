package com.app_health.app.feature.adminconnected

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.app_health.app.core.network.AppContainer
import com.app_health.app.feature.integrated.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConnectedAdminScreen(container: AppContainer, onExit: () -> Unit, onLogout: () -> Unit) {
    val owner = container.sessionStore.snapshot()?.user?.id ?: return
    val vm: NativeAdminViewModel = viewModel(key = "native-admin-$owner", factory = NativeAdminViewModel.factory(container))
    val state by vm.state.collectAsState()
    DisposableEffect(vm) { vm.activate(); onDispose { vm.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    BackHandler { if (!state.busy && !state.uncertain) onExit() }
    val enabled = state.authorized && !state.loading && !state.busy && !state.uncertain
    Scaffold(containerColor = HealthBackground, topBar = {
        TopAppBar(title = { Text("Quản trị AppHealth") }, actions = {
            TextButton(enabled = !state.busy && !state.loading, onClick = vm::refresh) { Text("Tải lại") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !state.busy && !state.uncertain, onClick = onExit, modifier = Modifier.weight(1f)) { Text("Về ứng dụng") }
                    TextButton(enabled = !state.busy, onClick = onLogout, modifier = Modifier.weight(1f)) { Text("Đăng xuất") }
                }
                state.identity?.let { Text("${it.full_name}\n${it.email}", color = Muted) }
                state.error?.let { Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                    Text(it, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                } }
                state.notice?.let { Text(it, color = Mint, style = MaterialTheme.typography.bodySmall) }
                if (state.uncertain) Text("Kiểm tra danh sách sau khi Tải lại trước khi thực hiện thêm thao tác. Không tự gửi lại yêu cầu cũ.", color = Muted)
                if (state.denied) {
                    AdminDeniedPanel(onExit)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AdminTab.entries.forEach { tab -> FilterChip(selected = state.query.tab == tab,
                            enabled = !state.busy && !state.uncertain && !state.loading,
                            onClick = { vm.select(AdminQuery(tab = tab)) }, label = { Text(tab.title) }) }
                    }
                    AdminFilters(state, enabled, vm::select)
                    if (!state.authorized && !state.loading) Text("Cần kết nối máy chủ để xác minh quyền Admin.", color = Muted)
                    if (state.authorized) {
                        when (state.query.tab) {
                            AdminTab.DASHBOARD -> AdminDashboard(state.report)
                            AdminTab.USERS -> AdminUsers(vm, state, enabled)
                            AdminTab.FOODS, AdminTab.ACTIVITIES -> AdminCatalog(vm, state, enabled)
                            AdminTab.LOGS -> AdminLogs(state)
                        }
                        if (state.query.tab != AdminTab.DASHBOARD) AdminPagination(state, enabled) { page -> vm.select(state.query.copy(page = page)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminFilters(state: NativeAdminState, enabled: Boolean, apply: (AdminQuery) -> Unit) {
    val query = state.query
    var search by rememberSaveable(query.tab, query.search) { mutableStateOf(query.search) }
    var status by rememberSaveable(query.tab, query.status) { mutableStateOf(query.status) }
    var active by rememberSaveable(query.tab, query.active) { mutableStateOf(query.active?.toString() ?: "") }
    var from by rememberSaveable(query.tab, query.from) { mutableStateOf(query.from) }
    var to by rememberSaveable(query.tab, query.to) { mutableStateOf(query.to) }
    var userId by rememberSaveable(query.userId) { mutableStateOf(query.userId) }
    var kind by rememberSaveable(query.kind) { mutableStateOf(query.kind) }
    val proposed = query.copy(page = 1, search = search, status = status, active = active.toBooleanStrictOrNull(),
        from = from, to = to, userId = userId, kind = kind)
    val error = adminQueryError(proposed)
    HealthCard("Bộ lọc") {
        if (query.tab in listOf(AdminTab.USERS, AdminTab.FOODS, AdminTab.ACTIVITIES))
            OutlinedTextField(search, { search = it.take(100) }, label = { Text(if (query.tab == AdminTab.USERS) "Tên hoặc email" else "Tên danh mục") },
                enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (query.tab == AdminTab.USERS) Choice("Trạng thái", status,
            listOf("" to "Tất cả", "active" to "Đang hoạt động", "disabled" to "Đã khóa"), enabled) { status = it }
        if (query.tab in listOf(AdminTab.FOODS, AdminTab.ACTIVITIES)) Choice("Sử dụng", active,
            listOf("" to "Tất cả", "true" to "Đang sử dụng", "false" to "Đã ngừng"), enabled) { active = it }
        if (query.tab == AdminTab.DASHBOARD || query.tab == AdminTab.LOGS) {
            OutlinedTextField(from, { from = it.take(10) }, label = { Text("Từ ngày (YYYY-MM-DD)") }, enabled = enabled,
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(to, { to = it.take(10) }, label = { Text("Đến ngày (YYYY-MM-DD)") }, enabled = enabled,
                singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (query.tab == AdminTab.LOGS) {
            OutlinedTextField(userId, { userId = it.filter(Char::isDigit).take(18) }, label = { Text("ID người dùng (để trống: tất cả)") },
                enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth())
            Choice("Loại AI", kind, listOf("" to "Tất cả", "chat" to "Chat", "photo" to "Ảnh"), enabled) { kind = it }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Button(enabled = enabled && error == null, onClick = { apply(proposed) }) { Text(if (query.tab == AdminTab.DASHBOARD) "Xem báo cáo" else "Áp dụng bộ lọc") }
    }
}

@Composable
internal fun AdminDeniedPanel(onExit: () -> Unit) {
    HealthCard("Không có quyền quản trị") {
        Text("Quyền được xác minh từ máy chủ. Bạn có thể quay lại ứng dụng hoặc đăng xuất để chọn tài khoản khác.")
        Button(onClick = onExit) { Text("Quay lại ứng dụng") }
    }
}

@Composable
private fun AdminPagination(state: NativeAdminState, enabled: Boolean, change: (Int) -> Unit) {
    val page = state.query.page
    val pages = maxOf(1, (state.total + 19) / 20)
    HealthCard {
        Text("${state.total} bản ghi • Trang $page/$pages", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(enabled = enabled && page > 1, onClick = { change(page - 1) }) { Text("Trang trước") }
            OutlinedButton(enabled = enabled && page < pages, onClick = { change(page + 1) }) { Text("Trang sau") }
        }
    }
}
