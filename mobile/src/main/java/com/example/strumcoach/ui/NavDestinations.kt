package com.example.strumcoach.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Exercises : Screen("exercises", "Exercises", Icons.Default.LibraryMusic)
    object Songs : Screen("songs", "Songs", Icons.Default.MusicNote)
    object Community : Screen("community", "Community", Icons.Default.Groups)
    object Progress : Screen("progress", "Progress", Icons.Default.History)
    object SessionReport : Screen("report", "Report", Icons.Default.History)
    object Achievements : Screen("achievements", "Achievements", Icons.Default.EmojiEvents)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val navItems = listOf(
    Screen.Dashboard,
    Screen.Exercises,
    Screen.Songs,
    Screen.Community,
    Screen.Progress
)
