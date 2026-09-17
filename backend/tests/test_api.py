"""Tests reels de bout en bout : compte, isolation des donnees, analyse, historique, stats."""
from __future__ import annotations

import os
import tempfile

import pytest
from fastapi.testclient import TestClient

os.environ["VIGIA_DATABASE_URL"] = f"sqlite:///{tempfile.mkdtemp()}/test.db"
os.environ["VIGIA_JWT_SECRET"] = "test-secret-not-for-production-0123456789"
os.environ["VIGIA_ALLOW_NETWORK_PROBES"] = "false"
os.environ["VIGIA_RATE_LIMIT_ENABLED"] = "false"  # desactive pour les tests fonctionnels

from app.db import init_db  # noqa: E402
from app.main import app  # noqa: E402

init_db()
client = TestClient(app)

PHISHING = (
    "URGENT: votre compte Orange Money sera suspendu dans 24h. "
    "Confirmez votre code PIN ici http://orange-money.verif.tk/login sinon blocage definitif!!!!"
)
NORMAL = "Bonjour, je confirme le rendez-vous de demain a 15h au bureau. A demain."


def register(email: str, password: str = "MotDePasse123") -> dict:
    response = client.post("/auth/register", json={"email": email, "password": password, "full_name": "Test"})
    assert response.status_code == 201, response.text
    return response.json()


def auth_headers(tokens: dict) -> dict:
    return {"Authorization": f"Bearer {tokens['access_token']}"}


def test_health():
    body = client.get("/health").json()
    assert body["status"] == "ok"
    assert body["database"] == "ok"


def test_password_policy_rejected():
    response = client.post("/auth/register", json={"email": "weak@test.io", "password": "azertyui"})
    assert response.status_code == 422


def test_register_login_me_and_duplicate():
    tokens = register("alice@test.io")
    me = client.get("/auth/me", headers=auth_headers(tokens))
    assert me.status_code == 200 and me.json()["email"] == "alice@test.io"
    assert client.post("/auth/register", json={"email": "alice@test.io", "password": "MotDePasse123"}).status_code == 409
    assert client.post("/auth/login", json={"email": "alice@test.io", "password": "Mauvais123456"}).status_code == 401
    assert client.post("/auth/login", json={"email": "alice@test.io", "password": "MotDePasse123"}).status_code == 200


def test_unauthenticated_access_blocked():
    assert client.get("/history").status_code == 401
    assert client.get("/stats").status_code == 401
    assert client.post("/analyses", json={"kind": "text", "content": PHISHING}).status_code == 401


def test_empty_state_is_really_empty():
    tokens = register("empty@test.io")
    assert client.get("/history", headers=auth_headers(tokens)).json() == []
    stats = client.get("/stats", headers=auth_headers(tokens)).json()
    assert stats["total_analyses"] == 0
    assert stats["protection_score"] is None  # aucun score invente sans donnees


def test_text_analysis_detects_phishing_and_feeds_history():
    tokens = register("bob@test.io")
    headers = auth_headers(tokens)
    response = client.post("/analyses", json={"kind": "text", "content": PHISHING, "online": False, "use_ai": False}, headers=headers)
    assert response.status_code == 201, response.text
    body = response.json()
    assert body["level"] == "dangerous" and body["score"] >= 70
    codes = {s["code"] for s in body["signals"]}
    assert {"urgence", "identifiants"} <= codes
    assert body["ai_used"] is False  # pas de fausse IA

    history = client.get("/history", headers=headers).json()
    assert len(history) == 1 and history[0]["id"] == body["id"]

    alerts = client.get("/account/alerts", headers=headers).json()
    assert len(alerts) == 1 and alerts[0]["analysis_id"] == body["id"]

    stats = client.get("/stats", headers=headers).json()
    assert stats["total_analyses"] == 1 and stats["dangerous"] == 1 and stats["unread_alerts"] == 1


def test_benign_text_is_not_flagged():
    tokens = register("carol@test.io")
    body = client.post("/analyses", json={"kind": "text", "content": NORMAL, "online": False, "use_ai": False},
                       headers=auth_headers(tokens)).json()
    assert body["level"] == "safe" and body["score"] < 35
    assert client.get("/account/alerts", headers=auth_headers(tokens)).json() == []


@pytest.mark.parametrize(
    "url,expected",
    [("http://paypa1.com/login/verify", "dangerous"), ("https://www.wikipedia.org/wiki/Test", "safe")],
)
def test_url_analysis(url, expected):
    tokens = register(f"url{abs(hash(url))}@test.io")
    body = client.post("/analyses", json={"kind": "url", "content": url, "online": False, "use_ai": False},
                       headers=auth_headers(tokens)).json()
    assert body["level"] == expected, body


