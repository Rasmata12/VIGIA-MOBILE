package ai.vigia.app.guard

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import ai.vigia.app.ServiceLocator
import ai.vigia.app.net.GuardEventRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * VIGIA GUARD — service Android officiel de lecture des notifications.
 *
 * Limites respectees :
 *  - l'utilisateur doit activer explicitement l'acces dans les Reglages Android ;
 *  - seules les notifications des applications de messagerie connues sont traitees ;
 *  - aucun contenu n'est conserve localement : il est envoye pour analyse puis oublie ;
 *  - le service peut etre coupe a tout moment (interrupteur Guard ou Reglages Android).
 *
 * Ce n'est pas un keylogger : il ne lit rien d'autre que ce qu'Android expose
 * officiellement via NotificationListenerService.
 */
class VigiaNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "VigiaGuard"

        /** Applications dont les notifications peuvent contenir un message entrant. */
        val WATCHED_PACKAGES = setOf(
            "com.whatsapp", "com.whatsapp.w4b", "com.google.android.apps.messaging",
            "com.samsung.android.messaging", "org.telegram.messenger", "com.facebook.orca",
            "com.google.android.gm", "com.microsoft.office.outlook", "com.viber.voip",
            "com.instagram.android", "com.facebook.katana", "com.imo.android.imoim"
        )

        /** Etat REEL du service, lu depuis Android — jamais suppose. */
        fun isListenerEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            val component = ComponentName(context, VigiaNotificationListener::class.java)
            return flat.split(":").any {
                ComponentName.unflattenFromString(it)?.packageName == component.packageName &&
                    ComponentName.unflattenFromString(it)?.className == component.className
            }
        }

        fun settingsIntent() = android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    @Volatile private var connected = false

    override fun onListenerConnected() {
        connected = true
        Log.i(TAG, "GUARD listener connecte")
    }

    override fun onListenerDisconnected() {
        connected = false
        Log.i(TAG, "GUARD listener deconnecte")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName !in WATCHED_PACKAGES) return
        if (!ServiceLocator.tokens.isLoggedIn) return
        if (!GuardPreferences.isEnabled(applicationContext)) return

        val extras = notification.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.length < 12) return

        // Deduplication : Android reposte la meme notification a chaque mise a jour.
        if (!GuardPreferences.shouldProcess(applicationContext, notification.packageName, text)) return

        scope.launch {
            runCatching {
                val response = ServiceLocator.api.guardEvent(
                    GuardEventRequest(packageName = notification.packageName, title = title, text = text)
                )
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Log.i(TAG, "GUARD analyse=${body.assessment.level} score=${body.assessment.score}")
                    ai.vigia.app.notif.Notifier.threatDetected(
                        applicationContext, body.analysisId, body.assessment.level, body.assessment.summary
                    )
                } else if (response.code() == 403) {
                    // Guard desactive cote serveur : on arrete d'envoyer.
                    GuardPreferences.setEnabled(applicationContext, false)
                    Log.w(TAG, "GUARD desactive cote serveur, arret de l'envoi")
                }
            }.onFailure {
                // Hors ligne : on n'invente aucun resultat, l'evenement est simplement ignore.
                Log.w(TAG, "GUARD envoi impossible: ${it.javaClass.simpleName}")
            }
        }
    }
}
