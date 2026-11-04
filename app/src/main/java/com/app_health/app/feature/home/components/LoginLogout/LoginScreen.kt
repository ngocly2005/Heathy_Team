package com.app_health.app.feature.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app_health.app.feature.auth.components.AuthTextField
import com.app_health.app.feature.home.components.LoginLogout.AuthValidator
import com.app_health.app.feature.home.components.LoginLogout.DataAuthManager
import com.app_health.app.feature.home.components.LoginLogout.CurrentUserManager
import com.app_health.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (isAdmin: Boolean) -> Unit

) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUserManager = remember { CurrentUserManager(context) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(AppPrimary.copy(alpha = 0.15f), AppBackground),
        startY = 0f,
        endY = 1200f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = "Logo",
            tint = AppPrimary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Chào mừng trở lại!",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tiếp tục hành trình sức khỏe của bạn",
            color = TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
        )

        AuthTextField(
            value = email,
            onValueChange = { email = it; emailError = null },
            label = "Email",
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            errorMessage = emailError
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = "Mật khẩu",
            icon = Icons.Default.Lock,
            isPassword = true,
            errorMessage = passwordError
        )

        Text(
            text = "Quên mật khẩu?",
            color = AppPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 12.dp)
                .clickable { }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                emailError = AuthValidator.validateEmail(email)
                passwordError = AuthValidator.validateLoginPassword(password)

                if (emailError == null && passwordError == null) {
                    coroutineScope.launch {
                        val cleanEmail = email.trim()

                        // Kiểm tra với DataAuthManager tương ứng của email đó
                        val authManagerForCheck = DataAuthManager(context, cleanEmail)
                        val savedPass = authManagerForCheck.savedPassword.first()
                        val isAdminAccount = (cleanEmail == "admin@gmail.com" && password == "admin123")
                        val isDefaultAccount = (cleanEmail == "test@gmail.com" && password == "123456")
                        val isRegisteredAccount = (password == savedPass && savedPass != null)

                        if (isAdminAccount || isDefaultAccount || isRegisteredAccount) {
                            // Lưu phiên đăng nhập hiện tại để các màn hình khác biết đang dùng tài khoản nào
                            currentUserManager.saveCurrentEmail(cleanEmail)

                            // Chỉ gọi hàm này 1 lần duy nhất với tham số isAdminAccount
                            onLoginSuccess(isAdminAccount)
                        } else {
                            passwordError = "Sai mật khẩu hoặc tài khoản chưa được đăng ký!"
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Đăng Nhập", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Chưa có tài khoản? ", color = TextSecondary)
            Text(
                text = "Đăng ký ngay",
                color = AppPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onNavigateToRegister = {},
        onLoginSuccess = { isAdmin -> }
    )
}