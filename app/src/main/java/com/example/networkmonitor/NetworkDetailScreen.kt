package com.example.networkmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import com.example.networkmonitor.data.DataManager
import com.example.networkmonitor.models.Network

@Composable
fun NetworkDetailScreen(navController: NavHostController, backStackEntry: NavBackStackEntry, networks: List<Network>) {
    val bssid = backStackEntry.arguments?.getString("bssid")
    val network = networks.find { it.bssid == bssid }

    var isMonitoring by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Status: Idle") }
    var output by remember { mutableStateOf("") }

    network?.let {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(text = "Network Details", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "SSID: ${it.essid}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "BSSID: ${it.bssid}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Channel: ${it.channel}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Privacy: ${it.privacy}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Cipher: ${it.cipher}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Authentication: ${it.authentication}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Power: ${it.power}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isMonitoring) {
                        DataManager.stopNetworkMonitoring(it) { newStatus, newOutput ->
                            status = newStatus
                            output = newOutput
                        }
                    } else {
                        DataManager.startNetworkMonitoring(it) { newStatus, newOutput ->
                            status = newStatus
                            output = newOutput
                        }
                    }
                    isMonitoring = !isMonitoring
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(if (isMonitoring) "Stop Monitoring" else "Start Monitoring")
            }

            Button(
                onClick = {
                    DataManager.deauthenticate(it) { newStatus, newOutput ->
                        status = newStatus
                        output = newOutput
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("Deauthenticate")
            }


            Text(
                text = status,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = output,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    } ?: run {
        Text("Network not found", modifier = Modifier.padding(16.dp))
    }
}
