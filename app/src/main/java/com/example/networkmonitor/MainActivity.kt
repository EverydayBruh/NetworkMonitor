package com.example.networkmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.networkmonitor.ui.theme.NetworkMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkMonitorTheme {
                // Инициализация экрана сканирования
                ScanScreen()
            }
        }
    }
}
