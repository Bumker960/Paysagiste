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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.LocalConfiguration
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
    object Accueil : BottomNavItem(ScreenDestinations.ACCUEIL_ROUTE, "Accueil", Icons.Filled.Home) // NOUVEL ITEM
    object Chantiers : BottomNavItem(ScreenDestinations.CHANTIER_LIST_ROUTE, "Chantiers", Icons.Filled.List)
    object TontesPrio : BottomNavItem(ScreenDestinations.TONTES_PRIORITAIRES_ROUTE, "Tontes Prio.", Icons.Filled.Grass)
    object Carte : BottomNavItem(ScreenDestinations.MAP_ROUTE, "Carte", Icons.Filled.LocationOn)
}

// Éléments pour le Navigation Drawer
sealed class DrawerNavItem(val route: String, val label: String, val icon: ImageVector) {
    object TaillesPrio : DrawerNavItem(ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE, "Tailles Prio.", Icons.Filled.ContentCut)
    object DesherbagesPrio : DrawerNavItem(ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE, "Désherbage Prio.", Icons.Filled.Spa)
    object FacturationExtras : DrawerNavItem(ScreenDestinations.FACTURATION_EXTRAS_ROUTE, "Facturation Extras", Icons.Filled.EuroSymbol)
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
    onPrimaryContainer = ModernColors.selectedContent,
    surfaceVariant = Color(0xFF2C2C2C),
    outline = Color(0xFF8A8A8A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF388E3C),
    secondary = Color(0xFF689F38),
    tertiary = Color(0xFF9CCC65),
    background = Color(0xFFF7F7F7),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    primaryContainer = ModernColors.barBackground,
    onPrimaryContainer = ModernColors.selectedContent,
    surfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFF757575)
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
        route == ScreenDestinations.ACCUEIL_ROUTE -> "Tableau de Bord" // NOUVEAU TITRE
        route == ScreenDestinations.CHANTIER_LIST_ROUTE -> "Mes Chantiers"
        route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true -> {
            selectedChantier?.nomClient ?: "Détail Chantier"
        }
        route == ScreenDestinations.TONTES_PRIORITAIRES_ROUTE -> "Tontes Prioritaires"
        route == ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE -> "Tailles Prioritaires"
        route == ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE -> "Désherbages Prioritaires"
        route == ScreenDestinations.SETTINGS_ROUTE -> "Réglages"
        route == ScreenDestinations.MAP_ROUTE -> "Carte des Chantiers"
        route == ScreenDestinations.FACTURATION_EXTRAS_ROUTE -> "Facturation Extras"
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
    val configuration = LocalConfiguration.current
    val drawerWidth = configuration.screenWidthDp.dp * 0.75f


    val bottomNavItems = listOf(
        BottomNavItem.Accueil, // MIS EN PREMIÈRE POSITION
        BottomNavItem.Chantiers,
        BottomNavItem.TontesPrio,
        BottomNavItem.Carte
    )

    val drawerNavItems = listOf(
        DrawerNavItem.TaillesPrio,
        DrawerNavItem.DesherbagesPrio,
        DrawerNavItem.FacturationExtras,
        DrawerNavItem.Reglages
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(drawerWidth)
            ) {
                Text(
                    "Menu Principal",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
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
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
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
                        val isSelected = currentDestination?.hierarchy?.any { hir ->
                            hir.route == screen.route ||
                                    // Cas spécial: considérer l'onglet Chantiers comme sélectionné si on est sur un détail de chantier
                                    (screen.route == ScreenDestinations.CHANTIER_LIST_ROUTE && hir.route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true)
                        } == true

                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = isSelected,
                            onClick = {
                                val targetRoute = screen.route
                                // Naviguer seulement si la destination est différente,
                                // OU si la cible est Accueil (pour réinitialiser sa pile).
                                if (currentDestination?.route != targetRoute || targetRoute == ScreenDestinations.ACCUEIL_ROUTE) {
                                    navController.navigate(targetRoute) {
                                        if (targetRoute == ScreenDestinations.ACCUEIL_ROUTE) {
                                            // Pour Accueil, on pop jusqu'à lui-même (inclus) pour vider sa pile interne
                                            popUpTo(navController.graph.findStartDestination().id) { // graph.findStartDestination().id est ACCUEIL_ROUTE
                                                inclusive = true
                                                saveState = true
                                            }
                                        } else {
                                            // Comportement standard pour les autres onglets
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                        }
                                        launchSingleTop = true
                                        restoreState = true // Important pour restaurer l'état des onglets
                                    }
                                }
                                // Effacer le chantier sélectionné si on navigue vers une liste principale ou l'accueil
                                if (targetRoute == ScreenDestinations.CHANTIER_LIST_ROUTE || targetRoute == ScreenDestinations.ACCUEIL_ROUTE) {
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
                startDestination = ScreenDestinations.ACCUEIL_ROUTE, // DÉMARRAGE SUR L'ACCUEIL
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(route = ScreenDestinations.ACCUEIL_ROUTE) { // NOUVELLE ROUTE
                    HomeScreen(viewModel = chantierViewModel, navController = navController)
                }
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
                composable(route = ScreenDestinations.FACTURATION_EXTRAS_ROUTE) {
                    FacturationExtrasScreen(viewModel = chantierViewModel, navController = navController)
                }
            }
        }
    }
}
