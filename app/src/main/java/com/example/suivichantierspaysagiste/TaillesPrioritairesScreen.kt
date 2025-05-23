package com.example.suivichantierspaysagiste

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Importer Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.Icons
// Imports pour les icônes et IconButton si vous voulez un bouton retour
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.ArrowBack
// import androidx.compose.material3.IconButton
// import androidx.compose.material3.Icon

// Définition d'une couleur pour les boutons, vous pourriez l'ajouter à votre objet ModernColors
val ModernButtonBackgroundColor = Color(0xFF00796B) // Un vert sarcelle un peu plus soutenu
val ModernButtonTextColor = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaillesPrioritairesScreen(
    viewModel: ChantierViewModel,
    navController: NavHostController
) {
    // Collecter la liste des tailles prioritaires depuis le ViewModel
    val listeTaillesPrioritaires by viewModel.taillesPrioritaires.collectAsStateWithLifecycle()
    // Collecter l'ordre de tri actuel (pourrait être utilisé pour styler les boutons de tri)
    val sortOrder by viewModel.taillesSortOrder.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tailles Prioritaires") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ModernColors.barBackground, // Utilisez la même couleur
                    titleContentColor = ModernColors.selectedContent, // Et la même couleur de contenu
                )
                // Optionnel: Bouton Retour
                // navigationIcon = {
                //    IconButton(onClick = { navController.popBackStack() }) {
                //        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                //    }
                // }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp) // Marge générale pour le contenu
        ) {
            // Section pour les boutons de tri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.changerOrdreTriTailles(SortOrder.DESC) },
                    colors = ButtonDefaults.buttonColors( // Personnalisation des couleurs du bouton
                        containerColor = ModernButtonBackgroundColor,
                        contentColor = ModernButtonTextColor
                    )
                ) {
                    Text("Plus Urgent")
                }
                Button(
                    onClick = { viewModel.changerOrdreTriTailles(SortOrder.ASC) },
                    colors = ButtonDefaults.buttonColors( // Personnalisation des couleurs du bouton
                        containerColor = ModernButtonBackgroundColor,
                        contentColor = ModernButtonTextColor
                    )
                ) {
                    Text("Moins Urgent")
                }
            }

            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Affichage de la liste des tailles
            if (listeTaillesPrioritaires.isEmpty()) {
                Text(
                    text = "Aucune taille à afficher ou tous les chantiers sont à jour.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(listeTaillesPrioritaires) { item ->
                        TaillePrioritaireListItem( // Nouveau Composable pour les tailles
                            item = item,
                            dateFormat = dateFormat,
                            onClick = {
                                navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/${item.chantierId}")
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

// Nouveau Composable pour afficher un élément de la liste des tailles prioritaires
@Composable
fun TaillePrioritaireListItem(
    item: TaillePrioritaireUiItem, // Utilise la data class que nous avons définie
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.nomClient, style = MaterialTheme.typography.titleMedium)

            val tailleStatusText: String
            if (item.derniereTailleDate != null) {
                tailleStatusText = "Dernière taille : ${dateFormat.format(item.derniereTailleDate)}\n" +
                        "Jours écoulés : ${item.joursEcoules ?: "N/A"} - " +
                        "Cette année : ${item.nombreTaillesCetteAnnee}/2"
            } else {
                tailleStatusText = "Jamais taillé - Cette année : ${item.nombreTaillesCetteAnnee}/2"
            }
            Text(
                text = tailleStatusText,
                style = MaterialTheme.typography.bodySmall,
                color = item.urgencyColor // Applique la couleur d'urgence
            )
        }
        // On pourrait ajouter une icône ou un autre indicateur visuel ici si besoin
    }
}
