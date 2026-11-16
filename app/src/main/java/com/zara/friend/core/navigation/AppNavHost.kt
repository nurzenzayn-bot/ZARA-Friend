package com.zara.friend.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zara.friend.feature.home.HomeScreen

/**
 * Graph navigasi MVP.
 * Layar Riwayat/Chat/Pengaturan lengkap menyusul di Fase 5;
 * placeholder dipasang agar struktur navigasi sudah final.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartChat = { navController.navigate(Routes.chat()) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        // Placeholder Fase 5 (chat dengan conversationId nyata menyusul)
        composable(Routes.CHAT_PATTERN) {
            com.zara.friend.feature.chat.ChatPlaceholderScreen()
        }

        composable(Routes.HISTORY) {
            com.zara.friend.feature.history.HistoryPlaceholderScreen()
        }

        composable(Routes.SETTINGS) {
            com.zara.friend.feature.settings.SettingsPlaceholderScreen()
        }
    }
}
