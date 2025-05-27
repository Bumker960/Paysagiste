package com.example.suivichantierspaysagiste

import android.widget.Toast // IMPORT AJOUTÉ POUR TOAST
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions // Pour gérer l'action du clavier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation // Icône pour géocodage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
// Assurez-vous que cet import est présent pour LatLng si vous l'utilisez directement ici,
// bien que ce soit géré dans le ViewModel pour le géocodage.
// import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch


// Cet objet peut être supprimé si vous utilisez MaterialTheme.colorScheme partout
object ModernColorsScreen { // Cet objet n'est plus utilisé si les couleurs sont gérées par le thème principal
    // val barBackground = Color(0xFF004D40)
    // val contentColor = Color.White
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

    // Le Scaffold principal est maintenant dans MainActivity, donc pas de TopAppBar ici.
    // Le contenu de l'écran est directement dans une Column ou autre layout.
    // Le FAB est aussi géré par le Scaffold principal si besoin, ou reste ici s'il est spécifique à cet écran.
    // Pour cet exemple, on garde le FAB ici car il est lié à l'ajout de chantier.

    Box(modifier = Modifier.fillMaxSize()) { // Utiliser un Box pour positionner le FAB
        Column(
            modifier = Modifier
                // .padding(innerPadding) // innerPadding vient du Scaffold principal dans MainActivity
                .fillMaxSize() // La Column prend toute la place disponible
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
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) { // fillMaxSize pour prendre la place restante
                    items(chantiers, key = { chantier -> chantier.id }) { chantier ->
                        ChantierItem(
                            chantier = chantier,
                            onClick = {
                                navController.navigate("${ScreenDestinations.CHANTIER_DETAIL_ROUTE_PREFIX}/${chantier.id}")
                            }
                        )
                        Divider()
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Ajouter un chantier")
        }

        if (showDialog) {
            AjouterChantierDialog(
                viewModel = viewModel, // Passer le viewModel
                onDismissRequest = { showDialog = false },
                onConfirm = { nom, adresse, tonteActive, tailleActive, desherbageActive, latitude, longitude ->
                    viewModel.ajouterChantier(nom, adresse, tonteActive, tailleActive, desherbageActive, latitude, longitude)
                    viewModel.onSearchQueryChanged("") // Effacer la recherche après ajout
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
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        Text(text = chantier.nomClient, style = MaterialTheme.typography.titleMedium)
        if (chantier.adresse != null && chantier.adresse!!.isNotBlank()) {
            Text(text = chantier.adresse!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (chantier.latitude != null && chantier.longitude != null) {
            Text(
                text = "Lat: %.4f, Lng: %.4f".format(chantier.latitude, chantier.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            if (chantier.serviceTonteActive) {
                ServiceChip("Tonte", MaterialTheme.colorScheme.primary)
            }
            if (chantier.serviceTailleActive) {
                ServiceChip("Taille", MaterialTheme.colorScheme.secondary)
            }
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjouterChantierDialog(
    viewModel: ChantierViewModel,
    onDismissRequest: () -> Unit,
    onConfirm: (nom: String, adresse: String?, tonteActive: Boolean, tailleActive: Boolean, desherbageActive: Boolean, latitude: Double?, longitude: Double?) -> Unit
) {
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
    // val scope = rememberCoroutineScope() // Non utilisé directement ici

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
                                viewModel.geocodeAdresse(adresse) { latLng -> // Appel au ViewModel
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
                                onConfirm(nomClient, adresse.ifBlank { null }, tonteActive, tailleActive, desherbageActive, latitude, longitude)
                            }
                        },
                        enabled = nomClient.isNotBlank() && !geocodingInProgress
                    ) {
                        Text("Ajouter")
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceActivationRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}