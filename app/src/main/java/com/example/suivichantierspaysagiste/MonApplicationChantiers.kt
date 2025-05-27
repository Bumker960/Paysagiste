package com.example.suivichantierspaysagiste

import android.app.Application

class MonApplicationChantiers : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val chantierRepository by lazy {
        ChantierRepository(
            database.chantierDao(),
            database.interventionDao(),
            database.desherbagePlanifieDao(),
            database.prestationHorsContratDao() // Injection du nouveau DAO
        )
    }
}
