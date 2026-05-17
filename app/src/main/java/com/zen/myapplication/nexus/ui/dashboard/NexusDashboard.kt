package com.zen.myapplication.nexus.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zen.myapplication.nexus.core.storage.GameEntry
import com.zen.myapplication.nexus.core.storage.GameLibraryManager
import com.zen.myapplication.nexus.core.storage.displayName
import com.zen.myapplication.nexus.runtime.RuntimeController
import com.zen.myapplication.nexus.runtime.RuntimeState
import com.zen.myapplication.nexus.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

// --- PREMIUM DESIGN TOKENS ---
val NeonBlue = Color(0xFF00E5FF)
val NeonPurple = Color(0xFFB300FF)
val GlassWhite = Color(0x15FFFFFF)
val DarkMatte = Color(0xFF050608)
val PanelBackground = Color(0x990D0E15) // Translucent for ambient background

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NexusDashboard(
    runtimeController: RuntimeController,
    windowSizeClass: WindowSizeClass,
    onNavigateToGame: (GameEntry) -> Unit,
    importGameAction: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val widthClass = windowSizeClass.widthSizeClass

    var currentPage by remember { mutableStateOf(DashboardPage.Home) }
    val runtimeState by runtimeController.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(DarkMatte)) {
        // Dynamic Ambient Background
        if (runtimeState !is RuntimeState.Running) {
            AmbientBackground()
        }

        if (isLandscape) {
            LandscapeLayout(
                widthClass = widthClass,
                currentPage = currentPage,
                onPageSelected = { currentPage = it },
                runtimeState = runtimeState,
                runtimeController = runtimeController,
                onNavigateToGame = onNavigateToGame,
                importGameAction = importGameAction
            )
        } else {
            PortraitLayout(
                currentPage = currentPage,
                onPageSelected = { currentPage = it },
                runtimeState = runtimeState,
                runtimeController = runtimeController,
                onNavigateToGame = onNavigateToGame,
                importGameAction = importGameAction
            )
        }
    }
}

@Composable
private fun AmbientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset1"
    )
    
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1000f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = (-200 + offset1 * 0.1f).dp, y = (-200 + offset1 * 0.05f).dp)
                .size(600.dp)
                .background(Brush.radialGradient(listOf(NeonBlue.copy(alpha = 0.15f), Color.Transparent)), shape = CircleShape)
                .alpha(0.8f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (200 - offset2 * 0.1f).dp, y = (200 - offset2 * 0.05f).dp)
                .size(700.dp)
                .background(Brush.radialGradient(listOf(NeonPurple.copy(alpha = 0.1f), Color.Transparent)), shape = CircleShape)
                .alpha(0.8f)
        )
        // Subtle grid overlay could go here
    }
}

