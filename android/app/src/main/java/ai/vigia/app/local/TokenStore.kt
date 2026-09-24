package ai.vigia.app.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage chiffre des jetons (AES-256-GCM, cle dans le Keystore Android).
 * Les jetons de session et la clé Hugging Face de l'utilisateur sont stockés chiffrés.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        // Aucun fallback en clair : les jetons de session et la clé Hugging Face sont
        // des secrets. Si le Keystore Android est indisponible, VIGIA échoue fermé
        // plutôt que de les écrire dans SharedPreferences ordinaires.
        EncryptedSharedPreferences.create(
            context,
            "vigia_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_REFRESH, value).apply()

    var hfToken: String?
        get() = prefs.getString(KEY_HF_TOKEN, null)
        set(value) { if (value.isNullOrBlank()) prefs.edit().remove(KEY_HF_TOKEN).apply() else prefs.edit().putString(KEY_HF_TOKEN, value).apply() }

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
        const val KEY_HF_TOKEN = "hf_token"
        const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    }
}
