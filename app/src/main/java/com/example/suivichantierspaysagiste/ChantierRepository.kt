package com.example.suivichantierspaysagiste

import kotlinx.coroutines.flow.Flow
import java.util.Date

class ChantierRepository(
    private val chantierDao: ChantierDao,
    private val interventionDao: InterventionDao
) {

    // Fonctions pour les Chantiers
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

    // Fonctions pour les Interventions
    fun getInterventionsForChantier(chantierId: Long): Flow<List<Intervention>> {
        return interventionDao.getInterventionsForChantier(chantierId)
    }

    suspend fun insertIntervention(intervention: Intervention) {
        interventionDao.insertIntervention(intervention)
    }

    suspend fun updateIntervention(intervention: Intervention) { // AJOUT DE CETTE FONCTION
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
    fun getChantierByIdFlow(id: Long): Flow<Chantier?> {
        return chantierDao.getChantierByIdFlow(id)
    }
    fun getTontesPrioritairesFlow(): Flow<List<TontePrioritaireInfo>> {
        return chantierDao.getTontesPrioritairesFlow()
    }
    // NOUVELLE FONCTION pour les informations de priorité des tailles
    fun getTaillesPrioritairesInfoFlow(startOfYearTimestamp: Long, endOfYearTimestamp: Long): Flow<List<TaillePrioritaireDbInfo>> {
        return chantierDao.getTaillesPrioritairesInfoFlow(startOfYearTimestamp, endOfYearTimestamp)
    }
    suspend fun getInterventionById(interventionId: Long): Intervention? {
        return interventionDao.getInterventionById(interventionId)
    }
}
