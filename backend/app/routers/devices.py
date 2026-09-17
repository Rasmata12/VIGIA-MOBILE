from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import AuthSession, Device, User, utcnow
from app.schemas import DeviceOut, DeviceRegisterIn
from app.security import current_user

router = APIRouter(prefix="/devices", tags=["devices"])


@router.post("", response_model=DeviceOut, status_code=201)
def register_device(
    payload: DeviceRegisterIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> Device:
    """Enregistre l'appareil avec son identifiant d'installation REEL.
    Un appareil deja connu est mis a jour, jamais duplique."""
    device = db.query(Device).filter(
        Device.user_id == user.id, Device.install_id == payload.install_id
    ).first()
    if device is None:
        device = Device(user_id=user.id, install_id=payload.install_id)
        db.add(device)
    device.label = payload.label or device.label
    device.platform = payload.platform
    device.app_version = payload.app_version
    device.revoked = False
    device.last_seen_at = utcnow()
    db.commit()
    db.refresh(device)
    return device


@router.get("", response_model=list[DeviceOut])
def list_devices(user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[Device]:
    return db.query(Device).filter(Device.user_id == user.id).order_by(Device.last_seen_at.desc()).all()


@router.delete("/{device_id}", status_code=204, response_model=None)
def revoke_device(device_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)) -> None:
    device = db.get(Device, device_id)
    if device is None or device.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Appareil introuvable.")
    device.revoked = True
    # Revoque aussi les sessions ouvertes depuis cet appareil.
    db.query(AuthSession).filter(
        AuthSession.user_id == user.id, AuthSession.device_label == device.label
    ).update({"revoked": True})
    db.commit()


@router.get("/sessions", response_model=list[dict])
def list_sessions(user: User = Depends(current_user), db: Session = Depends(get_db)) -> list[dict]:
    rows = db.query(AuthSession).filter(AuthSession.user_id == user.id).order_by(
        AuthSession.created_at.desc()
    ).limit(50).all()
    return [{
        "id": s.id, "device_label": s.device_label, "revoked": s.revoked,
        "created_at": s.created_at.isoformat(), "expires_at": s.expires_at.isoformat(),
    } for s in rows]
