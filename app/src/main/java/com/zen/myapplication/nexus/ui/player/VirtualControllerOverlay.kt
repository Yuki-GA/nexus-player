package com.zen.myapplication.nexus.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.zen.myapplication.nexus.core.input.ActionKey
import com.zen.myapplication.nexus.core.input.DPadDirection

/**
 * A customizable virtual controller overlay for the game runtime.
 */
@Composable
fun VirtualControllerOverlay(
    opacity: Float = 0.6f,
    sizeScale: Float = 1.0f,
    onDPadInput: (DPadDirection, Boolean) -> Unit,
    onActionInput: (ActionKey, Boolean) -> Unit
) {
    val buttonSize = 56.dp * sizeScale.coerceIn(0.75f, 1.35f)
    val edgePadding = 24.dp * sizeScale.coerceIn(0.85f, 1.2f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(edgePadding)
                .alpha(opacity.coerceIn(0.2f, 1.0f))
        ) {
            DPad(buttonSize = buttonSize, onDPadInput = onDPadInput)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(edgePadding)
                .alpha(opacity.coerceIn(0.2f, 1.0f))
        ) {
            ActionButtons(buttonSize = buttonSize, onActionInput = onActionInput)
        }
    }
}

@Composable
fun DPad(
    buttonSize: Dp,
    onDPadInput: (DPadDirection, Boolean) -> Unit
) {
    val gap = buttonSize
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ControllerButton(label = "UP", size = buttonSize) { onDPadInput(DPadDirection.UP, it) }
        Row {
            ControllerButton(label = "L", size = buttonSize) { onDPadInput(DPadDirection.LEFT, it) }
            Spacer(modifier = Modifier.width(gap))
            ControllerButton(label = "R", size = buttonSize) { onDPadInput(DPadDirection.RIGHT, it) }
        }
        ControllerButton(label = "DN", size = buttonSize) { onDPadInput(DPadDirection.DOWN, it) }
    }
}

@Composable
fun ActionButtons(
    buttonSize: Dp,
    onActionInput: (ActionKey, Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControllerButton(label = "SH", size = buttonSize) { onActionInput(ActionKey.SHIFT, it) }
            ControllerButton(label = "ESC", size = buttonSize) { onActionInput(ActionKey.ESC, it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControllerButton(label = "Z", size = buttonSize) { onActionInput(ActionKey.Z, it) }
            ControllerButton(label = "X", size = buttonSize) { onActionInput(ActionKey.X, it) }
            ControllerButton(label = "OK", size = buttonSize) { onActionInput(ActionKey.ENTER, it) }
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
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
