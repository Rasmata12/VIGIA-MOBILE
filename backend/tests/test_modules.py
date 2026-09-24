"""Tests des modules avances : Verify, Scam DNA, Risk Engine, Moment, Guard,
Before Pay, Shield, Privacy, Devices, securite."""
from __future__ import annotations

import os
import tempfile

import pytest
from fastapi.testclient import TestClient

os.environ["VIGIA_DATABASE_URL"] = f"sqlite:///{tempfile.mkdtemp()}/modules.db"
os.environ["VIGIA_JWT_SECRET"] = "test-secret-modules-0123456789abcdef"
os.environ["VIGIA_ALLOW_NETWORK_PROBES"] = "false"
os.environ["VIGIA_RATE_LIMIT_ENABLED"] = "false"

from app.db import init_db  # noqa: E402
from app.main import app  # noqa: E402

init_db()
client = TestClient(app)

PHISHING = (
    "URGENT: votre compte Orange Money sera suspendu. Confirmez votre code PIN ici "
    "http://orange-money.verif.tk/login sinon blocage definitif!"
)
LOTERIE = (
    "FELICITATIONS vous avez gagne 2000000 FCFA a la loterie MTN. Envoyez 15000 FCFA "
    "de frais de dossier par Orange Money pour recevoir votre lot. Ne parlez a personne."
)


def account(email: str) -> dict:
    r = client.post("/auth/register", json={"email": email, "password": "MotDePasse123", "full_name": "T"})
    assert r.status_code == 201, r.text
    return {"Authorization": f"Bearer {r.json()['access_token']}"}


# ------------------------------------------------------------------ VERIFY

def test_verify_detects_kind_automatically():
    h = account("v1@test.io")
    url = client.post("/verify", json={"content": "http://paypa1.com/login", "online": False, "use_ai": False}, headers=h)
    assert url.status_code == 201 and url.json()["kind"] == "url"
    txt = client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h)
    assert txt.json()["kind"] == "text"


def test_verify_returns_full_risk_assessment():
    h = account("v2@test.io")
    body = client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h).json()
    a = body["assessment"]
    assert a["level"] == "dangerous" and a["score"] >= 70
    assert 0 <= a["confidence"] <= 100
    assert a["ai_used"] is False                       # aucune IA simulee
    assert a["recommendation"], "une recommandation concrete est attendue"
    assert a["evidence"], "les preuves doivent etre listees"
    # tracabilite par couches
    assert set(a["layers"]) >= {"L1_LOCAL_RULES", "L2_URL", "L3_THREAT_INTEL", "L6_AGGREGATION"}
    assert a["layers"]["L1_LOCAL_RULES"]["signals"] > 0
    # scam dna reellement derive des signaux
    categories = {t["category"] for t in a["scam_dna"]}
    assert {"CREDENTIAL_REQUEST", "URGENCY"} <= categories


def test_confidence_is_not_risk_score():
    h = account("v3@test.io")
    safe = client.post("/verify", json={"content": "Bonjour, rendez-vous demain a 15h au bureau.",
                                        "online": False, "use_ai": False}, headers=h).json()["assessment"]
    assert safe["level"] == "safe" and safe["score"] == 0
    assert safe["confidence"] > 0            # une confiance existe meme sans risque
    assert safe["scam_dna"] == []            # aucun trait invente


def test_verify_detail_and_isolation():
    h1, h2 = account("v4@test.io"), account("v5@test.io")
    created = client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h1).json()
    assert client.get(f"/verify/{created['analysis_id']}", headers=h1).status_code == 200
    assert client.get(f"/verify/{created['analysis_id']}", headers=h2).status_code == 404


# ------------------------------------------------------------------ MOMENT

def test_moment_correlates_two_real_events_only():
    h = account("m1@test.io")
    empty = client.get("/moment", headers=h).json()
    assert empty["events"] == [] and empty["correlations"] == [] and empty["elevated_risk"] is False

    client.post("/verify", json={"content": LOTERIE, "online": False, "use_ai": False}, headers=h)
    second = client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h).json()

    moment = client.get("/moment", headers=h).json()
    assert len(moment["events"]) == 2
    assert moment["elevated_risk"] is True
    assert any(len(c["event_ids"]) >= 2 for c in moment["correlations"])
    # la correlation a bien influence la 2e evaluation
    assert any(s["category"] == "correlation" for s in second["assessment"]["signals"])


