package com.example.suivichantierspaysagiste

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Assurez-vous que UrgencyUtils.kt (ou équivalent) est accessible avec getUrgencyColor, OrangeCustom etc.
// et que les fonctions dans UrgencyUtils.kt sont correctement définies.

@OptIn(ExperimentalMaterial3Api::class)
private object NoFutureDatesSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        // Permet de sélectionner aujourd'hui et les dates passées.
        // On compare le début du jour pour éviter les problèmes de fuseau horaire / heure exacte.
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Pour la date sélectionnée, on la normalise aussi au début du jour
        val selectedCal = Calendar.getInstance().apply { timeInMillis = utcTimeMillis }
        selectedCal.set(Calendar.HOUR_OF_DAY, 0)
        selectedCal.set(Calendar.MINUTE, 0)
        selectedCal.set(Calendar.SECOND, 0)
        selectedCal.set(Calendar.MILLISECOND, 0)

        // Permettre la sélection si la date sélectionnée est aujourd'hui ou avant.
        return selectedCal.timeInMillis <= System.currentTimeMillis()
    }

    override fun isSelectableYear(year: Int): Boolean {
        // Permet de sélectionner l'année en cours et les années passées.
        return year <= Calendar.getInstance().get(Calendar.YEAR)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChantierDetailScreen(
    chantierId: Long,
    viewModel: ChantierViewModel,
    navController: NavHostController
) {
    LaunchedEffect(key1 = chantierId) {
        viewModel.loadChantierById(chantierId)
    }

    val chantier by viewModel.selectedChantier.collectAsStateWithLifecycle()
    val derniereTonte by viewModel.derniereTonte.collectAsStateWithLifecycle()
    val nombreTotalTontes by viewModel.nombreTotalTontes.collectAsStateWithLifecycle()
    val derniereTaille by viewModel.derniereTaille.collectAsStateWithLifecycle()
    val nombreTotalTailles by viewModel.nombreTotalTailles.collectAsStateWithLifecycle()
    val nombreTaillesCetteAnnee by viewModel.nombreTaillesCetteAnnee.collectAsStateWithLifecycle()
    val interventions by viewModel.interventionsDuChantier.collectAsStateWithLifecycle()

    val tonteUrgencyColor by viewModel.selectedChantierTonteUrgencyColor.collectAsStateWithLifecycle()
    val tailleUrgencyColor by viewModel.selectedChantierTailleUrgencyColor.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var interventionASupprimer by remember { mutableStateOf<Intervention?>(null) }
    var showTonteDatePickerDialog by remember { mutableStateOf(false) }
    val tonteDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)
    var showTailleDatePickerDialog by remember { mutableStateOf(false) }
    val tailleDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)

    // Nouveaux états pour la saisie des notes
    var interventionTypeForNotes by remember { mutableStateOf<String?>(null) } // "Tonte" ou "Taille"
    var selectedDateForNotes by remember { mutableStateOf<Date?>(null) }
    var currentInterventionNotes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (chantier == null) {
            Text("Chargement du chantier...")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chantier!!.nomClient,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier le chantier")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer le chantier", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            chantier!!.adresse?.let { adresse ->
                if (adresse.isNotBlank()) {
                    Text(
                        text = "Adresse: $adresse",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section Tontes
            if (chantier!!.serviceTonteActive) {
                Text("Suivi des Tontes :", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (derniereTonte == null) {
                    Text("Aucune tonte enregistrée.", color = tonteUrgencyColor)
                } else {
                    val dateFormatee = dateFormat.format(derniereTonte!!.dateIntervention)
                    val joursEcoules = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - derniereTonte!!.dateIntervention.time)
                    Text("Dernière tonte : $dateFormatee (il y a $joursEcoules jour(s))", color = tonteUrgencyColor)
                }
                Text("Nombre total de tontes : $nombreTotalTontes")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        interventionTypeForNotes = null
                        showTonteDatePickerDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = interventionTypeForNotes != "Tonte"
                ) { Text("Enregistrer une Tonte") }

                if (interventionTypeForNotes == "Tonte" && selectedDateForNotes != null) {
                    InterventionNotesInputSection(
                        selectedDate = selectedDateForNotes!!,
                        notes = currentInterventionNotes,
                        onNotesChange = { currentInterventionNotes = it },
                        onConfirm = {
                            viewModel.ajouterTonte(chantierId, selectedDateForNotes!!, currentInterventionNotes.ifBlank { null })
                            interventionTypeForNotes = null
                            selectedDateForNotes = null
                            currentInterventionNotes = ""
                        },
                        onCancel = {
                            interventionTypeForNotes = null
                            selectedDateForNotes = null
                            currentInterventionNotes = ""
                        },
                        dateFormat = dateFormat
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section Tailles
            if (chantier!!.serviceTailleActive) {
                Text("Suivi des Tailles de Haie :", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (derniereTaille == null) {
                    Text("Aucune taille enregistrée.", color = tailleUrgencyColor)
                } else {
                    val dateFormatee = dateFormat.format(derniereTaille!!.dateIntervention)
                    val joursEcoules = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - derniereTaille!!.dateIntervention.time)
                    Text("Dernière taille : $dateFormatee (il y a $joursEcoules jour(s))", color = tailleUrgencyColor)
                }
                Text("Nombre total de tailles : $nombreTotalTailles")
                Text("Tailles cette année : $nombreTaillesCetteAnnee / 2")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        interventionTypeForNotes = null
                        showTailleDatePickerDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = interventionTypeForNotes != "Taille"
                ) { Text("Enregistrer une Taille") }

                if (interventionTypeForNotes == "Taille" && selectedDateForNotes != null) {
                    InterventionNotesInputSection(
                        selectedDate = selectedDateForNotes!!,
                        notes = currentInterventionNotes,
                        onNotesChange = { currentInterventionNotes = it },
                        onConfirm = {
                            viewModel.ajouterTailleHaie(chantierId, selectedDateForNotes!!, currentInterventionNotes.ifBlank { null })
                            interventionTypeForNotes = null
                            selectedDateForNotes = null
                            currentInterventionNotes = ""
                        },
                        onCancel = {
                            interventionTypeForNotes = null
                            selectedDateForNotes = null
                            currentInterventionNotes = ""
                        },
                        dateFormat = dateFormat
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (!chantier!!.serviceTonteActive && !chantier!!.serviceTailleActive) {
                Text("Aucun service de suivi (tonte ou taille) n'est actif pour ce chantier.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Historique des Interventions :", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (interventions.isEmpty()) {
                Text("Aucune intervention enregistrée pour ce chantier.")
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    interventions.forEach { intervention ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${intervention.typeIntervention} - ${dateFormat.format(intervention.dateIntervention)}")
                                if (!intervention.notes.isNullOrBlank()) {
                                    Text(
                                        "Notes: ${intervention.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { interventionASupprimer = intervention }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer cette intervention", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }

    // ---- DIALOGUES ----
    if (interventionASupprimer != null) {
        val interventionPourDialogue = interventionASupprimer!!
        AlertDialog(
            onDismissRequest = { interventionASupprimer = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Êtes-vous sûr de vouloir supprimer l'intervention : ${interventionPourDialogue.typeIntervention} du ${dateFormat.format(interventionPourDialogue.dateIntervention)} ?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteIntervention(interventionPourDialogue)
                        interventionASupprimer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { interventionASupprimer = null }) { Text("Annuler") }
            }
        )
    }

    if (showEditDialog && chantier != null) {
        EditChantierDialog(
            chantier = chantier!!,
            onDismissRequest = { showEditDialog = false },
            onConfirm = { chantierModifie ->
                viewModel.updateChantier(chantierModifie)
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog && chantier != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirmer la suppression") },
            text = { Text("Êtes-vous sûr de vouloir supprimer le chantier \"${chantier!!.nomClient}\" et toutes ses interventions associées ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChantier(chantier!!)
                        showDeleteConfirmDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Annuler") }
            }
        )
    }

    // DatePicker pour les TONTES
    if (showTonteDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showTonteDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showTonteDatePickerDialog = false
                    tonteDatePickerState.selectedDateMillis?.let { millis ->
                        selectedDateForNotes = Date(millis)
                        interventionTypeForNotes = "Tonte"
                        currentInterventionNotes = ""
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTonteDatePickerDialog = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = tonteDatePickerState) }
    }

    // DatePicker pour les TAILLES
    if (showTailleDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showTailleDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showTailleDatePickerDialog = false
                    tailleDatePickerState.selectedDateMillis?.let { millis ->
                        selectedDateForNotes = Date(millis)
                        interventionTypeForNotes = "Taille"
                        currentInterventionNotes = ""
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTailleDatePickerDialog = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = tailleDatePickerState) }
    }
}

@Composable
fun InterventionNotesInputSection(
    selectedDate: Date,
    notes: String,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    dateFormat: SimpleDateFormat
) {
    // Définition d'une couleur vert pâle personnalisée
    val paleGreen = Color(0xFFE8F5E9) // Un vert très clair (Material Design Green 50)
    // Couleur de contenu qui contraste bien avec le vert pâle (un vert foncé)
    val darkGreenContent = Color(0xFF1B5E20) // Material Design Green 900

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = paleGreen, // <<<<------ COULEUR VERT PÂLE APPLIQUÉE
            contentColor = darkGreenContent // Couleur de contenu pour contraster
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Ajouter des notes pour l'intervention du : ${dateFormat.format(selectedDate)}",
                style = MaterialTheme.typography.titleSmall
                // La couleur du texte sera darkGreenContent grâce au contentColor de la Card
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                // Les couleurs du TextField s'adapteront généralement bien,
                // mais peuvent être personnalisées via le paramètre `colors` de OutlinedTextField si besoin.
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("Annuler") // La couleur de ce texte sera darkGreenContent
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm
                    // Les couleurs du bouton peuvent aussi être personnalisées via ButtonDefaults.buttonColors
                    // si le contraste n'est pas bon avec darkGreenContent pour le texte du bouton.
                ) {
                    Text("Valider Intervention")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChantierDialog(
    chantier: Chantier,
    onDismissRequest: () -> Unit,
    onConfirm: (Chantier) -> Unit
) {
    var nomClient by remember(chantier.nomClient) { mutableStateOf(chantier.nomClient) }
    var adresse by remember(chantier.adresse) { mutableStateOf(chantier.adresse ?: "") }
    var tonteActive by remember(chantier.serviceTonteActive) { mutableStateOf(chantier.serviceTonteActive) }
    var tailleActive by remember(chantier.serviceTailleActive) { mutableStateOf(chantier.serviceTailleActive) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Modifier le chantier", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = nomClient, onValueChange = { nomClient = it }, label = { Text("Nom du client / Chantier") }, singleLine = true)
                OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse (optionnel)") })

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = tonteActive, onCheckedChange = { tonteActive = it })
                    Text("Suivi des Tontes Actif", modifier = Modifier.clickable { tonteActive = !tonteActive }.padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = tailleActive, onCheckedChange = { tailleActive = it })
                    Text("Suivi des Tailles Actif", modifier = Modifier.clickable { tailleActive = !tailleActive }.padding(start = 4.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nomClient.isNotBlank()) {
                                val chantierModifie = chantier.copy(
                                    nomClient = nomClient,
                                    adresse = adresse.ifBlank { null },
                                    serviceTonteActive = tonteActive,
                                    serviceTailleActive = tailleActive
                                )
                                onConfirm(chantierModifie)
                            }
                        },
                        enabled = nomClient.isNotBlank()
                    ) { Text("Enregistrer") }
                }
            }
        }
    }
}