package com.example.suivichantierspaysagiste

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Chantier::class, Intervention::class, DesherbagePlanifie::class], version = 3, exportSchema = false) // VERSION INCLEMENTÉE à 3
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chantierDao(): ChantierDao
    abstract fun interventionDao(): InterventionDao
    abstract fun desherbagePlanifieDao(): DesherbagePlanifieDao // NOUVEAU DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration de la version 2 à 3 (ajout de la table desherbages_planifies et colonne à chantiers)
        // Ceci est un exemple basique. Pour des migrations complexes, référez-vous à la documentation Room.
        // IMPORTANT: Si vous avez des utilisateurs avec la version 2, cette migration doit être correcte.
        // Pour la nouvelle colonne serviceDesherbageActive, on peut la définir avec une valeur par défaut.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ajouter la nouvelle table desherbages_planifies
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
                // Ajouter un index pour chantierId dans la nouvelle table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_desherbages_planifies_chantierId` ON `desherbages_planifies` (`chantierId`)")

                // Ajouter la nouvelle colonne serviceDesherbageActive à la table chantiers
                // avec une valeur par défaut de 1 (true) pour les chantiers existants.
                // Adaptez la valeur par défaut si nécessaire.
                db.execSQL("ALTER TABLE `chantiers` ADD COLUMN `serviceDesherbageActive` INTEGER NOT NULL DEFAULT 1")
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "suivi_chantiers_database"
                )
                    // IMPORTANT: Pour le développement, fallbackToDestructiveMigration est simple.
                    // Pour la production, vous DEVEZ fournir des objets Migration.
                    // J'ai ajouté MIGRATION_2_3 comme exemple, mais assurez-vous que la version précédente était bien 2.
                    // Si vous partez d'une version 1, vous auriez besoin de MIGRATION_1_2 puis MIGRATION_2_3.
                    // Ou une MIGRATION_1_3 directe.
                    // Pour cet exercice, si la version précédente est inconnue ou si c'est la première fois avec des migrations,
                    // .fallbackToDestructiveMigration() est plus sûr pour éviter les erreurs de migration.
                    // .addMigrations(MIGRATION_2_3) // Ajoutez la migration ici
                    .fallbackToDestructiveMigration() // Supprime et recrée si la migration échoue ou n'est pas fournie
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}