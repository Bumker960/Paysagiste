package com.example.suivichantierspaysagiste

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DataBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "DataBackupManager"
        const val DATABASE_NAME = "suivi_chantiers_database" // Nom de votre base de données Room
        const val DEVIS_FOLDER_NAME = "dossier_devis" // Dossier contenant les PDF des devis
        const val PREFERENCES_FOLDER_NAME = "datastore" // Dossier standard pour DataStore
        const val PREFERENCES_FILE_NAME = "settings.preferences_pb" // Nom de votre fichier DataStore

        // Noms des entrées dans le fichier ZIP
        private const val ZIP_DB_FOLDER = "database/"
        private const val ZIP_DEVIS_FOLDER = "devis_pdfs/"
        private const val ZIP_PREFS_FOLDER = "preferences/"
    }

    /**
     * Exporte toutes les données de l'application dans un fichier ZIP à l'URI spécifiée.
     * @param zipFileUri L'URI où le fichier ZIP de sauvegarde sera écrit.
     * @return Boolean True si l'exportation a réussi, False sinon.
     */
    suspend fun exportData(zipFileUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(zipFileUri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zos ->
                        // 1. Sauvegarder la base de données Room
                        val dbFolder = context.getDatabasePath(DATABASE_NAME).parentFile
                        if (dbFolder != null && dbFolder.exists()) {
                            // Room peut créer plusieurs fichiers (db, -shm, -wal)
                            dbFolder.listFiles { _, name ->
                                name.startsWith(DATABASE_NAME)
                            }?.forEach { file ->
                                Log.d(TAG, "Ajout du fichier de base de données au ZIP: ${file.name}")
                                addFileToZip(zos, file, ZIP_DB_FOLDER + file.name)
                            }
                        } else {
                            Log.w(TAG, "Dossier de la base de données non trouvé: ${dbFolder?.absolutePath}")
                        }

                        // 2. Sauvegarder le dossier des devis PDF
                        val devisDir = File(context.filesDir, DEVIS_FOLDER_NAME)
                        if (devisDir.exists() && devisDir.isDirectory) {
                            Log.d(TAG, "Ajout du dossier des devis au ZIP: ${devisDir.name}")
                            addFolderToZip(zos, devisDir, ZIP_DEVIS_FOLDER)
                        } else {
                            Log.w(TAG, "Dossier des devis non trouvé ou n'est pas un répertoire: ${devisDir.absolutePath}")
                        }

                        // 3. Sauvegarder le fichier de préférences DataStore
                        val prefsFile = context.preferencesDataStoreFile(PREFERENCES_FILE_NAME)
                        if (prefsFile.exists()) {
                            Log.d(TAG, "Ajout du fichier de préférences au ZIP: ${prefsFile.name}")
                            addFileToZip(zos, prefsFile, ZIP_PREFS_FOLDER + prefsFile.name)
                        } else {
                            Log.w(TAG, "Fichier de préférences DataStore non trouvé: ${prefsFile.absolutePath}")
                        }
                        Log.i(TAG, "Exportation des données terminée avec succès.")
                    }
                } ?: return@withContext false // Échec de l'ouverture de l'OutputStream
                true
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'exportation des données", e)
                false
            }
        }
    }

    /**
     * Importe les données de l'application à partir d'un fichier ZIP.
     * ATTENTION: Cette opération écrase les données existantes.
     * @param zipFileUri L'URI du fichier ZIP de sauvegarde à importer.
     * @param dbHelper Instance de AppDatabase pour fermer la connexion avant l'import.
     * @return Boolean True si l'importation a réussi, False sinon.
     */
    suspend fun importData(zipFileUri: Uri, dbHelper: AppDatabase): Boolean {
        return withContext(Dispatchers.IO) {
            // Étape cruciale : Fermer la base de données Room avant de remplacer ses fichiers
            if (dbHelper.isOpen) {
                Log.d(TAG, "Fermeture de la base de données Room avant l'importation.")
                dbHelper.close()
            }

            try {
                // Nettoyer les anciens fichiers avant l'importation
                Log.d(TAG, "Nettoyage des anciennes données avant l'importation.")
                clearApplicationDataForImport()

                context.contentResolver.openInputStream(zipFileUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var zipEntry: ZipEntry? = zis.nextEntry
                        while (zipEntry != null) {
                            val entryName = zipEntry.name
                            Log.d(TAG, "Extraction de l'entrée ZIP: $entryName")
                            val targetFile = when {
                                entryName.startsWith(ZIP_DB_FOLDER) -> {
                                    val dbFileName = entryName.substring(ZIP_DB_FOLDER.length)
                                    // Les fichiers de la base de données sont stockés dans le répertoire databases de l'application
                                    val dbDir = context.getDatabasePath(DATABASE_NAME).parentFile
                                    if (dbDir != null && !dbDir.exists()) dbDir.mkdirs()
                                    File(dbDir, dbFileName)
                                }
                                entryName.startsWith(ZIP_DEVIS_FOLDER) -> {
                                    val devisFileName = entryName.substring(ZIP_DEVIS_FOLDER.length)
                                    val devisDir = File(context.filesDir, DEVIS_FOLDER_NAME)
                                    if (!devisDir.exists()) devisDir.mkdirs()
                                    File(devisDir, devisFileName)
                                }
                                entryName.startsWith(ZIP_PREFS_FOLDER) -> {
                                    val prefsFileName = entryName.substring(ZIP_PREFS_FOLDER.length)
                                    // Les fichiers DataStore sont dans un sous-répertoire "datastore"
                                    val datastoreDir = File(context.filesDir, PREFERENCES_FOLDER_NAME)
                                    if (!datastoreDir.exists()) datastoreDir.mkdirs()
                                    File(datastoreDir, prefsFileName)
                                }
                                else -> {
                                    Log.w(TAG, "Entrée ZIP inconnue ou non gérée: $entryName")
                                    null
                                }
                            }

                            if (targetFile != null) {
                                if (zipEntry.isDirectory) {
                                    if (!targetFile.exists()) {
                                        targetFile.mkdirs()
                                        Log.d(TAG, "Création du répertoire: ${targetFile.absolutePath}")
                                    }
                                } else {
                                    // Assurez-vous que le répertoire parent existe pour les fichiers
                                    targetFile.parentFile?.mkdirs()
                                    FileOutputStream(targetFile).use { fos ->
                                        val buffer = ByteArray(1024)
                                        var len: Int
                                        while (zis.read(buffer).also { len = it } > 0) {
                                            fos.write(buffer, 0, len)
                                        }
                                    }
                                    Log.d(TAG, "Fichier extrait: ${targetFile.absolutePath}")
                                }
                            }
                            zis.closeEntry()
                            zipEntry = zis.nextEntry
                        }
                    }
                } ?: return@withContext false // Échec de l'ouverture de l'InputStream
                Log.i(TAG, "Importation des données terminée avec succès.")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'importation des données", e)
                // En cas d'erreur, il pourrait être judicieux de tenter de restaurer un état précédent
                // ou de laisser l'application dans un état "propre" si possible.
                // Pour l'instant, nous signalons simplement l'échec.
                false
            }
            // La base de données sera rouverte au prochain appel à AppDatabase.getDatabase()
        }
    }

    /**
     * Ajoute un fichier individuel à l'archive ZIP.
     */
    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) {
            Log.w(TAG, "Le fichier à zipper n'existe pas: ${file.absolutePath}")
            return
        }
        FileInputStream(file).use { fis ->
            val zipEntry = ZipEntry(entryName)
            zos.putNextEntry(zipEntry)
            val buffer = ByteArray(1024)
            var len: Int
            while (fis.read(buffer).also { len = it } > 0) {
                zos.write(buffer, 0, len)
            }
            zos.closeEntry()
            Log.d(TAG, "Fichier ajouté au ZIP: $entryName")
        }
    }

    /**
     * Ajoute récursivement le contenu d'un dossier à l'archive ZIP.
     */
    private fun addFolderToZip(zos: ZipOutputStream, folder: File, baseEntryPath: String) {
        folder.listFiles()?.forEach { file ->
            val entryName = if (baseEntryPath.endsWith("/")) {
                baseEntryPath + file.name
            } else {
                "$baseEntryPath/${file.name}"
            }

            if (file.isDirectory) {
                addFolderToZip(zos, file, "$entryName/")
            } else {
                addFileToZip(zos, file, entryName)
            }
        }
    }

    /**
     * Nettoie les données de l'application avant une importation.
     * Cela inclut la suppression des fichiers de base de données, du dossier des devis et des préférences.
     */
    private fun clearApplicationDataForImport() {
        // 1. Supprimer les fichiers de la base de données Room
        val dbFolder = context.getDatabasePath(DATABASE_NAME).parentFile
        if (dbFolder != null && dbFolder.exists()) {
            dbFolder.listFiles { _, name -> name.startsWith(DATABASE_NAME) }?.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "Ancien fichier de base de données supprimé: ${file.name}")
                } else {
                    Log.w(TAG, "Échec de la suppression de l'ancien fichier de base de données: ${file.name}")
                }
            }
        }

        // 2. Supprimer le contenu du dossier des devis PDF
        val devisDir = File(context.filesDir, DEVIS_FOLDER_NAME)
        if (devisDir.exists() && devisDir.isDirectory) {
            if (deleteRecursive(devisDir)) { // Supprime le dossier puis le recrée vide
                devisDir.mkdirs()
                Log.d(TAG, "Ancien dossier des devis nettoyé et recréé.")
            } else {
                Log.w(TAG, "Échec partiel ou total du nettoyage du dossier des devis.")
            }
        }


        // 3. Supprimer le fichier de préférences DataStore
        // DataStore est un peu plus délicat. Supprimer le fichier directement peut fonctionner,
        // mais il est généralement préférable de le laisser se recréer.
        // L'importation écrasera le fichier de toute façon.
        // Cependant, pour être sûr, nous pouvons tenter de supprimer le fichier.
        val datastoreDir = File(context.filesDir, PREFERENCES_FOLDER_NAME)
        val prefsFile = File(datastoreDir, PREFERENCES_FILE_NAME)
        if (prefsFile.exists()) {
            if (prefsFile.delete()) {
                Log.d(TAG, "Ancien fichier de préférences DataStore supprimé: ${prefsFile.name}")
            } else {
                Log.w(TAG, "Échec de la suppression de l'ancien fichier de préférences DataStore: ${prefsFile.name}")
            }
        }
        // Si le dossier datastore est vide après cela, on peut le supprimer aussi.
        if (datastoreDir.exists() && datastoreDir.isDirectory && datastoreDir.listFiles()?.isEmpty() == true) {
            datastoreDir.delete()
        }
    }

    /**
     * Supprime un fichier ou un dossier de manière récursive.
     * @param fileOrDirectory Le fichier ou dossier à supprimer.
     * @return True si la suppression a réussi, False sinon.
     */
    private fun deleteRecursive(fileOrDirectory: File): Boolean {
        var success = true
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                success = success && deleteRecursive(child)
            }
        }
        if (success) {
            success = fileOrDirectory.delete()
            if (success) {
                Log.d(TAG, "Supprimé : ${fileOrDirectory.absolutePath}")
            } else {
                Log.w(TAG, "Échec de la suppression : ${fileOrDirectory.absolutePath}")
            }
        }
        return success
    }
}