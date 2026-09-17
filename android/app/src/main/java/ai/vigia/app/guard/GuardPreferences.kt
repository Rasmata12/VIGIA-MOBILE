package ai.vigia.app.guard

import android.content.Context
import java.security.MessageDigest

/** Etat local de Guard + deduplication des notifications repostees par Android. */
object GuardPreferences {

    private const val PREFS = "vigia_guard"
    private const val KEY_ENABLED = "guard_enabled"
    private const val KEY_LAST_HASHES = "recent_hashes"
    private const val MAX_HASHES = 40

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()

    /**
     * Retourne true une seule fois par contenu : evite d'analyser 10 fois le meme message.
     * Seules des empreintes sont stockees, jamais le texte.
     */
    @Synchronized
    fun shouldProcess(context: Context, packageName: String, text: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$packageName|$text".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val store = prefs(context)
        val existing = store.getStringSet(KEY_LAST_HASHES, emptySet())!!.toMutableList()
        if (digest in existing) return false
        existing.add(0, digest)
        while (existing.size > MAX_HASHES) existing.removeAt(existing.lastIndex)
        store.edit().putStringSet(KEY_LAST_HASHES, existing.toSet()).apply()
        return true
    }

    fun clear(context: Context) = prefs(context).edit().clear().apply()
}
