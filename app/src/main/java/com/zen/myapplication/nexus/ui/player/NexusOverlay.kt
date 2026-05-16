package com.zen.myapplication.nexus.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zen.myapplication.nexus.runtime.RuntimeController
import com.zen.myapplication.nexus.runtime.RuntimeState
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * NexusOverlay: Immersive gaming controls inspired by Ludens.
 */
@Composable
fun NexusOverlay(
    runtimeController: RuntimeController,
    opacity: Float = 0.6f,
    onOpenSettings: () -> Unit
) {
    val state by runtimeController.state.collectAsState()
    var isVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

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
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                QuickActionsBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    onSettings = onOpenSettings,
                    onScreenshot = { runtimeController.webViewInstance?.let { /* Capture */ } },
                    onToggleFps = { runtimeController.webViewInstance?.post { runtimeController.webViewInstance?.evaluateJavascript("if(window.__nexus.engine) window.__nexus.engine.toggleFps();", null) } }
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 32.dp, bottom = 32.dp)
                ) {
                    VirtualJoystick(
                        opacity = opacity,
                        onDirectionChange = { keyCode, isPressed ->
                            if (keyCode != null) {
                                val type = if (isPressed) "keydown" else "keyup"
                                runtimeController.webViewInstance?.post {
                                    runtimeController.webViewInstance?.evaluateJavascript(
                                        "(function(){ window.dispatchEvent(new KeyboardEvent('$type', {keyCode: $keyCode, which: $keyCode})); })();",
                                        null
                                    )
                                }
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 32.dp, bottom = 32.dp)
                ) {
                    ActionButtons(
                        opacity = opacity,
                        onButtonAction = { keyCode, isPressed ->
                            val type = if (isPressed) "keydown" else "keyup"
                            runtimeController.webViewInstance?.post {
                                runtimeController.webViewInstance?.evaluateJavascript(
                                    "(function(){ window.dispatchEvent(new KeyboardEvent('$type', {keyCode: $keyCode, which: $keyCode})); })();",
                                    null
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsBar(
    modifier: Modifier = Modifier,
    onSettings: () -> Unit,
    onScreenshot: () -> Unit,
    onToggleFps: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFps) { Icon(Icons.Default.Speed, "FPS", tint = Color.White) }
            IconButton(onClick = onScreenshot) { Icon(Icons.Default.Screenshot, "Capture", tint = Color.White) }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
        }
    }
}

@Composable
fun VirtualJoystick(
    opacity: Float,
    onDirectionChange: (Int?, Boolean) -> Unit
) {
    val size = 160.dp
    val thumbSize = 60.dp
    val density = LocalDensity.current
    val radiusPx = with(density) { (size / 2).toPx() }
    
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var activeKeyCode by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .size(size)
            .alpha(opacity)
            .clip(CircleShape)
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (activeKeyCode != null) onDirectionChange(activeKeyCode, false)
                        activeKeyCode = null
                        thumbOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val distance = sqrt(newOffset.x.pow(2) + newOffset.y.pow(2))
                        
                        if (distance <= radiusPx) {
                            thumbOffset = newOffset
                        } else {
                            val ratio = radiusPx / distance
                            thumbOffset = Offset(newOffset.x * ratio, newOffset.y * ratio)
                        }

                        val nx = thumbOffset.x / radiusPx
                        val ny = thumbOffset.y / radiusPx
                        
                        val newDir = when {
                            abs(nx) < 0.3 && abs(ny) < 0.3 -> null
                            abs(nx) > abs(ny) -> if (nx > 0) 39 else 37
                            else -> if (ny > 0) 40 else 38
                        }

                        if (newDir != activeKeyCode) {
                            if (activeKeyCode != null) onDirectionChange(activeKeyCode, false)
                            if (newDir != null) onDirectionChange(newDir, true)
                            activeKeyCode = newDir
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(size / 3).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.roundToInt(), thumbOffset.y.roundToInt()) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        )
    }
}

@Composable
fun ActionButtons(
    opacity: Float,
    onButtonAction: (Int, Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GameButton("Y", 9, opacity, onButtonAction) 
            GameButton("X", 16, opacity, onButtonAction)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GameButton("B", 27, opacity, onButtonAction, isPrimary = true)
            GameButton("A", 13, opacity, onButtonAction, isPrimary = true)
        }
    }
}

@Composable
fun GameButton(
    label: String,
    keyCode: Int,
    opacity: Float,
    onAction: (Int, Boolean) -> Unit,
    isPrimary: Boolean = false
) {
    Surface(
        modifier = Modifier
            .size(64.dp)
            .alpha(opacity)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            onAction(keyCode, true)
                        } else {
                            onAction(keyCode, false)
                        }
                    }
                }
            },
        shape = CircleShape,
        color = if (isPrimary) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) 
                else Color.DarkGray.copy(alpha = 0.6f),
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
    }
}
