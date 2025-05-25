package com.example.suivichantierspaysagiste

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Chantier::class, Intervention::class, DesherbagePlanifie::class], version = 4, exportSchema = false) // VERSION INCLEMENTÉE à 4
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chantierDao(): ChantierDao
    abstract fun interventionDao(): InterventionDao
    abstract fun desherbagePlanifieDao(): DesherbagePlanifieDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) { // Garde l'ancienne migration si elle est toujours pertinente
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

        // Nouvelle migration de la version 3 à 4 pour ajouter les colonnes exporteAgenda
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ajouter la colonne exporteAgenda à la table interventions
                db.execSQL("ALTER TABLE `interventions` ADD COLUMN `exporteAgenda` INTEGER NOT NULL DEFAULT 0")
                // Ajouter la colonne exporteAgenda à la table desherbages_planifies
                db.execSQL("ALTER TABLE `desherbages_planifies` ADD COLUMN `exporteAgenda` INTEGER NOT NULL DEFAULT 0")
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "suivi_chantiers_database"
                )
                    // Ajoutez les migrations dans l'ordre.
                    // Si la base de données de l'utilisateur est en version 2, MIGRATION_2_3 sera exécutée, puis MIGRATION_3_4.
                    // Si elle est en version 3, seule MIGRATION_3_4 sera exécutée.
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4) // Ajout de la nouvelle migration
                    .fallbackToDestructiveMigrationFrom(1) // Si la version est 1, détruire et recréer (ou fournir MIGRATION_1_2, MIGRATION_1_3, MIGRATION_1_4)
                    // Cette ligne permet de gérer les cas où la version est < 2 sans avoir toutes les migrations intermédiaires.
                    // Si vous êtes sûr que tous les utilisateurs sont au moins en version 2, vous pouvez l'enlever ou ajuster.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}