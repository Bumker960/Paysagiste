package com.example.suivichantierspaysagiste

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "desherbages_planifies",
    foreignKeys = [ForeignKey(
        entity = Chantier::class,
        parentColumns = ["id"],
        childColumns = ["chantierId"],
        onDelete = ForeignKey.CASCADE // Si un chantier est supprimé, ses planifications le sont aussi
    )],
    indices = [Index(value = ["chantierId"])]
)
data class DesherbagePlanifie(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chantierId: Long,
    val datePlanifiee: Date,
    var estEffectue: Boolean = false,
    var notesPlanification: String? = null, // Notes spécifiques à cette planification
    var exporteAgenda: Boolean = false // Nouveau champ pour le suivi de l'exportation vers l'agenda
)