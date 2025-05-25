package com.example.suivichantierspaysagiste

import android.Manifest
import android.annotation.SuppressLint
// import android.app.Application // Commenté car non utilisé directement ici, mais pourrait l'être si ViewModel est injecté
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme // IMPORT AJOUTÉ
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// import androidx.compose.ui.Alignment // Non utilisé directement, Box centre par défaut si aucun alignement n'est spécifié pour son contenu direct.
import androidx.compose.ui.Modifier
// import androidx.compose.ui.graphics.Color // Non utilisé directement ici après modification TopAppBar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
// import com.google.maps.android.compose.Marker // Commenté car non utilisé pour l'instant
// import com.google.maps.android.compose.MarkerState // Commenté car non utilisé pour l'instant
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // IMPORT AJOUTÉ pour la fonction d'extension await()


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    // chantierViewModel: ChantierViewModel = viewModel(factory = ChantierViewModelFactory(LocalContext.current.applicationContext as Application, (LocalContext.current.applicationContext as MonApplicationChantiers).chantierRepository))
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // État pour la position de la caméra
    val defaultCameraPosition = LatLng(46.2276, 2.2137) // Centre sur la France
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 5f)
    }

    // État pour la position actuelle de l'utilisateur
    var userLocation: LatLng? by remember { mutableStateOf(null) }

    // FusedLocationProviderClient pour obtenir la localisation
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // État des permissions
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Lanceur pour la demande de permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                hasLocationPermission = true
                // La permission est accordée, on essaie de récupérer la localisation
                scope.launch {
                    userLocation = getLastKnownLocation(context, fusedLocationClient)
                    userLocation?.let {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(it, 15f) // Zoom plus proche sur la position utilisateur
                            ),
                            1000 // Durée de l'animation en ms
                        )
                    }
                }
            } else {
                // La permission est refusée. Afficher un message ou gérer ce cas.
                Toast.makeText(context, "Permission de localisation refusée.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Demander la permission au lancement de l'écran si elle n'est pas déjà accordée
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // Si la permission est déjà accordée, récupérer la localisation
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
        }
    }

    // Propriétés et UI Settings pour la carte
    // Mise à jour de mapProperties pour réagir aux changements de userLocation et hasLocationPermission
    val mapProperties by remember(userLocation, hasLocationPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = userLocation != null && hasLocationPermission, // Activer le point bleu si permission et localisation connues
            )
        )
    }
    val uiSettings by remember { // uiSettings ne dépend pas de l'état changeant ici, donc pas besoin de clés pour remember
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = false, // Nous allons créer notre propre bouton
                zoomControlsEnabled = true,      // Activer les contrôles de zoom par défaut
                compassEnabled = true
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carte des Chantiers") },
                colors = TopAppBarDefaults.topAppBarColors(
                    // Utilisation de MaterialTheme pour les couleurs de la TopAppBar
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (hasLocationPermission) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val location = getLastKnownLocation(context, fusedLocationClient)
                            location?.let {
                                userLocation = it // Mettre à jour notre état interne
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(it, 15f),
                                    1000
                                )
                            } ?: run {
                                Toast.makeText(context, "Impossible de récupérer la position actuelle.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Centrer sur ma position")
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
                uiSettings = uiSettings
            ) {
                // Marqueurs des chantiers seront ajoutés ici
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
        val locationResult = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await() // Utilisation de await()
        locationResult?.let { location ->
            Log.d("MapScreen", "Position actuelle obtenue: Lat ${location.latitude}, Lng ${location.longitude}")
            LatLng(location.latitude, location.longitude)
        } ?: run {
            val lastLocation = fusedLocationClient.lastLocation.await() // Utilisation de await()
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