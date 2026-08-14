package com.wifisecuritylab.app.manager

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.NetworkInterface

class HotspotManager(private val context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _hotspotState = MutableStateFlow<HotspotState>(HotspotState.Stopped)
    val hotspotState: StateFlow<HotspotState> = _hotspotState.asStateFlow()

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    sealed class HotspotState {
        object Stopped : HotspotState()
        object Starting : HotspotState()
        data class Running(
            val ssid: String? = null,
            val password: String? = null,
            val gateway: String? = null
        ) : HotspotState()
        data class Error(val message: String) : HotspotState()
        data class Limited(val message: String) : HotspotState()
    }

    fun canCreateHotspot(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    fun startHotspot(ssid: String = "LAB-WIFI") {
        if (!canCreateHotspot()) {
            _hotspotState.value = HotspotState.Error(
                "Local-only hotspot requires Android 8.0 (API 26) or higher."
            )
            return
        }

        if (_hotspotState.value is HotspotState.Running || _hotspotState.value is HotspotState.Starting) {
            return
        }

        _hotspotState.value = HotspotState.Starting

        try {
            wifiManager.startLocalOnlyHotspot(
                object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                        super.onStarted(reservation)
                        this@HotspotManager.reservation = reservation
                        // Android does not expose the actual SSID for local-only hotspots easily
                        // The system generates one. We report what we can.
                        Handler(Looper.getMainLooper()).postDelayed({
                            val gateway = getGatewayIp()
                            val generatedSsid: String?
                            val generatedPassword: String?
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                generatedSsid = reservation?.softApConfiguration?.ssid
                                generatedPassword = reservation?.softApConfiguration?.passphrase
                            } else {
                                @Suppress("DEPRECATION")
                                val legacyConfig = reservation?.wifiConfiguration
                                @Suppress("DEPRECATION")
                                generatedSsid = legacyConfig?.SSID?.trim('"')
                                @Suppress("DEPRECATION")
                                generatedPassword = legacyConfig?.preSharedKey
                            }
                            _hotspotState.value = HotspotState.Running(
                                ssid = generatedSsid ?: "AndroidShare_####",
                                password = generatedPassword,
                                gateway = gateway
                            )
                        }, 1500)
                    }

                    override fun onStopped() {
                        super.onStopped()
                        _hotspotState.value = HotspotState.Stopped
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        val reasonText = when (reason) {
                            WifiManager.LocalOnlyHotspotCallback.ERROR_INCOMPATIBLE_MODE -> 
                                "Incompatible mode: Wi-Fi may be in use for station connection."
                            WifiManager.LocalOnlyHotspotCallback.ERROR_TETHERING_DISALLOWED -> 
                                "Tethering is disallowed by device policy or carrier."
                            WifiManager.LocalOnlyHotspotCallback.ERROR_NO_CHANNEL -> 
                                "No valid Wi-Fi channel available."
                            else -> "Unknown error (code: $reason)"
                        }
                        _hotspotState.value = HotspotState.Error(
                            "Hotspot failed: $reasonText"
                        )
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: SecurityException) {
            _hotspotState.value = HotspotState.Error(
                "Permission denied: ${e.message}. Ensure location permission is granted."
            )
        } catch (e: Exception) {
            _hotspotState.value = HotspotState.Error(
                "Failed to start hotspot: ${e.message}"
            )
        }
    }

    fun stopHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                reservation?.close()
                reservation = null
            } catch (e: Exception) {
                // Ignore
            }
        }
        _hotspotState.value = HotspotState.Stopped
    }

    private fun getGatewayIp(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filter {
                    !it.isLoopbackAddress &&
                        it is java.net.Inet4Address &&
                        it.hostAddress?.startsWith("192.168.") == true
                }
                .sortedBy { address ->
                    if (address.hostAddress == "192.168.43.1") 0 else 1
                }
                .firstOrNull()
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }

    fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && it is java.net.Inet4Address }
                .firstOrNull()
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
