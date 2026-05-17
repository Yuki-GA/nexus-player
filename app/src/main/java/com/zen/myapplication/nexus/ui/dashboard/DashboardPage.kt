package com.zen.myapplication.nexus.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class DashboardPage(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Library("Library", Icons.Default.Menu),
    Runtime("Runtime", Icons.Default.PlayArrow),
    Tools("Tools", Icons.Default.Build),
    System("System", Icons.Default.Info),
    Settings("Settings", Icons.Default.Settings)
}
