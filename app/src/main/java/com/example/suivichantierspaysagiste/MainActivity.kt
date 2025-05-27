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
import androidx.compose.foundation.layout.width // AJOUT DE L'IMPORT POUR MODIFIER.WIDTH
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Garder pour les icônes existantes
import androidx.compose.material3.Divider // AJOUT DE L'IMPORT POUR DIVIDER
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
    object Chantiers : BottomNavItem(ScreenDestinations.CHANTIER_LIST_ROUTE, "Chantiers", Icons.Filled.List)
    object TontesPrio : BottomNavItem(ScreenDestinations.TONTES_PRIORITAIRES_ROUTE, "Tontes Prio.", Icons.Filled.Grass)
    object Carte : BottomNavItem(ScreenDestinations.MAP_ROUTE, "Carte", Icons.Filled.LocationOn)
    // La page d'accueil sera ajoutée ici plus tard
}

// Éléments pour le Navigation Drawer
sealed class DrawerNavItem(val route: String, val label: String, val icon: ImageVector) {
    object TaillesPrio : DrawerNavItem(ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE, "Tailles Prio.", Icons.Filled.ContentCut)
    object DesherbagesPrio : DrawerNavItem(ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE, "Désherbage Prio.", Icons.Filled.Spa)
    object FacturationExtras : DrawerNavItem(ScreenDestinations.FACTURATION_EXTRAS_ROUTE, "Facturation Extras", Icons.Filled.EuroSymbol) // NOUVEL ITEM
    object Reglages : DrawerNavItem(ScreenDestinations.SETTINGS_ROUTE, "Réglages", Icons.Filled.Settings)
}


// Définition de nos couleurs modernes
object ModernColors {
    val barBackground = Color(0xFF004D40) // Vert foncé pour la barre
    val selectedContent = Color.White
    val unselectedContent = Color(0xFFB2DFDB) // Vert plus clair pour non sélectionné
}

// Définition des schémas de couleurs
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF558B2F), // Vert principal plus soutenu
    secondary = Color(0xFF8BC34A), // Vert secondaire
    tertiary = Color(0xFFAED581), // Vert tertiaire
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E), // Un peu plus clair que le fond pour les surfaces comme Card
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = ModernColors.barBackground, // Utilisé pour TopAppBar et BottomNavBar
    onPrimaryContainer = ModernColors.selectedContent, // Texte sur ces conteneurs
    surfaceVariant = Color(0xFF2C2C2C), // Pour les cards en mode sombre, légèrement différent de surface
    outline = Color(0xFF8A8A8A) // Pour les bordures et dividers
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF388E3C), // Vert principal
    secondary = Color(0xFF689F38), // Vert secondaire
    tertiary = Color(0xFF9CCC65), // Vert tertiaire
    background = Color(0xFFF7F7F7), // Fond légèrement blanc cassé
    surface = Color(0xFFFFFFFF), // Blanc pur pour les surfaces comme Card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    primaryContainer = ModernColors.barBackground,
    onPrimaryContainer = ModernColors.selectedContent,
    surfaceVariant = Color(0xFFE0E0E0), // Pour les cards en mode clair, un gris très clair
    outline = Color(0xFF757575) // Pour les bordures et dividers
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
                // Expliquer pourquoi la permission est nécessaire puis la demander
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Demander directement la permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannelForChronoService(this) // S'assurer que le canal est créé
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
                // typography = Typography, // Si vous avez une typo personnalisée
                // shapes = Shapes // Si vous avez des formes personnalisées
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
            val importance = NotificationManager.IMPORTANCE_LOW // LOW pour ne pas être trop intrusif
            val channel = NotificationChannel(ChronomailleurService.NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // Optionnel: désactiver le son et la vibration si c'est une notif silencieuse
                setSound(null, null)
                enableVibration(false)
            }
            // Enregistrer le canal avec le système
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
        route == ScreenDestinations.FACTURATION_EXTRAS_ROUTE -> "Facturation Extras" // NOUVEAU TITRE
        else -> "SP" // Nom de l'application par défaut
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
    val drawerWidth = configuration.screenWidthDp.dp * 0.75f // 75% de la largeur de l'écran


    val bottomNavItems = listOf(
        BottomNavItem.Chantiers,
        BottomNavItem.TontesPrio,
        BottomNavItem.Carte
    )

    val drawerNavItems = listOf(
        DrawerNavItem.TaillesPrio,
        DrawerNavItem.DesherbagesPrio,
        DrawerNavItem.FacturationExtras, // AJOUTÉ ICI
        DrawerNavItem.Reglages
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(drawerWidth) // Utiliser la largeur calculée
            ) {
                Text(
                    "Menu Principal",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                Divider() // CORRIGÉ : Divider est un composant Material3
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
                    containerColor = ModernColors.barBackground // Utilisation de la couleur ModernColors
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
                                            saveState = targetRoute != ScreenDestinations.CHANTIER_LIST_ROUTE // Ne pas sauvegarder l'état de la liste si on va vers un détail
                                            inclusive = targetRoute == ScreenDestinations.CHANTIER_LIST_ROUTE && currentDestination?.route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true
                                        }
                                        launchSingleTop = true
                                        restoreState = targetRoute != ScreenDestinations.CHANTIER_LIST_ROUTE // Ne pas restaurer l'état de la liste si on revient d'un détail
                                    }
                                } else if (targetRoute == ScreenDestinations.CHANTIER_LIST_ROUTE && currentDestination?.route?.startsWith(ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX) == true) {
                                    // Si on est sur un détail et on clique sur "Chantiers", on pop juste le détail
                                    navController.popBackStack(ScreenDestinations.CHANTIER_LIST_ROUTE, inclusive = false)
                                }
                                // Effacer le chantier sélectionné si on navigue vers la liste des chantiers
                                if (targetRoute == ScreenDestinations.CHANTIER_LIST_ROUTE) {
                                    chantierViewModel.clearSelectedChantierId()
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ModernColors.selectedContent,
                                selectedTextColor = ModernColors.selectedContent,
                                unselectedIconColor = ModernColors.unselectedContent,
                                unselectedTextColor = ModernColors.unselectedContent,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Couleur de l'indicateur de sélection
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
                        Text("Erreur: Chantier ID manquant") // Gérer le cas où l'ID est null
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
                        navController = navController // Passer navController si nécessaire
                    )
                }
                composable(route = ScreenDestinations.MAP_ROUTE) { // Assurez-vous que la route est correcte
                    MapScreen(chantierViewModel = chantierViewModel, navController = navController)
                }
                // AJOUT DE LA NOUVELLE ROUTE
                composable(route = ScreenDestinations.FACTURATION_EXTRAS_ROUTE) {
                    FacturationExtrasScreen(viewModel = chantierViewModel, navController = navController)
                }
            }
        }
    }
}
