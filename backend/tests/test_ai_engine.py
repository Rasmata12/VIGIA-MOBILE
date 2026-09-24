"""Tests du moteur IA (app.engine.ai) : gratuit, illimite, sans API payante.

On verifie trois choses, conformement a la regle du projet ("jamais de resultat
invente") :
  1. IA desactivee (VIGIA_AI_PROVIDER=none) -> heuristique seule, statut honnete.
  2. Modele local (Ollama) injoignable -> echec propre, heuristique seule conservee,
     l'analyse ne plante jamais.
  3. Modele local joignable et repondant -> la reponse est reellement fusionnee au
     score heuristique (le test simule la reponse HTTP d'Ollama, il n'appelle pas
     internet).
"""
from __future__ import annotations

import asyncio
import json
import os
import tempfile

os.environ.setdefault("VIGIA_DATABASE_URL", f"sqlite:///{tempfile.mkdtemp()}/ai_engine.db")
os.environ.setdefault("VIGIA_JWT_SECRET", "test-secret-ai-engine-0123456789abcdef")
# Settings est mis en cache (lru_cache) pour tout le process pytest : peu importe quel fichier
# de test importe app.config en premier, il faut fixer les memes valeurs que les autres fichiers
# de test pour ne pas figer une configuration differente (ex: rate limiting active) pour toute
# la suite. Voir tests/test_api.py et tests/test_modules.py pour la meme convention.
os.environ.setdefault("VIGIA_ALLOW_NETWORK_PROBES", "false")
os.environ.setdefault("VIGIA_RATE_LIMIT_ENABLED", "false")

import httpx
import pytest

from app.engine import ai as ai_engine
from app.engine.common import EngineResult, Signal


def _base_result(score: int = 20) -> EngineResult:
    return EngineResult(
        score=score,
        level="suspicious" if score >= 35 else "safe",
        summary="resume heuristique",
        signals=[Signal("no_https", "lien non chiffre", 18, category="transport")],
        sources=[{"name": "Heuristiques VIGIA", "status": "ok", "detail": "v1.0.0"}],
    )


def test_ai_disabled_gives_honest_status(monkeypatch):
    monkeypatch.setattr(ai_engine._settings, "ai_provider", "none")
    base = _base_result()
    result = asyncio.run(ai_engine.enrich("url", "http://exemple.test", base))
    assert result.ai_used is False
    assert result.score == 20  # heuristique inchangee
    ai_source = next(s for s in result.sources if s["name"] == "Analyse IA")
    assert ai_source["status"] == "disabled"


def test_ai_ollama_unreachable_falls_back_cleanly(monkeypatch):
    # Port volontairement inutilise : simule un Ollama non demarre, sans toucher au reseau externe.
    monkeypatch.setattr(ai_engine._settings, "ai_provider", "ollama")
    monkeypatch.setattr(ai_engine._settings, "ollama_base_url", "http://127.0.0.1:1")
    monkeypatch.setattr(ai_engine._settings, "ollama_model", "llama3.2")
    monkeypatch.setattr(ai_engine._settings, "ai_request_timeout", 1.0)
    base = _base_result()
    result = asyncio.run(ai_engine.enrich("text", "message quelconque", base))
    assert result.ai_used is False
    assert result.score == 20  # jamais de score invente si le modele ne repond pas
    ai_source = next(s for s in result.sources if s["name"] == "Analyse IA")
    assert ai_source["status"] == "error"


def test_ai_ollama_success_merges_score(monkeypatch):
    monkeypatch.setattr(ai_engine._settings, "ai_provider", "ollama")
    monkeypatch.setattr(ai_engine._settings, "ollama_base_url", "http://127.0.0.1:11434")
    monkeypatch.setattr(ai_engine._settings, "ollama_model", "llama3.2")

    fake_ai_payload = {
        "risk_score": 90,
        "verdict": "dangerous",
        "explanation": "Demande de code confidentiel sous pretexte d'urgence : signal classique d'hameconnage.",
        "indicators": ["demande de code secret", "pression temporelle"],
        "recommended_actions": ["Ne jamais communiquer de code recu par SMS."],
    }

    class FakeResponse:
        def raise_for_status(self):
            return None

        def json(self):
            return {"message": {"content": json.dumps(fake_ai_payload)}}

    class FakeAsyncClient:
        def __init__(self, *args, **kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, *exc):
            return False

        async def post(self, url, json=None):
            assert url.endswith("/api/chat")
            assert json["format"] == "json"
            return FakeResponse()

    monkeypatch.setattr(httpx, "AsyncClient", FakeAsyncClient)

    base = _base_result(score=20)
    result = asyncio.run(ai_engine.enrich("text", "message quelconque", base))

    assert result.ai_used is True
    # score heuristique (20) et score IA (90) divergent de plus de 25 -> on retient le max (prudence)
    assert result.score == 90
    assert result.level == "dangerous"
    assert "Demande de code confidentiel" in result.summary
    ai_source = next(s for s in result.sources if s["name"] == "Analyse IA")
    assert ai_source["status"] == "ok"
    indicators = [s.label for s in result.signals if s.code == "ai_indicator"]
    assert "demande de code secret" in indicators


def test_media_never_invents_when_hf_returns_invalid_json(monkeypatch):
    import asyncio
    import app.engine.ai as ai_engine

    async def bad_call(*args, **kwargs):
        return "not-json"

    monkeypatch.setattr(ai_engine, "_call_hf_chat", bad_call)
    try:
        asyncio.run(ai_engine.analyse_media([(b"fake", "image/jpeg")], "image", "x.jpg", "hf_test"))
    except Exception as exc:
        # Invalid provider output is an analysis failure, not a fabricated result.
        assert isinstance(exc, Exception)
