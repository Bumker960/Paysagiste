package com.example.suivichantierspaysagiste

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

    // Mode sombre (existant)
    val isDarkModeEnabled: StateFlow<Boolean> = settingsDataStore.darkModeEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setDarkMode(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDarkModeEnabled(isEnabled)
        }
    }

    // Seuils pour les tontes (existant)
    val tonteSeuilVert: StateFlow<Int> = settingsDataStore.tonteSeuilVertFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsDataStore.DEFAULT_TONTE_SEUIL_VERT
        )
    fun setTonteSeuilVert(days: Int) {
        viewModelScope.launch { settingsDataStore.setTonteSeuilVert(days) }
    }

    val tonteSeuilOrange: StateFlow<Int> = settingsDataStore.tonteSeuilOrangeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsDataStore.DEFAULT_TONTE_SEUIL_ORANGE
        )
    fun setTonteSeuilOrange(days: Int) {
        viewModelScope.launch { settingsDataStore.setTonteSeuilOrange(days) }
    }

    // Seuils pour les tailles (existant)
    val tailleSeuil1Vert: StateFlow<Int> = settingsDataStore.tailleSeuil1VertFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsDataStore.DEFAULT_TAILLE_SEUIL_1_VERT
        )
    fun setTailleSeuil1Vert(days: Int) {
        viewModelScope.launch { settingsDataStore.setTailleSeuil1Vert(days) }
    }

    val tailleSeuil2Orange: StateFlow<Int> = settingsDataStore.tailleSeuil2OrangeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsDataStore.DEFAULT_TAILLE_SEUIL_2_ORANGE
        )
    fun setTailleSeuil2Orange(days: Int) {
        viewModelScope.launch { settingsDataStore.setTailleSeuil2Orange(days) }
    }

    // NOUVEAU: StateFlow et fonction pour le seuil de désherbage
    val desherbageSeuilOrangeJoursAvant: StateFlow<Int> = settingsDataStore.desherbageSeuilOrangeJoursAvantFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsDataStore.DEFAULT_DESHERBAGE_SEUIL_ORANGE_JOURS_AVANT
        )
    fun setDesherbageSeuilOrangeJoursAvant(days: Int) {
        viewModelScope.launch { settingsDataStore.setDesherbageSeuilOrangeJoursAvant(days) }
    }
}

// La Factory reste la même
class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for SettingsViewModel")
    }
}