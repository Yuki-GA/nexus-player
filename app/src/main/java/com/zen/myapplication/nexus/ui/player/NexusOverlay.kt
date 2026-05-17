package com.zen.myapplication.nexus.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zen.myapplication.nexus.core.input.NexusInput
import com.zen.myapplication.nexus.runtime.RuntimeController
import com.zen.myapplication.nexus.runtime.RuntimeState
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * NexusOverlay: Professional PC Keyboard Emulation for RPG Maker.
 * Optimized for touchscreens. Surivives rotation and recomposition.
 */
@Composable
fun NexusOverlay(
    runtimeController: RuntimeController,
    opacity: Float = 0.6f,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    val state by runtimeController.state.collectAsState()
    var isVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-hide utility
    LaunchedEffect(lastInteraction) {
        delay(3000)
        if (System.currentTimeMillis() - lastInteraction >= 3000) {
            isVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, _ -> 
                    isVisible = true
                    lastInteraction = System.currentTimeMillis()
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        isVisible = true
                        lastInteraction = System.currentTimeMillis()
                    }
                }
            }
    ) {
        AnimatedVisibility(
            visible = isVisible && state is RuntimeState.Running,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // --- TOP UTILITY BAR ---
                QuickActionsBar(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    onSettings = onOpenSettings,
                    onExit = onExit
                )

                // --- LEFT: DIGITAL D-PAD (PC Arrow Keys) ---
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 48.dp)) {
                    DigitalDPad(
                        opacity = opacity,
                        onKeyAction = { key, pressed -> runtimeController.input?.dispatchKey(key, pressed) }
                    )
                }

                // --- RIGHT: KEYBOARD CLUSTER (Z, X, SHIFT) ---
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 48.dp)) {
                    ActionCluster(
                        opacity = opacity,
                        onKeyAction = { key, pressed -> runtimeController.input?.dispatchKey(key, pressed) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalDPad(
    opacity: Float,
    onKeyAction: (Int, Boolean) -> Unit
) {
    val size = 180.dp
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    
    // State to track current directions to avoid spam
    val pressedDirections = remember { mutableSetOf<Int>() }

    Box(
        modifier = Modifier
            .size(size)
            .alpha(opacity)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.pressed }
                        
                        val newKeys = mutableSetOf<Int>()
                        if (pointer != null) {
                            val pos = pointer.position
                            val cx = sizePx / 2
                            val cy = sizePx / 2
                            val dx = pos.x - cx
                            val dy = pos.y - cy
                            
                            val dist = sqrt(dx*dx + dy*dy)
                            if (dist > 20) { // Deadzone
                                val angle = atan2(dy, dx) * 180 / PI
                                
                                // 8-Direction Digital Mapping
                                if (angle in -112.5..-67.5) { newKeys.add(NexusInput.KEY_UP) }
                                else if (angle in 67.5..112.5) { newKeys.add(NexusInput.KEY_DOWN) }
                                else if (angle in -22.5..22.5) { newKeys.add(NexusInput.KEY_RIGHT) }
                                else if (angle in 157.5..180.0 || angle in -180.0..-157.5) { newKeys.add(NexusInput.KEY_LEFT) }
                                // Diagonals
                                else if (angle in -67.5..-22.5) { newKeys.add(NexusInput.KEY_UP); newKeys.add(NexusInput.KEY_RIGHT) }
                                else if (angle in -157.5..-112.5) { newKeys.add(NexusInput.KEY_UP); newKeys.add(NexusInput.KEY_LEFT) }
                                else if (angle in 22.5..67.5) { newKeys.add(NexusInput.KEY_DOWN); newKeys.add(NexusInput.KEY_RIGHT) }
                                else if (angle in 112.5..157.5) { newKeys.add(NexusInput.KEY_DOWN); newKeys.add(NexusInput.KEY_LEFT) }
                            }
                        }

                        // Diff and dispatch
                        val released = pressedDirections - newKeys
                        val pressed = newKeys - pressedDirections
                        
                        released.forEach { onKeyAction(it, false) }
                        pressed.forEach { onKeyAction(it, true) }
                        
                        pressedDirections.clear()
                        pressedDirections.addAll(newKeys)
                    }
                }
            }
    ) {
        // Visual D-Pad Cross
        Box(Modifier.fillMaxSize()) {
            // Background Circle
            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.DarkGray.copy(alpha = 0.4f)))
            
            // Directional Indicators
            DPadArrow(Alignment.TopCenter, Icons.Default.ArrowDropUp)
            DPadArrow(Alignment.BottomCenter, Icons.Default.ArrowDropDown)
            DPadArrow(Alignment.CenterStart, Icons.Default.ArrowLeft)
            DPadArrow(Alignment.CenterEnd, Icons.Default.ArrowRight)
        }
    }
}

@Composable
private fun BoxScope.DPadArrow(align: Alignment, icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.8f),
        modifier = Modifier.align(align).size(48.dp)
    )
}

@Composable
private fun ActionCluster(
    opacity: Float,
    onKeyAction: (Int, Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        // PC Action: SHIFT (Dash)
        KeyboardButton("SHIFT", NexusInput.KEY_SHIFT, opacity, onKeyAction, size = 64.dp)
        
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // PC Action: X (Cancel/Menu)
            KeyboardButton("X", NexusInput.KEY_X, opacity, onKeyAction, isCancel = true)
            // PC Action: Z (Confirm/OK)
            KeyboardButton("Z", NexusInput.KEY_Z, opacity, onKeyAction, isPrimary = true)
        }
    }
}

@Composable
private fun KeyboardButton(
    label: String,
    keyCode: Int,
    opacity: Float,
    onAction: (Int, Boolean) -> Unit,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    isPrimary: Boolean = false,
    isCancel: Boolean = false
) {
    val color = when {
        isPrimary -> MaterialTheme.colorScheme.primary
        isCancel -> MaterialTheme.colorScheme.error
        else -> Color.DarkGray
    }

    Surface(
        modifier = Modifier
            .size(size)
            .alpha(opacity)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val isPressed = event.changes.any { it.pressed }
                        onAction(keyCode, isPressed)
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.7f),
        contentColor = Color.White,
        tonalElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun QuickActionsBar(
    modifier: Modifier = Modifier,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onSettings) { Icon(Icons.Default.Tune, "Settings", tint = Color.White) }
            VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(alpha = 0.3f))
            IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Exit", tint = Color.Red) }
        }
    }
}
