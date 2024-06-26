package com.example.networkmonitor.adapters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.networkmonitor.models.CapturedFile

@Composable
fun CapturedFileList(
    files: List<CapturedFile>,
    onFileClick: (CapturedFile) -> Unit,
    onSendClick: (CapturedFile) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files) { file ->
            CapturedFileItem(file = file, onClick = { onFileClick(file) }, onSendClick = { onSendClick(file) })
        }
    }
}

@Composable
fun CapturedFileItem(file: CapturedFile, onClick: () -> Unit, onSendClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = file.fileName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Status: ${file.status}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (file.queuePosition >= 0) {
            Text(
                text = "Queue Position: ${file.queuePosition}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSendClick, modifier = Modifier.fillMaxWidth()) {
            Text("Send to Server")
        }
    }
}
