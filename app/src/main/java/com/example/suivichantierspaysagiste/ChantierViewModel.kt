package com.example.suivichantierspaysagiste

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

data class DesherbagePrioritaireUiItem(
    val chantierId: Long,
    val nomClient: String,
    val prochaineDatePlanifiee: Date?,
    val planificationId: Long?,
    val urgencyColor: Color,
    val joursAvantEcheance: Long?
)

// NOUVEAU: Data class pour représenter une intervention en cours avec sa durée écoulée
data class InterventionEnCoursUi(
    val intervention: Intervention,
    val dureeEcouleeFormattee: String,
    val typeInterventionLisible: String // "Tonte", "Taille", "Désherbage"
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
    // val tontesSortOrder: StateFlow<SortOrder> = _tontesSortOrder.asStateFlow() // Déjà défini plus bas

    val tontesPrioritaires: StateFlow<List<TontePrioritaireItem>> =
        combine(
            repository.getTontesPrioritairesFlow(), _searchQuery, _tontesSortOrder,
            tonteSeuilVert, tonteSeuilOrange
        ) { tontesInfoList, query, sortOrder, seuilV, seuilO ->
            val filteredList = if (query.isBlank()) tontesInfoList else tontesInfoList.filter { it.nomClient.contains(query, ignoreCase = true) }
            val items = filteredList.mapNotNull { info ->
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
    // val taillesSortOrder: StateFlow<SortOrder> = _taillesSortOrder.asStateFlow() // Déjà défini plus bas

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
        else repository.countInterventionsOfTypeForChantierFlow(id, "Désherbage")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _desherbagesSortOrder = MutableStateFlow(SortOrder.ASC)
    // val desherbagesSortOrder: StateFlow<SortOrder> = _desherbagesSortOrder.asStateFlow() // Déjà défini plus bas

    val desherbagesPrioritaires: StateFlow<List<DesherbagePrioritaireUiItem>> = combine(
        repository.getAllPendingDesherbagesFlow(),
        tousLesChantiers,
        _searchQuery,
        _desherbagesSortOrder,
        desherbageSeuilOrangeJoursAvant
    ) { pendingPlanifs, chantiers, query, sortOrder, seuilOrange ->
        val chantierMap = chantiers.associateBy { it.id }

        val uiItems = pendingPlanifs.mapNotNull { planif ->
            chantierMap[planif.chantierId]?.let { chantier ->
                if (chantier.serviceDesherbageActive) {
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
        }.filter {
            if (query.isBlank()) true
            else it.nomClient.contains(query, ignoreCase = true)
        }

        when (sortOrder) {
            SortOrder.ASC -> uiItems.sortedWith(compareBy<DesherbagePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenBy { it.joursAvantEcheance ?: Long.MAX_VALUE })
            SortOrder.DESC -> uiItems.sortedWith(compareByDescending<DesherbagePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenByDescending { it.joursAvantEcheance ?: Long.MIN_VALUE })
            SortOrder.NONE -> uiItems.sortedBy { it.nomClient }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // --- NOUVEAU: Logique pour le CHRONOMÈTRE ---
    private val _interventionEnCours = MutableStateFlow<InterventionEnCoursUi?>(null)
    val interventionEnCoursUi: StateFlow<InterventionEnCoursUi?> = _interventionEnCours.asStateFlow()

    private var timerJob: Job? = null

    // Appelé quand on charge un chantier, pour vérifier s'il y avait une intervention en cours pour ce chantier
    private fun verifierInterventionEnCoursExistante(chantierId: Long) {
        viewModelScope.launch {
            // Vérifier pour chaque type d'intervention si une est en cours
            val types = listOf("Tonte de pelouse", "Taille de haie", "Désherbage")
            for (type in types) {
                val interventionExistante = repository.getInterventionEnCours(chantierId, type)
                if (interventionExistante != null) {
                    _interventionEnCours.value = creerInterventionEnCoursUi(interventionExistante)
                    lancerMiseAJourTimer()
                    break // On suppose une seule intervention en cours à la fois par chantier pour simplifier
                }
            }
        }
    }

    private fun creerInterventionEnCoursUi(intervention: Intervention): InterventionEnCoursUi? {
        if (intervention.heureDebut == null) return null
        val dureeEcouleeMillis = System.currentTimeMillis() - intervention.heureDebut!!.time
        val typeLisible = when (intervention.typeIntervention) {
            "Tonte de pelouse" -> "Tonte"
            "Taille de haie" -> "Taille"
            "Désherbage" -> "Désherbage"
            else -> intervention.typeIntervention
        }
        return InterventionEnCoursUi(
            intervention = intervention,
            dureeEcouleeFormattee = formaterDuree(dureeEcouleeMillis),
            typeInterventionLisible = typeLisible
        )
    }


    private fun lancerMiseAJourTimer() {
        timerJob?.cancel() // Annule le job précédent s'il existe
        timerJob = viewModelScope.launch {
            while (true) {
                _interventionEnCours.value?.let { current ->
                    val nouvelleDureeMillis = System.currentTimeMillis() - current.intervention.heureDebut!!.time
                    _interventionEnCours.value = current.copy(dureeEcouleeFormattee = formaterDuree(nouvelleDureeMillis))
                }
                delay(1000) // Met à jour toutes les secondes
            }
        }
    }

    private fun formaterDuree(millis: Long): String {
        val heures = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val secondes = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", heures, minutes, secondes)
    }

    fun demarrerIntervention(chantierId: Long, typeIntervention: String) {
        viewModelScope.launch {
            // Vérifier s'il y a déjà une intervention en cours (globalement ou pour ce type)
            // Pour l'instant, on simplifie : on écrase si une autre était en cours (ou on interdit)
            if (_interventionEnCours.value != null) {
                Toast.makeText(getApplication(), "Une intervention est déjà en cours.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val maintenant = Date()
            val nouvelleIntervention = Intervention(
                chantierId = chantierId,
                typeIntervention = typeIntervention,
                dateIntervention = maintenant, // Sera heureDebut
                heureDebut = maintenant,
                statutIntervention = InterventionStatus.IN_PROGRESS.name
            )
            val idIntervention = repository.insertIntervention(nouvelleIntervention)
            // Récupérer l'intervention avec son ID pour la stocker dans _interventionEnCours
            val interventionAvecId = repository.getInterventionById(idIntervention)
            if (interventionAvecId != null) {
                _interventionEnCours.value = creerInterventionEnCoursUi(interventionAvecId)
                lancerMiseAJourTimer()
            }
        }
    }

    fun terminerInterventionEnCours(notes: String?) {
        viewModelScope.launch {
            _interventionEnCours.value?.intervention?.let { interventionActive ->
                timerJob?.cancel() // Arrête le timer
                val maintenant = Date()
                val duree = maintenant.time - interventionActive.heureDebut!!.time // heureDebut ne devrait pas être null ici

                val interventionTerminee = interventionActive.copy(
                    heureFin = maintenant,
                    dureeEffective = duree,
                    notes = notes?.ifBlank { null } ?: interventionActive.notes, // Conserve les notes si déjà présentes et nouvelles notes vides
                    statutIntervention = InterventionStatus.COMPLETED.name
                )
                repository.updateIntervention(interventionTerminee)
                _interventionEnCours.value = null // Efface l'intervention en cours de l'UI
                // Rafraîchir la liste des interventions pour ce chantier si nécessaire (déjà géré par le Flow)
            }
        }
    }

    // --- Fonctions de modification et d'action ---
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun loadChantierById(chantierId: Long) {
        _selectedChantierId.value = chantierId
        // À chaque chargement de chantier, on vérifie s'il y avait une intervention en cours pour LUI
        _interventionEnCours.value = null // D'abord reset pour éviter d'afficher un chrono d'un autre chantier
        timerJob?.cancel()
        verifierInterventionEnCoursExistante(chantierId)
    }
    fun clearSelectedChantierId() {
        _selectedChantierId.value = null
        _interventionEnCours.value = null // Effacer aussi l'intervention en cours
        timerJob?.cancel() // Arrêter le timer
    }


    val tontesSortOrder: StateFlow<SortOrder> = _tontesSortOrder.asStateFlow()
    val taillesSortOrder: StateFlow<SortOrder> = _taillesSortOrder.asStateFlow()
    val desherbagesSortOrder: StateFlow<SortOrder> = _desherbagesSortOrder.asStateFlow()

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
                serviceDesherbageActive = serviceDesherbageActive
            )
            repository.insertChantier(chantier)
        }
    }
    fun updateChantier(chantier: Chantier) { viewModelScope.launch { repository.updateChantier(chantier) } }
    fun deleteChantier(chantier: Chantier) { viewModelScope.launch { repository.deleteChantier(chantier) } }


    // Les fonctions ajouterTonte, ajouterTailleHaie, ajouterDesherbageIntervention sont maintenant pour l'enregistrement MANUEL
    fun enregistrerInterventionManuelle(chantierId: Long, typeIntervention: String, dateDebut: Date, dateFin: Date?, dureeManuelleMillis: Long?, notes: String?) {
        viewModelScope.launch {
            val heureDebutEffective = dateDebut
            var heureFinEffective: Date? = dateFin
            var dureeEffectiveMillis: Long? = dureeManuelleMillis

            if (dateFin != null && dateDebut.before(dateFin)) {
                dureeEffectiveMillis = dateFin.time - dateDebut.time
            } else if (dureeManuelleMillis != null && dureeManuelleMillis > 0) {
                heureFinEffective = Date(dateDebut.time + dureeManuelleMillis)
            } else {
                // Cas où ni dateFin valide ni durée manuelle n'est fournie.
                // On pourrait choisir de ne pas enregistrer d'heure de fin/durée, ou mettre une durée par défaut (ex: 0 ou 1h)
                // Pour l'instant, on les laisse nulls si non valides.
                if (dureeManuelleMillis != null && dureeManuelleMillis <=0) dureeEffectiveMillis = null // Invalide
                if (dateFin != null && !dateDebut.before(dateFin)) heureFinEffective = null // Invalide
            }


            val intervention = Intervention(
                chantierId = chantierId,
                typeIntervention = typeIntervention,
                dateIntervention = heureDebutEffective, // dateIntervention est l'heure de début
                heureDebut = heureDebutEffective,
                heureFin = heureFinEffective,
                dureeEffective = dureeEffectiveMillis,
                notes = notes,
                statutIntervention = InterventionStatus.COMPLETED.name // Manuel donc complété
            )
            repository.insertIntervention(intervention)
        }
    }


    fun updateInterventionNotes(intervention: Intervention, newNotes: String?) {
        val updatedIntervention = intervention.copy(notes = newNotes?.ifBlank { null })
        viewModelScope.launch { repository.updateIntervention(updatedIntervention) }
    }

    fun modifierTempsIntervention(interventionId: Long, nouvelleHeureDebut: Date, nouvelleHeureFin: Date?, nouvelleDureeMillis: Long?) {
        viewModelScope.launch {
            val interventionExistante = repository.getInterventionById(interventionId) ?: return@launch

            var heureFinCalc: Date? = nouvelleHeureFin
            var dureeCalc: Long? = nouvelleDureeMillis

            if (nouvelleHeureFin != null && nouvelleHeureDebut.before(nouvelleHeureFin)) {
                dureeCalc = nouvelleHeureFin.time - nouvelleHeureDebut.time
            } else if (nouvelleDureeMillis != null && nouvelleDureeMillis > 0) {
                heureFinCalc = Date(nouvelleHeureDebut.time + nouvelleDureeMillis)
            } else {
                // Si ni heureFin valide ni durée valide, on efface les deux pour éviter incohérence
                heureFinCalc = null
                dureeCalc = null
            }

            val updatedIntervention = interventionExistante.copy(
                dateIntervention = nouvelleHeureDebut, // Mettre à jour l'ancienne dateIntervention aussi
                heureDebut = nouvelleHeureDebut,
                heureFin = heureFinCalc,
                dureeEffective = dureeCalc,
                statutIntervention = if (heureFinCalc != null) InterventionStatus.COMPLETED.name else interventionExistante.statutIntervention // Si pas de fin, garde le statut
            )
            repository.updateIntervention(updatedIntervention)
        }
    }


    fun deleteIntervention(intervention: Intervention) { viewModelScope.launch { repository.deleteIntervention(intervention) } }

    fun ajouterDesherbagePlanifie(chantierId: Long, datePlanifiee: Date, notes: String? = null) {
        viewModelScope.launch {
            val count = repository.countDesherbagesPlanifiesForDate(chantierId, datePlanifiee)
            if (count == 0) {
                repository.insertDesherbagePlanifie(DesherbagePlanifie(chantierId = chantierId, datePlanifiee = datePlanifiee, notesPlanification = notes))
            } else {
                Toast.makeText(getApplication(), "Une planification existe déjà pour cette date.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateDesherbagePlanifie(planification: DesherbagePlanifie) {
        viewModelScope.launch { repository.updateDesherbagePlanifie(planification) }
    }

    fun deleteDesherbagePlanifie(planificationId: Long) {
        viewModelScope.launch { repository.deleteDesherbagePlanifieById(planificationId) }
    }

    fun marquerDesherbagePlanifieEffectue(planificationId: Long, dateEffectiveIntervention: Date, notesIntervention: String?) {
        viewModelScope.launch {
            val chantierId = selectedChantier.value?.id ?: return@launch
            // On ne crée plus automatiquement une intervention ici. L'utilisateur doit démarrer/arrêter ou enregistrer manuellement.
            // On marque juste la planification comme faite.
            repository.markDesherbagePlanifieAsDone(planificationId)
            // Si on veut lier, il faudrait que l'enregistrement de l'intervention de désherbage permette de sélectionner une planification.
            // Pour l'instant, on simplifie: marquer comme fait est une action séparée de l'enregistrement de l'intervention de désherbage.
            // L'utilisateur peut enregistrer une intervention de désherbage manuellement ou avec le chrono et y mettre une note faisant référence à la planification.
        }
    }

    fun marquerDesherbagePlanifieNonEffectue(planificationId: Long) {
        viewModelScope.launch { repository.markDesherbagePlanifieAsNotDone(planificationId) }
    }

    fun exporterElementVersAgenda(context: Context, item: Any, chantierNom: String) {
        val intent = Intent(Intent.ACTION_INSERT)
        intent.type = "vnd.android.cursor.item/event"

        var titre: String? = null
        var description: String? = "Événement pour le chantier: $chantierNom"
        var dateDebutMillis: Long? = null
        var dateFinMillis: Long? = null
        var itemId: Long? = null
        var itemType: String? = null

        when (item) {
            is Intervention -> {
                titre = "${item.typeIntervention} - $chantierNom"
                // Utiliser heureDebut si disponible, sinon dateIntervention (qui est heureDebut)
                dateDebutMillis = item.heureDebut?.time ?: item.dateIntervention.time

                // Si heureFin est disponible, l'utiliser. Sinon, ajouter 1h par défaut à heureDebut.
                dateFinMillis = item.heureFin?.time ?: (dateDebutMillis + TimeUnit.HOURS.toMillis(1))

                description += "\nType: ${item.typeIntervention}"
                if (item.dureeEffective != null) {
                    description += "\nDurée: ${formaterDuree(item.dureeEffective!!)}"
                }
                if (!item.notes.isNullOrBlank()) {
                    description += "\nNotes: ${item.notes}"
                }
                itemId = item.id
                itemType = "intervention"
            }
            is DesherbagePlanifie -> {
                titre = "Désherbage Planifié - $chantierNom"
                dateDebutMillis = item.datePlanifiee.time
                dateFinMillis = item.datePlanifiee.time + TimeUnit.HOURS.toMillis(1) // Par défaut 1h
                description += "\nType: Désherbage Planifié"
                if (!item.notesPlanification.isNullOrBlank()) {
                    description += "\nNotes de planification: ${item.notesPlanification}"
                }
                if (item.estEffectue) {
                    description += "\nStatut: Déjà effectué"
                }
                itemId = item.id
                itemType = "planification"
            }
        }

        if (titre != null && dateDebutMillis != null && itemId != null && itemType != null) {
            intent.putExtra(CalendarContract.Events.TITLE, titre)
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dateDebutMillis)
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dateFinMillis)
            intent.putExtra(CalendarContract.Events.DESCRIPTION, description)
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, selectedChantier.value?.adresse ?: "")
            intent.putExtra(CalendarContract.Events.ALL_DAY, false)

            try {
                context.startActivity(intent)
                viewModelScope.launch {
                    if (itemType == "intervention") {
                        repository.marquerInterventionExportee(itemId, true)
                    } else if (itemType == "planification") {
                        repository.marquerDesherbagePlanifieExportee(itemId, true)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Aucune application de calendrier trouvée.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "Impossible de créer l'événement (données manquantes).", Toast.LENGTH_SHORT).show()
        }
    }
}

class ChantierViewModelFactory(
    private val application: Application,
    private val repository: ChantierRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChantierViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChantierViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for ChantierViewModel")
    }
}