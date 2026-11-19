package com.app_health.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
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
fun CredentialPasswordField(label: String, value: String, enabled: Boolean, errorText: String? = null, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, enabled = enabled,
        isError = value.isNotEmpty() && errorText != null,
        supportingText = errorText?.takeIf { value.isNotEmpty() }?.let { { Text(it) } },
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
    errorText: String? = null, hint: String? = null, forceError: Boolean = false,
    done: Boolean = false, onDone: () -> Unit = {},
    onChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var focusedBefore by remember { mutableStateOf(false) }
    var blurred by remember { mutableStateOf(false) }
    val showError = errorText != null && (forceError || value.isNotEmpty() || blurred)
    val focus = LocalFocusManager.current
    TextField(value, onChange, label = { Text(label, fontSize = 13.sp) }, isError = showError,
        supportingText = (if (showError) errorText else hint)?.let { message -> { Text(message) } },
        singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .semantics { contentDescription = label }.onFocusChanged {
                if (it.isFocused) focusedBefore = true else if (focusedBefore) blurred = true
            },
        shape = RoundedCornerShape(17.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = AuthInk),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false,
            imeAction = if (done) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }, onDone = { onDone() }),
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
            errorContainerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.65f), cursorColor = Mint,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent))
}

@Composable
fun ConnectedSignInScreen(auth: AuthViewModel) {
    val context = LocalContext.current
    val ui by auth.uiState.collectAsState()
    var registering by rememberSaveable { mutableStateOf(false) }
    var recovering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    // Credentials remain in this composition only, never saved in a Bundle or preferences.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    if (recovering) {
        PasswordRecoveryScreen(email, ui.busy, ui.error, ui.message,
            onEmailChange = { email = it; auth.clearFeedback() },
            onRequest = { auth.requestPasswordReset(email) },
            onReset = { token, next -> auth.resetPassword(email, token, next) },
            onBack = { recovering = false; auth.clearFeedback() })
        return
    }
    CredentialEntryScreen(registering, email, name, password, confirmation, ui.busy, ui.error,
        onEmailChange = { email = it; auth.clearFeedback() }, onNameChange = { name = it; auth.clearFeedback() },
        onPasswordChange = { password = it; auth.clearFeedback() }, onConfirmationChange = { confirmation = it; auth.clearFeedback() },
        onSubmit = { if (registering) auth.register(email, password, name) else auth.signInEmail(email, password) },
        onSwitch = { registering = !registering; password = ""; confirmation = ""; auth.clearFeedback() },
        onForgot = { recovering = true; password = ""; auth.clearFeedback() },
        onGoogle = { auth.signIn(context) })
}

