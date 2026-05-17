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
 * NexusSettingsScreen: Simplified quick-settings overlay for in-game use.
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
    val tabs = listOf("Engine", "Controls")

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.6f).padding(horizontal = 16.dp)) {
            Text(
                text = "Quick Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp)) {
                when (selectedTab) {
                    0 -> QuickEngineTab(settingsManager)
                    1 -> QuickControlsTab(settingsManager)
                }
            }
        }
    }
}

@Composable
private fun QuickEngineTab(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val hardwareAccel by settingsManager.forceHardwareAccel.collectAsState(initial = true)

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SettingsToggle(
            label = "Hardware Acceleration",
            description = "Toggle GPU compositing.",
            checked = hardwareAccel,
            onCheckedChange = { scope.launch { settingsManager.setForceHardwareAccel(it) } }
        )
    }
}

@Composable
private fun QuickControlsTab(settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val opacity by settingsManager.controllerOpacity.collectAsState(initial = 0.6f)
    val size by settingsManager.controllerSize.collectAsState(initial = 1.0f)

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SettingsSlider(
            label = "Overlay Opacity",
            value = opacity,
            valueText = "${(opacity * 100).roundToInt()}%",
            valueRange = 0.1f..1.0f,
            onValueChange = { scope.launch { settingsManager.setControllerOpacity(it) } }
        )
        SettingsSlider(
            label = "Button Scale",
            value = size,
            valueText = "${(size * 100).roundToInt()}%",
            valueRange = 0.5f..1.5f,
            onValueChange = { scope.launch { settingsManager.setControllerSize(it) } }
        )
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
