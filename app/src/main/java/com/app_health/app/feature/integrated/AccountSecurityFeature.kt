package com.app_health.app.feature.integrated

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app_health.app.feature.auth.AuthViewModel
import com.app_health.app.feature.auth.CredentialPasswordField
import com.app_health.app.feature.auth.validPasswordUpdate

fun accountSecurityFeature() = AppFeature("security", "Bảo mật tài khoản") { vm, _ -> AccountSecurityScreen(vm) }

@Composable
fun AccountSecurityScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val auth: AuthViewModel = viewModel(factory = AuthViewModel.factory(vm.container.authRepository))
    val ui by auth.uiState.collectAsState()
    val ownerId = vm.context().owner.userId
    val account = ui.account?.takeIf { it.id == ownerId }
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    LaunchedEffect(ownerId) { auth.clearFeedback(); auth.loadAccount(ownerId) }
    LaunchedEffect(ui.message) { if (ui.message != null) { current = ""; next = ""; confirmation = "" } }
    Page {
        Text("Hai cách đăng nhập dùng cùng hồ sơ, quyền và dữ liệu.")
        OutlinedButton(enabled = !ui.busy, onClick = { auth.loadAccount(ownerId) }) { Text("Tải lại phương thức đăng nhập") }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (account == null) {
            Text("Đang đọc phương thức đăng nhập của tài khoản…")
        } else {
            Panel(if (account.hasPassword) "Đổi mật khẩu" else "Thêm mật khẩu cho tài khoản") {
                Text(account.email)
                Text("Mật khẩu 8–128 ký tự. Mật khẩu được gửi đến BE để xác minh, không lưu trên thiết bị.")
                if (account.hasPassword) CredentialPasswordField("Mật khẩu hiện tại", current, !ui.busy) { current = it }
                CredentialPasswordField("Mật khẩu mới", next, !ui.busy) { next = it }
                CredentialPasswordField("Nhập lại mật khẩu mới", confirmation, !ui.busy) { confirmation = it }
                Button(enabled = !ui.busy && validPasswordUpdate(account.hasPassword, current, next, confirmation), onClick = {
                    auth.updatePassword(ownerId, current.takeIf { account.hasPassword }, next)
                }) { Text("Lưu mật khẩu") }
            }
            Panel("Liên kết Google") {
                if (!account.googleSubject.isNullOrBlank()) Text("Đã liên kết Google với ${account.email}.")
                else {
                    Text("Chọn tài khoản Google có đúng email ${account.email}. Việc liên kết giữ nguyên hồ sơ và quyền hiện tại.")
                    OutlinedButton(enabled = !ui.busy, onClick = { auth.linkGoogle(ownerId, context) }) { Text("Liên kết tài khoản Google") }
                }
            }
        }
    }
}
