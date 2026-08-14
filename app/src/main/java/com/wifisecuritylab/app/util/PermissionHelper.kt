package com.wifisecuritylab.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun getRequiredPermissions(): List<String> {
        return buildList {
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.INTERNET)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
            }
        }
    }

    fun hasPermissions(context: Context): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> 
                "Required to scan nearby Wi-Fi networks and analyze their security characteristics."
            Manifest.permission.ACCESS_COARSE_LOCATION -> 
                "Required for approximate location to identify nearby Wi-Fi networks."
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> 
                "Required to continue Wi-Fi analysis when the app is in the background."
            Manifest.permission.NEARBY_WIFI_DEVICES -> 
                "Required on Android 13+ to discover and analyze nearby Wi-Fi networks without needing full location access."
            Manifest.permission.CHANGE_WIFI_STATE -> 
                "Required to enable/disable Wi-Fi radio and start the laboratory hotspot."
            Manifest.permission.INTERNET -> 
                "Required to run the local educational portal server."
            else -> "This permission is required for laboratory Wi-Fi functionality."
        }
    }
}