def test_moment_events_are_per_user():
    h1, h2 = account("m2@test.io"), account("m3@test.io")
    client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h1)
    assert client.get("/moment", headers=h2).json()["events"] == []


# ------------------------------------------------------------------ GUARD

def test_guard_refuses_events_when_disabled():
    h = account("g1@test.io")
    r = client.post("/guard/events", json={"package_name": "com.whatsapp", "text": PHISHING}, headers=h)
    assert r.status_code == 403  # jamais d'ingestion silencieuse


def test_guard_status_reports_device_truth():
    h = account("g2@test.io")
    status = client.post("/guard/status", json={"listener_enabled": False}, headers=h).json()
    assert status["guard_enabled_server_side"] is False
    assert status["listener_enabled_device"] is False
    assert status["events_last_24h"] == 0


def test_guard_ingests_and_alerts_when_enabled():
    h = account("g3@test.io")
    client.patch("/account/settings", json={"guard_enabled": True}, headers=h)
    ignored = client.post("/guard/events", json={"package_name": "com.android.settings", "text": PHISHING}, headers=h)
    assert ignored.json()["ignored"] is True          # application non couverte -> aucun faux resultat

    short = client.post("/guard/events", json={"package_name": "com.whatsapp", "text": "ok"}, headers=h)
    assert short.json()["ignored"] is True

    body = client.post("/guard/events", json={"package_name": "com.whatsapp", "text": PHISHING,
                                              "analyse": True}, headers=h).json()
    assert body["module"] == "guard" and body["assessment"]["level"] == "dangerous"
    assert body["alert_created"] is True
    alerts = client.get("/account/alerts", headers=h).json()
    assert alerts and alerts[0]["level"] == "dangerous"
    st = client.post("/guard/status", json={"listener_enabled": True}, headers=h).json()
    assert st["events_last_24h"] == 1 and st["alerts_last_24h"] == 1


def test_guard_minimizes_stored_content():
    h = account("g4@test.io")
    client.patch("/account/settings", json={"guard_enabled": True}, headers=h)
    body = client.post("/guard/events", json={"package_name": "com.whatsapp", "text": PHISHING}, headers=h).json()
    assert len(body["input_preview"]) <= 125          # extrait court, pas le message complet


# ------------------------------------------------------------------ BEFORE PAY

def test_before_pay_refuses_card_data():
    h = account("b1@test.io")
    r = client.post("/before-pay", json={"message": "voici ma carte 4111111111111111"}, headers=h)
    assert r.status_code == 400 and "jamais" in r.json()["detail"]


def test_before_pay_requires_content():
    h = account("b2@test.io")
    assert client.post("/before-pay", json={"beneficiary": "Jean"}, headers=h).status_code == 422


def test_before_pay_returns_stop_decision():
    h = account("b3@test.io")
    body = client.post("/before-pay", json={
        "message": LOTERIE, "beneficiary": "+22670000000", "amount": "15000 FCFA",
        "context": "on me demande de payer maintenant", "online": False, "use_ai": False,
    }, headers=h).json()
    assert body["decision"] == "stop"
    assert body["checklist"] and body["beneficiary_notes"]
    assert body["assessment"]["level"] == "dangerous"


# ------------------------------------------------------------------ JOB OFFER

SCAM_JOB_OFFER = (
    "Bonjour, nous recrutons en urgence des agents de saisie, travail a domicile, "
    "sans entretien, salaire de 150000 FCFA par semaine garanti."
)


def test_job_offer_requires_content():
    h = account("j1@test.io")
    r = client.post("/job-offer", json={"online": False, "use_ai": False}, headers=h)
    assert r.status_code == 422


def test_job_offer_flags_upfront_fee_as_red_flag():
    h = account("j2@test.io")
    body = client.post("/job-offer", json={
        "content": SCAM_JOB_OFFER,
        "company_name": "Global Services SARL",
        "contact_email": "recrutement.global2024@gmail.com",
        "salary_promised": "150000 FCFA par semaine",
        "fee_requested": "25000 FCFA de kit de formation",
        "online": False, "use_ai": False,
    }, headers=h).json()
    assert body["decision"] in {"stop", "verifier"}
    assert body["assessment"]["level"] in {"dangerous", "suspicious"}
    assert any("paiement" in flag.lower() or "payer" in flag.lower() for flag in body["red_flags"])
    assert any("messagerie grand public" in flag for flag in body["red_flags"])
    assert body["checklist"]  # la regle "ne jamais payer pour etre recrute" est toujours rappelee


