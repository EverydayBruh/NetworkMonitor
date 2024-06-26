package com.example.networkmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.networkmonitor.ui.theme.NetworkMonitorTheme
import com.example.networkmonitor.models.Network
import com.example.networkmonitor.data.DataManager

class MainActivity : ComponentActivity() {
    private val networks = mutableListOf<Network>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkMonitorTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController, networks)
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, networks: List<Network>) {
    NavHost(navController = navController, startDestination = "scan") {
        composable("scan") {
            ScanScreen(navController)
        }
        composable("details/{bssid}") { backStackEntry ->
            NetworkDetailScreen(navController = navController, backStackEntry = backStackEntry, networks = DataManager.readAirodumpFile())
        }
        composable("captured_files") {
            CapturedFilesScreen()
        }
    }
}
