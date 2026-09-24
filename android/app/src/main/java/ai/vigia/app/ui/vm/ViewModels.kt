package ai.vigia.app.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.vigia.app.ServiceLocator
import ai.vigia.app.local.AnalysisEntity
import ai.vigia.app.net.ApiFactory
import ai.vigia.app.net.SessionDto
import ai.vigia.app.net.SettingsPatch
import ai.vigia.app.net.FullSettingsPatch
import ai.vigia.app.net.SignalDto
import ai.vigia.app.net.SourceDto
import ai.vigia.app.net.StatsResponse
import ai.vigia.app.notif.Notifier
import ai.vigia.app.repo.ErrorKind
import ai.vigia.app.repo.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import ai.vigia.app.net.BeforePayRequest
import ai.vigia.app.net.BeforePayResponse
import ai.vigia.app.net.GuardStatusRequest
import ai.vigia.app.net.GuardStatusResponse
import ai.vigia.app.net.MomentResponse
import ai.vigia.app.net.ShieldResponse
import ai.vigia.app.net.PrivacySummaryDto
import ai.vigia.app.net.DeviceDto
import ai.vigia.app.guard.GuardPreferences
import ai.vigia.app.guard.PermissionCenter
import ai.vigia.app.guard.VigiaNotificationListener
import ai.vigia.app.net.JobOfferRequest
import ai.vigia.app.net.JobOfferResponse
import ai.vigia.app.net.ListingRequest
import ai.vigia.app.net.ListingResponse
import ai.vigia.app.net.CommunityReportRequest
import ai.vigia.app.net.CommunityReportResponse
import ai.vigia.app.net.CommunityCheckResponse
import ai.vigia.app.net.CommunityTrendingResponse

fun parseSignals(json: String): List<SignalDto> =
    runCatching { ApiFactory.json.decodeFromString(ListSerializer(SignalDto.serializer()), json) }.getOrDefault(emptyList())

fun parseSources(json: String): List<SourceDto> =
    runCatching { ApiFactory.json.decodeFromString(ListSerializer(SourceDto.serializer()), json) }.getOrDefault(emptyList())

// ---------------------------------------------------------------- AUTH

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val repo = ServiceLocator.authRepository
    private val _state = MutableStateFlow(AuthUiState(loggedIn = repo.isLoggedIn))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val email: String? get() = repo.currentEmail

    fun login(email: String, password: String) = run(
        validate = { validateLogin(email, password) },
        action = { repo.login(email, password, android.os.Build.MODEL ?: "Android") }
    )

    fun register(email: String, password: String, confirm: String, name: String) = run(
        validate = { validateRegister(email, password, confirm) },
        action = { repo.register(email, password, name, android.os.Build.MODEL ?: "Android") }
    )

    private fun run(validate: () -> String?, action: suspend () -> Outcome<Unit>) {
        val problem = validate()
        if (problem != null) { _state.value = _state.value.copy(error = problem); return }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val outcome = action()) {
                is Outcome.Success -> _state.value = AuthUiState(loggedIn = true)
                is Outcome.Failure -> _state.value = AuthUiState(error = outcome.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _state.value = AuthUiState(loggedIn = false)
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    private fun validateLogin(email: String, password: String): String? = when {
        email.isBlank() || !email.contains("@") -> "Entre une adresse email valide."
        password.isBlank() -> "Entre ton mot de passe."
        else -> null
    }

    private fun validateRegister(email: String, password: String, confirm: String): String? = when {
        email.isBlank() || !email.contains("@") -> "Entre une adresse email valide."
        password.length < 10 -> "Le mot de passe doit contenir au moins 10 caractères."
        !password.any { it.isUpperCase() } || !password.any { it.isLowerCase() } || !password.any { it.isDigit() } ->
            "Le mot de passe doit contenir une majuscule, une minuscule et un chiffre."
        password != confirm -> "Les deux mots de passe ne correspondent pas."
        else -> null
    }
}

// ---------------------------------------------------------------- ANALYSE

data class AnalyzeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val result: AnalysisEntity? = null,
    val offline: Boolean = false
)

class AnalyzeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ServiceLocator.analysisRepository
    private val _state = MutableStateFlow(AnalyzeUiState())
    val state: StateFlow<AnalyzeUiState> = _state.asStateFlow()

    fun analyse(kind: String, content: String, useAi: Boolean = true) {
        if (content.isBlank()) {
            _state.value = AnalyzeUiState(error = "Entre un lien ou colle un message à analyser.")
            return
        }
        _state.value = AnalyzeUiState(loading = true)
        viewModelScope.launch {
            when (val outcome = repo.analyse(kind, content.trim(), useAi)) {
                is Outcome.Success -> {
                    _state.value = AnalyzeUiState(result = outcome.data, offline = outcome.offline)
                    Notifier.threatDetected(
                        getApplication(), outcome.data.id, outcome.data.level, outcome.data.summary
                    )
                }
                is Outcome.Failure -> _state.value = AnalyzeUiState(
                    error = outcome.message + if (outcome.kind == ErrorKind.AUTH) " Reconnecte-toi." else ""
                )
            }
        }
    }

    fun reset() { _state.value = AnalyzeUiState() }
}

// ---------------------------------------------------------------- DASHBOARD

data class DashboardUiState(
    val loading: Boolean = true,
    val stats: StatsResponse? = null,
    val recent: List<AnalysisEntity> = emptyList(),
    val error: String? = null,
    val offline: Boolean = false,
    val localTotal: Int = 0,
    val localDangerous: Int = 0
)

class DashboardViewModel : ViewModel() {
    private val repo = ServiceLocator.analysisRepository
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repo.syncPending()
            val (total, dangerous, _) = repo.localStats()
            when (val stats = repo.stats()) {
                is Outcome.Success -> {
                    repo.refreshHistory()
                    _state.value = _state.value.copy(
                        loading = false, stats = stats.data, offline = false,
                        localTotal = total, localDangerous = dangerous, error = null
                    )
                }
                is Outcome.Failure -> _state.value = _state.value.copy(
                    loading = false, offline = true, error = stats.message,
                    localTotal = total, localDangerous = dangerous
                )
            }
        }
    }

    val history: StateFlow<List<AnalysisEntity>> =
        repo.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// ---------------------------------------------------------------- HISTORIQUE

class HistoryViewModel : ViewModel() {
    private val repo = ServiceLocator.analysisRepository
    val items: StateFlow<List<AnalysisEntity>> =
        repo.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _detail = MutableStateFlow<AnalysisEntity?>(null)
    val detail: StateFlow<AnalysisEntity?> = _detail.asStateFlow()

    init { viewModelScope.launch { repo.refreshHistory() } }

    fun open(id: String) {
        viewModelScope.launch {
            when (val outcome = repo.detail(id)) {
                is Outcome.Success -> _detail.value = outcome.data
                is Outcome.Failure -> _error.value = outcome.message
            }
        }
    }

    fun closeDetail() { _detail.value = null }

    fun delete(id: String) {
        viewModelScope.launch {
            if (repo.delete(id) is Outcome.Failure) _error.value = "Suppression impossible hors ligne côté serveur."
        }
    }

    fun clearAll() { viewModelScope.launch { repo.clearAll() } }
}

// ---------------------------------------------------------------- PARAMETRES

data class SettingsUiState(
    val email: String = "",
    val notifications: Boolean = true,
    val aiEnabled: Boolean = true,
    val serverReachable: Boolean? = null,
    val aiConfigured: Boolean? = null,
    val message: String? = null,
    val deleted: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val auth = ServiceLocator.authRepository
    private val _state = MutableStateFlow(SettingsUiState(email = auth.currentEmail.orEmpty()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { api.health() }.onSuccess {
                _state.value = _state.value.copy(serverReachable = it.status == "ok", aiConfigured = it.aiEnabled)
            }.onFailure {
                _state.value = _state.value.copy(serverReachable = false, aiConfigured = null)
            }
            runCatching { api.settings() }.onSuccess {
                _state.value = _state.value.copy(notifications = it.notificationsEnabled, aiEnabled = it.aiEnabled)
            }
        }
    }

    fun setNotifications(enabled: Boolean) = patch(notifications = enabled)
    fun setAi(enabled: Boolean) = patch(ai = enabled)

    private fun patch(notifications: Boolean? = null, ai: Boolean? = null) {
        viewModelScope.launch {
            runCatching { api.updateSettings(SettingsPatch(notifications, ai)) }
                .onSuccess { _state.value = _state.value.copy(notifications = it.notificationsEnabled, aiEnabled = it.aiEnabled) }
                .onFailure { _state.value = _state.value.copy(message = "Modification impossible : serveur injoignable.") }
        }
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            when (val outcome = auth.deleteAccount(password)) {
                is Outcome.Success -> _state.value = _state.value.copy(deleted = true)
                is Outcome.Failure -> _state.value = _state.value.copy(message = outcome.message)
            }
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }
}

