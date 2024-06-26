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
import com.example.networkmonitor.models.Network

@Composable
fun NetworkList(
    networks: List<Network>,
    onNetworkClick: (Network) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(networks) { network ->
            NetworkItem(network = network, onClick = { onNetworkClick(network) })
        }
    }
}

@Composable
fun NetworkItem(network: Network, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = network.essid,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "BSSID: ${network.bssid}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Privacy: ${network.privacy}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
