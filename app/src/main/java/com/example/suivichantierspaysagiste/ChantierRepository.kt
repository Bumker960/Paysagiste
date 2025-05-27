package com.example.suivichantierspaysagiste

import kotlinx.coroutines.flow.Flow
import java.util.Date

class ChantierRepository(
    private val chantierDao: ChantierDao,
    private val interventionDao: InterventionDao,
    private val desherbagePlanifieDao: DesherbagePlanifieDao,
    private val prestationHorsContratDao: PrestationHorsContratDao // Ajout du DAO
) {

    // --- Fonctions pour les Chantiers (inchangées) ---
    fun getAllChantiers(): Flow<List<Chantier>> {
        return chantierDao.getAllChantiers()
    }

    suspend fun insertChantier(chantier: Chantier) {
        chantierDao.insertChantier(chantier)
    }

    suspend fun updateChantier(chantier: Chantier) {
        chantierDao.updateChantier(chantier)
    }

    suspend fun deleteChantier(chantier: Chantier) {
        chantierDao.deleteChantier(chantier)
    }

    fun getChantierByIdFlow(id: Long): Flow<Chantier?> {
        return chantierDao.getChantierByIdFlow(id)
    }

    // --- Fonctions pour les Interventions (inchangées) ---
    fun getInterventionsForChantier(chantierId: Long): Flow<List<Intervention>> {
        return interventionDao.getInterventionsForChantier(chantierId)
    }

    suspend fun insertIntervention(intervention: Intervention): Long {
        return interventionDao.insertIntervention(intervention)
    }

    suspend fun updateIntervention(intervention: Intervention) {
        interventionDao.updateIntervention(intervention)
    }

    suspend fun deleteIntervention(intervention: Intervention) {
        interventionDao.deleteIntervention(intervention)
    }

    fun getLastInterventionOfTypeForChantierFlow(chantierId: Long, type: String): Flow<Intervention?> {
        return interventionDao.getLastInterventionOfTypeForChantierFlow(chantierId, type)
    }

    fun countInterventionsOfTypeForChantierFlow(chantierId: Long, type: String): Flow<Int> {
        return interventionDao.countInterventionsOfTypeForChantierFlow(chantierId, type)
    }

    suspend fun countTaillesHaieBetweenDates(chantierId: Long, dateDebut: Date, dateFin: Date): Int {
        return interventionDao.countTaillesHaieBetweenDates(chantierId, dateDebut, dateFin)
    }
    fun getTaillesHaieBetweenDates(chantierId: Long, dateDebut: Date, dateFin: Date): Flow<List<Intervention>> {
        return interventionDao.getTaillesHaieBetweenDates(chantierId,dateDebut,dateFin)
    }
    fun getTaillesHaieBetweenDatesCountFlow(chantierId: Long, dateDebut: Date, dateFin: Date): Flow<Int> {
        return interventionDao.getTaillesHaieBetweenDatesCountFlow(chantierId, dateDebut, dateFin)
    }
    suspend fun getInterventionById(interventionId: Long): Intervention? {
        return interventionDao.getInterventionById(interventionId)
    }

    suspend fun marquerInterventionExportee(interventionId: Long, estExportee: Boolean) {
        val intervention = interventionDao.getInterventionById(interventionId)
        intervention?.let {
            interventionDao.updateIntervention(it.copy(exporteAgenda = estExportee))
        }
    }

    // --- Fonctions pour les Tontes et Tailles Prioritaires (inchangées) ---
    fun getTontesPrioritairesFlow(): Flow<List<TontePrioritaireInfo>> {
        return chantierDao.getTontesPrioritairesFlow()
    }
    fun getTaillesPrioritairesInfoFlow(startOfYearTimestamp: Long, endOfYearTimestamp: Long): Flow<List<TaillePrioritaireDbInfo>> {
        return chantierDao.getTaillesPrioritairesInfoFlow(startOfYearTimestamp, endOfYearTimestamp)
    }

    // --- Fonctions pour DesherbagePlanifie (inchangées) ---
    fun getDesherbagesPlanifiesForChantier(chantierId: Long): Flow<List<DesherbagePlanifie>> {
        return desherbagePlanifieDao.getDesherbagesPlanifiesForChantier(chantierId)
    }

    suspend fun insertDesherbagePlanifie(desherbagePlanifie: DesherbagePlanifie) {
        desherbagePlanifieDao.insert(desherbagePlanifie)
    }

    suspend fun updateDesherbagePlanifie(desherbagePlanifie: DesherbagePlanifie) {
        desherbagePlanifieDao.update(desherbagePlanifie)
    }

    suspend fun deleteDesherbagePlanifie(desherbagePlanifie: DesherbagePlanifie) {
        desherbagePlanifieDao.delete(desherbagePlanifie)
    }

    suspend fun deleteDesherbagePlanifieById(planificationId: Long) {
        desherbagePlanifieDao.deleteById(planificationId)
    }

    fun getNextPendingDesherbageForChantier(chantierId: Long): Flow<DesherbagePlanifie?> {
        return desherbagePlanifieDao.getNextPendingDesherbageForChantier(chantierId)
    }

    fun getAllPendingDesherbagesFlow(): Flow<List<DesherbagePlanifie>> {
        return desherbagePlanifieDao.getAllPendingDesherbagesWithChantierInfo()
    }

    suspend fun markDesherbagePlanifieAsDone(planificationId: Long) {
        desherbagePlanifieDao.markAsDone(planificationId)
    }

    suspend fun markDesherbagePlanifieAsNotDone(planificationId: Long) {
        desherbagePlanifieDao.markAsNotDone(planificationId)
    }

    suspend fun countDesherbagesPlanifiesForDate(chantierId: Long, date: Date): Int {
        return desherbagePlanifieDao.countDesherbagesPlanifiesForDate(chantierId, date)
    }

    suspend fun marquerDesherbagePlanifieExportee(planificationId: Long, estExportee: Boolean) {
        val planification = desherbagePlanifieDao.getDesherbagePlanifieById(planificationId)
        planification?.let {
            desherbagePlanifieDao.update(it.copy(exporteAgenda = estExportee))
        }
    }

    suspend fun getInterventionEnCours(chantierId: Long, typeIntervention: String): Intervention? {
        return interventionDao.getInterventionEnCours(chantierId, typeIntervention)
    }

    // --- Nouvelles fonctions pour PrestationHorsContrat ---
    fun getPrestationsDisplayByStatut(statut: StatutFacturationExtras): Flow<List<PrestationHorsContratDisplay>> {
        return prestationHorsContratDao.getPrestationsDisplayByStatut(statut.name)
    }

    suspend fun insertPrestationHorsContrat(prestation: PrestationHorsContrat): Long {
        return prestationHorsContratDao.insert(prestation)
    }

    suspend fun updatePrestationHorsContrat(prestation: PrestationHorsContrat) {
        prestationHorsContratDao.update(prestation)
    }

    suspend fun deletePrestationHorsContrat(prestation: PrestationHorsContrat) {
        prestationHorsContratDao.delete(prestation)
    }

    suspend fun getPrestationHorsContratById(id: Long): PrestationHorsContrat? {
        return prestationHorsContratDao.getPrestationById(id)
    }
}
