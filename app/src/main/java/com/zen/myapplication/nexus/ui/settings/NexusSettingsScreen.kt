package com.zen.myapplication.nexus.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zen.myapplication.nexus.core.settings.SettingsManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * NexusSettingsScreen: Comprehensive multi-tab settings inspired by Ludens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusSettingsScreen(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Engine", "Controls", "Game", "Display")

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.85f)) {
            // Header
            Text(
                text = "Nexus Runtime Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (selectedTab) {
                    0 -> EngineTab(settingsManager)
                    1 -> ControlsTab(settingsManager)
                    2 -> GameTab()
                    3 -> DisplayTab()
                }
            }
        }
    }
}

@Composable
private fun EngineTab(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val hardwareAccel by settingsManager.forceHardwareAccel.collectAsState(initial = true)
    val showFps by settingsManager.showFps.collectAsState(initial = false)
    
    var resolutionScale by remember { mutableFloatStateOf(1.0f) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsToggle(
            label = "Hardware Acceleration",
            description = "Force GPU rasterization (highly recommended).",
            checked = hardwareAccel,
            onCheckedChange = { scope.launch { settingsManager.setForceHardwareAccel(it) } }
        )
        SettingsToggle(
            label = "Show FPS Counter",
            description = "Overlay engine performance metrics.",
            checked = showFps,
            onCheckedChange = { scope.launch { settingsManager.setShowFps(it) } }
        )
        
        Column {
            Text("Resolution Scale", style = MaterialTheme.typography.bodyLarge)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5f, 0.75f, 1.0f, 1.5f).forEach { scale ->
                    FilterChip(
                        selected = resolutionScale == scale,
                        onClick = { resolutionScale = scale },
                        label = { Text("${scale}x") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlsTab(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val opacity by settingsManager.controllerOpacity.collectAsState(initial = 0.6f)
    val size by settingsManager.controllerSize.collectAsState(initial = 1.0f)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSlider(
            label = "Control Opacity",
            value = opacity,
            valueText = "${(opacity * 100).roundToInt()}%",
            valueRange = 0.2f..1.0f,
            onValueChange = { scope.launch { settingsManager.setControllerOpacity(it) } }
        )
        SettingsSlider(
            label = "Control Size",
            value = size,
            valueText = "${(size * 100).roundToInt()}%",
            valueRange = 0.75f..1.35f,
            onValueChange = { scope.launch { settingsManager.setControllerSize(it) } }
        )
        
        OutlinedButton(
            onClick = { /* Drag repositioning */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DragIndicator, null)
            Spacer(Modifier.width(8.dp))
            Text("Edit Control Positions")
        }
    }
}

@Composable
private fun GameTab() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow("Engine", "RPG Maker MZ")
            InfoRow("Entry Point", "index.html")
            InfoRow("Translation", "Active (1,240 strings)")
            InfoRow("Decryption", "Enabled (XOR 16-byte)")
        }
    }
}

@Composable
private fun DisplayTab() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("System", "Dark", "Light").forEach { theme ->
                FilterChip(selected = theme == "Dark", onClick = {}, label = { Text(theme) })
            }
        }
        
        Text("Language", style = MaterialTheme.typography.titleMedium)
        listOf("English", "Bahasa Indonesia", "日本語").forEach { lang ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = lang == "English", onClick = {})
                Spacer(Modifier.width(8.dp))
                Text(lang)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsToggle(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSlider(label: String, value: Float, valueText: String, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(valueText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, valueRange = valueRange, onValueChange = onValueChange)
    }
}