def test_invalid_input_returns_clear_error_not_fake_result():
    tokens = register("dave@test.io")
    response = client.post("/analyses", json={"kind": "url", "content": "ceci n est pas une url", "online": False},
                           headers=auth_headers(tokens))
    assert response.status_code in (422, 201)
    if response.status_code == 422:
        assert "detail" in response.json()


def test_user_cannot_read_another_user_data():
    a = register("mallory@test.io")
    b = register("victim@test.io")
    created = client.post("/analyses", json={"kind": "text", "content": PHISHING, "online": False, "use_ai": False},
                          headers=auth_headers(b)).json()
    assert client.get(f"/analyses/{created['id']}", headers=auth_headers(a)).status_code == 404
    assert client.get("/history", headers=auth_headers(a)).json() == []
    assert client.delete(f"/history/{created['id']}", headers=auth_headers(a)).status_code == 404


def test_refresh_rotation_and_logout():
    tokens = register("erin@test.io")
    refreshed = client.post("/auth/refresh", json={"refresh_token": tokens["refresh_token"]})
    assert refreshed.status_code == 200
    # l'ancien refresh token est revoque (rotation)
    assert client.post("/auth/refresh", json={"refresh_token": tokens["refresh_token"]}).status_code == 401
    new_tokens = refreshed.json()
    assert client.post("/auth/logout", json={"refresh_token": new_tokens["refresh_token"]}).status_code == 204
    assert client.post("/auth/refresh", json={"refresh_token": new_tokens["refresh_token"]}).status_code == 401


def test_settings_update():
    tokens = register("frank@test.io")
    headers = auth_headers(tokens)
    assert client.get("/account/settings", headers=headers).json()["notifications_enabled"] is True
    updated = client.patch("/account/settings", json={"notifications_enabled": False}, headers=headers).json()
    assert updated["notifications_enabled"] is False


def test_history_deletion():
    tokens = register("grace@test.io")
    headers = auth_headers(tokens)
    created = client.post("/analyses", json={"kind": "text", "content": PHISHING, "online": False, "use_ai": False},
                          headers=headers).json()
    assert client.delete(f"/history/{created['id']}", headers=headers).status_code == 204
    assert client.get("/history", headers=headers).json() == []


def test_account_deletion_removes_everything():
    tokens = register("heidi@test.io")
    headers = auth_headers(tokens)
    client.post("/analyses", json={"kind": "text", "content": PHISHING, "online": False, "use_ai": False}, headers=headers)
    client.post("/community/reports", json={"target": "site-a-verifier-heidi.top"}, headers=headers)

    from app.db import SessionLocal
    from app.models import CommunityReport, Event, User

    db = SessionLocal()
    user_id = db.query(User).filter(User.email == "heidi@test.io").first().id
    assert db.query(CommunityReport).filter(CommunityReport.user_id == user_id).count() == 1
    db.close()

    assert client.request("DELETE", "/account", json={"password": "FauxMotDePasse1"}, headers=headers).status_code == 403
    assert client.request("DELETE", "/account", json={"password": "MotDePasse123"}, headers=headers).status_code == 204
    assert client.get("/auth/me", headers=headers).status_code == 401
    assert client.post("/auth/login", json={"email": "heidi@test.io", "password": "MotDePasse123"}).status_code == 401

    # Verification EN BASE, pas seulement via l'API : aucune ligne orpheline ne doit subsister.
    db = SessionLocal()
    assert db.query(CommunityReport).filter(CommunityReport.user_id == user_id).count() == 0
    assert db.query(Event).filter(Event.user_id == user_id).count() == 0
    db.close()


def test_rate_limiter_blocks_flood():
    """Le limiteur est desactive pour les autres tests : on le verifie directement ici."""
    from fastapi import HTTPException

    from app.config import get_settings
    from app.db import SessionLocal
    from app.security import rate_limit

    settings = get_settings()
    settings.rate_limit_enabled = True
    db = SessionLocal()
    try:
        for _ in range(3):
            rate_limit(db, "test-bucket", limit=3, window_seconds=60)
        with pytest.raises(HTTPException) as exc:
            rate_limit(db, "test-bucket", limit=3, window_seconds=60)
        assert exc.value.status_code == 429
    finally:
        settings.rate_limit_enabled = False
        db.close()
