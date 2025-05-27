package com.example.suivichantierspaysagiste

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrestationHorsContratDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prestation: PrestationHorsContrat): Long

    @Update
    suspend fun update(prestation: PrestationHorsContrat)

    @Delete
    suspend fun delete(prestation: PrestationHorsContrat)

    @Query("SELECT * FROM prestations_extras WHERE id = :id")
    suspend fun getPrestationById(id: Long): PrestationHorsContrat?

    /**
     * Récupère les prestations pour un statut donné, en joignant le nom du chantier si disponible.
     * Trie par date de prestation la plus récente en premier.
     */
    @Query("""
        SELECT 
            pe.id, pe.chantierId, pe.referenceChantierTexteLibre, pe.description, 
            pe.datePrestation, pe.montant, pe.statut, pe.notes,
            COALESCE(c.nomClient, pe.referenceChantierTexteLibre) AS nomAffichageChantier
        FROM prestations_extras pe
        LEFT JOIN chantiers c ON pe.chantierId = c.id
        WHERE pe.statut = :statut
        ORDER BY pe.datePrestation DESC
    """)
    fun getPrestationsDisplayByStatut(statut: String): Flow<List<PrestationHorsContratDisplay>>

    // Si vous avez besoin de toutes les prestations sans distinction de statut pour une raison quelconque
    @Query("""
        SELECT 
            pe.id, pe.chantierId, pe.referenceChantierTexteLibre, pe.description, 
            pe.datePrestation, pe.montant, pe.statut, pe.notes,
            COALESCE(c.nomClient, pe.referenceChantierTexteLibre) AS nomAffichageChantier
        FROM prestations_extras pe
        LEFT JOIN chantiers c ON pe.chantierId = c.id
        ORDER BY pe.datePrestation DESC
    """)
    fun getAllPrestationsDisplay(): Flow<List<PrestationHorsContratDisplay>>
}