package com.zen.myapplication.nexus.ui

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zen.myapplication.nexus.core.storage.GameEntry
import com.zen.myapplication.nexus.core.storage.GameLibraryManager
import com.zen.myapplication.nexus.core.storage.SafManager
import com.zen.myapplication.nexus.core.storage.canUseHtml5Runtime
import com.zen.myapplication.nexus.core.storage.displayName
import com.zen.myapplication.nexus.core.storage.requiresNativeRuntime
import com.zen.myapplication.nexus.runtime.OverlayHost
import com.zen.myapplication.nexus.runtime.RuntimeController
import com.zen.myapplication.nexus.ui.dashboard.NexusDashboard
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp(
    runtimeController: RuntimeController,
    windowSizeClass: WindowSizeClass
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safManager = remember { SafManager(context) }
    val gameLibraryManager = remember { GameLibraryManager(context) }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            safManager.takePersistableUriPermission(it)
            scope.launch {
                val engineType = safManager.detectEngine(it)
                gameLibraryManager.addGame(it, engineType)
                navController.navigate(playerRoute(it, engineType))
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("dashboard") {
            NexusDashboard(
                runtimeController = runtimeController,
                windowSizeClass = windowSizeClass,
                onNavigateToGame = { game: GameEntry ->
                    val gameUri = Uri.parse(game.uri)
                    if (safManager.isUriPermissionValid(gameUri)) {
                        navController.navigate(playerRoute(gameUri, game.engine))
                    } else {
                        android.util.Log.e("NexusApp", "Game folder inaccessible: ${game.uri}")
                    }
                },
                importGameAction = { dirPickerLauncher.launch(null) }
            )
        }
        
        composable("player/{encodedUri}/{engineName}") { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("encodedUri") ?: return@composable
            val engineName = backStackEntry.arguments?.getString("engineName") ?: "UNSUPPORTED"
            
            val decodedUriString = decodeRouteArg(encodedUri)
            val gameUri = Uri.parse(decodedUriString)
            val engineType = try {
                SafManager.GameEngine.valueOf(engineName)
            } catch (e: Exception) {
                SafManager.GameEngine.UNSUPPORTED
            }
            
            when {
                engineType.canUseHtml5Runtime || engineType.requiresNativeRuntime -> {
                    OverlayHost(
                        rootUri = gameUri,
                        engineType = engineType,
                        runtimeController = runtimeController,
                        onExit = {
                            navController.popBackStack("dashboard", inclusive = false)
                        }
                    )
                }
                else -> {
                    RuntimeUnavailableScreen(
                        engineType = engineType,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RuntimeUnavailableScreen(
    engineType: SafManager.GameEngine,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = engineType.displayName,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = androidx.compose.ui.graphics.Color.White
            )
            Text(
                text = if (engineType.requiresNativeRuntime) {
                    "This needs a bundled native runtime before it can run in Nexus."
                } else {
                    "This folder does not look like a supported game export."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.Gray,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

private fun playerRoute(uri: Uri, engine: SafManager.GameEngine): String {
    val encodedUri = encodeRouteArg(uri.toString())
    return "player/$encodedUri/${engine.name}"
}

private fun encodeRouteArg(value: String): String {
    return Base64.encodeToString(
        value.toByteArray(StandardCharsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )
}

private fun decodeRouteArg(value: String): String {
    return String(
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
        StandardCharsets.UTF_8
    )
}
