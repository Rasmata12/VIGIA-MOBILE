from __future__ import annotations

from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import Alert, Device, Event, User, UserSettings, utcnow
from app.schemas import GuardEventIn, GuardStatusIn, GuardStatusOut, VerifyOut
from app.security import current_user, rate_limit
from app.services.pipeline import run_pipeline

router = APIRouter(prefix="/guard", tags=["guard"])

# Applications dont les notifications peuvent contenir des messages entrants.
MESSAGING_PACKAGES = {
    "com.whatsapp", "com.whatsapp.w4b", "com.google.android.apps.messaging",
    "com.samsung.android.messaging", "org.telegram.messenger", "com.facebook.orca",
    "com.google.android.gm", "com.microsoft.office.outlook", "com.viber.voip",
    "com.instagram.android", "com.facebook.katana", "com.imo.android.imoim",
}


def _settings(db: Session, user: User) -> UserSettings:
    if user.settings is None:
        row = UserSettings(user_id=user.id)
        db.add(row)
        db.commit()
        db.refresh(row)
        return row
    return user.settings


@router.get("/status", response_model=GuardStatusOut)
def get_status(
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> GuardStatusOut:
    """Retourne l'etat actuel du service Guard pour l'utilisateur."""
    row = _settings(db, user)
    since = datetime.now(timezone.utc) - timedelta(hours=24)
    events = db.query(func.count(Event.id)).filter(
        Event.user_id == user.id, Event.source == "guard", Event.created_at >= since
    ).scalar() or 0
    alerts = db.query(func.count(Alert.id)).filter(
        Alert.user_id == user.id, Alert.source == "guard", Alert.created_at >= since
    ).scalar() or 0
    return GuardStatusOut(
        guard_enabled_server_side=row.guard_enabled,
        # Un GET serveur ne peut pas connaître l'état réel du NotificationListener Android.
        # L'appareil doit le déclarer via POST /guard/status.
        listener_enabled_device=None,
        events_last_24h=int(events),
        alerts_last_24h=int(alerts),
        store_content=row.guard_store_content,
        retention_days=row.retention_days,
    )


@router.post("/status", response_model=GuardStatusOut)
def report_status(
    payload: GuardStatusIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> GuardStatusOut:
    """L'application declare l'etat REEL du service Android. Le serveur ne devine jamais
    que Guard est actif : il se contente d'enregistrer ce que l'appareil rapporte."""
    row = _settings(db, user)
    if payload.install_id:
        device = db.query(Device).filter(
            Device.user_id == user.id, Device.install_id == payload.install_id
        ).first()
        if device:
            device.last_seen_at = utcnow()
            device.app_version = payload.app_version or device.app_version
            db.commit()

    since = datetime.now(timezone.utc) - timedelta(hours=24)
    events = db.query(func.count(Event.id)).filter(
        Event.user_id == user.id, Event.source == "guard", Event.created_at >= since
    ).scalar() or 0
    alerts = db.query(func.count(Alert.id)).filter(
        Alert.user_id == user.id, Alert.source == "guard", Alert.created_at >= since
    ).scalar() or 0

    return GuardStatusOut(
        guard_enabled_server_side=row.guard_enabled,
        listener_enabled_device=payload.listener_enabled,
        events_last_24h=int(events),
        alerts_last_24h=int(alerts),
        store_content=row.guard_store_content,
        retention_days=row.retention_days,
    )


@router.post("/events", response_model=VerifyOut | dict, status_code=201)
async def ingest_event(
    payload: GuardEventIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
):
    """Recoit une notification reellement captee sur l'appareil et l'analyse.
    Refuse l'ingestion si l'utilisateur n'a pas active Guard."""
    row = _settings(db, user)
    if not row.guard_enabled:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN,
            "VIGIA Guard est desactive pour ce compte. Active-le dans les parametres avant d'envoyer des evenements.",
        )
    rate_limit(db, f"guard:{user.id}", limit=400, window_seconds=3600)

    content = " ".join(part for part in (payload.title, payload.text) if part).strip()
    if len(content) < 12:
        return {"ignored": True, "reason": "Notification sans contenu exploitable."}
    if payload.package_name not in MESSAGING_PACKAGES:
        return {"ignored": True, "reason": "Application non couverte par Guard.", "package": payload.package_name}

    record, assessment, alert_created = await run_pipeline(
        db, user, "text", content, module="guard",
        online=True, use_ai=row.ai_enabled, package_name=payload.package_name, hf_token=payload.hf_token,
    )
    return VerifyOut(
        analysis_id=record.id, kind=record.kind, module=record.module,
        input_preview=record.input_preview, duration_ms=record.duration_ms,
        alert_created=alert_created, assessment=assessment.dict(), created_at=record.created_at,
    )
