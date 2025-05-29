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


    // --- NOUVELLES REQUÊTES POUR L'ANALYSE DU TEMPS ---

    /**
     * Récupère toutes les interventions complétées (avec une durée effective) dans une plage de dates donnée.
     * Utilisé pour les calculs globaux de temps.
     */
    @Query("""
        SELECT id, chantierId, typeIntervention, heureDebut, dureeEffective 
        FROM interventions 
        WHERE statutIntervention = :statutTermine 
        AND dureeEffective IS NOT NULL AND dureeEffective > 0
        AND heureDebut BETWEEN :dateDebut AND :dateFin
    """)
    fun getInterventionsAvecDureeDansPeriode(dateDebut: Date, dateFin: Date, statutTermine: String = InterventionStatus.COMPLETED.name): Flow<List<InterventionAvecDuree>>

    /**
     * Récupère toutes les interventions complétées (avec une durée effective) sans filtre de date.
     */
    @Query("""
        SELECT id, chantierId, typeIntervention, heureDebut, dureeEffective 
        FROM interventions 
        WHERE statutIntervention = :statutTermine 
        AND dureeEffective IS NOT NULL AND dureeEffective > 0
    """)
    fun getAllInterventionsAvecDuree(statutTermine: String = InterventionStatus.COMPLETED.name): Flow<List<InterventionAvecDuree>>


    /**
     * Calcule le temps total passé par chantier pour les interventions complétées dans une plage de dates.
     * Utilise une jointure avec la table des chantiers pour obtenir le nom du client.
     */
    @Query("""
        SELECT 
            i.chantierId, 
            c.nomClient, 
            SUM(i.dureeEffective) as tempsTotalMillis
        FROM interventions i
        INNER JOIN chantiers c ON i.chantierId = c.id
        WHERE i.statutIntervention = :statutTermine 
        AND i.dureeEffective IS NOT NULL AND i.dureeEffective > 0
        AND i.heureDebut BETWEEN :dateDebut AND :dateFin
        GROUP BY i.chantierId, c.nomClient
    """)
    fun getTempsTotalParChantierDansPeriode(dateDebut: Date, dateFin: Date, statutTermine: String = InterventionStatus.COMPLETED.name): Flow<List<ChantierTempsTotal>>

    /**
     * Calcule le temps total passé par chantier pour toutes les interventions complétées.
     */
    @Query("""
        SELECT 
            i.chantierId, 
            c.nomClient, 
            SUM(i.dureeEffective) as tempsTotalMillis
        FROM interventions i
        INNER JOIN chantiers c ON i.chantierId = c.id
        WHERE i.statutIntervention = :statutTermine 
        AND i.dureeEffective IS NOT NULL AND i.dureeEffective > 0
        GROUP BY i.chantierId, c.nomClient
    """)
    fun getAllTempsTotalParChantier(statutTermine: String = InterventionStatus.COMPLETED.name): Flow<List<ChantierTempsTotal>>


    /**
     * Calcule le temps total passé par type d'intervention pour les interventions complétées dans une plage de dates.
     */
    @Query("""
        SELECT 
            typeIntervention, 
            SUM(dureeEffective) as tempsTotalMillis
        FROM interventions
        WHERE statutIntervention = :statutTermine 
        AND dureeEffective IS NOT NULL AND dureeEffective > 0
        AND heureDebut BETWEEN :dateDebut AND :dateFin
        GROUP BY typeIntervention
    """)
    fun getTempsTotalParTypeInterventionDansPeriode(dateDebut: Date, dateFin: Date, statutTermine: String = InterventionStatus.COMPLETED.name): Flow<List<TypeInterventionTempsTotal>>

    /**
     * Calcule le temps total passé par type d'intervention pour toutes les interventions complétées.
     */
    @Query("""
        SELECT 
            typeIntervention, 
            SUM(dureeEffective) as tempsTotalMillis
        FROM interventions
        WHERE statutIntervention = :statutTermine 
        AND dureeEffective IS NOT NULL AND dureeEffective > 0
        GROUP BY typeIntervention
    """)
    fun getAllTempsTotalParTypeIntervention(statutTermine: String = InterventionStatus.COMPLETED.name): Flow<List<TypeInterventionTempsTotal>>

}
