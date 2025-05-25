package com.example.suivichantierspaysagiste

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// VERSION INCLEMENTÉE à 6 pour la nouvelle migration
@Database(entities = [Chantier::class, Intervention::class, DesherbagePlanifie::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chantierDao(): ChantierDao
    abstract fun interventionDao(): InterventionDao
    abstract fun desherbagePlanifieDao(): DesherbagePlanifieDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration existante de 2 à 3
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

        // Migration existante de la version 3 à 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `exporteAgenda` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `desherbages_planifies` ADD COLUMN `exporteAgenda` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration existante de la version 4 à 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `heureDebut` INTEGER")
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `heureFin` INTEGER")
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `dureeEffective` INTEGER")
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `statutIntervention` TEXT NOT NULL DEFAULT '${InterventionStatus.COMPLETED.name}'")
                // Mise à jour de heureDebut pour les anciennes interventions
                db.execSQL("UPDATE `interventions` SET `heureDebut` = `dateIntervention` WHERE `heureDebut` IS NULL")
            }
        }

        // NOUVELLE migration de la version 5 à 6 pour ajouter latitude et longitude à Chantiers
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ajoute la colonne latitude à la table chantiers. REAL est utilisé pour les Double en SQLite.
                db.execSQL("ALTER TABLE `chantiers` ADD COLUMN `latitude` REAL")
                // Ajoute la colonne longitude à la table chantiers.
                db.execSQL("ALTER TABLE `chantiers` ADD COLUMN `longitude` REAL")
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "suivi_chantiers_database"
                )
                    // Ajout de la nouvelle migration MIGRATION_5_6 à la liste
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    // Gère les versions antérieures à la première migration définie (ici, < 2)
                    // Si une version < 2 est détectée, la base de données sera détruite et reconstruite.
                    // Soyez prudent avec cette option en production si des données importantes existent déjà.
                    .fallbackToDestructiveMigrationFrom(1)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}