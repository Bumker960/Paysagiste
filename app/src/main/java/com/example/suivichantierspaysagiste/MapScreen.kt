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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe // Icône pour "chantier le plus proche"
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

    val defaultCameraPosition = LatLng(46.2276, 2.2137)
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

    // État pour suivre quel marqueur est "sélectionné" pour afficher l'info-bulle
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
                myLocationButtonEnabled = false,
                zoomControlsEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = false // Désactiver la barre d'outils Google Maps au clic sur marqueur
            )
        )
    }

    fun findNearestChantier() {
        if (!hasLocationPermission || userLocation == null) {
            Toast.makeText(context, "Position utilisateur inconnue ou permission refusée.", Toast.LENGTH_SHORT).show()
            if (!hasLocationPermission) locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        if (chantiersAvecCoordonnees.isEmpty()) {
            Toast.makeText(context, "Aucun chantier avec coordonnées disponible.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserLocation = Location("").apply {
            latitude = userLocation!!.latitude
            longitude = userLocation!!.longitude
        }

        var nearestChantier: Chantier? = null
        var minDistance = Float.MAX_VALUE

        chantiersAvecCoordonnees.forEach { chantier ->
            val chantierLocation = Location("").apply {
                latitude = chantier.latitude!!
                longitude = chantier.longitude!!
            }
            val distance = currentUserLocation.distanceTo(chantierLocation) // Distance en mètres
            if (distance < minDistance) {
                minDistance = distance
                nearestChantier = chantier
            }
        }

        nearestChantier?.let {
            val chantierLatLng = LatLng(it.latitude!!, it.longitude!!)
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(chantierLatLng, 15f),
                    1000
                )
            }
            // Trouver l'état du marqueur correspondant pour le "sélectionner"
            // Ceci est une simplification ; une meilleure approche nécessiterait de stocker les MarkerStates
            // ou de trouver un moyen d'ouvrir l'info-bulle programmatiquement via l'API GoogleMap.
            // Pour l'instant, on affiche un Toast.
            val distanceKm = minDistance / 1000f
            val df = DecimalFormat("#.##")
            Toast.makeText(context, "Chantier le plus proche: ${it.nomClient} à ${df.format(distanceKm)} km", Toast.LENGTH_LONG).show()

            // Alternative: Mettre à jour un état pour que le Marker concerné s'affiche différemment ou ouvre son info-bulle.
            // selectedMarkerState = MarkerState(position = chantierLatLng) // Ne fonctionne pas directement pour ouvrir info-bulle.
            // L'API Compose Maps actuelle ne permet pas d'ouvrir une info-bulle par programmation aussi facilement.
            // La navigation vers les détails au clic sur l'info-bulle reste la méthode principale d'interaction après sélection.
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carte des Chantiers") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
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
                        containerColor = MaterialTheme.colorScheme.secondaryContainer, // Couleur distincte
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Centrer sur ma position")
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Espace entre les FABs

                    FloatingActionButton(
                        onClick = { findNearestChantier() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Filled.NearMe, contentDescription = "Trouver chantier le plus proche")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapLoaded = {
                    if (chantiersAvecCoordonnees.isNotEmpty() && userLocation == null) {
                        val firstChantierLatLng = LatLng(chantiersAvecCoordonnees.first().latitude!!, chantiersAvecCoordonnees.first().longitude!!)
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(firstChantierLatLng, 10f))
                    }
                }
            ) {
                chantiersAvecCoordonnees.forEach { chantier ->
                    // Créer un MarkerState pour chaque marqueur pour potentiellement le contrôler
                    val markerState = rememberMarkerState(position = LatLng(chantier.latitude!!, chantier.longitude!!))

                    Marker(
                        state = markerState,
                        title = chantier.nomClient,
                        snippet = chantier.adresse ?: "Aucune adresse",
                        // icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN), // Option de couleur
                        onClick = {
                            // Au clic sur le marqueur, l'info-bulle s'affiche par défaut.
                            // On peut stocker ce markerState si on veut le manipuler (mais l'API est limitée pour cela).
                            selectedMarkerState = markerState
                            false // Retourner false pour indiquer que l'on n'a pas consommé l'événement, laissant Google Maps afficher l'info-bulle.
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

// La fonction getLastKnownLocation reste inchangée
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