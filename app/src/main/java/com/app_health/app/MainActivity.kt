package com.app_health.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app_health.app.feature.integrated.ConnectedApp
import com.app_health.app.ui.theme.AppHealthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppHealthTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ConnectedApp()
                }
            }
        }
    }
}
