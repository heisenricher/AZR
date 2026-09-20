package com.offline.toolbox

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.offline.toolbox.core.database.PreferencesRepository
import com.offline.toolbox.core.database.ToolboxDatabase
import com.offline.toolbox.core.database.UserPreferences
import com.offline.toolbox.core.designsystem.theme.OfflineToolboxTheme
import com.offline.toolbox.features.home.HomeScreen
import com.offline.toolbox.features.settings.SettingsScreen
import com.offline.toolbox.features.toolrunner.ToolRunnerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = ToolboxDatabase.getInstance(this)
        val favoriteDao = database.favoriteDao()
        val recentDao = database.recentDao()
        val preferencesRepository = PreferencesRepository(this)

        setContent {
            val preferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferences())

            OfflineToolboxTheme(
                themeMode = preferences.themeMode,
                dynamicColor = preferences.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                favoriteDao = favoriteDao,
                                recentDao = recentDao,
                                onToolClick = { toolId ->
                                    navController.navigate("tool/$toolId")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable(
                            route = "tool/{toolId}?payload={payload}",
                            arguments = listOf(
                                navArgument("toolId") { type = NavType.StringType },
                                navArgument("payload") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val toolId = backStackEntry.arguments?.getString("toolId") ?: ""
                            val rawPayload = backStackEntry.arguments?.getString("payload")
                            val payload = rawPayload?.let { Uri.decode(it) }

                            ToolRunnerScreen(
                                toolId = toolId,
                                initialPayload = payload,
                                favoriteDao = favoriteDao,
                                recentDao = recentDao,
                                onBackClick = { navController.popBackStack() },
                                onNavigateToTool = { targetToolId, newPayload ->
                                    val encodedPayload = Uri.encode(newPayload)
                                    navController.navigate("tool/$targetToolId?payload=$encodedPayload")
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                preferencesRepository = preferencesRepository,
                                favoriteDao = favoriteDao,
                                recentDao = recentDao,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
