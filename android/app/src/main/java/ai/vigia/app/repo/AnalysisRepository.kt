package ai.vigia.app.repo

import ai.vigia.app.engine.LocalAnalyzer
import ai.vigia.app.local.AnalysisDao
import ai.vigia.app.local.AnalysisEntity
import ai.vigia.app.local.PendingAnalysisEntity
import ai.vigia.app.local.PendingDao
import ai.vigia.app.net.AnalyseRequest
import ai.vigia.app.net.AnalysisResponse
import ai.vigia.app.net.ApiFactory
import ai.vigia.app.net.ApiService
import ai.vigia.app.net.SignalDto
import ai.vigia.app.net.SourceDto
import ai.vigia.app.net.StatsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.time.Instant
import java.util.UUID

/**
 * Regle de fonctionnement : on privilegie TOUJOURS le moteur serveur (plus complet).
 * Si le reseau est absent, on execute le moteur LOCAL reel et on marque le resultat
 * comme "analyse hors ligne" — l'utilisateur sait exactement ce qu'il regarde.
 */
class AnalysisRepository(
    private val api: ApiService,
    private val dao: AnalysisDao,
    private val pending: PendingDao,
    private val networkAvailable: () -> Boolean
) {
    fun observeHistory(): Flow<List<AnalysisEntity>> = dao.observeAll()

    suspend fun analyse(kind: String, content: String, useAi: Boolean): Outcome<AnalysisEntity> =
        withContext(Dispatchers.IO) {
            if (!networkAvailable()) return@withContext localOnly(kind, content)
            runCatching { api.analyse(AnalyseRequest(kind, content, online = true, useAi = useAi)) }
                .fold(
                    { response ->
                        val entity = response.toEntity()
                        dao.upsert(entity)
                        Outcome.Success(entity)
                    },
                    { throwable ->
                        val failure = toFailure(throwable)
                        if (failure.kind == ErrorKind.NETWORK || failure.kind == ErrorKind.SERVER) {
                            localOnly(kind, content)
                        } else failure
                    }
                )
        }

    private suspend fun localOnly(kind: String, content: String): Outcome<AnalysisEntity> {
        val result = if (kind == "url") LocalAnalyzer.analyzeUrl(content) else LocalAnalyzer.analyzeText(content)
        if (result.level == "invalid") {
            return Outcome.Failure("Cette adresse n'est pas un lien valide.", ErrorKind.VALIDATION)
        }
        val entity = AnalysisEntity(
            id = "local-" + UUID.randomUUID(),
            kind = kind,
            preview = content.take(280),
            score = result.score,
            level = result.level,
            summary = result.summary,
            signalsJson = ApiFactory.json.encodeToString(
                ListSerializer(SignalDto.serializer()),
                result.signals.map { SignalDto(it.code, it.label, it.weight, it.evidence, "local") }
            ),
            sourcesJson = ApiFactory.json.encodeToString(
                ListSerializer(SourceDto.serializer()),
                listOf(SourceDto("Moteur local VIGIA", "ok", "v${LocalAnalyzer.VERSION} — analyse effectuée sur l'appareil, sans réseau"))
            ),
            aiUsed = false,
            createdAt = Instant.now().toString(),
            syncedWithServer = false
        )
        dao.upsert(entity)
        pending.add(PendingAnalysisEntity(kind = kind, content = content, createdAt = System.currentTimeMillis()))
        return Outcome.Success(entity, offline = true)
    }

    /** Rejoue les analyses faites hors ligne quand la connexion revient. */
    suspend fun syncPending(): Int = withContext(Dispatchers.IO) {
        if (!networkAvailable()) return@withContext 0
        var synced = 0
        for (item in pending.all()) {
            val ok = runCatching { api.analyse(AnalyseRequest(item.kind, item.content)) }.getOrNull()
            if (ok != null) {
                dao.upsert(ok.toEntity())
                pending.remove(item.id)
                synced++
            }
        }
        synced
    }

    suspend fun refreshHistory(): Outcome<Unit> = withContext(Dispatchers.IO) {
        runCatching { api.history(limit = 100) }.fold(
            { items ->
                dao.upsertAll(items.map {
                    AnalysisEntity(
                        id = it.id, kind = it.kind, preview = it.inputPreview, score = it.score,
                        level = it.level, summary = it.summary, signalsJson = "[]", sourcesJson = "[]",
                        aiUsed = it.aiUsed, createdAt = it.createdAt, syncedWithServer = true
                    )
                })
                Outcome.Success(Unit)
            },
            { toFailure(it) }
        )
    }

    suspend fun detail(id: String): Outcome<AnalysisEntity> = withContext(Dispatchers.IO) {
        dao.byId(id)?.let { cached ->
            if (cached.signalsJson != "[]" || cached.id.startsWith("local-")) return@withContext Outcome.Success(cached)
        }
        runCatching { api.analysis(id) }.fold(
            { it.toEntity().also { e -> dao.upsert(e) }.let(Outcome::Success) },
            { failure -> dao.byId(id)?.let { Outcome.Success(it, offline = true) } ?: toFailure(failure) }
        )
    }

    suspend fun stats(): Outcome<StatsResponse> = withContext(Dispatchers.IO) {
        runCatching { api.stats() }.fold({ Outcome.Success(it) }, { toFailure(it) })
    }

    suspend fun localStats(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        Triple(dao.count(), dao.countByLevel("dangerous"), dao.countByLevel("suspicious"))
    }

    suspend fun delete(id: String): Outcome<Unit> = withContext(Dispatchers.IO) {
        dao.delete(id)
        if (id.startsWith("local-")) return@withContext Outcome.Success(Unit)
        runCatching { api.deleteHistoryItem(id) }.fold({ Outcome.Success(Unit) }, { toFailure(it) })
    }

    suspend fun clearAll(): Outcome<Unit> = withContext(Dispatchers.IO) {
        dao.clear()
        runCatching { api.clearHistory() }.fold({ Outcome.Success(Unit) }, { toFailure(it) })
    }

    private fun AnalysisResponse.toEntity() = AnalysisEntity(
        id = id, kind = kind, preview = inputPreview, score = score, level = level, summary = summary,
        signalsJson = ApiFactory.json.encodeToString(ListSerializer(SignalDto.serializer()), signals),
        sourcesJson = ApiFactory.json.encodeToString(ListSerializer(SourceDto.serializer()), sources),
        aiUsed = aiUsed, createdAt = createdAt, syncedWithServer = true
    )
}
