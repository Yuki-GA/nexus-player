package com.zen.myapplication.nexus.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zen.myapplication.nexus.core.settings.SettingsManager
import com.zen.myapplication.nexus.ui.dashboard.GlassCard
import com.zen.myapplication.nexus.ui.dashboard.NeonBlue
import com.zen.myapplication.nexus.ui.dashboard.NeonPurple
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    Appearance("Appearance", Icons.Default.Palette),
    Layout("Layout", Icons.Default.Dashboard),
    Runtime("Runtime", Icons.Default.Speed),
    Input("Input", Icons.Default.Gamepad),
    Library("Library", Icons.AutoMirrored.Filled.LibraryBooks),
    System("System", Icons.Default.Memory)
}

@Composable
fun SettingsScreen() {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp
    
    val useNavRail = isLandscape || screenWidth > 600.dp
    var selectedCategory by remember { mutableStateOf(SettingsCategory.Appearance) }

    if (useNavRail) {
        LandscapeSettings(selectedCategory) { selectedCategory = it }
    } else {
        PortraitSettings(selectedCategory) { selectedCategory = it }
    }
}

@Composable
private fun LandscapeSettings(
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .padding(end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsCategory.values().forEach { category ->
                CategoryItem(
                    category = category,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }

        GlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                SettingsCategoryContent(selectedCategory)
            }
        }
    }
}

@Composable
private fun PortraitSettings(
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit
) {
    var expandedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsCategory.values().forEach { category ->
            val isExpanded = expandedCategory == category
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCategory = if (isExpanded) null else category }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(category.icon, null, tint = if (isExpanded) NeonBlue else Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = category.title,
                            color = if (isExpanded) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (isExpanded) {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            SettingsCategoryContent(category, isScrollable = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(if (isSelected) NeonBlue.copy(alpha = 0.15f) else Color.Transparent)
    val fgColor by animateColorAsState(if (isSelected) NeonBlue else Color.Gray)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(category.icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = category.title,
            color = fgColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsCategoryContent(category: SettingsCategory, isScrollable: Boolean = true) {
    AnimatedContent(
        targetState = category,
        transitionSpec = { 
            fadeIn(tween(300)) + slideInVertically { 20 } togetherWith fadeOut(tween(200))
        },
        label = "SettingsContent"
    ) { targetCategory ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isScrollable) {
                Text(targetCategory.title.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }

            when (targetCategory) {
                SettingsCategory.Appearance -> AppearanceSettings()
                SettingsCategory.Layout -> LayoutSettings()
                SettingsCategory.Runtime -> RuntimeSettings()
                SettingsCategory.Input -> InputSettings()
                SettingsCategory.Library -> LibrarySettings()
                SettingsCategory.System -> SystemSettings()
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AppearanceSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val amoled by settingsManager.amoledMode.collectAsState(initial = true)
    val glassFx by settingsManager.glassFx.collectAsState(initial = true)
    val cornerRadius by settingsManager.cornerRadius.collectAsState(initial = 20)

    SettingsSection("Visual Environment", onReset = { scope.launch { settingsManager.resetCategory("Appearance") } }) {
        SettingsToggle("AMOLED True Black", "Reduces battery consumption and increases contrast", amoled) {
            scope.launch { settingsManager.setAmoledMode(it) }
        }
        SettingsToggle("Glassmorphism FX", "Enable translucent frosted-glass panels", glassFx) {
            scope.launch { settingsManager.setGlassFx(it) }
        }
        SettingsSlider("UI Corner Radius", "Adjust the roundness of panels", cornerRadius.toFloat(), "${cornerRadius}dp", 0f..32f) {
            scope.launch { settingsManager.setCornerRadius(it.roundToInt()) }
        }
    }
}

@Composable
private fun LayoutSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val compactSidebar by settingsManager.sidebarCompact.collectAsState(initial = false)
    val showTelemetry by settingsManager.showTelemetry.collectAsState(initial = true)

    SettingsSection("User Interface") {
        SettingsToggle("Compact Sidebar", "Force icons-only mode in landscape", compactSidebar) {
            scope.launch { settingsManager.setSidebarCompact(it) }
        }
        SettingsToggle("Telemetry Dashboard", "Show real-time engine stats on the right", showTelemetry) {
            scope.launch { settingsManager.setShowTelemetry(it) }
        }
        SettingsSlider("Panel Spacing", "Adjust gaps between dashboard cards", 16f, "16dp", 8f..32f) {}
    }
}

@Composable
private fun RuntimeSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val hardwareAccel by settingsManager.forceHardwareAccel.collectAsState(initial = true)
    val fpsLimit by settingsManager.fpsLimit.collectAsState(initial = 60)
    val debugMode by settingsManager.debugMode.collectAsState(initial = false)

    SettingsSection("Engine & Stability", onReset = { scope.launch { settingsManager.resetCategory("Runtime") } }) {
        SettingsToggle("Hardware Acceleration", "Enable GPU-accelerated WebView compositor", hardwareAccel) {
            scope.launch { settingsManager.setForceHardwareAccel(it) }
        }
        SettingsToggle("Debug Mode", "Enable raw developer tools and logs", debugMode) {
            scope.launch { settingsManager.setDebugMode(it) }
        }
        SettingsSlider("FPS Cap", "Synchronize engine loop frequency", fpsLimit.toFloat(), "$fpsLimit FPS", 30f..120f, 2) {
            scope.launch { settingsManager.setFpsLimit(it.roundToInt()) }
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
    val haptic by settingsManager.hapticFeedback.collectAsState(initial = true)

    SettingsSection("Virtual Controls", onReset = { scope.launch { settingsManager.resetCategory("Input") } }) {
        SettingsSlider("Touch Overlay Opacity", "Control transparency of the gamepad", opacity, "${(opacity * 100).roundToInt()}%", 0.1f..1.0f) {
            scope.launch { settingsManager.setControllerOpacity(it) }
        }
        SettingsSlider("Button Scale", "Adjust the size of D-pad and Action keys", size, "${(size * 100).roundToInt()}%", 0.5f..1.5f) {
            scope.launch { settingsManager.setControllerSize(it) }
        }
        SettingsToggle("Haptic Feedback", "Vibrate on button press", haptic) {
            scope.launch { settingsManager.setHapticFeedback(it) }
        }
    }
}

@Composable
private fun LibrarySettings() {
    SettingsSection("Game Collection") {
        SettingsToggle("Fetch Metadata", "Automatically download game icons and descriptions", true) {}
        SettingsToggle("Show Unsupported", "Show folders that may not be valid RPG Maker exports", false) {}
    }
}

@Composable
private fun SystemSettings() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    SettingsSection("Advanced Maintenance") {
        Button(
            onClick = { /* Clear Cache Logic */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Purge VFS Cache", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Export Diagnostic Logs", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsSection(title: String, onReset: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (onReset != null) {
                Text(
                    "RESET", 
                    color = Color.Gray, 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.clickable { onReset() }.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsToggle(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(description, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonBlue, 
                checkedTrackColor = NeonBlue.copy(alpha = 0.2f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.White.copy(alpha = 0.05f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    description: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(description, color = Color.Gray, fontSize = 11.sp)
            }
            Text(valueText, color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            valueRange = valueRange,
            steps = steps,
            onValueChange = onValueChange,
            modifier = Modifier.height(24.dp), // Thinner slider track area
            colors = SliderDefaults.colors(
                thumbColor = NeonBlue, 
                activeTrackColor = NeonBlue.copy(alpha = 0.7f),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}
