package com.zen.myapplication.nexus.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zen.myapplication.nexus.core.settings.SettingsManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val NeonBlue = Color(0xFF00E5FF)
val GlassWhite = Color(0x1AFFFFFF)

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    Appearance("Appearance", Icons.Default.Palette),
    Layout("Layout", Icons.Default.Dashboard),
    Runtime("Runtime", Icons.Default.Speed),
    Input("Input", Icons.Default.Gamepad),
    Library("Library", Icons.Default.LibraryBooks),
    System("System", Icons.Default.Memory)
}

@Composable
fun SettingsScreen() {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.Appearance) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Categories Sidebar
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .padding(end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                val bgColor = if (isSelected) NeonBlue.copy(alpha = 0.2f) else Color.Transparent
                val fgColor = if (isSelected) NeonBlue else Color.Gray

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable { selectedCategory = category }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(category.icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(category.title, color = fgColor, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // Settings Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(24.dp)
        ) {
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SettingsCategory"
            ) { category ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(category.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                    when (category) {
                        SettingsCategory.Appearance -> AppearanceSettings()
                        SettingsCategory.Layout -> LayoutSettings()
                        SettingsCategory.Runtime -> RuntimeSettings()
                        SettingsCategory.Input -> InputSettings()
                        SettingsCategory.Library -> LibrarySettings()
                        SettingsCategory.System -> SystemSettings()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettings() {
    SettingsSection("Theme & Styling") {
        SettingsToggle("AMOLED Dark Mode", true) {}
        SettingsToggle("Glassmorphism Effects", true) {}
        SettingsSlider("UI Blur Intensity", 0.5f, "50%", 0f..1f) {}
        SettingsSlider("Corner Radius", 16f, "16dp", 0f..32f) {}
    }
}

@Composable
private fun LayoutSettings() {
    SettingsSection("Dashboard Layout") {
        SettingsToggle("Compact Sidebar Mode", false) {}
        SettingsToggle("Show Telemetry Panel", true) {}
        SettingsSlider("Card Spacing", 16f, "16dp", 8f..32f) {}
    }
}

@Composable
private fun RuntimeSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val hardwareAccel by settingsManager.forceHardwareAccel.collectAsState(initial = true)
    val fpsLimit by settingsManager.fpsLimit.collectAsState(initial = 60)

    SettingsSection("Engine & Performance") {
        SettingsToggle("Hardware Acceleration", hardwareAccel) { scope.launch { settingsManager.setForceHardwareAccel(it) } }
        SettingsToggle("Force WebGL Mode", true) {}
        SettingsToggle("Lifecycle Stabilization Mode", true) {}
        SettingsSlider(
            label = "FPS Limit",
            value = fpsLimit.toFloat(),
            valueText = "$fpsLimit FPS",
            valueRange = 30f..120f,
            steps = 2
        ) { value ->
            val snapped = when { value < 45f -> 30; value < 90f -> 60; else -> 120 }
            scope.launch { settingsManager.setFpsLimit(snapped) }
        }
    }
}

@Composable
private fun InputSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val opacity by settingsManager.controllerOpacity.collectAsState(initial = 0.6f)
    val size by settingsManager.controllerSize.collectAsState(initial = 1.0f)

    SettingsSection("Virtual Controls") {
        SettingsSlider("Overlay Opacity", opacity, "${(opacity * 100).roundToInt()}%", 0.2f..1.0f) { scope.launch { settingsManager.setControllerOpacity(it) } }
        SettingsSlider("Button Scale", size, "${(size * 100).roundToInt()}%", 0.5f..1.5f) { scope.launch { settingsManager.setControllerSize(it) } }
        SettingsToggle("Haptic Feedback", true) {}
    }
}

@Composable
private fun LibrarySettings() {
    SettingsSection("Library Preferences") {
        SettingsToggle("Show Hidden Games", false) {}
        SettingsToggle("Fetch Custom Artwork", true) {}
        SettingsSlider("Grid Items Per Row", 3f, "3", 2f..6f) {}
    }
}

@Composable
private fun SystemSettings() {
    SettingsSection("System & Maintenance") {
        SettingsToggle("Enable Verbose Telemetry", false) {}
        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
            Text("Clear VFS Cache", color = Color.White)
        }
        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = GlassWhite)) {
            Text("Export Diagnostic Logs", color = Color.White)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = NeonBlue, checkedTrackColor = NeonBlue.copy(alpha = 0.3f)))
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text(valueText, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            valueRange = valueRange,
            steps = steps,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue)
        )
    }
}
