package com.example.suivichantierspaysagiste // Adaptez à votre nom de package

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
    suspend fun insertIntervention(intervention: Intervention): Long

    @Update
    suspend fun updateIntervention(intervention: Intervention)

    @Delete
    suspend fun deleteIntervention(intervention: Intervention)

    @Query("SELECT * FROM interventions WHERE chantierId = :chantierId ORDER BY dateIntervention DESC")
    fun getInterventionsForChantier(chantierId: Long): Flow<List<Intervention>>

    @Query("SELECT * FROM interventions WHERE id = :interventionId")
    suspend fun getInterventionById(interventionId: Long): Intervention?

    @Query("SELECT * FROM interventions WHERE chantierId = :chantierId AND typeIntervention = :type ORDER BY dateIntervention DESC LIMIT 1")
    fun getLastInterventionOfTypeForChantierFlow(chantierId: Long, type: String): Flow<Intervention?> 

    @Query("SELECT COUNT(id) FROM interventions WHERE chantierId = :chantierId AND typeIntervention = :type")
    fun countInterventionsOfTypeForChantierFlow(chantierId: Long, type: String): Flow<Int>

    @Query("SELECT COUNT(id) FROM interventions WHERE chantierId = :chantierId AND typeIntervention = 'Taille de haie' AND dateIntervention BETWEEN :dateDebut AND :dateFin")
    suspend fun countTaillesHaieBetweenDates(chantierId: Long, dateDebut: Date, dateFin: Date): Int

    @Query("SELECT * FROM interventions WHERE chantierId = :chantierId AND typeIntervention = 'Taille de haie' AND dateIntervention BETWEEN :dateDebut AND :dateFin ORDER BY dateIntervention DESC")
    fun getTaillesHaieBetweenDates(chantierId: Long, dateDebut: Date, dateFin: Date): Flow<List<Intervention>>

    @Query("SELECT COUNT(id) FROM interventions WHERE chantierId = :chantierId AND typeIntervention = 'Taille de haie' AND dateIntervention BETWEEN :dateDebut AND :dateFin")
    fun getTaillesHaieBetweenDatesCountFlow(chantierId: Long, dateDebut: Date, dateFin: Date): Flow<Int> // Doit retourner Flow<Int>
}