from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import Alert, AuthSession, CommunityReport, Device, Event, User, UserSettings
from app.schemas import AlertOut, DeleteAccountIn, SettingsFullOut, SettingsFullPatch, UserOut
from app.security import current_user, verify_password

router = APIRouter(prefix="/account", tags=["account"])


def _settings_row(db: Session, user: User) -> UserSettings:
    if user.settings is None:
        row = UserSettings(user_id=user.id)
        db.add(row)
        db.commit()
        db.refresh(row)
        return row
    return user.settings


@router.get("/settings", response_model=SettingsFullOut)
def get_settings(user: User = Depends(current_user), db: Session = Depends(get_db)) -> UserSettings:
    return _settings_row(db, user)


@router.patch("/settings", response_model=SettingsFullOut)
def update_settings(payload: SettingsFullPatch, user: User = Depends(current_user), db: Session = Depends(get_db)) -> UserSettings:
    row = _settings_row(db, user)
    for field, value in payload.model_dump(exclude_none=True).items():
        setattr(row, field, value)
    db.commit()
    db.refresh(row)
    return row


@router.get("/alerts", response_model=list[AlertOut])
def list_alerts(
    unread_only: bool = Query(False),
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> list[Alert]:
    query = db.query(Alert).filter(Alert.user_id == user.id)
    if unread_only:
        query = query.filter(Alert.read.is_(False))
    return query.order_by(Alert.created_at.desc()).limit(50).all()


@router.post("/alerts/{alert_id}/read", status_code=204, response_model=None)
def mark_read(alert_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> None:
    alert = db.get(Alert, alert_id)
    if alert is None or alert.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Alerte introuvable.")
    alert.read = True
    db.commit()


@router.get("/profile", response_model=UserOut)
def profile(user: User = Depends(current_user)) -> User:
    return user


@router.delete("", status_code=204, response_model=None)
def delete_account(payload: DeleteAccountIn, user: User = Depends(current_user), db: Session = Depends(get_db)) -> None:
    if not verify_password(payload.password, user.password_hash):
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Mot de passe incorrect.")
    # Event, Device et CommunityReport n'ont pas de relation ORM en cascade sur User (contrairement
    # a analyses/alertes/parametres) et SQLite n'applique pas ON DELETE CASCADE par defaut :
    # sans cette purge explicite, ces lignes restaient orphelines apres suppression du compte.
    db.query(AuthSession).filter(AuthSession.user_id == user.id).delete()
    db.query(Event).filter(Event.user_id == user.id).delete()
    db.query(Device).filter(Device.user_id == user.id).delete()
    db.query(CommunityReport).filter(CommunityReport.user_id == user.id).delete()
    db.delete(user)  # cascade ORM -> analyses, alertes, parametres
    db.commit()
