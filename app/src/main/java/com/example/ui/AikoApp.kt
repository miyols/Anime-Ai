package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.games.GamesScreen
import com.example.ui.memories.MemoriesScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.voice.VoiceCallScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object VoiceCall : Screen("voice_call", "Voice Call", Icons.Default.PhoneInTalk)
    object Games : Screen("games", "Corner", Icons.Default.SportsEsports)
    object Memories : Screen("memories", "Memories", Icons.Default.Favorite)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AikoApp() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()

    val items = listOf(
        Screen.Chat,
        Screen.VoiceCall,
        Screen.Games,
        Screen.Memories,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("nav_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Chat.route) {
                ChatScreen(viewModel = chatViewModel)
            }
            composable(Screen.VoiceCall.route) {
                VoiceCallScreen(
                    chatViewModel = chatViewModel,
                    onEndCall = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.Chat.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Games.route) {
                GamesScreen(viewModel = chatViewModel)
            }
            composable(Screen.Memories.route) {
                MemoriesScreen(viewModel = chatViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = chatViewModel)
            }
        }
    }
}
