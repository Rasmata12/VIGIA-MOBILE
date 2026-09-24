"""Couche IA reelle, sans API payante.

Deux fournisseurs possibles (voir app.config) :
  - "ollama"            : un vrai modele de langage (Llama, Qwen, Mistral...) tourne
                           localement via Ollama. Gratuit, illimite, et surtout : le
                           contenu de l'utilisateur ne quitte jamais le serveur -- ce
                           qui est coherent avec la mission de VIGIA (protection
                           numerique). C'est le mode par defaut.
  - "openai_compatible" : pour brancher une API gratuite tierce qui parle le format
                           OpenAI (ex: niveau gratuit de Groq/OpenRouter), si on ne
                           veut pas heberger de modele.

Dans tous les cas : jamais de resultat invente. Si le modele n'est pas joignable ou
repond n'importe quoi, on retombe silencieusement sur l'heuristique seule et on le
dit honnetement dans `sources`.
"""
from __future__ import annotations

import asyncio
import json
import time

import httpx

from app.config import get_settings
from app.engine.common import EngineResult, Signal, clamp_score, level_from_score

_settings = get_settings()

SYSTEM_PROMPT = (
    "Tu es le moteur d'analyse de VIGIA AI, une application de protection numerique. "
    "Tu recois un contenu soumis par un utilisateur et la liste des signaux detectes par le moteur heuristique. "
    "Tu evalues le risque d'arnaque, d'hameconnage ou d'escroquerie. "
    "Reponds UNIQUEMENT avec un objet JSON valide, sans texte autour et sans balises Markdown, au format exact : "
    '{"risk_score": 0-100, "verdict": "safe|suspicious|dangerous", '
    '"explanation": "2 a 4 phrases en francais simple expliquant le raisonnement", '
    '"indicators": ["indicateur concret", ...], '
    '"recommended_actions": ["action concrete que l utilisateur doit faire", ...]}'
    " N'invente jamais de fait technique que tu ne peux pas deduire du contenu fourni. "
    "Ne considere jamais HTTPS, une marque, un nouveau domaine ou un seul signal heuristique comme une preuve suffisante. "
    "Si les preuves sont contradictoires ou insuffisantes, conserve un score prudent et explique la limite."
)

# Petit cache memoire pour eviter de sonder Ollama a chaque appel de /health :
# une verification de disponibilite reelle, mais pas plus d'une fois toutes les 20s.
_reachability_cache: dict[str, tuple[float, bool, str]] = {}
_CACHE_TTL = 20.0


def _build_prompt(kind: str, content: str, base: EngineResult) -> str:
    signals_desc = [{"code": s.code, "label": s.label, "weight": s.weight} for s in base.signals]
    return (
        f"Type de contenu : {kind}\n"
        f"Score heuristique : {base.score}/100 ({base.level})\n"
        f"Signaux detectes : {json.dumps(signals_desc, ensure_ascii=False)}\n"
        f"Donnees techniques deja verifiees : {json.dumps(base.extracted, ensure_ascii=False, default=str)[:6000]}\n"
        f"Contenu soumis :\n<<<\n{content[:6000]}\n>>>"
    )


async def ai_reachable() -> tuple[bool, str]:
    """Etat REEL de la couche IA (pas seulement 'configuree'). Utilise par /health.
    Mis en cache brievement pour ne pas ralentir les appels frequents a /health."""
    provider = _settings.ai_provider.strip().lower()
    if provider == "none" or not _settings.has_ai:
        return False, "IA desactivee"

    cache_key = provider
    cached = _reachability_cache.get(cache_key)
    now = time.monotonic()
    if cached and now - cached[0] < _CACHE_TTL:
        return cached[1], cached[2]

    ok, detail = await _probe(provider)
    _reachability_cache[cache_key] = (now, ok, detail)
    return ok, detail


