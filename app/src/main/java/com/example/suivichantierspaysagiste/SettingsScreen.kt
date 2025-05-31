package com.example.suivichantierspaysagiste

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudUpload // Pour Importer
import androidx.compose.material.icons.filled.Save // Pour Exporter
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    chantierViewModel: ChantierViewModel, // Paramètre ajouté
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // États pour les dialogues et les opérations de sauvegarde/restauration
    val exportState by chantierViewModel.exportState.collectAsStateWithLifecycle()
    val importState by chantierViewModel.importState.collectAsStateWithLifecycle()
    var showConfirmExportDialog by remember { mutableStateOf(false) }
    var showConfirmImportDialog by remember { mutableStateOf(false) }
    var importFileUri by remember { mutableStateOf<Uri?>(null) }


    // Récupération des valeurs des seuils et du mode sombre
    val isDarkMode by settingsViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()
    val tonteSeuilVert by settingsViewModel.tonteSeuilVert.collectAsStateWithLifecycle()
    val tonteSeuilOrange by settingsViewModel.tonteSeuilOrange.collectAsStateWithLifecycle()
    val tailleSeuil1Vert by settingsViewModel.tailleSeuil1Vert.collectAsStateWithLifecycle()
    val tailleSeuil2Orange by settingsViewModel.tailleSeuil2Orange.collectAsStateWithLifecycle()
    val desherbageSeuilOrangeJoursAvant by settingsViewModel.desherbageSeuilOrangeJoursAvant.collectAsStateWithLifecycle()

    var showTontesDialog by remember { mutableStateOf(false) }
    var showTaillesDialog by remember { mutableStateOf(false) }
    var showDesherbageDialog by remember { mutableStateOf(false) }

    // Lanceurs pour la sélection/création de fichiers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri: Uri? ->
            uri?.let {
                chantierViewModel.exportData(it)
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                importFileUri = it // Stocker l'URI pour confirmation
                showConfirmImportDialog = true
            }
        }
    )

    // Observer les messages Toast du ViewModel
    LaunchedEffect(Unit) {
        chantierViewModel.backupToastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            if (message.contains("Veuillez redémarrer l'application")) {
                // Forcer la fermeture de l'application pour que l'utilisateur la redémarre manuellement.
                // C'est une approche simple. Une solution plus élégante pourrait impliquer de
                // naviguer vers un écran de "redémarrage requis" ou de tenter un redémarrage programmatique (complexe).
                (context as? Activity)?.finishAffinity()
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Section Mode Sombre (existante)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mode Sombre",
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = isDarkMode,
                onCheckedChange = { enabled ->
                    settingsViewModel.setDarkMode(enabled)
                }
            )
        }
        Divider()

        Spacer(modifier = Modifier.height(16.dp))
        Text("Seuils d'Urgence", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Lignes pour les seuils (existantes)
        SettingSummaryRow(
            title = "Seuils pour Tontes",
            summary = "OK < ${tonteSeuilVert}j, Attention < ${tonteSeuilOrange}j",
            onClick = { showTontesDialog = true }
        )
        Divider()
        SettingSummaryRow(
            title = "Seuils pour Tailles (si 1/2 faite)",
            summary = "OK < ${tailleSeuil1Vert}j, Attention < ${tailleSeuil2Orange}j",
            onClick = { showTaillesDialog = true }
        )
        Divider()
        SettingSummaryRow(
            title = "Seuil pour Désherbage Planifié",
            summary = "Attention < ${desherbageSeuilOrangeJoursAvant} jours avant échéance",
            onClick = { showDesherbageDialog = true }
        )
        Divider()

        Spacer(modifier = Modifier.height(24.dp))
        Text("Gestion des Données", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Bouton Exporter les données
        Button(
            onClick = { showConfirmExportDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = exportState != BackupState.IN_PROGRESS && importState != BackupState.IN_PROGRESS
        ) {
            Icon(Icons.Filled.Save, contentDescription = "Exporter", modifier = Modifier.padding(end = 8.dp))
            Text("Exporter les données")
        }
        if (exportState == BackupState.IN_PROGRESS) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bouton Importer les données
        Button(
            onClick = { importLauncher.launch(arrayOf("application/zip")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = exportState != BackupState.IN_PROGRESS && importState != BackupState.IN_PROGRESS
        ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = "Importer", modifier = Modifier.padding(end = 8.dp))
            Text("Importer les données")
        }
        if (importState == BackupState.IN_PROGRESS) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }

    // Dialogues pour les seuils (existants)
    if (showTontesDialog) {
        UrgencyThresholdsDialog(
            title = "Modifier Seuils Tontes",
            initialSeuilVert = tonteSeuilVert.toString(),
            labelSeuilVert = "Tonte OK jusqu'à (jours) :",
            initialSeuilOrange = tonteSeuilOrange.toString(),
            labelSeuilOrange = "Tonte Attention jusqu'à (jours) :",
            helperTextOrange = "Doit être > au seuil OK",
            onConfirm = { newVert, newOrange ->
                settingsViewModel.setTonteSeuilVert(newVert)
                settingsViewModel.setTonteSeuilOrange(newOrange)
            },
            onDismissRequest = { showTontesDialog = false }
        )
    }
    if (showTaillesDialog) {
        UrgencyThresholdsDialog(
            title = "Modifier Seuils Tailles (si 1/2 faite)",
            initialSeuilVert = tailleSeuil1Vert.toString(),
            labelSeuilVert = "2ème Taille OK jusqu'à (jours après 1ère) :",
            initialSeuilOrange = tailleSeuil2Orange.toString(),
            labelSeuilOrange = "2ème Taille Attention jusqu'à (jours après 1ère) :",
            helperTextOrange = "Doit être > au seuil OK précédent",
            onConfirm = { newVert, newOrange ->
                settingsViewModel.setTailleSeuil1Vert(newVert)
                settingsViewModel.setTailleSeuil2Orange(newOrange)
            },
            onDismissRequest = { showTaillesDialog = false }
        )
    }
    if (showDesherbageDialog) {
        SingleUrgencyThresholdDialog(
            title = "Modifier Seuil Désherbage",
            initialValue = desherbageSeuilOrangeJoursAvant.toString(),
            label = "Alerte 'Attention' (jours avant échéance) :",
            helperText = "Nombre de jours avant la date planifiée pour passer en orange.",
            onConfirm = { newValue ->
                settingsViewModel.setDesherbageSeuilOrangeJoursAvant(newValue)
            },
            onDismissRequest = { showDesherbageDialog = false }
        )
    }

    // NOUVEAU: Dialogue de confirmation pour l'exportation
    if (showConfirmExportDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmExportDialog = false },
            title = { Text("Confirmer l'exportation") },
            text = { Text("Voulez-vous exporter toutes les données de l'application ? Un fichier ZIP sera créé.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmExportDialog = false
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportLauncher.launch("Sauvegarde_Paysagiste_$timestamp.zip")
                    }
                ) { Text("Exporter") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmExportDialog = false }) { Text("Annuler") }
            }
        )
    }

    // NOUVEAU: Dialogue de confirmation pour l'importation
    if (showConfirmImportDialog && importFileUri != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmImportDialog = false
                importFileUri = null
                chantierViewModel.resetBackupStates()
            },
            title = { Text("Confirmer l'importation") },
            text = { Text("ATTENTION : L'importation écrasera toutes les données actuelles de l'application. Cette action est irréversible. Voulez-vous continuer ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmImportDialog = false
                        importFileUri?.let { chantierViewModel.importData(it) }
                        importFileUri = null // Réinitialiser après usage
                    }
                ) { Text("Importer et Écraser") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmImportDialog = false
                    importFileUri = null
                    chantierViewModel.resetBackupStates()
                }) { Text("Annuler") }
            }
        )
    }
}

