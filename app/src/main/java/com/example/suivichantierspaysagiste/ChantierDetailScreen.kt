package com.example.suivichantierspaysagiste

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Pas utilisé directement ici, mais peut rester si d'autres parties l'utilisent
import androidx.compose.foundation.lazy.items // Pas utilisé directement ici
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
import androidx.compose.ui.window.Dialog // Pas utilisé directement ici, AlertDialog l'est
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
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val selectedCal = Calendar.getInstance().apply { timeInMillis = utcTimeMillis }
        selectedCal.set(Calendar.HOUR_OF_DAY, 0)
        selectedCal.set(Calendar.MINUTE, 0)
        selectedCal.set(Calendar.SECOND, 0)
        selectedCal.set(Calendar.MILLISECOND, 0)
        return selectedCal.timeInMillis <= System.currentTimeMillis() // Permet aujourd'hui
    }

    override fun isSelectableYear(year: Int): Boolean {
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
    var showEditDialog by remember { mutableStateOf(false) } // Pour modifier le chantier
    var showDeleteConfirmDialog by remember { mutableStateOf(false) } // Pour supprimer le chantier
    var interventionASupprimer by remember { mutableStateOf<Intervention?>(null) }
    var showTonteDatePickerDialog by remember { mutableStateOf(false) }
    val tonteDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)
    var showTailleDatePickerDialog by remember { mutableStateOf(false) }
    val tailleDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)

    // Nouveaux états pour la saisie des notes (ajout d'intervention)
    var interventionTypeForNotes by remember { mutableStateOf<String?>(null) } // "Tonte" ou "Taille"
    var selectedDateForNotes by remember { mutableStateOf<Date?>(null) }
    var currentInterventionNotes by remember { mutableStateOf("") }

    // NOUVEAUX ÉTATS pour la modification de note d'une intervention existante
    var interventionAModifierNote by remember { mutableStateOf<Intervention?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (chantier == null) {
            Text("Chargement du chantier...")
        } else {
            // ... (Affichage du nom du chantier, adresse, boutons Edit/Delete Chantier - INCHANGÉ) ...
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

            // Section Tontes (INCHANGÉ sauf si vous voulez ajouter des notes ici aussi)
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
                        interventionTypeForNotes = null // Réinitialiser au cas où
                        showTonteDatePickerDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = interventionTypeForNotes != "Tonte" // Désactiver si déjà en saisie de notes pour une tonte
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


            // Section Tailles (INCHANGÉ sauf si vous voulez ajouter des notes ici aussi)
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
                        interventionTypeForNotes = null // Réinitialiser
                        showTailleDatePickerDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = interventionTypeForNotes != "Taille" // Désactiver si déjà en saisie de notes pour une taille
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top // Changé pour mieux aligner les icônes avec le haut du texte
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
                            // MODIFICATION ICI: Ajout de l'icône Modifier la note
                            Row { // Enveloppe les icônes dans une Row pour les mettre côte à côte
                                IconButton(
                                    onClick = {
                                        interventionAModifierNote = intervention
                                        showEditNoteDialog = true
                                    },
                                    modifier = Modifier.size(36.dp) // Réduire un peu la taille pour mieux s'aligner
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Modifier la note")
                                }
                                IconButton(
                                    onClick = { interventionASupprimer = intervention },
                                    modifier = Modifier.size(36.dp) // Réduire un peu la taille
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer cette intervention", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }

    // ---- DIALOGUES ----

    // Dialogue pour supprimer une intervention (INCHANGÉ)
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

    // Dialogue pour modifier le chantier (INCHANGÉ)
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

    // Dialogue pour supprimer le chantier (INCHANGÉ)
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
                        navController.popBackStack() // Retour à l'écran précédent après suppression
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Annuler") }
            }
        )
    }

    // DatePicker pour les TONTES (INCHANGÉ)
    if (showTonteDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showTonteDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showTonteDatePickerDialog = false
                    tonteDatePickerState.selectedDateMillis?.let { millis ->
                        selectedDateForNotes = Date(millis)
                        interventionTypeForNotes = "Tonte"
                        currentInterventionNotes = "" // Réinitialiser les notes pour une nouvelle intervention
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTonteDatePickerDialog = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = tonteDatePickerState) }
    }

    // DatePicker pour les TAILLES (INCHANGÉ)
    if (showTailleDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showTailleDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showTailleDatePickerDialog = false
                    tailleDatePickerState.selectedDateMillis?.let { millis ->
                        selectedDateForNotes = Date(millis)
                        interventionTypeForNotes = "Taille"
                        currentInterventionNotes = "" // Réinitialiser les notes
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTailleDatePickerDialog = false }) { Text("Annuler") }
            }
        ) { DatePicker(state = tailleDatePickerState) }
    }

    // NOUVEAU DIALOGUE: Pour modifier la note d'une intervention existante
    if (showEditNoteDialog && interventionAModifierNote != null) {
        EditInterventionNoteDialog( // Appel du nouveau Composable de dialogue
            intervention = interventionAModifierNote!!,
            onDismissRequest = {
                showEditNoteDialog = false
                interventionAModifierNote = null // Important de réinitialiser
            },
            onConfirm = { nouvellesNotes ->
                viewModel.updateInterventionNotes(interventionAModifierNote!!, nouvellesNotes)
                showEditNoteDialog = false
                interventionAModifierNote = null // Important de réinitialiser
            }
        )
    }
}

// Composable pour la section de saisie des notes (INCHANGÉ)
@Composable
fun InterventionNotesInputSection(
    selectedDate: Date,
    notes: String,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    dateFormat: SimpleDateFormat
) {
    val paleGreen = Color(0xFFE8F5E9)
    val darkGreenContent = Color(0xFF1B5E20)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = paleGreen,
            contentColor = darkGreenContent
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Ajouter des notes pour l'intervention du : ${dateFormat.format(selectedDate)}",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("Annuler")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onConfirm) {
                    Text("Valider Intervention")
                }
            }
        }
    }
}

// Composable pour le dialogue de modification du chantier (INCHANGÉ)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Permet le défilement si le contenu est trop grand
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Modifier le chantier", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = nomClient, onValueChange = { nomClient = it }, label = { Text("Nom du client / Chantier") }, singleLine = true)
                OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse (optionnel)") })

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = tonteActive, onCheckedChange = { tonteActive = it })
                    Text("Suivi des Tontes Actif", modifier = Modifier
                        .clickable { tonteActive = !tonteActive }
                        .padding(start = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = tailleActive, onCheckedChange = { tailleActive = it })
                    Text("Suivi des Tailles Actif", modifier = Modifier
                        .clickable { tailleActive = !tailleActive }
                        .padding(start = 4.dp))
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

// NOUVEAU COMPOSABLE: Dialogue pour modifier la note d'une intervention
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInterventionNoteDialog(
    intervention: Intervention,
    onDismissRequest: () -> Unit,
    onConfirm: (String?) -> Unit // Prend les nouvelles notes (String nullable)
) {
    var notesText by remember(intervention.notes) { mutableStateOf(intervention.notes ?: "") }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }


    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Modifier la note") },
        text = {
            Column { // Ajout d'une Column pour mieux structurer si besoin
                Text(
                    "Intervention: ${intervention.typeIntervention} du ${dateFormat.format(intervention.dateIntervention)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(notesText.ifBlank { null }) // Passe null si le texte est vide ou ne contient que des espaces
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Annuler")
            }
        }
    )
}