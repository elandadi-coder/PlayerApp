package com.foot22.samedi.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.foot22.samedi.ui.fees.FeesScreen
import com.foot22.samedi.ui.history.HistoryScreen
import com.foot22.samedi.ui.match.MatchScreen
import com.foot22.samedi.ui.players.PlayersScreen
import com.foot22.samedi.ui.settings.SettingsScreen

enum class AppTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    MATCH("match", "Match", Icons.Default.SportsSoccer),
    PLAYERS("players", "Joueurs", Icons.Default.Groups),
    FEES("fees", "Cotisations", Icons.Default.Payments),
    HISTORY("history", "Historique", Icons.Default.History)
}

private const val SETTINGS_ROUTE = "settings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Samedi22NavShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppTab.MATCH.route
    val isSettings = currentRoute == SETTINGS_ROUTE
    val currentTab = AppTab.entries.find { it.route == currentRoute } ?: AppTab.MATCH

    Scaffold(
        topBar = {
            if (!isSettings) {
                TopAppBar(
                    title = {
                        Text(
                            when (currentTab) {
                                AppTab.MATCH -> "Matchs JB-T5"
                                AppTab.PLAYERS -> "Joueurs & Niveaux"
                                AppTab.FEES -> "Cotisations & Caisse"
                                AppTab.HISTORY -> "Scores & Historique"
                            }
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(SETTINGS_ROUTE) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isSettings) {
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppTab.MATCH.route,
            modifier = if (isSettings) Modifier else Modifier.padding(innerPadding)
        ) {
            composable(AppTab.MATCH.route) { MatchScreen() }
            composable(AppTab.PLAYERS.route) { PlayersScreen() }
            composable(AppTab.FEES.route) { FeesScreen() }
            composable(AppTab.HISTORY.route) { HistoryScreen() }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
