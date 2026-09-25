from __future__ import annotations

import hashlib
import secrets
import time
from datetime import datetime, timedelta, timezone

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
import base64

import bcrypt
from sqlalchemy.orm import Session

from app.config import get_settings
from app.db import get_db
from app.models import AuthSession, RateLimitCounter, User, utcnow

settings = get_settings()
bearer = HTTPBearer(auto_error=False)

PASSWORD_RULES = "Le mot de passe doit contenir au moins 6 caractères."


def validate_password(password: str) -> None:
    if len(password) < 6:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, PASSWORD_RULES)


def _prehash(password: str) -> bytes:
    """bcrypt tronque a 72 octets : on pre-hache en SHA-256/base64 pour accepter
    n'importe quelle longueur de mot de passe sans perte d'entropie."""
    digest = hashlib.sha256(password.encode("utf-8")).digest()
    return base64.b64encode(digest)


def hash_password(password: str) -> str:
    return bcrypt.hashpw(_prehash(password), bcrypt.gensalt(rounds=12)).decode("utf-8")


def verify_password(password: str, hashed: str) -> bool:
    try:
        return bcrypt.checkpw(_prehash(password), hashed.encode("utf-8"))
    except (ValueError, TypeError):
        return False


def create_access_token(user_id: str) -> tuple[str, int]:
    expires = datetime.now(timezone.utc) + timedelta(minutes=settings.access_token_minutes)
    payload = {"sub": user_id, "exp": expires, "iat": datetime.now(timezone.utc), "typ": "access"}
    token = jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)
    return token, settings.access_token_minutes * 60


def sha256(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def create_refresh_token(db: Session, user: User, device_label: str = "") -> str:
    raw = secrets.token_urlsafe(48)
    session = AuthSession(
        user_id=user.id,
        token_hash=sha256(raw),
        device_label=device_label[:120],
        expires_at=datetime.now(timezone.utc) + timedelta(days=settings.refresh_token_days),
    )
    db.add(session)
    db.commit()
    return raw


def resolve_refresh_token(db: Session, raw: str) -> AuthSession:
    session = db.query(AuthSession).filter(AuthSession.token_hash == sha256(raw)).first()
    if session is None or session.revoked:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Session invalide ou revoquee.")
    expires = session.expires_at
    if expires.tzinfo is None:
        expires = expires.replace(tzinfo=timezone.utc)
    if expires < datetime.now(timezone.utc):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Session expiree, reconnecte-toi.")
    return session


def current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    db: Session = Depends(get_db),
) -> User:
    if credentials is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Authentification requise.")
    try:
        payload = jwt.decode(
            credentials.credentials, settings.jwt_secret, algorithms=[settings.jwt_algorithm]
        )
    except jwt.ExpiredSignatureError:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Token expire.")
    except jwt.PyJWTError:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Token invalide.")
    if payload.get("typ") != "access":
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Type de token invalide.")
    user = db.get(User, payload.get("sub", ""))
    if user is None or not user.is_active:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Compte introuvable ou desactive.")
    user.last_seen = utcnow()
    return user


def rate_limit(db: Session, key: str, limit: int, window_seconds: int) -> None:
    """Limiteur persistant simple (suffisant pour un deploiement mono-instance)."""
    if not settings.rate_limit_enabled:
        return
    now = time.time()
    row = db.get(RateLimitCounter, key)
    if row is None:
        db.add(RateLimitCounter(id=key, count=1, window_start=now))
        db.commit()
        return
    if now - row.window_start > window_seconds:
        row.count = 1
        row.window_start = now
        db.commit()
        return
    if row.count >= limit:
        raise HTTPException(status.HTTP_429_TOO_MANY_REQUESTS, "Trop de requetes, reessaie plus tard.")
    row.count += 1
    db.commit()
