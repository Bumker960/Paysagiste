package com.example.suivichantierspaysagiste

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DevisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevis(devis: Devis): Long

    @Delete
    suspend fun deleteDevis(devis: Devis)

    @Query("SELECT * FROM devis WHERE chantierId = :chantierId ORDER BY dateAjout DESC")
    fun getDevisForChantier(chantierId: Long): Flow<List<Devis>>

    @Query("SELECT * FROM devis WHERE id = :devisId")
    suspend fun getDevisById(devisId: Long): Devis?

    // Optionnel: si vous avez besoin de supprimer par ID directement
    @Query("DELETE FROM devis WHERE id = :devisId")
    suspend fun deleteDevisById(devisId: Long)
}
