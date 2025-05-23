package com.example.suivichantierspaysagiste

import android.app.Application // Assurez-vous que cet import est là
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

// Définition des écrans pour la barre de navigation
sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Chantiers : BottomNavItem(ScreenDestinations.CHANTIER_LIST_ROUTE, "Chantiers", Icons.Filled.List)
    object TontesPrio : BottomNavItem(ScreenDestinations.TONTES_PRIORITAIRES_ROUTE, "Tontes Prio.", Icons.Filled.Grass)
    object TaillesPrio : BottomNavItem(ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE, "Tailles Prio.", Icons.Filled.ContentCut)
    object Reglages : BottomNavItem(ScreenDestinations.SETTINGS_ROUTE, "Réglages", Icons.Filled.Settings)
}

// Définition de nos couleurs modernes
object ModernColors {
    val barBackground = Color(0xFF004D40)
    val selectedContent = Color.White
    val unselectedContent = Color(0xFFB2DFDB)
}

// Définition des schémas de couleurs
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF558B2F),
    secondary = Color(0xFF8BC34A),
    tertiary = Color(0xFFAED581),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = ModernColors.barBackground,
    onPrimaryContainer = ModernColors.selectedContent
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF388E3C),
    secondary = Color(0xFF689F38),
    tertiary = Color(0xFF9CCC65),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    primaryContainer = ModernColors.barBackground,
    onPrimaryContainer = ModernColors.selectedContent
)

class MainActivity : ComponentActivity() {

    private val chantierViewModel: ChantierViewModel by viewModels {
        // MODIFICATION ICI pour passer 'application' à la factory
        ChantierViewModelFactory(
            application, // Passer l'instance de l'application
            (application as MonApplicationChantiers).chantierRepository
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDarkTheme by settingsViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()

            val colors = if (useDarkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            }

            MaterialTheme(
                colorScheme = colors
            ) {
                AppNavigation(
                    chantierViewModel = chantierViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    chantierViewModel: ChantierViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController: NavHostController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem.Chantiers,
        BottomNavItem.TontesPrio,
        BottomNavItem.TaillesPrio,
        BottomNavItem.Reglages
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = ModernColors.barBackground
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            if (screen.route == ScreenDestinations.CHANTIER_LIST_ROUTE) {
                                chantierViewModel.onSearchQueryChanged("")
                            }
                            navController.navigate(screen.route) {
                                popUpTo(screen.route) {
                                    inclusive = true
                                    saveState = false
                                }
                                launchSingleTop = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ModernColors.selectedContent,
                            selectedTextColor = ModernColors.selectedContent,
                            unselectedIconColor = ModernColors.unselectedContent,
                            unselectedTextColor = ModernColors.unselectedContent,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScreenDestinations.CHANTIER_LIST_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = ScreenDestinations.CHANTIER_LIST_ROUTE) {
                ChantierListScreen(viewModel = chantierViewModel, navController = navController)
            }
            composable(
                ScreenDestinations.CHANTIER_DETAIL_ROUTE_TEMPLATE,
                arguments = listOf(navArgument(ScreenDestinations.CHANTIER_ID_ARG) { type = NavType.LongType })
            ) { backStackEntry ->
                val chantierId = backStackEntry.arguments?.getLong(ScreenDestinations.CHANTIER_ID_ARG)
                if (chantierId != null) {
                    ChantierDetailScreen(
                        chantierId = chantierId,
                        viewModel = chantierViewModel,
                        navController = navController
                    )
                } else {
                    Text("Erreur: Chantier ID manquant")
                }
            }
            composable(ScreenDestinations.TONTES_PRIORITAIRES_ROUTE) {
                TontesPrioritairesScreen(viewModel = chantierViewModel, navController = navController)
            }
            composable(ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE) {
                TaillesPrioritairesScreen(viewModel = chantierViewModel, navController = navController)
            }
            composable(route = ScreenDestinations.SETTINGS_ROUTE) {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    navController = navController
                )
            }
        }
    }
}