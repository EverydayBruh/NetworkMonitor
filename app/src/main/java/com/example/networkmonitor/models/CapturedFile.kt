package com.example.networkmonitor.models

data class CapturedFile(
    val fileName: String,
    var status: String = "Pending",
    var queuePosition: Int = -1
)
