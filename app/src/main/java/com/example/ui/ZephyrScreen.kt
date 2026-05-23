package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.api.ZephyrState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZephyrScreen(
    modifier: Modifier = Modifier,
    viewModel: ZephyrViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ipAddress by viewModel.ipAddress.collectAsStateWithLifecycle()
    
    var showIpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zephyr Cooler", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showIpDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is ZephyrUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ZephyrUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showIpDialog = true }) {
                            Text("Change Device IP")
                        }
                    }
                }
                is ZephyrUiState.Success -> {
                    DashboardContent(
                        state = state.state,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showIpDialog) {
        var tempIp by remember { mutableStateOf(ipAddress.removePrefix("http://").removeSuffix("/")) }
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            title = { Text("Device IP Address") },
            text = {
                OutlinedTextField(
                    value = tempIp,
                    onValueChange = { tempIp = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateIpAddress(tempIp)
                    showIpDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DashboardContent(
    state: ZephyrState,
    viewModel: ZephyrViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            LedStrip(leds = state.leds)
        }

        item {
            AnimatedVisibility(visible = state.pause.isNotEmpty()) {
                val message = when (state.pause) {
                    "water" -> "Low water — cooler paused"
                    "sched" -> "Outside schedule — cooler paused"
                    "hum" -> "High humidity — cooler paused"
                    else -> "Paused"
                }
                val color = when (state.pause) {
                    "water" -> MaterialTheme.colorScheme.errorContainer
                    "sched" -> MaterialTheme.colorScheme.tertiaryContainer
                    "hum" -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = color),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        
        item {
            ModeControl(
                currentMode = state.mode,
                onModeChange = { viewModel.setMode(it) }
            )
        }

        item {
            EnvironmentCard(
                state = state,
                onFanToggle = { viewModel.setFan(it) }
            )
        }

        item {
            WaterCard(
                state = state,
                onPumpToggle = { viewModel.setPump(it) }
            )
        }

        item {
            ThresholdsCard(
                state = state,
                onTempChange = { viewModel.setTempThresh(it) },
                onHumChange = { viewModel.setHumThresh(it) },
                onBrightnessChange = { viewModel.setBrightness(it) }
            )
        }
    }
}

@Composable
fun LedStrip(leds: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp)) {
            LedDot(hex = leds.getOrNull(0) ?: "#000")
            Spacer(modifier = Modifier.height(4.dp))
            Text("STATUS", style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 1..3) {
                    LedDot(hex = leds.getOrNull(i) ?: "#000")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("WATER", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun LedDot(hex: String) {
    val color = try {
        val paddedHex = if (hex.length == 4) {
            "#" + hex[1] + hex[1] + hex[2] + hex[2] + hex[3] + hex[3] 
        } else {
            hex
        }
        // Arduino may send #000 for off. In sketch: "#000" or "#000000"
        if (paddedHex == "#000000" || paddedHex == "#000") Color.DarkGray else Color(paddedHex.toColorInt())
    } catch (e: Exception) {
        Color.DarkGray
    }
    
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
    )
}

@Composable
fun ModeControl(currentMode: String, onModeChange: (String) -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onModeChange("auto") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentMode == "auto") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Text("Auto")
                }
                OutlinedButton(
                    onClick = { onModeChange("on") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentMode == "on") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Text("On")
                }
                OutlinedButton(
                    onClick = { onModeChange("off") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (currentMode == "off") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Text("Off")
                }
            }
        }
    }
}

@Composable
fun EnvironmentCard(state: ZephyrState, onFanToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Environment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("${String.format("%.1f", state.temp)}°", style = MaterialTheme.typography.displayMedium)
                    Text("Temperature", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("${state.hum.toInt()}%", style = MaterialTheme.typography.displayMedium)
                    Text("Humidity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (state.fan) "Fan On" else "Fan Off", fontWeight = FontWeight.Medium)
                    val statusText = if (state.mode == "auto") {
                        if (state.fan) "Auto · Running" else (if (state.pause.isNotEmpty()) "Auto · Paused" else "Auto · Standby")
                    } else {
                        if (state.mode == "off") "Manual · Forced Off" else "Manual · Forced On"
                    }
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.fan,
                    onCheckedChange = { if (state.mode != "auto") onFanToggle(it) },
                    enabled = state.mode != "auto"
                )
            }
        }
    }
}

@Composable
fun WaterCard(state: ZephyrState, onPumpToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Water System", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tank Capacity", style = MaterialTheme.typography.bodyMedium)
                Text("${state.water}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (state.water / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (state.water < 15) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sensor Distance: ${if (state.dist > 0) String.format("%.1f cm", state.dist) else "--"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pump", fontWeight = FontWeight.Medium)
                val statusText = if (state.mode == "auto") {
                    "Follows fan (auto)"
                } else {
                    if (state.pump) "Pump on" else "Pump off"
                }
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = state.pump,
                onCheckedChange = { if (state.mode != "auto") onPumpToggle(it) },
                enabled = state.mode != "auto"
            )
        }
    }
}

@Composable
fun ThresholdsCard(
    state: ZephyrState,
    onTempChange: (Float) -> Unit,
    onHumChange: (Int) -> Unit,
    onBrightnessChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings & Thresholds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Temperature Trigger", fontWeight = FontWeight.Medium)
                Text("${String.format("%.1f", state.tempTh)}°C", color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = state.tempTh,
                onValueChange = onTempChange,
                valueRange = 20f..40f,
                steps = 39 // (40-20)*2 - 1 for 0.5 steps
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Humidity Limit", fontWeight = FontWeight.Medium)
                Text("${state.humTh}%", color = MaterialTheme.colorScheme.primary)
            }
            Text("(pause above)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = state.humTh.toFloat(),
                onValueChange = { onHumChange(it.toInt()) },
                valueRange = 40f..95f,
                steps = 54
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LED Brightness", fontWeight = FontWeight.Medium)
                Text("${state.br}", color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = state.br.toFloat(),
                onValueChange = { onBrightnessChange(it.toInt()) },
                valueRange = 10f..220f,
                steps = 41 // (220-10)/5 - 1
            )
        }
    }
}
