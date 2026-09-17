package ai.vigia.app.guard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * CENTRE DE PERMISSIONS — lit l'etat REEL de chaque permission Android.
 * Aucun etat n'est suppose ni memorise : tout est relu a chaque affichage.
 */
object PermissionCenter {

    enum class State { GRANTED, DENIED, NOT_REQUIRED, ACTION_REQUIRED }

    data class Item(
        val key: String,
        val name: String,
        val why: String,
        val state: State,
        val actionLabel: String?,
        val intent: Intent?
    )

    fun snapshot(context: Context): List<Item> = listOf(
        internet(),
        notifications(context),
        camera(context),
        notificationListener(context)
    )

    private fun internet() = Item(
        key = "internet",
        name = "Accès Internet",
        why = "Communiquer avec le serveur d'analyse. L'analyse locale fonctionne sans.",
        state = State.GRANTED,   // permission normale, accordée à l'installation
        actionLabel = null,
        intent = null
    )

    private fun notifications(context: Context): Item {
        val state = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) State.NOT_REQUIRED
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) State.GRANTED
        else State.ACTION_REQUIRED
        return Item(
            key = "notifications",
            name = "Notifications",
            why = "T'alerter quand une analyse détecte un risque réel. Aucune notification décorative n'est envoyée.",
            state = state,
            actionLabel = if (state == State.ACTION_REQUIRED) "Autoriser" else null,
            intent = if (state == State.ACTION_REQUIRED) appSettingsIntent(context) else null
        )
    }

    private fun camera(context: Context): Item {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        return Item(
            key = "camera",
            name = "Caméra",
            why = "Scanner un QR code. Demandée uniquement au moment du scan, jamais au démarrage.",
            state = if (granted) State.GRANTED else State.DENIED,
            actionLabel = if (granted) null else "Ouvrir les réglages",
            intent = if (granted) null else appSettingsIntent(context)
        )
    }

    private fun notificationListener(context: Context): Item {
        val enabled = VigiaNotificationListener.isListenerEnabled(context)
        return Item(
            key = "notification_listener",
            name = "Accès aux notifications (VIGIA Guard)",
            why = "Analyser automatiquement les messages reçus dans les applications de messagerie. " +
                "Activation manuelle obligatoire dans les réglages Android ; révocable à tout moment.",
            state = if (enabled) State.GRANTED else State.ACTION_REQUIRED,
            actionLabel = if (enabled) "Gérer" else "Activer dans Android",
            intent = VigiaNotificationListener.settingsIntent()
        )
    }

    private fun appSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
