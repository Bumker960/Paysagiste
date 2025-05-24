package com.example.suivichantierspaysagiste

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.compose.ui.graphics.Color

// --- Data classes pour l'UI (existantes et nouvelles) ---
data class TontePrioritaireItem(
    val chantierId: Long,
    val nomClient: String,
    val derniereTonteDate: Date?,
    val joursEcoules: Long?,
    val urgencyColor: Color
)

data class TaillePrioritaireUiItem(
    val chantierId: Long,
    val nomClient: String,
    val derniereTailleDate: Date?,
    val nombreTaillesCetteAnnee: Int,
    val joursEcoules: Long?,
    val urgencyColor: Color
)

// NOUVELLE data class pour l'écran des désherbages prioritaires
data class DesherbagePrioritaireUiItem(
    val chantierId: Long,
    val nomClient: String,
    val prochaineDatePlanifiee: Date?,
    val planificationId: Long?, // Pour pouvoir marquer comme fait depuis l'écran prioritaire
    val urgencyColor: Color,
    val joursAvantEcheance: Long? // Pour le tri
)


enum class SortOrder {
    ASC,
    DESC,
    NONE
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChantierViewModel(
    application: Application,
    private val repository: ChantierRepository
) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application.applicationContext)

    // --- StateFlows pour les seuils (lus depuis SettingsDataStore) ---
    private val tonteSeuilVert: StateFlow<Int> = settingsDataStore.tonteSeuilVertFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_TONTE_SEUIL_VERT)
    private val tonteSeuilOrange: StateFlow<Int> = settingsDataStore.tonteSeuilOrangeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_TONTE_SEUIL_ORANGE)
    private val tailleSeuil1Vert: StateFlow<Int> = settingsDataStore.tailleSeuil1VertFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_TAILLE_SEUIL_1_VERT)
    private val tailleSeuil2Orange: StateFlow<Int> = settingsDataStore.tailleSeuil2OrangeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_TAILLE_SEUIL_2_ORANGE)
    // NOUVEAU seuil pour désherbage
    private val desherbageSeuilOrangeJoursAvant: StateFlow<Int> = settingsDataStore.desherbageSeuilOrangeJoursAvantFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_DESHERBAGE_SEUIL_ORANGE_JOURS_AVANT)


    // --- StateFlows généraux et pour le chantier sélectionné ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val tousLesChantiers: StateFlow<List<Chantier>> =
        combine(repository.getAllChantiers(), _searchQuery) { chantiers, query ->
            if (query.isBlank()) chantiers
            else chantiers.filter { it.nomClient.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedChantierId = MutableStateFlow<Long?>(null)
    val selectedChantierId : StateFlow<Long?> = _selectedChantierId.asStateFlow()


    val selectedChantier: StateFlow<Chantier?> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(null as Chantier?)
        else repository.getChantierByIdFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val interventionsDuChantier: StateFlow<List<Intervention>> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getInterventionsForChantier(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())


    // --- Logique et StateFlows pour les TONTES ---
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

    private val _tontesSortOrder = MutableStateFlow(SortOrder.DESC)
    val tontesSortOrder: StateFlow<SortOrder> = _tontesSortOrder.asStateFlow()

    val tontesPrioritaires: StateFlow<List<TontePrioritaireItem>> =
        combine(
            repository.getTontesPrioritairesFlow(), _searchQuery, _tontesSortOrder,
            tonteSeuilVert, tonteSeuilOrange
        ) { tontesInfoList, query, sortOrder, seuilV, seuilO ->
            val filteredList = if (query.isBlank()) tontesInfoList else tontesInfoList.filter { it.nomClient.contains(query, ignoreCase = true) }
            val items = filteredList.mapNotNull { info ->
                // On ne garde que les chantiers où le service de tonte est actif
                // Cette logique pourrait être dans la requête DAO pour plus d'efficacité
                // Pour l'instant on filtre ici après récupération.
                // Il faudrait récupérer l'état du service pour chaque chantier.
                // La requête `getTontesPrioritairesFlow` a été modifiée pour déjà filtrer.
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

    // --- Logique et StateFlows pour les TAILLES ---
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

    private val _taillesSortOrder = MutableStateFlow(SortOrder.DESC)
    val taillesSortOrder: StateFlow<SortOrder> = _taillesSortOrder.asStateFlow()

    private fun getTaillesInfoFlowFromRepository(): Flow<List<TaillePrioritaireDbInfo>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1); /* ... set to start of year ... */
        val startOfYearTimestamp = calendar.timeInMillis
        calendar.set(Calendar.MONTH, Calendar.DECEMBER); /* ... set to end of year ... */
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

    // --- NOUVELLE Logique et StateFlows pour le DESHERBAGE ---
    val desherbagesPlanifiesDuChantier: StateFlow<List<DesherbagePlanifie>> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getDesherbagesPlanifiesForChantier(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val prochainDesherbagePlanifie: StateFlow<DesherbagePlanifie?> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getNextPendingDesherbageForChantier(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val selectedChantierDesherbageUrgencyColor: StateFlow<Color> = combine(
        prochainDesherbagePlanifie, desherbageSeuilOrangeJoursAvant
    ) { prochainePlanif, seuilOrange ->
        getUrgencyColorForDesherbage(prochainePlanif?.datePlanifiee, seuilOrange)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Color.Gray)

    val nombreTotalDesherbagesEffectues: StateFlow<Int> = _selectedChantierId.flatMapLatest { id ->
        if (id == null) flowOf(0)
        else repository.countInterventionsOfTypeForChantierFlow(id, "Désherbage") // Type d'intervention "Désherbage"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _desherbagesSortOrder = MutableStateFlow(SortOrder.ASC) // ASC pour voir les plus proches en premier
    val desherbagesSortOrder: StateFlow<SortOrder> = _desherbagesSortOrder.asStateFlow()

    val desherbagesPrioritaires: StateFlow<List<DesherbagePrioritaireUiItem>> = combine(
        repository.getAllPendingDesherbagesFlow(), // Récupère les DesherbagePlanifie non faits
        tousLesChantiers, // Pour obtenir le nom du client
        _searchQuery,
        _desherbagesSortOrder,
        desherbageSeuilOrangeJoursAvant
    ) { pendingPlanifs, chantiers, query, sortOrder, seuilOrange ->
        val chantierMap = chantiers.associateBy { it.id } // Map pour un accès facile au nomClient

        val uiItems = pendingPlanifs.mapNotNull { planif ->
            chantierMap[planif.chantierId]?.let { chantier ->
                if (chantier.serviceDesherbageActive) { // Vérification si le service est actif (déjà fait dans la query DAO mais double check)
                    val joursAvantEcheance = planif.datePlanifiee.let { date ->
                        val today = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis(); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        val target = Calendar.getInstance().apply { time = date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        TimeUnit.MILLISECONDS.toDays(target.timeInMillis - today.timeInMillis)
                    }
                    DesherbagePrioritaireUiItem(
                        chantierId = chantier.id,
                        nomClient = chantier.nomClient,
                        prochaineDatePlanifiee = planif.datePlanifiee,
                        planificationId = planif.id,
                        urgencyColor = getUrgencyColorForDesherbage(planif.datePlanifiee, seuilOrange),
                        joursAvantEcheance = joursAvantEcheance
                    )
                } else null
            }
        }.filter { // Filtrage par searchQuery
            if (query.isBlank()) true
            else it.nomClient.contains(query, ignoreCase = true)
        }

        when (sortOrder) {
            // ASC: du plus urgent (joursAvantEcheance le plus petit, y compris négatif) au moins urgent
            SortOrder.ASC -> uiItems.sortedWith(compareBy<DesherbagePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenBy { it.joursAvantEcheance ?: Long.MAX_VALUE })
            // DESC: du moins urgent au plus urgent
            SortOrder.DESC -> uiItems.sortedWith(compareByDescending<DesherbagePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenByDescending { it.joursAvantEcheance ?: Long.MIN_VALUE })
            SortOrder.NONE -> uiItems.sortedBy { it.nomClient }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())


    // --- Fonctions de modification et d'action ---
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun loadChantierById(chantierId: Long) { _selectedChantierId.value = chantierId }
    fun clearSelectedChantierId() { _selectedChantierId.value = null }


    fun changerOrdreTriTontes(sortOrder: SortOrder) { _tontesSortOrder.value = sortOrder }
    fun changerOrdreTriTailles(sortOrder: SortOrder) { _taillesSortOrder.value = sortOrder }
    fun changerOrdreTriDesherbages(sortOrder: SortOrder) { _desherbagesSortOrder.value = sortOrder }


    fun ajouterChantier(nomClient: String, adresse: String?, serviceTonteActive: Boolean, serviceTailleActive: Boolean, serviceDesherbageActive: Boolean) {
        viewModelScope.launch {
            val chantier = Chantier(
                nomClient = nomClient,
                adresse = adresse,
                serviceTonteActive = serviceTonteActive,
                serviceTailleActive = serviceTailleActive,
                serviceDesherbageActive = serviceDesherbageActive // NOUVEAU
            )
            repository.insertChantier(chantier)
        }
    }
    fun updateChantier(chantier: Chantier) { viewModelScope.launch { repository.updateChantier(chantier) } }
    fun deleteChantier(chantier: Chantier) { viewModelScope.launch { repository.deleteChantier(chantier) } }


    fun ajouterTonte(chantierId: Long, dateIntervention: Date, notes: String? = null) { viewModelScope.launch { repository.insertIntervention(Intervention(chantierId = chantierId, typeIntervention = "Tonte de pelouse", dateIntervention = dateIntervention, notes = notes)) } }
    fun ajouterTailleHaie(chantierId: Long, dateIntervention: Date, notes: String? = null) { viewModelScope.launch { repository.insertIntervention(Intervention(chantierId = chantierId, typeIntervention = "Taille de haie", dateIntervention = dateIntervention, notes = notes)) } }
    // NOUVELLE fonction pour ajouter une intervention de désherbage
    fun ajouterDesherbageIntervention(chantierId: Long, dateIntervention: Date, notes: String? = null, planificationIdLiee: Long? = null) {
        viewModelScope.launch {
            repository.insertIntervention(Intervention(chantierId = chantierId, typeIntervention = "Désherbage", dateIntervention = dateIntervention, notes = notes))
            planificationIdLiee?.let {
                repository.markDesherbagePlanifieAsDone(it)
            }
        }
    }

    fun updateInterventionNotes(intervention: Intervention, newNotes: String?) {
        val updatedIntervention = intervention.copy(notes = newNotes?.ifBlank { null })
        viewModelScope.launch { repository.updateIntervention(updatedIntervention) }
    }
    fun deleteIntervention(intervention: Intervention) { viewModelScope.launch { repository.deleteIntervention(intervention) } }

    // NOUVELLES fonctions pour gérer DesherbagePlanifie
    fun ajouterDesherbagePlanifie(chantierId: Long, datePlanifiee: Date, notes: String? = null) {
        viewModelScope.launch {
            // Vérifier si une planification existe déjà pour cette date pour éviter les doublons stricts
            val count = repository.countDesherbagesPlanifiesForDate(chantierId, datePlanifiee)
            if (count == 0) {
                repository.insertDesherbagePlanifie(DesherbagePlanifie(chantierId = chantierId, datePlanifiee = datePlanifiee, notesPlanification = notes))
            } else {
                // Gérer le cas du doublon (ex: message à l'utilisateur, non géré ici)
                println("Une planification de désherbage existe déjà pour cette date.")
            }
        }
    }

    fun updateDesherbagePlanifie(planification: DesherbagePlanifie) {
        viewModelScope.launch { repository.updateDesherbagePlanifie(planification) }
    }

    fun deleteDesherbagePlanifie(planificationId: Long) { // Changé pour prendre Long
        viewModelScope.launch { repository.deleteDesherbagePlanifieById(planificationId) }
    }

    fun marquerDesherbagePlanifieEffectue(planificationId: Long, dateEffectiveIntervention: Date, notesIntervention: String?) {
        viewModelScope.launch {
            val chantierId = selectedChantier.value?.id ?: return@launch // Assure-toi que chantierId est disponible
            repository.markDesherbagePlanifieAsDone(planificationId)
            // Optionnel: créer une intervention correspondante si ce n'est pas déjà fait
            ajouterDesherbageIntervention(chantierId, dateEffectiveIntervention, notesIntervention, planificationId)
        }
    }

    fun marquerDesherbagePlanifieNonEffectue(planificationId: Long) {
        viewModelScope.launch { repository.markDesherbagePlanifieAsNotDone(planificationId) }
    }
}

// MODIFIER LA FACTORY pour qu'elle corresponde au constructeur de ChantierViewModel (ajout du desherbagePlanifieDao)
class ChantierViewModelFactory(
    private val application: Application,
    private val repository: ChantierRepository // Le repository a déjà tous les DAOs
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChantierViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChantierViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for ChantierViewModel")
    }
}