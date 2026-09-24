package ai.vigia.app.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("device_label") val deviceLabel: String = ""
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    @SerialName("device_label") val deviceLabel: String = ""
)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AnalyseRequest(
    val kind: String,
    val content: String,
    val online: Boolean = true,
    @SerialName("use_ai") val useAi: Boolean = true
)

@Serializable
data class SignalDto(
    val code: String,
    val label: String,
    val weight: Int,
    val evidence: String = "",
    val category: String = "general"
)

@Serializable
data class SourceDto(val name: String, val status: String, val detail: String = "")

@Serializable
data class AnalysisResponse(
    val id: String,
    val kind: String,
    @SerialName("input_preview") val inputPreview: String,
    val score: Int,
    val level: String,
    val summary: String,
    val signals: List<SignalDto> = emptyList(),
    val sources: List<SourceDto> = emptyList(),
    val technical: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    @SerialName("ai_used") val aiUsed: Boolean = false,
    @SerialName("duration_ms") val durationMs: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class HistoryItemDto(
    val id: String,
    val kind: String,
    @SerialName("input_preview") val inputPreview: String,
    val score: Int,
    val level: String,
    val summary: String,
    @SerialName("ai_used") val aiUsed: Boolean = false,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class DayBucketDto(
    val date: String,
    val total: Int,
    val dangerous: Int,
    val suspicious: Int,
    val safe: Int
)

@Serializable
data class StatsResponse(
    @SerialName("total_analyses") val totalAnalyses: Int,
    val safe: Int,
    val suspicious: Int,
    val dangerous: Int,
    @SerialName("url_analyses") val urlAnalyses: Int,
    @SerialName("text_analyses") val textAnalyses: Int,
    @SerialName("last_7_days") val last7Days: List<DayBucketDto> = emptyList(),
    @SerialName("last_analysis_at") val lastAnalysisAt: String? = null,
    @SerialName("protection_score") val protectionScore: Int? = null,
    @SerialName("unread_alerts") val unreadAlerts: Int = 0
)

@Serializable
data class AlertDto(
    val id: String,
    @SerialName("analysis_id") val analysisId: String? = null,
    val title: String,
    val body: String,
    val level: String,
    val read: Boolean,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SettingsDto(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerialName("ai_enabled") val aiEnabled: Boolean = true,
    @SerialName("store_full_input") val storeFullInput: Boolean = false,
    val language: String = "fr"
)

@Serializable
data class SettingsPatch(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = null,
    @SerialName("ai_enabled") val aiEnabled: Boolean? = null,
    @SerialName("store_full_input") val storeFullInput: Boolean? = null
)

@Serializable
data class DeleteAccountRequest(val password: String)

@Serializable
data class HealthResponse(
    val status: String,
    @SerialName("engine_version") val engineVersion: String,
    val database: String,
    @SerialName("ai_enabled") val aiEnabled: Boolean,
    @SerialName("safebrowsing_enabled") val safebrowsingEnabled: Boolean,
    @SerialName("virustotal_enabled") val virustotalEnabled: Boolean
)

@Serializable
data class ApiError(val detail: String = "Erreur inconnue")

// ---------------------------------------------------------------- MODULES AVANCES

@Serializable
data class VerifyRequest(
    val content: String,
    val kind: String? = null,
    val source: String = "verify",
    val online: Boolean = true,
    @SerialName("use_ai") val useAi: Boolean = true
)

@Serializable
data class ScamDnaDto(val category: String, val label: String, val evidence: List<String> = emptyList(), val strength: Int)

@Serializable
data class RiskAssessmentDto(
    val score: Int,
    val level: String,
    val confidence: Int,
    val summary: String,
    val signals: List<SignalDto> = emptyList(),
    val evidence: List<String> = emptyList(),
    @SerialName("scam_dna") val scamDna: List<ScamDnaDto> = emptyList(),
    val sources: List<SourceDto> = emptyList(),
    val technical: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    val recommendation: List<String> = emptyList(),
    @SerialName("ai_used") val aiUsed: Boolean = false,
    val timestamp: String = ""
)

@Serializable
data class VerifyResponse(
    @SerialName("analysis_id") val analysisId: String,
    val kind: String,
    val module: String,
    @SerialName("input_preview") val inputPreview: String,
    @SerialName("duration_ms") val durationMs: Int = 0,
    @SerialName("alert_created") val alertCreated: Boolean = false,
    val assessment: RiskAssessmentDto,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class GuardEventRequest(
    @SerialName("package_name") val packageName: String,
    val title: String = "",
    val text: String
)

@Serializable
data class GuardStatusRequest(
    @SerialName("listener_enabled") val listenerEnabled: Boolean,
    @SerialName("notifications_permission") val notificationsPermission: Boolean = false,
    @SerialName("app_version") val appVersion: String = "",
    @SerialName("install_id") val installId: String = ""
)

@Serializable
data class GuardStatusResponse(
    @SerialName("guard_enabled_server_side") val guardEnabledServerSide: Boolean,
    @SerialName("listener_enabled_device") val listenerEnabledDevice: Boolean? = null,
    @SerialName("events_last_24h") val eventsLast24h: Int = 0,
    @SerialName("alerts_last_24h") val alertsLast24h: Int = 0,
    @SerialName("store_content") val storeContent: Boolean = false,
    @SerialName("retention_days") val retentionDays: Int = 90
)

@Serializable
data class EventDto(
    val id: String,
    @SerialName("analysis_id") val analysisId: String? = null,
    val source: String,
    val type: String,
    @SerialName("package_name") val packageName: String = "",
    @SerialName("risk_score") val riskScore: Int = 0,
    @SerialName("risk_level") val riskLevel: String = "safe",
    @SerialName("scam_dna") val scamDna: List<ScamDnaDto> = emptyList(),
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class MomentResponse(
    @SerialName("window_hours") val windowHours: Int,
    val events: List<EventDto> = emptyList(),
    val correlations: List<Map<String, kotlinx.serialization.json.JsonElement>> = emptyList(),
    @SerialName("elevated_risk") val elevatedRisk: Boolean = false
)

@Serializable
data class BeforePayRequest(
    val message: String = "",
    val url: String = "",
    val beneficiary: String = "",
    val amount: String = "",
    val context: String = "",
    val online: Boolean = true,
    @SerialName("use_ai") val useAi: Boolean = true
)

@Serializable
data class BeforePayResponse(
    @SerialName("analysis_id") val analysisId: String? = null,
    val decision: String,
    val headline: String,
    val checklist: List<String> = emptyList(),
    val assessment: RiskAssessmentDto? = null,
    @SerialName("beneficiary_notes") val beneficiaryNotes: List<String> = emptyList()
)

@Serializable
data class ShieldResponse(
    @SerialName("has_enough_data") val hasEnoughData: Boolean,
    val message: String,
    val personal: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    val community: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
    @SerialName("top_categories") val topCategories: List<Map<String, kotlinx.serialization.json.JsonElement>> = emptyList()
)

@Serializable
data class DeviceDto(
    val id: String,
    val label: String = "",
    val platform: String = "android",
    @SerialName("app_version") val appVersion: String = "",
    @SerialName("install_id") val installId: String = "",
    val revoked: Boolean = false,
    @SerialName("last_seen_at") val lastSeenAt: String
)

@Serializable
data class SessionDto(
    val id: String,
    @SerialName("device_label") val deviceLabel: String = "",
    val revoked: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class DeviceRegisterRequest(
    val label: String = "",
    val platform: String = "android",
    @SerialName("app_version") val appVersion: String = "",
    @SerialName("install_id") val installId: String
)

@Serializable
data class PrivacySummaryDto(
    @SerialName("analyses_stored") val analysesStored: Int = 0,
    @SerialName("events_stored") val eventsStored: Int = 0,
    @SerialName("alerts_stored") val alertsStored: Int = 0,
    @SerialName("sessions_active") val sessionsActive: Int = 0,
    val devices: Int = 0,
    @SerialName("stores_full_content") val storesFullContent: Boolean = false,
    @SerialName("guard_stores_content") val guardStoresContent: Boolean = false,
    @SerialName("retention_days") val retentionDays: Int = 90,
    @SerialName("ai_sends_content") val aiSendsContent: Boolean = false,
    @SerialName("ai_provider") val aiProvider: String? = null,
    @SerialName("external_reputation_enabled") val externalReputationEnabled: List<String> = emptyList(),
    @SerialName("what_is_stored") val whatIsStored: List<String> = emptyList(),
    @SerialName("what_is_never_stored") val whatIsNeverStored: List<String> = emptyList()
)

@Serializable
data class FullSettingsDto(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerialName("ai_enabled") val aiEnabled: Boolean = true,
    @SerialName("store_full_input") val storeFullInput: Boolean = false,
    @SerialName("guard_enabled") val guardEnabled: Boolean = false,
    @SerialName("guard_store_content") val guardStoreContent: Boolean = false,
    @SerialName("auto_analyze_shared") val autoAnalyzeShared: Boolean = true,
    @SerialName("vibration_enabled") val vibrationEnabled: Boolean = true,
    @SerialName("retention_days") val retentionDays: Int = 90,
    val theme: String = "system",
    val language: String = "fr"
)

// ---------------------------------------------------------------- OFFRES D'EMPLOI / FORMATION

@Serializable
data class JobOfferRequest(
    val content: String = "",
    @SerialName("company_name") val companyName: String = "",
    @SerialName("contact_email") val contactEmail: String = "",
    @SerialName("salary_promised") val salaryPromised: String = "",
    @SerialName("fee_requested") val feeRequested: String = "",
    val online: Boolean = true,
    @SerialName("use_ai") val useAi: Boolean = true
)

@Serializable
data class JobOfferResponse(
    @SerialName("analysis_id") val analysisId: String? = null,
    val decision: String,
    val headline: String,
    val checklist: List<String> = emptyList(),
    @SerialName("red_flags") val redFlags: List<String> = emptyList(),
    val assessment: RiskAssessmentDto? = null
)

// ---------------------------------------------------------------- PETITES ANNONCES

@Serializable
data class ListingRequest(
    val content: String = "",
    val category: String = "autre",
    @SerialName("price_asked") val priceAsked: String = "",
    @SerialName("seller_contact") val sellerContact: String = "",
    @SerialName("deposit_requested") val depositRequested: String = "",
    @SerialName("can_visit_in_person") val canVisitInPerson: Boolean? = null,
    val online: Boolean = true,
    @SerialName("use_ai") val useAi: Boolean = true
)

@Serializable
data class ListingResponse(
    @SerialName("analysis_id") val analysisId: String? = null,
    val decision: String,
    val headline: String,
    val checklist: List<String> = emptyList(),
    @SerialName("red_flags") val redFlags: List<String> = emptyList(),
    val assessment: RiskAssessmentDto? = null
)

// ---------------------------------------------------------------- ESPACE COMMUNAUTAIRE

@Serializable
data class CommunityReportRequest(
    val target: String,
    val category: String = "autre",
    val description: String = "",
    @SerialName("analysis_id") val analysisId: String? = null
)

@Serializable
data class CommunityReportResponse(
    val id: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_key") val targetKey: String,
    val category: String,
    @SerialName("already_reported_by_me") val alreadyReportedByMe: Boolean = false,
    @SerialName("community_reporters") val communityReporters: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CommunityCheckResponse(
    @SerialName("target_type") val targetType: String,
    @SerialName("target_key") val targetKey: String,
    val reporters: Int = 0,
    @SerialName("by_category") val byCategory: Map<String, Int> = emptyMap(),
    @SerialName("risk_from_reports") val riskFromReports: String = "aucun"
)

@Serializable
data class CommunityTrendingItem(
    @SerialName("target_type") val targetType: String,
    @SerialName("target_key") val targetKey: String,
    val reporters: Int,
    @SerialName("last_reported_at") val lastReportedAt: String,
    @SerialName("top_category") val topCategory: String
)

@Serializable
data class CommunityTrendingResponse(
    @SerialName("window_days") val windowDays: Int,
    val items: List<CommunityTrendingItem> = emptyList()
)

@Serializable
data class FullSettingsPatch(
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean? = null,
    @SerialName("ai_enabled") val aiEnabled: Boolean? = null,
    @SerialName("store_full_input") val storeFullInput: Boolean? = null,
    @SerialName("guard_enabled") val guardEnabled: Boolean? = null,
    @SerialName("guard_store_content") val guardStoreContent: Boolean? = null,
    @SerialName("auto_analyze_shared") val autoAnalyzeShared: Boolean? = null,
    @SerialName("vibration_enabled") val vibrationEnabled: Boolean? = null,
    @SerialName("retention_days") val retentionDays: Int? = null,
    val theme: String? = null,
    val language: String? = null
)


@Serializable
data class MediaAnalysisResponse(
    val score: Int,
    val level: String,
    val summary: String,
    val indicators: List<String> = emptyList(),
    @SerialName("recommended_actions") val recommendedActions: List<String> = emptyList(),
    @SerialName("observed_text") val observedText: String = "",
    @SerialName("detected_urls") val detectedUrls: List<String> = emptyList(),
    @SerialName("detected_phones") val detectedPhones: List<String> = emptyList(),
    @SerialName("vision_score") val visionScore: Int = 0,
    @SerialName("corroborating_score") val corroboratingScore: Int = 0,
    val model: String = "",
    @SerialName("frames_analyzed") val framesAnalyzed: Int = 0,
    @SerialName("media_type") val mediaType: String = ""
)
