package com.example.suivichantierspaysagiste

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface InterventionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: Intervention): Long // Retourne l'ID de l'intervention insérée

    @Update
    suspend fun updateIntervention(intervention: Intervention)

    @Delete
    suspend fun deleteIntervention(intervention: Intervention)

    // Récupère toutes les interventions pour un chantier, triées par date de début (anciennement dateIntervention)
    // Les interventions en cours pourraient être affichées en premier si nécessaire par une logique supplémentaire dans le ViewModel.
    @Query("SELECT * FROM interventions WHERE chantierId = :chantierId ORDER BY dateIntervention DESC")
    fun getInterventionsForChantier(chantierId: Long): Flow<List<Intervention>>

    @Query("SELECT * FROM interventions WHERE id = :interventionId")
    suspend fun getInterventionById(interventionId: Long): Intervention?

    // Récupère la dernière intervention d'un type spécifique (basée sur heureDebut/dateIntervention)
    @Query("SELECT * FROM interventions WHERE chantierId = :chantierId AND typeIntervention = :type ORDER BY dateIntervention DESC LIMIT 1")
    fun getLastInterventionOfTypeForChantierFlow(chantierId: Long, type: String): Flow<Intervention?>

    // Compte le nombre d'interventions d'un type spécifique
    @Query("SELECT COUNT(id) FROM interventions WHERE chantierId = :chantierId AND typeIntervention = :type")
    fun countInterventionsOfTypeForChantierFlow(chantierId: Long, type: String): Flow<Int>

    // Compte les tailles de haie entre deux dates (basé sur heureDebut/dateIntervention)
    @Query("SELECT COUNT(id) FROM interventions WHERE chantierId = :chantierId AND typeIntervention = 'Taille de haie' AND dateIntervention BETWEEN :dateDebut AND :dateFin")
    suspend fun countTaillesHaieBetweenDates(chantierId: Long, dateDebut: Date, dateFin: Date): Int

    // Récupère les tailles de haie entre deux dates (basé sur heureDebut/dateIntervention)
    @Query("SELECT * FROM interventions WHERE chantierId = :chantierId AND typeIntervention = 'Taille de haie' AND dateIntervention BETWEEN :dateDebut AND :dateFin ORDER BY dateIntervention DESC")
    fun getTaillesHaieBetweenDates(chantierId: Long, dateDebut: Date, dateFin: Date): Flow<List<Intervention>>

    // Compte les tailles de haie entre deux dates (Flow) (basé sur heureDebut/dateIntervention)
    @Query("SELECT COUNT(id) FROM interventions WHERE chantierId = :chantierId AND typeIntervention = 'Taille de haie' AND dateIntervention BETWEEN :dateDebut AND :dateFin")
    fun getTaillesHaieBetweenDatesCountFlow(chantierId: Long, dateDebut: Date, dateFin: Date): Flow<Int>

    // NOUVELLE REQUÊTE: Récupérer une intervention en cours pour un chantier et un type donné
    @Query("SELECT * FROM interventions WHERE chantierId = :chantierId AND typeIntervention = :type AND statutIntervention = :statut ORDER BY heureDebut DESC LIMIT 1")
    suspend fun getInterventionEnCours(chantierId: Long, type: String, statut: String = InterventionStatus.IN_PROGRESS.name): Intervention?

    // NOUVELLE REQUÊTE: Récupérer une intervention en cours pour un chantier (Flow)
    // Utile si on veut afficher un indicateur global si *une* intervention est en cours sur ce chantier.
    // Pour l'instant, on se concentre sur une intervention en cours par type.
    // @Query("SELECT * FROM interventions WHERE chantierId = :chantierId AND statutIntervention = :statut LIMIT 1")
    // fun getAnyInterventionEnCoursForChantierFlow(chantierId: Long, statut: String = InterventionStatus.IN_PROGRESS.name): Flow<Intervention?>
}