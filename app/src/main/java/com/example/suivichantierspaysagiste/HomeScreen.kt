package com.example.suivichantierspaysagiste

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChantierViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val interventionEnCours by viewModel.interventionEnCoursUi.collectAsStateWithLifecycle()

    // États pour les dialogues
    var showSaisieNotesDialog by remember { mutableStateOf(false) }
    var notesPourInterventionTerminee by remember { mutableStateOf("") }
    var showDemarrerInterventionDialog by remember { mutableStateOf(false) }
    var showAjouterChantierDialog by remember { mutableStateOf(false) }
    var showAjouterPrestationExtraDialog by remember { mutableStateOf(false) }


    // Données pour les aperçus
    val apercuTontes by viewModel.apercuTontesUrgentes.collectAsStateWithLifecycle()
    val totalTontesUrgentes by viewModel.nombreTotalTontesUrgentes.collectAsStateWithLifecycle()

    val apercuTailles by viewModel.apercuTaillesUrgentes.collectAsStateWithLifecycle()
    val totalTaillesUrgentes by viewModel.nombreTotalTaillesUrgentes.collectAsStateWithLifecycle()

    val apercuDesherbages by viewModel.apercuDesherbagesUrgents.collectAsStateWithLifecycle()
    val totalDesherbagesUrgents by viewModel.nombreTotalDesherbagesUrgents.collectAsStateWithLifecycle()

    val resumeFacturation by viewModel.resumeFacturationAccueil.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val currencyFormat = remember { DecimalFormat("#,##0.00 €") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1. Intervention en Cours
        interventionEnCours?.let {
            InterventionEnCoursAccueilCard(
                interventionEnCoursUi = it,
                onTerminerClick = {
                    notesPourInterventionTerminee = "" // Réinitialiser les notes
                    showSaisieNotesDialog = true
                },
                onCardClick = {
                    navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/${it.chantierId}")
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Actions Rapides
        Text("Actions Rapides", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Button(
                    onClick = { showAjouterChantierDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = "Ajouter chantier", modifier = Modifier.padding(end = 8.dp))
                    Text("Ajouter Chantier")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAjouterPrestationExtraDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PostAdd, contentDescription = "Prestation Extra", modifier = Modifier.padding(end = 8.dp))
                    Text("Prestation Extra")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDemarrerInterventionDialog = true },
                    enabled = interventionEnCours == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayCircleOutline, contentDescription = "Démarrer intervention", modifier = Modifier.padding(end = 8.dp))
                    Text("Démarrer Intervention")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // 3. Tâches Prioritaires
        Text("Tâches Prioritaires", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        // Tontes Urgentes
        ApercuSection(
            titre = "Tontes Urgentes ($totalTontesUrgentes)",
            items = apercuTontes,
            onVoirToutClick = { navController.navigate(ScreenDestinations.TONTES_PRIORITAIRES_ROUTE) },
            onItemClick = { chantierId ->
                navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/$chantierId")
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Tailles Urgentes
        ApercuSection(
            titre = "Tailles Urgentes ($totalTaillesUrgentes)",
            items = apercuTailles,
            onVoirToutClick = { navController.navigate(ScreenDestinations.TAILLES_PRIORITAIRES_ROUTE) },
            onItemClick = { chantierId ->
                navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/$chantierId")
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Désherbages Prochains/Urgents
        ApercuSection(
            titre = "Désherbages Prochains/Urgents ($totalDesherbagesUrgents)",
            items = apercuDesherbages,
            onVoirToutClick = { navController.navigate(ScreenDestinations.DESHERBAGES_PRIORITAIRES_ROUTE) },
            onItemClick = { chantierId ->
                navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/$chantierId")
            }
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 4. Facturation (Résumé)
        Text("Facturation Extras", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("À Facturer: ${resumeFacturation.nombrePrestationsAFacturer} prestation(s)", style = MaterialTheme.typography.bodyLarge)
                Text("Montant total estimé: ${currencyFormat.format(resumeFacturation.montantTotalEstimeAFacturer)}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { navController.navigate(ScreenDestinations.FACTURATION_EXTRAS_ROUTE) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Gérer la Facturation")
                }
            }
        }
    }

    // Dialogues
    if (showSaisieNotesDialog) {
        SaisieNotesInterventionDialogAccueil( // Version adaptée pour l'accueil
            notesInitiales = notesPourInterventionTerminee,
            onDismissRequest = {
                // Si l'utilisateur annule la saisie de notes, on termine quand même l'intervention sans notes.
                viewModel.terminerInterventionChrono(notesPourInterventionTerminee.ifBlank { null })
                showSaisieNotesDialog = false
            },
            onConfirm = { notesSaisies ->
                viewModel.terminerInterventionChrono(notesSaisies)
                showSaisieNotesDialog = false
            }
        )
    }

    if (showDemarrerInterventionDialog) {
        DemarrerInterventionDialog(
            viewModel = viewModel,
            onDismissRequest = { showDemarrerInterventionDialog = false },
            onConfirm = { chantierId, typeIntervention, chantierNom ->
                viewModel.demarrerInterventionChrono(chantierId, typeIntervention, chantierNom)
                showDemarrerInterventionDialog = false
            }
        )
    }

    if (showAjouterChantierDialog) {
        // Utilisation d'une version adaptée de AjouterChantierDialog
        AjouterChantierDialogAccueil(
            viewModel = viewModel,
            onDismissRequest = { showAjouterChantierDialog = false },
            onConfirm = { nom, adresse, tonteActive, tailleActive, desherbageActive, latitude, longitude ->
                viewModel.ajouterChantier(nom, adresse, tonteActive, tailleActive, desherbageActive, latitude, longitude)
                showAjouterChantierDialog = false
            }
        )
    }
    if (showAjouterPrestationExtraDialog) {
        // Utilisation d'une version adaptée de AddEditPrestationExtraDialog
        AddEditPrestationExtraDialogAccueil(
            prestationInitiale = null, // Pour ajout uniquement depuis l'accueil
            chantiersExistants = viewModel.tousLesChantiers.collectAsStateWithLifecycle().value,
            onDismiss = { showAjouterPrestationExtraDialog = false },
            onConfirm = { prestation ->
                viewModel.ajouterPrestationExtra(
                    chantierId = prestation.chantierId,
                    referenceChantierTexteLibre = prestation.referenceChantierTexteLibre,
                    description = prestation.description,
                    datePrestation = prestation.datePrestation,
                    montant = prestation.montant,
                    notes = prestation.notes
                )
                showAjouterPrestationExtraDialog = false
            },
            viewModel = viewModel // Pass chantierViewModel
        )
    }
}

@Composable
fun InterventionEnCoursAccueilCard(
    interventionEnCoursUi: InterventionEnCoursUi,
    onTerminerClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${interventionEnCoursUi.typeInterventionLisible} sur \"${interventionEnCoursUi.nomChantier}\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Temps écoulé: ${interventionEnCoursUi.dureeEcouleeFormattee}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Button(
                onClick = onTerminerClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.StopCircle, contentDescription = "Terminer")
                Spacer(Modifier.width(4.dp))
                Text("Terminer")
            }
        }
    }
}

@Composable
fun ApercuSection(
    titre: String,
    items: List<ApercuTachePrioritaireItem>,
    onVoirToutClick: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titre, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onVoirToutClick) {
                    Text("Voir tout")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text("Aucune tâche urgente pour le moment.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            } else {
                items.forEach { item ->
                    ApercuItem(item = item, onItemClick = { onItemClick(item.chantierId) })
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ApercuItem(
    item: ApercuTachePrioritaireItem,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = "Urgent",
            tint = item.urgencyColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(item.nomClient, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = item.urgencyColor)
        }
    }
}

@Composable
fun SaisieNotesInterventionDialogAccueil(
    notesInitiales: String,
    onDismissRequest: () -> Unit, // Appelé si on clique en dehors ou bouton Annuler
    onConfirm: (String?) -> Unit  // Appelé avec les notes (ou null si vide)
) {
    var notes by remember { mutableStateOf(notesInitiales) }

    AlertDialog(
        onDismissRequest = onDismissRequest, // Permet de fermer en cliquant à l'extérieur
        title = { Text("Ajouter des notes (optionnel)") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes pour l'intervention") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(notes.ifBlank { null }) }) {
                Text("Valider et Terminer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { // Bouton Annuler explicite
                Text("Terminer sans notes")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemarrerInterventionDialog(
    viewModel: ChantierViewModel,
    onDismissRequest: () -> Unit,
    onConfirm: (chantierId: Long, typeIntervention: String, chantierNom: String) -> Unit
) {
    val chantiers by viewModel.tousLesChantiers.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val filteredChantiers = remember(chantiers, searchQuery) {
        chantiers.filter { it.nomClient.contains(searchQuery, ignoreCase = true) }
    }
    var selectedChantier by remember { mutableStateOf<Chantier?>(null) }
    var selectedTypeIntervention by remember { mutableStateOf<String?>(null) }
    val typesIntervention = listOf("Tonte de pelouse", "Taille de haie", "Désherbage")
    var expandedChantierDropdown by remember { mutableStateOf(false) }
    var expandedTypeDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).heightIn(max = 500.dp), // Limiter la hauteur
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Démarrer une Intervention", style = MaterialTheme.typography.titleLarge)

                // Sélection Chantier
                ExposedDropdownMenuBox(
                    expanded = expandedChantierDropdown,
                    onExpandedChange = { expandedChantierDropdown = !expandedChantierDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedChantier?.nomClient ?: (if(searchQuery.isNotEmpty() && filteredChantiers.isEmpty()) "Aucun chantier trouvé" else "Choisir un chantier..."),
                        onValueChange = { searchQuery = it; if(!expandedChantierDropdown) expandedChantierDropdown = true },
                        label = { Text("Chantier *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedChantierDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = selectedChantier != null && searchQuery != selectedChantier?.nomClient // Allow typing to search
                    )
                    ExposedDropdownMenu(
                        expanded = expandedChantierDropdown,
                        onDismissRequest = { expandedChantierDropdown = false },
                        modifier = Modifier.heightIn(max=200.dp) // Limiter hauteur dropdown
                    ) {
                        if (filteredChantiers.isEmpty()){
                            Text("Aucun chantier ne correspond à '${searchQuery}'", modifier = Modifier.padding(12.dp))
                        }
                        filteredChantiers.forEach { chantier ->
                            DropdownMenuItem(
                                text = { Text(chantier.nomClient) },
                                onClick = {
                                    selectedChantier = chantier
                                    searchQuery = chantier.nomClient // Update search query to reflect selection
                                    expandedChantierDropdown = false
                                }
                            )
                        }
                    }
                }


                // Sélection Type d'Intervention
                ExposedDropdownMenuBox(
                    expanded = expandedTypeDropdown,
                    onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedTypeIntervention ?: "Choisir un type...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type d'intervention *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeDropdown,
                        onDismissRequest = { expandedTypeDropdown = false }
                    ) {
                        typesIntervention.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedTypeIntervention = type
                                    expandedTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Annuler") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedChantier != null && selectedTypeIntervention != null) {
                                onConfirm(selectedChantier!!.id, selectedTypeIntervention!!, selectedChantier!!.nomClient)
                            }
                        },
                        enabled = selectedChantier != null && selectedTypeIntervention != null
                    ) { Text("Démarrer") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjouterChantierDialogAccueil(
    viewModel: ChantierViewModel,
    onDismissRequest: () -> Unit,
    onConfirm: (nom: String, adresse: String?, tonteActive: Boolean, tailleActive: Boolean, desherbageActive: Boolean, latitude: Double?, longitude: Double?) -> Unit
) {
    // Cette fonction est une copie adaptée de AjouterChantierDialog de ChantierScreen.kt
    // pour être utilisée ici. Idéalement, ce dialogue serait un composant commun.
    var nomClient by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var tonteActive by remember { mutableStateOf(true) }
    var tailleActive by remember { mutableStateOf(true) }
    var desherbageActive by remember { mutableStateOf(true) }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var geocodingInProgress by remember { mutableStateOf(false) }
    var geocodingResultMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Ajouter un nouveau chantier", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = nomClient,
                    onValueChange = { nomClient = it },
                    label = { Text("Nom du client / Chantier *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = adresse,
                        onValueChange = { adresse = it },
                        label = { Text("Adresse (pour géocodage)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                    IconButton(
                        onClick = {
                            if (adresse.isNotBlank()) {
                                focusManager.clearFocus()
                                geocodingInProgress = true
                                geocodingResultMessage = "Recherche..."
                                viewModel.geocodeAdresse(adresse) { latLng ->
                                    geocodingInProgress = false
                                    if (latLng != null) {
                                        latitude = latLng.latitude
                                        longitude = latLng.longitude
                                        geocodingResultMessage = "Coordonnées trouvées !"
                                    } else {
                                        latitude = null
                                        longitude = null
                                        geocodingResultMessage = "Adresse non trouvée."
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Veuillez entrer une adresse.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !geocodingInProgress && adresse.isNotBlank()
                    ) {
                        if (geocodingInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Filled.MyLocation, contentDescription = "Obtenir coordonnées")
                        }
                    }
                }
                geocodingResultMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (latitude != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
                if (latitude != null && longitude != null) {
                    Text("Lat: %.4f, Lng: %.4f".format(latitude, longitude), style = MaterialTheme.typography.bodySmall)
                }

                ServiceActivationRowAccueil(label = "Suivi des Tontes Actif", checked = tonteActive, onCheckedChange = { tonteActive = it })
                ServiceActivationRowAccueil(label = "Suivi des Tailles Actif", checked = tailleActive, onCheckedChange = { tailleActive = it })
                ServiceActivationRowAccueil(label = "Suivi Désherbage Actif", checked = desherbageActive, onCheckedChange = { desherbageActive = it })

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nomClient.isNotBlank()) {
                                onConfirm(nomClient, adresse.ifBlank { null }, tonteActive, tailleActive, desherbageActive, latitude, longitude)
                            }
                        },
                        enabled = nomClient.isNotBlank() && !geocodingInProgress
                    ) { Text("Ajouter") }
                }
            }
        }
    }
}

@Composable
fun ServiceActivationRowAccueil(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // Copie de ServiceActivationRow de ChantierScreen.kt
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPrestationExtraDialogAccueil(
    prestationInitiale: PrestationHorsContrat?, // Sera null pour l'ajout depuis l'accueil
    chantiersExistants: List<Chantier>,
    onDismiss: () -> Unit,
    onConfirm: (PrestationHorsContrat) -> Unit,
    viewModel: ChantierViewModel // Ajout du ViewModel
) {
    // Copie adaptée de AddEditPrestationExtraDialog de FacturationExtrasScreen.kt
    val context = LocalContext.current
    val isEditing = prestationInitiale != null
    val title = if (isEditing) "Modifier Prestation Extra" else "Ajouter Prestation Extra"

    var selectedChantierId by remember { mutableStateOf(prestationInitiale?.chantierId) }
    var referenceChantierTexteLibre by remember(selectedChantierId) {
        mutableStateOf(if (selectedChantierId == null) prestationInitiale?.referenceChantierTexteLibre ?: "" else "")
    }
    var description by remember { mutableStateOf(prestationInitiale?.description ?: "") }
    var datePrestation by remember { mutableStateOf(prestationInitiale?.datePrestation ?: Date()) }
    var montantString by remember { mutableStateOf(prestationInitiale?.montant?.let { DecimalFormat("#.##").format(it).replace('.', ',') } ?: "") }
    var notes by remember { mutableStateOf(prestationInitiale?.notes ?: "") }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = datePrestation.time,
        selectableDates = NoFutureDatesSelectableDates // Assurez-vous que NoFutureDatesSelectableDates est accessible
    )

    var chantierDropdownExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

                OutlinedTextField(
                    value = referenceChantierTexteLibre,
                    onValueChange = { referenceChantierTexteLibre = it },
                    label = { Text("Client / Référence Chantier") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedChantierId == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )

                if (chantiersExistants.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = chantierDropdownExpanded,
                        onExpandedChange = { chantierDropdownExpanded = !chantierDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = chantiersExistants.find { it.id == selectedChantierId }?.nomClient ?: "Ou choisir un chantier existant...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Chantier Existant (Optionnel)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chantierDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                .clickable(enabled = !chantierDropdownExpanded) { chantierDropdownExpanded = true }
                        )
                        ExposedDropdownMenu(
                            expanded = chantierDropdownExpanded,
                            onDismissRequest = { chantierDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Aucun (utiliser réf. manuelle)", fontStyle = FontStyle.Italic) },
                                onClick = {
                                    selectedChantierId = null
                                    chantierDropdownExpanded = false
                                }
                            )
                            chantiersExistants.forEach { chantier ->
                                DropdownMenuItem(
                                    text = { Text(chantier.nomClient) },
                                    onClick = {
                                        selectedChantierId = chantier.id
                                        referenceChantierTexteLibre = "" // Effacer la réf manuelle si un chantier est choisi
                                        chantierDropdownExpanded = false
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description de la prestation *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = "Date", modifier = Modifier.padding(end = 8.dp))
                    Text("Date Prestation: ${dateFormat.format(datePrestation)}")
                }
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { datePrestation = Date(it) }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
                    ) { DatePicker(state = datePickerState) }
                }

                OutlinedTextField(
                    value = montantString,
                    onValueChange = { montantString = it },
                    label = { Text("Montant TTC (€) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.EuroSymbol, contentDescription = "Euro") }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val montantDouble = montantString.replace(',', '.').toDoubleOrNull()
                            if (selectedChantierId == null && referenceChantierTexteLibre.isBlank()) {
                                Toast.makeText(context, "Veuillez sélectionner un chantier ou saisir une référence.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (description.isBlank()) {
                                Toast.makeText(context, "La description est requise.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (montantDouble == null || montantDouble <= 0) {
                                Toast.makeText(context, "Veuillez saisir un montant valide.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val finalPrestation = prestationInitiale?.copy(
                                chantierId = selectedChantierId,
                                referenceChantierTexteLibre = if (selectedChantierId != null) null else referenceChantierTexteLibre.ifBlank { null },
                                description = description,
                                datePrestation = datePrestation,
                                montant = montantDouble,
                                notes = notes.ifBlank { null }
                            ) ?: PrestationHorsContrat(
                                chantierId = selectedChantierId,
                                referenceChantierTexteLibre = if (selectedChantierId != null) null else referenceChantierTexteLibre.ifBlank { null },
                                description = description,
                                datePrestation = datePrestation,
                                montant = montantDouble,
                                notes = notes.ifBlank { null },
                                statut = StatutFacturationExtras.A_FACTURER.name // Statut par défaut pour un nouvel ajout
                            )
                            onConfirm(finalPrestation)
                        }
                    ) { Text(if (isEditing) "Modifier" else "Ajouter") }
                }
            }
        }
    }
}
