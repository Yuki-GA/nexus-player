package com.zen.myapplication.nexus.ui.player

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zen.myapplication.nexus.core.input.NexusInput
import com.zen.myapplication.nexus.core.settings.SettingsManager
import com.zen.myapplication.nexus.runtime.RuntimeController
import com.zen.myapplication.nexus.runtime.RuntimeState
import com.zen.myapplication.nexus.ui.dashboard.NeonBlue
import com.zen.myapplication.nexus.ui.dashboard.GlassWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * NexusOverlay v2.0: High-fidelity Gaming Controller with Editor Support.
 */
@Composable
fun NexusOverlay(
    runtimeController: RuntimeController,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    
    val opacity by settingsManager.controllerOpacity.collectAsState(initial = 0.6f)
    val scale by settingsManager.controllerSize.collectAsState(initial = 1.0f)
    val hapticEnabled by settingsManager.hapticFeedback.collectAsState(initial = true)

    val dpadSavedOffset by settingsManager.dpadOffset.collectAsState(initial = 0f to 0f)
    val actionSavedOffset by settingsManager.actionOffset.collectAsState(initial = 0f to 0f)

    val state by runtimeController.state.collectAsState()
    var isVisible by remember { mutableStateOf(true) }
    var isEditorMode by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var dpadOffset by remember(dpadSavedOffset) { mutableStateOf(Offset(dpadSavedOffset.first, dpadSavedOffset.second)) }
    var actionOffset by remember(actionSavedOffset) { mutableStateOf(Offset(actionSavedOffset.first, actionSavedOffset.second)) }

    fun triggerHaptic() {
        if (hapticEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        }
    }

    LaunchedEffect(lastInteraction, isEditorMode) {
        if (!isEditorMode) {
            delay(5000)
            if (System.currentTimeMillis() - lastInteraction >= 5000) {
                isVisible = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 1.1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                QuickActionsBar(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                    isEditor = isEditorMode,
                    onToggleEditor = { 
                        if (isEditorMode) {
                            scope.launch {
                                settingsManager.setDpadOffset(dpadOffset.x, dpadOffset.y)
                                settingsManager.setActionOffset(actionOffset.x, actionOffset.y)
                            }
                        }
                        isEditorMode = !isEditorMode 
                    },
                    onSettings = onOpenSettings,
                    onExit = onExit
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset { IntOffset(dpadOffset.x.roundToInt(), dpadOffset.y.roundToInt()) }
                        .padding(start = 48.dp, bottom = 48.dp)
                        .then(if (isEditorMode) Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dpadOffset += dragAmount
                            }
                        } else Modifier)
                ) {
                    NexusDPad(
                        opacity = opacity,
                        scale = scale,
                        isEditor = isEditorMode,
                        onKeyAction = { key, pressed -> 
                            if (pressed) triggerHaptic()
                            runtimeController.input?.dispatchKey(key, pressed)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset { IntOffset(actionOffset.x.roundToInt(), actionOffset.y.roundToInt()) }
                        .padding(end = 48.dp, bottom = 48.dp)
                        .then(if (isEditorMode) Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                actionOffset += dragAmount
                            }
                        } else Modifier)
                ) {
                    NexusActionCluster(
                        opacity = opacity,
                        scale = scale,
                        isEditor = isEditorMode,
                        onKeyAction = { key, pressed -> 
                            if (pressed) triggerHaptic()
                            runtimeController.input?.dispatchKey(key, pressed)
                        }
                    )
                }
                
                if (isEditorMode) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).align(Alignment.Center)) {
                        Text(
                            "OVERLAY EDITOR ACTIVE\nDrag controls to reposition",
                            color = NeonBlue,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NexusDPad(
    opacity: Float,
    scale: Float,
    isEditor: Boolean,
    onKeyAction: (Int, Boolean) -> Unit
) {
    val size = (200 * scale).dp
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val pressedDirections = remember { mutableSetOf<Int>() }

    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (isEditor) 1f else opacity)
            .pointerInput(isEditor) {
                if (isEditor) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.pressed }
                        val newKeys = mutableSetOf<Int>()
                        
                        if (pointer != null) {
                            val pos = pointer.position
                            val dx = pos.x - (sizePx / 2)
                            val dy = pos.y - (sizePx / 2)
                            val dist = sqrt(dx*dx + dy*dy)
                            
                            if (dist > 30) {
                                val angle = atan2(dy, dx) * 180 / PI
                                if (angle in -112.5..-67.5) newKeys.add(NexusInput.KEY_UP)
                                else if (angle in 67.5..112.5) newKeys.add(NexusInput.KEY_DOWN)
                                else if (angle in -22.5..22.5) newKeys.add(NexusInput.KEY_RIGHT)
                                else if (angle in 157.5..180.0 || angle in -180.0..-157.5) newKeys.add(NexusInput.KEY_LEFT)
                                else if (angle in -67.5..-22.5) { newKeys.add(NexusInput.KEY_UP); newKeys.add(NexusInput.KEY_RIGHT) }
                                else if (angle in -157.5..-112.5) { newKeys.add(NexusInput.KEY_UP); newKeys.add(NexusInput.KEY_LEFT) }
                                else if (angle in 22.5..67.5) { newKeys.add(NexusInput.KEY_DOWN); newKeys.add(NexusInput.KEY_RIGHT) }
                                else if (angle in 112.5..157.5) { newKeys.add(NexusInput.KEY_DOWN); newKeys.add(NexusInput.KEY_LEFT) }
                            }
                        }
                        
                        (pressedDirections - newKeys).forEach { onKeyAction(it, false) }
                        (newKeys - pressedDirections).forEach { onKeyAction(it, true) }
                        pressedDirections.clear(); pressedDirections.addAll(newKeys)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)))
                .border(2.dp, if (isEditor) NeonBlue else GlassWhite, CircleShape)
        ) {
            val iconSize = (48 * scale).dp
            Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.align(Alignment.TopCenter).size(iconSize))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomCenter).size(iconSize))
            Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.White, modifier = Modifier.align(Alignment.CenterStart).size(iconSize))
            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.align(Alignment.CenterEnd).size(iconSize))
        }
    }
}

