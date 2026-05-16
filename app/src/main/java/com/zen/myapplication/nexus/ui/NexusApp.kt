package com.zen.myapplication.nexus.ui

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.lifecycle.viewmodel.compose.viewModel

import com.zen.myapplication.nexus.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusApp(runtimeController: RuntimeController) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safManager = remember { SafManager(context) }
    val gameLibraryManager = remember { GameLibraryManager(context) }
    val games by gameLibraryManager.games.collectAsState(initial = emptyList())

    // No longer using local viewModel() to ensure we use the Activity-scoped one
    // Passed in via parameter

    // Launcher for selecting a game directory via SAF
    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            safManager.takePersistableUriPermission(it)
            
            // Detect the engine and start indexing in background
            scope.launch {
                val engineType = safManager.detectEngine(it)
                gameLibraryManager.addGame(it, engineType)
                // The addGame call already triggers indexing, but we can navigate immediately
                navController.navigate(playerRoute(it, engineType))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nexus Runtime") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dirPickerLauncher.launch(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Import Game")
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "library",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("library") {
                GameLibraryScreen(
                    games = games,
                    onGameClick = { game ->
                        val gameUri = Uri.parse(game.uri)
                        if (safManager.isUriPermissionValid(gameUri)) {
                            navController.navigate(playerRoute(gameUri, game.engine))
                        } else {
                            scope.launch {
                                // Provide feedback to user that the folder is missing
                                // In a real app we'd use a Snackbar or Dialog
                                android.util.Log.e("NexusApp", "Game folder is no longer accessible: $gameUri")
                            }
                        }
                    },
                    onRemoveGame = { game ->
                        scope.launch { gameLibraryManager.removeGame(game.uri) }
                    }
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
                            runtimeController = runtimeController
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
            composable("settings") {
                SettingsScreen()
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
                textAlign = TextAlign.Center
            )
            Text(
                text = if (engineType.requiresNativeRuntime) {
                    "This needs a bundled native runtime before it can run in Nexus."
                } else {
                    "This folder does not look like a supported game export."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
fun GameLibraryScreen(
    games: List<GameEntry>,
    onGameClick: (GameEntry) -> Unit,
    onRemoveGame: (GameEntry) -> Unit
) {
    if (games.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
                Text(
                    text = "No games imported",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Use the add button to choose an exported game folder.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 176.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(games, key = { it.uri }) { game ->
            Card(
                modifier = Modifier
                    .height(156.dp)
                    .clickable { onGameClick(game) },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = game.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = game.engine.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        EngineBadge(engine = game.engine)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onGameClick(game) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        }
                        IconButton(onClick = { onRemoveGame(game) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineBadge(engine: SafManager.GameEngine) {
    val color = when {
        engine.canUseHtml5Runtime -> MaterialTheme.colorScheme.primary
        engine.requiresNativeRuntime -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Text(
            text = when {
                engine.canUseHtml5Runtime -> "Ready"
                engine.requiresNativeRuntime -> "Native pending"
                else -> "Unsupported"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
