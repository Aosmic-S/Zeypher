package com.example.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.api.ZephyrState
import com.example.R

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ZephyrScreen(
    modifier: Modifier = Modifier,
    viewModel: ZephyrViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ipAddress by viewModel.ipAddress.collectAsStateWithLifecycle()
    val isMicEnabled by viewModel.isMicEnabled.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    val micPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val context = LocalContext.current
    var showIpDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableIntStateOf(0) }
    var fwStatus by remember { mutableStateOf("") }
    
    val pickFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fwStatus = "Uploading firmware..."
            viewModel.updateFirmware(context, uri) { result -> fwStatus = result }
        }
    }

    // Continuous Speech Recognizer
    DisposableEffect(isMicEnabled) {
        if (!isMicEnabled || !micPermissionState.status.isGranted) return@DisposableEffect onDispose {}
        
        var recognizer: SpeechRecognizer? = null
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                
                val listener = object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        try {
                            if (viewModel.isMicEnabled.value) recognizer?.startListening(intent)
                        } catch (e: Exception) {}
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            viewModel.sendVoiceCmd(matches[0])
                        }
                        try {
                            if (viewModel.isMicEnabled.value) recognizer?.startListening(intent)
                        } catch (e: Exception) {}
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
                
                recognizer?.setRecognitionListener(listener)
                recognizer?.startListening(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        onDispose { 
            try {
                recognizer?.destroy() 
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MAX OS", fontWeight = FontWeight.Black, letterSpacing = 2.sp, style = MaterialTheme.typography.titleMedium)
                        Text("Zephyr System", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } 
                },
                actions = {
                    IconButton(onClick = { showIpDialog = true }) {
                        Icon(Icons.Default.SettingsEthernet, contentDescription = "IP Config")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState is ZephyrUiState.Success) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Lightbulb, contentDescription = "LED Engine") },
                        label = { Text("LEDs") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.List, contentDescription = "Guide") },
                        label = { Text("Guide") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { 
                            currentTab = 4
                            viewModel.fetchEvents()
                            viewModel.fetchHistory()
                        },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Data") },
                        label = { Text("Data") }
                    )
                }
            }
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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MAX OS",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 8.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Connecting to device...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is ZephyrUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Connection Lost", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Cannot reach Zephyr at\n${ipAddress.removePrefix("http://").removeSuffix("/")}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                is ZephyrUiState.Success -> {
                    when (currentTab) {
                        0 -> DashboardContent(
                                state = state.state,
                                viewModel = viewModel
                             )
                        1 -> LedEngineContent(
                                state = state.state,
                                viewModel = viewModel
                             )
                        2 -> SettingsContent(
                                state = state.state,
                                isMicEnabled = isMicEnabled,
                                appTheme = appTheme,
                                micPermissionState = micPermissionState,
                                fwStatus = fwStatus,
                                onMicChange = { viewModel.setMicEnabled(it) },
                                onThemeChange = { viewModel.setTheme(it) },
                                onUploadFirmwareClick = { 
                                    try { pickFileLauncher.launch("*/*") } 
                                    catch (e: Exception) { fwStatus = "No file picker available" }
                                },
                                viewModel = viewModel
                             )
                        3 -> GuideContent()
                        4 -> DataContent(
                                historyJson = viewModel.historyJson.collectAsStateWithLifecycle().value,
                                eventsJson = viewModel.eventsJson.collectAsStateWithLifecycle().value,
                                onRefresh = { 
                                    viewModel.fetchHistory()
                                    viewModel.fetchEvents() 
                                }
                             )
                    }
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
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DashboardContent(state: ZephyrState, viewModel: ZephyrViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
        item { LedStrip(leds = state.leds) }
        item {
            AnimatedVisibility(visible = state.lastCmd != "none") {
                Text(
                    text = "Last Voice/Sound Command: ${state.lastCmd}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
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
                    "water", "hum" -> MaterialTheme.colorScheme.errorContainer
                    "sched" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
                ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = color), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        item { ModeControl(currentMode = state.mode, onModeChange = { viewModel.setMode(it) }) }
        item { EnvironmentCard(state = state, onFanToggle = { viewModel.setFan(it) }) }
        item { WaterCard(state = state, onPumpToggle = { viewModel.setPump(it) }) }
    }
}

@Composable
fun LedEngineContent(state: ZephyrState, viewModel: ZephyrViewModel) {
    var selectedAnim by remember { mutableIntStateOf(0) }
    var red by remember { mutableFloatStateOf(0f) }
    var green by remember { mutableFloatStateOf(0f) }
    var blue by remember { mutableFloatStateOf(255f) }
    var ledIdx by remember { mutableIntStateOf(0) }
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
        item {
            ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Current Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    LedStrip(leds = state.leds)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Brightness", fontWeight = FontWeight.Medium)
                    Slider(
                        value = state.br.toFloat(),
                        onValueChange = { viewModel.setBrightness(it.toInt()) },
                        valueRange = 10f..255f
                    )
                }
            }
        }
        item {
            ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Select Effect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val anims = listOf("Smart (Zephyr Sync)" to 0, "Breathe" to 1, "Rainbow" to 2, "Shimmer" to 3, "Fire" to 4, "Static Colors" to 5)
                    anims.forEach { (name, v) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedAnim = v }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedAnim == v, onClick = { selectedAnim = v })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.setEffect(selectedAnim) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Apply Effect")
                    }
                }
            }
        }
        item {
            ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Static LED Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Set individual LEDs to specific colors (switches to Static mode automatically).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Select LED", fontWeight = FontWeight.Medium)
                    val leds = listOf(0 to "Target", 1 to "LED 1", 2 to "LED 2", 3 to "LED 3")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        leds.forEach { (i, n) ->
                            FilterChip(selected = ledIdx == i, onClick = { ledIdx = i }, label = { Text(n) })
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Select Color", fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("R", modifier = Modifier.width(24.dp), color = Color.Red, fontWeight = FontWeight.Bold)
                        Slider(value = red, onValueChange = { red = it }, valueRange = 0f..255f, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("G", modifier = Modifier.width(24.dp), color = Color.Green, fontWeight = FontWeight.Bold)
                        Slider(value = green, onValueChange = { green = it }, valueRange = 0f..255f, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("B", modifier = Modifier.width(24.dp), color = Color.Blue, fontWeight = FontWeight.Bold)
                        Slider(value = blue, onValueChange = { blue = it }, valueRange = 0f..255f, modifier = Modifier.weight(1f))
                    }
                    
                    Button(
                        onClick = { viewModel.setSingleLed(ledIdx, red.toInt(), green.toInt(), blue.toInt()) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Set Color")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsContent(
    state: ZephyrState,
    isMicEnabled: Boolean,
    appTheme: String,
    micPermissionState: com.google.accompanist.permissions.PermissionState,
    fwStatus: String,
    onMicChange: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onUploadFirmwareClick: () -> Unit,
    viewModel: ZephyrViewModel
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
        item {
            ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("App Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Always-On Voice Commands", fontWeight = FontWeight.Medium)
                            Text(if (isMicEnabled) "Listening via device mic" else "Mic off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isMicEnabled,
                            onCheckedChange = {
                                if (it && !micPermissionState.status.isGranted) {
                                    micPermissionState.launchPermissionRequest()
                                } else {
                                    onMicChange(it)
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Theme", fontWeight = FontWeight.Medium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(selected = appTheme == "light", onClick = { onThemeChange("light") }, label = { Text("Light") })
                            FilterChip(selected = appTheme == "dark", onClick = { onThemeChange("dark") }, label = { Text("Dark") })
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).clickable { onUploadFirmwareClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Firmware Update (OTA)", fontWeight = FontWeight.Medium)
                            Text(if (fwStatus.isNotEmpty()) fwStatus else "Select .bin file to upload", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            ThresholdsCard(
                state = state,
                onTempChange = { viewModel.setTempThresh(it) },
                onHumChange = { viewModel.setHumThresh(it) },
                onBrightnessChange = { viewModel.setBrightness(it) },
                onTankDepthChange = { viewModel.setTankH(it) }
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
        val paddedHex = if (hex.length == 4) "#" + hex[1] + hex[1] + hex[2] + hex[2] + hex[3] + hex[3] else hex
        if (paddedHex == "#000000" || paddedHex == "#000") Color.DarkGray else Color(paddedHex.toColorInt())
    } catch (e: Exception) { Color.DarkGray }
    
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
    ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Mode Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onModeChange("auto") },
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (currentMode == "auto") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                ) { Text("Auto") }
                OutlinedButton(
                    onClick = { onModeChange("on") },
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (currentMode == "on") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                ) { Text("On") }
                OutlinedButton(
                    onClick = { onModeChange("off") },
                    modifier = Modifier.weight(1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (currentMode == "off") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                ) { Text("Off") }
            }
        }
    }
}

@Composable
fun EnvironmentCard(state: ZephyrState, onFanToggle: (Boolean) -> Unit) {
    ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Environment Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("${String.format("%.1f", state.temp)}°", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary)
                    Text("Temperature", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("${state.hum.toInt()}%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary)
                    Text("Humidity", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (state.fan) "Fan Status: Running" else "Fan Status: Stopped", fontWeight = FontWeight.Bold)
                    val statusText = if (state.mode == "auto") {
                        if (state.fan) "Auto · Active" else (if (state.pause.isNotEmpty()) "Auto · Interrupted" else "Auto · Idling")
                    } else {
                        if (state.mode == "off") "Manual Override · Off" else "Manual Override · On"
                    }
                    Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.fan, onCheckedChange = { if (state.mode != "auto") onFanToggle(it) }, enabled = state.mode != "auto")
            }
        }
    }
}

@Composable
fun WaterCard(state: ZephyrState, onPumpToggle: (Boolean) -> Unit) {
    ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Hydration Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tank Resource", style = MaterialTheme.typography.bodyMedium)
                Text("${state.water}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (state.water / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = if (state.water < 15) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Proximity Map: ${if (state.dist > 0) String.format("%.1f cm", state.dist) else "--"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Distribution Pump", fontWeight = FontWeight.Bold)
                val statusText = if (state.mode == "auto") "Algorithmic Control" else (if (state.pump) "Pump Operating" else "Pump Idle")
                Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.pump, onCheckedChange = { if (state.mode != "auto") onPumpToggle(it) }, enabled = state.mode != "auto")
        }
    }
}

