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


// Helper pour formater la durée en heures et minutes
fun formatMillisToHoursMinutes(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    if (hours == 0L && minutes == 0L) return "0m"
    if (hours == 0L) return "${minutes}m"
    return "${hours}h ${minutes}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyseTempsScreen(
    viewModel: ChantierViewModel,
    navController: NavHostController // Peut être utilisé pour naviguer vers un détail de chantier si besoin
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
                        modifier = Modifier.menuAnchor()
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
                        modifier = Modifier.menuAnchor()
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
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("Temps total passé: ${formatMillisToHoursMinutes(tempsTotalGlobal)}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chantiers les plus chronophages:", style = MaterialTheme.typography.titleMedium)
                if (chantiersPlusChronophages.isEmpty()) {
                    Text("Aucune donnée d'intervention pour cette période.")
                } else {
                    BarChart(
                        data = chantiersPlusChronophages.take(5).associate {
                            it.nomClient to (it.tempsTotalMillis.toFloat() / (1000 * 60 * 60)) // en heures
                        },
                        barColor = MaterialTheme.colorScheme.primary,
                        axisColor = MaterialTheme.colorScheme.onSurface,
                        label = "Heures"
                    )
                    chantiersPlusChronophages.forEach { chantierTemps ->
                        Text("${chantierTemps.nomClient}: ${formatMillisToHoursMinutes(chantierTemps.tempsTotalMillis)}")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Répartition par type d'intervention:", style = MaterialTheme.typography.titleMedium)
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
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ) // Fournir une liste de couleurs
                        )
                    }
                    tempsParTypeInterventionGlobal.forEach { typeTemps ->
                        val pourcentage = if (tempsTotalGlobal > 0) (typeTemps.tempsTotalMillis.toDouble() / tempsTotalGlobal.toDouble() * 100) else 0.0
                        Text("${typeTemps.typeIntervention}: ${formatMillisToHoursMinutes(typeTemps.tempsTotalMillis)} (${String.format("%.1f", pourcentage)}%)")
                    }
                }
            }

        } else {
            // Vue Détaillée pour un chantier
            item {
                detailTempsChantierSelectionne?.let { detail ->
                    SectionTitle("Analyse pour: ${detail.nomClient} (${periodeToString(selectedPeriode)})")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("Temps total sur ce chantier: ${formatMillisToHoursMinutes(detail.tempsTotalMillis)}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Détail par type d'intervention:", style = MaterialTheme.typography.titleSmall)
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
                                }
                                detail.detailsParType.forEach { typeTemps ->
                                    val pourcentage = if (detail.tempsTotalMillis > 0) (typeTemps.tempsTotalMillis.toDouble() / detail.tempsTotalMillis.toDouble() * 100) else 0.0
                                    Text("${typeTemps.typeIntervention}: ${formatMillisToHoursMinutes(typeTemps.tempsTotalMillis)} (${String.format("%.1f", pourcentage)}%)")
                                }
                            }
                        }
                    }
                } ?: run {
                    Text("Chargement des détails du chantier...")
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
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

fun periodeToString(periode: PeriodeSelection): String {
    return when (periode) {
        PeriodeSelection.SEMAINE_EN_COURS -> "Semaine en cours"
        PeriodeSelection.MOIS_EN_COURS -> "Mois en cours"
        PeriodeSelection.ANNEE_EN_COURS -> "Année en cours"
        PeriodeSelection.TOUT -> "Tout"
    }
}

@Composable
fun BarChart(
    data: Map<String, Float>, // Libellé -> Valeur
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    axisColor: Color = MaterialTheme.colorScheme.onBackground,
    label: String = "Valeur"
) {
    if (data.isEmpty()) {
        Text("Aucune donnée pour le graphique.", modifier = modifier.padding(16.dp))
        return
    }

    val maxValue = data.values.maxOrNull() ?: 0f
    val barWidthPx = 60f
    val spaceBetweenBarsPx = 20f
    val chartHeightPx = 200f
    val labelTextSizePx = 12.dp.value // Convert Dp to Px for Paint

    val paint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = labelTextSizePx
            color = axisColor.hashCode() // Need to convert Compose Color to Android Graphics Color
        }
    }
    val textStyle = MaterialTheme.typography.labelSmall


    Box(modifier = modifier.padding(16.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightPx.dp + 40.dp) // Espace pour les libellés en bas
        ) {
            val chartBottomY = chartHeightPx
            val maxBarHeight = chartHeightPx * 0.9f // Laisser un peu d'espace en haut

            data.entries.forEachIndexed { index, entry ->
                val barHeight = if (maxValue > 0) (entry.value / maxValue) * maxBarHeight else 0f
                val barLeft = index * (barWidthPx + spaceBetweenBarsPx)
                val barTop = chartBottomY - barHeight

                // Dessiner la barre
                drawRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidthPx, barHeight)
                )

                // Dessiner le libellé du chantier (X-axis)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        entry.key.take(10) + if(entry.key.length > 10) "..." else "", // Tronquer si trop long
                        barLeft + barWidthPx / 2,
                        chartBottomY + 20.dp.toPx(), // Espace pour le texte sous la barre
                        paint
                    )
                    // Dessiner la valeur au-dessus de la barre
                    canvas.nativeCanvas.drawText(
                        String.format("%.1f %s", entry.value, label.take(1)), // ex: "2.5 h"
                        barLeft + barWidthPx / 2,
                        barTop - 5.dp.toPx(), // Espace au-dessus de la barre
                        paint
                    )
                }
            }
            // Dessiner l'axe Y (simple ligne pour l'instant)
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
    strokeWidth: Float = 40f, // Largeur de l'anneau du donut
    chartSize: Float = 150f // Taille en Dp
) {
    if (data.isEmpty()) {
        Text("Aucune donnée pour le graphique circulaire.", modifier = modifier.padding(16.dp))
        return
    }

    val totalValue = data.values.sum()
    if (totalValue == 0f) {
        Text("Données nulles pour le graphique circulaire.", modifier = modifier.padding(16.dp))
        return
    }

    var startAngle = -90f // Commence en haut

    Box(modifier = modifier.padding(16.dp).size(chartSize.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2f
            val innerRadius = outerRadius - strokeWidth.dp.toPx() // Pour un effet Donut

            data.entries.forEachIndexed { index, entry ->
                val proportion = entry.value / totalValue
                val sweepAngle = 360f * proportion
                val color = colors.getOrElse(index) { Color.Gray } // Couleur par défaut si pas assez de couleurs

                // Dessiner l'arc pour un effet Donut
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false, // Important pour l'effet Donut
                    topLeft = Offset( (size.width - 2 * outerRadius) / 2f, (size.height - 2 * outerRadius) / 2f),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.dp.toPx())
                )
                startAngle += sweepAngle
            }
        }
        // Optionnel: Afficher un texte au centre si ce n'est pas un Donut
        // Text("Total", style = MaterialTheme.typography.labelSmall)
    }
}

