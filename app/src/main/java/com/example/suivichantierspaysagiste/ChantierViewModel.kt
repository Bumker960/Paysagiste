package com.example.suivichantierspaysagiste

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.AndroidViewModel // MODIFIÉ pour prendre Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi // Assurez-vous que cet import est là
import kotlinx.coroutines.flow.* // Import générique pour tous les opérateurs de Flow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.compose.ui.graphics.Color // Assurez-vous de cet import

// --- Data classes (doivent être définies) ---
data class TontePrioritaireItem(
    val chantierId: Long,
    val nomClient: String,
    val derniereTonteDate: Date?,
    val joursEcoules: Long?,
    val urgencyColor: Color // Champ pour la couleur d'urgence
)

data class TaillePrioritaireUiItem(
    val chantierId: Long,
    val nomClient: String,
    val derniereTailleDate: Date?,
    val nombreTaillesCetteAnnee: Int,
    val joursEcoules: Long?,
    val urgencyColor: Color
)
enum class SortOrder {
    ASC, // Jours écoulés croissants (le plus récent en premier si on trie par date)
    DESC, // Jours écoulés décroissants (le plus ancien en premier)
    NONE // Pas de tri spécifique ou tri par défaut (ex: par nom de client)
}

// N'oubliez pas cette annotation si vous utilisez flatMapLatest
@OptIn(ExperimentalCoroutinesApi::class)
class ChantierViewModel(
    application: Application, // Reçoit l'application
    private val repository: ChantierRepository
) : AndroidViewModel(application) { // Hérite de AndroidViewModel

    // ---- INITIALISATION DE settingsDataStore ICI ----
    private val settingsDataStore = SettingsDataStore(application.applicationContext)

    // --- StateFlows pour les seuils (lus depuis SettingsDataStore) ---
    // Ces StateFlows utilisent settingsDataStore, donc settingsDataStore doit être défini AVANT.
    private val tonteSeuilVert: StateFlow<Int> = settingsDataStore.tonteSeuilVertFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_TONTE_SEUIL_VERT)

    private val tonteSeuilOrange: StateFlow<Int> = settingsDataStore.tonteSeuilOrangeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_TONTE_SEUIL_ORANGE)

    private val tailleSeuil1Vert: StateFlow<Int> = settingsDataStore.tailleSeuil1VertFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_TAILLE_SEUIL_1_VERT)

    private val tailleSeuil2Orange: StateFlow<Int> = settingsDataStore.tailleSeuil2OrangeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_TAILLE_SEUIL_2_ORANGE)

    // --- Autres StateFlows ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val tousLesChantiers: StateFlow<List<Chantier>> =
        combine(repository.getAllChantiers(), _searchQuery) { chantiers, query ->
            if (query.isBlank()) chantiers
            else chantiers.filter { it.nomClient.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedChantierId = MutableStateFlow<Long?>(null)

    val selectedChantier: StateFlow<Chantier?> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(null as Chantier?)
        else repository.getChantierByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    // ... (le reste de vos StateFlows pour derniereTonte, nombreTotalTontes, selectedChantierTonteUrgencyColor, etc.
    //      ils doivent aussi être APRÈS la définition de _selectedChantierId et, si applicable, settingsDataStore)

    val derniereTonte: StateFlow<Intervention?> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(null as Intervention?)
        else repository.getLastInterventionOfTypeForChantierFlow(id, "Tonte de pelouse")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val nombreTotalTontes: StateFlow<Int> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(0)
        else repository.countInterventionsOfTypeForChantierFlow(id, "Tonte de pelouse")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val selectedChantierTonteUrgencyColor: StateFlow<Color> = combine(
        derniereTonte, tonteSeuilVert, tonteSeuilOrange
    ) { lastTonte, seuilV, seuilO ->
        val jours = lastTonte?.dateIntervention?.let { TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.time) }
        getUrgencyColor(jours, seuilV, seuilO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.Transparent)

    val derniereTaille: StateFlow<Intervention?> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(null as Intervention?)
        else repository.getLastInterventionOfTypeForChantierFlow(id, "Taille de haie")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val nombreTotalTailles: StateFlow<Int> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(0)
        else repository.countInterventionsOfTypeForChantierFlow(id, "Taille de haie")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val nombreTaillesCetteAnnee: StateFlow<Int> = _selectedChantierId.flatMapLatest { chantierId ->
        if (chantierId == null) { flowOf(0) }
        else {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_YEAR, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val startOfYearTimestamp = calendar.timeInMillis
            calendar.set(Calendar.MONTH, Calendar.DECEMBER); calendar.set(Calendar.DAY_OF_MONTH, 31); calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
            val endOfYearTimestamp = calendar.timeInMillis
            repository.getTaillesHaieBetweenDatesCountFlow(chantierId, Date(startOfYearTimestamp), Date(endOfYearTimestamp))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val selectedChantierTailleUrgencyColor: StateFlow<Color> = combine(
        derniereTaille, nombreTaillesCetteAnnee, tailleSeuil1Vert, tailleSeuil2Orange
    ) { lastTaille, countThisYear, seuil1V, seuil2O ->
        getUrgencyColorForTaille(lastTaille?.dateIntervention, countThisYear, seuil1V, seuil2O)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.Transparent)

    val interventionsDuChantier: StateFlow<List<Intervention>> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getInterventionsForChantier(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _tontesSortOrder = MutableStateFlow(SortOrder.DESC)
    val tontesSortOrder: StateFlow<SortOrder> = _tontesSortOrder.asStateFlow()

    val tontesPrioritaires: StateFlow<List<TontePrioritaireItem>> =
        combine(
            repository.getTontesPrioritairesFlow(), _searchQuery, _tontesSortOrder,
            tonteSeuilVert, tonteSeuilOrange
        ) { tontesInfoList, query, sortOrder, seuilV, seuilO ->
            val filteredList = if (query.isBlank()) tontesInfoList else tontesInfoList.filter { it.nomClient.contains(query, ignoreCase = true) }
            val items = filteredList.map { info ->
                val joursEcoules = info.derniereTonteDate?.let { date -> TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - date.time) }
                TontePrioritaireItem(
                    chantierId = info.chantierId,
                    nomClient = info.nomClient,
                    derniereTonteDate = info.derniereTonteDate,
                    joursEcoules = joursEcoules,
                    urgencyColor = getUrgencyColor(joursEcoules, seuilV, seuilO)
                )
            }
            when (sortOrder) {
                SortOrder.ASC -> items.sortedBy { it.joursEcoules ?: Long.MAX_VALUE }
                SortOrder.DESC -> items.sortedByDescending { it.joursEcoules ?: -1L }
                SortOrder.NONE -> items.sortedBy { it.nomClient }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _taillesSortOrder = MutableStateFlow(SortOrder.DESC)
    val taillesSortOrder: StateFlow<SortOrder> = _taillesSortOrder.asStateFlow()

    private fun getTaillesInfoFlowFromRepository(): Flow<List<TaillePrioritaireDbInfo>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startOfYearTimestamp = calendar.timeInMillis
        calendar.set(Calendar.MONTH, Calendar.DECEMBER); calendar.set(Calendar.DAY_OF_MONTH, 31); calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val endOfYearTimestamp = calendar.timeInMillis
        return repository.getTaillesPrioritairesInfoFlow(startOfYearTimestamp, endOfYearTimestamp)
    }
    private fun getPriorityScore(color: Color): Int {
        return when (color) {
            Color.Red -> 0
            OrangeCustom -> 1
            Color.Green -> 2
            else -> 3
        }
    }

    val taillesPrioritaires: StateFlow<List<TaillePrioritaireUiItem>> =
        combine(
            getTaillesInfoFlowFromRepository(), _searchQuery, _taillesSortOrder,
            tailleSeuil1Vert, tailleSeuil2Orange
        ) { taillesDbInfoList, query, sortOrder, seuil1V, seuil2O ->
            val filteredList = if (query.isBlank()) taillesDbInfoList else taillesDbInfoList.filter { it.nomClient.contains(query, ignoreCase = true) }
            val uiItems = filteredList.map { info ->
                val joursEcoules = info.derniereTailleDate?.let { date -> TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - date.time) }
                TaillePrioritaireUiItem(
                    chantierId = info.chantierId,
                    nomClient = info.nomClient,
                    derniereTailleDate = info.derniereTailleDate,
                    nombreTaillesCetteAnnee = info.nombreTaillesCetteAnnee,
                    joursEcoules = joursEcoules,
                    urgencyColor = getUrgencyColorForTaille(info.derniereTailleDate, info.nombreTaillesCetteAnnee, seuil1V, seuil2O)
                )
            }
            when (sortOrder) {
                SortOrder.ASC -> uiItems.sortedWith( compareByDescending<TaillePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenBy { it.joursEcoules ?: Long.MIN_VALUE })
                SortOrder.DESC -> uiItems.sortedWith( compareBy<TaillePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenByDescending { it.joursEcoules ?: Long.MAX_VALUE })
                SortOrder.NONE -> uiItems.sortedBy { it.nomClient }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // --- Fonctions de modification ---
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun loadChantierById(chantierId: Long) { _selectedChantierId.value = chantierId }
    fun changerOrdreTriTontes(sortOrder: SortOrder) { _tontesSortOrder.value = sortOrder }
    fun changerOrdreTriTailles(sortOrder: SortOrder) { _taillesSortOrder.value = sortOrder }

    fun ajouterChantier(nomClient: String, adresse: String?, serviceTonteActive: Boolean, serviceTailleActive: Boolean) { viewModelScope.launch { repository.insertChantier(Chantier(nomClient = nomClient, adresse = adresse, serviceTonteActive = serviceTonteActive, serviceTailleActive = serviceTailleActive)) } }
    fun ajouterTonte(chantierId: Long, dateIntervention: Date, notes: String? = null) { viewModelScope.launch { repository.insertIntervention(Intervention(chantierId = chantierId, typeIntervention = "Tonte de pelouse", dateIntervention = dateIntervention, notes = notes)) } }
    fun ajouterTailleHaie(chantierId: Long, dateIntervention: Date, notes: String? = null) { viewModelScope.launch { repository.insertIntervention(Intervention(chantierId = chantierId, typeIntervention = "Taille de haie", dateIntervention = dateIntervention, notes = notes)) } }
    fun updateChantier(chantier: Chantier) { viewModelScope.launch { repository.updateChantier(chantier) } }
    fun deleteChantier(chantier: Chantier) { viewModelScope.launch { repository.deleteChantier(chantier) } }
    fun deleteIntervention(intervention: Intervention) { viewModelScope.launch { repository.deleteIntervention(intervention) } }
}

// MODIFIER LA FACTORY pour qu'elle corresponde au constructeur de ChantierViewModel
class ChantierViewModelFactory(
    private val application: Application,
    private val repository: ChantierRepository
    // settingsDataStore n'est plus passé ici car le ViewModel l'instancie lui-même
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChantierViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Le ViewModel prend 'application' et 'repository'
            return ChantierViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for ChantierViewModel")
    }
}
