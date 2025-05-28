package com.example.suivichantierspaysagiste

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent // AJOUT: Import pour Intent
import android.content.pm.PackageManager
import android.location.Location // IMPORT NÉCESSAIRE POUR LE CALCUL DE DISTANCE
import android.net.Uri // AJOUT: Import pour Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility // AJOUT: Import pour AnimatedVisibility
import androidx.compose.animation.slideInVertically // AJOUT: Import pour slideInVertically
import androidx.compose.animation.slideOutVertically // AJOUT: Import pour slideOutVertically
import androidx.compose.foundation.layout.Arrangement // AJOUT: Import pour Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column // Pour superposer les FABs
import androidx.compose.foundation.layout.Row // AJOUT: Import pour Row
import androidx.compose.foundation.layout.Spacer // Pour espacer les FABs
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth // AJOUT: Import pour fillMaxWidth
import androidx.compose.foundation.layout.height // Pour espacer les FABs
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass // Icône pour Tonte
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation // AJOUT: Icône pour Navigation
// import androidx.compose.material.icons.filled.NearMe // Icône pour "chantier le plus proche" (générique) - Déjà commenté
import androidx.compose.material3.Button // AJOUT: Import pour Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.google.maps.android.compose.MarkerState // AJOUT: Import explicite, même si déjà utilisé implicitement
import com.google.maps.android.compose.rememberCameraPositionState
// import com.google.maps.android.compose.rememberMarkerState // Pour gérer l'état du marqueur sélectionné - Déjà commenté
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

    // Utilisation de mapData pour les marqueurs
    val mapChantiersData by chantierViewModel.mapData.collectAsState()

    // Récupération du chantier sélectionné depuis le ViewModel
    val selectedMapChantier by chantierViewModel.selectedMapChantier.collectAsState()

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

    // selectedMarkerState n'est plus nécessaire pour suivre le chantier pour l'itinéraire
    // var selectedMarkerState: MarkerState? by remember { mutableStateOf(null) }


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
                if (cameraPositionState.position.target == defaultCameraPosition || mapChantiersData.isEmpty()) {
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
                myLocationButtonEnabled = false,
                zoomControlsEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = true // Garder true pour le comportement par défaut de l'info-bulle
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
                // Sélectionner le chantier correspondant pour afficher le bouton d'itinéraire
                result.chantier?.let { chantierViewModel.setSelectedMapChantier(it) }
            }
        }
    }

    fun launchNavigation(chantier: Chantier) {
        if (chantier.latitude != null && chantier.longitude != null) {
            val gmmIntentUri = Uri.parse("geo:${chantier.latitude},${chantier.longitude}?q=${chantier.latitude},${chantier.longitude}(${Uri.encode(chantier.nomClient)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            // mapIntent.setPackage("com.google.android.apps.maps") // Pour cibler Google Maps spécifiquement
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                Toast.makeText(context, "Aucune application GPS trouvée.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Coordonnées du chantier non disponibles.", Toast.LENGTH_SHORT).show()
        }
    }


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
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Centrer sur ma position")
                    }
                }

                FloatingActionButton(
                    onClick = { findAndShowNearestMowingSite() },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Filled.Grass, contentDescription = "Chantier de tonte le plus proche")
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
                    if (mapChantiersData.isNotEmpty() && userLocation == null && cameraPositionState.position.target == defaultCameraPosition) {
                        val firstChantierData = mapChantiersData.first()
                        val firstChantierLatLng = LatLng(firstChantierData.chantier.latitude!!, firstChantierData.chantier.longitude!!)
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(firstChantierLatLng, 10f))
                    }
                },
                onMapClick = { // AJOUT: Pour désélectionner le chantier si on clique sur la carte
                    chantierViewModel.clearSelectedMapChantier()
                }
            ) {
                mapChantiersData.forEach { mapData ->
                    val chantier = mapData.chantier
                    val markerState = MarkerState(position = LatLng(chantier.latitude!!, chantier.longitude!!))

                    Marker(
                        state = markerState,
                        title = chantier.nomClient,
                        snippet = chantier.adresse ?: "Aucune adresse",
                        icon = BitmapDescriptorFactory.defaultMarker(mapData.markerHue),
                        onClick = {
                            // Mettre à jour le chantier sélectionné dans le ViewModel
                            chantierViewModel.setSelectedMapChantier(chantier)
                            // Laisser l'info-bulle s'afficher par défaut
                            false
                        },
                        onInfoWindowClick = {
                            // Naviguer vers les détails si l'info-bulle est cliquée
                            navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/${chantier.id}")
                        },
                        onInfoWindowClose = { // AJOUT: Désélectionner si l'info-bulle est fermée
                            if (selectedMapChantier == chantier) { // Seulement si c'est le chantier actuellement sélectionné
                                chantierViewModel.clearSelectedMapChantier()
                            }
                        }
                    )
                }
            }

            // AJOUT: Bouton "Lancer l'itinéraire" en bas de l'écran
            AnimatedVisibility(
                visible = selectedMapChantier != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp) // Espacement par rapport au bas
            ) {
                selectedMapChantier?.let { chantier ->
                    Button(
                        onClick = { launchNavigation(chantier) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Icon(Icons.Filled.Navigation, contentDescription = "Icône de navigation", modifier = Modifier.padding(end = 8.dp))
                        Text("Lancer l'itinéraire vers ${chantier.nomClient}")
                    }
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
