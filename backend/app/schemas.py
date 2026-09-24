from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field


class RegisterIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=10, max_length=128)
    full_name: str = Field(default="", max_length=120)
    device_label: str = Field(default="", max_length=120)


class LoginIn(BaseModel):
    email: EmailStr
    password: str
    device_label: str = Field(default="", max_length=120)


class RefreshIn(BaseModel):
    refresh_token: str


class TokenOut(BaseModel):
    access_token: str
    refresh_token: str
    expires_in: int
    token_type: str = "Bearer"


class UserOut(BaseModel):
    id: str
    email: EmailStr
    full_name: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class AnalyseIn(BaseModel):
    kind: str = Field(pattern="^(url|text)$")
    content: str = Field(min_length=1, max_length=20000)
    online: bool = True
    use_ai: bool = True
    hf_token: str | None = Field(default=None, max_length=500)


class SignalOut(BaseModel):
    code: str
    label: str
    weight: int
    evidence: str = ""
    category: str = "general"


class AnalysisOut(BaseModel):
    id: str
    kind: str
    input_preview: str
    score: int
    level: str
    summary: str
    signals: list[SignalOut]
    sources: list[dict]
    technical: dict = {}
    ai_used: bool
    duration_ms: int
    created_at: datetime


class HistoryItem(BaseModel):
    id: str
    kind: str
    input_preview: str
    score: int
    level: str
    summary: str
    ai_used: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class StatsOut(BaseModel):
    total_analyses: int
    safe: int
    suspicious: int
    dangerous: int
    url_analyses: int
    text_analyses: int
    last_7_days: list[dict]
    last_analysis_at: datetime | None
    protection_score: int | None
    unread_alerts: int


class AlertOut(BaseModel):
    id: str
    analysis_id: str | None
    title: str
    body: str
    level: str
    read: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class SettingsIn(BaseModel):
    notifications_enabled: bool | None = None
    ai_enabled: bool | None = None
    store_full_input: bool | None = None
    language: str | None = Field(default=None, max_length=8)


class SettingsOut(BaseModel):
    notifications_enabled: bool
    ai_enabled: bool
    store_full_input: bool
    language: str

    model_config = ConfigDict(from_attributes=True)


class DeleteAccountIn(BaseModel):
    password: str


class HealthOut(BaseModel):
    status: str
    engine_version: str
    database: str
    ai_enabled: bool
    ai_detail: str = ""
    safebrowsing_enabled: bool
    virustotal_enabled: bool
    network_probes: bool


# --------------------------------------------------------------- MODULES AVANCES

class VerifyIn(BaseModel):
    """Entree unique du module Verify. `kind` peut etre deduit automatiquement."""
    content: str = Field(min_length=1, max_length=20000)
    kind: str | None = Field(default=None, pattern="^(url|text)$")
    source: str = Field(default="verify", pattern="^(verify|qr|share|guard|before_pay)$")
    online: bool = True
    use_ai: bool = True
    hf_token: str | None = Field(default=None, max_length=500)


class ScamDnaOut(BaseModel):
    category: str
    label: str
    evidence: list[str]
    strength: int


class RiskAssessmentOut(BaseModel):
    score: int
    level: str
    confidence: int
    summary: str
    signals: list[SignalOut]
    evidence: list[str]
    scam_dna: list[ScamDnaOut]
    layers: dict
    sources: list[dict]
    technical: dict = {}
    recommendation: list[str]
    ai_used: bool
    timestamp: str


class VerifyOut(BaseModel):
    analysis_id: str
    kind: str
    module: str
    input_preview: str
    duration_ms: int
    alert_created: bool
    assessment: RiskAssessmentOut
    created_at: datetime


class GuardEventIn(BaseModel):
    """Evenement capte par le NotificationListenerService Android."""
    package_name: str = Field(max_length=120)
    title: str = Field(default="", max_length=300)
    text: str = Field(max_length=6000)
    posted_at: datetime | None = None
    analyse: bool = True
    hf_token: str | None = Field(default=None, max_length=500)


class GuardStatusIn(BaseModel):
    listener_enabled: bool
    notifications_permission: bool = False
    app_version: str = Field(default="", max_length=32)
    install_id: str = Field(default="", max_length=64)


class GuardStatusOut(BaseModel):
    guard_enabled_server_side: bool
    listener_enabled_device: bool | None
    events_last_24h: int
    alerts_last_24h: int
    store_content: bool
    retention_days: int


class EventOut(BaseModel):
    id: str
    analysis_id: str | None
    source: str
    type: str
    package_name: str
    risk_score: int
    risk_level: str
    scam_dna: list[ScamDnaOut] = []
    related_ids: list[str] = []
    created_at: datetime


class MomentOut(BaseModel):
    window_hours: int
    events: list[EventOut]
    correlations: list[dict]
    elevated_risk: bool


class BeforePayIn(BaseModel):
    message: str = Field(default="", max_length=8000)
    url: str = Field(default="", max_length=2000)
    beneficiary: str = Field(default="", max_length=160)
    amount: str = Field(default="", max_length=40)
    context: str = Field(default="", max_length=1000)
    online: bool = True
    use_ai: bool = True


