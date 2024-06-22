package com.example.networkmonitor.models

data class Network(
    val bssid: String,
    val firstTimeSeen: String,
    val lastTimeSeen: String,
    val channel: String,
    val speed: String,
    val privacy: String,
    val cipher: String,
    val authentication: String,
    val power: String,
    val beacons: String,
    val essid: String
)
