package com.example.suivichantierspaysagiste

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.text.DecimalFormat

// --- Data classes pour l'UI (existantes et nouvelles) ---
data class TontePrioritaireItem(
    val chantierId: Long,
    val nomClient: String,
    val derniereTonteDate: Date?,
    val joursEcoules: Long?,
    val urgencyColor: Color,
    val latitude: Double?,
    val longitude: Double?
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
    val joursAvantEcheance: Long? // Négatif si en retard, positif si à venir, 0 si aujourd'hui
)

data class InterventionEnCoursUi(
    val interventionId: Long,
    val chantierId: Long,
    val typeIntervention: String,
    val nomChantier: String,
    val heureDebut: Long,
    val dureeEcouleeFormattee: String,
    val typeInterventionLisible: String
)

data class NearestSiteResult(
    val chantier: Chantier?,
    val message: String,
    val latLng: LatLng?
)

data class MapChantierData(
    val chantier: Chantier,
    val derniereTonteDate: Date?,
    val joursDepuisDerniereTonte: Long?,
    val tonteUrgencyColor: Color,
    val markerHue: Float
)

// NOUVEAU: Pour la page d'accueil
data class ApercuTachePrioritaireItem(
    val chantierId: Long,
    val nomClient: String,
    val detail: String, // Ex: "Dernière tonte le JJ/MM/AAAA, X jours écoulés" ou "Planifié pour..."
    val urgencyColor: Color
)

data class ResumeFacturationAccueil(
    val nombrePrestationsAFacturer: Int,
    val montantTotalEstimeAFacturer: Double
)


enum class SortOrder {
    ASC,
    DESC,
    NONE
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChantierViewModel(
    private val applicationContext: Application,
    private val repository: ChantierRepository
) : AndroidViewModel(applicationContext) {

    private val settingsDataStore = SettingsDataStore(applicationContext)

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
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
            listOf(
                repository.getTontesPrioritairesFlow(),
                tousLesChantiers,
                _searchQuery,
                _tontesSortOrder,
                tonteSeuilVert,
                tonteSeuilOrange
            )
        ) { values: Array<*> ->
            @Suppress("UNCHECKED_CAST")
            val tontesInfoList = values[0] as List<TontePrioritaireInfo>
            @Suppress("UNCHECKED_CAST")
            val allChantiersList = values[1] as List<Chantier>
            @Suppress("UNCHECKED_CAST")
            val query = values[2] as String
            @Suppress("UNCHECKED_CAST")
            val sortOrder = values[3] as SortOrder
            @Suppress("UNCHECKED_CAST")
            val seuilV = values[4] as Int
            @Suppress("UNCHECKED_CAST")
            val seuilO = values[5] as Int

            val chantiersMap = allChantiersList.associateBy { chantier: Chantier -> chantier.id }

            val filteredTontesInfo = if (query.isBlank()) {
                tontesInfoList
            } else {
                tontesInfoList.filter { tonteInfo: TontePrioritaireInfo -> tonteInfo.nomClient.contains(query, ignoreCase = true) }
            }

            val items = filteredTontesInfo.mapNotNull { tonteInfo: TontePrioritaireInfo ->
                val chantierDetail = chantiersMap[tonteInfo.chantierId]
                if (chantierDetail?.serviceTonteActive == true) {
                    val joursEcoules = tonteInfo.derniereTonteDate?.let { date ->
                        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - date.time)
                    }
                    TontePrioritaireItem(
                        chantierId = tonteInfo.chantierId,
                        nomClient = tonteInfo.nomClient,
                        derniereTonteDate = tonteInfo.derniereTonteDate,
                        joursEcoules = joursEcoules,
                        urgencyColor = getUrgencyColor(joursEcoules, seuilV, seuilO),
                        latitude = chantierDetail.latitude,
                        longitude = chantierDetail.longitude
                    )
                } else null
            }

