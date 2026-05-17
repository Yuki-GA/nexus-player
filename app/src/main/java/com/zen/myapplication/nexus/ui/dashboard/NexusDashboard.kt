package com.zen.myapplication.nexus.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

// --- FUTURISTIC DESIGN TOKENS ---
val NeonBlue = Color(0xFF00E5FF)
val GlassWhite = Color(0x1AFFFFFF)
val DarkMatte = Color(0xFF0A0B10)
val PanelBackground = Color(0xFF161822)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun NexusDashboard(
    runtimeController: RuntimeController,
    windowSizeClass: WindowSizeClass,
    onNavigateToGame: (GameEntry) -> Unit,
    importGameAction: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded || isLandscape

    var currentPage by remember { mutableStateOf(DashboardPage.Home) }
    val runtimeState by runtimeController.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkMatte
    ) {
        if (isExpanded) {
            LandscapeDashboard(
                currentPage = currentPage,
                onPageSelected = { currentPage = it },
                runtimeState = runtimeState,
                runtimeController = runtimeController,
                onNavigateToGame = onNavigateToGame,
                importGameAction = importGameAction
            )
        } else {
            PortraitDashboard(
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
private fun LandscapeDashboard(
    currentPage: DashboardPage,
    onPageSelected: (DashboardPage) -> Unit,
    runtimeState: RuntimeState,
    runtimeController: RuntimeController,
    onNavigateToGame: (GameEntry) -> Unit,
    importGameAction: () -> Unit
) {
    var isSidebarExpanded by remember { mutableStateOf(false) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarExpanded) 200.dp else 80.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "SidebarWidth"
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // 1. Animated & Scrollable Sidebar
        Column(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .background(Color.Transparent)
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Nexus",
                tint = NeonBlue,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 24.dp)
                    .clickable { isSidebarExpanded = !isSidebarExpanded }
            )
            
            DashboardPage.values().forEach { page ->
                val isSelected = currentPage == page
                val bgColor = if (isSelected) NeonBlue.copy(alpha = 0.15f) else Color.Transparent
                val fgColor = if (isSelected) NeonBlue else Color.Gray

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onPageSelected(page) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isSidebarExpanded) Arrangement.Start else Arrangement.Center
                ) {
                    if (isSidebarExpanded) Spacer(modifier = Modifier.width(16.dp))
                    Icon(page.icon, contentDescription = page.title, tint = fgColor, modifier = Modifier.size(24.dp))
                    if (isSidebarExpanded) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(page.title, color = fgColor, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        // 2. Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .background(PanelBackground)
                .border(1.dp, GlassWhite, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
        ) {
            DashboardContent(
                page = currentPage,
                runtimeState = runtimeState,
                runtimeController = runtimeController,
                onNavigateToGame = onNavigateToGame,
                importGameAction = importGameAction
            )
        }

        // 3. Right Telemetry Panel (Scrollable)
        DashboardTelemetryPanel(runtimeState, modifier = Modifier.width(280.dp))
    }
}

@Composable
private fun PortraitDashboard(
    currentPage: DashboardPage,
    onPageSelected: (DashboardPage) -> Unit,
    runtimeState: RuntimeState,
    runtimeController: RuntimeController,
    onNavigateToGame: (GameEntry) -> Unit,
    importGameAction: () -> Unit
) {
    Scaffold(
        containerColor = DarkMatte,
        bottomBar = {
            NavigationBar(
                containerColor = PanelBackground,
                tonalElevation = 8.dp
            ) {
                DashboardPage.values().forEach { page ->
                    NavigationBarItem(
                        selected = currentPage == page,
                        onClick = { onPageSelected(page) },
                        icon = { Icon(page.icon, contentDescription = page.title) },
                        label = { Text(page.title, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonBlue,
                            selectedTextColor = NeonBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = NeonBlue.copy(alpha = 0.1f)
                        )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "NEXUS PLAYER",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
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
            fadeIn() + slideInHorizontally { it / 2 } togetherWith fadeOut() + slideOutHorizontally { -it / 2 }
        },
        label = "PageTransition"
    ) { targetPage ->
        when (targetPage) {
            DashboardPage.Home -> HomePanel(runtimeState, onNavigateToGame)
            DashboardPage.Library -> LibraryPanel(onNavigateToGame, importGameAction)
            DashboardPage.Runtime -> RuntimePanel(runtimeController)
            DashboardPage.Tools -> ToolsPanel()
            DashboardPage.System -> SystemPanel()
            DashboardPage.Settings -> SettingsPanel()
        }
    }
}

@Composable
private fun DashboardTelemetryPanel(runtimeState: RuntimeState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "TELEMETRY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonBlue.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                TelemetryRow("CPU", "OPTIMIZED", Icons.Default.Memory)
                TelemetryRow("GPU", "HARDENED", Icons.Default.Speed)
                TelemetryRow("VFS", "MOUNTED", Icons.Default.Storage)
                TelemetryRow("FPS", "60 / FIXED", Icons.Default.Refresh)
            }
        }

        Text(
            "TERMINAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonBlue.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, GlassWhite, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            TerminalConsole()
        }
    }
}

@Composable
private fun TerminalConsole() {
    val logs = remember { 
        mutableStateListOf(
            "S[0x01] VFS MOUNTED",
            "S[0x01] SHADER COMPILED",
            "S[0x01] RENDERER READY",
            "S[0x01] LIFECYCLE OK"
        ) 
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(logs) { log ->
            Text(
                text = "> $log",
                color = Color(0xFF00FF00),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, GlassWhite, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        content()
    }
}

@Composable
private fun RuntimeStatusBadge(state: RuntimeState) {
    val label = when(state) {
        is RuntimeState.Running -> "RUNNING"
        is RuntimeState.Booting -> "BOOTING"
        is RuntimeState.Error -> "ERROR"
        else -> "IDLE"
    }
    val color = when(state) {
        is RuntimeState.Running -> Color(0xFF00FF00)
        is RuntimeState.Error -> Color.Red
        is RuntimeState.Booting -> NeonBlue
        else -> Color.Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomePanel(state: RuntimeState, onNavigateToGame: (GameEntry) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "CORE DASHBOARD",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("NEXUS PLAYER ALPHA", color = NeonBlue, fontWeight = FontWeight.Bold)
                    Text("Core runtime operational.", color = Color.Gray, fontSize = 12.sp)
                }
                Icon(Icons.Default.Verified, null, tint = NeonBlue, modifier = Modifier.size(32.dp))
            }
        }

        if (state is RuntimeState.Running) {
            Text("NOW PLAYING", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gamepad, null, tint = NeonBlue)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(state.gameId, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(if (state.isNative) "NATIVE ENGINE" else "HTML5 ENGINE", color = Color.Gray, fontSize = 12.sp)
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIBRARY", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Button(
                onClick = importGameAction,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("IMPORT")
            }
        }

        if (games.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No games imported.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
    GlassCard(
        modifier = Modifier.height(180.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(game.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(game.engine.displayName, color = Color.Gray, fontSize = 10.sp)
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable fun RuntimePanel(controller: RuntimeController) { 
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("RUNTIME CONTROL", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text("TELEMETRY STREAM ACTIVE", color = Color(0xFF00FF00), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
@Composable fun ToolsPanel() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("TOOLS PANEL", color = Color.Gray) } }
@Composable fun SystemPanel() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("SYSTEM CORE", color = Color.Gray) } }
@Composable fun SettingsPanel() { 
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("SETTINGS", style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
        SettingsScreen()
    }
}
