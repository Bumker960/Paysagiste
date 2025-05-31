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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput // Ajout pour la détection de clic sur Canvas
import androidx.compose.ui.platform.LocalDensity // Ajout pour convertir Dp en Px si besoin pour Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Ajout pour sp
import androidx.compose.ui.window.Popup // Ajout pour le Tooltip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.util.concurrent.TimeUnit
import android.graphics.Paint as AndroidPaint // Alias pour éviter confusion si Paint de Compose est utilisé
import androidx.compose.foundation.background // Importation ajoutée
import androidx.compose.foundation.gestures.detectTapGestures // Importation AJOUTÉE
import kotlin.math.max


// Helper pour formater la durée en heures et minutes
fun formatMillisToHoursMinutes(millis: Long): String {
    if (millis < 0) return "N/A"
    if (millis == 0L) return "0m"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    if (hours == 0L) return "${minutes}m"
    return "${hours}h ${minutes}m"
}

// Data class pour les informations du tooltip
data class TooltipInfo(
    val text: String,
    val offset: Offset, // Coordonnée du clic sur le canvas, peut être utile pour d'autres logiques
    val barRect: Rect // Rectangle de la barre cliquée, peut être utile pour d'autres logiques
)

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

    var barChartTooltipInfo by remember { mutableStateOf<TooltipInfo?>(null) }

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


    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                        barChartTooltipInfo = null
                                        periodeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
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
                                    barChartTooltipInfo = null
                                    chantierDropdownExpanded = false
                                }
                            )
                            tousLesChantiers.forEach { chantier ->
                                DropdownMenuItem(
                                    text = { Text(chantier.nomClient) },
                                    onClick = {
                                        viewModel.setAnalyseTempsChantierId(chantier.id)
                                        barChartTooltipInfo = null
                                        chantierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (selectedChantierIdForAnalyse == null) {
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
                                    data = chantiersPlusChronophages.associate {
                                        it.nomClient to (it.tempsTotalMillis.toFloat() / (1000 * 60 * 60))
                                    },
                                    barColor = MaterialTheme.colorScheme.primary,
                                    axisColor = MaterialTheme.colorScheme.onSurface,
                                    label = "h",
                                    onBarClick = { barLabel, clickOffset, barRect ->
                                        barChartTooltipInfo = TooltipInfo(barLabel, clickOffset, barRect)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                chantiersPlusChronophages.forEach { chantierTemps ->
                                    InfoRow(
                                        label = chantierTemps.nomClient,
                                        value = formatMillisToHoursMinutes(chantierTemps.tempsTotalMillis),
                                        modifier = Modifier.clickable {
                                            viewModel.setAnalyseTempsChantierId(chantierTemps.chantierId)
                                            barChartTooltipInfo = null
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
                                            modifier = Modifier.weight(0.5f)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(0.5f)) {
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
                    val detail = detailTempsChantierSelectionne
                    if (detail != null) {
                        ChantierAnalysisDetailView(detail, selectedPeriode, pieChartColors)
                    } else {
                        ChantierAnalysisLoadingView()
                    }
                }
            }
        }

        val currentTooltipInfo = barChartTooltipInfo
        if (currentTooltipInfo != null) {
            ChartTooltip(
                tooltipInfo = currentTooltipInfo,
                onDismissRequest = { barChartTooltipInfo = null }
            )
        }
    }
}

@Composable
private fun ChantierAnalysisDetailView(
    detail: AnalyseTempsChantierDetail,
    currentPeriode: PeriodeSelection,
    pieChartColors: List<Color>
) {
    SectionTitle("Analyse pour: ${detail.nomClient} (${periodeToString(currentPeriode)})")
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
}

@Composable
private fun ChantierAnalysisLoadingView() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
        Text("Chargement des détails du chantier...", modifier = Modifier.padding(top = 60.dp))
    }
}


@Composable
private fun ChartTooltip(tooltipInfo: TooltipInfo, onDismissRequest: () -> Unit) {
    val density = LocalDensity.current
    val yOffsetPx = with(density) { (-200).dp.roundToPx() }

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, yOffsetPx),
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier.padding(8.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Text(
                text = tooltipInfo.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    label: String = "", // Ex: "h" pour heures
    onBarClick: (barLabel: String, clickOffset: Offset, barRect: Rect) -> Unit = { _, _, _ -> }
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

    val barWidthPx = 90f // Largeur visuelle de la barre
    val spaceBetweenBarsPx = 50f
    val chartHeightPx = 200f
    val density = LocalDensity.current
    val valueTextSizePx = with(density) { 18.sp.toPx() }

    // Dimensions pour la zone cliquable
    val minClickableHeightPx = with(density) { 48.dp.toPx() } // Hauteur tactile minimale
    val clickableHorizontalPaddingPx = with(density) { 8.dp.toPx() } // Espace tactile supplémentaire sur les côtés

    val valuePaint = remember {
        AndroidPaint().apply {
            textAlign = AndroidPaint.Align.CENTER
            textSize = valueTextSizePx
        }
    }
    val labelColor = MaterialTheme.colorScheme.onSurface

    val scrollState = rememberScrollState()
    val totalWidthRequiredForBars = data.size * (barWidthPx + spaceBetweenBarsPx) - spaceBetweenBarsPx
    val totalWidth = totalWidthRequiredForBars.coerceAtLeast(0f)

    val barRects = remember(data) { mutableStateListOf<Pair<Rect, String>>() }


    Box(
        modifier = modifier
            .padding(top = 8.dp, bottom = 8.dp)
            .horizontalScroll(scrollState)
    ) {
        Canvas(
            modifier = Modifier
                .width(with(LocalDensity.current) { totalWidth.toDp() })
                .height(chartHeightPx.dp + 40.dp)
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        // Itérer en ordre inverse pour que les barres potentiellement superposées
                        // (si padding important) soient gérées correctement (la plus en avant)
                        barRects.asReversed().find { (rect, _) -> rect.contains(tapOffset) }?.let { (barRect, barLabel) ->
                            onBarClick(barLabel, tapOffset, barRect) // barRect ici est le rectangle cliquable
                        }
                    }
                }
        ) {
            barRects.clear()
            val chartBottomY = chartHeightPx
            val maxBarHeight = chartHeightPx * 0.85f // Hauteur max pour la partie visuelle de la barre

            data.entries.forEachIndexed { index, entry ->
                // Calculs pour la barre VISUELLE
                val visualBarHeight = if (maxValue > 0) (entry.value / maxValue) * maxBarHeight else 0f
                val visualBarLeft = index * (barWidthPx + spaceBetweenBarsPx) + (spaceBetweenBarsPx / 2)
                val visualBarTop = chartBottomY - visualBarHeight

                // Calculs pour le RECTANGLE CLIQUABLE
                // La hauteur effective pour le clic est le maximum entre la hauteur visuelle et la hauteur minimale cliquable.
                val effectiveClickableHeight = max(visualBarHeight, minClickableHeightPx)
                // Le haut du rectangle cliquable, s'assurant qu'il s'étend depuis chartBottomY vers le haut.
                val clickableRectTop = chartBottomY - effectiveClickableHeight

                // Définir le rectangle cliquable avec le padding horizontal.
                val clickableRectLeftWithPadding = visualBarLeft - clickableHorizontalPaddingPx
                val clickableRectWidthWithPadding = barWidthPx + (2 * clickableHorizontalPaddingPx)

                val currentClickableBarRect = Rect(
                    left = clickableRectLeftWithPadding,
                    top = clickableRectTop,
                    right = clickableRectLeftWithPadding + clickableRectWidthWithPadding,
                    bottom = chartBottomY // Ancré à la ligne de base du graphique
                )
                barRects.add(Pair(currentClickableBarRect, entry.key))


                // Dessin de la barre VISUELLE (utilise les dimensions visuelles)
                drawRect(
                    color = barColor,
                    topLeft = Offset(visualBarLeft, visualBarTop),
                    size = Size(barWidthPx, visualBarHeight) // Utilise la largeur et hauteur visuelles
                )

                // Dessin du texte de la valeur (comme avant)
                valuePaint.color = labelColor.hashCode()
                if (entry.value > 0) {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            String.format("%.1f%s", entry.value, label),
                            visualBarLeft + barWidthPx / 2, // Centré sur la barre visuelle
                            visualBarTop - 15.dp.toPx(),    // Au-dessus de la barre visuelle
                            valuePaint
                        )
                    }
                }
            }
            // Dessin de l'axe X (comme avant)
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
            .size(chartSizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2f
            val strokeWidthPx = strokeWidthDp.toPx()

            if (strokeWidthPx >= outerRadius * 2) {
                return@Canvas
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