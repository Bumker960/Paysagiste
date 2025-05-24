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
interface DesherbagePlanifieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(desherbagePlanifie: DesherbagePlanifie): Long

    @Update
    suspend fun update(desherbagePlanifie: DesherbagePlanifie)

    @Delete
    suspend fun delete(desherbagePlanifie: DesherbagePlanifie)

    @Query("DELETE FROM desherbages_planifies WHERE id = :planificationId")
    suspend fun deleteById(planificationId: Long)

    @Query("SELECT * FROM desherbages_planifies WHERE chantierId = :chantierId ORDER BY datePlanifiee ASC")
    fun getDesherbagesPlanifiesForChantier(chantierId: Long): Flow<List<DesherbagePlanifie>>

    @Query("SELECT * FROM desherbages_planifies WHERE id = :id")
    suspend fun getDesherbagePlanifieById(id: Long): DesherbagePlanifie?

    // Récupère la prochaine planification non effectuée pour un chantier
    @Query("SELECT * FROM desherbages_planifies WHERE chantierId = :chantierId AND estEffectue = 0 ORDER BY datePlanifiee ASC LIMIT 1")
    fun getNextPendingDesherbageForChantier(chantierId: Long): Flow<DesherbagePlanifie?>

    // Récupère toutes les planifications non effectuées pour tous les chantiers actifs pour le désherbage
    @Query("""
        SELECT dp.* FROM desherbages_planifies dp
        INNER JOIN chantiers c ON dp.chantierId = c.id
        WHERE c.serviceDesherbageActive = 1 AND dp.estEffectue = 0
        ORDER BY dp.datePlanifiee ASC
    """)
    fun getAllPendingDesherbagesWithChantierInfo(): Flow<List<DesherbagePlanifie>> // Sera utilisé pour l'écran prioritaire

    // Marquer une planification comme effectuée
    @Query("UPDATE desherbages_planifies SET estEffectue = 1 WHERE id = :planificationId")
    suspend fun markAsDone(planificationId: Long)

    // Marquer une planification comme non effectuée
    @Query("UPDATE desherbages_planifies SET estEffectue = 0 WHERE id = :planificationId")
    suspend fun markAsNotDone(planificationId: Long)

    // Compte le nombre de désherbages planifiés pour un chantier à une date donnée
    @Query("SELECT COUNT(*) FROM desherbages_planifies WHERE chantierId = :chantierId AND datePlanifiee = :date")
    suspend fun countDesherbagesPlanifiesForDate(chantierId: Long, date: Date): Int
}