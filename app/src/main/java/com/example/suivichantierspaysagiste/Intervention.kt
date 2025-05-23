package com.example.suivichantierspaysagiste // Adaptez si nécessaire

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
    val typeIntervention: String,
    val dateIntervention: Date,
    var notes: String? = null
)