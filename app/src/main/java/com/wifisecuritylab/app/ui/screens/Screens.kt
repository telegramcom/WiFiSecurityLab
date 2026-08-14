package com.wifisecuritylab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifisecuritylab.app.Screen
import com.wifisecuritylab.app.data.model.ClientInfo
import com.wifisecuritylab.app.data.model.LabConfiguration
import com.wifisecuritylab.app.data.model.LabEvent
import com.wifisecuritylab.app.data.model.LabStatus
import com.wifisecuritylab.app.data.model.WiFiNetwork
import com.wifisecuritylab.app.ui.viewmodel.MainViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(viewModel: MainViewModel, onNavigate: (String) -> Unit) {
    val status by viewModel.labStatus.collectAsState()
    val config by viewModel.labConfig.collectAsState()
    val events by viewModel.events.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val serverRunning by viewModel.serverRunning.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("WiFiSecurityLab~") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Authorized laboratory controller",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Use only with explicit permission. This app never stores real passwords.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                StatusCard(status, config.ssid, serverRunning, viewModel)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.LabConfig.route) }
                    ) { Text("Lab setup") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.LiveEvents.route) }
                    ) { Text("Events (${events.size})") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.ClientInfo.route) }
                    ) { Text("Clients (${clients.size})") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Detection.route) }
                    ) { Text("Wi-Fi scan") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.AttackFlow.route) }
                    ) { Text("Awareness flow") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Settings.route) }
                    ) { Text("Settings") }
                }
            }
            item {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium)
            }
            if (events.isEmpty()) {
                item { Text("No events yet. Start a lab to begin the demonstration.") }
            } else {
                items(events.take(5), key = { it.id }) { event ->
                    EventRow(event)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    status: LabStatus,
    ssid: String,
    serverRunning: Boolean,
    viewModel: MainViewModel
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lab status", style = MaterialTheme.typography.titleMedium)
            Text(status.name, fontWeight = FontWeight.Bold, color = statusColor(status))
            Text("SSID: $ssid")
            Text("Portal server: ${if (serverRunning) "running on port 8080" else "stopped"}")
            if (status == LabStatus.RUNNING || status == LabStatus.STARTING) {
                Button(onClick = viewModel::stopLabWifi) { Text("Stop lab") }
            } else {
                Button(onClick = { viewModel.createLabWifi(ssid, LabConfiguration.SecurityType.OPEN) }) {
                    Text("Start LAB-WIFI")
                }
            }
        }
    }
}

private fun statusColor(status: LabStatus) = when (status) {
    LabStatus.RUNNING -> MaterialTheme.colorScheme.primary
    LabStatus.ERROR -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
fun LabConfigScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val config by viewModel.labConfig.collectAsState()
    var ssid by remember(config.ssid) { mutableStateOf(config.ssid) }
    var security by remember(config.securityType) { mutableStateOf(config.securityType) }

    ScreenScaffold("Lab setup", onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Configure a local-only training network.", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("Laboratory SSID") },
                singleLine = true
            )
            Text("Security mode: ${security.name}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { security = LabConfiguration.SecurityType.OPEN }) {
                    Text("Open test network")
                }
                OutlinedButton(onClick = { security = LabConfiguration.SecurityType.WPA2_TEST }) {
                    Text("WPA2 test mode")
                }
            }
            Button(
                enabled = ssid.isNotBlank(),
                onClick = { viewModel.createLabWifi(ssid.trim(), security) }
            ) { Text("Create lab hotspot") }
            OutlinedButton(onClick = viewModel::stopLabWifi) { Text("Stop hotspot") }
            Text(
                "Portal address: ${viewModel.portalAddress.collectAsState().value}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LiveEventsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val events by viewModel.events.collectAsState()
    ScreenScaffold("Live events", onBack) { padding ->
        if (events.isEmpty()) {
            EmptyState("No laboratory events have been recorded.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { EventRow(it) }
            }
        }
    }
}

@Composable
private fun EventRow(event: LabEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.message, fontWeight = FontWeight.SemiBold)
                Text(event.type.name, style = MaterialTheme.typography.labelSmall)
            }
            if (event.details.isNotBlank()) Text(event.details, style = MaterialTheme.typography.bodySmall)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(event.timestamp)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun ClientInfoScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val clients by viewModel.clients.collectAsState()
    ScreenScaffold("Connected clients", onBack) { padding ->
        if (clients.isEmpty()) {
            EmptyState("No test client is connected yet.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clients) { ClientRow(it) }
            }
        }
    }
}

@Composable
private fun ClientRow(client: ClientInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(client.deviceName ?: "Unknown test device", fontWeight = FontWeight.SemiBold)
            Text("IP: ${client.ipAddress ?: "not reported"}")
            Text("Connection: ${client.connectionType} · ${client.status}")
        }
    }
}

@Composable
fun DetectionScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val networks by viewModel.scanResults.collectAsState()
    val scanning by viewModel.isScanning.collectAsState()
    ScreenScaffold("Rogue Wi-Fi detection", onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "Scan nearby networks and compare them with the authorized lab SSID.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = viewModel::startWifiScan, enabled = !scanning) {
                Text(if (scanning) "Scanning…" else "Scan nearby Wi-Fi")
            }
            Spacer(Modifier.height(12.dp))
            if (networks.isEmpty()) {
                Text("No scan results yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(networks, key = { "${it.ssid}-${it.bssid}" }) { NetworkRow(it) }
                }
            }
        }
    }
}

@Composable
private fun NetworkRow(network: WiFiNetwork) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(network.ssid.ifBlank { "Hidden network" }, fontWeight = FontWeight.SemiBold)
            Text("${network.capabilities.ifBlank { "Unknown security" }} · signal ${network.level} dBm")
            if (network.isConnected) Text("Connected to this device", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AttackFlowScreen(onBack: () -> Unit) {
    ScreenScaffold("Security-awareness flow", onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Controlled demonstration", style = MaterialTheme.typography.headlineSmall)
            Text(
                "The educational portal uses synthetic values only. After submission, it shows why "
                    + "untrusted Wi-Fi login pages are risky."
            )
            AwarenessStep("1", "Connect Phone B to LAB-WIFI")
            AwarenessStep("2", "Open the local portal at the displayed address")
            AwarenessStep("3", "Submit only the prefilled synthetic values")
            AwarenessStep("4", "Review the warning and discuss safer habits")
            Text(
                "Passwords are never stored and are shown as [REDACTED — SIMULATION].",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AwarenessStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(number, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text)
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val config by viewModel.labConfig.collectAsState()
    var consent by remember(config.requireConsent) { mutableStateOf(config.requireConsent) }
    var redact by remember(config.redactCredentials) { mutableStateOf(config.redactCredentials) }

    ScreenScaffold("Settings", onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingToggle("Require participant consent", consent) {
                consent = it
                viewModel.updateLabConfig(config.copy(requireConsent = it))
            }
            SettingToggle("Redact credential fields", redact) {
                redact = it
                viewModel.updateLabConfig(config.copy(redactCredentials = it))
            }
            HorizontalDivider()
            Text("Local server port: 8080")
            Text("Minimum Android version: API 26")
            Text("No cloud services or remote telemetry are used.")
        }
    }
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        content = content
    )
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}