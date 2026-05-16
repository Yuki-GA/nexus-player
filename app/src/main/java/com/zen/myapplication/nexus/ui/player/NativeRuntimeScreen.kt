package com.zen.myapplication.nexus.ui.player

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zen.myapplication.nexus.core.engine.NativeEngineBridge
import com.zen.myapplication.nexus.core.input.NexusInput
import com.zen.myapplication.nexus.core.storage.SafManager
import com.zen.myapplication.nexus.core.storage.displayName

@Composable
fun NativeRuntimeScreen(
    gameFolderUri: Uri,
    engineType: SafManager.GameEngine
) {
    val bridge = remember { NativeEngineBridge() }
    val status = remember { mutableStateOf("Preparing ${engineType.displayName} runtime...") }

    DisposableEffect(Unit) {
        onDispose {
            bridge.shutdownEngine()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            if (!bridge.isRuntimeAvailable(engineType.name)) {
                                status.value = "${engineType.displayName} runtime is not bundled in this APK yet."
                                return
                            }

                            val initialized = bridge.initEngine(
                                engineType.name,
                                gameFolderUri.toString()
                            )
                            bridge.surfaceCreated(holder.surface)
                            status.value = if (initialized) {
                                "${engineType.displayName} bridge initialized. Engine core is not bundled yet."
                            } else {
                                "${engineType.displayName} engine failed to initialize."
                            }
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                            bridge.surfaceChanged(width, height)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            bridge.surfaceDestroyed()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = status.value,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        VirtualControllerOverlay(
            opacity = 0.55f,
            onDPadInput = { direction, pressed ->
                bridge.sendKeyEvent(NexusInput.getDirectionKeycode(direction), pressed)
            },
            onActionInput = { action, pressed ->
                bridge.sendKeyEvent(NexusInput.getActionKeycode(action), pressed)
            }
        )
    }
}
