from __future__ import annotations

import json

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.engine.url_engine import normalize_url
from app.models import Analysis, User
from app.schemas import VerifyIn, VerifyOut
from app.security import current_user, rate_limit
from app.services.pipeline import run_pipeline

router = APIRouter(prefix="/verify", tags=["verify"])


def detect_kind(content: str) -> str:
    candidate = content.strip()
    if " " in candidate or "\n" in candidate:
        return "text"
    try:
        normalize_url(candidate)
        return "url"
    except ValueError:
        return "text"


@router.post("", response_model=VerifyOut, status_code=201)
async def verify(
    payload: VerifyIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> VerifyOut:
    rate_limit(db, f"verify:{user.id}", limit=120, window_seconds=3600)
    kind = payload.kind or detect_kind(payload.content)
    record, assessment, alert_created = await run_pipeline(
        db, user, kind, payload.content, module=payload.source,
        online=payload.online, use_ai=payload.use_ai,
    )
    return VerifyOut(
        analysis_id=record.id,
        kind=record.kind,
        module=record.module,
        input_preview=record.input_preview,
        duration_ms=record.duration_ms,
        alert_created=alert_created,
        assessment=assessment.dict(),
        created_at=record.created_at,
    )


@router.get("/{analysis_id}", response_model=VerifyOut)
def verify_detail(analysis_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> VerifyOut:
    record = db.get(Analysis, analysis_id)
    if record is None or record.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Analyse introuvable.")
    assessment = json.loads(record.assessment_json or "{}")
    if not assessment:
        raise HTTPException(status.HTTP_409_CONFLICT, "Cette analyse est anterieure au moteur de risque actuel.")
    return VerifyOut(
        analysis_id=record.id, kind=record.kind, module=record.module,
        input_preview=record.input_preview, duration_ms=record.duration_ms,
        alert_created=False, assessment=assessment, created_at=record.created_at,
    )