def test_job_offer_without_fee_is_less_severe_than_with_fee():
    h = account("j3@test.io")
    with_fee = client.post("/job-offer", json={
        "content": SCAM_JOB_OFFER, "fee_requested": "10000 FCFA", "online": False, "use_ai": False,
    }, headers=h).json()
    without_fee = client.post("/job-offer", json={
        "content": SCAM_JOB_OFFER, "online": False, "use_ai": False,
    }, headers=h).json()
    assert with_fee["assessment"]["score"] > without_fee["assessment"]["score"]
    assert with_fee["red_flags"] and not without_fee["red_flags"]


def test_job_offer_persists_and_can_be_found_in_history():
    h = account("j4@test.io")
    body = client.post("/job-offer", json={
        "content": SCAM_JOB_OFFER, "fee_requested": "15000 FCFA", "online": False, "use_ai": False,
    }, headers=h).json()
    assert body["analysis_id"]
    history = client.get("/history", headers=h).json()
    assert any(item["id"] == body["analysis_id"] for item in history)


# ------------------------------------------------------------------ LISTING (ANNONCES)

SCAM_LISTING = (
    "A vendre appartement meuble tres bel etat, prix imbattable car depart urgent a l'etranger. "
    "Pas possible de visiter avant le virement, les cles seront remises apres paiement."
)


def test_listing_requires_content():
    h = account("l1@test.io")
    r = client.post("/listing", json={"online": False, "use_ai": False}, headers=h)
    assert r.status_code == 422


def test_listing_flags_deposit_and_no_visit_as_red_flags():
    h = account("l2@test.io")
    body = client.post("/listing", json={
        "content": SCAM_LISTING, "category": "immobilier", "price_asked": "80000 FCFA/mois",
        "can_visit_in_person": False, "deposit_requested": "150000 FCFA d'arrhes",
        "online": False, "use_ai": False,
    }, headers=h).json()
    assert body["decision"] in {"stop", "verifier"}
    assert body["assessment"]["level"] in {"dangerous", "suspicious"}
    assert len(body["red_flags"]) == 2
    assert body["checklist"]


def test_listing_with_visit_possible_is_less_severe():
    h = account("l3@test.io")
    with_deposit = client.post("/listing", json={
        "content": SCAM_LISTING, "deposit_requested": "50000 FCFA", "can_visit_in_person": False,
        "online": False, "use_ai": False,
    }, headers=h).json()
    plain = client.post("/listing", json={
        "content": "Appartement 2 pieces a louer, visites possibles cette semaine sur rendez-vous.",
        "online": False, "use_ai": False,
    }, headers=h).json()
    assert with_deposit["assessment"]["score"] > plain["assessment"]["score"]
    assert plain["red_flags"] == []


# ------------------------------------------------------------------ COMMUNAUTE

def test_community_report_requires_valid_target():
    h = account("c1@test.io")
    r = client.post("/community/reports", json={"target": "bla bla pas une cible"}, headers=h)
    assert r.status_code == 422


def test_community_report_rejects_bank_data():
    h = account("c2@test.io")
    r = client.post("/community/reports", json={
        "target": "faux-site-arnaque.tk", "description": "il demande ma carte 4111111111111111",
    }, headers=h)
    assert r.status_code == 400


def test_community_check_reflects_distinct_reporters_not_duplicates():
    h1, h2, h3 = account("c3@test.io"), account("c4@test.io"), account("c5@test.io")
    target = "arnaque-recrutement-xyz.top"

    # h1 signale deux fois la meme cible -> ne doit compter qu'une fois
    r1 = client.post("/community/reports", json={"target": target, "category": "emploi"}, headers=h1).json()
    assert r1["already_reported_by_me"] is False
    r1b = client.post("/community/reports", json={"target": target, "category": "emploi"}, headers=h1).json()
    assert r1b["already_reported_by_me"] is True

    check1 = client.get("/community/check", params={"target": target}, headers=h1).json()
    assert check1["reporters"] == 1 and check1["risk_from_reports"] == "a_surveiller"

    client.post("/community/reports", json={"target": target, "category": "emploi"}, headers=h2)
    client.post("/community/reports", json={"target": target, "category": "phishing"}, headers=h3)

    check2 = client.get("/community/check", params={"target": target}, headers=h2).json()
    assert check2["reporters"] == 3
    assert check2["by_category"].get("emploi") == 2 and check2["by_category"].get("phishing") == 1
    assert check2["risk_from_reports"] == "suspect"


