package com.example.suivichantierspaysagiste

import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

val OrangeCustom = Color(0xFFFFA500) // Couleur orange personnalisée

// Fonction pour la couleur d'urgence des TONTES
fun getUrgencyColor(
    joursEcoules: Long?,
    seuilVert: Int,
    seuilOrange: Int
): Color {
    return when {
        joursEcoules == null -> Color.Red // Jamais fait, donc urgent
        joursEcoules <= seuilVert -> Color.Green
        joursEcoules <= seuilOrange -> OrangeCustom
        else -> Color.Red
    }
}

// Fonction pour la couleur d'urgence des TAILLES
fun getUrgencyColorForTaille(
    derniereTailleDate: Date?,
    nombreTaillesCetteAnnee: Int,
    seuil1Vert: Int,
    seuil2Orange: Int
): Color {
    if (nombreTaillesCetteAnnee >= 2) {
        return Color.Green
    }
    if (nombreTaillesCetteAnnee == 0 && derniereTailleDate == null) { // Si jamais taillé et 0 cette année
        return Color.Red
    }
    // Si une seule taille a été faite cette année, ou si aucune n'a été faite mais il y en a eu les années précédentes
    if (derniereTailleDate == null) { // Devrait être couvert par le cas ci-dessus si 0 cette année, mais sécurité
        return Color.Red
    }

    val joursEcoules = TimeUnit.MILLISECONDS.toDays(
        System.currentTimeMillis() - derniereTailleDate.time
    )

    if (nombreTaillesCetteAnnee == 1) { // Une taille faite cette année, on regarde l'urgence pour la deuxième
        return when {
            joursEcoules <= seuil1Vert -> Color.Green
            joursEcoules <= seuil2Orange -> OrangeCustom
            else -> Color.Red
        }
    } else { // Aucune taille faite cette année, mais il y en a eu avant (derniereTailleDate n'est pas null)
        // On pourrait appliquer une logique différente ici, par ex. toujours rouge ou basé sur un seuil annuel.
        // Pour l'instant, on applique la même logique que pour la 2ème taille,
        // mais cela pourrait être affiné.
        return when {
            joursEcoules <= seuil1Vert * 2 -> Color.Green // Exemple: double les seuils si c'est la première de l'année
            joursEcoules <= seuil2Orange * 2 -> OrangeCustom
            else -> Color.Red
        }
    }
}

// NOUVELLE Fonction pour la couleur d'urgence du DESHERBAGE
fun getUrgencyColorForDesherbage(
    prochaineDatePlanifiee: Date?,
    seuilOrangeJoursAvant: Int // Configurable dans les settings, ex: 7 jours
): Color {
    if (prochaineDatePlanifiee == null) {
        return Color.Gray // Aucune planification, ou toutes effectuées
    }

    val aujourdhuiCal = Calendar.getInstance()
    val prochaineDateCal = Calendar.getInstance().apply { time = prochaineDatePlanifiee }

    // Mettre l'heure à 0 pour comparer uniquement les jours
    aujourdhuiCal.set(Calendar.HOUR_OF_DAY, 0); aujourdhuiCal.set(Calendar.MINUTE, 0); aujourdhuiCal.set(Calendar.SECOND, 0); aujourdhuiCal.set(Calendar.MILLISECOND, 0)
    prochaineDateCal.set(Calendar.HOUR_OF_DAY, 0); prochaineDateCal.set(Calendar.MINUTE, 0); prochaineDateCal.set(Calendar.SECOND, 0); prochaineDateCal.set(Calendar.MILLISECOND, 0)

    val diffMillis = prochaineDateCal.timeInMillis - aujourdhuiCal.timeInMillis
    val joursAvantEcheance = TimeUnit.MILLISECONDS.toDays(diffMillis)

    return when {
        joursAvantEcheance < 0 -> Color.Red // En retard
        joursAvantEcheance == 0L -> Color.Red // Aujourd'hui
        joursAvantEcheance <= seuilOrangeJoursAvant -> OrangeCustom // Bientôt
        else -> Color.Green // Planifié plus tard
    }
}