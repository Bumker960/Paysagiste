package com.example.suivichantierspaysagiste

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
// Potentiellement, vous aurez besoin du ChantierViewModel ici plus tard
// import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    // chantierViewModel: ChantierViewModel = viewModel(factory = ChantierViewModelFactory(LocalContext.current.applicationContext as Application, (LocalContext.current.applicationContext as MonApplicationChantiers).chantierRepository))
    // Pour l'instant, nous n'utilisons pas de ViewModel directement ici, mais il sera probablement nécessaire.
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carte des Chantiers") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ModernColors.barBackground, // Utilisation des couleurs ModernColors de MainActivity
                    titleContentColor = ModernColors.selectedContent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp), // Ajout d'un padding pour le contenu si nécessaire
            contentAlignment = Alignment.Center
        ) {
            // Ce Text est un placeholder. Il sera remplacé par le Composable GoogleMap.
            Text("Affichage de la carte Google Maps ici...")
            // Plus tard, nous ajouterons ici :
            // val cameraPositionState = rememberCameraPositionState {
            //     position = CameraPosition.fromLatLngZoom(LatLng(46.2276, 2.2137), 5f) // Centre sur la France
            // }
            // GoogleMap(
            //     modifier = Modifier.fillMaxSize(),
            //     cameraPositionState = cameraPositionState
            // )
        }
    }
}