from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import AuthSession, Device, User, UserSettings, utcnow
from app.schemas import LoginIn, RefreshIn, RegisterIn, TokenOut, UserOut
from app.security import (
    create_access_token,
    create_refresh_token,
    current_user,
    hash_password,
    rate_limit,
    resolve_refresh_token,
    sha256,
    validate_password,
    verify_password,
)

router = APIRouter(prefix="/auth", tags=["auth"])


def _client_key(request: Request, suffix: str) -> str:
    host = request.client.host if request.client else "unknown"
    return f"{suffix}:{host}"


@router.post("/register", response_model=TokenOut, status_code=201)
def register(payload: RegisterIn, request: Request, db: Session = Depends(get_db)) -> TokenOut:
    rate_limit(db, _client_key(request, "register"), limit=10, window_seconds=3600)
    email = payload.email.lower().strip()
    if db.query(User).filter(User.email == email).first():
        raise HTTPException(status.HTTP_409_CONFLICT, "Un compte existe deja avec cet email.")
    validate_password(payload.password)
    user = User(email=email, full_name=payload.full_name.strip(), password_hash=hash_password(payload.password))
    db.add(user)
    db.flush()
    db.add(UserSettings(user_id=user.id))
    if payload.device_label:
        db.add(Device(user_id=user.id, label=payload.device_label))
    db.commit()
    access, expires = create_access_token(user.id)
    refresh = create_refresh_token(db, user, payload.device_label)
    return TokenOut(access_token=access, refresh_token=refresh, expires_in=expires)


@router.post("/login", response_model=TokenOut)
def login(payload: LoginIn, request: Request, db: Session = Depends(get_db)) -> TokenOut:
    rate_limit(db, _client_key(request, "login"), limit=20, window_seconds=900)
    email = payload.email.lower().strip()
    user = db.query(User).filter(User.email == email).first()
    if user is None or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Email ou mot de passe incorrect.")
    if not user.is_active:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Ce compte est desactive.")
    user.last_login_at = utcnow()
    if user.settings is None:
        db.add(UserSettings(user_id=user.id))
    db.commit()
    access, expires = create_access_token(user.id)
    refresh = create_refresh_token(db, user, payload.device_label)
    return TokenOut(access_token=access, refresh_token=refresh, expires_in=expires)


@router.post("/refresh", response_model=TokenOut)
def refresh_token(payload: RefreshIn, db: Session = Depends(get_db)) -> TokenOut:
    session = resolve_refresh_token(db, payload.refresh_token)
    user = db.get(User, session.user_id)
    if user is None or not user.is_active:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Compte indisponible.")
    session.revoked = True  # rotation du refresh token
    db.commit()
    access, expires = create_access_token(user.id)
    new_refresh = create_refresh_token(db, user, session.device_label)
    return TokenOut(access_token=access, refresh_token=new_refresh, expires_in=expires)


@router.post("/logout", status_code=204, response_model=None)
def logout(payload: RefreshIn, db: Session = Depends(get_db)) -> None:
    session = db.query(AuthSession).filter(AuthSession.token_hash == sha256(payload.refresh_token)).first()
    if session:
        session.revoked = True
        db.commit()


@router.get("/me", response_model=UserOut)
def me(user: User = Depends(current_user)) -> User:
    return user