// SettingSummaryRow et les Dialogues de seuil restent inchangés par rapport à votre version précédente
// de SettingsScreen.kt, donc je ne les répète pas ici pour la concision.
// Assurez-vous qu'ils sont présents dans votre fichier final.

// Rappel: SettingSummaryRow, UrgencyThresholdsDialog, SingleUrgencyThresholdDialog
// et ThresholdSettingItem doivent être présents dans ce fichier ou importés.
// Je les remets ici pour que le fichier soit complet.

@Composable
fun SettingSummaryRow(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun UrgencyThresholdsDialog( // Pour Tontes et Tailles (deux valeurs)
    title: String,
    initialSeuilVert: String,
    labelSeuilVert: String,
    initialSeuilOrange: String,
    labelSeuilOrange: String,
    helperTextVert: String? = null,
    helperTextOrange: String? = null,
    onConfirm: (newVert: Int, newOrange: Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var tempSeuilVert by remember(initialSeuilVert) { mutableStateOf(initialSeuilVert) }
    var tempSeuilOrange by remember(initialSeuilOrange) { mutableStateOf(initialSeuilOrange) }
    var errorSeuilVert by remember { mutableStateOf<String?>(null) }
    var errorSeuilOrange by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column {
                ThresholdSettingItem(
                    label = labelSeuilVert,
                    value = tempSeuilVert,
                    onValueChange = {
                        tempSeuilVert = it
                        errorSeuilVert = null
                    },
                    helperText = helperTextVert,
                    isError = errorSeuilVert != null
                )
                errorSeuilVert?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                Spacer(modifier = Modifier.height(8.dp))

                ThresholdSettingItem(
                    label = labelSeuilOrange,
                    value = tempSeuilOrange,
                    onValueChange = {
                        tempSeuilOrange = it
                        errorSeuilOrange = null
                    },
                    helperText = helperTextOrange,
                    isError = errorSeuilOrange != null
                )
                errorSeuilOrange?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val vertInt = tempSeuilVert.toIntOrNull()
                val orangeInt = tempSeuilOrange.toIntOrNull()
                var hasError = false

                errorSeuilVert = null
                errorSeuilOrange = null

                if (vertInt == null || vertInt <=0) {
                    errorSeuilVert = "Valeur numérique positive requise"
                    hasError = true
                }
                if (orangeInt == null || orangeInt <= 0) {
                    errorSeuilOrange = "Valeur numérique positive requise"
                    hasError = true
                }

                if (!hasError && vertInt != null && orangeInt != null) {
                    if (orangeInt <= vertInt) {
                        errorSeuilOrange = "Doit être > au seuil OK (${vertInt}j)"
                        hasError = true
                    }
                }

                if (!hasError && vertInt != null && orangeInt != null) {
                    onConfirm(vertInt, orangeInt)
                    onDismissRequest()
                }
            }) {
                Text("Confirmer")
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
fun SingleUrgencyThresholdDialog(
    title: String,
    initialValue: String,
    label: String,
    helperText: String? = null,
    onConfirm: (newValue: Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var tempValue by remember(initialValue) { mutableStateOf(initialValue) }
    var errorValue by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column {
                ThresholdSettingItem(
                    label = label,
                    value = tempValue,
                    onValueChange = {
                        tempValue = it
                        errorValue = null
                    },
                    helperText = helperText,
                    isError = errorValue != null
                )
                errorValue?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val valueInt = tempValue.toIntOrNull()
                var hasError = false
                errorValue = null

                if (valueInt == null || valueInt < 0) { // 0 est permis pour "le jour même"
                    errorValue = "Valeur numérique positive ou nulle requise"
                    hasError = true
                }

                if (!hasError && valueInt != null) {
                    onConfirm(valueInt)
                    onDismissRequest()
                }
            }) {
                Text("Confirmer")
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
fun ThresholdSettingItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    helperText: String? = null,
    isError: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = value,
            onValueChange = {
                // Permet seulement les chiffres, mais la validation toIntOrNull gère les cas non numériques
                if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                    onValueChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = isError,
            supportingText = {
                if (isError) {
                    // Le message d'erreur est affiché à l'extérieur par le dialogue parent
                } else if (helperText != null) {
                    Text(helperText)
                }
            }
        )
    }
}