@Composable
private fun PasswordRecoveryScreen(email: String, busy: Boolean, error: String?, message: String?,
    onEmailChange: (String) -> Unit, onRequest: () -> Unit,
    onReset: (String, String) -> Unit, onBack: () -> Unit) {
    // Recovery secrets are intentionally not saved in a Bundle or preferences.
    var token by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val resetDone = message?.startsWith("Đã đặt lại mật khẩu") == true
    val validPassword = newPasswordError(password) == null && password == confirmation
    Column(Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFFD4EFE6), Color(0xFFF5F6FA))))
        .systemBarsPadding().imePadding().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(45.dp))
        Box(Modifier.size(56.dp).background(AuthAccent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Lock, null, Modifier.size(29.dp), tint = Color.White)
        }
        Text("Quên mật khẩu", color = AuthInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("Nhập email tài khoản, sau đó nhập mã nhận được để đặt mật khẩu mới.",
            color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (!resetDone) {
            AuthEntryField("Email", email, !busy, Icons.Default.Email, KeyboardType.Email,
                errorText = accountEmailError(email), onChange = onEmailChange)
            Button(enabled = !busy && accountEmailError(email) == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = onRequest) {
                Text("Gửi mã đặt lại")
            }
            Text("Mã chỉ gửi cho tài khoản đã đăng ký bằng email, có thời hạn và chỉ dùng một lần.",
                color = Muted, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(token, { token = it.filter { digit -> digit in '0'..'9' }.take(8) },
                label = { Text("Mã đặt lại từ email") }, singleLine = true,
                supportingText = { Text("Mã gồm 8 chữ số được gửi tới email của bạn.", color = Muted) },
                enabled = !busy, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                    autoCorrectEnabled = false, imeAction = ImeAction.Next))
            AuthEntryField("Mật khẩu mới", password, !busy, Icons.Default.Lock,
                KeyboardType.Password, password = true, errorText = newPasswordError(password),
                hint = "8–128 ký tự", onChange = { password = it })
            AuthEntryField("Xác nhận mật khẩu mới", confirmation, !busy, Icons.Default.Lock,
                KeyboardType.Password, password = true,
                errorText = confirmationError(password, confirmation), onChange = { confirmation = it })
            Button(enabled = !busy && token.length == 8 && validPassword,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = { onReset(token, password) }) { Text("Đặt lại mật khẩu") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall) }
        message?.let { Text(it, color = AuthAccent, style = MaterialTheme.typography.bodySmall) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        TextButton(enabled = !busy, onClick = onBack) {
            Text(if (resetDone) "Về đăng nhập" else "Quay lại đăng nhập")
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Pure presentation also used by offline UI previews; all writes stay in AuthViewModel. */
@Composable
fun CredentialEntryScreen(registering: Boolean, email: String, name: String, password: String, confirmation: String,
    busy: Boolean, error: String?, onEmailChange: (String) -> Unit, onNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit, onConfirmationChange: (String) -> Unit,
    onSubmit: () -> Unit, onSwitch: () -> Unit, onForgot: () -> Unit, onGoogle: () -> Unit) {
    val errors = if (registering) registrationErrors(email, name, password, confirmation) else emailLoginErrors(email, password)
    var revealErrors by remember(registering) { mutableStateOf(false) }
    val submit = { if (!busy && errors.isValid) onSubmit() else revealErrors = true }
    key(registering) {
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
            if (registering) AuthEntryField("Họ và tên", name, !busy, Icons.Default.Person,
                errorText = errors.name, forceError = revealErrors, onChange = onNameChange)
            AuthEntryField("Email", email, !busy, Icons.Default.Email, KeyboardType.Email,
                errorText = errors.email, forceError = revealErrors, onChange = onEmailChange)
            AuthEntryField("Mật khẩu", password, !busy, Icons.Default.Lock, KeyboardType.Password, password = true,
                errorText = errors.password, hint = if (registering) "8–128 ký tự, không chỉ gồm khoảng trắng." else null,
                forceError = revealErrors, done = !registering, onDone = submit, onChange = onPasswordChange)
            if (registering) AuthEntryField("Xác nhận mật khẩu", confirmation, !busy, Icons.Default.Lock,
                KeyboardType.Password, password = true, errorText = errors.confirmation, forceError = revealErrors,
                done = true, onDone = submit, onChange = onConfirmationChange)
            if (!registering) TextButton(onClick = onForgot, enabled = !busy,
                modifier = Modifier.align(Alignment.End)) { Text("Quên mật khẩu?") }
        }
        if (!errors.isValid && (revealErrors || listOf(email, name, password, confirmation).any { it.isNotEmpty() })) {
            Text("Chưa thể ${if (registering) "đăng ký" else "đăng nhập"}: ${errors.messages.first()}",
                Modifier.fillMaxWidth().padding(top = 12.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 12.dp), color = MaterialTheme.colorScheme.error) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
        Spacer(Modifier.height(if (registering) 24.dp else 30.dp))
        Button(enabled = !busy && errors.isValid, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Color.White),
            onClick = submit) {
            Text(if (registering) "Đăng Ký" else "Đăng Nhập", fontWeight = FontWeight.Bold)
        }
        TextButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = onSwitch) {
            Text(buildAnnotatedString {
                withStyle(SpanStyle(color = Muted)) { append(if (registering) "Đã có tài khoản? " else "Chưa có tài khoản? ") }
                withStyle(SpanStyle(color = Mint, fontWeight = FontWeight.SemiBold)) { append(if (registering) "Đăng nhập" else "Đăng ký ngay") }
            }, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Mint),
            onClick = onGoogle) { Text("Tiếp tục với Google", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(24.dp))
    }
}
}