            when (sortOrder) {
                SortOrder.ASC -> items.sortedBy { it.joursEcoules ?: Long.MAX_VALUE }
                SortOrder.DESC -> items.sortedByDescending { it.joursEcoules ?: -1L }
                SortOrder.NONE -> items.sortedBy { it.nomClient }
                // else -> items // Ajout pour exhaustivité si l'IDE le demande encore
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
            listOf(
                getTaillesInfoFlowFromRepository(),
                tousLesChantiers,
                _searchQuery,
                _taillesSortOrder,
                tailleSeuil1Vert,
                tailleSeuil2Orange
            )
        ) { values: Array<*> ->
            @Suppress("UNCHECKED_CAST")
            val taillesDbInfoList = values[0] as List<TaillePrioritaireDbInfo>
            @Suppress("UNCHECKED_CAST")
            val allChantiersList = values[1] as List<Chantier>
            @Suppress("UNCHECKED_CAST")
            val query = values[2] as String
            @Suppress("UNCHECKED_CAST")
            val sortOrder = values[3] as SortOrder
            @Suppress("UNCHECKED_CAST")
            val seuil1V = values[4] as Int
            @Suppress("UNCHECKED_CAST")
            val seuil2O = values[5] as Int

            val chantiersMap = allChantiersList.associateBy { chantier: Chantier -> chantier.id }
            val filteredList = if (query.isBlank()) taillesDbInfoList else taillesDbInfoList.filter { tailleInfo: TaillePrioritaireDbInfo -> tailleInfo.nomClient.contains(query, ignoreCase = true) }

            val uiItems = filteredList.mapNotNull { info: TaillePrioritaireDbInfo ->
                val chantierDetail = chantiersMap[info.chantierId]
                if (chantierDetail?.serviceTailleActive == true) {
                    val joursEcoules = info.derniereTailleDate?.let { date -> TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - date.time) }
                    TaillePrioritaireUiItem(
                        chantierId = info.chantierId,
                        nomClient = info.nomClient,
                        derniereTailleDate = info.derniereTailleDate,
                        nombreTaillesCetteAnnee = info.nombreTaillesCetteAnnee,
                        joursEcoules = joursEcoules,
                        urgencyColor = getUrgencyColorForTaille(info.derniereTailleDate, info.nombreTaillesCetteAnnee, seuil1V, seuil2O)
                    )
                } else null
            }
            when (sortOrder) {
                SortOrder.DESC -> uiItems.sortedWith( compareBy<TaillePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenByDescending { it.joursEcoules ?: -1L })
                SortOrder.ASC -> uiItems.sortedWith( compareByDescending<TaillePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenBy { it.joursEcoules ?: Long.MAX_VALUE })
                SortOrder.NONE -> uiItems.sortedBy { it.nomClient }
                // else -> uiItems // Ajout pour exhaustivité
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // --- Logique et StateFlows pour le DESHERBAGE ---
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
    val desherbagesSortOrder: StateFlow<SortOrder> = _desherbagesSortOrder.asStateFlow()

    val desherbagesPrioritaires: StateFlow<List<DesherbagePrioritaireUiItem>> = combine(
        listOf(
            repository.getAllPendingDesherbagesFlow(),
            tousLesChantiers,
            _searchQuery,
            _desherbagesSortOrder,
            desherbageSeuilOrangeJoursAvant
        )
    ) { values: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        val pendingPlanifs = values[0] as List<DesherbagePlanifie>
        @Suppress("UNCHECKED_CAST")
        val chantiersList = values[1] as List<Chantier>
        @Suppress("UNCHECKED_CAST")
        val query = values[2] as String
        @Suppress("UNCHECKED_CAST")
        val sortOrder = values[3] as SortOrder
        @Suppress("UNCHECKED_CAST")
        val seuilOrange = values[4] as Int

        val chantierMap = chantiersList.associateBy { chantier: Chantier -> chantier.id }

        val uiItems = pendingPlanifs.mapNotNull { planif: DesherbagePlanifie ->
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
        }.filter { desherbageItem: DesherbagePrioritaireUiItem ->
            if (query.isBlank()) true
            else desherbageItem.nomClient.contains(query, ignoreCase = true)
        }

        when (sortOrder) {
            SortOrder.ASC -> uiItems.sortedWith(compareBy<DesherbagePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenBy { it.joursAvantEcheance ?: Long.MAX_VALUE })
            SortOrder.DESC -> uiItems.sortedWith(compareByDescending<DesherbagePrioritaireUiItem> { getPriorityScore(it.urgencyColor) }.thenByDescending { it.joursAvantEcheance ?: Long.MIN_VALUE })
            SortOrder.NONE -> uiItems.sortedBy { it.nomClient }
            // else -> uiItems // Ajout pour exhaustivité
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())


    // --- CHRONOMÈTRE ---
    val interventionEnCoursUi: StateFlow<InterventionEnCoursUi?> =
        ChronomailleurService.serviceState.map { state ->
            when (state) {
                is ChronomailleurService.ChronoServiceState.Running -> {
                    val typeLisible = when (state.typeIntervention) {
                        "Tonte de pelouse" -> "Tonte"
                        "Taille de haie" -> "Taille"
                        "Désherbage" -> "Désherbage"
                        else -> state.typeIntervention
                    }
                    InterventionEnCoursUi(
                        interventionId = state.interventionId,
                        chantierId = state.chantierId,
                        typeIntervention = state.typeIntervention,
                        nomChantier = state.nomChantier,
                        heureDebut = state.heureDebut,
                        dureeEcouleeFormattee = state.dureeFormattee,
                        typeInterventionLisible = typeLisible
                    )
                }
                ChronomailleurService.ChronoServiceState.Idle -> null
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)


    // --- Données de la carte ---
    private val _rawTontesInfoForAllActiveChantiers: Flow<Map<Long, Date?>> =
        repository.getTontesPrioritairesFlow()
            .map { list ->
                list.associate { it.chantierId to it.derniereTonteDate }
            }
            .distinctUntilChanged()

    val mapData: StateFlow<List<MapChantierData>> = combine(
        tousLesChantiers,
        _rawTontesInfoForAllActiveChantiers,
        tonteSeuilVert,
        tonteSeuilOrange
    ) { chantiers, tontesInfoMap, seuilV, seuilO ->
        chantiers
            .filter { it.latitude != null && it.longitude != null }
            .map { chantier ->
                val derniereTonteDate: Date?
                val joursDepuisDerniereTonte: Long?
                val finalUrgencyColor: Color
                val finalMarkerHue: Float

                if (chantier.serviceTonteActive) {
                    derniereTonteDate = tontesInfoMap[chantier.id]
                    joursDepuisDerniereTonte = derniereTonteDate?.let { date ->
                        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - date.time)
                    }

                    finalUrgencyColor = getUrgencyColor(joursDepuisDerniereTonte, seuilV, seuilO)
                    finalMarkerHue = when (finalUrgencyColor) {
                        Color.Red -> BitmapDescriptorFactory.HUE_RED
                        OrangeCustom -> BitmapDescriptorFactory.HUE_ORANGE
                        Color.Green -> BitmapDescriptorFactory.HUE_GREEN
                        else -> BitmapDescriptorFactory.HUE_AZURE
                    }
                } else {
                    derniereTonteDate = null
                    joursDepuisDerniereTonte = null
                    var markerColorToUse = BitmapDescriptorFactory.HUE_AZURE
                    if(chantier.serviceTailleActive) markerColorToUse = BitmapDescriptorFactory.HUE_ORANGE
                    if(chantier.serviceDesherbageActive && !chantier.serviceTonteActive && !chantier.serviceTailleActive) markerColorToUse = BitmapDescriptorFactory.HUE_YELLOW

                    finalUrgencyColor = Color.Gray
                    finalMarkerHue = markerColorToUse
                }

                MapChantierData(
                    chantier = chantier,
                    derniereTonteDate = derniereTonteDate,
                    joursDepuisDerniereTonte = joursDepuisDerniereTonte,
                    tonteUrgencyColor = finalUrgencyColor,
                    markerHue = finalMarkerHue
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // --- NOUVEAU: StateFlows pour la page "Facturation Extras" ---
    val prestationsExtrasAFacturer: StateFlow<List<PrestationHorsContratDisplay>> =
        repository.getPrestationsDisplayByStatut(StatutFacturationExtras.A_FACTURER)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val prestationsExtrasFactureesHistorique: StateFlow<List<PrestationHorsContratDisplay>> =
        repository.getPrestationsDisplayByStatut(StatutFacturationExtras.FACTUREE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())


    // --- NOUVEAU: StateFlows pour la page d'accueil ---
    private val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)

    val apercuTontesUrgentes: StateFlow<List<ApercuTachePrioritaireItem>> =
        tontesPrioritaires.map { list ->
            list.take(3).map { tonte ->
                val detail = if (tonte.derniereTonteDate != null) {
                    "Dernière tonte le ${dateFormat.format(tonte.derniereTonteDate)}, ${tonte.joursEcoules ?: 0} jours écoulés"
                } else {
                    "Jamais tondu"
                }
                ApercuTachePrioritaireItem(
                    chantierId = tonte.chantierId,
                    nomClient = tonte.nomClient,
                    detail = detail,
                    urgencyColor = tonte.urgencyColor
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val nombreTotalTontesUrgentes: StateFlow<Int> = tontesPrioritaires.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)


    val apercuTaillesUrgentes: StateFlow<List<ApercuTachePrioritaireItem>> =
        taillesPrioritaires.map { list ->
            list.take(3).map { taille ->
                val detail = if (taille.derniereTailleDate != null) {
                    "Dernière taille le ${dateFormat.format(taille.derniereTailleDate)}, ${taille.joursEcoules ?: 0} jours écoulés - ${taille.nombreTaillesCetteAnnee}/2 faites"
                } else {
                    "Jamais taillé - ${taille.nombreTaillesCetteAnnee}/2 faites"
                }
                ApercuTachePrioritaireItem(
                    chantierId = taille.chantierId,
                    nomClient = taille.nomClient,
                    detail = detail,
                    urgencyColor = taille.urgencyColor
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val nombreTotalTaillesUrgentes: StateFlow<Int> = taillesPrioritaires.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)


    val apercuDesherbagesUrgents: StateFlow<List<ApercuTachePrioritaireItem>> =
        desherbagesPrioritaires.map { list ->
            list.take(3).map { desherbage ->
                val detail = if (desherbage.prochaineDatePlanifiee != null) {
                    val jours = desherbage.joursAvantEcheance
                    val prefix = when {
                        jours == null -> ""
                        jours < 0 -> "En retard de ${-jours} jours"
                        jours == 0L -> "Aujourd'hui"
                        else -> "Dans $jours jours"
                    }
                    "Planifié pour le ${dateFormat.format(desherbage.prochaineDatePlanifiee)} ($prefix)"
                } else {
                    "Aucune planification"
                }
                ApercuTachePrioritaireItem(
                    chantierId = desherbage.chantierId,
                    nomClient = desherbage.nomClient,
                    detail = detail,
                    urgencyColor = desherbage.urgencyColor
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val nombreTotalDesherbagesUrgents: StateFlow<Int> = desherbagesPrioritaires.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)


    val resumeFacturationAccueil: StateFlow<ResumeFacturationAccueil> =
        prestationsExtrasAFacturer.map { list ->
            ResumeFacturationAccueil(
                nombrePrestationsAFacturer = list.size,
                montantTotalEstimeAFacturer = list.sumOf { it.montant }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ResumeFacturationAccueil(0, 0.0))


    fun demarrerInterventionChrono(chantierId: Long, typeIntervention: String, chantierNom: String) {
        if (interventionEnCoursUi.value != null) {
            Toast.makeText(applicationContext, "Un chronomètre est déjà en cours.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(applicationContext, ChronomailleurService::class.java).apply {
            action = ChronomailleurService.ACTION_START
            putExtra(ChronomailleurService.EXTRA_CHANTIER_ID, chantierId)
            putExtra(ChronomailleurService.EXTRA_TYPE_INTERVENTION, typeIntervention)
            putExtra(ChronomailleurService.EXTRA_CHANTIER_NOM, chantierNom)
        }
        applicationContext.startService(intent)
    }

    fun terminerInterventionChrono(notes: String?) {
        if (interventionEnCoursUi.value == null) {
            return
        }
        val intent = Intent(applicationContext, ChronomailleurService::class.java).apply {
            action = ChronomailleurService.ACTION_STOP
            putExtra(ChronomailleurService.EXTRA_NOTES, notes)
        }
        applicationContext.startService(intent)
    }


    // --- Fonctions de modification et d'action ---
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun loadChantierById(chantierId: Long) {
        _selectedChantierId.value = chantierId
    }
    fun clearSelectedChantierId() {
        _selectedChantierId.value = null
    }

    fun changerOrdreTriTontes(sortOrder: SortOrder) { _tontesSortOrder.value = sortOrder }
    fun changerOrdreTriTailles(sortOrder: SortOrder) { _taillesSortOrder.value = sortOrder }
    fun changerOrdreTriDesherbages(sortOrder: SortOrder) { _desherbagesSortOrder.value = sortOrder }


    fun ajouterChantier(
        nomClient: String,
        adresse: String?,
        serviceTonteActive: Boolean,
        serviceTailleActive: Boolean,
        serviceDesherbageActive: Boolean,
        latitude: Double?,
        longitude: Double?
    ) {
        viewModelScope.launch {
            val chantier = Chantier(
                nomClient = nomClient,
                adresse = adresse,
                serviceTonteActive = serviceTonteActive,
                serviceTailleActive = serviceTailleActive,
                serviceDesherbageActive = serviceDesherbageActive,
                latitude = latitude,
                longitude = longitude
            )
            repository.insertChantier(chantier)
        }
    }

    fun updateChantier(chantier: Chantier) {
        viewModelScope.launch { repository.updateChantier(chantier) }
    }

    fun deleteChantier(chantier: Chantier) { viewModelScope.launch { repository.deleteChantier(chantier) } }


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
                if (dureeManuelleMillis != null && dureeManuelleMillis <=0) dureeEffectiveMillis = null
                if (dateFin != null && !dateDebut.before(dateFin)) heureFinEffective = null
            }

            val intervention = Intervention(
                chantierId = chantierId,
                typeIntervention = typeIntervention,
                dateIntervention = heureDebutEffective,
                heureDebut = heureDebutEffective,
                heureFin = heureFinEffective,
                dureeEffective = dureeEffectiveMillis,
                notes = notes,
                statutIntervention = InterventionStatus.COMPLETED.name
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
                heureFinCalc = null
                dureeCalc = null
            }

            val updatedIntervention = interventionExistante.copy(
                dateIntervention = nouvelleHeureDebut,
                heureDebut = nouvelleHeureDebut,
                heureFin = heureFinCalc,
                dureeEffective = dureeCalc,
                statutIntervention = if (heureFinCalc != null) InterventionStatus.COMPLETED.name else interventionExistante.statutIntervention
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
            repository.markDesherbagePlanifieAsDone(planificationId)
        }
    }

    fun marquerDesherbagePlanifieNonEffectue(planificationId: Long) {
        viewModelScope.launch { repository.markDesherbagePlanifieAsNotDone(planificationId) }
    }

    fun geocodeAdresse(adresse: String, onResult: (LatLng?) -> Unit) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(applicationContext, "Service de géocodage non disponible.", Toast.LENGTH_SHORT).show()
            onResult(null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val geocoder = Geocoder(applicationContext)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(adresse, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (addresses.isNotEmpty()) {
                                val location = addresses[0]
                                onResult(LatLng(location.latitude, location.longitude))
                            } else {
                                onResult(null)
                            }
                        }
                        override fun onError(errorMessage: String?) {
                            super.onError(errorMessage)
                            Log.e("Geocoding", "Erreur de géocodage: $errorMessage")
                            onResult(null)
                        }
                    })
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(adresse, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val location = addresses[0]
                        onResult(LatLng(location.latitude, location.longitude))
                    } else {
                        onResult(null)
                    }
                }
            } catch (e: IOException) {
                Log.e("Geocoding", "Erreur réseau ou I/O pendant le géocodage", e)
                onResult(null)
            } catch (e: IllegalArgumentException) {
                Log.e("Geocoding", "Adresse invalide pour le géocodage", e)
                onResult(null)
            }
        }
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
                dateDebutMillis = item.heureDebut?.time ?: item.dateIntervention.time
                dateFinMillis = item.heureFin?.time ?: (dateDebutMillis + TimeUnit.HOURS.toMillis(1))
                description += "\nType: ${item.typeIntervention}"
                if (item.dureeEffective != null) {
                    description += "\nDurée: ${ChronomailleurService.formatDuration(item.dureeEffective!!)}"
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
                dateFinMillis = item.datePlanifiee.time + TimeUnit.HOURS.toMillis(1)
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
                    when (itemType) {
                        "intervention" -> repository.marquerInterventionExportee(itemId, true)
                        "planification" -> repository.marquerDesherbagePlanifieExportee(itemId, true)
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

    fun findNearestMowingSite(currentUserLocation: LatLng, onResult: (NearestSiteResult) -> Unit) {
        viewModelScope.launch {
            val allChantiersSnapshot = tousLesChantiers.first()
            val potentialSites = tontesPrioritaires.first()
                .filter { it.latitude != null && it.longitude != null && it.chantierId != 0L }

            if (potentialSites.isEmpty()) {
                onResult(NearestSiteResult(null, "Aucun chantier de tonte avec coordonnées disponible.", null))
                return@launch
            }

            val userAndroidLocation = Location("user").apply {
                latitude = currentUserLocation.latitude
                longitude = currentUserLocation.longitude
            }

            val urgentSites = potentialSites.filter { it.urgencyColor == Color.Red || it.urgencyColor == OrangeCustom }
            val greenSites = potentialSites.filter { it.urgencyColor == Color.Green }

            var nearestSiteInfo: TontePrioritaireItem? = null
            var minDistance = Float.MAX_VALUE
            var message: String
            var targetChantier: Chantier? = null

            val sitesToSearch = if (urgentSites.isNotEmpty()) urgentSites else greenSites

            if (sitesToSearch.isNotEmpty()) {
                sitesToSearch.forEach { siteInfo ->
                    val siteLocation = Location("site").apply {
                        latitude = siteInfo.latitude!!
                        longitude = siteInfo.longitude!!
                    }
                    val distance = userAndroidLocation.distanceTo(siteLocation)
                    if (distance < minDistance) {
                        minDistance = distance
                        nearestSiteInfo = siteInfo
                    }
                }

                targetChantier = nearestSiteInfo?.let { info -> allChantiersSnapshot.find { it.id == info.chantierId } }
                val df = DecimalFormat("#.##")
                val distanceKm = df.format(minDistance / 1000f)
                val statusMessage = if (urgentSites.isNotEmpty() && nearestSiteInfo?.urgencyColor != Color.Green) "(urgent)" else "(à jour)"
                message = "Chantier tonte le plus proche ${statusMessage}: ${targetChantier?.nomClient ?: nearestSiteInfo?.nomClient ?: "N/A"} à ${distanceKm} km."

            } else {
                message = "Aucun chantier de tonte éligible (urgent ou à jour) trouvé pour la recherche."
            }

            val finalLatLng = nearestSiteInfo?.let { LatLng(it.latitude!!, it.longitude!!) }
            onResult(NearestSiteResult(targetChantier, message, finalLatLng))
        }
    }


    // --- Fonctions pour PrestationHorsContrat ---
    fun ajouterPrestationExtra(
        chantierId: Long?,
        referenceChantierTexteLibre: String?,
        description: String,
        datePrestation: Date,
        montant: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            if (chantierId == null && referenceChantierTexteLibre.isNullOrBlank()) {
                Toast.makeText(applicationContext, "Veuillez sélectionner un chantier ou saisir une référence.", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (description.isBlank()) {
                Toast.makeText(applicationContext, "La description est requise.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val prestation = PrestationHorsContrat(
                chantierId = chantierId,
                referenceChantierTexteLibre = referenceChantierTexteLibre?.ifBlank { null },
                description = description,
                datePrestation = datePrestation,
                montant = montant,
                statut = StatutFacturationExtras.A_FACTURER.name,
                notes = notes?.ifBlank { null }
            )
            repository.insertPrestationHorsContrat(prestation)
            Toast.makeText(applicationContext, "Prestation extra ajoutée.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePrestationExtra(prestation: PrestationHorsContrat) {
        viewModelScope.launch {
            if (prestation.chantierId == null && prestation.referenceChantierTexteLibre.isNullOrBlank()) {
                Toast.makeText(applicationContext, "Veuillez sélectionner un chantier ou saisir une référence.", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (prestation.description.isBlank()) {
                Toast.makeText(applicationContext, "La description est requise.", Toast.LENGTH_LONG).show()
                return@launch
            }
            repository.updatePrestationHorsContrat(prestation)
            Toast.makeText(applicationContext, "Prestation extra mise à jour.", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun getPrestationExtraById(id: Long): PrestationHorsContrat? {
        return repository.getPrestationHorsContratById(id)
    }

    fun deletePrestationExtra(prestation: PrestationHorsContrat) {
        viewModelScope.launch {
            repository.deletePrestationHorsContrat(prestation)
            Toast.makeText(applicationContext, "Prestation extra supprimée.", Toast.LENGTH_SHORT).show()
        }
    }

    fun marquerPrestationExtraFacturee(prestationId: Long) {
        viewModelScope.launch {
            val prestation = repository.getPrestationHorsContratById(prestationId)
            prestation?.let {
                val updatedPrestation = it.copy(statut = StatutFacturationExtras.FACTUREE.name)
                repository.updatePrestationHorsContrat(updatedPrestation)
                Toast.makeText(applicationContext, "Prestation marquée comme facturée.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun remettrePrestationExtraAFacturer(prestationId: Long) {
        viewModelScope.launch {
            val prestation = repository.getPrestationHorsContratById(prestationId)
            prestation?.let {
                val updatedPrestation = it.copy(statut = StatutFacturationExtras.A_FACTURER.name)
                repository.updatePrestationHorsContrat(updatedPrestation)
                Toast.makeText(applicationContext, "Prestation remise à facturer.", Toast.LENGTH_SHORT).show()
            }
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
