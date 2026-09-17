package ai.vigia.app.repo

import ai.vigia.app.local.TokenStore
import ai.vigia.app.local.VigiaDatabase
import ai.vigia.app.net.ApiService
import ai.vigia.app.net.DeleteAccountRequest
import ai.vigia.app.net.LoginRequest
import ai.vigia.app.net.RefreshRequest
import ai.vigia.app.net.RegisterRequest
import ai.vigia.app.net.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val api: ApiService,
    private val tokens: TokenStore,
    private val db: VigiaDatabase
) {
    val isLoggedIn: Boolean get() = tokens.isLoggedIn
    val currentEmail: String? get() = tokens.email

    suspend fun register(email: String, password: String, fullName: String, device: String): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = api.register(RegisterRequest(email.trim(), password, fullName.trim(), device))
                tokens.save(result.accessToken, result.refreshToken, email.trim())
            }.fold({ Outcome.Success(Unit) }, { toFailure(it) })
        }

    suspend fun login(email: String, password: String, device: String): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = api.login(LoginRequest(email.trim(), password, device))
                tokens.save(result.accessToken, result.refreshToken, email.trim())
            }.fold({ Outcome.Success(Unit) }, { toFailure(it) })
        }

    suspend fun me(): Outcome<UserResponse> = withContext(Dispatchers.IO) {
        runCatching { api.me() }.fold({ Outcome.Success(it) }, { toFailure(it) })
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        tokens.refreshToken?.let { runCatching { api.logout(RefreshRequest(it)) } }
        tokens.clear()
        db.analyses().clear()
    }

    suspend fun deleteAccount(password: String): Outcome<Unit> = withContext(Dispatchers.IO) {
        runCatching { api.deleteAccount(DeleteAccountRequest(password)) }
            .fold({
                tokens.clear()
                db.analyses().clear()
                Outcome.Success(Unit)
            }, { toFailure(it) })
    }
}