async def _probe(provider: str) -> tuple[bool, str]:
    try:
        async with httpx.AsyncClient(timeout=4.0) as client:
            if provider == "ollama":
                resp = await client.get(f"{_settings.ollama_base_url.rstrip('/')}/api/tags")
                resp.raise_for_status()
                models = [m.get("name", "") for m in resp.json().get("models", [])]
                wanted = _settings.ollama_model
                if models and not any(wanted == m or m.startswith(wanted.split(":")[0]) for m in models):
                    return False, f"Ollama joignable mais modele '{wanted}' non telecharge (ollama pull {wanted})"
                return True, f"Ollama joignable ({_settings.ollama_base_url})"
            if provider == "openai_compatible":
                # Pas d'endpoint de sante standard garanti : on considere "joignable" si configure,
                # l'appel reel lors d'une analyse confirmera ou infirmera.
                return bool(_settings.oc_base_url and _settings.oc_api_key), "configure"
    except Exception as exc:
        return False, f"{type(exc).__name__}"
    return False, "fournisseur inconnu"


async def _call_ollama(prompt: str) -> str:
    payload = {
        "model": _settings.ollama_model,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt},
        ],
        "stream": False,
        "format": "json",  # Ollama force une sortie JSON valide : plus fiable qu'un parsing de texte libre.
        "options": {"temperature": 0.15},
    }
    async with httpx.AsyncClient(timeout=_settings.ai_request_timeout) as client:
        response = await client.post(
            f"{_settings.ollama_base_url.rstrip('/')}/api/chat",
            json=payload,
        )
        response.raise_for_status()
        data = response.json()
    return data.get("message", {}).get("content", "")


async def _call_openai_compatible(prompt: str) -> str:
    payload = {
        "model": _settings.oc_model,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt},
        ],
        "temperature": 0.15,
        "response_format": {"type": "json_object"},
    }
    async with httpx.AsyncClient(timeout=_settings.ai_request_timeout) as client:
        response = await client.post(
            f"{_settings.oc_base_url.rstrip('/')}/chat/completions",
            json=payload,
            headers={"Authorization": f"Bearer {_settings.oc_api_key}", "content-type": "application/json"},
        )
        response.raise_for_status()
        data = response.json()
    choices = data.get("choices", [])
    return choices[0]["message"]["content"] if choices else ""


async def _call_hf_chat(messages: list[dict], token: str, model: str) -> str:
    payload = {"model": model, "messages": messages, "temperature": 0.1, "stream": False}
    async with httpx.AsyncClient(timeout=_settings.ai_request_timeout) as client:
        response = await client.post(
            f"{_settings.hf_base_url.rstrip('/')}/chat/completions",
            json=payload,
            headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
        )
        response.raise_for_status()
        data = response.json()
    return data.get("choices", [{}])[0].get("message", {}).get("content", "")


