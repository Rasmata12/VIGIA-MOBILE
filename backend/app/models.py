from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base


def _uuid() -> str:
    return str(uuid.uuid4())


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


class User(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    full_name: Mapped[str] = mapped_column(String(120), default="")
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    last_login_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    analyses: Mapped[list[Analysis]] = relationship(back_populates="user", cascade="all, delete-orphan")
    sessions: Mapped[list[AuthSession]] = relationship(back_populates="user", cascade="all, delete-orphan")
    alerts: Mapped[list[Alert]] = relationship(back_populates="user", cascade="all, delete-orphan")
    settings: Mapped[UserSettings | None] = relationship(
        back_populates="user", cascade="all, delete-orphan", uselist=False
    )


class AuthSession(Base):
    """Refresh token persistant -> permet la deconnexion reelle et la revocation."""

    __tablename__ = "auth_sessions"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    token_hash: Mapped[str] = mapped_column(String(128), unique=True, index=True)
    device_label: Mapped[str] = mapped_column(String(120), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    revoked: Mapped[bool] = mapped_column(Boolean, default=False)

    user: Mapped[User] = relationship(back_populates="sessions")


class Analysis(Base):
    __tablename__ = "analyses"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    kind: Mapped[str] = mapped_column(String(16))  # url | text
    input_preview: Mapped[str] = mapped_column(String(300))
    input_sha256: Mapped[str] = mapped_column(String(64), index=True)
    score: Mapped[int] = mapped_column(Integer)
    level: Mapped[str] = mapped_column(String(16), index=True)  # safe | suspicious | dangerous
    summary: Mapped[str] = mapped_column(Text)
    signals_json: Mapped[str] = mapped_column(Text)
    sources_json: Mapped[str] = mapped_column(Text)
    ai_used: Mapped[bool] = mapped_column(Boolean, default=False)
    confidence: Mapped[int] = mapped_column(Integer, default=0)
    module: Mapped[str] = mapped_column(String(24), default="verify", index=True)  # verify|guard|qr|before_pay|share
    assessment_json: Mapped[str] = mapped_column(Text, default="{}")
    engine_version: Mapped[str] = mapped_column(String(16), default="1.0.0")
    duration_ms: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, index=True)

    user: Mapped[User] = relationship(back_populates="analyses")


class Alert(Base):
    __tablename__ = "alerts"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    analysis_id: Mapped[str | None] = mapped_column(
        ForeignKey("analyses.id", ondelete="CASCADE"), nullable=True
    )
    title: Mapped[str] = mapped_column(String(160))
    body: Mapped[str] = mapped_column(Text)
    level: Mapped[str] = mapped_column(String(16))
    source: Mapped[str] = mapped_column(String(24), default="verify")
    reason: Mapped[str] = mapped_column(Text, default="")
    recommended_action: Mapped[str] = mapped_column(Text, default="")
    read: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, index=True)

    user: Mapped[User] = relationship(back_populates="alerts")


class UserSettings(Base):
    __tablename__ = "user_settings"
    __table_args__ = (UniqueConstraint("user_id"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    notifications_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    ai_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    store_full_input: Mapped[bool] = mapped_column(Boolean, default=False)
    guard_enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    guard_store_content: Mapped[bool] = mapped_column(Boolean, default=False)
    auto_analyze_shared: Mapped[bool] = mapped_column(Boolean, default=True)
    vibration_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    retention_days: Mapped[int] = mapped_column(Integer, default=90)
    theme: Mapped[str] = mapped_column(String(12), default="system")
    language: Mapped[str] = mapped_column(String(8), default="fr")

    user: Mapped[User] = relationship(back_populates="settings")


class Device(Base):
    __tablename__ = "devices"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    label: Mapped[str] = mapped_column(String(120), default="")
    platform: Mapped[str] = mapped_column(String(32), default="android")
    app_version: Mapped[str] = mapped_column(String(32), default="")
    install_id: Mapped[str] = mapped_column(String(64), index=True, default="")
    revoked: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    last_seen_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class Event(Base):
    """Evenement reel observe (analyse volontaire, notification captee par Guard...).
    Sert au moteur de correlation VIGIA Moment. Duree de vie limitee."""

    __tablename__ = "events"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    analysis_id: Mapped[str | None] = mapped_column(
        ForeignKey("analyses.id", ondelete="CASCADE"), nullable=True
    )
    source: Mapped[str] = mapped_column(String(24), index=True)   # verify|guard|qr|share|before_pay
    type: Mapped[str] = mapped_column(String(24))                 # message|url|payment_request|notification
    package_name: Mapped[str] = mapped_column(String(120), default="")
    signals_json: Mapped[str] = mapped_column(Text, default="[]")
    dna_json: Mapped[str] = mapped_column(Text, default="[]")
    content_hash: Mapped[str] = mapped_column(String(64), default="", index=True)
    risk_score: Mapped[int] = mapped_column(Integer, default=0)
    risk_level: Mapped[str] = mapped_column(String(16), default="safe")
    related_ids_json: Mapped[str] = mapped_column(Text, default="[]")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, index=True)
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)


class RateLimitCounter(Base):
    __tablename__ = "rate_limits"

    id: Mapped[str] = mapped_column(String(64), primary_key=True)  # bucket key
    count: Mapped[int] = mapped_column(Integer, default=0)
    window_start: Mapped[float] = mapped_column(Float, default=0.0)


class CommunityReport(Base):
    """Signalement communautaire : un utilisateur signale une cible (domaine ou numero)
    comme suspecte ou frauduleuse. Le comptage se fait par UTILISATEUR DISTINCT (voir
    app/engine/community.py) pour qu'une seule personne ne puisse pas gonfler un score
    en signalant plusieurs fois la meme cible."""

    __tablename__ = "community_reports"
    __table_args__ = (UniqueConstraint("user_id", "target_type", "target_key", name="uq_report_per_user_target"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    target_type: Mapped[str] = mapped_column(String(16), index=True)   # domain | phone
    target_key: Mapped[str] = mapped_column(String(160), index=True)   # valeur normalisee
    category: Mapped[str] = mapped_column(String(24), default="autre")  # emploi|annonce|paiement|phishing|autre
    description: Mapped[str] = mapped_column(String(400), default="")
    analysis_id: Mapped[str | None] = mapped_column(ForeignKey("analyses.id", ondelete="SET NULL"), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, index=True)
