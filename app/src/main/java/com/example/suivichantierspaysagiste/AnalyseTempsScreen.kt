package com.example.suivichantierspaysagiste

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp // Importation ajoutée


// Helper pour formater la durée en heures et minutes
fun formatMillisToHoursMinutes(millis: Long): String {
    if (millis < 0) return "N/A"
    if (millis == 0L) return "0m"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    if (hours == 0L) return "${minutes}m"
    return "${hours}h ${minutes}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyseTempsScreen(
    viewModel: ChantierViewModel,
    navController: NavHostController
) {
    val selectedPeriode by viewModel.selectedAnalysePeriode.collectAsStateWithLifecycle()
    val tousLesChantiers by viewModel.tousLesChantiers.collectAsStateWithLifecycle()
    val selectedChantierIdForAnalyse by viewModel.analyseTempsChantierId.collectAsStateWithLifecycle()

    // Données globales
    val tempsTotalGlobal by viewModel.analyseTempsTotalGlobal.collectAsStateWithLifecycle()
    val chantiersPlusChronophages by viewModel.analyseChantiersPlusChronophages.collectAsStateWithLifecycle()
    val tempsParTypeInterventionGlobal by viewModel.analyseTempsParTypeInterventionGlobal.collectAsStateWithLifecycle()

    // Données pour le chantier sélectionné
    val detailTempsChantierSelectionne by viewModel.analyseDetailTempsPourChantierSelectionne.collectAsStateWithLifecycle()

    var periodeDropdownExpanded by remember { mutableStateOf(false) }
    var chantierDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Sélecteurs ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sélecteur de Période
                ExposedDropdownMenuBox(
                    expanded = periodeDropdownExpanded,
                    onExpandedChange = { periodeDropdownExpanded = !periodeDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = periodeToString(selectedPeriode),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Période") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodeDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = periodeDropdownExpanded,
                        onDismissRequest = { periodeDropdownExpanded = false }
                    ) {
                        PeriodeSelection.values().forEach { periode ->
                            DropdownMenuItem(
                                text = { Text(periodeToString(periode)) },
                                onClick = {
                                    viewModel.setPeriodeSelectionAnalyse(periode)
                                    periodeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sélecteur de Chantier (pour détail)
                ExposedDropdownMenuBox(
                    expanded = chantierDropdownExpanded,
                    onExpandedChange = { chantierDropdownExpanded = !chantierDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = tousLesChantiers.find { it.id == selectedChantierIdForAnalyse }?.nomClient ?: "Tous les chantiers",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chantier (détail)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chantierDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = chantierDropdownExpanded,
                        onDismissRequest = { chantierDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tous les chantiers (Vue Globale)") },
                            onClick = {
                                viewModel.setAnalyseTempsChantierId(null)
                                chantierDropdownExpanded = false
                            }
                        )
                        tousLesChantiers.forEach { chantier ->
                            DropdownMenuItem(
                                text = { Text(chantier.nomClient) },
                                onClick = {
                                    viewModel.setAnalyseTempsChantierId(chantier.id)
                                    chantierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Affichage des Analyses ---
        if (selectedChantierIdForAnalyse == null) {
            // Vue Globale
            item {
                SectionTitle("Analyse Globale (${periodeToString(selectedPeriode)})")
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Temps total passé (tous chantiers): ${formatMillisToHoursMinutes(tempsTotalGlobal)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chantiers les plus chronophages:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()){
                    Column(Modifier.padding(16.dp)) {
                        if (chantiersPlusChronophages.isEmpty()) {
                            Text("Aucune donnée d'intervention pour cette période.")
                        } else {
                            BarChart(
                                data = chantiersPlusChronophages.take(5).associate {
                                    it.nomClient to (it.tempsTotalMillis.toFloat() / (1000 * 60 * 60)) // en heures
                                },
                                barColor = MaterialTheme.colorScheme.primary,
                                axisColor = MaterialTheme.colorScheme.onSurface,
                                label = "h" // Label pour heures
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            chantiersPlusChronophages.forEach { chantierTemps ->
                                // MODIFICATION ICI: InfoRow devient cliquable
                                InfoRow(
                                    label = chantierTemps.nomClient,
                                    value = formatMillisToHoursMinutes(chantierTemps.tempsTotalMillis),
                                    modifier = Modifier.clickable {
                                        viewModel.setAnalyseTempsChantierId(chantierTemps.chantierId)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Répartition par type d'intervention:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()){
                    Column(Modifier.padding(16.dp)) {
                        if (tempsParTypeInterventionGlobal.isEmpty()) {
                            Text("Aucune donnée d'intervention pour cette période.")
                        } else {
                            val totalGlobalPourcentage = tempsParTypeInterventionGlobal.sumOf { it.tempsTotalMillis }.toFloat()
                            if (totalGlobalPourcentage > 0) {
                                PieChart(
                                    data = tempsParTypeInterventionGlobal.associate {
                                        it.typeIntervention to (it.tempsTotalMillis.toFloat() / totalGlobalPourcentage)
                                    },
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                tempsParTypeInterventionGlobal.forEach { typeTemps ->
                                    val pourcentage = (typeTemps.tempsTotalMillis.toDouble() / totalGlobalPourcentage.toDouble() * 100)
                                    // Pour l'instant, cette liste n'est pas cliquable pour la navigation
                                    InfoRow(
                                        label = typeTemps.typeIntervention,
                                        value = "${formatMillisToHoursMinutes(typeTemps.tempsTotalMillis)} (${String.format("%.1f", pourcentage)}%)"
                                    )
                                }
                            } else {
                                Text("Aucun temps enregistré pour cette période.")
                            }
                        }
                    }
                }
            }

        } else {
            // Vue Détaillée pour un chantier
            item {
                detailTempsChantierSelectionne?.let { detail ->
                    SectionTitle("Analyse pour: ${detail.nomClient} (${periodeToString(selectedPeriode)})")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Temps total sur ce chantier: ${formatMillisToHoursMinutes(detail.tempsTotalMillis)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Détail par type d'intervention:", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (detail.detailsParType.isEmpty()) {
                                Text("Aucune intervention enregistrée pour ce chantier sur cette période.")
                            } else {
                                if (detail.tempsTotalMillis > 0) {
                                    PieChart(
                                        data = detail.detailsParType.associate {
                                            it.typeIntervention to (it.tempsTotalMillis.toFloat() / detail.tempsTotalMillis.toFloat())
                                        },
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.tertiary,
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                detail.detailsParType.forEach { typeTemps ->
                                    val pourcentage = if (detail.tempsTotalMillis > 0) (typeTemps.tempsTotalMillis.toDouble() / detail.tempsTotalMillis.toDouble() * 100) else 0.0
                                    // Pour l'instant, cette liste n'est pas cliquable pour la navigation
                                    InfoRow(
                                        label = typeTemps.typeIntervention,
                                        value = "${formatMillisToHoursMinutes(typeTemps.tempsTotalMillis)} (${String.format("%.1f", pourcentage)}%)"
                                    )
                                }
                            }
                        }
                    }
                } ?: run {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Chargement des détails du chantier...", modifier = Modifier.padding(top = 60.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp) // Ajout d'un padding top
    )
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier // Le modificateur est appliqué ici, incluant le .clickable si fourni
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}


fun periodeToString(periode: PeriodeSelection): String {
    return when (periode) {
        PeriodeSelection.SEMAINE_EN_COURS -> "Semaine en cours"
        PeriodeSelection.MOIS_EN_COURS -> "Mois en cours"
        PeriodeSelection.ANNEE_EN_COURS -> "Année en cours"
        PeriodeSelection.TOUT -> "Depuis toujours"
    }
}

@Composable
fun BarChart(
    data: Map<String, Float>, // Libellé -> Valeur (ex: heures)
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    axisColor: Color = MaterialTheme.colorScheme.onSurface,
    label: String = "" // Ex: "h" pour heures
) {
    if (data.isEmpty()) {
        Text(
            "Aucune donnée à afficher pour le graphique.",
            modifier = modifier.padding(vertical = 16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    val maxValue = data.values.maxOrNull() ?: 0f
    if (maxValue == 0f && data.isNotEmpty()) { // Toutes les valeurs sont 0
        Text(
            "Toutes les valeurs sont nulles pour le graphique.",
            modifier = modifier.padding(vertical = 16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }


    val barWidthPx = 50f // Réduit pour plus d'espace si beaucoup de barres
    val spaceBetweenBarsPx = 25f
    val chartHeightPx = 200f
    val labelTextSizePx = 11.dp.value // Taille de texte pour les labels

    val paint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = labelTextSizePx
            // La couleur sera définie par MaterialTheme pour le texte des labels
        }
    }
    val labelColor = MaterialTheme.colorScheme.onSurface


    Box(modifier = modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightPx.dp + 50.dp) // Espace pour les libellés en bas et valeurs en haut
        ) {
            val chartBottomY = chartHeightPx
            val maxBarHeight = chartHeightPx * 0.85f

            data.entries.forEachIndexed { index, entry ->
                val barHeight = if (maxValue > 0) (entry.value / maxValue) * maxBarHeight else 0f
                val barLeft = index * (barWidthPx + spaceBetweenBarsPx) + (spaceBetweenBarsPx/2) // Centrer un peu plus les barres
                val barTop = chartBottomY - barHeight

                drawRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidthPx, barHeight)
                )

                // Utiliser Text de Compose pour les labels pour une meilleure gestion du thème et du style
                // Cette approche est plus complexe à intégrer directement dans Canvas pour le positionnement exact.
                // Pour la simplicité, on garde drawText, mais on utilise la couleur du thème.
                paint.color = labelColor.hashCode()

                drawIntoCanvas { canvas ->
                    // Libellé X (nom du chantier)
                    canvas.nativeCanvas.drawText(
                        entry.key.take(7) + if (entry.key.length > 7) ".." else "",
                        barLeft + barWidthPx / 2,
                        chartBottomY + 20.dp.toPx(),
                        paint
                    )
                    // Valeur au-dessus de la barre
                    if (entry.value > 0) { // N'affiche pas 0.0
                        canvas.nativeCanvas.drawText(
                            String.format("%.1f%s", entry.value, label),
                            barLeft + barWidthPx / 2,
                            barTop - 6.dp.toPx(),
                            paint
                        )
                    }
                }
            }
            drawLine(
                color = axisColor,
                start = Offset(0f, chartBottomY),
                end = Offset(size.width, chartBottomY),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun PieChart(
    data: Map<String, Float>, // Libellé -> Pourcentage (0.0 à 1.0)
    modifier: Modifier = Modifier,
    colors: List<Color>,
    strokeWidthDp: Dp = 30.dp, // Largeur de l'anneau du donut en Dp
    chartSizeDp: Dp = 150.dp // Taille du graphique en Dp
) {
    if (data.isEmpty()) {
        Text(
            "Aucune donnée à afficher pour le graphique circulaire.",
            modifier = modifier.padding(vertical = 16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    val totalValue = data.values.sum()
    if (totalValue == 0f) {
        Text(
            "Données nulles pour le graphique circulaire.",
            modifier = modifier.padding(vertical = 16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    var startAngle = -90f

    Box(
        modifier = modifier
            .padding(vertical = 8.dp) // Ajout de padding vertical
            .size(chartSizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2f
            val strokeWidthPx = strokeWidthDp.toPx()

            data.entries.forEachIndexed { index, entry ->
                val proportion = entry.value / totalValue
                val sweepAngle = 360f * proportion
                val color = colors.getOrElse(index) { Color.LightGray }

                if (sweepAngle > 0.1f) { // Ne pas dessiner des arcs minuscules
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 1.0f, // Petit espace entre les segments
                        useCenter = false,
                        topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
                        size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
                    )
                }
                startAngle += sweepAngle
            }
        }
    }
}
