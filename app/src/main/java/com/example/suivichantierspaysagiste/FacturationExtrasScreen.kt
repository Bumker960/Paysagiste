package com.example.suivichantierspaysagiste

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// IMPORTANT: La définition de NoFutureDatesSelectableDates a été retirée de ce fichier.
// Assurez-vous qu'elle est définie UNE SEULE FOIS dans votre projet (par exemple dans
// ChantierDetailScreen.kt ou, mieux, dans un fichier Utils.kt dédié) et qu'elle est
// rendue accessible (par exemple, avec le modificateur 'internal').
// Vous devrez ensuite l'importer ici. Par exemple :
// import com.example.suivichantierspaysagiste.NoFutureDatesSelectableDates // Ajustez le chemin si nécessaire
// Si vous avez vérifié et que NoFutureDatesSelectableDates est bien 'internal object' dans ChantierDetailScreen.kt
// et que ChantierDetailScreen.kt est dans le même package 'com.example.suivichantierspaysagiste',
// alors l'import ci-dessus devrait fonctionner.


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturationExtrasScreen(
    viewModel: ChantierViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("À Facturer", "Historique")

    val prestationsAFacturer by viewModel.prestationsExtrasAFacturer.collectAsStateWithLifecycle()
    val historiquePrestations by viewModel.prestationsExtrasFactureesHistorique.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var prestationAEditer by remember { mutableStateOf<PrestationHorsContrat?>(null) }
    var showConfirmDeleteDialog by remember { mutableStateOf<PrestationHorsContrat?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }
    val currencyFormat = remember { DecimalFormat("#,##0.00 €") }

    val tousLesChantiersPourDropdown by viewModel.tousLesChantiers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()


    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = {
                        prestationAEditer = null
                        showAddEditDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.Add, "Ajouter une prestation extra")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 ->
                    ListPrestationsExtras(
                        prestations = prestationsAFacturer,
                        dateFormat = dateFormat,
                        currencyFormat = currencyFormat,
                        onEditClick = { prestationDisplay ->
                            scope.launch {
                                prestationAEditer = viewModel.getPrestationExtraById(prestationDisplay.id)
                                showAddEditDialog = true
                            }
                        },
                        onDeleteClick = { prestationDisplay ->
                            scope.launch {
                                val prestationToDelete = viewModel.getPrestationExtraById(prestationDisplay.id)
                                prestationToDelete?.let { showConfirmDeleteDialog = it }
                            }
                        },
                        onMarkAsFactureeClick = { prestationDisplay ->
                            viewModel.marquerPrestationExtraFacturee(prestationDisplay.id)
                        },
                        isHistorique = false,
                        viewModel = viewModel
                    )
                1 ->
                    ListPrestationsExtras(
                        prestations = historiquePrestations,
                        dateFormat = dateFormat,
                        currencyFormat = currencyFormat,
                        onEditClick = { prestationDisplay ->
                            scope.launch {
                                prestationAEditer = viewModel.getPrestationExtraById(prestationDisplay.id)
                                showAddEditDialog = true
                            }
                        },
                        onDeleteClick = { prestationDisplay ->
                            scope.launch {
                                val prestationToDelete = viewModel.getPrestationExtraById(prestationDisplay.id)
                                prestationToDelete?.let { showConfirmDeleteDialog = it }
                            }
                        },
                        onRemettreAFacturerClick = { prestationDisplay ->
                            viewModel.remettrePrestationExtraAFacturer(prestationDisplay.id)
                        },
                        isHistorique = true,
                        viewModel = viewModel
                    )
            }
        }
    }

    if (showAddEditDialog) {
        AddEditPrestationExtraDialog(
            prestationInitiale = prestationAEditer,
            chantiersExistants = tousLesChantiersPourDropdown,
            onDismiss = { showAddEditDialog = false; prestationAEditer = null },
            onConfirm = { prestation ->
                if (prestation.id == 0L) {
                    viewModel.ajouterPrestationExtra(
                        chantierId = prestation.chantierId,
                        referenceChantierTexteLibre = prestation.referenceChantierTexteLibre,
                        description = prestation.description,
                        datePrestation = prestation.datePrestation,
                        montant = prestation.montant,
                        notes = prestation.notes
                    )
                } else {
                    viewModel.updatePrestationExtra(prestation)
                }
                showAddEditDialog = false
                prestationAEditer = null
            }
        )
    }

    showConfirmDeleteDialog?.let { prestationASupprimer ->
        // IMPORTANT: Assurez-vous que ConfirmDeleteDialog est accessible ici.
        // Il est probablement défini dans ChantierDetailScreen.kt.
        // Rendez-le 'internal' ou 'public' dans son fichier d'origine et importez-le,
        // ou déplacez-le dans un fichier utilitaire commun.
        ConfirmDeleteDialog( // Cet appel suppose que ConfirmDeleteDialog est importé et accessible
            title = "Supprimer Prestation Extra",
            text = "Êtes-vous sûr de vouloir supprimer la prestation : \"${prestationASupprimer.description}\" pour ${prestationASupprimer.referenceChantierTexteLibre ?: viewModel.tousLesChantiers.value.find{it.id == prestationASupprimer.chantierId}?.nomClient ?: "N/A"} ?",
            onConfirm = {
                viewModel.deletePrestationExtra(prestationASupprimer)
                showConfirmDeleteDialog = null
            },
            onDismiss = { showConfirmDeleteDialog = null }
        )
    }
}

