package com.example.suivichantierspaysagiste

import java.util.Date

data class TontePrioritaireInfo(
    val chantierId: Long,
    val nomClient: String,
    val derniereTonteDate: Date?, // Peut être null si aucune tonte n'a été enregistrée
    // Les jours écoulés seront calculés plus tard dans le ViewModel ou l'UI
)