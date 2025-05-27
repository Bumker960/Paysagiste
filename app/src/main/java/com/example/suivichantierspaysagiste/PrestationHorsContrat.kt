package com.example.suivichantierspaysagiste

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Énumération pour les statuts des prestations hors contrat.
 * A_FACTURER: La prestation a été effectuée mais pas encore facturée.
 * FACTUREE: La facture a été émise et la prestation est archivée.
 */
enum class StatutFacturationExtras {
    A_FACTURER,
    FACTUREE // Ou ARCHIVEE si vous préférez ce terme pour l'historique
}

/**
 * Représente une prestation effectuée en dehors d'un contrat régulier.
 * Peut être liée à un chantier existant (via chantierId) ou avoir une référence textuelle libre.
 */
@Entity(
    tableName = "prestations_extras", // Nom de table mis à jour pour clarté
    foreignKeys = [ForeignKey(
        entity = Chantier::class,
        parentColumns = ["id"],
        childColumns = ["chantierId"],
        onDelete = ForeignKey.SET_NULL // Si un chantier est supprimé, ne pas supprimer la prestation extra, juste délier.
    )],
    indices = [Index(value = ["chantierId"])]
)
data class PrestationHorsContrat(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Lien optionnel vers un chantier existant
    val chantierId: Long? = null,

    // Référence textuelle libre si non lié à un chantier existant ou pour plus de détails
    var referenceChantierTexteLibre: String? = null,

    var description: String,
    var datePrestation: Date,
    var montant: Double, // Montant TTC final pour simplifier
    var statut: String = StatutFacturationExtras.A_FACTURER.name,
    var notes: String? = null
)

/**
 * Data class pour combiner les informations de PrestationHorsContrat
 * avec le nom du client (si chantierId est fourni) pour l'affichage.
 */
data class PrestationHorsContratDisplay(
    // Champs de PrestationHorsContrat
    val id: Long,
    val chantierId: Long?,
    val referenceChantierTexteLibre: String?,
    val description: String,
    val datePrestation: Date,
    val montant: Double,
    val statut: String,
    val notes: String?,

    // Champ additionnel pour le nom du client/chantier à afficher
    val nomAffichageChantier: String // Sera soit nomClient (si chantierId) soit referenceChantierTexteLibre
)
