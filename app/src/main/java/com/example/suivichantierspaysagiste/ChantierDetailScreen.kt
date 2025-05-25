package com.example.suivichantierspaysagiste

import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation // Icône pour géocodage
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.StopCircle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch


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
        Log.d("ChantierDetailScreen", "Chargement du chantier ID: $chantierId")
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

    val interventionEnCoursUiState by viewModel.interventionEnCoursUi.collectAsStateWithLifecycle()


    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }

    var showEditChantierDialog by remember { mutableStateOf(false) }
    var showDeleteChantierConfirmDialog by remember { mutableStateOf(false) }
    var interventionASupprimer by remember { mutableStateOf<Intervention?>(null) }

    var showEnregistrerManuellementDialog by remember { mutableStateOf(false) }
    var typeInterventionPourManuel by remember { mutableStateOf("") }
    var interventionAModifierTemps by remember { mutableStateOf<Intervention?>(null) }

    var showSaisieNotesDialog by remember { mutableStateOf(false) }
    var notesPourInterventionTerminee by remember { mutableStateOf("") }


    var interventionAModifierNoteExplicite by remember { mutableStateOf<Intervention?>(null) }
    var showEditNoteExpliciteDialog by remember { mutableStateOf(false) }

    var showPlanifierDesherbageDialog by remember { mutableStateOf(false) }
    var desherbagePlanifieAModifier by remember { mutableStateOf<DesherbagePlanifie?>(null) }
    var desherbagePlanifieASupprimerId by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

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

            interventionEnCoursUiState?.let { enCoursUi ->
                if (enCoursUi.chantierId == currentChantier.id) {
                    InterventionEnCoursCard(
                        interventionEnCoursUi = enCoursUi,
                        onTerminerClick = {
                            notesPourInterventionTerminee = ""
                            showSaisieNotesDialog = true
                        }
                    )
                }
            }


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
                    Text(text = "Adresse: $adresse", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                }
            }
            // Affichage des coordonnées si disponibles
            if (currentChantier.latitude != null && currentChantier.longitude != null) {
                Text(
                    text = "Coordonnées: Lat %.4f, Lng %.4f".format(currentChantier.latitude, currentChantier.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentChantier.serviceTonteActive) {
                ServiceSectionChrono(
                    titre = "Suivi des Tontes",
                    derniereInterventionDate = derniereTonte?.dateIntervention,
                    nombreTotalInterventions = nombreTotalTontes,
                    urgencyColor = tonteUrgencyColor,
                    typeIntervention = "Tonte de pelouse",
                    chantierId = currentChantier.id,
                    chantierNom = currentChantier.nomClient,
                    viewModel = viewModel,
                    interventionEnCoursGlobale = interventionEnCoursUiState,
                    onDemarrerClick = { viewModel.demarrerInterventionChrono(currentChantier.id, "Tonte de pelouse", currentChantier.nomClient) },
                    onEnregistrerManuelClick = {
                        typeInterventionPourManuel = "Tonte de pelouse"
                        interventionAModifierTemps = null
                        showEnregistrerManuellementDialog = true
                    },
                    dateTimeFormat = dateTimeFormat
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (currentChantier.serviceTailleActive) {
                ServiceSectionChrono(
                    titre = "Suivi des Tailles de Haie",
                    derniereInterventionDate = derniereTaille?.dateIntervention,
                    nombreTotalInterventions = nombreTotalTailles,
                    infosSupplementaires = "Tailles cette année: $nombreTaillesCetteAnnee / 2",
                    urgencyColor = tailleUrgencyColor,
                    typeIntervention = "Taille de haie",
                    chantierId = currentChantier.id,
                    chantierNom = currentChantier.nomClient,
                    viewModel = viewModel,
                    interventionEnCoursGlobale = interventionEnCoursUiState,
                    onDemarrerClick = { viewModel.demarrerInterventionChrono(currentChantier.id, "Taille de haie", currentChantier.nomClient) },
                    onEnregistrerManuelClick = {
                        typeInterventionPourManuel = "Taille de haie"
                        interventionAModifierTemps = null
                        showEnregistrerManuellementDialog = true
                    },
                    dateTimeFormat = dateTimeFormat
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (currentChantier.serviceDesherbageActive) {
                ServiceSectionHeader("Suivi du Désherbage")
                InfoLine("Prochain désherbage planifié:", prochainDesherbagePlanifie?.datePlanifiee?.let { dateFormat.format(it) } ?: "Aucun planifié", desherbageUrgencyColor)
                InfoLine("Nombre total de désherbages effectués:", "$nombreTotalDesherbagesEffectues")

                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.demarrerInterventionChrono(currentChantier.id, "Désherbage", currentChantier.nomClient) },
                        modifier = Modifier.weight(1f),
                        enabled = interventionEnCoursUiState == null
                    ) { Text("Démarrer Désherbage") }
                    OutlinedButton(
                        onClick = {
                            typeInterventionPourManuel = "Désherbage"
                            interventionAModifierTemps = null
                            showEnregistrerManuellementDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Enreg. Manuel Désherbage") }
                }

                Button(
                    onClick = { showPlanifierDesherbageDialog = true; desherbagePlanifieAModifier = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Planifier un Désherbage") }

                Text("Désherbages Planifiés:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                if (desherbagesPlanifies.isEmpty()) {
                    Text("Aucun désherbage planifié pour ce chantier.")
                } else {
                    desherbagesPlanifies.forEach { planif ->
                        DesherbagePlanifieItem(
                            planification = planif,
                            dateFormat = dateFormat,
                            onMarkAsDone = {
                                viewModel.marquerDesherbagePlanifieEffectue(planif.id, Date(), planif.notesPlanification)
                                Toast.makeText(context, "Planification marquée comme effectuée.", Toast.LENGTH_LONG).show()
                            },
                            onEdit = { desherbagePlanifieAModifier = planif; showPlanifierDesherbageDialog = true },
                            onDelete = { desherbagePlanifieASupprimerId = planif.id },
                            onExportToCalendar = { viewModel.exporterElementVersAgenda(context, planif, currentChantier.nomClient) }
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
                    interventions.filter { it.statutIntervention == InterventionStatus.COMPLETED.name }.forEach { intervention ->
                        InterventionHistoriqueItem(
                            intervention = intervention,
                            dateTimeFormat = dateTimeFormat,
                            timeFormat = timeFormat,
                            onEditNote = {
                                interventionAModifierNoteExplicite = intervention
                                showEditNoteExpliciteDialog = true
                            },
                            onEditTime = {
                                interventionAModifierTemps = intervention
                                typeInterventionPourManuel = intervention.typeIntervention
                                showEnregistrerManuellementDialog = true
                            },
                            onDelete = { interventionASupprimer = intervention },
                            onExportToCalendar = { viewModel.exporterElementVersAgenda(context, intervention, currentChantier.nomClient) }
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
            viewModel = viewModel, // Passer le viewModel
            onDismissRequest = { showEditChantierDialog = false },
            onConfirm = { chantierModifie -> // chantierModifie inclut maintenant lat/lng
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
            text = "Êtes-vous sûr de vouloir supprimer l'intervention : ${interventionPourDialogue.typeIntervention} du ${interventionPourDialogue.dateIntervention.let { dateTimeFormat.format(it) }} ?",
            onConfirm = {
                viewModel.deleteIntervention(interventionPourDialogue)
                interventionASupprimer = null
            },
            onDismiss = { interventionASupprimer = null }
        )
    }

    if (showEditNoteExpliciteDialog && interventionAModifierNoteExplicite != null) {
        EditInterventionNoteDialog(
            intervention = interventionAModifierNoteExplicite!!,
            onDismissRequest = { showEditNoteExpliciteDialog = false; interventionAModifierNoteExplicite = null },
            onConfirm = { nouvellesNotes ->
                viewModel.updateInterventionNotes(interventionAModifierNoteExplicite!!, nouvellesNotes)
                showEditNoteExpliciteDialog = false; interventionAModifierNoteExplicite = null
            },
            dateTimeFormat = dateTimeFormat
        )
    }

    if (showSaisieNotesDialog) {
        SaisieNotesInterventionDialog(
            notesInitiales = notesPourInterventionTerminee,
            onDismissRequest = {
                viewModel.terminerInterventionChrono(notesPourInterventionTerminee.ifBlank { null })
                showSaisieNotesDialog = false
            },
            onConfirm = { notesSaisies ->
                viewModel.terminerInterventionChrono(notesSaisies)
                showSaisieNotesDialog = false
            }
        )
    }


    if (showEnregistrerManuellementDialog && chantier != null) {
        EnregistrerTempsManuellementDialog(
            chantierNom = chantier!!.nomClient,
            typeIntervention = typeInterventionPourManuel,
            interventionExistante = interventionAModifierTemps,
            onDismissRequest = {
                showEnregistrerManuellementDialog = false
                interventionAModifierTemps = null
            },
            onConfirm = { dateDebut, dateFin, dureeMillis, notes ->
                if (interventionAModifierTemps != null) {
                    viewModel.modifierTempsIntervention(interventionAModifierTemps!!.id, dateDebut, dateFin, dureeMillis)
                    if(notes != interventionAModifierTemps!!.notes) { // Mettre à jour les notes seulement si elles ont changé
                        viewModel.updateInterventionNotes(interventionAModifierTemps!!, notes)
                    }
                } else {
                    viewModel.enregistrerInterventionManuelle(chantier!!.id, typeInterventionPourManuel, dateDebut, dateFin, dureeMillis, notes)
                }
                showEnregistrerManuellementDialog = false
                interventionAModifierTemps = null
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

// ... (Les autres composables comme ServiceSectionChrono, InterventionEnCoursCard, etc. restent ici)
// ... Assurez-vous qu'ils sont présents ou importés si définis ailleurs.

@Composable
fun ServiceSectionChrono(
    titre: String,
    derniereInterventionDate: Date?,
    nombreTotalInterventions: Int,
    infosSupplementaires: String? = null,
    urgencyColor: Color,
    typeIntervention: String,
    chantierId: Long,
    chantierNom: String,
    viewModel: ChantierViewModel,
    interventionEnCoursGlobale: InterventionEnCoursUi?,
    onDemarrerClick: () -> Unit,
    onEnregistrerManuelClick: () -> Unit,
    dateTimeFormat: SimpleDateFormat
) {
    val estEnCoursPourCeTypeEtChantier = interventionEnCoursGlobale?.let {
        it.chantierId == chantierId && it.typeIntervention == typeIntervention
    } ?: false

    ServiceSectionHeader(titre)
    InfoLine(
        "Dernière intervention:",
        derniereInterventionDate?.let { date -> "${dateTimeFormat.format(date)} (${TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - date.time)}j)" } ?: "Aucune",
        urgencyColor
    )
    InfoLine("Nombre total d'interventions:", "$nombreTotalInterventions")
    infosSupplementaires?.let { InfoLine("", it) }

    if (!estEnCoursPourCeTypeEtChantier) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDemarrerClick,
                modifier = Modifier.weight(1f),
                enabled = interventionEnCoursGlobale == null // Désactivé si N'IMPORTE QUELLE intervention est en cours
            ) { Text("Démarrer") }
            OutlinedButton(
                onClick = onEnregistrerManuelClick,
                modifier = Modifier.weight(1f)
            ) { Text("Enreg. Manuel") }
        }
    }
}


@Composable
fun InterventionEnCoursCard(
    interventionEnCoursUi: InterventionEnCoursUi,
    onTerminerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
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
                    "${interventionEnCoursUi.typeInterventionLisible} sur \"${interventionEnCoursUi.nomChantier}\" en cours",
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
fun InterventionHistoriqueItem(
    intervention: Intervention,
    dateTimeFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    onEditNote: () -> Unit,
    onEditTime: () -> Unit,
    onDelete: () -> Unit,
    onExportToCalendar: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Augmentation du padding vertical
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top // Alignement en haut pour les notes longues
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { // Ajout d'un padding à droite
            Text(
                "${intervention.typeIntervention} - ${intervention.heureDebut?.let { dateTimeFormat.format(it) } ?: dateTimeFormat.format(intervention.dateIntervention)}",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge // Style un peu plus grand
            )
            if (intervention.heureFin != null && intervention.dureeEffective != null) {
                Text(
                    "Terminée à: ${timeFormat.format(intervention.heureFin!!)}, Durée: ${ChronomailleurService.formatDuration(intervention.dureeEffective!!)}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (intervention.statutIntervention == InterventionStatus.IN_PROGRESS.name) {
                Text("En cours...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            intervention.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Text(
                        "Notes: $notes",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp) // Léger padding pour les notes
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExportToCalendar, modifier = Modifier.size(40.dp)) { // Taille un peu augmentée
                Icon(
                    imageVector = if (intervention.exporteAgenda) Icons.Filled.EventAvailable else Icons.Filled.EventNote,
                    contentDescription = if (intervention.exporteAgenda) "Exporté vers l'agenda" else "Ajouter à l'agenda",
                    tint = if (intervention.exporteAgenda) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) { // Taille un peu augmentée
                    Icon(Icons.Filled.MoreVert, contentDescription = "Plus d'options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifier la note") },
                        onClick = { onEditNote(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Modifier le temps") },
                        onClick = { onEditTime(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnregistrerTempsManuellementDialog(
    chantierNom: String,
    typeIntervention: String,
    interventionExistante: Intervention?,
    onDismissRequest: () -> Unit,
    onConfirm: (dateDebut: Date, dateFin: Date?, dureeMillis: Long?, notes: String?) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }

    var dateDebut by remember { mutableStateOf(interventionExistante?.heureDebut ?: interventionExistante?.dateIntervention ?: Date()) }
    var heureDebut by remember { mutableStateOf(interventionExistante?.heureDebut ?: interventionExistante?.dateIntervention ?: Date()) }

    var dateFinState by remember { mutableStateOf(interventionExistante?.heureFin ?: Date(dateDebut.time + TimeUnit.HOURS.toMillis(1))) }
    var heureFinState by remember { mutableStateOf(interventionExistante?.heureFin ?: Date(dateDebut.time + TimeUnit.HOURS.toMillis(1))) }

    var notes by remember { mutableStateOf(interventionExistante?.notes ?: "") }

    var dureeHeures by remember { mutableStateOf(
        interventionExistante?.dureeEffective?.let { TimeUnit.MILLISECONDS.toHours(it).toString() } ?: "1"
    )}
    var dureeMinutes by remember { mutableStateOf(
        interventionExistante?.dureeEffective?.let { (TimeUnit.MILLISECONDS.toMinutes(it) % 60).toString() } ?: "0"
    )}

    var tabIndex by remember { mutableStateOf(if (interventionExistante?.dureeEffective != null && interventionExistante.heureFin == null) 1 else 0) } // Onglet durée si durée mais pas heure fin

    LaunchedEffect(interventionExistante) {
        interventionExistante?.let {
            dateDebut = it.heureDebut ?: it.dateIntervention
            heureDebut = it.heureDebut ?: it.dateIntervention
            it.heureFin?.let { hf ->
                dateFinState = hf
                heureFinState = hf
                tabIndex = 0
            } ?: run { // Pas d'heure de fin
                val defaultFin = Date(dateDebut.time + TimeUnit.HOURS.toMillis(1)) // Fin par défaut
                dateFinState = defaultFin
                heureFinState = defaultFin
                if (it.dureeEffective != null && it.dureeEffective!! > 0) { // Si durée existe, privilégier cet onglet
                    tabIndex = 1
                } else {
                    tabIndex = 0 // Sinon, onglet heure de fin
                }
            }
            notes = it.notes ?: ""
            it.dureeEffective?.let { de ->
                dureeHeures = TimeUnit.MILLISECONDS.toHours(de).toString()
                dureeMinutes = (TimeUnit.MILLISECONDS.toMinutes(de) % 60).toString()
            } ?: run {
                if(it.heureFin == null){ // Si pas d'heure de fin et pas de durée, mettre durée par défaut
                    dureeHeures = "1"
                    dureeMinutes = "0"
                }
            }
        } ?: run { // Nouvelle intervention
            val now = Date()
            dateDebut = now
            heureDebut = now
            val defaultEndTime = Date(now.time + TimeUnit.HOURS.toMillis(1))
            dateFinState = defaultEndTime
            heureFinState = defaultEndTime
            dureeHeures = "1"
            dureeMinutes = "0"
            notes = ""
            tabIndex = 0
        }
    }

    fun getCombinedDateTime(datePart: Date, timePart: Date): Date {
        val dateCalendar = Calendar.getInstance().apply { time = datePart }
        val timeCalendar = Calendar.getInstance().apply { time = timePart }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    LaunchedEffect(tabIndex, dateDebut, heureDebut, dateFinState, heureFinState) {
        if (tabIndex == 0) { // Si l'onglet "Heure Fin" est sélectionné
            val combinedDebut = getCombinedDateTime(dateDebut, heureDebut)
            val combinedFin = getCombinedDateTime(dateFinState, heureFinState)
            if (combinedFin.after(combinedDebut)) {
                val diff = combinedFin.time - combinedDebut.time
                dureeHeures = TimeUnit.MILLISECONDS.toHours(diff).toString()
                dureeMinutes = (TimeUnit.MILLISECONDS.toMinutes(diff) % 60).toString()
            } else {
                // Optionnel: réinitialiser durée si fin < début, ou afficher erreur
                dureeHeures = "0"
                dureeMinutes = "0"
            }
        }
    }

    LaunchedEffect(tabIndex, dateDebut, heureDebut, dureeHeures, dureeMinutes) {
        if (tabIndex == 1) { // Si l'onglet "Durée" est sélectionné
            val combinedDebut = getCombinedDateTime(dateDebut, heureDebut)
            val h = dureeHeures.toLongOrNull() ?: 0
            val m = dureeMinutes.toLongOrNull() ?: 0
            if (h >= 0 && m >= 0 && m < 60) {
                val dureeMillis = TimeUnit.HOURS.toMillis(h) + TimeUnit.MINUTES.toMillis(m)
                if (dureeMillis >= 0) {
                    val calFin = Calendar.getInstance().apply { time = combinedDebut }
                    calFin.add(Calendar.MILLISECOND, dureeMillis.toInt())
                    dateFinState = calFin.time
                    heureFinState = calFin.time
                }
            }
        }
    }

    var showDatePickerDebut by remember { mutableStateOf(false) }
    val datePickerStateDebut = rememberDatePickerState(
        initialSelectedDateMillis = dateDebut.time,
        selectableDates = NoFutureDatesSelectableDates
    )
    var showDatePickerFin by remember { mutableStateOf(false) }
    val datePickerStateFin = rememberDatePickerState(
        initialSelectedDateMillis = dateFinState.time,
        selectableDates = NoFutureDatesSelectableDates
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (interventionExistante == null) "Enregistrer $typeIntervention" else "Modifier Temps $typeIntervention",
                    style = MaterialTheme.typography.titleLarge
                )
                Text("Chantier: $chantierNom", style = MaterialTheme.typography.titleSmall)

                Text("Début", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDatePickerDebut = true }, modifier = Modifier.weight(1f)) {
                        Text(dateFormat.format(dateDebut))
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance().apply { time = heureDebut }
                        TimePickerDialog(context, { _, h, m ->
                            val currentHourCal = Calendar.getInstance().apply { time = heureDebut }
                            currentHourCal.set(Calendar.HOUR_OF_DAY, h)
                            currentHourCal.set(Calendar.MINUTE, m)
                            heureDebut = currentHourCal.time
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                    }, modifier = Modifier.weight(1f)) {
                        Text(timeFormat.format(heureDebut))
                    }
                }

                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Heure Fin") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Durée") })
                }

                when (tabIndex) {
                    0 -> {
                        Text("Fin", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { showDatePickerFin = true }, modifier = Modifier.weight(1f)) {
                                Text(dateFormat.format(dateFinState))
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                val cal = Calendar.getInstance().apply { time = heureFinState }
                                TimePickerDialog(context, { _, h, m ->
                                    val currentHourCal = Calendar.getInstance().apply { time = heureFinState }
                                    currentHourCal.set(Calendar.HOUR_OF_DAY, h)
                                    currentHourCal.set(Calendar.MINUTE, m)
                                    heureFinState = currentHourCal.time
                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                            }, modifier = Modifier.weight(1f)) {
                                Text(timeFormat.format(heureFinState))
                            }
                        }
                    }
                    1 -> {
                        Text("Durée", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dureeHeures,
                                onValueChange = { if (it.length <= 2 && it.all {c -> c.isDigit()}) dureeHeures = it },
                                label = { Text("Heures") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dureeMinutes,
                                onValueChange = { if (it.length <= 2 && it.all {c -> c.isDigit()} && (it.toIntOrNull() ?: 0) < 60) dureeMinutes = it },
                                label = { Text("Min") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                val combinedDebutCheck = getCombinedDateTime(dateDebut, heureDebut)
                val combinedFinCheck = getCombinedDateTime(dateFinState, heureFinState)
                if (combinedFinCheck.before(combinedDebutCheck) && tabIndex == 0) {
                    Text("L'heure de fin doit être après l'heure de début.", color = MaterialTheme.colorScheme.error)
                }

                if (showDatePickerDebut) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDebut = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerStateDebut.selectedDateMillis?.let { dateDebut = Date(it) }
                                showDatePickerDebut = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showDatePickerDebut = false }) { Text("Annuler") } }
                    ) { DatePicker(state = datePickerStateDebut) }
                }
                if (showDatePickerFin) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerFin = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerStateFin.selectedDateMillis?.let { dateFinState = Date(it) }
                                showDatePickerFin = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showDatePickerFin = false }) { Text("Annuler") } }
                    ) { DatePicker(state = datePickerStateFin) }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Annuler") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalDebut = getCombinedDateTime(dateDebut, heureDebut)
                            var finalFin: Date? = null
                            var finalDuree: Long? = null

                            if (tabIndex == 0) { // Onglet Heure Fin
                                val tempFinalFin = getCombinedDateTime(dateFinState, heureFinState)
                                if (tempFinalFin.after(finalDebut)) {
                                    finalFin = tempFinalFin
                                    finalDuree = finalFin.time - finalDebut.time
                                } else {
                                    Toast.makeText(context, "L'heure de fin doit être après l'heure de début.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                            } else { // Onglet Durée
                                val h = dureeHeures.toLongOrNull() ?: 0
                                val m = dureeMinutes.toLongOrNull() ?: 0
                                if (h < 0 || m < 0 || m >= 60) {
                                    Toast.makeText(context, "Durée invalide.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                finalDuree = TimeUnit.HOURS.toMillis(h) + TimeUnit.MINUTES.toMillis(m)
                                if (finalDuree <= 0 && !(h == 0L && m == 0L && interventionExistante != null) ) { // Permettre 0 si modification et c'était déjà 0
                                    if (finalDuree == 0L && interventionExistante == null) {
                                        Toast.makeText(context, "La durée doit être supérieure à 0.", Toast.LENGTH_LONG).show()
                                        return@Button
                                    } else if (finalDuree <0) {
                                        Toast.makeText(context, "La durée ne peut pas être négative.", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                }
                                finalFin = Date(finalDebut.time + finalDuree!!)
                            }
                            onConfirm(finalDebut, finalFin, finalDuree, notes.ifBlank { null })
                        }
                    ) { Text(if (interventionExistante == null) "Enregistrer" else "Modifier") }
                }
            }
        }
    }
}

@Composable
fun SaisieNotesInterventionDialog(
    notesInitiales: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var notes by remember { mutableStateOf(notesInitiales) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
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
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Annuler")
            }
        }
    )
}


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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChantierDialog(
    chantierInitial: Chantier,
    viewModel: ChantierViewModel, // ViewModel nécessaire pour le géocodage
    onDismissRequest: () -> Unit,
    onConfirm: (Chantier) -> Unit // Le Chantier contiendra lat/lng
) {
    var nomClient by remember(chantierInitial.nomClient) { mutableStateOf(chantierInitial.nomClient) }
    var adresse by remember(chantierInitial.adresse) { mutableStateOf(chantierInitial.adresse ?: "") }
    var tonteActive by remember(chantierInitial.serviceTonteActive) { mutableStateOf(chantierInitial.serviceTonteActive) }
    var tailleActive by remember(chantierInitial.serviceTailleActive) { mutableStateOf(chantierInitial.serviceTailleActive) }
    var desherbageActive by remember(chantierInitial.serviceDesherbageActive) { mutableStateOf(chantierInitial.serviceDesherbageActive) }

    // États pour les coordonnées et le géocodage
    var latitude by remember(chantierInitial.latitude) { mutableStateOf(chantierInitial.latitude) }
    var longitude by remember(chantierInitial.longitude) { mutableStateOf(chantierInitial.longitude) }
    var geocodingInProgress by remember { mutableStateOf(false) }
    var geocodingResultMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Modifier le chantier", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = nomClient,
                    onValueChange = { nomClient = it },
                    label = { Text("Nom du client / Chantier *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = adresse,
                        onValueChange = {
                            adresse = it
                            // Si l'adresse change, on pourrait vouloir réinitialiser les coordonnées
                            // ou indiquer qu'elles pourraient ne plus être valides.
                            // Pour l'instant, on laisse l'utilisateur re-géocoder manuellement.
                            geocodingResultMessage = if (latitude != null && it != chantierInitial.adresse) "Adresse modifiée, re-géocoder si besoin." else null
                        },
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
                                        geocodingResultMessage = "Nouvelles coordonnées trouvées !"
                                    } else {
                                        // Garder les anciennes coordonnées si le nouveau géocodage échoue ?
                                        // latitude = null // Ou chantierInitial.latitude
                                        // longitude = null // Ou chantierInitial.longitude
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
                            Icon(Icons.Filled.MyLocation, contentDescription = "Obtenir/Modifier coordonnées")
                        }
                    }
                }
                geocodingResultMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (latitude != null && geocodingResultMessage?.contains("trouvées") == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
                if (latitude != null && longitude != null) {
                    Text("Lat: %.4f, Lng: %.4f".format(latitude, longitude), style = MaterialTheme.typography.bodySmall)
                }


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
                                    serviceDesherbageActive = desherbageActive,
                                    latitude = latitude, // Sauvegarder les coordonnées
                                    longitude = longitude // Sauvegarder les coordonnées
                                ))
                            }
                        },
                        enabled = nomClient.isNotBlank() && !geocodingInProgress
                    ) { Text("Enregistrer") }
                }
            }
        }
    }
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
                Text("Intervention: ${intervention.typeIntervention} du ${intervention.heureDebut?.let { dateTimeFormat.format(it) } ?: dateTimeFormat.format(intervention.dateIntervention)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(value = notesText, onValueChange = { notesText = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(notesText.ifBlank { null }) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("Annuler") } }
    )
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
        selectableDates = AllDatesSelectableDates // Permet de sélectionner toutes les dates
    )
    var showDatePicker by remember { mutableStateOf(false) }
    // Utiliser selectedDateMillis directement pour la confirmation, pas besoin d'un autre état pour la date affichée
    // var displayedDateMillis by remember(datePickerState.selectedDateMillis) { mutableStateOf(datePickerState.selectedDateMillis ?: System.currentTimeMillis())}


    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    val selectedDate = datePickerState.selectedDateMillis?.let { Date(it) } ?: Date()
                    Text(SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(selectedDate))
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                // La date est déjà dans datePickerState.selectedDateMillis
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
                            val confirmedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis() // Fallback si null
                            onConfirm(chantierId, Date(confirmedDateMillis), notes.ifBlank { null }, desherbagePlanifieInitial)
                        }
                    ) { Text(if (isEditing) "Enregistrer" else "Planifier") }
                }
            }
        }
    }
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
            .padding(vertical = 8.dp), // Padding vertical augmenté
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Event,
            contentDescription = "Date planifiée",
            modifier = Modifier.padding(end = 12.dp), // Espace augmenté
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
            planification.notesPlanification?.let { notes ->
                if (notes.isNotBlank()) {
                    Text("Notes planif.: $notes", style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic), color = itemColor.copy(alpha = 0.8f))
                }
            }
        }
        // Actions
        Row {
            if (!planification.estEffectue) {
                IconButton(onClick = onMarkAsDone, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Done, contentDescription = "Marquer comme effectué", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onExportToCalendar, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (planification.exporteAgenda) Icons.Filled.EventAvailable else Icons.Filled.EventNote,
                    contentDescription = if (planification.exporteAgenda) "Exporté vers l'agenda" else "Ajouter à l'agenda",
                    tint = if (planification.exporteAgenda) MaterialTheme.colorScheme.primary else (if (planification.estEffectue) itemColor else LocalContentColor.current)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Modifier planification", tint = if (planification.estEffectue) itemColor else LocalContentColor.current)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer planification", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}