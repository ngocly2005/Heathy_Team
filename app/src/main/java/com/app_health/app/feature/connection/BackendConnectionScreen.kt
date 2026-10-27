package com.app_health.app.feature.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app_health.app.core.network.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Debug-only connection check; /health confirms the server, not database/OAuth. */
@Composable
fun BackendConnectionScreen(container: AppContainer, onContinue: () -> Unit) {
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Kết nối AppHealth", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(container.apiBaseUrl, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text("Kiểm tra máy chủ trước khi đăng nhập. Database và Google cần được kiểm tra riêng.")
        Spacer(Modifier.height(20.dp))
        if (checking) CircularProgressIndicator()
        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = !checking,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    checking = true
                    message = null
                    try {
                        val result = container.healthApi.health()
                        message = if (result.status == "ok") "Máy chủ đã phản hồi."
                        else "Máy chủ trả trạng thái: ${result.status}"
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        message = "Chưa kết nối được. Kiểm tra địa chỉ, máy chủ và mạng của thiết bị."
                    } finally {
                        checking = false
                    }
                }
            },
        ) { Text("Kiểm tra kết nối") }
        OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Tiếp tục vào ứng dụng")
        }
    }
}