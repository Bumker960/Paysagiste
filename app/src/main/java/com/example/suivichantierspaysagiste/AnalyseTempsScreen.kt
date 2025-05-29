package com.example.suivichantierspaysagiste

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll // Ajout pour le défilement horizontal
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState // Ajout pour le défilement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity // Ajout pour convertir Dp en Px si besoin pour Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.util.concurrent.TimeUnit
import android.graphics.Paint as AndroidPaint // Alias pour éviter confusion si Paint de Compose est utilisé
import androidx.compose.foundation.background // Importation ajoutée


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

    val pieChartColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
        Color.Cyan.copy(alpha=0.7f),
        Color.Magenta.copy(alpha=0.7f)
    )


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
                                // MODIFICATION ICI: .take(10) retiré
                                data = chantiersPlusChronophages.associate {
                                    it.nomClient to (it.tempsTotalMillis.toFloat() / (1000 * 60 * 60)) // en heures
                                },
                                barColor = MaterialTheme.colorScheme.primary,
                                axisColor = MaterialTheme.colorScheme.onSurface,
                                label = "h"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // La liste détaillée sous le graphique reste, car le graphique peut être tronqué
                            chantiersPlusChronophages.forEach { chantierTemps ->
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PieChart(
                                        data = tempsParTypeInterventionGlobal.associate {
                                            it.typeIntervention to (it.tempsTotalMillis.toFloat() / totalGlobalPourcentage)
                                        },
                                        colors = pieChartColors,
                                        modifier = Modifier.weight(0.5f) // Le graphique prend de la place
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(0.5f)) { // La légende prend de la place
                                        tempsParTypeInterventionGlobal.forEachIndexed { index, typeTemps ->
                                            val pourcentage = (typeTemps.tempsTotalMillis.toDouble() / totalGlobalPourcentage.toDouble() * 100)
                                            LegendItem(
                                                color = pieChartColors.getOrElse(index) { Color.Gray },
                                                text = "${typeTemps.typeIntervention}: ${formatMillisToHoursMinutes(typeTemps.tempsTotalMillis)} (${String.format("%.1f", pourcentage)}%)"
                                            )
                                        }
                                    }
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PieChart(
                                            data = detail.detailsParType.associate {
                                                it.typeIntervention to (it.tempsTotalMillis.toFloat() / detail.tempsTotalMillis.toFloat())
                                            },
                                            colors = pieChartColors,
                                            modifier = Modifier.weight(0.5f)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(0.5f)) {
                                            detail.detailsParType.forEachIndexed { index, typeTemps ->
                                                val pourcentage = (typeTemps.tempsTotalMillis.toDouble() / detail.tempsTotalMillis.toDouble() * 100)
                                                LegendItem(
                                                    color = pieChartColors.getOrElse(index) { Color.Gray },
                                                    text = "${typeTemps.typeIntervention}: ${formatMillisToHoursMinutes(typeTemps.tempsTotalMillis)} (${String.format("%.1f", pourcentage)}%)"
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    detail.detailsParType.forEach { typeTemps ->
                                        InfoRow(
                                            label = typeTemps.typeIntervention,
                                            value = formatMillisToHoursMinutes(typeTemps.tempsTotalMillis)
                                        )
                                    }
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
        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
    )
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LegendItem(color: Color, text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelMedium)
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
    if (maxValue == 0f && data.isNotEmpty()) {
        Text(
            "Toutes les valeurs sont nulles pour le graphique.",
            modifier = modifier.padding(vertical = 16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    val barWidthPx = 50f
    val spaceBetweenBarsPx = 25f
    val chartHeightPx = 200f
    val labelTextSizePx = 11.dp.value

    val paint = remember {
        AndroidPaint().apply {
            textAlign = AndroidPaint.Align.CENTER
            textSize = labelTextSizePx
        }
    }
    val labelColor = MaterialTheme.colorScheme.onSurface

    val scrollState = rememberScrollState()
    val totalWidth = (data.size * (barWidthPx + spaceBetweenBarsPx) - spaceBetweenBarsPx).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .padding(top = 8.dp, bottom = 8.dp)
            .horizontalScroll(scrollState) // Ajout du défilement horizontal
    ) {
        Canvas(
            modifier = Modifier
                .width(with(LocalDensity.current) { totalWidth.toDp() }) // Largeur dynamique
                .height(chartHeightPx.dp + 50.dp) // Espace pour les libellés et valeurs
        ) {
            val chartBottomY = chartHeightPx
            val maxBarHeight = chartHeightPx * 0.85f

            data.entries.forEachIndexed { index, entry ->
                val barHeight = if (maxValue > 0) (entry.value / maxValue) * maxBarHeight else 0f
                val barLeft = index * (barWidthPx + spaceBetweenBarsPx) + (spaceBetweenBarsPx / 2)
                val barTop = chartBottomY - barHeight

                drawRect(
                    color = barColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barWidthPx, barHeight)
                )
                paint.color = labelColor.hashCode()

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        entry.key.take(7) + if (entry.key.length > 7) ".." else "",
                        barLeft + barWidthPx / 2,
                        chartBottomY + 20.dp.toPx(),
                        paint
                    )
                    if (entry.value > 0) {
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
                end = Offset(totalWidth, chartBottomY), // Ligne jusqu'à la fin du contenu scrollable
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
    strokeWidthDp: Dp = 30.dp,
    chartSizeDp: Dp = 150.dp
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
            .size(chartSizeDp), // Le padding vertical est géré par le parent maintenant
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2f
            val strokeWidthPx = strokeWidthDp.toPx()

            if (strokeWidthPx >= outerRadius * 2) { // Évite un rayon intérieur négatif ou nul
                return@Canvas // Ne rien dessiner si la largeur du trait est trop grande
            }

            data.entries.forEachIndexed { index, entry ->
                val proportion = entry.value / totalValue
                val sweepAngle = 360f * proportion
                val color = colors.getOrElse(index) { Color.LightGray }

                if (sweepAngle > 0.1f) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 1.0f,
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