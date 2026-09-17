package ai.vigia.app.repo

import retrofit2.HttpException
import java.io.IOException

sealed interface Outcome<out T> {
    data class Success<T>(val data: T, val offline: Boolean = false) : Outcome<T>
    data class Failure(val message: String, val kind: ErrorKind) : Outcome<Nothing>
}

enum class ErrorKind { NETWORK, AUTH, SERVER, VALIDATION, RATE_LIMIT, UNKNOWN }

/** Traduit une exception en message clair. Aucun faux resultat n'est jamais renvoye. */
fun toFailure(t: Throwable): Outcome.Failure = when (t) {
    is IOException -> Outcome.Failure(
        "Pas de connexion au serveur VIGIA. Vérifie ton réseau — l'analyse locale reste disponible.",
        ErrorKind.NETWORK
    )
    is HttpException -> {
        val detail = runCatching {
            t.response()?.errorBody()?.string()?.let { body ->
                ai.vigia.app.net.ApiFactory.json.decodeFromString(
                    ai.vigia.app.net.ApiError.serializer(), body
                ).detail
            }
        }.getOrNull()
        when (t.code()) {
            401 -> Outcome.Failure(detail ?: "Session expirée, reconnecte-toi.", ErrorKind.AUTH)
            403 -> Outcome.Failure(detail ?: "Accès refusé.", ErrorKind.AUTH)
            409 -> Outcome.Failure(detail ?: "Cet email est déjà utilisé.", ErrorKind.VALIDATION)
            422 -> Outcome.Failure(detail ?: "Données invalides.", ErrorKind.VALIDATION)
            429 -> Outcome.Failure(detail ?: "Trop de requêtes, réessaie dans quelques minutes.", ErrorKind.RATE_LIMIT)
            in 500..599 -> Outcome.Failure("Le serveur VIGIA est indisponible. Réessaie plus tard.", ErrorKind.SERVER)
            else -> Outcome.Failure(detail ?: "Erreur ${t.code()}.", ErrorKind.UNKNOWN)
        }
    }
    else -> Outcome.Failure(t.message ?: "Erreur inattendue.", ErrorKind.UNKNOWN)
}
