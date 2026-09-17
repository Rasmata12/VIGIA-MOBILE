from __future__ import annotations

from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import Alert, Analysis, User
from app.schemas import StatsOut
from app.security import current_user

router = APIRouter(prefix="/stats", tags=["stats"])


@router.get("", response_model=StatsOut)
def stats(user: User = Depends(current_user), db: Session = Depends(get_db)) -> StatsOut:
    rows = db.query(Analysis.level, func.count()).filter(Analysis.user_id == user.id).group_by(Analysis.level).all()
    by_level = {level: count for level, count in rows}
    kinds = db.query(Analysis.kind, func.count()).filter(Analysis.user_id == user.id).group_by(Analysis.kind).all()
    by_kind = {kind: count for kind, count in kinds}
    total = sum(by_level.values())

    since = datetime.now(timezone.utc) - timedelta(days=6)
    recent = db.query(Analysis.created_at, Analysis.level).filter(
        Analysis.user_id == user.id, Analysis.created_at >= since
    ).all()
    buckets: dict[str, dict[str, int]] = {}
    for day_offset in range(6, -1, -1):
        key = (datetime.now(timezone.utc) - timedelta(days=day_offset)).date().isoformat()
        buckets[key] = {"date": key, "total": 0, "dangerous": 0, "suspicious": 0, "safe": 0}
    for created_at, level in recent:
        key = created_at.date().isoformat()
        if key in buckets:
            buckets[key]["total"] += 1
            buckets[key][level] += 1

    last = db.query(func.max(Analysis.created_at)).filter(Analysis.user_id == user.id).scalar()
    unread = db.query(func.count(Alert.id)).filter(Alert.user_id == user.id, Alert.read.is_(False)).scalar() or 0

    # Score de protection : calcule uniquement s'il existe de vraies donnees.
    protection = None
    if total > 0:
        dangerous = by_level.get("dangerous", 0)
        suspicious = by_level.get("suspicious", 0)
        exposure = (dangerous * 2 + suspicious) / (total * 2)
        habit = min(total, 20) / 20  # regularite d'usage reelle
        protection = int(round(100 * (0.6 * (1 - exposure) + 0.4 * habit)))

    return StatsOut(
        total_analyses=total,
        safe=by_level.get("safe", 0),
        suspicious=by_level.get("suspicious", 0),
        dangerous=by_level.get("dangerous", 0),
        url_analyses=by_kind.get("url", 0),
        text_analyses=by_kind.get("text", 0),
        last_7_days=list(buckets.values()),
        last_analysis_at=last,
        protection_score=protection,
        unread_alerts=int(unread),
    )
