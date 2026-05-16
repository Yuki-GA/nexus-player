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
 * SettingsScreen: Handheld-optimized settings interface.
 * High-density card-based design with Android 15+ WindowInsets support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val opacity by settingsManager.controllerOpacity.collectAsState(initial = 0.6f)
    val controllerSize by settingsManager.controllerSize.collectAsState(initial = 1.0f)
    val showFps by settingsManager.showFps.collectAsState(initial = false)
    val hardwareAccel by settingsManager.forceHardwareAccel.collectAsState(initial = true)
    val fpsLimit by settingsManager.fpsLimit.collectAsState(initial = 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing) // Android 15+ Notch/Gesture safety
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = "Runtime Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Optimize your handheld experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section: Interface & Overlays
        SettingsCard(title = "Interface & Overlays", icon = Icons.Default.Layers) {
            SettingsSlider(
                label = "Overlay Opacity",
                value = opacity,
                valueText = "${(opacity * 100).roundToInt()}%",
                valueRange = 0.2f..1.0f,
                onValueChange = { scope.launch { settingsManager.setControllerOpacity(it) } }
            )
            SettingsSlider(
                label = "Button Size",
                value = controllerSize,
                valueText = "${(controllerSize * 100).roundToInt()}%",
                valueRange = 0.75f..1.35f,
                onValueChange = { scope.launch { settingsManager.setControllerSize(it) } }
            )
            SettingsToggle(
                label = "Show Performance Overlay",
                checked = showFps,
                onCheckedChange = { scope.launch { settingsManager.setShowFps(it) } }
            )
        }

        // Section: Engine & Performance
        SettingsCard(title = "Engine & Performance", icon = Icons.Default.Speed) {
            SettingsToggle(
                label = "Hardware Acceleration",
                checked = hardwareAccel,
                onCheckedChange = { scope.launch { settingsManager.setForceHardwareAccel(it) } }
            )
            SettingsSlider(
                label = "FPS Target",
                value = fpsLimit.toFloat(),
                valueText = "$fpsLimit FPS",
                valueRange = 30f..120f,
                steps = 2,
                onValueChange = { value ->
                    val snapped = when {
                        value < 45f -> 30
                        value < 90f -> 60
                        else -> 120
                    }
                    scope.launch { settingsManager.setFpsLimit(snapped) }
                }
            )
        }

        // Placeholder for future engine-specific settings (UX Completeness)
        SettingsCard(title = "Engine Compatibility", icon = Icons.Default.Extension) {
            Text(
                "Advanced compatibility shims for MV/MZ plugins and NW.js behavior are managed automatically per-game.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Slider(
            value = value,
            valueRange = valueRange,
            steps = steps,
            onValueChange = onValueChange,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
