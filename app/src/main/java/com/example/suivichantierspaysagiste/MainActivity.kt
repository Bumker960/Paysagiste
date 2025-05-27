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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Grass // Tonte
import androidx.compose.material.icons.filled.ContentCut // Taille
import androidx.compose.material.icons.filled.Spa // Icône pour Désherbage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LocationOn // ICÔNE POUR LA CARTE
import androidx.compose.material.icons.filled.Menu // ICÔNE POUR LE DRAWER
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration // Import pour obtenir la largeur de l'écran
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch

// Éléments pour la barre de navigation inférieure
sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Chantiers : BottomNavItem(ScreenDestinations.CHANTIER_LIST_ROUTE, "Chantiers", Icons.Filled.List)
    object TontesPrio : BottomNavItem(ScreenDestinations.TONTES_PRIORITAIRES_ROUTE, "Tontes Prio.", Icons.Filled.Grass)
    object Carte : BottomNavItem(ScreenDestinations.MAP_ROUTE, "Carte", Icons.Filled.LocationOn)
    // La page d'accueil sera ajoutée ici plus tard
}

// Éléments pour le Navigation Drawer
sealed class DrawerNavItem(val route: String, val label: String, val icon: ImageVector) {
    object TaillesPrio : DrawerNavItem(ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE, "Tailles Prio.", Icons.Filled.ContentCut)
    object DesherbagesPrio : DrawerNavItem(ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE, "Désherbage Prio.", Icons.Filled.Spa)
    object Reglages : DrawerNavItem(ScreenDestinations.SETTINGS_ROUTE, "Réglages", Icons.Filled.Settings)
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
        ChantierViewModelFactory(
            application,
            (application as MonApplicationChantiers).chantierRepository
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(application)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // La permission est accordée.
            } else {
                // L'utilisateur a refusé la permission.
            }
        }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // La permission est déjà accordée
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannelForChronoService(this)
        askNotificationPermission()

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
                AppNavigationWithDrawer(
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
            val importance = NotificationManager.IMPORTANCE_LOW
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

@Composable
fun GetCurrentScreenTitle(route: String?, chantierViewModel: ChantierViewModel): String {
    val selectedChantier by chantierViewModel.selectedChantier.collectAsStateWithLifecycle()

    return when {
        route == ScreenDestinations.CHANTIER_LIST_ROUTE -> "Mes Chantiers"
        route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true -> {
            selectedChantier?.nomClient ?: "Détail Chantier"
        }
        route == ScreenDestinations.TONTES_PRIORITAIRES_ROUTE -> "Tontes Prioritaires"
        route == ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE -> "Tailles Prioritaires"
        route == ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE -> "Désherbages Prioritaires"
        route == ScreenDestinations.SETTINGS_ROUTE -> "Réglages"
        route == ScreenDestinations.MAP_ROUTE -> "Carte des Chantiers"
        else -> "SP"
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationWithDrawer(
    chantierViewModel: ChantierViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController: NavHostController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current // Pour obtenir la largeur de l'écran
    val drawerWidth = configuration.screenWidthDp.dp / 1.5f // Ajuster le diviseur pour la largeur souhaitée (ex: 1.5f pour un peu plus de la moitié, 2f pour la moitié)


    val bottomNavItems = listOf(
        BottomNavItem.Chantiers,
        BottomNavItem.TontesPrio,
        BottomNavItem.Carte
    )

    val drawerNavItems = listOf(
        DrawerNavItem.TaillesPrio,
        DrawerNavItem.DesherbagesPrio,
        DrawerNavItem.Reglages
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, // Désactive le geste d'ouverture par swipe, mais permet le swipe pour fermer si ouvert
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f) // Occupe 75% de la largeur de l'écran, ou utilisez drawerWidth
                // modifier = Modifier.width(drawerWidth) // Alternative avec une largeur calculée
            ) {
                Spacer(Modifier.height(12.dp))
                drawerNavItems.forEach { item ->
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val titleText = GetCurrentScreenTitle(currentRoute, chantierViewModel)
                        Text(text = titleText)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Ouvrir/Fermer le menu de navigation")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
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
                                if (currentDestination?.route != targetRoute) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = targetRoute != ScreenDestinations.CHANTIER_LIST_ROUTE
                                            inclusive = targetRoute == ScreenDestinations.CHANTIER_LIST_ROUTE && currentDestination?.route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true
                                        }
                                        launchSingleTop = true
                                        restoreState = targetRoute != ScreenDestinations.CHANTIER_LIST_ROUTE
                                    }
                                } else if (targetRoute == ScreenDestinations.CHANTIER_LIST_ROUTE && currentDestination?.route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true) {
                                    navController.popBackStack(ScreenDestinations.CHANTIER_LIST_ROUTE, inclusive = false)
                                }
                                if (targetRoute == ScreenDestinations.CHANTIER_LIST_ROUTE) {
                                    chantierViewModel.clearSelectedChantierId()
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
                composable(ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE) {
                    DesherbagesPrioritairesScreen(viewModel = chantierViewModel, navController = navController)
                }
                composable(route = ScreenDestinations.SETTINGS_ROUTE) {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        navController = navController
                    )
                }
                composable(route = ScreenDestinations.MAP_ROUTE) {
                    MapScreen(chantierViewModel = chantierViewModel, navController = navController)
                }
            }
        }
    }
}
