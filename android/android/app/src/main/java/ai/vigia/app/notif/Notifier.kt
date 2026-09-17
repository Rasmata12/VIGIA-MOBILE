package ai.vigia.app.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ai.vigia.app.MainActivity
import ai.vigia.app.R

/**
 * Notifications reelles uniquement : declenchees par le resultat d'une analyse
 * effectivement realisee. Aucune notification decorative n'est generee.
 */
object Notifier {

    const val CHANNEL_THREATS = "vigia_threats"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_THREATS,
            "Menaces détectées",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Alertes émises lorsqu'une analyse détecte un risque réel." }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun threatDetected(context: Context, analysisId: String, level: String, summary: String) {
        if (level != "dangerous" && level != "suspicious") return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("analysis_id", analysisId)
        }
        val pending = PendingIntent.getActivity(
            context, analysisId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (level == "dangerous") "Menace détectée" else "Contenu suspect détecté"
        val notification = NotificationCompat.Builder(context, CHANNEL_THREATS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summary.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(analysisId.hashCode(), notification) }
    }
}
