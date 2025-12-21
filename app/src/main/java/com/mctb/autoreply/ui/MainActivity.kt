package com.mctb.autoreply.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mctb.autoreply.ui.screens.ActiveHoursScreen
import com.mctb.autoreply.ui.screens.HomeScreen
import com.mctb.autoreply.ui.screens.MessageEditorScreen
import com.mctb.autoreply.ui.screens.UsageScreen
import com.mctb.autoreply.ui.theme.AppTheme

/**
 * Main activity for the app.
 * Sets up Jetpack Compose navigation and theming.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(navController)
                        }
                        composable("message_editor") {
                            MessageEditorScreen(navController)
                        }
                        composable("active_hours") {
                            ActiveHoursScreen(navController)
                        }
                        composable("usage") {
                            UsageScreen(navController)
                        }
                    }
                }
            }
        }
    }
}
