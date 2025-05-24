package com.example.suivichantierspaysagiste

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chantiers")
data class Chantier(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var nomClient: String,
    var adresse: String? = null, // L'adresse est optionnelle
    var serviceTonteActive: Boolean = true,
    var serviceTailleActive: Boolean = true,
    var serviceDesherbageActive: Boolean = true // NOUVEAU: Service de désherbage
)