@Composable
private fun LandscapeLayout(
    widthClass: WindowWidthSizeClass,
    currentPage: DashboardPage,
    onPageSelected: (DashboardPage) -> Unit,
    runtimeState: RuntimeState,
    runtimeController: RuntimeController,
    onNavigateToGame: (GameEntry) -> Unit,
    importGameAction: () -> Unit
) {
    val showTelemetry = widthClass == WindowWidthSizeClass.Expanded
    var isSidebarExpanded by remember { mutableStateOf(false) }
    
    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarExpanded) 180.dp else 72.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "SidebarWidth"
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // 1. Sidebar
        Column(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.2f))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                Icons.Default.Star,
                null,
                tint = NeonBlue,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isSidebarExpanded = !isSidebarExpanded }
            )
            Spacer(Modifier.height(32.dp))
            
            DashboardPage.values().forEach { page ->
                SidebarItem(
                    page = page,
                    isSelected = currentPage == page,
                    isExpanded = isSidebarExpanded,
                    onClick = { onPageSelected(page) }
                )
            }
        }

        // 2. Main Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(PanelBackground)
                .border(1.dp, GlassWhite, RoundedCornerShape(24.dp))
        ) {
            DashboardContent(
                page = currentPage,
                runtimeState = runtimeState,
                runtimeController = runtimeController,
                onNavigateToGame = onNavigateToGame,
                importGameAction = importGameAction
            )
        }

        // 3. Telemetry (Tablet/Desktop Only)
        if (showTelemetry) {
            DashboardTelemetryPanel(
                runtimeController = runtimeController,
                modifier = Modifier.width(280.dp)
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    currentPage: DashboardPage,
    onPageSelected: (DashboardPage) -> Unit,
    runtimeState: RuntimeState,
    runtimeController: RuntimeController,
    onNavigateToGame: (GameEntry) -> Unit,
    importGameAction: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent, // Let ambient background show through
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xEE090A0F),
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
            ) {
                DashboardPage.values().forEach { page ->
                    val isSelected = currentPage == page
                    val iconColor by animateColorAsState(if (isSelected) NeonBlue else Color.Gray)
                    
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onPageSelected(page) },
                        icon = { Icon(page.icon, null, modifier = Modifier.size(22.dp)) },
                        label = null, // Hide labels for cleaner look
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonBlue,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = NeonBlue.copy(alpha = 0.15f)
                        ),
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NEXUS PLAYER", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White, letterSpacing = 2.sp)
                    Text("System Runtime Core", fontSize = 10.sp, color = Color.Gray, letterSpacing = 1.sp)
                }
                RuntimeStatusBadge(runtimeState)
            }

            Box(modifier = Modifier.weight(1f)) {
                DashboardContent(
                    page = currentPage,
                    runtimeState = runtimeState,
                    runtimeController = runtimeController,
                    onNavigateToGame = onNavigateToGame,
                    importGameAction = importGameAction
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    page: DashboardPage,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val fgColor by animateColorAsState(if (isSelected) NeonBlue else Color.Gray)
    val bgColor by animateColorAsState(if (isSelected) NeonBlue.copy(alpha = 0.12f) else Color.Transparent)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Custom ripple could be added, but keeping it clean
                onClick = onClick
            )
            .padding(horizontal = if (isExpanded) 16.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
    ) {
        Icon(page.icon, null, tint = fgColor, modifier = Modifier.size(20.dp))
        if (isExpanded) {
            Spacer(Modifier.width(16.dp))
            Text(page.title, color = fgColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun DashboardContent(
    page: DashboardPage,
    runtimeState: RuntimeState,
    runtimeController: RuntimeController,
    onNavigateToGame: (GameEntry) -> Unit,
    importGameAction: () -> Unit
) {
    AnimatedContent(
        targetState = page,
        transitionSpec = { 
            (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 }) togetherWith 
            (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 8 }) 
        },
        label = "DashboardContent"
    ) { targetPage ->
        when (targetPage) {
            DashboardPage.Home -> HomePanel(runtimeState, onNavigateToGame)
            DashboardPage.Library -> LibraryPanel(onNavigateToGame, importGameAction)
            DashboardPage.Runtime -> RuntimePanel(runtimeController)
            DashboardPage.Settings -> SettingsScreen()
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(targetPage.title, color = Color.Gray) }
        }
    }
}

@Composable
private fun DashboardTelemetryPanel(runtimeController: RuntimeController, modifier: Modifier = Modifier) {
    val state by runtimeController.state.collectAsState()
    val isVfsMounted by runtimeController.isVfsMounted.collectAsState()
    val logs by runtimeController.telemetryLogs.collectAsState()
    val fps by runtimeController.fps.collectAsState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("SYSTEM TELEMETRY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeonPurple, letterSpacing = 1.5.sp)
        
        GlassCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TelemetryRow("ENGINE", when(state) {
                    is RuntimeState.Running -> if ((state as RuntimeState.Running).isNative) "NATIVE_SDL2" else "WEBVIEW"
                    is RuntimeState.Booting -> "TRANSITIONING"
                    else -> "STANDBY"
                }, Icons.Default.Settings)
                TelemetryRow("VFS", if (isVfsMounted) "MOUNTED" else "DISMOUNTED", Icons.Default.Storage)
                TelemetryRow("MEM", runtimeController.getMemoryUsage(), Icons.Default.Memory)
                TelemetryRow("FPS", if (state is RuntimeState.Running) "$fps / 60" else "0", Icons.Default.Speed)
            }
        }

        Text("CONSOLE OUTPUT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeonPurple, letterSpacing = 1.5.sp)
        TerminalConsole(logs, Modifier.height(300.dp))
    }
}

