package com.example.suivichantierspaysagiste

import android.app.Application

class MonApplicationChantiers : Application() {
    // Utilisation de 'lazy' pour que la base de données et le repository
    // ne soient créés que lorsqu'ils sont accédés pour la première fois.
    val database by lazy { AppDatabase.getDatabase(this) }
    val chantierRepository by lazy { ChantierRepository(database.chantierDao(), database.interventionDao()) }
}