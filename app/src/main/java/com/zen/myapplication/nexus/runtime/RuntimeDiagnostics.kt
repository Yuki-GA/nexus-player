package com.zen.myapplication.nexus.runtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Lightweight debug overlay.
 * Disabled completely in release builds to ensure zero overhead.
 */
@Composable
fun RuntimeDiagnosticsOverlay() {
    var fps by remember { mutableIntStateOf(0) }
    var frames by remember { mutableIntStateOf(0) }
    
    // Memory tracking
    var memoryUsageMb by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val runtime = Runtime.getRuntime()
        while (true) {
            fps = frames
            frames = 0
            memoryUsageMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                frames++
            }
        }
    }

    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        Text(
            text = "FPS: $fps | Mem: ${memoryUsageMb}MB",
            color = Color.Green,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