def test_community_reports_boost_analysis_score():
    h_reporters = [account(f"cr{i}@test.io") for i in range(3)]
    target = "faux-recruteur-alerte.top"
    for h in h_reporters:
        client.post("/community/reports", json={"target": target, "category": "emploi"}, headers=h)

    h_victim = account("cv1@test.io")
    without_target = client.post("/verify", json={
        "content": "Message tout a fait neutre sans aucun lien.", "online": False, "use_ai": False,
    }, headers=h_victim).json()
    with_target = client.post("/verify", json={
        "content": f"Message neutre qui mentionne le site https://{target}/offre en passant.",
        "online": False, "use_ai": False,
    }, headers=h_victim).json()
    assert with_target["assessment"]["score"] > without_target["assessment"]["score"]
    assert any(s["code"] == "community_flagged" for s in with_target["assessment"]["signals"])


# ------------------------------------------------------------------ SHIELD

def test_shield_says_when_data_is_insufficient():
    h = account("s1@test.io")
    body = client.get("/shield", headers=h).json()
    assert body["personal"]["total_analyses"] == 0
    assert body["community"]["analyses_last_30_days"] is None   # rien d'invente
    assert "Pas suffisamment" in body["message"] or body["has_enough_data"]


def test_shield_personal_numbers_match_real_analyses():
    h = account("s2@test.io")
    for _ in range(3):
        client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h)
    body = client.get("/shield", headers=h).json()
    assert body["personal"]["total_analyses"] == 3
    assert body["personal"]["flagged"] == 3
    assert body["personal"]["available"] is True


# ------------------------------------------------------------------ PRIVACY

def test_privacy_summary_and_export_and_wipe():
    h = account("p1@test.io")
    client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h)

    summary = client.get("/privacy/summary", headers=h).json()
    assert summary["analyses_stored"] == 1 and summary["events_stored"] == 1
    assert summary["stores_full_content"] is False
    assert summary["ai_sends_content"] is False        # pas de cle -> pas d'envoi
    assert summary["what_is_never_stored"]

    export = client.get("/privacy/export", headers=h)
    assert export.status_code == 200
    data = export.json()
    assert data["account"]["email"] == "p1@test.io"
    assert len(data["analyses"]) == 1 and len(data["events"]) == 1

    assert client.delete("/privacy/data", headers=h).status_code == 204
    assert client.get("/history", headers=h).json() == []
    assert client.get("/moment", headers=h).json()["events"] == []
    assert client.get("/auth/me", headers=h).status_code == 200   # le compte existe toujours


def test_retention_policy_applies():
    h = account("p2@test.io")
    client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h)
    body = client.post("/privacy/retention/apply", headers=h).json()
    assert body["retention_days"] == 90 and body["deleted_records"] == 0  # rien d'assez ancien


# ------------------------------------------------------------------ DEVICES

def test_device_registration_is_idempotent_and_revocable():
    h = account("d1@test.io")
    first = client.post("/devices", json={"install_id": "abc-123", "label": "Pixel 7",
                                          "app_version": "1.0.0"}, headers=h).json()
    again = client.post("/devices", json={"install_id": "abc-123", "label": "Pixel 7",
                                          "app_version": "1.0.1"}, headers=h).json()
    assert first["id"] == again["id"] and again["app_version"] == "1.0.1"
    devices = client.get("/devices", headers=h).json()
    assert len(devices) == 1                          # aucun faux appareil
    assert client.delete(f"/devices/{first['id']}", headers=h).status_code == 204
    assert client.get("/devices", headers=h).json()[0]["revoked"] is True


def test_devices_are_isolated():
    h1, h2 = account("d2@test.io"), account("d3@test.io")
    dev = client.post("/devices", json={"install_id": "xyz-1"}, headers=h1).json()
    assert client.get("/devices", headers=h2).json() == []
    assert client.delete(f"/devices/{dev['id']}", headers=h2).status_code == 404


# ------------------------------------------------------------------ SETTINGS / SECURITE

