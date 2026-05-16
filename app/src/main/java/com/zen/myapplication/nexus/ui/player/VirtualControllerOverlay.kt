package com.zen.myapplication.nexus.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zen.myapplication.nexus.core.input.NexusInputIntent

/**
 * VirtualControllerOverlay: A passive visual layer for touch controls.
 * Dispatches abstract NexusInputIntents to the authoritative pipeline.
 */
@Composable
fun VirtualControllerOverlay(
    opacity: Float = 0.6f,
    onInput: (NexusInputIntent, Boolean) -> Unit
) {
    val buttonSize = 64.dp
    val padding = 24.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // D-Pad Area
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(padding)
                .alpha(opacity)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ControllerButton("UP", buttonSize) { onInput(NexusInputIntent.DPAD_UP, it) }
                Row {
                    ControllerButton("LEFT", buttonSize) { onInput(NexusInputIntent.DPAD_LEFT, it) }
                    Spacer(modifier = Modifier.width(buttonSize))
                    ControllerButton("RIGHT", buttonSize) { onInput(NexusInputIntent.DPAD_RIGHT, it) }
                }
                ControllerButton("DOWN", buttonSize) { onInput(NexusInputIntent.DPAD_DOWN, it) }
            }
        }

        // Action Buttons Area
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(padding)
                .alpha(opacity)
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControllerButton("SHIFT", buttonSize) { onInput(NexusInputIntent.ACTION_WEST, it) }
                    ControllerButton("ESC", buttonSize) { onInput(NexusInputIntent.ACTION_NORTH, it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControllerButton("Z", buttonSize) { onInput(NexusInputIntent.ACTION_SOUTH, it) }
                    ControllerButton("X", buttonSize) { onInput(NexusInputIntent.ACTION_EAST, it) }
                }
            }
        }
    }
}

@Composable
private fun ControllerButton(
    label: String,
    size: Dp,
    onPressedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .size(size)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        onPressedChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onPressedChange(false)
                        }
                    }
                )
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
