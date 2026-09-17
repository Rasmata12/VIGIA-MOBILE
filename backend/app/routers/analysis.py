"""Route historique /analyses — conservee pour compatibilite.
Elle delegue desormais au meme pipeline que /verify (aucune logique dupliquee)."""
from __future__ import annotations

import json

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import Analysis, User
from app.schemas import AnalyseIn, AnalysisOut
from app.security import current_user, rate_limit
from app.services.pipeline import run_pipeline

router = APIRouter(prefix="/analyses", tags=["analyses"])


def _to_out(record: Analysis) -> AnalysisOut:
    return AnalysisOut(
        id=record.id, kind=record.kind, input_preview=record.input_preview, score=record.score,
        level=record.level, summary=record.summary, signals=json.loads(record.signals_json or "[]"),
        sources=json.loads(record.sources_json or "[]"), ai_used=record.ai_used,
        duration_ms=record.duration_ms, created_at=record.created_at,
    )


@router.post("", response_model=AnalysisOut, status_code=201)
async def create_analysis(
    payload: AnalyseIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> AnalysisOut:
    rate_limit(db, f"analyse:{user.id}", limit=120, window_seconds=3600)
    record, _assessment, _alert = await run_pipeline(
        db, user, payload.kind, payload.content, module="verify",
        online=payload.online, use_ai=payload.use_ai,
    )
    return _to_out(record)


@router.get("/{analysis_id}", response_model=AnalysisOut)
def get_analysis(analysis_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> AnalysisOut:
    record = db.get(Analysis, analysis_id)
    if record is None or record.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Analyse introuvable.")
    return _to_out(record)
