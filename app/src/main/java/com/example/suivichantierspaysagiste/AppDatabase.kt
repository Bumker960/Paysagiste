package com.example.suivichantierspaysagiste

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// VERSION INCLEMENTÉE à 5 pour la nouvelle migration
@Database(entities = [Chantier::class, Intervention::class, DesherbagePlanifie::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chantierDao(): ChantierDao
    abstract fun interventionDao(): InterventionDao
    abstract fun desherbagePlanifieDao(): DesherbagePlanifieDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration existante de 2 à 3 (pour DesherbagePlanifie et serviceDesherbageActive)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `desherbages_planifies` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `chantierId` INTEGER NOT NULL,
                        `datePlanifiee` INTEGER NOT NULL,
                        `estEffectue` INTEGER NOT NULL DEFAULT 0,
                        `notesPlanification` TEXT,
                        FOREIGN KEY(`chantierId`) REFERENCES `chantiers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_desherbages_planifies_chantierId` ON `desherbages_planifies` (`chantierId`)")
                db.execSQL("ALTER TABLE `chantiers` ADD COLUMN `serviceDesherbageActive` INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Migration existante de la version 3 à 4 pour ajouter les colonnes exporteAgenda
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `exporteAgenda` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `desherbages_planifies` ADD COLUMN `exporteAgenda` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // NOUVELLE migration de la version 4 à 5 pour ajouter les champs de chronométrage à Intervention
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ajouter les nouvelles colonnes à la table interventions
                // On rend heureDebut nullable pour le moment, car dateIntervention existe déjà.
                // On pourrait envisager de copier dateIntervention vers heureDebut pour les anciennes données
                // mais pour l'instant, on les ajoute comme nullables et on gère la logique dans le code.
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `heureDebut` INTEGER") // Nullable, car dateIntervention existe
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `heureFin` INTEGER") // Nullable
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `dureeEffective` INTEGER") // Nullable, en millisecondes
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `statutIntervention` TEXT NOT NULL DEFAULT '${InterventionStatus.COMPLETED.name}'")

                // Potentiellement, mettre à jour heureDebut pour les anciennes interventions
                // en utilisant la valeur de dateIntervention existante.
                // Cela assure que heureDebut n'est pas null pour les anciennes données si on décide de le rendre non-nullable plus tard
                // ou pour simplifier les requêtes.
                db.execSQL("UPDATE `interventions` SET `heureDebut` = `dateIntervention` WHERE `heureDebut` IS NULL")
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "suivi_chantiers_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5) // Ajout de la nouvelle migration
                    .fallbackToDestructiveMigrationFrom(1) // Gère les versions antérieures à la première migration définie (ici, < 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}