async def analyse_media(frames: list[tuple[bytes, str]], media_type: str, filename: str) -> dict:
    """Analyse photo/vidéo par plusieurs couches.

    Couche A: VLM Hugging Face examine réellement les images.
    Couche B: le texte/URL/téléphone explicitement observés par le VLM sont
    repassés dans les moteurs déterministes VIGIA pour corroboration.
    """
    api_token = _settings.hf_token.strip()
    if not api_token:
        raise RuntimeError("Le moteur vision Hugging Face n'est pas configuré sur le serveur VIGIA.")
    if not frames:
        raise ValueError("Aucune image exploitable n'a été extraite du média.")

    content = [{"type": "text", "text": (
        "Tu es l'analyste visuel forensique de VIGIA. Examine TOUTES les images fournies. "
        "Cherche phishing, faux paiement, fausse preuve de transfert, usurpation de marque, "
        "QR ou URL suspecte, demande de code secret/OTP, coordonnées incohérentes, fausse annonce, "
        "capture d'écran manipulée ou autre tentative de fraude. "
        "Pour le texte, recopie uniquement ce qui est réellement lisible. Pour les URLs et numéros, "
        "recopie uniquement ceux visibles. Ne devine jamais. "
        "Réponds UNIQUEMENT en JSON: "
        "{\"risk_score\":0-100,\"verdict\":\"safe|suspicious|dangerous\", "
        "\"explanation\":\"2-4 phrases en français\", "
        "\"indicators\":[...],\"recommended_actions\":[...], "
        "\"observed_text\":\"texte réellement lisible, sinon chaîne vide\", "
        "\"detected_urls\":[...],\"detected_phones\":[...]}."
    )}]
    import base64
    for data, mime in frames[:6]:
        encoded = base64.b64encode(data).decode("ascii")
        content.append({"type": "image_url", "image_url": {"url": f"data:{mime};base64,{encoded}"}})

    try:
        raw = await _call_hf_chat([{"role": "user", "content": content}], api_token, _settings.hf_vision_model)
    except httpx.HTTPStatusError as exc:
        if exc.response.status_code in {401, 403}:
            raise RuntimeError(
                "Le jeton Hugging Face doit autoriser les appels Inference Providers "
                "(permission « Make calls to Inference Providers »)."
            ) from exc
        if exc.response.status_code == 402:
            raise RuntimeError("Le compte Hugging Face n'a plus de crédit Inference Providers disponible.") from exc
        raise RuntimeError(
            f"Le fournisseur vision Hugging Face a répondu HTTP {exc.response.status_code}."
        ) from exc
    except httpx.TimeoutException as exc:
        raise RuntimeError("L'analyse visuelle a expiré côté Hugging Face. Réessayez dans quelques instants.") from exc
    cleaned = raw.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
    parsed = json.loads(cleaned)

    vision_score = clamp_score(int(float(parsed.get("risk_score", 50))))
    observed_text = str(parsed.get("observed_text", "")).strip()[:12000]
    detected_urls = [str(x).strip() for x in parsed.get("detected_urls", []) if str(x).strip()][:10]
    detected_phones = [str(x).strip() for x in parsed.get("detected_phones", []) if str(x).strip()][:10]

    corroborating_score = 0
    corroborating_indicators: list[str] = []
    if observed_text or detected_urls:
        from app.engine.text_engine import analyse_text
        from app.engine.url_engine import analyse_url
        local_text = "\n".join(x for x in [observed_text, *detected_urls, *detected_phones] if x)[:20000]
        if local_text:
            local = await analyse_text(local_text, online=False)
            corroborating_score = local.score
            corroborating_indicators.extend(s.label for s in local.signals if s.weight > 0)
        for url in detected_urls[:3]:
            try:
                report = await analyse_url(url, online=True)
                corroborating_score = max(corroborating_score, report.score)
                corroborating_indicators.extend(s.label for s in report.signals if s.weight >= 12)
            except Exception:
                pass

    if corroborating_score > 0:
        score = clamp_score(
            max(vision_score, corroborating_score)
            if abs(vision_score - corroborating_score) > 25
            else round(0.55 * vision_score + 0.45 * corroborating_score)
        )
    else:
        score = vision_score
    verdict = level_from_score(score)

    indicators = [str(x) for x in parsed.get("indicators", [])][:8]
    for item in corroborating_indicators:
        if item and item not in indicators:
            indicators.append(item)
    actions = [str(x) for x in parsed.get("recommended_actions", [])][:6]
    if verdict == "dangerous" and not actions:
        actions = ["N'ouvrez pas le contenu et ne communiquez aucun code ou paiement."]
    elif verdict == "suspicious" and not actions:
        actions = ["Vérifiez l'expéditeur par un canal officiel avant toute action."]

    summary = str(parsed.get("explanation", "Analyse visuelle effectuée.")).strip()
    if corroborating_score and corroborating_score >= 35:
        summary += " Des indices textuels ou URL visibles ont également été corroborés par les moteurs VIGIA."

    return {
        "score": score, "level": verdict, "summary": summary,
        "indicators": indicators[:10], "recommended_actions": actions[:6],
        "observed_text": observed_text, "detected_urls": detected_urls, "detected_phones": detected_phones,
        "vision_score": vision_score, "corroborating_score": corroborating_score,
        "model": _settings.hf_vision_model, "frames_analyzed": len(frames[:6]), "media_type": media_type,
    }


