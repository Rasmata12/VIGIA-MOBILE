from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import Analysis, User
from app.schemas import HistoryItem
from app.security import current_user

router = APIRouter(prefix="/history", tags=["history"])


@router.get("", response_model=list[HistoryItem])
def list_history(
    limit: int = Query(30, ge=1, le=100),
    offset: int = Query(0, ge=0),
    level: str | None = Query(None, pattern="^(safe|suspicious|dangerous)$"),
    kind: str | None = Query(None, pattern="^(url|text)$"),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[Analysis]:
    query = db.query(Analysis).filter(Analysis.user_id == user.id)
    if level:
        query = query.filter(Analysis.level == level)
    if kind:
        query = query.filter(Analysis.kind == kind)
    return query.order_by(Analysis.created_at.desc()).offset(offset).limit(limit).all()


@router.delete("/{analysis_id}", status_code=204, response_model=None)
def delete_item(analysis_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> None:
    record = db.get(Analysis, analysis_id)
    if record is None or record.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Analyse introuvable.")
    db.delete(record)
    db.commit()


@router.delete("", status_code=204, response_model=None)
def clear_history(user: User = Depends(current_user), db: Session = Depends(get_db)) -> None:
    db.query(Analysis).filter(Analysis.user_id == user.id).delete()
    db.commit()
