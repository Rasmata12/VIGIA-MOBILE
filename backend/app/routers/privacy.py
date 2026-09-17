from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Response
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.config import get_settings
from app.db import get_db
from app.models import Alert, Analysis, AuthSession, CommunityReport, Device, Event, User, UserSettings
from app.schemas import PrivacySummaryOut
from app.security import current_user

router = APIRouter(prefix="/privacy", tags=["privacy"])
settings = get_settings()


def _row(db: Session, user: User) -> UserSettings:
    if user.settings is None:
        row = UserSettings(user_id=user.id)
        db.add(row)
        db.commit()
        db.refresh(row)
        return row
    return user.settings


@router.get("/summary", response_model=PrivacySummaryOut)
def summary(user: User = Depends(current_user), db: Session = Depends(get_db)) -> PrivacySummaryOut:
    row = _row(db, user)
    count = lambda model: int(db.query(func.count(model.id)).filter(model.user_id == user.id).scalar() or 0)  # noqa: E731
    external = []
    if settings.has_safebrowsing:
        external.append("Google Safe Browsing")
    if settings.has_virustotal:
        external.append("VirusTotal")

    return PrivacySummaryOut(
        account_created_at=user.created_at,
        analyses_stored=count(Analysis),
        events_stored=count(Event),
        alerts_stored=count(Alert),
        community_reports_stored=count(CommunityReport),
        sessions_active=int(db.query(func.count(AuthSession.id)).filter(
            AuthSession.user_id == user.id, AuthSession.revoked.is_(False)
        ).scalar() or 0),
        devices=count(Device),
        stores_full_content=row.store_full_input,
        guard_stores_content=row.guard_store_content,
        retention_days=row.retention_days,
        # Avec le fournisseur "ollama" (par defaut), le modele tourne sur le serveur VIGIA
        # lui-meme : le contenu ne part jamais vers un tiers, donc ai_sends_content=False
        # meme quand l'IA est active. Seul "openai_compatible" envoie reellement le contenu
        # a un service externe.
        ai_sends_content=settings.has_ai and row.ai_enabled and settings.ai_provider.strip().lower() == "openai_compatible",
        ai_provider=settings.ai_label if settings.has_ai and row.ai_enabled else None,
        external_reputation_enabled=external,
        what_is_stored=[
            "Un extrait court du contenu analyse (120 caracteres) sauf si tu actives la conservation complete.",
            "Une empreinte SHA-256 du contenu, qui ne permet pas de reconstituer le texte.",
            "Les signaux detectes, le score, le niveau de risque et l'explication.",
            "La date, le module d'origine et la duree de l'analyse.",
            "Les jetons de session (haches) et les appareils enregistres.",
            "Si tu as signale une cible dans l'espace communautaire : le lien/domaine ou le numero "
            "signale, la categorie, et un lien vers ton compte (jamais visible par les autres utilisateurs, "
            "qui ne voient qu'un nombre total de signalants).",
        ],
        what_is_never_stored=[
            "Ton mot de passe en clair (uniquement un hachage bcrypt).",
            "Les codes PIN, OTP, cryptogrammes ou numeros de carte : le module Before Pay les refuse.",
            "Le contenu integral de tes notifications si Guard est en mode minimisation (defaut).",
            "Aucune donnee n'est vendue ni transmise a un tiers hors des services de reputation que tu actives.",
        ],
    )


@router.get("/export")
def export_data(user: User = Depends(current_user), db: Session = Depends(get_db)) -> Response:
    """Export complet et reel des donnees du compte (RGPD-like), en JSON."""
    analyses = db.query(Analysis).filter(Analysis.user_id == user.id).all()
    alerts = db.query(Alert).filter(Alert.user_id == user.id).all()
    events = db.query(Event).filter(Event.user_id == user.id).all()
    devices = db.query(Device).filter(Device.user_id == user.id).all()
    community_reports = db.query(CommunityReport).filter(CommunityReport.user_id == user.id).all()
    row = _row(db, user)

    payload = {
        "exported_at": datetime.now(timezone.utc).isoformat(),
        "account": {
            "id": user.id, "email": user.email, "full_name": user.full_name,
            "created_at": user.created_at.isoformat(),
            "last_login_at": user.last_login_at.isoformat() if user.last_login_at else None,
        },
        "settings": {
            "notifications_enabled": row.notifications_enabled, "ai_enabled": row.ai_enabled,
            "store_full_input": row.store_full_input, "guard_enabled": row.guard_enabled,
            "guard_store_content": row.guard_store_content, "retention_days": row.retention_days,
            "theme": row.theme, "language": row.language,
        },
        "analyses": [{
            "id": a.id, "kind": a.kind, "module": a.module, "preview": a.input_preview,
            "sha256": a.input_sha256, "score": a.score, "level": a.level, "confidence": a.confidence,
            "summary": a.summary, "signals": json.loads(a.signals_json or "[]"),
            "sources": json.loads(a.sources_json or "[]"), "ai_used": a.ai_used,
            "created_at": a.created_at.isoformat(),
        } for a in analyses],
        "alerts": [{
            "id": x.id, "title": x.title, "body": x.body, "level": x.level, "source": x.source,
            "read": x.read, "created_at": x.created_at.isoformat(),
        } for x in alerts],
        "events": [{
            "id": e.id, "source": e.source, "type": e.type, "package_name": e.package_name,
            "risk_level": e.risk_level, "risk_score": e.risk_score,
            "scam_dna": json.loads(e.dna_json or "[]"), "created_at": e.created_at.isoformat(),
        } for e in events],
        "devices": [{
            "id": d.id, "label": d.label, "platform": d.platform, "install_id": d.install_id,
            "revoked": d.revoked, "last_seen_at": d.last_seen_at.isoformat(),
        } for d in devices],
        "community_reports": [{
            "id": r.id, "target_type": r.target_type, "target_key": r.target_key,
            "category": r.category, "description": r.description, "created_at": r.created_at.isoformat(),
        } for r in community_reports],
    }
    body = json.dumps(payload, ensure_ascii=False, indent=2)
    return Response(
        content=body,
        media_type="application/json",
        headers={"Content-Disposition": 'attachment; filename="vigia-export.json"'},
    )


@router.delete("/data", status_code=204, response_model=None)
def delete_all_data(user: User = Depends(current_user), db: Session = Depends(get_db)) -> None:
    """Supprime toutes les donnees d'analyse SANS supprimer le compte."""
    for model in (Alert, Event, Analysis, CommunityReport):
        db.query(model).filter(model.user_id == user.id).delete()
    db.commit()


@router.post("/retention/apply")
def apply_retention(user: User = Depends(current_user), db: Session = Depends(get_db)) -> dict:
    """Applique immediatement la politique de conservation choisie par l'utilisateur."""
    row = _row(db, user)
    cutoff = datetime.now(timezone.utc) - timedelta(days=row.retention_days)
    deleted = 0
    for model in (Alert, Event, Analysis):
        deleted += db.query(model).filter(model.user_id == user.id, model.created_at < cutoff).delete(
            synchronize_session=False
        ) or 0
    db.commit()
    return {"retention_days": row.retention_days, "deleted_records": int(deleted)}
