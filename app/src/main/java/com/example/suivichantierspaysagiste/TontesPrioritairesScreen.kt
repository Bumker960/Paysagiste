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
// import androidx.compose.ui.graphics.Color // Pas besoin si ModernColors et ModernButtonBackgroundColor sont accessibles
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Locale
// import androidx.compose.material.icons.Icons // Si vous ajoutez des icônes
// import androidx.compose.material.icons.filled.ArrowBack

// Assurez-vous que ModernColors (pour la TopAppBar) et ModernButtonBackgroundColor/TextColor
// sont accessibles depuis ce fichier (par exemple, définis dans UrgencyUtils.kt ou importés).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TontesPrioritairesScreen(
    viewModel: ChantierViewModel,
    navController: NavHostController
) {
    val listeTontesPrioritaires by viewModel.tontesPrioritaires.collectAsStateWithLifecycle()
    // val sortOrder by viewModel.tontesSortOrder.collectAsStateWithLifecycle() // Décommentez si vous l'utilisez

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tontes Prioritaires") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ModernColors.barBackground,
                    titleContentColor = ModernColors.selectedContent,
                    navigationIconContentColor = ModernColors.selectedContent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.changerOrdreTriTontes(SortOrder.DESC) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ModernButtonBackgroundColor,
                        contentColor = ModernButtonTextColor
                    )
                ) {
                    Text("Plus Urgent")
                }
                Button(
                    onClick = { viewModel.changerOrdreTriTontes(SortOrder.ASC) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ModernButtonBackgroundColor,
                        contentColor = ModernButtonTextColor
                    )
                ) {
                    Text("Moins Urgent")
                }
            }

            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            if (listeTontesPrioritaires.isEmpty()) {
                Text(
                    text = "Aucune tonte à afficher ou tous les chantiers sont à jour.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(listeTontesPrioritaires) { item -> // item est de type TontePrioritaireItem
                        TontePrioritaireListItem(
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

@Composable
fun TontePrioritaireListItem(
    item: TontePrioritaireItem, // item contient maintenant item.urgencyColor
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
            if (item.derniereTonteDate != null) {
                Text(
                    "Dernière tonte : ${dateFormat.format(item.derniereTonteDate)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Jours écoulés : ${item.joursEcoules ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = item.urgencyColor // MODIFIÉ ICI: Utilise la couleur de l'item
                )
            } else {
                Text("Jamais tondu", style = MaterialTheme.typography.bodySmall)
                Text(
                    "À faire (priorité max)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = item.urgencyColor // MODIFIÉ ICI: Utilise la couleur de l'item
                )
            }
        }
    }
}
