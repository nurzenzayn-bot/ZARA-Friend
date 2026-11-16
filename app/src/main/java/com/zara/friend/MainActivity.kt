package com.zara.friend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zara.friend.core.navigation.AppNavHost
import com.zara.friend.core.ui.theme.ZaraFriendTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Seluruh navigasi ditangani Navigation Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZaraFriendTheme {
                AppNavHost()
            }
        }
    }
}
