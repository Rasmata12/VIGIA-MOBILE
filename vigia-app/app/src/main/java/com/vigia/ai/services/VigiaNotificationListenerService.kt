package com.vigia.ai.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.vigia.ai.MainActivity
import com.vigia.ai.data.AnalysisHistory
import com.vigia.ai.data.VigiaDatabase
import com.vigia.ai.domain.LocalRiskEngine
import com.vigia.ai.domain.RiskLevel
import com.vigia.ai.domain.RiskResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VigiaNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName == applicationContext.packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val fullContent = "$title $text".trim()

        if (fullContent.isNotBlank()) {
            val riskResult = LocalRiskEngine.analyzeText(fullContent)

            // Sauvegarde réelle dans la base de données locale Room
            serviceScope.launch {
                val db = VigiaDatabase.getDatabase(applicationContext)
                val summary = if (fullContent.length > 60) fullContent.take(60) + "..." else fullContent
                db.historyDao().insertHistory(
                    AnalysisHistory(
                        type = "GUARD (Auto)",
                        contentSummary = summary,
                        riskScore = riskResult.score,
                        riskLevel = riskResult.level.label
                    )
                )
            }

            // Déclencher une vraie alerte Android si le risque est élevé ou critique
            if (riskResult.level == RiskLevel.ELEVE || riskResult.level == RiskLevel.CRITIQUE) {
                triggerVigiaAlert(riskResult, fullContent)
            }
        }
    }

    private fun triggerVigiaAlert(result: RiskResult, originalContent: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vigia_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VIGIA Alertes de Sécurité",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes de risque et tentatives de fraude interceptées"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Deep link vers l'application
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ALERT_CONTENT", originalContent)
            putExtra("EXTRA_ALERT_SCORE", result.score)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val signalsText = result.signals.joinToString("\n• ", prefix = "• ")
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🛡️ VIGIA : RISQUE ${result.level.label}")
            .setContentText("Prenez une seconde pour vérifier avant d'agir.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Score : ${result.score}/100\n\nSignaux observés :\n$signalsText\n\n${result.recommendation}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
