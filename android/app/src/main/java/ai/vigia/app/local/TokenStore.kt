package ai.vigia.app.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage chiffre des jetons (AES-256-GCM, cle dans le Keystore Android).
 * Aucun secret d'API n'est stocke cote application : seuls les jetons de session le sont.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "vigia_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        // Si le Keystore est indisponible (appareil corrompu), on echoue proprement
        // vers un stockage non chiffre en mode prive plutot que de crasher l'app.
        context.getSharedPreferences("vigia_prefs_fallback", Context.MODE_PRIVATE)
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    val isLoggedIn: Boolean get() = !refreshToken.isNullOrBlank()

    /** Vrai dès que l'utilisateur a déjà vu l'accueil + le carrousel d'onboarding une fois. */
    var onboardingSeen: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, value).apply()

    fun save(access: String, refresh: String, userEmail: String? = null) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .apply()
        if (userEmail != null) email = userEmail
    }

    fun clear() {
        val seenOnboarding = onboardingSeen
        prefs.edit().clear().apply()
        onboardingSeen = seenOnboarding
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EMAIL = "email"
        const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    }
}
