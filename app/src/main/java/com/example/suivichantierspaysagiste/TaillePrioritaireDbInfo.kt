package com.example.suivichantierspaysagiste // Vérifiez que ce package est le même que vos autres classes de données et DAO

import java.util.Date

data class TaillePrioritaireDbInfo(
    val chantierId: Long,
    val nomClient: String,
    val derniereTailleDate: Date?,
    val nombreTaillesCetteAnnee: Int
)