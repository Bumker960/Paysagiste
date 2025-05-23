package com.example.suivichantierspaysagiste

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey // NOUVEL IMPORT
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")

        // NOUVELLES CLÉS POUR LES SEUILS D'URGENCE
        // Tontes
        val TONTE_SEUIL_VERT_KEY = intPreferencesKey("tonte_seuil_vert")
        val TONTE_SEUIL_ORANGE_KEY = intPreferencesKey("tonte_seuil_orange")
        // Tailles (si 1/2 faite)
        val TAILLE_SEUIL_1_VERT_KEY = intPreferencesKey("taille_seuil_1_vert") // Pour la 2ème taille, seuil vert
        val TAILLE_SEUIL_2_ORANGE_KEY = intPreferencesKey("taille_seuil_2_orange") // Pour la 2ème taille, seuil orange

        // VALEURS PAR DÉFAUT
        const val DEFAULT_TONTE_SEUIL_VERT = 15
        const val DEFAULT_TONTE_SEUIL_ORANGE = 21
        const val DEFAULT_TAILLE_SEUIL_1_VERT = 90
        const val DEFAULT_TAILLE_SEUIL_2_ORANGE = 150
    }

    // Flow pour le mode sombre (existant)
    val darkModeEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    suspend fun setDarkModeEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[DARK_MODE_KEY] = isEnabled
        }
    }

    // NOUVEAU: Flows et fonctions pour les seuils des tontes
    val tonteSeuilVertFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TONTE_SEUIL_VERT_KEY] ?: DEFAULT_TONTE_SEUIL_VERT
    }
    suspend fun setTonteSeuilVert(days: Int) {
        context.dataStore.edit { settings -> settings[TONTE_SEUIL_VERT_KEY] = days }
    }

    val tonteSeuilOrangeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TONTE_SEUIL_ORANGE_KEY] ?: DEFAULT_TONTE_SEUIL_ORANGE
    }
    suspend fun setTonteSeuilOrange(days: Int) {
        context.dataStore.edit { settings -> settings[TONTE_SEUIL_ORANGE_KEY] = days }
    }

    // NOUVEAU: Flows et fonctions pour les seuils des tailles (si 1/2 faite)
    val tailleSeuil1VertFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TAILLE_SEUIL_1_VERT_KEY] ?: DEFAULT_TAILLE_SEUIL_1_VERT
    }
    suspend fun setTailleSeuil1Vert(days: Int) {
        context.dataStore.edit { settings -> settings[TAILLE_SEUIL_1_VERT_KEY] = days }
    }

    val tailleSeuil2OrangeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TAILLE_SEUIL_2_ORANGE_KEY] ?: DEFAULT_TAILLE_SEUIL_2_ORANGE
    }
    suspend fun setTailleSeuil2Orange(days: Int) {
        context.dataStore.edit { settings -> settings[TAILLE_SEUIL_2_ORANGE_KEY] = days }
    }

}