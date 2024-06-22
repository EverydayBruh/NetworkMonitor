package com.example.networkmonitor.data

import android.util.Log
import com.example.networkmonitor.models.Network
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader

object DataManager {
    private const val CHROOT_PATH = "chroot /data/local/nhsystem/kali-arm64 /bin/sh -c 'export PATH=/usr/sbin:/usr/bin:/sbin:/bin && export LC_ALL=C.UTF-8 &&"

    fun runCustomCommand(command: String, updateStatus: (String, String) -> Unit) {
        updateStatus("Status: Running Command", "")
        val fullCommand = "$CHROOT_PATH $command'"
        executeCommand(fullCommand, updateStatus)
    }

    fun runKillAndStartMonitorMode(updateStatus: (String, String) -> Unit) {
        updateStatus("Status: Killing Processes", "")
        val killCommand = "$CHROOT_PATH airmon-ng check kill'"
        val startMonitorModeCommand = "$CHROOT_PATH airmon-ng start wlan0'"

        executeCommand(killCommand) { killStatus, killOutput ->
            if (killStatus.contains("Failed")) {
                updateStatus(killStatus, killOutput)
            } else {
                executeCommand(startMonitorModeCommand, updateStatus)
            }
        }
    }

    fun startAirodump(updateStatus: (String, String) -> Unit) {
        updateStatus("Status: Starting Airodump-ng", "")
        val deleteFilesCommand = "rm -f ./output-01.cap ./output-01.csv ./output-01.kismet.netxml ./output-01.kismet.csv ./output-01.log.csv"
        val airodumpCommand = "airodump-ng -w ./output wlan0"
        val fullCommand = "$CHROOT_PATH $deleteFilesCommand && $airodumpCommand'"

        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", fullCommand))
            updateStatus("Status: Airodump-ng Running", "Airodump-ng has started.")
        } catch (e: IOException) {
            Log.e("DataManager", "Failed to start Airodump-ng", e)
            updateStatus("Status: Failed to start Airodump-ng", e.message ?: "")
        }
    }

    fun stopAirodump(updateStatus: (String, String) -> Unit) {
        updateStatus("Status: Stopping Airodump-ng", "")
        val stopCommand = "killall -9 airodump-ng"
        val fullCommand = "$CHROOT_PATH $stopCommand'"

        executeCommand(fullCommand) { status, output ->
            if (status.contains("Failed")) {
                updateStatus(status, output)
            } else {
                val networks = readAirodumpFile()
                updateStatus("Status: Airodump-ng Stopped", "Networks found: ${networks.size}")
            }
        }
    }

    fun readAirodumpFile(): List<Network> {
        val outputFile = File("/data/local/nhsystem/kali-arm64/output-01.csv")
        return if (outputFile.exists()) {
            parseCsvFile(outputFile)
        } else {
            Log.w("DataManager", "Output file not found")
            emptyList()
        }
    }

    private fun parseCsvFile(file: File): List<Network> {
        val networks = mutableListOf<Network>()
        try {
            BufferedReader(FileReader(file)).use { reader ->
                var line = reader.readLine() // Пропускаем заголовок
                while (line != null) {
                    line = reader.readLine()
                    if (line != null && line.isNotBlank() && !line.startsWith("Station MAC") && !line.startsWith("BSSID")) {
                        val columns = line.split(",").map { it.trim() }
                        if (columns.size >= 14) {
                            val network = Network(
                                bssid = columns[0],
                                firstTimeSeen = columns[1],
                                lastTimeSeen = columns[2],
                                channel = columns[3],
                                speed = columns[4],
                                privacy = columns[5],
                                cipher = columns[6],
                                authentication = columns[7],
                                power = columns[8],
                                beacons = columns[9],
                                essid = columns[13]
                            )
                            networks.add(network)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("DataManager", "Error reading CSV file", e)
        }
        return networks
    }

    private fun executeCommand(command: String, updateStatus: (String, String) -> Unit) {
        try {
            Log.d("DataManager", "Executing command: $command")
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
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
            Log.d("DataManager", "Command output: $finalOutput")
        } catch (e: IOException) {
            Log.e("DataManager", "Command execution failed", e)
            updateStatus("Status: Command Failed", e.message ?: "")
        } catch (e: InterruptedException) {
            Log.e("DataManager", "Command execution interrupted", e)
            updateStatus("Status: Command Interrupted", e.message ?: "")
        }
    }
}
