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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesherbagesPrioritairesScreen(
    viewModel: ChantierViewModel,
    navController: NavHostController
) {
    val listeDesherbagesPrioritaires by viewModel.desherbagesPrioritaires.collectAsStateWithLifecycle()
    // val sortOrder by viewModel.desherbagesSortOrder.collectAsStateWithLifecycle() // Pour styler les boutons si besoin

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Désherbages Prioritaires") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ModernColors.barBackground, // Utiliser les couleurs modernes définies
                    titleContentColor = ModernColors.selectedContent,
                    navigationIconContentColor = ModernColors.selectedContent // Si vous ajoutez une icône de navigation
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
                    onClick = { viewModel.changerOrdreTriDesherbages(SortOrder.ASC) }, // ASC pour plus urgent en premier (date la plus proche)
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ModernButtonBackgroundColor, // Défini dans TaillesPrioritairesScreen ou un fichier commun
                        contentColor = ModernButtonTextColor
                    )
                ) {
                    Text("Plus Urgent")
                }
                Button(
                    onClick = { viewModel.changerOrdreTriDesherbages(SortOrder.DESC) }, // DESC pour moins urgent en premier
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

            if (listeDesherbagesPrioritaires.isEmpty()) {
                Text(
                    text = "Aucun désherbage prioritaire à afficher ou toutes les planifications sont à jour.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(listeDesherbagesPrioritaires, key = { it.chantierId.toString() + "-" + (it.planificationId ?: "null") }) { item ->
                        DesherbagePrioritaireListItem(
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
fun DesherbagePrioritaireListItem(
    item: DesherbagePrioritaireUiItem, // Utilise la data class définie dans ChantierViewModel
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
            if (item.prochaineDatePlanifiee != null) {
                val joursRestantsText = when {
                    item.joursAvantEcheance == null -> "N/A"
                    item.joursAvantEcheance < 0 -> "En retard de ${-item.joursAvantEcheance}j"
                    item.joursAvantEcheance == 0L -> "Aujourd'hui"
                    else -> "Dans ${item.joursAvantEcheance}j"
                }
                Text(
                    "Prochain désherbage : ${dateFormat.format(item.prochaineDatePlanifiee)} ($joursRestantsText)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = item.urgencyColor
                )
            } else {
                Text(
                    "Aucune planification en attente", // Devrait être filtré par le ViewModel mais sécurité
                    style = MaterialTheme.typography.bodyMedium,
                    color = item.urgencyColor
                )
            }
        }
        // On pourrait ajouter une action rapide ici, comme "Marquer comme fait" si pertinent
        // Mais cela ouvrirait probablement le détail du chantier pour plus de contexte.
    }
}