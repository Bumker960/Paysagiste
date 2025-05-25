package com.example.suivichantierspaysagiste

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
private object NoFutureDatesSelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis <= System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= Calendar.getInstance().get(Calendar.YEAR)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object AllDatesSelectableDates : SelectableDates {
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
    val interventions by viewModel.interventionsDuChantier.collectAsStateWithLifecycle()
    val desherbagesPlanifies by viewModel.desherbagesPlanifiesDuChantier.collectAsStateWithLifecycle()

    val derniereTonte by viewModel.derniereTonte.collectAsStateWithLifecycle()
    val nombreTotalTontes by viewModel.nombreTotalTontes.collectAsStateWithLifecycle()
    val tonteUrgencyColor by viewModel.selectedChantierTonteUrgencyColor.collectAsStateWithLifecycle()
    val derniereTaille by viewModel.derniereTaille.collectAsStateWithLifecycle()
    val nombreTotalTailles by viewModel.nombreTotalTailles.collectAsStateWithLifecycle()
    val nombreTaillesCetteAnnee by viewModel.nombreTaillesCetteAnnee.collectAsStateWithLifecycle()
    val tailleUrgencyColor by viewModel.selectedChantierTailleUrgencyColor.collectAsStateWithLifecycle()
    val prochainDesherbagePlanifie by viewModel.prochainDesherbagePlanifie.collectAsStateWithLifecycle()
    val desherbageUrgencyColor by viewModel.selectedChantierDesherbageUrgencyColor.collectAsStateWithLifecycle()
    val nombreTotalDesherbagesEffectues by viewModel.nombreTotalDesherbagesEffectues.collectAsStateWithLifecycle()


    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }
    var showEditChantierDialog by remember { mutableStateOf(false) }
    var showDeleteChantierConfirmDialog by remember { mutableStateOf(false) }
    var interventionASupprimer by remember { mutableStateOf<Intervention?>(null) }

    var showTonteDatePickerDialog by remember { mutableStateOf(false) }
    val tonteDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)
    var showTailleDatePickerDialog by remember { mutableStateOf(false) }
    val tailleDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)
    var showDesherbageInterventionDatePickerDialog by remember { mutableStateOf(false) }
    val desherbageInterventionDatePickerState = rememberDatePickerState(selectableDates = NoFutureDatesSelectableDates)

    var interventionTypeForNotes by remember { mutableStateOf<String?>(null) }
    var selectedDateForNotes by remember { mutableStateOf<Date?>(null) }
    var currentInterventionNotes by remember { mutableStateOf("") }
    var planificationLieeIdForNotes by remember { mutableStateOf<Long?>(null) }

    var interventionAModifierNote by remember { mutableStateOf<Intervention?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    var showPlanifierDesherbageDialog by remember { mutableStateOf(false) }
    var desherbagePlanifieAModifier by remember { mutableStateOf<DesherbagePlanifie?>(null) }
    var desherbagePlanifieASupprimerId by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    fun combineDateWithCurrentTime(selectedDateMillisUtc: Long): Date {
        val selectedDateCalendarUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        selectedDateCalendarUtc.timeInMillis = selectedDateMillisUtc

        val currentTimeCalendarLocal = Calendar.getInstance()

        val combinedCalendarLocal = Calendar.getInstance()
        combinedCalendarLocal.set(
            selectedDateCalendarUtc.get(Calendar.YEAR),
            selectedDateCalendarUtc.get(Calendar.MONTH),
            selectedDateCalendarUtc.get(Calendar.DAY_OF_MONTH),
            currentTimeCalendarLocal.get(Calendar.HOUR_OF_DAY),
            currentTimeCalendarLocal.get(Calendar.MINUTE),
            currentTimeCalendarLocal.get(Calendar.SECOND)
        )
        combinedCalendarLocal.set(Calendar.MILLISECOND, currentTimeCalendarLocal.get(Calendar.MILLISECOND))
        return combinedCalendarLocal.time
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (chantier == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val currentChantier = chantier!!

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

            if (currentChantier.serviceTonteActive) {
                ServiceSectionHeader("Suivi des Tontes")
                InfoLine("Dernière tonte:", derniereTonte?.dateIntervention?.let { "${dateTimeFormat.format(it)} (${TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.time)}j)" } ?: "Aucune", tonteUrgencyColor)
                InfoLine("Nombre total de tontes:", "$nombreTotalTontes")
                Button(
                    onClick = { interventionTypeForNotes = null; planificationLieeIdForNotes = null; selectedDateForNotes = null; currentInterventionNotes = ""; showTonteDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    enabled = interventionTypeForNotes != "Tonte de pelouse"
                ) { Text("Enregistrer une Tonte") }
                if (interventionTypeForNotes == "Tonte de pelouse" && selectedDateForNotes != null) {
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
                        dateFormat = dateTimeFormat
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (currentChantier.serviceTailleActive) {
                ServiceSectionHeader("Suivi des Tailles de Haie")
                InfoLine("Dernière taille:", derniereTaille?.dateIntervention?.let { "${dateTimeFormat.format(it)} (${TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.time)}j)" } ?: "Aucune", tailleUrgencyColor)
                InfoLine("Nombre total de tailles:", "$nombreTotalTailles")
                InfoLine("Tailles cette année:", "$nombreTaillesCetteAnnee / 2")
                Button(
                    onClick = { interventionTypeForNotes = null; planificationLieeIdForNotes = null; selectedDateForNotes = null; currentInterventionNotes = ""; showTailleDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    enabled = interventionTypeForNotes != "Taille de haie"
                ) { Text("Enregistrer une Taille") }
                if (interventionTypeForNotes == "Taille de haie" && selectedDateForNotes != null) {
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
                        dateFormat = dateTimeFormat
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (currentChantier.serviceDesherbageActive) {
                ServiceSectionHeader("Suivi du Désherbage")
                InfoLine("Prochain désherbage planifié:", prochainDesherbagePlanifie?.datePlanifiee?.let { dateFormat.format(it) } ?: "Aucun planifié", desherbageUrgencyColor)
                InfoLine("Nombre total de désherbages effectués:", "$nombreTotalDesherbagesEffectues")

                Button(
                    onClick = { showPlanifierDesherbageDialog = true; desherbagePlanifieAModifier = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Planifier un Désherbage") }

                Button(
                    onClick = { interventionTypeForNotes = null; planificationLieeIdForNotes = null; selectedDateForNotes = null; currentInterventionNotes = ""; showDesherbageInterventionDatePickerDialog = true },
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
                        dateFormat = dateTimeFormat,
                        planificationLiee = desherbagesPlanifies.find { it.id == planificationLieeIdForNotes }
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
                                val datePlanifieeMillis = planif.datePlanifiee.time
                                selectedDateForNotes = combineDateWithCurrentTime(datePlanifieeMillis)
                                planificationLieeIdForNotes = planif.id
                                interventionTypeForNotes = "Désherbage"
                                currentInterventionNotes = planif.notesPlanification ?: ""
                            },
                            onEdit = { desherbagePlanifieAModifier = planif; showPlanifierDesherbageDialog = true },
                            onDelete = { desherbagePlanifieASupprimerId = planif.id },
                            onExportToCalendar = {
                                viewModel.exporterElementVersAgenda(context, planif, currentChantier.nomClient)
                            }
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

            Text("Historique des Interventions :", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (interventions.isEmpty()) {
                Text("Aucune intervention enregistrée pour ce chantier.")
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    interventions.forEach { intervention ->
                        InterventionHistoriqueItem(
                            intervention = intervention,
                            dateFormat = dateTimeFormat,
                            onEditNote = { interventionAModifierNote = intervention; showEditNoteDialog = true },
                            onDelete = { interventionASupprimer = intervention },
                            onExportToCalendar = {
                                viewModel.exporterElementVersAgenda(context, intervention, currentChantier.nomClient)
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    if (showEditChantierDialog && chantier != null) {
        EditChantierDialog(
            chantierInitial = chantier!!,
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
            text = "Êtes-vous sûr de vouloir supprimer l'intervention : ${interventionPourDialogue.typeIntervention} du ${dateTimeFormat.format(interventionPourDialogue.dateIntervention)} ?",
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
            },
            dateTimeFormat = dateTimeFormat
        )
    }

    if (showTonteDatePickerDialog) {
        DatePickerDialogIntervention(
            state = tonteDatePickerState,
            onDismiss = { showTonteDatePickerDialog = false },
            onConfirm = { millis ->
                selectedDateForNotes = combineDateWithCurrentTime(millis)
                interventionTypeForNotes = "Tonte de pelouse"; currentInterventionNotes = ""; showTonteDatePickerDialog = false
            }
        )
    }
    if (showTailleDatePickerDialog) {
        DatePickerDialogIntervention(
            state = tailleDatePickerState,
            onDismiss = { showTailleDatePickerDialog = false },
            onConfirm = { millis ->
                selectedDateForNotes = combineDateWithCurrentTime(millis)
                interventionTypeForNotes = "Taille de haie"; currentInterventionNotes = ""; showTailleDatePickerDialog = false
            }
        )
    }
    if (showDesherbageInterventionDatePickerDialog) {
        DatePickerDialogIntervention(
            state = desherbageInterventionDatePickerState,
            onDismiss = { showDesherbageInterventionDatePickerDialog = false },
            onConfirm = { millis ->
                selectedDateForNotes = combineDateWithCurrentTime(millis)
                interventionTypeForNotes = "Désherbage"; currentInterventionNotes = ""; showDesherbageInterventionDatePickerDialog = false
            }
        )
    }


    if (showPlanifierDesherbageDialog && chantier != null) {
        PlanifierDesherbageDialog(
            chantierId = chantier!!.id,
            desherbagePlanifieInitial = desherbagePlanifieAModifier,
            onDismissRequest = { showPlanifierDesherbageDialog = false; desherbagePlanifieAModifier = null },
            onConfirm = { chId, date, notes, planifExistante ->
                if (planifExistante != null) {
                    viewModel.updateDesherbagePlanifie(planifExistante.copy(datePlanifiee = date, notesPlanification = notes))
                } else {
                    viewModel.ajouterDesherbagePlanifie(chId, date, notes)
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

// --- Définitions des composables qui manquaient ---
@Composable
fun ServiceSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
fun InfoLine(label: String, value: String, color: Color = LocalContentColor.current) {
    Row(modifier = Modifier.padding(bottom = 2.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, color = color)
    }
}
// --- Fin des définitions des composables qui manquaient ---


@Composable
fun InterventionHistoriqueItem(
    intervention: Intervention,
    dateFormat: SimpleDateFormat,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    onExportToCalendar: () -> Unit
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExportToCalendar, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (intervention.exporteAgenda) Icons.Filled.EventAvailable else Icons.Filled.EventNote,
                    contentDescription = if (intervention.exporteAgenda) "Exporté vers l'agenda" else "Ajouter à l'agenda",
                    tint = if (intervention.exporteAgenda) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
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
    planificationLiee: DesherbagePlanifie? = null
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
                "Date et heure: ${dateFormat.format(selectedDate)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (planificationLiee != null) {
                Text(
                    "Lié à la planification du: ${SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(planificationLiee.datePlanifiee)}",
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
    var desherbageActive by remember(chantierInitial.serviceDesherbageActive) { mutableStateOf(chantierInitial.serviceDesherbageActive) }

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

                // Utilisation de ServiceActivationRow défini dans ChantierScreen.kt (ou un fichier partagé)
                ServiceActivationRow(label = "Suivi des Tontes Actif", checked = tonteActive, onCheckedChange = { tonteActive = it })
                ServiceActivationRow(label = "Suivi des Tailles Actif", checked = tailleActive, onCheckedChange = { tailleActive = it })
                ServiceActivationRow(label = "Suivi Désherbage Actif", checked = desherbageActive, onCheckedChange = { desherbageActive = it })


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
                                    serviceDesherbageActive = desherbageActive
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
    onConfirm: (String?) -> Unit,
    dateTimeFormat: SimpleDateFormat
) {
    var notesText by remember(intervention.notes) { mutableStateOf(intervention.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Modifier la note") },
        text = {
            Column {
                Text("Intervention: ${intervention.typeIntervention} du ${dateTimeFormat.format(intervention.dateIntervention)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
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

@Composable
fun DesherbagePlanifieItem(
    planification: DesherbagePlanifie,
    dateFormat: SimpleDateFormat,
    onMarkAsDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportToCalendar: () -> Unit
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
            IconButton(onClick = onExportToCalendar, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (planification.exporteAgenda) Icons.Filled.EventAvailable else Icons.Filled.EventNote,
                    contentDescription = if (planification.exporteAgenda) "Exporté vers l'agenda" else "Ajouter à l'agenda",
                    tint = if (planification.exporteAgenda) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = onMarkAsDone, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Done, contentDescription = "Marquer comme effectué et enregistrer intervention", tint = MaterialTheme.colorScheme.primary)
            }
        } else {
            IconButton(onClick = onExportToCalendar, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (planification.exporteAgenda) Icons.Filled.EventAvailable else Icons.Filled.EventNote,
                    contentDescription = if (planification.exporteAgenda) "Exporté vers l'agenda" else "Ajouter à l'agenda",
                    tint = if (planification.exporteAgenda) MaterialTheme.colorScheme.primary else itemColor
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanifierDesherbageDialog(
    chantierId: Long,
    desherbagePlanifieInitial: DesherbagePlanifie?,
    onDismissRequest: () -> Unit,
    onConfirm: (chantierId: Long, date: Date, notes: String?, planifExistante: DesherbagePlanifie?) -> Unit
) {
    val isEditing = desherbagePlanifieInitial != null
    val title = if (isEditing) "Modifier Planification Désherbage" else "Planifier un Désherbage"

    var notes by remember(desherbagePlanifieInitial?.notesPlanification) { mutableStateOf(desherbagePlanifieInitial?.notesPlanification ?: "") }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = desherbagePlanifieInitial?.datePlanifiee?.time ?: System.currentTimeMillis(),
        selectableDates = AllDatesSelectableDates
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var tempSelectedDateMillis by remember(datePickerState.selectedDateMillis) { mutableStateOf(datePickerState.selectedDateMillis ?: System.currentTimeMillis()) }


    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)

                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(tempSelectedDateMillis)))
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { tempSelectedDateMillis = it }
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
                            onConfirm(chantierId, Date(tempSelectedDateMillis), notes.ifBlank { null }, desherbagePlanifieInitial)
                        }
                    ) { Text(if (isEditing) "Enregistrer" else "Planifier") }
                }
            }
        }
    }
}

// Note: ServiceActivationRow n'est plus défini ici,
// il utilisera celui de ChantierScreen.kt
// Si ChantierScreen.kt ne le définit pas comme public top-level,
// il faudra le déplacer ici ou dans un fichier partagé.
// Pour l'instant, on suppose qu'il est accessible.
