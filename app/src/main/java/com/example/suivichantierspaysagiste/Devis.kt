package com.example.suivichantierspaysagiste

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Représente un devis (fichier PDF) associé à un chantier.
 */
@Entity(
    tableName = "devis", // Nom de la table pour les devis
    foreignKeys = [ForeignKey(
        entity = Chantier::class,
        parentColumns = ["id"],
        childColumns = ["chantierId"],
        onDelete = ForeignKey.CASCADE // Si un chantier est supprimé, ses devis le sont aussi
    )],
    indices = [Index(value = ["chantierId"])]
)
data class Devis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chantierId: Long,
    val nomFichier: String,      // Nom du fichier PDF tel que stocké dans le stockage interne de l'application
    val nomOriginal: String?,    // Nom original du fichier PDF (optionnel, pour l'affichage)
    val dateAjout: Date,         // Date à laquelle le devis a été ajouté à l'application
    var tailleFichier: Long? = null // Taille du fichier en octets (optionnel)
)
