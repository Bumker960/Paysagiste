package com.example.suivichantierspaysagiste

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date
import androidx.room.Index

// Énumération pour le statut de l'intervention
enum class InterventionStatus {
    PLANNED, // Planifiée (pourrait être utilisé plus tard)
    IN_PROGRESS, // En cours
    PAUSED, // En pause (pourrait être utilisé plus tard)
    COMPLETED // Terminée
}

@Entity(
    tableName = "interventions",
    foreignKeys = [ForeignKey(
        entity = Chantier::class,
        parentColumns = ["id"],
        childColumns = ["chantierId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["chantierId"])]
)
data class Intervention(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chantierId: Long,
    val typeIntervention: String, // Sera "Tonte de pelouse", "Taille de haie", ou "Désherbage"

    // dateIntervention est maintenant considérée comme l'heure de début pour les nouvelles interventions chronométrées.
    // Pour les anciennes données, elle conserve sa signification originale.
    var dateIntervention: Date, // Renommé conceptuellement en heureDebut pour les nouvelles interventions

    var notes: String? = null,
    var exporteAgenda: Boolean = false,

    // Nouveaux champs pour le chronomètre
    var heureDebut: Date? = null, // Heure de début explicite (peut être identique à dateIntervention)
    var heureFin: Date? = null,   // Heure de fin de l'intervention
    var dureeEffective: Long? = null, // Durée en millisecondes
    var statutIntervention: String = InterventionStatus.COMPLETED.name // Statut par défaut pour les anciennes et nouvelles interventions manuelles complétées.
) {
    // Constructeur auxiliaire pour la compatibilité avec l'ancien code si nécessaire,
    // bien que Room s'en charge généralement bien.
    // Ce constructeur n'est peut-être pas strictement nécessaire avec Room gérant les valeurs par défaut.
    constructor(
        id: Long = 0,
        chantierId: Long,
        typeIntervention: String,
        dateIntervention: Date, // Ce sera l'heure de début
        notes: String? = null,
        exporteAgenda: Boolean = false
    ) : this(
        id,
        chantierId,
        typeIntervention,
        dateIntervention,
        notes,
        exporteAgenda,
        heureDebut = dateIntervention, // Initialiser heureDebut avec dateIntervention
        heureFin = null,
        dureeEffective = null,
        statutIntervention = InterventionStatus.COMPLETED.name // Les interventions créées ainsi sont considérées comme complétées manuellement
    )
}