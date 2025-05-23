package com.example.suivichantierspaysagiste // Adaptez à votre nom de package

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Chantier::class, Intervention::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class) // Référence à votre classe Converters.kt
abstract class AppDatabase : RoomDatabase() {

    abstract fun chantierDao(): ChantierDao
    abstract fun interventionDao(): InterventionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "suivi_chantiers_database" // Nom du fichier de base de données
                )
                    // Si vous prévoyez des modifications de schéma plus tard, vous devrez gérer les migrations.
                    // Pour l'instant, .fallbackToDestructiveMigration() est une option simple
                    // qui supprime et recrée la base de données en cas de changement de version.
                    // .fallbackToDestructiveMigration() // Décommentez si besoin pour une version ultérieure.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}