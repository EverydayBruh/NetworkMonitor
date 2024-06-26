package com.example.networkmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.networkmonitor.adapters.CapturedFileList
import com.example.networkmonitor.models.CapturedFile
import com.example.networkmonitor.data.DataManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CapturedFilesScreen() {
    var files by remember { mutableStateOf(listOf<CapturedFile>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadCapturedFiles { newFiles ->
            files = newFiles
        }
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
                // Логика при клике на файл (например, показать детали)
            },
            onSendClick = { file ->
                coroutineScope.launch {
                    sendFileToServer(file) { updatedFiles ->
                        files = updatedFiles
                    }
                }
            }
        )
    }
}

private suspend fun loadCapturedFiles(
    updateFiles: (List<CapturedFile>) -> Unit
) {
    val capturedFiles = DataManager.getCapturedFiles()
    val newFiles = capturedFiles.map { file ->
        CapturedFile(
            fileName = file.name,
            status = "Pending",
            queuePosition = -1
        )
    }
    updateFiles(newFiles)
}

private suspend fun sendFileToServer(
    file: CapturedFile,
    updateFiles: (List<CapturedFile>) -> Unit
) {
    file.status = "Sending"
    updateFiles { currentFiles ->
        currentFiles.map { if (it == file) file.copy(status = "Sending") else it }
    }

    // Заглушка для отправки файла на сервер
    val result = DataManager.sendFileToServer(file.fileName)

    file.status = if (result) "Sent" else "Failed"
    updateFiles { currentFiles ->
        currentFiles.map { if (it == file) file else it }
    }

    // Заглушка для запроса статуса от сервера
    updateFileStatus(file, updateFiles)
}

private suspend fun updateFileStatus(
    file: CapturedFile,
    updateFiles: (List<CapturedFile>) -> Unit
) {
    val status = DataManager.getFileStatus(file.fileName)
    file.status = status
    updateFiles { currentFiles ->
        currentFiles.map { if (it == file) file.copy(status = status) else it }
    }
}