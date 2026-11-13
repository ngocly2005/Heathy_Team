package com.app_health.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app_health.app.feature.integrated.Mint
import com.app_health.app.feature.integrated.Muted

private val AuthInk = Color(0xFF232D3C)
private val AuthAccent = Color(0xFF0DBD88)

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

/** Rounded filled treatment from the exported login/register frames. */
@Composable
private fun AuthEntryField(
    label: String, value: String, enabled: Boolean, icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text, password: Boolean = false,
    onChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    TextField(value, onChange, placeholder = { Text(label, color = Muted, fontSize = 13.sp) },
        singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(17.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = AuthInk),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        leadingIcon = { Icon(icon, null, tint = AuthAccent, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (password) {{
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                    tint = Muted, modifier = Modifier.size(20.dp))
            }
        }} else null,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.65f), cursorColor = Mint,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent))
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
    Column(Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFFD4EFE6), Color(0xFFF5F6FA))))
        .systemBarsPadding().imePadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(70.dp))
        Box(Modifier.size(56.dp).background(AuthAccent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LocalFireDepartment, null, Modifier.size(31.dp), tint = Color.White)
        }
        Spacer(Modifier.height(32.dp))
        Text(if (registering) "Bắt đầu ngay!" else "Chào mừng trở lại!", color = AuthInk,
            fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(if (registering) "Tạo tài khoản để theo dõi dinh dưỡng" else "Tiếp tục hành trình sức khỏe của bạn",
            color = Muted, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(34.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (registering) AuthEntryField("Họ và tên", name, !ui.busy, Icons.Default.Person) { name = it.take(120) }
            AuthEntryField("Email", email, !ui.busy, Icons.Default.Email, KeyboardType.Email) { email = it.take(320) }
            AuthEntryField("Mật khẩu", password, !ui.busy, Icons.Default.Lock, KeyboardType.Password, password = true) { password = it }
            if (registering) AuthEntryField("Xác nhận mật khẩu", confirmation, !ui.busy, Icons.Default.Lock,
                KeyboardType.Password, password = true) { confirmation = it }
        }
        if (registering) Text("Mật khẩu từ 8–128 ký tự", Modifier.fillMaxWidth().padding(top = 8.dp),
            color = Muted, style = MaterialTheme.typography.bodySmall)
        ui.error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = MaterialTheme.colorScheme.error) }
        if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
        Spacer(Modifier.height(if (registering) 24.dp else 30.dp))
        Button(enabled = !ui.busy && valid, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color.White),
            onClick = { if (registering) auth.register(email, password, name) else auth.signInEmail(email, password) }) {
            Text(if (registering) "Đăng Ký" else "Đăng Nhập", fontWeight = FontWeight.Bold)
        }
        TextButton(enabled = !ui.busy, modifier = Modifier.fillMaxWidth(), onClick = {
            registering = !registering; password = ""; confirmation = ""; auth.clearFeedback()
        }) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Muted)) { append(if (registering) "Đã có tài khoản? " else "Chưa có tài khoản? ") }
                withStyle(SpanStyle(color = Mint, fontWeight = FontWeight.SemiBold)) { append(if (registering) "Đăng nhập" else "Đăng ký ngay") }
            }, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(enabled = !ui.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Mint),
            onClick = { auth.signIn(context) }) { Text("Tiếp tục với Google", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(24.dp))
    }
}