// ---------------------------------------------------------------- BEFORE PAY

data class BeforePayUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val result: BeforePayResponse? = null
)

class BeforePayViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val _state = MutableStateFlow(BeforePayUiState())
    val state: StateFlow<BeforePayUiState> = _state.asStateFlow()

    fun verify(
        message: String,
        url: String,
        beneficiary: String,
        amount: String,
        context: String
    ) {
        if (message.isBlank() && url.isBlank()) {
            _state.value = BeforePayUiState(error = "Renseigne au moins le message reçu ou le lien du paiement.")
            return
        }
        _state.value = BeforePayUiState(loading = true)
        viewModelScope.launch {
            runCatching {
                api.beforePay(
                    BeforePayRequest(
                        message = message.trim(),
                        url = url.trim(),
                        beneficiary = beneficiary.trim(),
                        amount = amount.trim(),
                        context = context.trim()
                    )
                )
            }.onSuccess { response ->
                _state.value = BeforePayUiState(result = response)
            }.onFailure { err ->
                _state.value = BeforePayUiState(error = err.message ?: "Erreur lors de la vérification du paiement.")
            }
        }
    }

    fun reset() { _state.value = BeforePayUiState() }
}

// ---------------------------------------------------------------- GUARD

data class GuardUiState(
    val loading: Boolean = false,
    val localEnabled: Boolean = false,
    val listenerEnabled: Boolean = false,
    val serverStatus: GuardStatusResponse? = null,
    val error: String? = null
)

class GuardViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val _state = MutableStateFlow(GuardUiState())
    val state: StateFlow<GuardUiState> = _state.asStateFlow()

    fun load(context: android.content.Context) {
        val local = GuardPreferences.isEnabled(context)
        val listener = VigiaNotificationListener.isListenerEnabled(context)
        _state.value = _state.value.copy(localEnabled = local, listenerEnabled = listener, loading = true)

        viewModelScope.launch {
            runCatching {
                api.guardStatus(
                    GuardStatusRequest(
                        listenerEnabled = listener,
                        appVersion = "1.0.0",
                        installId = android.provider.Settings.Secure.getString(
                            context.contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID
                        ) ?: "unknown"
                    )
                )
            }.onSuccess { status ->
                _state.value = _state.value.copy(loading = false, serverStatus = status)
            }.onFailure { err ->
                _state.value = _state.value.copy(loading = false, error = err.message)
            }
        }
    }

    fun toggleGuard(context: android.content.Context, enabled: Boolean) {
        GuardPreferences.setEnabled(context, enabled)
        viewModelScope.launch {
            runCatching { api.updateFullSettings(FullSettingsPatch(guardEnabled = enabled)) }
                .onFailure { _state.value = _state.value.copy(error = "Le serveur n'a pas pu enregistrer l'état de Guard.") }
            load(context)
        }
    }

    /** Rafraichit juste les compteurs serveur (GET /guard/status), sans re-declarer l'etat
     * de l'appareil : plus leger que load(), utile pour un pull-to-refresh reactif. */
    fun refresh() {
        viewModelScope.launch {
            runCatching { api.guardStatusRead() }
                .onSuccess { status -> _state.value = _state.value.copy(serverStatus = status, error = null) }
                .onFailure { err -> _state.value = _state.value.copy(error = err.message) }
        }
    }
}

// ---------------------------------------------------------------- MOMENT & SHIELD

data class MomentShieldUiState(
    val loading: Boolean = false,
    val moment: MomentResponse? = null,
    val shield: ShieldResponse? = null,
    val error: String? = null
)

class MomentShieldViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val _state = MutableStateFlow(MomentShieldUiState(loading = true))
    val state: StateFlow<MomentShieldUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val momentRes = runCatching { api.moment() }.getOrNull()
            val shieldRes = runCatching { api.shield() }.getOrNull()
            _state.value = MomentShieldUiState(
                loading = false,
                moment = momentRes,
                shield = shieldRes,
                error = if (momentRes == null && shieldRes == null) "Serveur injoignable." else null
            )
        }
    }

    fun clearMoment() {
        viewModelScope.launch {
            runCatching { api.clearMoment() }
            load()
        }
    }
}

// ---------------------------------------------------------------- PRIVACY

data class PrivacyUiState(
    val loading: Boolean = false,
    val summary: PrivacySummaryDto? = null,
    val message: String? = null,
    val error: String? = null,
    val dataCleared: Boolean = false,
    val exporting: Boolean = false,
    val exportedFileUri: android.net.Uri? = null
)

class PrivacyViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val _state = MutableStateFlow(PrivacyUiState(loading = true))
    val state: StateFlow<PrivacyUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.privacySummary() }
                .onSuccess { _state.value = PrivacyUiState(summary = it) }
                .onFailure { _state.value = PrivacyUiState(error = it.message) }
        }
    }

    fun purgeAllData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { api.deleteAllData() }
                .onSuccess {
                    _state.value = _state.value.copy(loading = false, dataCleared = true, message = "Toutes les données d'analyse ont été supprimées.")
                    load()
                }
                .onFailure { _state.value = _state.value.copy(loading = false, error = "Échec de la purge.") }
        }
    }

    fun applyRetention() {
        viewModelScope.launch {
            runCatching { api.applyRetention() }
                .onSuccess { load() }
        }
    }

    /** Recupere le JSON reel expose par GET /privacy/export et le rend partageable/enregistrable. */
    fun exportData(context: android.content.Context) {
        if (_state.value.exporting) return
        _state.value = _state.value.copy(exporting = true, error = null)
        viewModelScope.launch {
            runCatching {
                val body = api.exportData()
                val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
                val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.FRANCE)
                    .format(java.util.Date())
                val file = java.io.File(dir, "vigia-export-$stamp.json")
                body.byteStream().use { input ->
                    java.io.FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
            }.onSuccess { uri ->
                _state.value = _state.value.copy(exporting = false, exportedFileUri = uri)
            }.onFailure { err ->
                _state.value = _state.value.copy(
                    exporting = false,
                    error = "Échec de l'export : ${err.message ?: "erreur reseau"}"
                )
            }
        }
    }

    /** A appeler apres avoir lance l'intent de partage pour ne pas le redeclencher. */
    fun consumeExportedFile() {
        _state.value = _state.value.copy(exportedFileUri = null)
    }
}

// ---------------------------------------------------------------- PERMISSIONS

data class PermissionsUiState(
    val items: List<PermissionCenter.Item> = emptyList()
)

class PermissionsViewModel : ViewModel() {
    private val _state = MutableStateFlow(PermissionsUiState())
    val state: StateFlow<PermissionsUiState> = _state.asStateFlow()

    fun refresh(context: android.content.Context) {
        _state.value = PermissionsUiState(items = PermissionCenter.snapshot(context))
    }
}

// ---------------------------------------------------------------- DEVICES

data class DevicesUiState(
    val loading: Boolean = false,
    val devices: List<DeviceDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val error: String? = null
)

class DevicesViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val _state = MutableStateFlow(DevicesUiState(loading = true))
    val state: StateFlow<DevicesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val devicesResult = runCatching { api.devices() }
            // Les sessions sont un complement d'info (GET /devices/sessions) : un echec
            // ne doit pas empecher d'afficher la liste des appareils.
            val sessionsResult = runCatching { api.sessions() }
            _state.value = DevicesUiState(
                devices = devicesResult.getOrDefault(_state.value.devices),
                sessions = sessionsResult.getOrDefault(emptyList()),
                error = devicesResult.exceptionOrNull()?.message
            )
        }
    }

    fun revoke(id: String) {
        viewModelScope.launch {
            runCatching { api.revokeDevice(id) }
            load()
        }
    }
}

