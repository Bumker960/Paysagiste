package com.example.suivichantierspaysagiste

import androidx.compose.ui.graphics.Color
import java.util.Date
import java.util.concurrent.TimeUnit

// La définition de OrangeCustom reste la même
val OrangeCustom = Color(0xFFFFA500)

// MODIFIÉ: getUrgencyColor prend maintenant les seuils en paramètres
fun getUrgencyColor(
    joursEcoules: Long?,
    seuilVert: Int,    // Exemple: settingsViewModel.tonteSeuilVert.value
    seuilOrange: Int   // Exemple: settingsViewModel.tonteSeuilOrange.value
): Color {
    return when {
        joursEcoules == null -> Color.Red // Jamais fait, donc urgent
        joursEcoules <= seuilVert -> Color.Green
        joursEcoules <= seuilOrange -> OrangeCustom // Entre seuilVert + 1 et seuilOrange
        else -> Color.Red // Au-delà de seuilOrange
    }
}

// MODIFIÉ: getUrgencyColorForTaille prend maintenant les seuils en paramètres
fun getUrgencyColorForTaille(
    derniereTailleDate: Date?,
    nombreTaillesCetteAnnee: Int,
    seuil1Vert: Int,     // Exemple: settingsViewModel.tailleSeuil1Vert.value
    seuil2Orange: Int    // Exemple: settingsViewModel.tailleSeuil2Orange.value
): Color {
    if (nombreTaillesCetteAnnee >= 2) {
        return Color.Green
    }
    if (nombreTaillesCetteAnnee == 0) {
        return Color.Red
    }
    if (nombreTaillesCetteAnnee == 1) {
        if (derniereTailleDate == null) {
            return Color.Red
        }
        val joursEcoules = TimeUnit.MILLISECONDS.toDays(
            System.currentTimeMillis() - derniereTailleDate.time
        )
        return when {
            joursEcoules <= seuil1Vert -> Color.Green
            joursEcoules <= seuil2Orange -> OrangeCustom
            else -> Color.Red
        }
    }
    return Color.Gray
}