class BeforePayOut(BaseModel):
    analysis_id: str | None
    decision: str            # stop | verifier | prudence
    headline: str
    checklist: list[str]
    assessment: RiskAssessmentOut | None
    beneficiary_notes: list[str]


# --------------------------------------------------------------- ESPACE COMMUNAUTAIRE

class CommunityReportIn(BaseModel):
    """Signalement d'une cible (lien/domaine ou numero de telephone) par un utilisateur."""
    target: str = Field(min_length=3, max_length=300)
    category: str = Field(default="autre", pattern="^(emploi|annonce|paiement|phishing|autre)$")
    description: str = Field(default="", max_length=400)
    analysis_id: str | None = None


class CommunityReportOut(BaseModel):
    id: str
    target_type: str
    target_key: str
    category: str
    already_reported_by_me: bool
    community_reporters: int
    created_at: datetime


class CommunityCheckOut(BaseModel):
    target_type: str
    target_key: str
    reporters: int
    by_category: dict
    risk_from_reports: str   # aucun | a_surveiller | suspect | tres_signale


class CommunityTrendingItem(BaseModel):
    target_type: str
    target_key: str
    reporters: int
    last_reported_at: datetime
    top_category: str


class CommunityTrendingOut(BaseModel):
    window_days: int
    items: list[CommunityTrendingItem]


class ListingIn(BaseModel):
    """Entree du module Annonces (immobilier, vehicules, objets, services)."""
    content: str = Field(default="", max_length=20000)
    category: str = Field(default="autre", pattern="^(immobilier|vehicule|objet|service|autre)$")
    price_asked: str = Field(default="", max_length=120)
    seller_contact: str = Field(default="", max_length=200)
    deposit_requested: str = Field(default="", max_length=200)
    can_visit_in_person: bool | None = None
    online: bool = True
    use_ai: bool = True


class ListingOut(BaseModel):
    analysis_id: str | None
    decision: str            # stop | verifier | prudence
    headline: str
    checklist: list[str]
    red_flags: list[str]
    assessment: RiskAssessmentOut | None


class JobOfferIn(BaseModel):
    """Entree du module Offres d'emploi & de formation."""
    content: str = Field(default="", max_length=20000)
    company_name: str = Field(default="", max_length=200)
    contact_email: str = Field(default="", max_length=200)
    salary_promised: str = Field(default="", max_length=200)
    fee_requested: str = Field(default="", max_length=200)
    online: bool = True
    use_ai: bool = True


class JobOfferOut(BaseModel):
    analysis_id: str | None
    decision: str            # stop | verifier | prudence
    headline: str
    checklist: list[str]
    red_flags: list[str]
    assessment: RiskAssessmentOut | None


class ShieldOut(BaseModel):
    has_enough_data: bool
    message: str
    personal: dict
    community: dict
    top_categories: list[dict]
    generated_at: datetime


class DeviceOut(BaseModel):
    id: str
    label: str
    platform: str
    app_version: str
    install_id: str
    revoked: bool
    created_at: datetime
    last_seen_at: datetime

    model_config = ConfigDict(from_attributes=True)


class DeviceRegisterIn(BaseModel):
    label: str = Field(default="", max_length=120)
    platform: str = Field(default="android", max_length=32)
    app_version: str = Field(default="", max_length=32)
    install_id: str = Field(max_length=64)


class PrivacySummaryOut(BaseModel):
    account_created_at: datetime
    analyses_stored: int
    events_stored: int
    alerts_stored: int
    community_reports_stored: int
    sessions_active: int
    devices: int
    stores_full_content: bool
    guard_stores_content: bool
    retention_days: int
    ai_sends_content: bool
    ai_provider: str | None
    external_reputation_enabled: list[str]
    what_is_stored: list[str]
    what_is_never_stored: list[str]


class SettingsFullOut(BaseModel):
    notifications_enabled: bool
    ai_enabled: bool
    store_full_input: bool
    guard_enabled: bool
    guard_store_content: bool
    auto_analyze_shared: bool
    vibration_enabled: bool
    retention_days: int
    theme: str
    language: str

    model_config = ConfigDict(from_attributes=True)


class SettingsFullPatch(BaseModel):
    notifications_enabled: bool | None = None
    ai_enabled: bool | None = None
    store_full_input: bool | None = None
    guard_enabled: bool | None = None
    guard_store_content: bool | None = None
    auto_analyze_shared: bool | None = None
    vibration_enabled: bool | None = None
    retention_days: int | None = Field(default=None, ge=1, le=365)
    theme: str | None = Field(default=None, pattern="^(system|light|dark)$")
    language: str | None = Field(default=None, pattern="^(fr|en)$")


class MediaAnalysisOut(BaseModel):
    score: int
    level: str
    summary: str
    indicators: list[str] = []
    recommended_actions: list[str] = []
    observed_text: str = ""
    detected_urls: list[str] = []
    detected_phones: list[str] = []
    vision_score: int = 0
    corroborating_score: int = 0
    model: str
    frames_analyzed: int
    media_type: str