// ---------------------------------------------------------------- OFFRES D'EMPLOI / FORMATION

data class JobOfferUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val result: JobOfferResponse? = null
)

class JobOfferViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val _state = MutableStateFlow(JobOfferUiState())
    val state: StateFlow<JobOfferUiState> = _state.asStateFlow()

    fun verify(
        content: String,
        companyName: String,
        contactEmail: String,
        salaryPromised: String,
        feeRequested: String
    ) {
        if (content.isBlank()) {
            _state.value = JobOfferUiState(error = "Colle le texte de l'offre reçue. Les autres champs sont facultatifs.")
            return
        }
        _state.value = JobOfferUiState(loading = true)
        viewModelScope.launch {
            runCatching {
                api.jobOffer(
                    JobOfferRequest(
                        content = content,
                        companyName = companyName,
                        contactEmail = contactEmail,
                        salaryPromised = salaryPromised,
                        feeRequested = feeRequested
                    )
                )
            }.onSuccess { _state.value = JobOfferUiState(result = it) }
                .onFailure { _state.value = JobOfferUiState(error = it.message ?: "Erreur réseau. Réessaie.") }
        }
    }

    fun reset() { _state.value = JobOfferUiState() }
}

// ---------------------------------------------------------------- PETITES ANNONCES

data class ListingUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val result: ListingResponse? = null
)

class ListingViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val _state = MutableStateFlow(ListingUiState())
    val state: StateFlow<ListingUiState> = _state.asStateFlow()

    fun verify(
        content: String,
        category: String,
        priceAsked: String,
        sellerContact: String,
        depositRequested: String,
        canVisitInPerson: Boolean?
    ) {
        if (content.isBlank()) {
            _state.value = ListingUiState(error = "Colle le texte de l'annonce à vérifier.")
            return
        }
        _state.value = ListingUiState(loading = true)
        viewModelScope.launch {
            runCatching {
                api.listing(
                    ListingRequest(
                        content = content,
                        category = category,
                        priceAsked = priceAsked,
                        sellerContact = sellerContact,
                        depositRequested = depositRequested,
                        canVisitInPerson = canVisitInPerson
                    )
                )
            }.onSuccess { _state.value = ListingUiState(result = it) }
                .onFailure { _state.value = ListingUiState(error = it.message ?: "Erreur réseau. Réessaie.") }
        }
    }

    fun reset() { _state.value = ListingUiState() }
}

// ---------------------------------------------------------------- ESPACE COMMUNAUTAIRE

data class CommunityUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val checkResult: CommunityCheckResponse? = null,
    val reportResult: CommunityReportResponse? = null,
    val reportSent: Boolean = false,
    val trending: CommunityTrendingResponse? = null
)

class CommunityViewModel : ViewModel() {
    private val api = ServiceLocator.api
    private val _state = MutableStateFlow(CommunityUiState())
    val state: StateFlow<CommunityUiState> = _state.asStateFlow()

    init { refreshTrending() }

    fun refreshTrending() {
        viewModelScope.launch {
            runCatching { api.communityTrending() }
                .onSuccess { _state.value = _state.value.copy(trending = it) }
        }
    }

    fun check(target: String) {
        if (target.isBlank()) {
            _state.value = _state.value.copy(error = "Entre un lien, un domaine ou un numéro à vérifier.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.checkCommunity(target.trim()) }
                .onSuccess { _state.value = _state.value.copy(loading = false, checkResult = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Erreur réseau. Réessaie.") }
        }
    }

    fun report(target: String, category: String, description: String) {
        if (target.isBlank()) {
            _state.value = _state.value.copy(error = "Indique la cible à signaler (lien, domaine ou numéro).")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null, reportSent = false)
        viewModelScope.launch {
            runCatching {
                api.reportToCommunity(CommunityReportRequest(target = target.trim(), category = category, description = description))
            }.onSuccess {
                _state.value = _state.value.copy(loading = false, reportResult = it, reportSent = true)
                refreshTrending()
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message ?: "Erreur réseau. Réessaie.")
            }
        }
    }

    fun reset() { _state.value = CommunityUiState(trending = _state.value.trending) }
}
