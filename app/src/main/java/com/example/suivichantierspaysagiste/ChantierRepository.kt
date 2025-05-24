package com.example.suivichantierspaysagiste

import kotlinx.coroutines.flow.Flow
import java.util.Date

class ChantierRepository(
    private val chantierDao: ChantierDao,
    private val interventionDao: InterventionDao,
    private val desherbagePlanifieDao: DesherbagePlanifieDao // NOUVEAU DAO
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

    suspend fun insertIntervention(intervention: Intervention) {
        interventionDao.insertIntervention(intervention)
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

    // --- Fonctions pour les Tontes et Tailles Prioritaires (inchangées) ---
    fun getTontesPrioritairesFlow(): Flow<List<TontePrioritaireInfo>> {
        return chantierDao.getTontesPrioritairesFlow()
    }
    fun getTaillesPrioritairesInfoFlow(startOfYearTimestamp: Long, endOfYearTimestamp: Long): Flow<List<TaillePrioritaireDbInfo>> {
        return chantierDao.getTaillesPrioritairesInfoFlow(startOfYearTimestamp, endOfYearTimestamp)
    }

    // --- NOUVELLES Fonctions pour DesherbagePlanifie ---
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

    // Pour l'écran des désherbages prioritaires
    // On doit mapper DesherbagePlanifie à une nouvelle data class qui inclut le nom du client.
    // Pour cela, on va d'abord récupérer les chantiers et les planifications, puis les combiner.
    // Ou, mieux, modifier le DAO pour faire une jointure.
    // Pour l'instant, je vais utiliser la requête DAO qui fait déjà une sorte de jointure.
    // La requête `getAllPendingDesherbagesWithChantierInfo` retourne déjà des `DesherbagePlanifie`.
    // Nous aurons besoin d'une data class pour l'UI qui combine `DesherbagePlanifie` et `nomClient`.
    // Je vais créer une data class `DesherbagePrioritaireInfo` (similaire à TontePrioritaireInfo)
    // et ajuster la requête DAO ou faire le mapping dans le ViewModel.
    // Pour l'instant, je vais supposer que le ViewModel s'occupera du mapping si nécessaire.
    fun getAllPendingDesherbagesFlow(): Flow<List<DesherbagePlanifie>> { // Renommé pour clarté
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
}