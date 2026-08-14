package com.wifisecuritylab.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wifisecuritylab.app.ui.screens.AttackFlowScreen
import com.wifisecuritylab.app.ui.screens.ClientInfoScreen
import com.wifisecuritylab.app.ui.screens.DashboardScreen
import com.wifisecuritylab.app.ui.screens.DetectionScreen
import com.wifisecuritylab.app.ui.screens.LabConfigScreen
import com.wifisecuritylab.app.ui.screens.LiveEventsScreen
import com.wifisecuritylab.app.ui.screens.SettingsScreen
import com.wifisecuritylab.app.ui.theme.WiFiSecurityLabTheme
import com.wifisecuritylab.app.ui.viewmodel.MainViewModel
import com.wifisecuritylab.app.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            // Handle denied permissions gracefully
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent {
            WiFiSecurityLabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WiFiSecurityLabApp()
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = PermissionHelper.getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object LabConfig : Screen("lab_config")
    object LiveEvents : Screen("live_events")
    object ClientInfo : Screen("client_info")
    object Detection : Screen("detection")
    object AttackFlow : Screen("attack_flow")
    object Settings : Screen("settings")
}

@Composable
fun WiFiSecurityLabApp(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.LabConfig.route) {
            LabConfigScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LiveEvents.route) {
            LiveEventsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ClientInfo.route) {
            ClientInfoScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Detection.route) {
            DetectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AttackFlow.route) {
            AttackFlowScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
