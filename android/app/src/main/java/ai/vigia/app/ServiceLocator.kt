package ai.vigia.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import ai.vigia.app.local.TokenStore
import ai.vigia.app.local.VigiaDatabase
import ai.vigia.app.net.ApiFactory
import ai.vigia.app.net.ApiService
import ai.vigia.app.repo.AnalysisRepository
import ai.vigia.app.repo.AuthRepository

/** Injection de dependances minimale et explicite (pas de framework superflu). */
object ServiceLocator {

    private lateinit var appContext: Context

    val tokens: TokenStore by lazy { TokenStore(appContext) }
    val database: VigiaDatabase by lazy { VigiaDatabase.get(appContext) }
    val api: ApiService by lazy { ApiFactory.create(tokens) }

    val authRepository: AuthRepository by lazy { AuthRepository(api, tokens, database) }
    val analysisRepository: AnalysisRepository by lazy {
        AnalysisRepository(api, database.analyses(), database.pending()) { isOnline() }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isOnline(): Boolean {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