async def enrich(kind: str, content: str, base: EngineResult) -> EngineResult:
    """Fusionne l'analyse heuristique avec une vraie reponse du modele. Echec = heuristique seule."""
    provider = _settings.ai_provider.strip().lower()
    if not _settings.has_ai or provider == "none":
        base.sources.append({
            "name": "Analyse IA", "status": "disabled",
            "detail": "Aucun modele configure (VIGIA_AI_PROVIDER=none ou parametres manquants)",
        })
        return base

    prompt = _build_prompt(kind, content, base)
    if provider == "huggingface":
        async def caller(prompt: str) -> str:
            return await _call_hf_chat(
                [
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": prompt},
                ],
                _settings.hf_token,
                _settings.hf_model,
            )
    else:
        caller = _call_ollama if provider == "ollama" else _call_openai_compatible

    parsed: dict | None = None
    last_error = ""
    # Un essai, puis une seule relance en cas d'erreur transitoire (reseau, 429, 5xx, modele qui
    # demarre a froid) : une IA qui echoue ne doit jamais faire planter l'analyse, mais on ne veut
    # pas non plus abandonner la couche IA a la premiere coupure passagere.
    for attempt in range(2):
        try:
            raw = await caller(prompt)
            parsed = json.loads(raw.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip())
            break
        except (httpx.TimeoutException, httpx.TransportError) as exc:
            last_error = type(exc).__name__
            if attempt == 0:
                await asyncio.sleep(0.6)
            continue  # retente une fois sur incident reseau/modele passager
        except httpx.HTTPStatusError as exc:
            last_error = f"HTTP {exc.response.status_code}"
            if exc.response.status_code in (404, 429, 500, 502, 503, 504) and attempt == 0:
                await asyncio.sleep(0.6)
                continue  # retente une fois (404 possible si Ollama charge encore le modele)
            break
        except (json.JSONDecodeError, KeyError, IndexError, TypeError) as exc:
            # reponse non-JSON ou structure inattendue : inutile de retenter, ca ne changera pas.
            last_error = type(exc).__name__
            break
        except Exception as exc:
            last_error = type(exc).__name__
            break

    if parsed is None:
        hint = ""
        if provider == "ollama" and last_error in ("ConnectError", "ConnectTimeout"):
            hint = " (Ollama ne repond pas : verifier qu'il tourne sur " + _settings.ollama_base_url + ")"
        base.sources.append({"name": "Analyse IA", "status": "error", "detail": (last_error or "reponse invalide") + hint})
        return base

    # Le contenu de `parsed` vient du modele : meme si c'est un JSON valide, sa FORME n'est pas
    # garantie (champ manquant, score non numerique, etc.). Toute anomalie ici doit retomber sur
    # l'heuristique seule plutot que de faire planter toute l'analyse.
    try:
        ai_score = clamp_score(int(float(parsed.get("risk_score", base.score))))
        explanation = str(parsed.get("explanation", "")).strip()
        indicators = [str(i) for i in parsed.get("indicators", [])][:6]
        actions = [str(a) for a in parsed.get("recommended_actions", [])][:5]
    except (TypeError, ValueError) as exc:
        base.sources.append({"name": "Analyse IA", "status": "error", "detail": f"reponse mal formee ({type(exc).__name__})"})
        return base

    # Fusion : on retient le maximum des deux moteurs (principe de prudence) si les deux moteurs
    # divergent fortement, sinon une moyenne ponderee.
    merged = clamp_score(max(base.score, ai_score) if abs(base.score - ai_score) > 25 else round(0.5 * base.score + 0.5 * ai_score))
    base.score = merged
    base.level = level_from_score(merged)
    base.ai_used = True
    if explanation:
        base.summary = explanation
    for indicator in indicators:
        base.signals.append(Signal("ai_indicator", indicator, 0, category="ia"))
    for action in actions:
        base.signals.append(Signal("ai_action", action, 0, category="recommandation"))
    if provider == "ollama":
        label = _settings.ollama_model
    elif provider == "huggingface":
        label = _settings.hf_model
    else:
        label = _settings.oc_model
    base.sources.append({"name": "Analyse IA", "status": "ok", "detail": f"{label} (score IA {ai_score})"})
    base.extracted["ai"] = {"score": ai_score, "indicators": indicators, "actions": actions}
    return base
