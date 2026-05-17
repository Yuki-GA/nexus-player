package com.zen.myapplication.nexus.runtime

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.util.Log

import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.zen.myapplication.BuildConfig
import com.zen.myapplication.nexus.ui.player.NexusOverlay
import com.zen.myapplication.nexus.ui.settings.NexusSettingsScreen
import com.zen.myapplication.nexus.core.storage.SafManager
import com.zen.myapplication.nexus.core.storage.requiresNativeRuntime
import com.zen.myapplication.nexus.core.settings.SettingsManager

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import android.app.Activity
import android.content.pm.ActivityInfo

/**
 * OverlayHost: The root UI component for the Nexus Player.
 * Strictly separates the Runtime View from the Compose overlay tree.
 */
@Composable
fun OverlayHost(
    rootUri: Uri,
    engineType: SafManager.GameEngine,
    runtimeController: RuntimeController,
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {}
) {
    val state by runtimeController.state.collectAsState()
    var retryKey by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val overlayOpacity by settingsManager.controllerOpacity.collectAsState(initial = 0.6f)
    
    val safeInsets = WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()

    // Dynamic Orientation Handler
    // Only changes orientation when state moves to Running
    LaunchedEffect(state) {
        val activity = context as? Activity
        if (state is RuntimeState.Running) {
            Log.e("NEXUS_UI", "FORCING LANDSCAPE for Gameplay")
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            
            // Immersive Fullscreen Mode for Gameplay
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(activity!!.window, activity.window.decorView)
            windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Lifecycle Cleanup & Orientation Reset
    // Removed stopGame() from onDispose to survive orientation changes
    DisposableEffect(rootUri) {
        onDispose {
            Log.e("NEXUS", "Detached without destroy")
            Log.e("NEXUS_UI", "Cleaning up OverlayHost (survives rotation if Activity persists)")
            // Restore portrait and system bars when leaving the game screen
            val activity = context as? Activity
            activity?.let {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }


    Box(modifier = modifier.fillMaxSize().background(Color.Black)) { 
        val currentState = state
        
        // --- Runtime Layer ---
        key(retryKey) {
            when {
                engineType.requiresNativeRuntime -> {
                    if (currentState is RuntimeState.Running && currentState.isNative) {
                        NativeSurfaceLayer(runtimeController)
                    }
                }
                else -> {
                    Html5RuntimeLayer(rootUri, runtimeController)
                }
            }
        }
        
        // --- Overlay Layer ---
        when (val s = currentState) {
            is RuntimeState.Booting -> {
                BootSequenceOverlay(s)
            }
            is RuntimeState.Error -> {
                RuntimeErrorOverlay(
                    error = s,
                    onRetry = { 
                        runtimeController.stopGame()
                        retryKey++ 
                    },
                    onExit = { 
                        runtimeController.stopGame() 
                        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        onExit()
                    }
                )
            }
            is RuntimeState.Running -> {
                Log.e("NEXUS_INPUT", "Overlay active")
                NexusOverlay(
                    runtimeController = runtimeController,
                    opacity = overlayOpacity,
                    onOpenSettings = { showSettings = true },
                    onExit = {
                        runtimeController.stopGame()
                        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        onExit()
                    }
                )
            }
            else -> {
                if (engineType.requiresNativeRuntime) {
                    LaunchedEffect(retryKey) {
                        runtimeController.startNativeGame(rootUri, engineType)
                    }
                }
            }
        }

        if (showSettings) {
            NexusSettingsScreen(onClose = { showSettings = false })
        }

        if (currentState is RuntimeState.Running && BuildConfig.DEBUG) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(safeInsets)) {
                RuntimeDiagnosticsOverlay()
            }
        }
    }
}


@Composable
private fun Html5RuntimeLayer(rootUri: Uri, runtimeController: RuntimeController) {
    AndroidView(
        factory = { context ->
            val webView = runtimeController.getOrCreateWebView(context)
            if (webView.parent != null) {
                Log.e("NEXUS", "Existing WebView attached")
                (webView.parent as ViewGroup).removeView(webView)
            }
            webView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            try {
                runtimeController.startHtml5Game(context, rootUri)
            } catch (t: Throwable) {
                Log.e("NEXUS_FATAL", "Exception in startHtml5Game", t)
            }
            webView
        },
        modifier = Modifier.fillMaxSize(),
        update = { },
        onRelease = { webView ->
            // DO NOT destroy here if we want to survive rotation without recreation
        }
    )
}

@Composable
private fun NativeSurfaceLayer(runtimeController: RuntimeController) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        runtimeController.onSurfaceCreated(holder.surface)
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        runtimeController.onSurfaceChanged(width, height)
                    }
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        runtimeController.onSurfaceDestroyed()
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { }
    )
}

@Composable
private fun BootSequenceOverlay(state: RuntimeState.Booting) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "NEXUS",
                style = MaterialTheme.typography.displayMedium,
                color = Color(0xFF00E5FF), 
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(
                    color = Color(0xFF00E5FF),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = when (state) {
                        RuntimeState.Booting.Initializing -> "INITIALIZING RUNTIME..."
                        RuntimeState.Booting.MountingVFS -> "MOUNTING VFS..."
                        RuntimeState.Booting.ResolvingEngine -> "RESOLVING ENGINE..."
                        RuntimeState.Booting.Finalizing -> "BOOTING ENGINE..."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun RuntimeErrorOverlay(
    error: RuntimeState.Error,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "RUNTIME CRASH",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (error.canRetry) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("RESTART")
                        }
                    }
                    OutlinedButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("LIBRARY")
                    }
                }
            }
        }
    }
}
