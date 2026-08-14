package com.wifisecuritylab.app.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wifisecuritylab.app.data.model.ClientInfo
import com.wifisecuritylab.app.data.model.LabConfiguration
import com.wifisecuritylab.app.data.model.LabEvent
import com.wifisecuritylab.app.data.model.LabStatus
import com.wifisecuritylab.app.data.model.PortalSubmission
import com.wifisecuritylab.app.manager.HotspotManager
import com.wifisecuritylab.app.manager.WifiScanManager
import com.wifisecuritylab.app.service.LabHttpService
import com.wifisecuritylab.app.service.LocalHttpServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val hotspotManager = HotspotManager(context)
    private val wifiScanManager = WifiScanManager(context)

    private val _labStatus = MutableStateFlow(LabStatus.STOPPED)
    val labStatus: StateFlow<LabStatus> = _labStatus.asStateFlow()

    private val _labConfig = MutableStateFlow(LabConfiguration())
    val labConfig: StateFlow<LabConfiguration> = _labConfig.asStateFlow()

    private val _events = MutableStateFlow<List<LabEvent>>(emptyList())
    val events: StateFlow<List<LabEvent>> = _events.asStateFlow()

    private val _clients = MutableStateFlow<List<ClientInfo>>(emptyList())
    val clients: StateFlow<List<ClientInfo>> = _clients.asStateFlow()

    private val _scanResults = MutableStateFlow(wifiScanManager.scanResults.value)
    val scanResults = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _serverRunning = MutableStateFlow(false)
    val serverRunning = _serverRunning.asStateFlow()

    private val _portalAddress = MutableStateFlow("192.168.43.1:8080")
    val portalAddress = _portalAddress.asStateFlow()

    private var httpServer: LocalHttpServer? = null

    init {
        viewModelScope.launch {
            hotspotManager.hotspotState.collect { state ->
                when (state) {
                    is HotspotManager.HotspotState.Running -> {
                        _labStatus.value = LabStatus.RUNNING
                        _portalAddress.value = "${state.gateway ?: "192.168.43.1"}:8080"
                        addEvent(LabEvent.EventType.SUCCESS, "LAB-WIFI started", "Hotspot active")
                        startHttpServer()
                    }
                    is HotspotManager.HotspotState.Error -> {
                        _labStatus.value = LabStatus.ERROR
                        _errorMessage.value = state.message
                        addEvent(LabEvent.EventType.ERROR, "Hotspot failed", state.message)
                    }
                    is HotspotManager.HotspotState.Starting -> {
                        _labStatus.value = LabStatus.STARTING
                    }
                    else -> {
                        _labStatus.value = LabStatus.STOPPED
                        stopHttpServer()
                    }
                }
            }
        }

        viewModelScope.launch {
            wifiScanManager.scanResults.collect {
                _scanResults.value = it
            }
        }
        viewModelScope.launch {
            wifiScanManager.isScanning.collect {
                _isScanning.value = it
            }
        }
    }

    fun createLabWifi(ssid: String, securityType: LabConfiguration.SecurityType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            _errorMessage.value = "Local-only hotspot requires Android 8.0+. Please use system settings to create a hotspot."
            addEvent(LabEvent.EventType.ERROR, "Unsupported Android version", "API ${Build.VERSION.SDK_INT} < 26")
            return
        }

        _labConfig.value = _labConfig.value.copy(
            ssid = ssid,
            securityType = securityType
        )
        addEvent(LabEvent.EventType.INFO, "Starting LAB-WIFI", "SSID: $ssid")
        hotspotManager.startHotspot(ssid)
    }

    fun stopLabWifi() {
        hotspotManager.stopHotspot()
        stopHttpServer()
        _clients.value = emptyList()
        addEvent(LabEvent.EventType.INFO, "LAB-WIFI stopped", "Laboratory network terminated")
    }

    fun startWifiScan() {
        wifiScanManager.register()
        wifiScanManager.startScan()
    }

    fun stopWifiScan() {
        wifiScanManager.unregister()
    }

    fun updateLabConfig(config: LabConfiguration) {
        _labConfig.value = config
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun startHttpServer() {
        httpServer?.stop()
        httpServer = LocalHttpServer(8080).apply {
            setSubmissionCallback { submission ->
                viewModelScope.launch {
                    handlePortalSubmission(submission)
                }
            }
            start()
        }
        _serverRunning.value = true
        addEvent(LabEvent.EventType.SUCCESS, "Portal server started", "Address: ${_portalAddress.value}")
    }

    private fun stopHttpServer() {
        httpServer?.stop()
        httpServer = null
        _serverRunning.value = false
        addEvent(LabEvent.EventType.INFO, "Portal server stopped", "")
    }

    private fun handlePortalSubmission(submission: PortalSubmission) {
        addEvent(
            LabEvent.EventType.SECURITY,
            "Synthetic form submitted",
            "Client: ${submission.clientIp}, User: ${submission.syntheticUsername}, Password: [REDACTED — SIMULATION]"
        )
        addEvent(
            LabEvent.EventType.SECURITY,
            "Credential field REDACTED",
            "No real credential was collected or stored"
        )
        addEvent(
            LabEvent.EventType.SECURITY,
            "Security warning displayed",
            "Educational warning served to client ${submission.clientIp}"
        )
    }

    private fun addEvent(type: LabEvent.EventType, message: String, details: String = "") {
        val newEvent = LabEvent(type = type, message = message, details = details)
        _events.value = listOf(newEvent) + _events.value.take(99)
    }

    fun addManualEvent(type: LabEvent.EventType, message: String, details: String = "") {
        addEvent(type, message, details)
    }

    override fun onCleared() {
        super.onCleared()
        stopHttpServer()
        wifiScanManager.unregister()
    }
}
