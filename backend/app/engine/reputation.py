"""Sources de reputation externes reelles. Actives uniquement si la cle existe."""
from __future__ import annotations

import base64

import httpx

from app.config import get_settings
from app.engine.common import Signal

_settings = get_settings()


async def google_safebrowsing(url: str) -> tuple[list[Signal], dict]:
    if not _settings.has_safebrowsing:
        return [], {"name": "Google Safe Browsing", "status": "disabled", "detail": "cle API non configuree"}
    payload = {
        "client": {"clientId": "vigia-ai", "clientVersion": "1.0.0"},
        "threatInfo": {
            "threatTypes": ["MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"],
            "platformTypes": ["ANY_PLATFORM"],
            "threatEntryTypes": ["URL"],
            "threatEntries": [{"url": url}],
        },
    }
    endpoint = f"https://safebrowsing.googleapis.com/v4/threatMatches:find?key={_settings.google_safebrowsing_key}"
    try:
        async with httpx.AsyncClient(timeout=_settings.http_timeout) as client:
            response = await client.post(endpoint, json=payload)
            response.raise_for_status()
            data = response.json()
    except Exception as exc:
        return [], {"name": "Google Safe Browsing", "status": "error", "detail": type(exc).__name__}

    matches = data.get("matches") or []
    if not matches:
        return [], {"name": "Google Safe Browsing", "status": "ok", "detail": "aucune correspondance"}
    kinds = sorted({m.get("threatType", "?") for m in matches})
    signal = Signal(
        "gsb_match",
        f"Google Safe Browsing signale ce lien comme dangereux ({', '.join(kinds)}).",
        70, ", ".join(kinds), "reputation",
    )
    return [signal], {"name": "Google Safe Browsing", "status": "flagged", "detail": ", ".join(kinds)}


async def virustotal(url: str) -> tuple[list[Signal], dict]:
    if not _settings.has_virustotal:
        return [], {"name": "VirusTotal", "status": "disabled", "detail": "cle API non configuree"}
    url_id = base64.urlsafe_b64encode(url.encode()).decode().strip("=")
    try:
        async with httpx.AsyncClient(timeout=_settings.http_timeout) as client:
            response = await client.get(
                f"https://www.virustotal.com/api/v3/urls/{url_id}",
                headers={"x-apikey": _settings.virustotal_key},
            )
            if response.status_code == 404:
                return [], {"name": "VirusTotal", "status": "unknown", "detail": "URL jamais analysee"}
            response.raise_for_status()
            stats = response.json()["data"]["attributes"]["last_analysis_stats"]
    except Exception as exc:
        return [], {"name": "VirusTotal", "status": "error", "detail": type(exc).__name__}

    malicious = int(stats.get("malicious", 0))
    suspicious = int(stats.get("suspicious", 0))
    detail = f"{malicious} moteur(s) malveillant / {suspicious} suspect"
    if malicious >= 3:
        return [Signal("vt_malicious", f"VirusTotal : {malicious} moteurs antivirus classent ce lien comme malveillant.", 60, detail, "reputation")], {"name": "VirusTotal", "status": "flagged", "detail": detail}
    if malicious + suspicious >= 1:
        return [Signal("vt_suspicious", f"VirusTotal : {malicious + suspicious} moteur(s) signalent ce lien.", 25, detail, "reputation")], {"name": "VirusTotal", "status": "flagged", "detail": detail}
    return [], {"name": "VirusTotal", "status": "ok", "detail": detail}
