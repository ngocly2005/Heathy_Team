package com.app_health.app.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CredentialPasswordField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = { visible = !visible }, enabled = enabled) {
            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu")
        } })
}

@Composable
fun ConnectedSignInScreen(auth: AuthViewModel) {
    val context = LocalContext.current
    val ui by auth.uiState.collectAsState()
    var registering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    // Credentials remain in this composition only, never saved in a Bundle or preferences.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = if (registering) validRegistration(email, name, password, confirmation) else validEmailLogin(email, password)
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text(if (registering) "Tạo tài khoản" else "Chào mừng trở lại!", style = MaterialTheme.typography.headlineMedium)
        Text("Tiếp tục hành trình sức khỏe của bạn")
        if (registering) OutlinedTextField(name, { name = it.take(120) }, label = { Text("Họ và tên") },
            enabled = !ui.busy, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it.take(320) }, label = { Text("Email") }, singleLine = true,
            enabled = !ui.busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        CredentialPasswordField(if (registering) "Mật khẩu (8–128 ký tự)" else "Mật khẩu", password, !ui.busy) { password = it }
        if (registering) CredentialPasswordField("Nhập lại mật khẩu", confirmation, !ui.busy) { confirmation = it }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(enabled = !ui.busy && valid, modifier = Modifier.fillMaxWidth(), onClick = {
            if (registering) auth.register(email, password, name) else auth.signInEmail(email, password)
        }) { Text(if (registering) "Đăng ký" else "Đăng nhập") }
        TextButton(enabled = !ui.busy, onClick = {
            registering = !registering; password = ""; confirmation = ""; auth.clearFeedback()
        }) { Text(if (registering) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký") }
        HorizontalDivider()
        OutlinedButton(enabled = !ui.busy, modifier = Modifier.fillMaxWidth(), onClick = { auth.signIn(context) }) {
            Text("Tiếp tục với Google")
        }
        Text("Google vẫn dùng được. Tài khoản mới qua Google được tạo tự động.", style = MaterialTheme.typography.bodySmall)
    }
}
