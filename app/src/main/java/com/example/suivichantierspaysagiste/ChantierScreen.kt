package com.example.suivichantierspaysagiste

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

object ModernColorsScreen {
    val barBackground = Color(0xFF004D40)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ModernColorsScreen.barBackground,
                    titleContentColor = ModernColorsScreen.contentColor,
                    actionIconContentColor = ModernColorsScreen.contentColor
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
                                // Pas besoin de viewModel.loadChantierById ici, car ChantierDetailScreen le fait dans LaunchedEffect
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
                onConfirm = { nom, adresse, tonteActive, tailleActive, desherbageActive -> // Ajout de desherbageActive
                    viewModel.ajouterChantier(nom, adresse, tonteActive, tailleActive, desherbageActive) // Appel au ViewModel modifié
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
            .padding(vertical = 8.dp, horizontal = 16.dp) // Ajusté pour correspondre au padding global
    ) {
        Text(text = chantier.nomClient, style = MaterialTheme.typography.titleMedium)
        if (chantier.adresse != null && chantier.adresse!!.isNotBlank()) {
            Text(text = chantier.adresse!!, style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            if (chantier.serviceTonteActive) {
                ServiceChip("Tonte", MaterialTheme.colorScheme.primary)
            }
            if (chantier.serviceTailleActive) {
                ServiceChip("Taille", MaterialTheme.colorScheme.secondary)
            }
            // NOUVEAU: Afficher si le service de désherbage est actif
            if (chantier.serviceDesherbageActive) {
                ServiceChip("Désherbage", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun ServiceChip(label: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        tonalElevation = 1.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjouterChantierDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String, String?, Boolean, Boolean, Boolean) -> Unit // Ajout de Boolean pour desherbageActive
) {
    var nomClient by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var tonteActive by remember { mutableStateOf(true) }
    var tailleActive by remember { mutableStateOf(true) }
    var desherbageActive by remember { mutableStateOf(true) } // NOUVEAU état pour le désherbage

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Permet le défilement
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ajouter un nouveau chantier", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = nomClient,
                    onValueChange = { nomClient = it },
                    label = { Text("Nom du client / Chantier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it },
                    label = { Text("Adresse (optionnel)") },
                    modifier = Modifier.fillMaxWidth()
                )

                ServiceActivationRow(
                    label = "Suivi des Tontes Actif",
                    checked = tonteActive,
                    onCheckedChange = { tonteActive = it }
                )
                ServiceActivationRow(
                    label = "Suivi des Tailles Actif",
                    checked = tailleActive,
                    onCheckedChange = { tailleActive = it }
                )
                // NOUVEAU: Checkbox pour le service de désherbage
                ServiceActivationRow(
                    label = "Suivi Désherbage Actif",
                    checked = desherbageActive,
                    onCheckedChange = { desherbageActive = it }
                )

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
                                onConfirm(nomClient, adresse.ifBlank { null }, tonteActive, tailleActive, desherbageActive)
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

// Composable réutilisable pour les lignes d'activation de service
@Composable
fun ServiceActivationRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) } // Rend toute la ligne cliquable
            .padding(vertical = 4.dp) // Un peu de padding vertical
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp) // Espace entre checkbox et texte
        )
    }
}