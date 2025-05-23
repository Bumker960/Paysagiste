package com.example.suivichantierspaysagiste

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
// import com.example.suivichantierspaysagiste.ui.theme.ModernColors // Laissez cette ligne si ModernColors est bien là

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    navController: NavHostController // Gardé au cas où, même si non utilisé dans cet extrait
) {
    val isDarkMode by settingsViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()

    // Collecter les états pour les seuils d'urgence depuis le SettingsViewModel
    val tonteSeuilVert by settingsViewModel.tonteSeuilVert.collectAsStateWithLifecycle()
    val tonteSeuilOrange by settingsViewModel.tonteSeuilOrange.collectAsStateWithLifecycle()
    val tailleSeuil1Vert by settingsViewModel.tailleSeuil1Vert.collectAsStateWithLifecycle()
    val tailleSeuil2Orange by settingsViewModel.tailleSeuil2Orange.collectAsStateWithLifecycle()

    // États pour contrôler la visibilité des dialogues
    var showTontesDialog by remember { mutableStateOf(false) }
    var showTaillesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                colors = TopAppBarDefaults.topAppBarColors(
                    // Modification temporaire pour résoudre l'erreur ModernColors
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // ANCIENNEMENT: ModernColors.barBackground
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer // ANCIENNEMENT: ModernColors.selectedContent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Section Mode Sombre
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
            Text("Seuils d'Urgence", style = MaterialTheme.typography.titleLarge) // Titre général pour les seuils
            Spacer(modifier = Modifier.height(8.dp))

            // Ligne pour les seuils de Tontes
            SettingSummaryRow(
                title = "Seuils pour Tontes",
                summary = "OK < ${tonteSeuilVert}j, Attention < ${tonteSeuilOrange}j",
                onClick = { showTontesDialog = true }
            )
            Divider()

            // Ligne pour les seuils de Tailles
            SettingSummaryRow(
                title = "Seuils pour Tailles (si 1/2 faite)",
                summary = "OK < ${tailleSeuil1Vert}j, Attention < ${tailleSeuil2Orange}j",
                onClick = { showTaillesDialog = true }
            )
            Divider()

            // Vous pourrez ajouter d'autres options de réglage ici plus tard
        }
    }

    // Dialogue pour les seuils de Tontes
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

    // Dialogue pour les seuils de Tailles
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
}

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
fun UrgencyThresholdsDialog(
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

                errorSeuilVert = null // Réinitialiser les messages d'erreur précédents
                errorSeuilOrange = null

                if (vertInt == null) {
                    errorSeuilVert = "Valeur numérique requise"
                    hasError = true
                }
                if (orangeInt == null) {
                    errorSeuilOrange = "Valeur numérique requise"
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
                if (it.all { char -> char.isDigit() }) {
                    onValueChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = isError
        )
        if (!isError && helperText != null) {
            Text(helperText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}