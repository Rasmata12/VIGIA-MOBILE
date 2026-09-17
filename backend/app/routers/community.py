from __future__ import annotations

import re

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.engine import community
from app.models import Analysis, User
from app.schemas import CommunityCheckOut, CommunityReportIn, CommunityReportOut
from app.security import current_user, rate_limit

router = APIRouter(prefix="/community", tags=["community"])

# Un signalement ne doit jamais devenir un canal de fuite de donnees bancaires.
FORBIDDEN_PATTERNS = [
    (re.compile(r"\b\d{13,19}\b"), "un numero de carte bancaire"),
    (re.compile(r"\bcvv\s*[:=]?\s*\d{3,4}\b", re.I), "un cryptogramme de carte"),
]

RISK_LABELS = [
    (5, "tres_signale"),
    (3, "suspect"),
    (1, "a_surveiller"),
    (0, "aucun"),
]


def _risk_label(reporters: int) -> str:
    for threshold, label in RISK_LABELS:
        if reporters >= threshold:
            return label
    return "aucun"


@router.post("/reports", response_model=CommunityReportOut, status_code=201)
def submit_report(
    payload: CommunityReportIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> CommunityReportOut:
    # Limite genereuse mais reelle : evite le bourrage automatise sans gener un usage normal.
    rate_limit(db, f"community_report:{user.id}", limit=30, window_seconds=3600)

    for pattern, what in FORBIDDEN_PATTERNS:
        if pattern.search(payload.target) or pattern.search(payload.description):
            raise HTTPException(
                status.HTTP_400_BAD_REQUEST,
                f"Ta saisie semble contenir {what}. Ne signale jamais de donnee bancaire, "
                "meme celle d'un escroc : signale uniquement le lien, le domaine ou le numero.",
            )

    detected = community.detect_target(payload.target)
    if detected is None:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            "Impossible d'identifier un lien/domaine ou un numero de telephone valide dans ta saisie.",
        )
    target_type, target_key = detected

    analysis_id = payload.analysis_id
    if analysis_id:
        owned = db.query(Analysis).filter(Analysis.id == analysis_id, Analysis.user_id == user.id).first()
        if owned is None:
            analysis_id = None  # on ignore silencieusement une reference invalide plutot que d'echouer

    report, created = community.record_report(
        db, user.id, target_type, target_key, payload.category, payload.description, analysis_id,
    )
    counts = community.report_counts(db, [(target_type, target_key)])
    reporters = counts.get((target_type, target_key), 1)

    return CommunityReportOut(
        id=report.id, target_type=target_type, target_key=target_key, category=report.category,
        already_reported_by_me=not created, community_reporters=reporters, created_at=report.created_at,
    )


@router.get("/check", response_model=CommunityCheckOut)
def check_target(
    target: str,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> CommunityCheckOut:
    """Verifie si la communaute a deja signale ce lien/domaine ou ce numero, AVANT d'agir."""
    detected = community.detect_target(target)
    if detected is None:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Cible non reconnue (ni lien, ni numero).")
    target_type, target_key = detected
    summary = community.community_summary(db, target_type, target_key)
    return CommunityCheckOut(
        target_type=target_type, target_key=target_key, reporters=summary["reporters"],
        by_category=summary["by_category"], risk_from_reports=_risk_label(summary["reporters"]),
    )