@Composable
fun ListPrestationsExtras(
    prestations: List<PrestationHorsContratDisplay>,
    dateFormat: SimpleDateFormat,
    currencyFormat: DecimalFormat,
    onEditClick: (PrestationHorsContratDisplay) -> Unit,
    onDeleteClick: (PrestationHorsContratDisplay) -> Unit,
    onMarkAsFactureeClick: ((PrestationHorsContratDisplay) -> Unit)? = null,
    onRemettreAFacturerClick: ((PrestationHorsContratDisplay) -> Unit)? = null,
    isHistorique: Boolean,
    viewModel: ChantierViewModel
) {
    if (prestations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(if (isHistorique) "Aucune prestation facturée dans l'historique." else "Aucune prestation à facturer.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            items(prestations, key = { it.id }) { prestationDisplay ->
                PrestationExtraItemRow(
                    prestationDisplay = prestationDisplay,
                    dateFormat = dateFormat,
                    currencyFormat = currencyFormat,
                    onEdit = { onEditClick(prestationDisplay) },
                    onDelete = { onDeleteClick(prestationDisplay) },
                    onMarkAsFacturee = if (!isHistorique && onMarkAsFactureeClick != null) {
                        { onMarkAsFactureeClick(prestationDisplay) }
                    } else null,
                    onRemettreAFacturer = if (isHistorique && onRemettreAFacturerClick != null) {
                        { onRemettreAFacturerClick(prestationDisplay) }
                    } else null,
                    isHistorique = isHistorique,
                    viewModel = viewModel
                )
                Divider(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
fun PrestationExtraItemRow(
    prestationDisplay: PrestationHorsContratDisplay,
    dateFormat: SimpleDateFormat,
    currencyFormat: DecimalFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsFacturee: (() -> Unit)?,
    onRemettreAFacturer: (() -> Unit)?,
    isHistorique: Boolean,
    viewModel: ChantierViewModel
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(if (isHistorique) 1.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHistorique) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    prestationDisplay.nomAffichageChantier,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    prestationDisplay.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Date: ${dateFormat.format(prestationDisplay.datePrestation)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Montant: ${currencyFormat.format(prestationDisplay.montant)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if(isHistorique) MaterialTheme.colorScheme.primary else OrangeCustom
                    )
                }

                if (!prestationDisplay.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Notes: ${prestationDisplay.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onMarkAsFacturee != null && !isHistorique) {
                        DropdownMenuItem(
                            text = { Text("Marquer comme Facturée") },
                            leadingIcon = { Icon(Icons.Filled.CheckCircleOutline, contentDescription = null) },
                            onClick = { onMarkAsFacturee(); showMenu = false }
                        )
                    }
                    if (onRemettreAFacturer != null && isHistorique) {
                        DropdownMenuItem(
                            text = { Text("Remettre à Facturer") },
                            leadingIcon = { Icon(Icons.Filled.Replay, contentDescription = null) },
                            onClick = { onRemettreAFacturer(); showMenu = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { onEdit(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPrestationExtraDialog(
    prestationInitiale: PrestationHorsContrat?,
    chantiersExistants: List<Chantier>,
    onDismiss: () -> Unit,
    onConfirm: (PrestationHorsContrat) -> Unit
) {
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

    // CORRECTION: Utiliser directement NoFutureDatesSelectableDates (en supposant qu'il est importé et accessible)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = datePrestation.time,
        selectableDates = NoFutureDatesSelectableDates // <-- Changement ici
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
                                        referenceChantierTexteLibre = ""
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
                                statut = StatutFacturationExtras.A_FACTURER.name
                            )
                            onConfirm(finalPrestation)
                        }
                    ) { Text(if (isEditing) "Modifier" else "Ajouter") }
                }
            }
        }
    }
}

// Assurez-vous que ConfirmDeleteDialog est défini et accessible
// S'il est dans ChantierDetailScreen.kt, rendez-le 'internal' ou 'public'
// ou déplacez-le dans un fichier Utils.kt
/*
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
*/