@Composable
private fun NexusActionCluster(
    opacity: Float,
    scale: Float,
    isEditor: Boolean,
    onKeyAction: (Int, Boolean) -> Unit
) {
    val spacing = (24 * scale).dp
    Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.Bottom) {
        NexusButton("SHIFT", NexusInput.KEY_SHIFT, opacity, scale, isEditor, NeonBlue, onKeyAction)
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            NexusButton("X", NexusInput.KEY_X, opacity, scale, isEditor, Color.Red, onKeyAction)
            NexusButton("Z", NexusInput.KEY_Z, opacity, scale, isEditor, NeonBlue, onKeyAction)
        }
    }
}

@Composable
private fun NexusButton(
    label: String,
    keyCode: Int,
    opacity: Float,
    scale: Float,
    isEditor: Boolean,
    accent: Color,
    onAction: (Int, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val buttonSize = (76 * scale).dp
    
    val buttonOpacity by animateFloatAsState(if (isPressed) 1f else if (isEditor) 1f else opacity, label = "BtnAlpha")
    val buttonScale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "BtnScale")

    Box(
        modifier = Modifier
            .size(buttonSize)
            .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale)
            .alpha(buttonOpacity)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .border(2.dp, if (isEditor) accent else GlassWhite, CircleShape)
            .pointerInput(isEditor) {
                if (isEditor) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        if (pressed != isPressed) {
                            isPressed = pressed
                            onAction(keyCode, pressed)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = (18 * scale).sp)
        if (isPressed) {
            Box(Modifier.fillMaxSize().border(4.dp, accent.copy(alpha = 0.5f), CircleShape).shadow(10.dp, CircleShape, spotColor = accent))
        }
    }
}

@Composable
private fun QuickActionsBar(
    modifier: Modifier = Modifier,
    isEditor: Boolean,
    onToggleEditor: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.Black.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, GlassWhite)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onToggleEditor) { 
                Icon(if (isEditor) Icons.Default.Check else Icons.Default.Edit, "Editor", tint = if (isEditor) NeonBlue else Color.White) 
            }
            VerticalDivider(modifier = Modifier.height(20.dp), color = GlassWhite)
            IconButton(onClick = onSettings) { Icon(Icons.Default.Tune, "Settings", tint = Color.White) }
            VerticalDivider(modifier = Modifier.height(20.dp), color = GlassWhite)
            IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Exit", tint = Color.Red.copy(alpha = 0.8f)) }
        }
    }
}
