package com.zen.myapplication.nexus.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zen.myapplication.nexus.core.settings.SettingsManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val opacity by settingsManager.controllerOpacity.collectAsState(initial = 0.6f)
    val controllerSize by settingsManager.controllerSize.collectAsState(initial = 1.0f)
    val showFps by settingsManager.showFps.collectAsState(initial = false)
    val translationEnabled by settingsManager.translationEnabled.collectAsState(initial = true)
    val hardwareAccel by settingsManager.forceHardwareAccel.collectAsState(initial = true)
    val fpsLimit by settingsManager.fpsLimit.collectAsState(initial = 60)
    val targetLang by settingsManager.targetLanguage.collectAsState(initial = "English")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Engine Settings", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Tune runtime behavior for imported games.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(title = "Virtual Controller") {
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
        }

        SettingsSection(title = "Compatibility & Performance") {
            SettingsToggle(
                label = "Force Hardware Acceleration",
                checked = hardwareAccel,
                onCheckedChange = { scope.launch { settingsManager.setForceHardwareAccel(it) } }
            )
            SettingsToggle(
                label = "Show FPS Overlay",
                checked = showFps,
                onCheckedChange = { scope.launch { settingsManager.setShowFps(it) } }
            )
            SettingsSlider(
                label = "RPG Maker FPS Limit",
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
            Text(
                "Use 60 FPS for most MV/MZ games. Drop to 30 FPS on older phones or raise to 120 only for light games.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsSection(title = "AI Translation Hub") {
            SettingsToggle(
                label = "Enable Real-Time Translation",
                checked = translationEnabled,
                onCheckedChange = { scope.launch { settingsManager.setTranslationEnabled(it) } }
            )

            OutlinedTextField(
                value = targetLang,
                onValueChange = { scope.launch { settingsManager.setTargetLanguage(it) } },
                label = { Text("Target Language") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        content()
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Slider(
            value = value,
            valueRange = valueRange,
            steps = steps,
            onValueChange = onValueChange
        )
    }
}
