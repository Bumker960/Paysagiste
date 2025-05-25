package com.example.suivichantierspaysagiste

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date
import androidx.room.Index

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
    val dateIntervention: Date,
    var notes: String? = null,
    var exporteAgenda: Boolean = false // Nouveau champ pour le suivi de l'exportation vers l'agenda
)