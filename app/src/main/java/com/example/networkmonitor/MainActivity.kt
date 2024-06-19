package com.example.networkmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.networkmonitor.ui.theme.NetworkMonitorTheme
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkMonitorTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        var status by remember { mutableStateOf("Status: Idle") }
        var output by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("") }

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
                    runCustomCommand(command) { newStatus, newOutput ->
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
                    runKillAndStartMonitorMode() { newStatus, newOutput ->
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
                    startAirodump() { newStatus, newOutput ->
                        status = newStatus
                        output = newOutput
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("Start Airodump-ng")
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
    }

    private fun runCustomCommand(command: String, updateStatus: (String, String) -> Unit) {
        updateStatus("Status: Running Command", "")
        val fullCommand = "chroot /data/local/nhsystem/kali-arm64 /bin/sh -c 'export PATH=/usr/sbin:/usr/bin:/sbin:/bin && export LC_ALL=C.UTF-8 && $command'"
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", fullCommand))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()
            val errorOutput = StringBuilder()

            var line: String? = reader.readLine()
            while (line != null) {
                output.append(line).append("\n")
                line = reader.readLine()
            }
            reader.close()

            line = errorReader.readLine()
            while (line != null) {
                errorOutput.append(line).append("\n")
                line = errorReader.readLine()
            }
            errorReader.close()

            process.waitFor()
            val exitCode = process.exitValue()

            val finalOutput = if (exitCode == 0) {
                "Output:\n$output"
            } else {
                "Error:\n$errorOutput"
            }

            updateStatus("Status: Command Completed with exit code $exitCode", finalOutput)
        } catch (e: IOException) {
            e.printStackTrace()
            updateStatus("Status: Command Failed", e.message ?: "")
        } catch (e: InterruptedException) {
            e.printStackTrace()
            updateStatus("Status: Command Interrupted", e.message ?: "")
        }
    }

    private fun runKillAndStartMonitorMode(updateStatus: (String, String) -> Unit) {
        updateStatus("Status: Killing Processes", "")
        val killCommand = "chroot /data/local/nhsystem/kali-arm64 /bin/sh -c 'export PATH=/usr/sbin:/usr/bin:/sbin:/bin && export LC_ALL=C.UTF-8 && airmon-ng check kill'"
        val startMonitorModeCommand = "chroot /data/local/nhsystem/kali-arm64 /bin/sh -c 'export PATH=/usr/sbin:/usr/bin:/sbin:/bin && export LC_ALL=C.UTF-8 && airmon-ng start wlan0'"

        try {
            // Выполнение killCommand
            val killProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", killCommand))
            val killReader = BufferedReader(InputStreamReader(killProcess.inputStream))
            val killErrorReader = BufferedReader(InputStreamReader(killProcess.errorStream))
            val killOutput = StringBuilder()
            val killErrorOutput = StringBuilder()

            var line: String? = killReader.readLine()
            while (line != null) {
                killOutput.append(line).append("\n")
                line = killReader.readLine()
            }
            killReader.close()

            line = killErrorReader.readLine()
            while (line != null) {
                killErrorOutput.append(line).append("\n")
                line = killErrorReader.readLine()
            }
            killErrorReader.close()

            killProcess.waitFor()
            val killExitCode = killProcess.exitValue()

            if (killExitCode != 0) {
                updateStatus("Status: Kill Command Failed", "Error:\n$killErrorOutput")
                return
            }

            // Выполнение startMonitorModeCommand после завершения killCommand
            val startMonitorModeProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", startMonitorModeCommand))
            val startMonitorModeReader = BufferedReader(InputStreamReader(startMonitorModeProcess.inputStream))
            val startMonitorModeErrorReader = BufferedReader(InputStreamReader(startMonitorModeProcess.errorStream))
            val startMonitorModeOutput = StringBuilder()
            val startMonitorModeErrorOutput = StringBuilder()

            line = startMonitorModeReader.readLine()
            while (line != null) {
                startMonitorModeOutput.append(line).append("\n")
                line = startMonitorModeReader.readLine()
            }
            startMonitorModeReader.close()

            line = startMonitorModeErrorReader.readLine()
            while (line != null) {
                startMonitorModeErrorOutput.append(line).append("\n")
                line = startMonitorModeErrorReader.readLine()
            }
            startMonitorModeErrorReader.close()

            startMonitorModeProcess.waitFor()
            val startMonitorModeExitCode = startMonitorModeProcess.exitValue()

            val finalOutput = if (startMonitorModeExitCode == 0) {
                "Output:\n$startMonitorModeOutput"
            } else {
                "Error:\n$startMonitorModeErrorOutput"
            }

            updateStatus("Status: Command Completed with exit code $startMonitorModeExitCode", finalOutput)
        } catch (e: IOException) {
            e.printStackTrace()
            updateStatus("Status: Command Failed", e.message ?: "")
        } catch (e: InterruptedException) {
            e.printStackTrace()
            updateStatus("Status: Command Interrupted", e.message ?: "")
        }
    }

    private fun startAirodump(updateStatus: (String, String) -> Unit) {
        updateStatus("Status: Starting Airodump-ng", "")
        val deleteFilesCommand = "rm -f ./output-01.cap ./output-01.csv ./output-01.kismet.netxml ./output-01.kismet.csv ./output-01.log.csv"
        val airodumpCommand = "airodump-ng -w ./output wlan0"
        val fullCommand = "chroot /data/local/nhsystem/kali-arm64 /bin/sh -c 'export PATH=/usr/sbin:/usr/bin:/sbin:/bin && export LC_ALL=C.UTF-8 && $deleteFilesCommand && $airodumpCommand'"
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", fullCommand))
            // Считывание результата из output-файла
            val outputFile = File("/data/local/nhsystem/kali-arm64/output-01.csv")
            if (outputFile.exists()) {
                val output = outputFile.readText()
                updateStatus("Status: Airodump-ng Running", output)
            } else {
                updateStatus("Status: Airodump-ng Running", "Output file not found")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            updateStatus("Status: Failed to start Airodump-ng", e.message ?: "")
        }
    }
}
