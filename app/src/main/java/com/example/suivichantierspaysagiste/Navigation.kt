package com.example.suivichantierspaysagiste // Adaptez à votre package

// Objet pour centraliser les définitions de nos routes
object ScreenDestinations {
    const val CHANTIER_LIST_ROUTE = "chantier_list"
    const val CHANTIER_DETAIL_ROUTE_PREFIX = "chantier_detail"
    const val CHANTIER_ID_ARG = "chantierId" // Nom de l'argument pour l'ID du chantier

    // Route complète pour le détail du chantier, incluant l'emplacement pour l'argument
    // Exemple: "chantier_detail/{chantierId}"
    const val CHANTIER_DETAIL_ROUTE_TEMPLATE = "$CHANTIER_DETAIL_ROUTE_PREFIX/{$CHANTIER_ID_ARG}"
    const val TONTES_PRIORITAIRES_ROUTE = "tontes_prioritaires"
    const val TAILLES_PRIORITAIRES_ROUTE = "tailles_prioritaires"
    const val SETTINGS_ROUTE = "settings"
}