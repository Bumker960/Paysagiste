package com.example.suivichantierspaysagiste

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EuroSymbol // Icône pour la facturation
import androidx.compose.material.icons.filled.Home // NOUVELLE ICÔNE
import androidx.compose.material.icons.filled.LocationOn

// Objet pour centraliser les définitions de nos routes
object ScreenDestinations {
    const val ACCUEIL_ROUTE = "accueil" // NOUVELLE ROUTE
    const val CHANTIER_LIST_ROUTE = "chantier_list"
    const val CHANTIER_DETAIL_ROUTE_PREFIX = "chantier_detail"
    const val CHANTIER_ID_ARG = "chantierId"
    const val CHANTIER_DETAIL_ROUTE_TEMPLATE = "$CHANTIER_DETAIL_ROUTE_PREFIX/{$CHANTIER_ID_ARG}"

    const val TONTES_PRIORITAIRES_ROUTE = "tontes_prioritaires"
    const val TAILLES_PRIORITAIRES_ROUTE = "tailles_prioritaires"
    const val DESHERBAGES_PRIORITAIRES_ROUTE = "desherbages_prioritaires"
    const val SETTINGS_ROUTE = "settings"
    const val MAP_ROUTE = "map"
    const val FACTURATION_EXTRAS_ROUTE = "facturation_extras"
}