def test_settings_switches_have_real_effect():
    h = account("st1@test.io")
    updated = client.patch("/account/settings", json={
        "notifications_enabled": False, "guard_enabled": True, "retention_days": 30, "theme": "light",
    }, headers=h).json()
    assert updated["notifications_enabled"] is False and updated["retention_days"] == 30
    # notifications desactivees -> aucune alerte creee malgre un contenu dangereux
    client.post("/verify", json={"content": PHISHING, "online": False, "use_ai": False}, headers=h)
    assert client.get("/account/alerts", headers=h).json() == []


def test_settings_reject_invalid_values():
    h = account("st2@test.io")
    assert client.patch("/account/settings", json={"retention_days": 9999}, headers=h).status_code == 422
    assert client.patch("/account/settings", json={"theme": "neon"}, headers=h).status_code == 422


@pytest.mark.parametrize("path", ["/verify", "/guard/events", "/before-pay", "/devices"])
def test_all_new_endpoints_require_auth(path):
    assert client.post(path, json={}).status_code == 401


@pytest.mark.parametrize("path", ["/moment", "/shield", "/privacy/summary", "/devices", "/privacy/export"])
def test_all_new_reads_require_auth(path):
    assert client.get(path).status_code == 401


def test_extreme_and_malformed_input_is_rejected_cleanly():
    h = account("x1@test.io")
    huge = client.post("/verify", json={"content": "a" * 25000, "online": False}, headers=h)
    assert huge.status_code == 422
    unicode_url = client.post("/verify", json={"content": "http://xn--80ak6aa92e.com/login",
                                               "kind": "url", "online": False, "use_ai": False}, headers=h)
    assert unicode_url.status_code == 201
    assert any(s["code"] == "punycode" for s in unicode_url.json()["assessment"]["signals"])
    injection = client.post("/verify", json={"content": "'; DROP TABLE users; --", "kind": "text",
                                             "online": False, "use_ai": False}, headers=h)
    assert injection.status_code == 201
    assert client.get("/auth/me", headers=h).status_code == 200   # base intacte


def test_invalid_token_is_rejected():
    assert client.get("/verify/abc", headers={"Authorization": "Bearer not-a-token"}).status_code == 401


def test_community_normalizes_phone_and_deduplicates_reporters():
    from app.db import SessionLocal
    from app.engine.community import record_report, report_counts, normalize_target
    from app.models import User

    assert normalize_target("phone", "00 225 07 11 22 33 44") == "+2250711223344"
    db = SessionLocal()
    try:
        users = db.query(User).all()
        assert len(users) >= 2
        target = "+2250711223344"
        record_report(db, users[0].id, "phone", target, "paiement", "tentative", None)
        record_report(db, users[1].id, "phone", target, "paiement", "tentative", None)
        _, created = record_report(db, users[0].id, "phone", target, "paiement", "doublon", None)
        assert created is False
        assert report_counts(db, [("phone", target)])[("phone", target)] == 2
    finally:
        db.close()


def test_url_deep_page_inspection_detects_sensitive_form_without_executing_page(monkeypatch):
    import asyncio
    import app.engine.url_engine as ue

    class FakeResponse:
        headers = {"content-type": "text/html; charset=utf-8"}
        async def __aenter__(self): return self
        async def __aexit__(self, *exc): return False
        async def aiter_bytes(self):
            yield b'''<html><title>Orange Money Secure</title><form action="https://collector.bad/top"><input name="pin" type="password"></form><p>Entrez votre code PIN et paiement</p></html>'''

    class FakeClient:
        def __init__(self, *args, **kwargs): pass
        async def __aenter__(self): return self
        async def __aexit__(self, *exc): return False
        def stream(self, *args, **kwargs): return FakeResponse()

    async def fake_resolve(host): return ["93.184.216.34"]
    monkeypatch.setattr(ue.httpx, "AsyncClient", FakeClient)
    monkeypatch.setattr(ue, "_resolve", fake_resolve)

    signals, sources, meta = asyncio.run(ue._page_content_signals("https://fake-orange.top/login"))
    codes = {s.code for s in signals}
    assert "credential_form" in codes
    assert "cross_domain_form" in codes
    assert "brand_page_domain_mismatch" in codes
    assert any(x["name"] == "Page HTML" and x["status"] == "ok" for x in sources)
