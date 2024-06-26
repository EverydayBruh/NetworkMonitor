package com.example.networkmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.navigation.NavHostController
import com.example.networkmonitor.data.DataManager
import com.example.networkmonitor.models.Network
import com.example.networkmonitor.adapters.NetworkList
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun ScanScreen(navController: NavHostController) {
    var status by rememberSaveable { mutableStateOf("Status: Idle") }
    var output by rememberSaveable { mutableStateOf("") }
    var networks by rememberSaveable { mutableStateOf(listOf<Network>()) }
    var isScanning by rememberSaveable { mutableStateOf(false) }
    var showAdvancedOptions by rememberSaveable { mutableStateOf(false) }
    var command by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (networks.isEmpty()) {
            networks = DataManager.readAirodumpFile()
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Network Monitor", style = MaterialTheme.typography.headlineSmall)

            IconButton(onClick = { showAdvancedOptions = !showAdvancedOptions }) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Advanced Options")
            }
        }

        if (showAdvancedOptions) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                    Text("Enter Monitor Mode")
                }

                Button(
                    onClick = {
                        DataManager.startManagedMode { newStatus, newOutput ->
                            status = newStatus
                            output = newOutput
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("Enter Normal Mode")
                }
                Button(
                    onClick = {
                        navController.navigate("captured_files")
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("Captured Files")
                }
            }
        }

        Button(
            onClick = {
                if (isScanning) {
                    DataManager.stopAirodump { newStatus, newOutput ->
                        status = newStatus
                        output = newOutput
                        networks = DataManager.readAirodumpFile()
                    }
                    isScanning = false
                } else {
                    DataManager.startAirodump { newStatus, newOutput ->
                        status = newStatus
                        output = newOutput
                    }
                    isScanning = true
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text(if (isScanning) "Stop Scan" else "Start Scan")
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
            NetworkList(networks = networks, onNetworkClick = { network ->
                navController.navigate("details/${network.bssid}")
            })
        }
    }
}
