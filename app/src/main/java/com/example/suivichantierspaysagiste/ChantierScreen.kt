package com.example.suivichantierspaysagiste

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import pour Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

// Réutiliser la définition de ModernColors (ou la définir dans un fichier Utils.kt et l'importer)
// Pour cet exemple, je la remets ici, mais idéalement elle serait dans un fichier partagé.
object ModernColorsScreen { // Nom différent pour éviter conflit si dans le même scope que MainActivity
    val barBackground = Color(0xFF004D40) // Vert sarcelle foncé
    val contentColor = Color.White
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChantierListScreen(
    viewModel: ChantierViewModel,
    navController: NavHostController
) {
    val chantiers by viewModel.tousLesChantiers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Chantiers") },
                colors = TopAppBarDefaults.topAppBarColors( // Personnalisation des couleurs de la TopAppBar
                    containerColor = ModernColorsScreen.barBackground,
                    titleContentColor = ModernColorsScreen.contentColor,
                    actionIconContentColor = ModernColorsScreen.contentColor // Pour les icônes d'action si vous en ajoutez
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un chantier")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                label = { Text("Rechercher un chantier...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Icône de recherche") },
                singleLine = true
            )

            if (chantiers.isEmpty() && searchQuery.isNotBlank()) {
                Text(
                    text = "Aucun chantier ne correspond à votre recherche.",
                    modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
                )
            } else if (chantiers.isEmpty() && searchQuery.isBlank()) {
                Text(
                    text = "Aucun chantier pour le moment. Cliquez sur le bouton '+' pour en ajouter un.",
                    modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
                )
            }
            else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chantiers) { chantier ->
                        ChantierItem(
                            chantier = chantier,
                            onClick = {
                                navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/${chantier.id}")
                            }
                        )
                        Divider()
                    }
                }
            }
        }

        if (showDialog) {
            AjouterChantierDialog(
                onDismissRequest = { showDialog = false },
                // MODIFIÉ: onConfirm prend maintenant les booléens pour les services
                onConfirm = { nom, adresse, tonteActive, tailleActive ->
                    viewModel.ajouterChantier(nom, adresse, tonteActive, tailleActive) // Appel au ViewModel modifié
                    viewModel.onSearchQueryChanged("")
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ChantierItem(chantier: Chantier, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(text = chantier.nomClient, style = MaterialTheme.typography.titleMedium)
        if (chantier.adresse != null && chantier.adresse!!.isNotBlank()) {
            Text(text = chantier.adresse!!, style = MaterialTheme.typography.bodySmall)
        }
        // Optionnel: Afficher ici si les services sont actifs pour ce chantier
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (chantier.serviceTonteActive) {
                Text("Tonte", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (chantier.serviceTailleActive) {
                Text("Taille", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjouterChantierDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, String?, Boolean, Boolean) -> Unit
) {
    var nomClient by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var tonteActive by remember { mutableStateOf(true) } // Par défaut à true
    var tailleActive by remember { mutableStateOf(true) } // Par défaut à true

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ajouter un nouveau chantier", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = nomClient,
                    onValueChange = { nomClient = it },
                    label = { Text("Nom du client / Chantier") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it },
                    label = { Text("Adresse (optionnel)") }
                )
                // NOUVEAU: Checkbox pour le service de tonte
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = tonteActive,
                        onCheckedChange = { tonteActive = it }
                    )
                    Text(
                        text = "Suivi des Tontes Actif",
                        modifier = Modifier.clickable { tonteActive = !tonteActive }.padding(start = 4.dp)
                    )
                }

                // NOUVEAU: Checkbox pour le service de taille
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = tailleActive,
                        onCheckedChange = { tailleActive = it }
                    )
                    Text(
                        text = "Suivi des Tailles Actif",
                        modifier = Modifier.clickable { tailleActive = !tailleActive }.padding(start = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nomClient.isNotBlank()) {
                                onConfirm(nomClient, adresse.ifBlank { null }, tonteActive, tailleActive)
                            }
                        },
                        enabled = nomClient.isNotBlank()
                    ) {
                        Text("Ajouter")
                    }
                }
            }
        }
    }
}