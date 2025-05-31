package com.example.suivichantierspaysagiste

import android.app.Application

class MonApplicationChantiers : Application() {
    // Instance de la base de données (existante)
    val database by lazy { AppDatabase.getDatabase(this) }

    // NOUVEAU: Instance de DataBackupManager
    private val dataBackupManager by lazy { DataBackupManager(this) }

    // Instance du ChantierRepository (existante, mais la factory du ViewModel en dépendra)
    val chantierRepository by lazy {
        ChantierRepository(
            database.chantierDao(),
            database.interventionDao(),
            database.desherbagePlanifieDao(),
            database.prestationHorsContratDao(),
            database.devisDao()
        )
    }

    // NOUVEAU: Fournir une instance de ChantierViewModelFactory
    // Cette factory sera utilisée dans MainActivity pour obtenir le ChantierViewModel.
    // Elle a besoin de l'application, du repository, du dataBackupManager et de la database.
    val chantierViewModelFactory by lazy {
        ChantierViewModelFactory(
            this,
            chantierRepository,
            dataBackupManager,
            database // Passer l'instance de la base de données
        )
    }
}
