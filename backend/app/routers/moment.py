from __future__ import annotations

import json

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.db import get_db
from app.engine.moment import COMBOS, WINDOW, purge_expired, recent_events
from app.models import User
from app.schemas import EventOut, MomentOut
from app.security import current_user

router = APIRouter(prefix="/moment", tags=["moment"])


def _to_out(event) -> EventOut:
    return EventOut(
        id=event.id,
        analysis_id=event.analysis_id,
        source=event.source,
        type=event.type,
        package_name=event.package_name,
        risk_score=event.risk_score,
        risk_level=event.risk_level,
        scam_dna=json.loads(event.dna_json or "[]"),
        related_ids=json.loads(event.related_ids_json or "[]"),
        created_at=event.created_at,
    )


@router.get("", response_model=MomentOut)
def current_moment(
    limit: int = Query(25, ge=1, le=100),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> MomentOut:
    purge_expired(db)
    events = recent_events(db, user.id)[:limit]

    seen: dict[str, list[str]] = {}
    for event in events:
        for trait in json.loads(event.dna_json or "[]"):
            seen.setdefault(trait.get("category", ""), []).append(event.id)

    correlations = []
    for combo, weight, explanation in COMBOS:
        if combo <= set(seen):
            involved = sorted({eid for c in combo for eid in seen[c]})
            if len(involved) >= 2:
                correlations.append({
                    "categories": sorted(combo),
                    "weight": weight,
                    "explanation": explanation,
                    "event_ids": involved,
                })

    return MomentOut(
        window_hours=int(WINDOW.total_seconds() // 3600),
        events=[_to_out(e) for e in events],
        correlations=correlations,
        elevated_risk=bool(correlations),
    )


@router.delete("", status_code=204, response_model=None)
def clear_events(user: User = Depends(current_user), db: Session = Depends(get_db)) -> None:
    from app.models import Event

    db.query(Event).filter(Event.user_id == user.id).delete()
    db.commit()
