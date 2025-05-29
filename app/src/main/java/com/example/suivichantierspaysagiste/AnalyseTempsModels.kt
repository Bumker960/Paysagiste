package com.example.suivichantierspaysagiste

import java.util.Date

/**
 * Représente le temps total passé sur un chantier spécifique.
 * Utilisé pour le classement des chantiers les plus chronophages.
 */
data class ChantierTempsTotal(
    val chantierId: Long,
    val nomClient: String,
    val tempsTotalMillis: Long // Durée totale en millisecondes
)

/**
 * Représente le temps total passé pour un type d'intervention spécifique.
 * Utilisé pour la répartition du temps par type d'intervention.
 */
data class TypeInterventionTempsTotal(
    val typeIntervention: String,
    val tempsTotalMillis: Long // Durée totale en millisecondes
)

/**
 * Représente une intervention avec sa durée, potentiellement filtrée par date.
 * Utilisé pour calculer le temps total sur tous les chantiers pour une période.
 */
data class InterventionAvecDuree(
    val id: Long,
    val chantierId: Long,
    val typeIntervention: String,
    val heureDebut: Date?, // Ou dateIntervention si heureDebut est null
    val dureeEffective: Long? // Durée en millisecondes
)

// Enum pour les filtres de période prédéfinis
enum class PeriodeSelection {
    SEMAINE_EN_COURS,
    MOIS_EN_COURS,
    ANNEE_EN_COURS,
    TOUT // Pas de filtre de date
    // On pourrait ajouter PERSONNALISE plus tard
}
