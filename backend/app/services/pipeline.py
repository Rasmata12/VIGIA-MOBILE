"""Pipeline d'analyse unique, partage par tous les modules.

INPUT -> VALIDATION -> NORMALISATION -> EXTRACTION -> URL ENGINE / TEXT ENGINE
      -> SCAM DNA -> AI ENGINE -> CORRELATION (MOMENT) -> RISK ENGINE
      -> EXPLICATION -> PERSISTANCE (analyse + evenement + alerte)

Chaque etape est reelle. Si une etape ne peut pas s'executer (pas de cle IA,
pas de reseau), elle est marquee comme telle dans les sources ; rien n'est invente.
"""
from __future__ import annotations

import hashlib
import json
import logging
import time

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.engine import ai as ai_engine
from app.engine import community, moment
from app.engine.common import Signal
from app.engine.risk import RiskAssessment, assess
from app.engine.text_engine import analyse_text
from app.engine.url_engine import ENGINE_VERSION, analyse_url, extract_urls
from app.models import Alert, Analysis, Event, User

logger = logging.getLogger("vigia.analysis")

VALID_KINDS = {"url", "text"}
VALID_MODULES = {"verify", "guard", "qr", "share", "before_pay", "job_offer", "listing"}


def _preview(content: str, store_full: bool, kind: str) -> str:
    if store_full:
        return content[:300]
    if kind == "url":
        return content[:200]
    # minimisation : on ne conserve qu'un extrait court d'un message
    return (content[:120] + "…") if len(content) > 120 else content


async def run_pipeline(
    db: Session,
    user: User,
    kind: str,
    content: str,
    module: str = "verify",
    online: bool = True,
    use_ai: bool = True,
    package_name: str = "",
    extra_context: str = "",
) -> tuple[Analysis, RiskAssessment, bool]:
    """Retourne (analyse persistee, evaluation, alerte_creee)."""
    if kind not in VALID_KINDS:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Type d'analyse inconnu.")
    if module not in VALID_MODULES:
        module = "verify"

    content = (content or "").strip()
    if not content:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Le contenu a analyser est vide.")
    if len(content) > 20000:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Contenu trop long (20 000 caracteres maximum).")

    started = time.perf_counter()
    payload = content if not extra_context else f"{content}\n\n[Contexte fourni par l'utilisateur] {extra_context}"

    try:
        if kind == "url":
            result = await analyse_url(content, online=online)
        else:
            result = await analyse_text(payload, online=online)
    except ValueError as exc:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, str(exc))

    # Espace communautaire : si d'autres utilisateurs ont deja signale un domaine ou un numero
    # present dans ce contenu, on l'integre au score AVANT l'IA (qui verra donc aussi ce signal).
    community_targets = community.extract_targets_from_content(kind, content, result.extracted)
    community_counts = community.report_counts(db, community_targets)
    for (target_type, target_key), n in community_counts.items():
        label_type = "domaine" if target_type == "domain" else "numero"
        plural = "s" if n > 1 else ""
        result.signals.append(Signal(
            "community_flagged",
            f"{n} personne{plural} de la communaute VIGIA {'ont' if n > 1 else 'a'} deja signale "
            f"le {label_type} '{target_key}' comme suspect ou frauduleux.",
            community.weight_for_reports(n), target_key, "communaute",
        ))
    if community_counts:
        from app.engine.common import clamp_score, level_from_score
        result.score = clamp_score(sum(s.weight for s in result.signals))
        result.level = level_from_score(result.score)
        result.sources.append({
            "name": "Signalements communautaires", "status": "ok",
            "detail": f"{sum(community_counts.values())} signalement(s) distinct(s) trouve(s) sur {len(community_counts)} cible(s)",
        })

    settings_row = user.settings
    ai_allowed = use_ai and online and (settings_row.ai_enabled if settings_row else True)
    if ai_allowed:
        result = await ai_engine.enrich(kind, payload, result)

    # SCAM DNA d'abord (necessaire a la correlation), puis correlation, puis risk engine
    from app.engine.scam_dna import profile

    dna_now = [t.category for t in profile(result.signals)]
    correlation_signals, related_ids = moment.correlate(db, user.id, dna_now)
    assessment = assess(result, correlation=correlation_signals)

    duration_ms = int((time.perf_counter() - started) * 1000)
    store_full = bool(settings_row.store_full_input) if settings_row else False
    if module == "guard" and settings_row and not settings_row.guard_store_content:
        store_full = False

    record = Analysis(
        user_id=user.id,
        kind=kind,
        input_preview=_preview(content, store_full, kind),
        input_sha256=hashlib.sha256(content.encode()).hexdigest(),
        score=assessment.score,
        level=assessment.level,
        summary=assessment.summary,
        signals_json=json.dumps(assessment.signals, ensure_ascii=False),
        sources_json=json.dumps(assessment.sources, ensure_ascii=False),
        ai_used=assessment.ai_used,
        confidence=assessment.confidence,
        module=module,
        assessment_json=json.dumps(assessment.dict(), ensure_ascii=False),
        engine_version=ENGINE_VERSION,
        duration_ms=duration_ms,
    )
    db.add(record)
    db.flush()

    event = Event(
        user_id=user.id,
        analysis_id=record.id,
        source=module,
        type="url" if kind == "url" else "message",
        package_name=package_name[:120],
        signals_json=json.dumps([s for s in assessment.signals if s.get("weight", 0) > 0], ensure_ascii=False),
        dna_json=json.dumps(assessment.scam_dna, ensure_ascii=False),
        content_hash=record.input_sha256,
        risk_score=assessment.score,
        risk_level=assessment.level,
        related_ids_json=json.dumps(related_ids),
    )
    db.add(event)

    alert_created = False
    notifications_on = settings_row.notifications_enabled if settings_row else True
    if assessment.level in {"dangerous", "suspicious"} and notifications_on:
        db.add(Alert(
            user_id=user.id,
            analysis_id=record.id,
            title="Menace detectee" if assessment.level == "dangerous" else "Contenu suspect detecte",
            body=assessment.summary[:500],
            level=assessment.level,
            source=module,
            reason=" | ".join(assessment.evidence[:3]),
            recommended_action=assessment.recommendation[0] if assessment.recommendation else "",
        ))
        alert_created = True

    db.commit()
    db.refresh(record)
    # journalisation technique SANS contenu ni jeton
    logger.info(
        "ANALYSIS module=%s kind=%s level=%s score=%s confidence=%s ai=%s ms=%s user=%s",
        module, kind, assessment.level, assessment.score, assessment.confidence,
        assessment.ai_used, duration_ms, user.id[:8],
    )
    return record, assessment, alert_created


def extracted_urls(text: str) -> list[str]:
    return extract_urls(text)
