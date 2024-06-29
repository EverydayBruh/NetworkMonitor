package com.example.networkmonitor

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.networkmonitor.adapters.CapturedFileList
import com.example.networkmonitor.models.CapturedFile
import com.example.networkmonitor.data.DataManager
import kotlinx.coroutines.launch

@Composable
fun CapturedFilesScreen() {
    var files by remember { mutableStateOf(listOf<CapturedFile>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        files = DataManager.loadCapturedFiles()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(text = "Captured Files", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        CapturedFileList(
            files = files,
            onFileClick = { file ->
                scope.launch {
                    DataManager.checkFileStatus(file) { status, details ->
                        val updatedFile = when (status) {
                            "in_process" -> file.copy(
                                status = "In Process (${details["progress"]}%)",
                                queuePosition = -1
                            )
                            "queued" -> file.copy(
                                status = "Queued",
                                queuePosition = details["position"] as Int
                            )
                            "processed" -> file.copy(
                                status = if (details["success"] as Boolean) "Cracked" else "Failed",
                                queuePosition = -1
                            )
                            "not_found" -> file.copy(status = "Not Found", queuePosition = -1)
                            else -> file.copy(status = "Error", queuePosition = -1)
                        }
                        files = files.map { if (it.fileName == file.fileName) updatedFile else it }
                    }
                }
            },
            onSendClick = { file ->
                scope.launch {
                    val bssid = "00:11:22:33:44:55"
                    val ssid = "ExampleNetwork"

                    file.status = "Sending"
                    files = files.map { if (it.fileName == file.fileName) file else it }

                    DataManager.sendFileToServer(file, bssid, ssid) { success, message ->
                        file.status = if (success) "Sent" else "Failed to send"
                        files = files.map { if (it.fileName == file.fileName) file else it }

                        Log.d("CapturedFilesScreen", message)
                    }
                }
            }
        )
    }
}