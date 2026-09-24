from __future__ import annotations

import re
from datetime import datetime, timedelta, timezone
from sqlalchemy import func

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.engine import community
from app.models import Analysis, CommunityReport, User
from app.schemas import CommunityCheckOut, CommunityReportIn, CommunityReportOut, CommunityTrendingOut, CommunityTrendingItem
from app.security import current_user, rate_limit

router = APIRouter(prefix="/community", tags=["community"])

# Un signalement ne doit jamais devenir un canal de fuite de donnees bancaires.
FORBIDDEN_PATTERNS = [
    (re.compile(r"\bcvv\s*[:=]?\s*\d{3,4}\b", re.I), "un cryptogramme de carte"),
]

CARD_NUMBER_CANDIDATE = re.compile(r"(?<!\d)(?:\d[ -]?){12,18}\d(?!\d)")


def _contains_card_number(value: str, allow_international_phone: bool = False) -> bool:
    """Bloque les cartes par checksum Luhn sans rejeter les numeros de telephone longs."""
    compact = re.sub(r"[\s-]", "", value or "")
    if allow_international_phone and (compact.startswith("+") or compact.startswith("00")):
        digits = re.sub(r"\D", "", compact)
        if 10 <= len(digits) <= 15:
            return False

    for candidate in CARD_NUMBER_CANDIDATE.findall(value or ""):
        digits = re.sub(r"\D", "", candidate)
        if not 13 <= len(digits) <= 19:
            continue
        total = 0
        parity = len(digits) % 2
        for index, char in enumerate(digits):
            number = int(char)
            if index % 2 == parity:
                number *= 2
                if number > 9:
                    number -= 9
            total += number
        if total % 10 == 0:
            return True
    return False

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

    if _contains_card_number(payload.target, allow_international_phone=True) or _contains_card_number(payload.description):
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            "Ta saisie semble contenir un numero de carte bancaire. Signale uniquement le lien, le domaine ou le numero de telephone.",
        )

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
    rate_limit(db, f"community_check:{user.id}", limit=120, window_seconds=3600)
    detected = community.detect_target(target)
    if detected is None:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Cible non reconnue (ni lien, ni numero).")
    target_type, target_key = detected
    summary = community.community_summary(db, target_type, target_key)
    return CommunityCheckOut(
        target_type=target_type, target_key=target_key, reporters=summary["reporters"],
        by_category=summary["by_category"], risk_from_reports=_risk_label(summary["reporters"]),
    )


@router.get("/trending", response_model=CommunityTrendingOut)
def trending(
    limit: int = 10,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> CommunityTrendingOut:
    """Expose uniquement des cibles deja signalees, agregees et anonymisees.

    Aucune description, identite ou analyse personnelle n'est renvoyee. Une cible n'apparait
    qu'apres au moins deux signalants distincts afin de limiter l'effet d'un faux signalement unique.
    """
    rate_limit(db, f"community_trending:{user.id}", limit=60, window_seconds=3600)
    limit = max(1, min(limit, 20))
    since = datetime.now(timezone.utc) - timedelta(days=30)
    rows = (
        db.query(
            CommunityReport.target_type,
            CommunityReport.target_key,
            func.count(func.distinct(CommunityReport.user_id)).label("reporters"),
            func.max(CommunityReport.created_at).label("last_reported_at"),
        )
        .filter(CommunityReport.created_at >= since)
        .group_by(CommunityReport.target_type, CommunityReport.target_key)
        .having(func.count(func.distinct(CommunityReport.user_id)) >= 2)
        .order_by(func.count(func.distinct(CommunityReport.user_id)).desc(), func.max(CommunityReport.created_at).desc())
        .limit(limit)
        .all()
    )
    items = []
    for target_type, target_key, reporters, last_reported_at in rows:
        top = (
            db.query(CommunityReport.category, func.count(CommunityReport.id).label("n"))
            .filter(CommunityReport.target_type == target_type, CommunityReport.target_key == target_key, CommunityReport.created_at >= since)
            .group_by(CommunityReport.category)
            .order_by(func.count(CommunityReport.id).desc())
            .first()
        )
        items.append(CommunityTrendingItem(
            target_type=target_type, target_key=target_key, reporters=int(reporters),
            last_reported_at=last_reported_at, top_category=top[0] if top else "autre",
        ))
    return CommunityTrendingOut(window_days=30, items=items)
