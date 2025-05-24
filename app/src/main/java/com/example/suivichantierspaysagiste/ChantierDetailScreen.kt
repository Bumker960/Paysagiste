package com.example.suivichantierspaysagiste

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event // Pour les dates planifiées
import androidx.compose.material.icons.filled.Done // Pour marquer comme fait
import androidx.compose.material.icons.filled.Close // Pour annuler/supprimer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
private object NoFutureDatesSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis <= System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1) // Permet aujourd'hui et hier
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= Calendar.getInstance().get(Calendar.YEAR)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object AllDatesSelectableDates : SelectableDates { // Pour la planification des désherbages
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return true
    }
    override fun isSelectableYear(year: Int): Boolean {
        return true
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
    // Tonte
    val derniereTonte by viewModel.derniereTonte.collectAsStateWithLifecycle()
    val nombreTotalTontes by viewModel.nombreTotalTontes.collectAsStateWithLifecycle()
    val tonteUrgencyColor by viewModel.selectedChantierTonteUrgencyColor.collectAsStateWithLifecycle()
    // Taille
    val derniereTaille by viewModel.derniereTaille.collectAsStateWithLifecycle()
    val nombreTotalTailles by viewModel.nombreTotalTailles.collectAsStateWithLifecycle()
    val nombreTaillesCetteAnnee by viewModel.nombreTaillesCetteAnnee.collectAsStateWithLifecycle()
    val tailleUrgencyColor by viewModel.selectedChantierTailleUrgencyColor.collectAsStateWithLifecycle()
    // Désherbage
    val desherbagesPlanifies by viewModel.desherbagesPlanifiesDuChantier.collectAsStateWithLifecycle()
    val prochainDesherbagePlanifie by viewModel.prochainDesherbagePlanifie.collectAsStateWithLifecycle()
    val desherbageUrgencyColor by viewModel.selectedChantierDesherbageUrgencyColor.collectAsStateWithLifecycle()
    val nombreTotalDesherbagesEffectues by viewModel.nombreTotalDesherbagesEffectues.collectAsStateWithLifecycle()

    val interventions by viewModel.interventionsDuChantier.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    var showEditChantierDialog by remember { mutableStateOf(false) }
    var showDeleteChantierConfirmDialog by remember { mutableStateOf(false) }
    var interventionASupprimer by remember { mutableStateOf<Intervention?>(null) }

    // États pour les DatePickers d'intervention
    var showTonteDatePickerDialog by remember { mutableStateOf(false) }
    val tonteDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)
    var showTailleDatePickerDialog by remember { mutableStateOf(false) }
    val tailleDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)
    var showDesherbageInterventionDatePickerDialog by remember { mutableStateOf(false) }
    val desherbageInterventionDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)


    // États pour la saisie des notes d'intervention
    var interventionTypeForNotes by remember { mutableStateOf<String?>(null) }
    var selectedDateForNotes by remember { mutableStateOf<Date?>(null) }
    var currentInterventionNotes by remember { mutableStateOf("") }
    var planificationLieeIdForNotes by remember { mutableStateOf<Long?>(null) }


    var interventionAModifierNote by remember { mutableStateOf<Intervention?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    // NOUVEAUX états pour la gestion des planifications de désherbage
    var showPlanifierDesherbageDialog by remember { mutableStateOf(false) }
    var desherbagePlanifieAModifier by remember { mutableStateOf<DesherbagePlanifie?>(null) }
    var desherbagePlanifieASupprimerId by remember { mutableStateOf<Long?>(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (chantier == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Text("Chargement du chantier...")
            }
        } else {
            val currentChantier = chantier!!

            // Affichage du nom du chantier, adresse, boutons Edit/Delete Chantier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentChantier.nomClient,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = { showEditChantierDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Modifier le chantier")
                    }
                    IconButton(onClick = { showDeleteChantierConfirmDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer le chantier", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            currentChantier.adresse?.let { adresse ->
                if (adresse.isNotBlank()) {
                    Text(text = "Adresse: $adresse", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Section Tontes
            if (currentChantier.serviceTonteActive) {
                ServiceSectionHeader("Suivi des Tontes")
                InfoLine("Dernière tonte:", derniereTonte?.dateIntervention?.let { "${dateFormat.format(it)} (${TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.time)}j)" } ?: "Aucune", tonteUrgencyColor)
                InfoLine("Nombre total de tontes:", "$nombreTotalTontes")
                Button(
                    onClick = { interventionTypeForNotes = null; showTonteDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    enabled = interventionTypeForNotes != "Tonte"
                ) { Text("Enregistrer une Tonte") }
                if (interventionTypeForNotes == "Tonte" && selectedDateForNotes != null) {
                    InterventionNotesInputSection(
                        interventionType = "Tonte de pelouse",
                        selectedDate = selectedDateForNotes!!,
                        notes = currentInterventionNotes,
                        onNotesChange = { currentInterventionNotes = it },
                        onConfirm = {
                            viewModel.ajouterTonte(currentChantier.id, selectedDateForNotes!!, currentInterventionNotes.ifBlank { null })
                            interventionTypeForNotes = null; selectedDateForNotes = null; currentInterventionNotes = ""
                        },
                        onCancel = { interventionTypeForNotes = null; selectedDateForNotes = null; currentInterventionNotes = "" },
                        dateFormat = dateFormat
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section Tailles
            if (currentChantier.serviceTailleActive) {
                ServiceSectionHeader("Suivi des Tailles de Haie")
                InfoLine("Dernière taille:", derniereTaille?.dateIntervention?.let { "${dateFormat.format(it)} (${TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.time)}j)" } ?: "Aucune", tailleUrgencyColor)
                InfoLine("Nombre total de tailles:", "$nombreTotalTailles")
                InfoLine("Tailles cette année:", "$nombreTaillesCetteAnnee / 2")
                Button(
                    onClick = { interventionTypeForNotes = null; showTailleDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    enabled = interventionTypeForNotes != "Taille"
                ) { Text("Enregistrer une Taille") }
                if (interventionTypeForNotes == "Taille" && selectedDateForNotes != null) {
                    InterventionNotesInputSection(
                        interventionType = "Taille de haie",
                        selectedDate = selectedDateForNotes!!,
                        notes = currentInterventionNotes,
                        onNotesChange = { currentInterventionNotes = it },
                        onConfirm = {
                            viewModel.ajouterTailleHaie(currentChantier.id, selectedDateForNotes!!, currentInterventionNotes.ifBlank { null })
                            interventionTypeForNotes = null; selectedDateForNotes = null; currentInterventionNotes = ""
                        },
                        onCancel = { interventionTypeForNotes = null; selectedDateForNotes = null; currentInterventionNotes = "" },
                        dateFormat = dateFormat
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // NOUVELLE Section Désherbage
            if (currentChantier.serviceDesherbageActive) {
                ServiceSectionHeader("Suivi du Désherbage")
                InfoLine("Prochain désherbage planifié:", prochainDesherbagePlanifie?.datePlanifiee?.let { dateFormat.format(it) } ?: "Aucun planifié", desherbageUrgencyColor)
                InfoLine("Nombre total de désherbages effectués:", "$nombreTotalDesherbagesEffectues")

                Button(
                    onClick = { showPlanifierDesherbageDialog = true; desherbagePlanifieAModifier = null }, // Ouvre pour ajouter
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Planifier un Désherbage") }

                Button(
                    onClick = { interventionTypeForNotes = null; planificationLieeIdForNotes = null; showDesherbageInterventionDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    enabled = interventionTypeForNotes != "Désherbage"
                ) { Text("Enregistrer un Désherbage (Intervention)") }

                if (interventionTypeForNotes == "Désherbage" && selectedDateForNotes != null) {
                    InterventionNotesInputSection(
                        interventionType = "Désherbage",
                        selectedDate = selectedDateForNotes!!,
                        notes = currentInterventionNotes,
                        onNotesChange = { currentInterventionNotes = it },
                        onConfirm = {
                            viewModel.ajouterDesherbageIntervention(currentChantier.id, selectedDateForNotes!!, currentInterventionNotes.ifBlank { null }, planificationLieeIdForNotes)
                            interventionTypeForNotes = null; selectedDateForNotes = null; currentInterventionNotes = ""; planificationLieeIdForNotes = null
                        },
                        onCancel = { interventionTypeForNotes = null; selectedDateForNotes = null; currentInterventionNotes = ""; planificationLieeIdForNotes = null},
                        dateFormat = dateFormat,
                        planificationLiee = prochainDesherbagePlanifie?.takeIf { it.id == planificationLieeIdForNotes }
                    )
                }

                Text("Désherbages Planifiés:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                if (desherbagesPlanifies.isEmpty()) {
                    Text("Aucun désherbage planifié pour ce chantier.")
                } else {
                    desherbagesPlanifies.forEach { planif ->
                        DesherbagePlanifieItem(
                            planification = planif,
                            dateFormat = dateFormat,
                            onMarkAsDone = {
                                // Proposer d'enregistrer une intervention liée
                                planificationLieeIdForNotes = planif.id
                                selectedDateForNotes = planif.datePlanifiee // Pré-remplir la date
                                interventionTypeForNotes = "Désherbage"
                                currentInterventionNotes = planif.notesPlanification ?: "" // Pré-remplir notes si existent
                            },
                            onEdit = { desherbagePlanifieAModifier = planif; showPlanifierDesherbageDialog = true },
                            onDelete = { desherbagePlanifieASupprimerId = planif.id }
                        )
                        Divider()
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }


            if (!currentChantier.serviceTonteActive && !currentChantier.serviceTailleActive && !currentChantier.serviceDesherbageActive) {
                Text("Aucun service de suivi (tonte, taille ou désherbage) n'est actif pour ce chantier.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Historique des Interventions
            Text("Historique des Interventions :", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (interventions.isEmpty()) {
                Text("Aucune intervention enregistrée pour ce chantier.")
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    interventions.forEach { intervention ->
                        InterventionHistoriqueItem(
                            intervention = intervention,
                            dateFormat = dateFormat,
                            onEditNote = { interventionAModifierNote = intervention; showEditNoteDialog = true },
                            onDelete = { interventionASupprimer = intervention }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // ---- DIALOGUES ----
    if (showEditChantierDialog && chantier != null) {
        EditChantierDialog(
            chantierInitial = chantier!!, // Pass initial chantier
            onDismissRequest = { showEditChantierDialog = false },
            onConfirm = { chantierModifie ->
                viewModel.updateChantier(chantierModifie)
                showEditChantierDialog = false
            }
        )
    }

    if (showDeleteChantierConfirmDialog && chantier != null) {
        ConfirmDeleteDialog(
            title = "Confirmer la suppression",
            text = "Êtes-vous sûr de vouloir supprimer le chantier \"${chantier!!.nomClient}\" et toutes ses données associées (interventions, planifications) ? Cette action est irréversible.",
            onConfirm = {
                viewModel.deleteChantier(chantier!!)
                showDeleteChantierConfirmDialog = false
                navController.popBackStack()
            },
            onDismiss = { showDeleteChantierConfirmDialog = false }
        )
    }

    if (interventionASupprimer != null) {
        val interventionPourDialogue = interventionASupprimer!!
        ConfirmDeleteDialog(
            title = "Confirmer la suppression",
            text = "Êtes-vous sûr de vouloir supprimer l'intervention : ${interventionPourDialogue.typeIntervention} du ${dateFormat.format(interventionPourDialogue.dateIntervention)} ?",
            onConfirm = {
                viewModel.deleteIntervention(interventionPourDialogue)
                interventionASupprimer = null
            },
            onDismiss = { interventionASupprimer = null }
        )
    }

    if (showEditNoteDialog && interventionAModifierNote != null) {
        EditInterventionNoteDialog(
            intervention = interventionAModifierNote!!,
            onDismissRequest = { showEditNoteDialog = false; interventionAModifierNote = null },
            onConfirm = { nouvellesNotes ->
                viewModel.updateInterventionNotes(interventionAModifierNote!!, nouvellesNotes)
                showEditNoteDialog = false; interventionAModifierNote = null
            }
        )
    }

    // DatePickers pour interventions
    if (showTonteDatePickerDialog) { DatePickerDialogIntervention(state = tonteDatePickerState, onDismiss = { showTonteDatePickerDialog = false }, onConfirm = { millis -> selectedDateForNotes = Date(millis); interventionTypeForNotes = "Tonte"; currentInterventionNotes = ""; showTonteDatePickerDialog = false }) }
    if (showTailleDatePickerDialog) { DatePickerDialogIntervention(state = tailleDatePickerState, onDismiss = { showTailleDatePickerDialog = false }, onConfirm = { millis -> selectedDateForNotes = Date(millis); interventionTypeForNotes = "Taille"; currentInterventionNotes = ""; showTailleDatePickerDialog = false }) }
    if (showDesherbageInterventionDatePickerDialog) { DatePickerDialogIntervention(state = desherbageInterventionDatePickerState, onDismiss = { showDesherbageInterventionDatePickerDialog = false }, onConfirm = { millis -> selectedDateForNotes = Date(millis); interventionTypeForNotes = "Désherbage"; currentInterventionNotes = ""; showDesherbageInterventionDatePickerDialog = false }) }


    // NOUVEAUX Dialogues pour la planification du désherbage
    if (showPlanifierDesherbageDialog && chantier != null) {
        PlanifierDesherbageDialog(
            chantierId = chantier!!.id,
            desherbagePlanifieInitial = desherbagePlanifieAModifier,
            onDismissRequest = { showPlanifierDesherbageDialog = false; desherbagePlanifieAModifier = null },
            onConfirm = { chantierId, date, notes, planifExistante ->
                if (planifExistante != null) {
                    viewModel.updateDesherbagePlanifie(planifExistante.copy(datePlanifiee = date, notesPlanification = notes))
                } else {
                    viewModel.ajouterDesherbagePlanifie(chantierId, date, notes)
                }
                showPlanifierDesherbageDialog = false
                desherbagePlanifieAModifier = null
            }
        )
    }
    if (desherbagePlanifieASupprimerId != null) {
        val idASupprimer = desherbagePlanifieASupprimerId!!
        ConfirmDeleteDialog(
            title = "Supprimer Planification",
            text = "Êtes-vous sûr de vouloir supprimer cette planification de désherbage ?",
            onConfirm = {
                viewModel.deleteDesherbagePlanifie(idASupprimer)
                desherbagePlanifieASupprimerId = null
            },
            onDismiss = { desherbagePlanifieASupprimerId = null }
        )
    }
}

@Composable
fun ServiceSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun InfoLine(label: String, value: String, color: Color = LocalContentColor.current) {
    Row {
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, color = color)
    }
}

@Composable
fun InterventionHistoriqueItem(
    intervention: Intervention,
    dateFormat: SimpleDateFormat,
    onEditNote: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
        Row {
            IconButton(onClick = onEditNote, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Modifier la note")
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer cette intervention", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogIntervention(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onConfirm(it) } }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    ) { DatePicker(state = state) }
}


@Composable
fun InterventionNotesInputSection(
    interventionType: String,
    selectedDate: Date,
    notes: String,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    dateFormat: SimpleDateFormat,
    planificationLiee: DesherbagePlanifie? = null // Pour afficher si c'est lié à une planification
) {
    val paleColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = paleColor, contentColor = contentColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Enregistrer Intervention: $interventionType",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "Date: ${dateFormat.format(selectedDate)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (planificationLiee != null) {
                Text(
                    "Lié à la planification du: ${dateFormat.format(planificationLiee.datePlanifiee)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            }
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
                TextButton(onClick = onCancel) { Text("Annuler") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onConfirm) { Text("Valider Intervention") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChantierDialog(
    chantierInitial: Chantier,
    onDismissRequest: () -> Unit,
    onConfirm: (Chantier) -> Unit
) {
    var nomClient by remember(chantierInitial.nomClient) { mutableStateOf(chantierInitial.nomClient) }
    var adresse by remember(chantierInitial.adresse) { mutableStateOf(chantierInitial.adresse ?: "") }
    var tonteActive by remember(chantierInitial.serviceTonteActive) { mutableStateOf(chantierInitial.serviceTonteActive) }
    var tailleActive by remember(chantierInitial.serviceTailleActive) { mutableStateOf(chantierInitial.serviceTailleActive) }
    var desherbageActive by remember(chantierInitial.serviceDesherbageActive) { mutableStateOf(chantierInitial.serviceDesherbageActive) } // NOUVEAU

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Modifier le chantier", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = nomClient, onValueChange = { nomClient = it }, label = { Text("Nom du client / Chantier") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse (optionnel)") }, modifier = Modifier.fillMaxWidth())

                ServiceActivationRow(label = "Suivi des Tontes Actif", checked = tonteActive, onCheckedChange = { tonteActive = it })
                ServiceActivationRow(label = "Suivi des Tailles Actif", checked = tailleActive, onCheckedChange = { tailleActive = it })
                ServiceActivationRow(label = "Suivi Désherbage Actif", checked = desherbageActive, onCheckedChange = { desherbageActive = it }) // NOUVEAU

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nomClient.isNotBlank()) {
                                onConfirm(chantierInitial.copy(
                                    nomClient = nomClient,
                                    adresse = adresse.ifBlank { null },
                                    serviceTonteActive = tonteActive,
                                    serviceTailleActive = tailleActive,
                                    serviceDesherbageActive = desherbageActive // NOUVEAU
                                ))
                            }
                        },
                        enabled = nomClient.isNotBlank()
                    ) { Text("Enregistrer") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInterventionNoteDialog(
    intervention: Intervention,
    onDismissRequest: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var notesText by remember(intervention.notes) { mutableStateOf(intervention.notes ?: "") }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Modifier la note") },
        text = {
            Column {
                Text("Intervention: ${intervention.typeIntervention} du ${dateFormat.format(intervention.dateIntervention)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(value = notesText, onValueChange = { notesText = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(notesText.ifBlank { null }) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Annuler") } }
    )
}

@Composable
fun ConfirmDeleteDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Supprimer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

// NOUVEAU: Composable pour afficher un item de désherbage planifié
@Composable
fun DesherbagePlanifieItem(
    planification: DesherbagePlanifie,
    dateFormat: SimpleDateFormat,
    onMarkAsDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val itemColor = if (planification.estEffectue) MaterialTheme.colorScheme.outline.copy(alpha = 0.7f) else LocalContentColor.current
    val textStyle = if (planification.estEffectue) MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic) else MaterialTheme.typography.bodyMedium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Event,
            contentDescription = "Date planifiée",
            modifier = Modifier.padding(end = 8.dp),
            tint = itemColor
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Planifié pour: ${dateFormat.format(planification.datePlanifiee)}",
                style = textStyle,
                color = itemColor
            )
            if (planification.estEffectue) {
                Text("Effectué", style = MaterialTheme.typography.labelSmall, color = Color.Green.copy(alpha = 0.8f))
            }
            planification.notesPlanification?.let {
                if (it.isNotBlank()) {
                    Text("Notes planif.: $it", style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = itemColor.copy(alpha = 0.8f))
                }
            }
        }
        if (!planification.estEffectue) {
            IconButton(onClick = onMarkAsDone, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Done, contentDescription = "Marquer comme effectué et enregistrer intervention", tint = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Modifier planification", tint = if (planification.estEffectue) itemColor else LocalContentColor.current)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Delete, contentDescription = "Supprimer planification", tint = MaterialTheme.colorScheme.error)
        }
    }
}

// NOUVEAU: Dialogue pour planifier/modifier un désherbage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanifierDesherbageDialog(
    chantierId: Long, // Nécessaire si on ajoute une nouvelle planification
    desherbagePlanifieInitial: DesherbagePlanifie?, // Null si ajout, non-null si modification
    onDismissRequest: () -> Unit,
    onConfirm: (chantierId: Long, date: Date, notes: String?, planifExistante: DesherbagePlanifie?) -> Unit
) {
    val isEditing = desherbagePlanifieInitial != null
    val title = if (isEditing) "Modifier Planification Désherbage" else "Planifier un Désherbage"

    var notes by remember(desherbagePlanifieInitial?.notesPlanification) { mutableStateOf(desherbagePlanifieInitial?.notesPlanification ?: "") }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = desherbagePlanifieInitial?.datePlanifiee?.time ?: System.currentTimeMillis(),
        selectableDates = AllDatesSelectableDates // Permet de sélectionner toutes les dates
    )
    var showDatePicker by remember { mutableStateOf(false) }
    // Utiliser un état pour la date sélectionnée pour pouvoir l'afficher avant confirmation du DatePicker
    var tempSelectedDateMillis by remember(datePickerState.selectedDateMillis) { mutableStateOf(datePickerState.selectedDateMillis) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)

                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(tempSelectedDateMillis?.let { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(it)) } ?: "Choisir une date")
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                tempSelectedDateMillis = datePickerState.selectedDateMillis // Mettre à jour la date affichée
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
                    ) { DatePicker(state = datePickerState) }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes de planification (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Annuler") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            tempSelectedDateMillis?.let { dateMillis ->
                                onConfirm(chantierId, Date(dateMillis), notes.ifBlank { null }, desherbagePlanifieInitial)
                            }
                        },
                        enabled = tempSelectedDateMillis != null // Activer seulement si une date est sélectionnée
                    ) { Text(if (isEditing) "Enregistrer" else "Planifier") }
                }
            }
        }
    }
}