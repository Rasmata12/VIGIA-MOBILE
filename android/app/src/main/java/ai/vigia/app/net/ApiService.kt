package ai.vigia.app.net

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {
    @GET("health")
    suspend fun health(): HealthResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): TokenResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)

    @GET("auth/me")
    suspend fun me(): UserResponse

    @POST("analyses")
    suspend fun analyse(@Body body: AnalyseRequest): AnalysisResponse

    @Multipart
    @POST("media/analyze")
    suspend fun analyzeMedia(
        @Part frames: List<okhttp3.MultipartBody.Part>,
        @Part("media_type") mediaType: okhttp3.RequestBody,
        @Part("filename") filename: okhttp3.RequestBody
    ): MediaAnalysisResponse

    @GET("analyses/{id}")
    suspend fun analysis(@Path("id") id: String): AnalysisResponse

    @GET("history")
    suspend fun history(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("level") level: String? = null
    ): List<HistoryItemDto>

    @DELETE("history/{id}")
    suspend fun deleteHistoryItem(@Path("id") id: String)

    @DELETE("history")
    suspend fun clearHistory()

    @GET("stats")
    suspend fun stats(): StatsResponse

    @GET("account/alerts")
    suspend fun alerts(@Query("unread_only") unreadOnly: Boolean = false): List<AlertDto>

    @POST("account/alerts/{id}/read")
    suspend fun markAlertRead(@Path("id") id: String)

    @PATCH("account/profile")
    suspend fun updateProfile(@Body body: ProfilePatch): UserResponse

    @GET("account/settings")
    suspend fun settings(): SettingsDto

    @PATCH("account/settings")
    suspend fun updateSettings(@Body body: SettingsPatch): SettingsDto

    @HTTP(method = "DELETE", path = "account", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountRequest)

    // ----------------------------------------------------------- modules avances

    @POST("verify")
    suspend fun verify(@Body body: VerifyRequest): VerifyResponse

    @GET("verify/{id}")
    suspend fun verifyDetail(@Path("id") id: String): VerifyResponse

    @POST("guard/status")
    suspend fun guardStatus(@Body body: GuardStatusRequest): GuardStatusResponse

    @POST("guard/events")
    suspend fun guardEvent(@Body body: GuardEventRequest): retrofit2.Response<VerifyResponse>

    @GET("moment")
    suspend fun moment(): MomentResponse

    @DELETE("moment")
    suspend fun clearMoment()

    @POST("before-pay")
    suspend fun beforePay(@Body body: BeforePayRequest): BeforePayResponse

    @GET("shield")
    suspend fun shield(): ShieldResponse

    @GET("privacy/summary")
    suspend fun privacySummary(): PrivacySummaryDto

    @POST("privacy/retention/apply")
    suspend fun applyRetention(): Map<String, kotlinx.serialization.json.JsonElement>

    @Streaming
    @GET("privacy/export")
    suspend fun exportData(): ResponseBody

    @DELETE("privacy/data")
    suspend fun deleteAllData()

    @POST("devices")
    suspend fun registerDevice(@Body body: DeviceRegisterRequest): DeviceDto

    @GET("devices")
    suspend fun devices(): List<DeviceDto>

    @DELETE("devices/{id}")
    suspend fun revokeDevice(@Path("id") id: String)

    @GET("devices/sessions")
    suspend fun sessions(): List<SessionDto>

    @GET("guard/status")
    suspend fun guardStatusRead(): GuardStatusResponse

    @GET("account/settings")
    suspend fun fullSettings(): FullSettingsDto

    @PATCH("account/settings")
    suspend fun updateFullSettings(@Body body: FullSettingsPatch): FullSettingsDto

    // ----------------------------------------------------------- offres d'emploi / formation

    @POST("job-offer")
    suspend fun jobOffer(@Body body: JobOfferRequest): JobOfferResponse

    // ----------------------------------------------------------- petites annonces

    @POST("listing")
    suspend fun listing(@Body body: ListingRequest): ListingResponse

    // ----------------------------------------------------------- espace communautaire

    @POST("community/reports")
    suspend fun reportToCommunity(@Body body: CommunityReportRequest): CommunityReportResponse

    @GET("community/check")
    suspend fun checkCommunity(@Query("target") target: String): CommunityCheckResponse

    @GET("community/trending")
    suspend fun communityTrending(@Query("limit") limit: Int = 10): CommunityTrendingResponse
}