@Composable
fun ThresholdsCard(
    state: ZephyrState,
    onTempChange: (Float) -> Unit,
    onHumChange: (Int) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onTankDepthChange: (Float) -> Unit
) {
    ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Thresholds", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Base Target Temp (${String.format("%.1f", state.tempTh)}°C)", fontWeight = FontWeight.Medium)
                Text(
                    text = "Effective: ${String.format("%.1f", state.effTh)}°C", 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("Auto mode adjusts threshold by ±2°C based on outdoor weather.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = state.tempTh, onValueChange = onTempChange, valueRange = 20f..40f, steps = 39)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Humidity Limit", fontWeight = FontWeight.Medium)
                Text("${state.humTh}%", color = MaterialTheme.colorScheme.tertiary)
            }
            Text("(pause above)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = state.humTh.toFloat(), onValueChange = { onHumChange(it.toInt()) }, valueRange = 40f..95f, steps = 54)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Empty Tank Depth", fontWeight = FontWeight.Medium)
                Text("${String.format("%.1f cm", state.tankH)}", color = MaterialTheme.colorScheme.secondary)
            }
            Slider(value = state.tankH, onValueChange = onTankDepthChange, valueRange = 5f..100f)
        }
    }
}

@Composable
fun GuideContent() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Voice Commands Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Here is a list of words you can say when voice command is enabled:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val commands = listOf(
                        "turn on cooler" to "Turns on the cooler and auto mode.",
                        "turn off cooler" to "Turns off the cooler.",
                        "set fan speed to high" to "Sets the fan to high speed.",
                        "pump on" to "Turns the water pump on.",
                        "pump off" to "Turns the water pump off.",
                        "enable auto mode" to "Enables auto climate control.",
                        "disable auto mode" to "Disables auto mode (manual control).",
                        "turn on leds" to "Turns on the LED strip.",
                        "turn off leds" to "Turns off the LED strip.",
                        "set led to red" to "Sets the LED color to red.",
                        "set led to blue" to "Sets the LED color to blue."
                    )
                    
                    commands.forEach { (cmd, desc) ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text("\"$cmd\"", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DataContent(historyJson: String, eventsJson: String, onRefresh: () -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("ON/OFF Event Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onRefresh) { Text("Refresh") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Last 20 operations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val eventsArray = remember(eventsJson) {
                        try {
                            org.json.JSONArray(eventsJson)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (eventsArray == null) {
                        Text("Error parsing events.", color = MaterialTheme.colorScheme.error)
                        Text(eventsJson, style = MaterialTheme.typography.bodySmall)
                    } else if (eventsArray.length() == 0) {
                        Text("No events recorded yet.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        for (i in 0 until eventsArray.length()) {
                            val ev = eventsArray.optJSONObject(i)
                            if (ev != null) {
                                val time = ev.optLong("time", ev.optLong("t", 0L))
                                val dev = ev.optString("device", ev.optString("dev", "unknown"))
                                val state = ev.optInt("state", ev.optInt("s", -1))
                                val reason = ev.optString("reason", ev.optString("r", "unknown"))
                                
                                val timeStr = if (time > 0) java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(time * 1000)) else "--:--"
                                val stateStr = if (state == 1) "ON" else if (state == 0) "OFF" else state.toString()
                                
                                Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(dev.uppercase() + " -> " + stateStr, fontWeight = FontWeight.Bold, color = if (state == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                        Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("Triggered by: $reason", style = MaterialTheme.typography.bodySmall)
                                }
                                if (i < eventsArray.length() - 1) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
        
        item {
            ElevatedCard(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Temperature History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Last 96 minutes (2-min intervals).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val histArray = remember(historyJson) {
                        try {
                            org.json.JSONArray(historyJson)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (histArray == null) {
                        Text("Error parsing history.", color = MaterialTheme.colorScheme.error)
                        Text(historyJson, style = MaterialTheme.typography.bodySmall)
                    } else if (histArray.length() == 0) {
                        Text("No history data yet.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        for (i in 0 until histArray.length()) {
                            val item = histArray.optJSONObject(i)
                            if (item != null) {
                                val time = item.optLong("time", item.optLong("t_stamp", 0L))
                                val temp = item.optDouble("temp", item.optDouble("t", 0.0))
                                val hum = item.optDouble("hum", item.optDouble("h", 0.0))
                                
                                val timeStr = if (time > 0) java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(time * 1000)) else "--:--"
                                
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(timeStr, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                    Text(String.format("%.1f°C", temp), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Text("${hum.toInt()}%", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                                }
                                if (i < histArray.length() - 1) HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}


