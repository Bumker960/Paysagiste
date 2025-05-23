package com.example.suivichantierspaysagiste

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date // Assurez-vous que cet import est là

@Dao
interface ChantierDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChantier(chantier: Chantier): Long

    @Update
    suspend fun updateChantier(chantier: Chantier)

    @Delete
    suspend fun deleteChantier(chantier: Chantier)

    @Query("SELECT * FROM chantiers ORDER BY nomClient ASC")
    fun getAllChantiers(): Flow<List<Chantier>>

    @Query("SELECT * FROM chantiers WHERE id = :chantierId")
    fun getChantierByIdFlow(chantierId: Long): Flow<Chantier?>

    // Requête pour les tontes prioritaires - MODIFIÉE
    @Query("""
        SELECT 
            c.id as chantierId, 
            c.nomClient as nomClient, 
            MAX(i.dateIntervention) as derniereTonteDate 
        FROM chantiers c
        LEFT JOIN interventions i ON c.id = i.chantierId AND i.typeIntervention = 'Tonte de pelouse'
        WHERE c.serviceTonteActive = 1 -- Ajout du filtre ici
        GROUP BY c.id, c.nomClient
    """)
    fun getTontesPrioritairesFlow(): Flow<List<TontePrioritaireInfo>>

    // Requête pour les informations de priorité des tailles - MODIFIÉE
    @Query("""
        SELECT
            c.id AS chantierId,
            c.nomClient AS nomClient,
            (SELECT MAX(i_last.dateIntervention) FROM interventions i_last
             WHERE i_last.chantierId = c.id AND i_last.typeIntervention = 'Taille de haie') AS derniereTailleDate,
            (SELECT COUNT(i_year.id) FROM interventions i_year
             WHERE i_year.chantierId = c.id AND i_year.typeIntervention = 'Taille de haie'
             AND i_year.dateIntervention >= :startOfYearTimestamp AND i_year.dateIntervention <= :endOfYearTimestamp) AS nombreTaillesCetteAnnee
        FROM chantiers c
        WHERE c.serviceTailleActive = 1 -- Ajout du filtre ici
    """)
    fun getTaillesPrioritairesInfoFlow(startOfYearTimestamp: Long, endOfYearTimestamp: Long): Flow<List<TaillePrioritaireDbInfo>>
}
