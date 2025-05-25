package com.example.suivichantierspaysagiste

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Grass // Tonte
import androidx.compose.material.icons.filled.ContentCut // Taille
import androidx.compose.material.icons.filled.Spa // Icône pour Désherbage (alternative: Eco, Nature, Yard)
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    object DesherbagesPrio : BottomNavItem(ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE, "Désherbage", Icons.Filled.Spa)
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
    primaryContainer = ModernColors.barBackground, // Utilisé pour la carte du chrono
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
    primaryContainer = ModernColors.barBackground, // Utilisé pour la carte du chrono
    onPrimaryContainer = ModernColors.selectedContent
)

class MainActivity : ComponentActivity() {

    private val chantierViewModel: ChantierViewModel by viewModels {
        ChantierViewModelFactory(
            application,
            (application as MonApplicationChantiers).chantierRepository
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(application)
    }

    // Lanceur pour la permission de notification
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // La permission est accordée.
            } else {
                // L'utilisateur a refusé la permission.
                // Vous pouvez afficher un message expliquant pourquoi la permission est nécessaire.
            }
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // La permission est déjà accordée
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Afficher une UI expliquant pourquoi la permission est utile
                // puis appeler requestPermissionLauncher.launch(...)
                // Pour l'instant, on demande directement :
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Demander directement la permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannelForChronoService(this)
        askNotificationPermission() // Demander la permission au démarrage

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

    private fun createNotificationChannelForChronoService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chronomètre Service Channel"
            val descriptionText = "Affiche le chronomètre en cours pour une intervention"
            val importance = NotificationManager.IMPORTANCE_LOW // LOW pour ne pas être trop intrusif
            val channel = NotificationChannel(ChronomailleurService.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
    val context = LocalContext.current // Pour demander la permission

    // Demander la permission de notification si ce n'est pas déjà fait
    // (Bien que ce soit mieux dans onCreate de l'Activity pour un seul appel)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // La logique de demande de permission est dans MainActivity.onCreate maintenant.
                // On pourrait ajouter un rappel ici si l'utilisateur navigue vers un écran qui en a besoin
                // et qu'elle n'a pas été accordée.
            }
        }
    }


    val bottomNavItems = listOf(
        BottomNavItem.Chantiers,
        BottomNavItem.TontesPrio,
        BottomNavItem.TaillesPrio,
        BottomNavItem.DesherbagesPrio,
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
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route || (it.route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true && screen.route == ScreenDestinations.CHANTIER_LIST_ROUTE) } == true

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = isSelected,
                        onClick = {
                            val targetRoute = screen.route
                            if (targetRoute == ScreenDestinations.CHANTIER_LIST_ROUTE) {
                                chantierViewModel.clearSelectedChantierId()
                                if (currentDestination?.route != ScreenDestinations.CHANTIER_LIST_ROUTE || currentDestination?.route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                if (currentDestination?.route != targetRoute) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
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
                    Text("Erreur: Chantier ID manquant") // Devrait être géré plus élégamment
                }
            }
            composable(ScreenDestinations.TONTES_PRIORITAIRES_ROUTE) {
                TontesPrioritairesScreen(viewModel = chantierViewModel, navController = navController)
            }
            composable(ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE) {
                TaillesPrioritairesScreen(viewModel = chantierViewModel, navController = navController)
            }
            composable(ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE) {
                DesherbagesPrioritairesScreen(viewModel = chantierViewModel, navController = navController)
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
