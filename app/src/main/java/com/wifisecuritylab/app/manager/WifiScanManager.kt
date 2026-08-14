package com.wifisecuritylab.app.manager

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class WifiScanManager(private val context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                _isScanning.value = false
                val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    wifiManager.scanResults
                } else {
                    @Suppress("DEPRECATION")
                    wifiManager.scanResults
                }
                _scanResults.value = results ?: emptyList()
            }
        }
    }

    fun register() {
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(scanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(scanReceiver, filter)
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(scanReceiver)
        } catch (e: IllegalArgumentException) {
            // Not registered
        }
    }

    fun startScan(): Boolean {
        _isScanning.value = true
        return try {
            wifiManager.startScan()
        } catch (e: SecurityException) {
            _isScanning.value = false
            false
        } catch (e: Exception) {
            _isScanning.value = false
            false
        }
    }

    fun getConnectionInfo(): android.net.wifi.WifiInfo? {
        return try {
            wifiManager.connectionInfo
        } catch (e: SecurityException) {
            null
        }
    }
}