@Composable
private fun TerminalConsole(logs: List<String>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF050505))
            .border(1.dp, GlassWhite, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
            items(logs.reversed()) { log ->
                Text("> $log", color = Color(0xFF00FF00).copy(alpha = 0.8f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glowColor: Color? = null,
    content: @Composable () -> Unit
) {
    val shadowAlpha = if (glowColor != null) 0.3f else 0f
    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = if (glowColor != null) 20f else 0f
                ambientShadowColor = glowColor ?: Color.Black
                spotShadowColor = glowColor ?: Color.Black
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0x15FFFFFF), Color(0x05FFFFFF))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x05FFFFFF))), RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) { content() }
}

@Composable
private fun RuntimeStatusBadge(state: RuntimeState) {
    val isRunning = state is RuntimeState.Running
    val color = when(state) {
        is RuntimeState.Running -> NeonBlue
        is RuntimeState.Error -> Color.Red
        is RuntimeState.Booting -> NeonPurple
        else -> Color.Gray
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "badgePulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color.copy(alpha = if (isRunning) alpha else 1f)))
        Spacer(Modifier.width(8.dp))
        Text(
            text = when(state) { is RuntimeState.Running -> "LIVE"; is RuntimeState.Booting -> "BOOT"; is RuntimeState.Error -> "ERR"; else -> "IDLE" },
            color = color, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun HomePanel(state: RuntimeState, onGameClick: (GameEntry) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text("DASHBOARD", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
        
        GlassCard(Modifier.fillMaxWidth(), glowColor = NeonPurple) {
            Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NEXUS PLAYER ALPHA", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("System is optimized and ready.", color = Color.Gray, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(NeonBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Verified, null, tint = NeonBlue, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (state is RuntimeState.Running) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("NOW PLAYING", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.5.sp)
                GlassCard(Modifier.fillMaxWidth(), glowColor = NeonBlue) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(NeonBlue.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Gamepad, null, tint = NeonBlue, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(state.gameId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(if (state.isNative) "NATIVE ENGINE" else "HTML5 ENGINE", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryPanel(onGameClick: (GameEntry) -> Unit, importGameAction: () -> Unit) {
    val context = LocalContext.current
    val gameLibraryManager = remember { GameLibraryManager(context) }
    val games by gameLibraryManager.games.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LIBRARY", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
            Button(
                onClick = importGameAction, 
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black), 
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("IMPORT", fontWeight = FontWeight.Bold)
            }
        }

        if (games.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No games imported.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(games, key = { it.uri }) { game ->
                    GameCard(
                        game = game,
                        onClick = { onGameClick(game) },
                        onRemove = { scope.launch { gameLibraryManager.removeGame(game.uri) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameEntry, onClick: () -> Unit, onRemove: () -> Unit) {
    GlassCard(modifier = Modifier.height(200.dp), onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, null, tint = Color.DarkGray, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(game.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(game.engine.displayName, color = Color.Gray, fontSize = 11.sp)
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RuntimePanel(controller: RuntimeController) {
    val state by controller.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("RUNTIME CONTROL", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black)
        
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("SESSION STATE", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(when(state) {
                    is RuntimeState.Running -> "STABLE ENGINE / RUNNING"
                    is RuntimeState.Booting -> "BOOTING SEQUENCE ACTIVE"
                    is RuntimeState.Error -> "RUNTIME CRITICAL FAULT"
                    else -> "STANDBY / IDLE"
                }, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                LinearProgressIndicator(
                    progress = { if (state is RuntimeState.Running) 1f else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = NeonBlue,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}
