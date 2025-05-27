package com.example.suivichantierspaysagiste

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location // IMPORT NÉCESSAIRE POUR LE CALCUL DE DISTANCE
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column // Pour superposer les FABs
import androidx.compose.foundation.layout.Spacer // Pour espacer les FABs
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height // Pour espacer les FABs
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass // Icône pour Tonte
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe // Icône pour "chantier le plus proche" (générique)
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold // Importation de Scaffold, même si on ne l'utilise que pour le FAB
import androidx.compose.material3.Text
// import androidx.compose.material3.TopAppBar // Supprimé car géré globalement
// import androidx.compose.material3.TopAppBarDefaults // Supprimé
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment // Pour aligner les FABs
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState // Pour gérer l'état du marqueur sélectionné
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    chantierViewModel: ChantierViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tousLesChantiers by chantierViewModel.tousLesChantiers.collectAsState()
    val chantiersAvecCoordonnees = remember(tousLesChantiers) {
        tousLesChantiers.filter { it.latitude != null && it.longitude != null }
    }

    val defaultCameraPosition = LatLng(46.2276, 2.2137) // Centre de la France
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 5f)
    }

    var userLocation: LatLng? by remember { mutableStateOf(null) }
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedMarkerState: MarkerState? by remember { mutableStateOf(null) }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                hasLocationPermission = true
                scope.launch {
                    userLocation = getLastKnownLocation(context, fusedLocationClient)
                    userLocation?.let {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(it, 15f)
                            ),
                            1000
                        )
                    }
                }
            } else {
                Toast.makeText(context, "Permission de localisation refusée.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            scope.launch {
                userLocation = getLastKnownLocation(context, fusedLocationClient)
                // Centrer sur l'utilisateur seulement si aucun chantier n'est déjà affiché ou si la carte est à sa position par défaut
                if (cameraPositionState.position.target == defaultCameraPosition || chantiersAvecCoordonnees.isEmpty()) {
                    userLocation?.let {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(it, 12f)
                            ),
                            1000
                        )
                    }
                }
            }
        }
    }

    val mapProperties by remember(userLocation, hasLocationPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = userLocation != null && hasLocationPermission,
            )
        )
    }
    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = false, // On utilise notre propre FAB
                zoomControlsEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        )
    }

    fun findAndShowNearestMowingSite() {
        if (!hasLocationPermission || userLocation == null) {
            Toast.makeText(context, "Position utilisateur inconnue ou permission refusée.", Toast.LENGTH_SHORT).show()
            if (!hasLocationPermission) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        chantierViewModel.findNearestMowingSite(userLocation!!) { result ->
            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            result.latLng?.let { siteLatLng ->
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(siteLatLng, 15f),
                        1000
                    )
                }
                // Mettre en évidence le marqueur ou ouvrir l'info-bulle si possible
            }
        }
    }

    // Le Scaffold ici est uniquement pour gérer les FABs spécifiques à cet écran.
    // La TopAppBar est gérée globalement.
    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (hasLocationPermission) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                val location = getLastKnownLocation(context, fusedLocationClient)
                                location?.let {
                                    userLocation = it
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(it, 15f),
                                        1000
                                    )
                                } ?: run {
                                    Toast.makeText(context, "Impossible de récupérer la position actuelle.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp) // Espace entre les FABs si plusieurs
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Centrer sur ma position")
                    }
                }

                FloatingActionButton(
                    onClick = { findAndShowNearestMowingSite() },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer, // Couleur distincte
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    // pas de padding bottom ici si c'est le dernier FAB
                ) {
                    Icon(Icons.Filled.Grass, contentDescription = "Chantier de tonte le plus proche")
                }
            }
        }
    ) { innerPadding -> // Ce innerPadding vient du Scaffold local, qui est à l'intérieur du Scaffold global
        Box(
            modifier = Modifier
                .padding(innerPadding) // Appliquer le padding du Scaffold local (pour les FABs)
                .fillMaxSize(),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapLoaded = {
                    if (chantiersAvecCoordonnees.isNotEmpty() && userLocation == null && cameraPositionState.position.target == defaultCameraPosition) {
                        val firstChantierLatLng = LatLng(chantiersAvecCoordonnees.first().latitude!!, chantiersAvecCoordonnees.first().longitude!!)
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(firstChantierLatLng, 10f))
                    }
                }
            ) {
                chantiersAvecCoordonnees.forEach { chantier ->
                    val markerState = rememberMarkerState(position = LatLng(chantier.latitude!!, chantier.longitude!!))
                    var iconColor = BitmapDescriptorFactory.HUE_GREEN // Couleur par défaut

                    if (chantier.serviceTonteActive) iconColor = BitmapDescriptorFactory.HUE_GREEN
                    if (chantier.serviceTailleActive && !chantier.serviceTonteActive) iconColor = BitmapDescriptorFactory.HUE_ORANGE
                    if (chantier.serviceDesherbageActive && !chantier.serviceTonteActive && !chantier.serviceTailleActive) iconColor = BitmapDescriptorFactory.HUE_YELLOW


                    Marker(
                        state = markerState,
                        title = chantier.nomClient,
                        snippet = chantier.adresse ?: "Aucune adresse",
                        icon = BitmapDescriptorFactory.defaultMarker(iconColor),
                        onClick = {
                            selectedMarkerState = markerState
                            false // Retourner false pour permettre le comportement par défaut (centrer et afficher l'info window)
                        },
                        onInfoWindowClick = {
                            navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/${chantier.id}")
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
suspend fun getLastKnownLocation(context: Context, fusedLocationClient: FusedLocationProviderClient): LatLng? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Log.w("MapScreen", "getLastKnownLocation appelé sans permission.")
        return null
    }

    return try {
        val locationResult = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
        locationResult?.let { location ->
            Log.d("MapScreen", "Position actuelle obtenue: Lat ${location.latitude}, Lng ${location.longitude}")
            LatLng(location.latitude, location.longitude)
        } ?: run {
            val lastLocation = fusedLocationClient.lastLocation.await()
            lastLocation?.let {
                Log.d("MapScreen", "Dernière position connue obtenue: Lat ${it.latitude}, Lng ${it.longitude}")
                LatLng(it.latitude, it.longitude)
            } ?: run {
                Log.w("MapScreen", "Impossible d'obtenir la localisation (actuelle ou dernière).")
                null
            }
        }
    } catch (e: SecurityException) {
        Log.e("MapScreen", "Erreur de sécurité lors de l'accès à la localisation: ${e.message}")
        null
    } catch (e: Exception) {
        Log.e("MapScreen", "Erreur lors de l'obtention de la localisation: ${e.message}")
        null
    }
}
