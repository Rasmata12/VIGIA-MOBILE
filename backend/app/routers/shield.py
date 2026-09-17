from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import Analysis, Event, User
from app.schemas import ShieldOut
from app.security import current_user

router = APIRouter(prefix="/shield", tags=["shield"])

# Seuil en dessous duquel une statistique communautaire n'a aucune valeur :
# on prefere dire "pas assez de donnees" que d'afficher un chiffre trompeur.
MIN_COMMUNITY_ANALYSES = 50
MIN_PERSONAL_ANALYSES = 3


@router.get("", response_model=ShieldOut)
def shield(user: User = Depends(current_user), db: Session = Depends(get_db)) -> ShieldOut:
    now = datetime.now(timezone.utc)
    since_30 = now - timedelta(days=30)

    personal_total = db.query(func.count(Analysis.id)).filter(Analysis.user_id == user.id).scalar() or 0
    personal_flagged = db.query(func.count(Analysis.id)).filter(
        Analysis.user_id == user.id, Analysis.level.in_(["dangerous", "suspicious"])
    ).scalar() or 0
    personal_30 = db.query(func.count(Analysis.id)).filter(
        Analysis.user_id == user.id, Analysis.created_at >= since_30
    ).scalar() or 0

    community_total = db.query(func.count(Analysis.id)).filter(Analysis.created_at >= since_30).scalar() or 0
    community_flagged = db.query(func.count(Analysis.id)).filter(
        Analysis.created_at >= since_30, Analysis.level.in_(["dangerous", "suspicious"])
    ).scalar() or 0

    # Categories SCAM DNA reellement observees (agregation sur les evenements stockes).
    counter: dict[str, int] = {}
    rows = db.query(Event.dna_json).filter(Event.created_at >= since_30).limit(5000).all()
    for (dna_json,) in rows:
        for trait in json.loads(dna_json or "[]"):
            category = trait.get("category")
            if category:
                counter[category] = counter.get(category, 0) + 1
    top = [{"category": k, "occurrences": v} for k, v in sorted(counter.items(), key=lambda kv: -kv[1])[:6]]

    enough_community = community_total >= MIN_COMMUNITY_ANALYSES
    enough_personal = personal_total >= MIN_PERSONAL_ANALYSES

    if not enough_personal and not enough_community:
        message = (
            "Pas suffisamment de donnees disponibles. VIGIA Shield n'affiche que des chiffres "
            "calcules a partir d'analyses reellement effectuees : effectue quelques analyses pour "
            "voir apparaitre tes tendances."
        )
    elif not enough_community:
        message = (
            f"Tes propres donnees sont affichees. Les tendances communautaires necessitent au moins "
            f"{MIN_COMMUNITY_ANALYSES} analyses sur 30 jours sur cette instance ({community_total} actuellement)."
        )
    else:
        message = "Chiffres calcules a partir des analyses reellement effectuees sur cette instance VIGIA."

    return ShieldOut(
        has_enough_data=enough_personal or enough_community,
        message=message,
        personal={
            "total_analyses": int(personal_total),
            "flagged": int(personal_flagged),
            "last_30_days": int(personal_30),
            "flagged_ratio": round(personal_flagged / personal_total, 3) if personal_total else None,
            "available": enough_personal,
        },
        community={
            "analyses_last_30_days": int(community_total) if enough_community else None,
            "flagged_last_30_days": int(community_flagged) if enough_community else None,
            "flagged_ratio": round(community_flagged / community_total, 3) if enough_community and community_total else None,
            "available": enough_community,
            "scope": "instance VIGIA courante uniquement — aucune statistique nationale n'est inventee",
        },
        top_categories=top if enough_community else [],
        generated_at=now,
    )
