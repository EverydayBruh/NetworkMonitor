package com.example.networkmonitor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.networkmonitor.data.DataManager
import com.example.networkmonitor.models.Network
import java.io.File

@Composable
fun ScanScreen() {
    var status by remember { mutableStateOf("Status: Idle") }
    var output by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var networks by remember { mutableStateOf(listOf<Network>()) }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        TextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Enter Command") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                DataManager.runCustomCommand(command) { newStatus, newOutput ->
                    status = newStatus
                    output = newOutput
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Run Command")
        }

        Button(
            onClick = {
                DataManager.runKillAndStartMonitorMode { newStatus, newOutput ->
                    status = newStatus
                    output = newOutput
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Kill and Start Monitor Mode")
        }

        Button(
            onClick = {
                DataManager.startAirodump { newStatus, newOutput ->
                    status = newStatus
                    output = newOutput
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Start Airodump-ng")
        }

        Button(
            onClick = {
                DataManager.stopAirodump { newStatus, newOutput ->
                    status = newStatus
                    output = newOutput
                    networks = DataManager.readAirodumpFile()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Stop Airodump-ng")
        }

        Text(
            text = status,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            text = output,
            modifier = Modifier.padding(top = 16.dp)
        )

        if (networks.isNotEmpty()) {
            networks.forEach { network ->
                Text(text = "SSID: ${network.essid}, BSSID: ${network.bssid}, Channel: ${network.channel}, Power: ${network.power}")
            }
        }
    }
}
