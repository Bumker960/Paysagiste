package com.example.suivichantierspaysagiste

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import java.util.concurrent.TimeUnit

class ChronomailleurService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null
    private var currentIntervention: Intervention? = null
    private var chantierNomForNotification: String? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var chantierRepository: ChantierRepository


    companion object {
        const val ACTION_START = "com.example.suivichantierspaysagiste.ACTION_START"
        const val ACTION_STOP = "com.example.suivichantierspaysagiste.ACTION_STOP"
        const val EXTRA_CHANTIER_ID = "extra_chantier_id"
        const val EXTRA_TYPE_INTERVENTION = "extra_type_intervention"
        const val EXTRA_CHANTIER_NOM = "extra_chantier_nom"
        const val EXTRA_NOTES = "extra_notes"


        private const val NOTIFICATION_ID = 123
        const val NOTIFICATION_CHANNEL_ID = "chronomailleur_channel"

        private val _serviceState = MutableStateFlow<ChronoServiceState>(ChronoServiceState.Idle)
        val serviceState: StateFlow<ChronoServiceState> = _serviceState.asStateFlow()

        fun formatDuration(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    sealed class ChronoServiceState {
        object Idle : ChronoServiceState()
        data class Running(
            val interventionId: Long,
            val chantierId: Long,
            val typeIntervention: String,
            val nomChantier: String,
            val heureDebut: Long,
            val dureeFormattee: String
        ) : ChronoServiceState()
    }


    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        chantierRepository = (application as MonApplicationChantiers).chantierRepository
        createNotificationChannelIfNeeded()
        Log.d("ChronomailleurService", "Service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ChronomailleurService", "onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                val chantierId = intent.getLongExtra(EXTRA_CHANTIER_ID, -1L)
                val typeIntervention = intent.getStringExtra(EXTRA_TYPE_INTERVENTION)
                chantierNomForNotification = intent.getStringExtra(EXTRA_CHANTIER_NOM) ?: "Chantier"

                if (chantierId != -1L && typeIntervention != null) {
                    serviceScope.launch {
                        if (currentIntervention != null || _serviceState.value is ChronoServiceState.Running) {
                            Log.w("ChronomailleurService", "Un chronomètre est déjà en cours.")
                            return@launch
                        }

                        val maintenant = Date()
                        val nouvelleIntervention = Intervention(
                            chantierId = chantierId,
                            typeIntervention = typeIntervention,
                            dateIntervention = maintenant,
                            heureDebut = maintenant,
                            statutIntervention = InterventionStatus.IN_PROGRESS.name
                        )
                        try {
                            val idIntervention = chantierRepository.insertIntervention(nouvelleIntervention)
                            currentIntervention = chantierRepository.getInterventionById(idIntervention)
                            currentIntervention?.let {
                                Log.d("ChronomailleurService", "Intervention démarrée: ID ${it.id}, Type: ${it.typeIntervention}")
                                startTimerAndForeground(it)
                            }
                        } catch (e: Exception) {
                            Log.e("ChronomailleurService", "Erreur lors du démarrage de l'intervention", e)
                            stopSelf()
                        }
                    }
                } else {
                    Log.e("ChronomailleurService", "ID Chantier ou Type d'intervention manquant pour START")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                val notes = intent.getStringExtra(EXTRA_NOTES)
                serviceScope.launch {
                    stopTimerAndForeground(notes)
                }
            }
        }
        return START_STICKY
    }

    private suspend fun startTimerAndForeground(intervention: Intervention) {
        currentIntervention = intervention
        val heureDebutMillis = intervention.heureDebut?.time ?: System.currentTimeMillis()

        _serviceState.value = ChronoServiceState.Running(
            interventionId = intervention.id,
            chantierId = intervention.chantierId,
            typeIntervention = intervention.typeIntervention,
            nomChantier = chantierNomForNotification ?: "Chantier",
            heureDebut = heureDebutMillis,
            dureeFormattee = formatDuration(0)
        )

        startForeground(NOTIFICATION_ID, buildNotification(formatDuration(0)))
        Log.d("ChronomailleurService", "Service en avant-plan démarré.")

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            try {
                while (isActive) {
                    currentIntervention?.heureDebut?.let { debut ->
                        val dureeMillis = System.currentTimeMillis() - debut.time
                        val dureeFormattee = formatDuration(dureeMillis)
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(dureeFormattee))

                        if (_serviceState.value is ChronoServiceState.Running) {
                            val currentState = _serviceState.value as ChronoServiceState.Running
                            _serviceState.value = currentState.copy(dureeFormattee = dureeFormattee)
                        }
                    }
                    delay(1000)
                }
            } catch (e: CancellationException) {
                Log.d("ChronomailleurService", "Timer annulé.")
            } catch (e: Exception) {
                Log.e("ChronomailleurService", "Erreur dans le timer", e)
            }
        }
    }

    private suspend fun stopTimerAndForeground(notes: String?) {
        timerJob?.cancel()
        timerJob = null
        Log.d("ChronomailleurService", "Timer arrêté.")

        currentIntervention?.let { interventionActive ->
            val maintenant = Date()
            val duree = interventionActive.heureDebut?.let { maintenant.time - it.time } ?: 0L

            val interventionTerminee = interventionActive.copy(
                heureFin = maintenant,
                dureeEffective = duree,
                notes = notes?.ifBlank { null } ?: interventionActive.notes,
                statutIntervention = InterventionStatus.COMPLETED.name
            )
            try {
                chantierRepository.updateIntervention(interventionTerminee)
                Log.d("ChronomailleurService", "Intervention terminée et mise à jour: ID ${interventionTerminee.id}")
            } catch (e: Exception) {
                Log.e("ChronomailleurService", "Erreur lors de la mise à jour de l'intervention terminée", e)
            }
        }
        currentIntervention = null
        chantierNomForNotification = null
        _serviceState.value = ChronoServiceState.Idle

        // Correction pour la compatibilité de stopForeground:
        // stopForeground(true) est disponible depuis API 5 et supprime la notification.
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
        Log.d("ChronomailleurService", "Service arrêté et notification retirée.")
    }


    private fun buildNotification(contentText: String): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val stopIntent = Intent(this, ChronomailleurService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, pendingIntentFlags)

        val typeInterventionLisible = currentIntervention?.typeIntervention?.replace(" de ", " ") ?: "Intervention"
        val titreNotification = "$typeInterventionLisible en cours"
        val sousTexteNotification = chantierNomForNotification ?: "Suivi de chantier"

        // Utilisation d'une icône système Android pour l'action "Terminer" comme placeholder.
        // Il est fortement recommandé de créer votre propre icône "stop" (ex: ic_action_stop.xml)
        // et de la placer dans res/drawable, puis de la référencer ici (ex: R.drawable.ic_action_stop).
        val stopIcon = android.R.drawable.ic_media_pause // Placeholder, idéalement une icône "stop"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(titreNotification)
            .setContentText("Temps écoulé: $contentText")
            .setSubText(sousTexteNotification)
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Utilisez une icône appropriée pour la notification
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(stopIcon, "Terminer", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Chronomètre Service Channel",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Affiche le chronomètre en cours pour une intervention"
                    setSound(null, null)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d("ChronomailleurService", "Canal de notification créé.")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        Log.d("ChronomailleurService", "Service onDestroy")
    }
}