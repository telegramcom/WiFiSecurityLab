package com.wifisecuritylab.app.data.model

import java.util.UUID

enum class LabStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

data class LabEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: EventType,
    val message: String,
    val details: String = ""
) {
    enum class EventType {
        INFO,
        WARNING,
        SUCCESS,
        ERROR,
        SECURITY
    }
}

data class ClientInfo(
    val macAddress: String? = null,
    val ipAddress: String? = null,
    val deviceName: String? = null,
    val connectionType: String = "Wi-Fi",
    val status: String = "Connected",
    val signalStrength: Int? = null,
    val firstSeen: Long = System.currentTimeMillis()
)

data class WiFiNetwork(
    val ssid: String,
    val bssid: String? = null,
    val capabilities: String = "",
    val frequency: Int = 0,
    val level: Int = 0,
    val isConnected: Boolean = false
)

data class PortalSubmission(
    val timestamp: Long = System.currentTimeMillis(),
    val clientIp: String = "",
    val syntheticUsername: String = "",
    val passwordStatus: String = "[REDACTED — SIMULATION]",
    val userAgent: String = ""
)

data class LabConfiguration(
    val ssid: String = "LAB-WIFI",
    val securityType: SecurityType = SecurityType.OPEN,
    val portalAddress: String = "192.168.43.1:8080",
    val requireConsent: Boolean = true,
    val redactCredentials: Boolean = true
) {
    enum class SecurityType {
        OPEN,
        WPA2_TEST
    }
}
