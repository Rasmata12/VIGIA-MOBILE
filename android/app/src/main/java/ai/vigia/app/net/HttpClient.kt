package ai.vigia.app.net

import ai.vigia.app.BuildConfig
import ai.vigia.app.local.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Ajoute le jeton d'acces et rafraichit automatiquement la session expiree. */
class AuthInterceptor(
    private val tokens: TokenStore,
    private val refresher: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.encodedPath.contains("/auth/")) return chain.proceed(original)

        val access = tokens.accessToken
        var response = chain.proceed(signed(original, access))

        if (response.code == 401) {
            response.close()
            val fresh = synchronized(this) { refresher() }
            response = if (fresh != null) chain.proceed(signed(original, fresh))
            else chain.proceed(signed(original, null))
        }
        return response
    }

    private fun signed(request: Request, token: String?): Request =
        if (token.isNullOrBlank()) request
        else request.newBuilder().header("Authorization", "Bearer $token").build()
}

object ApiFactory {

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun create(tokens: TokenStore): ApiService {
        // Client sans authentification, utilise uniquement pour rafraichir la session.
        val plain = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)

        val refresher: () -> String? = {
            val refresh = tokens.refreshToken
            if (refresh.isNullOrBlank()) null else try {
                val result = kotlinx.coroutines.runBlocking { plain.refresh(RefreshRequest(refresh)) }
                tokens.save(result.accessToken, result.refreshToken)
                result.accessToken
            } catch (e: Exception) {
                tokens.clear()
                null
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokens, refresher))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)   // l'analyse IA peut prendre du temps
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}
