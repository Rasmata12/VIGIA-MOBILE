"""RISK ENGINE — agregation en couches, tracable.

Layer 1 : regles locales (texte)
Layer 2 : analyse d'URL (statique + reseau)
Layer 3 : threat intelligence externe (Safe Browsing, VirusTotal)
Layer 4 : correlation d'evenements (VIGIA Moment)
Layer 5 : IA generative (uniquement si une cle est configuree)
Layer 6 : agregation + confiance

Le score de risque et la confiance sont deux grandeurs distinctes :
une confiance elevee sur un contenu benin ne rend pas le contenu "sur",
elle indique seulement que l'evaluation repose sur des elements solides.
"""
from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone

from app.engine.common import EngineResult, Signal, clamp_score, level_from_score
from app.engine.scam_dna import Trait, profile

LAYER_OF_CATEGORY = {
    "manipulation": "L1_LOCAL_RULES",
    "vol_de_donnees": "L1_LOCAL_RULES",
    "arnaque_financiere": "L1_LOCAL_RULES",
    "appat": "L1_LOCAL_RULES",
    "usurpation": "L1_LOCAL_RULES",
    "style": "L1_LOCAL_RULES",
    "contact": "L1_LOCAL_RULES",
    "synthese": "L1_LOCAL_RULES",
    "local": "L1_LOCAL_RULES",
    "arnaque_emploi": "L1_LOCAL_RULES",
    "arnaque_annonce": "L1_LOCAL_RULES",
    "communaute": "L4_CORRELATION",
    "transport": "L2_URL",
    "domaine": "L2_URL",
    "obfuscation": "L2_URL",
    "hebergement": "L2_URL",
    "contenu": "L2_URL",
    "charge": "L2_URL",
    "forme": "L2_URL",
    "reseau": "L2_URL",
    "lien": "L2_URL",
    "reputation": "L3_THREAT_INTEL",
    "correlation": "L4_CORRELATION",
    "ia": "L5_AI",
    "recommandation": "L5_AI",
}

RECOMMENDATIONS = {
    "dangerous": [
        "N'ouvre aucun lien de ce contenu et ne saisis aucune information.",
        "Ne paie rien et ne communique aucun code recu par SMS.",
        "Si le message se reclame d'un service que tu utilises, contacte-le par son canal officiel (application ou numero au dos de la carte).",
        "Signale puis supprime le message.",
    ],
    "suspicious": [
        "Verifie l'expediteur par un canal officiel avant toute action.",
        "N'entre aucun identifiant depuis un lien recu par message.",
        "En cas de doute sur un paiement, attends et fais verifier par un proche.",
    ],
    "safe": [
        "Aucun signal majeur detecte, mais reste attentif : un message peut etre frauduleux sans declencher de signal connu.",
        "Ne communique jamais un code de verification, meme a quelqu'un qui semble legitime.",
    ],
}


@dataclass
class RiskAssessment:
    score: int
    level: str
    confidence: int                      # 0-100 : solidite de l'evaluation
    summary: str
    signals: list[dict] = field(default_factory=list)
    evidence: list[str] = field(default_factory=list)
    scam_dna: list[dict] = field(default_factory=list)
    layers: dict = field(default_factory=dict)
    sources: list[dict] = field(default_factory=list)
    technical: dict = field(default_factory=dict)
    recommendation: list[str] = field(default_factory=list)
    ai_used: bool = False
    timestamp: str = ""

    def dict(self) -> dict:
        return asdict(self)


def _layer_of(signal: Signal) -> str:
    return LAYER_OF_CATEGORY.get(signal.category, "L1_LOCAL_RULES")


def _confidence(result: EngineResult, traits: list[Trait], correlation_bonus: int) -> int:
    """La confiance augmente avec : le nombre de signaux independants, la presence
    de sources externes reellement consultees, et l'accord entre couches."""
    positive = [s for s in result.signals if s.weight > 0]
    distinct_layers = {_layer_of(s) for s in positive}
    external_ok = sum(
        1 for s in result.sources
        if s.get("name") in {"Google Safe Browsing", "VirusTotal"} and s.get("status") in {"ok", "flagged"}
    )
    network_ok = sum(1 for s in result.sources if s.get("name") in {"DNS", "TLS", "HTTP"} and s.get("status") == "ok")
    page_ok = sum(1 for s in result.sources if s.get("name") == "Page HTML" and s.get("status") == "ok")
    rdap_ok = sum(1 for s in result.sources if s.get("name") == "RDAP" and s.get("status") == "ok")

    base = 25
    base += min(len(positive), 6) * 4          # jusqu'a +24
    base += (len(distinct_layers) - 1) * 5 if distinct_layers else 0
    base += external_ok * 12
    base += min(network_ok, 3) * 3
    base += page_ok * 5
    base += rdap_ok * 5
    base += 5 if result.ai_used else 0
    base += 4 if correlation_bonus else 0
    if not positive and not network_ok and not external_ok and not page_ok and not rdap_ok:
        base = 32
    # Une confiance >= 90 n'est possible que si au moins une source de reputation
    # independante a reellement repondu. Ainsi, "90 %" ne devient pas un simple
    # emballage marketing autour de l'heuristique locale.
    if external_ok == 0:
        base = min(base, 84)
    return clamp_score(base)


def assess(
    result: EngineResult,
    correlation: list[Signal] | None = None,
) -> RiskAssessment:
    signals = list(result.signals)
    correlation = correlation or []
    signals.extend(correlation)

    # `result.score` already contains the fused heuristic/AI score. Re-summing
    # `result.signals` here would discard the AI contribution because AI
    # explanations intentionally have weight 0. Only add new correlation
    # signals at this stage.
    score = clamp_score(result.score + sum(s.weight for s in correlation))
    level = level_from_score(score)
    traits = profile(signals)

    layers: dict[str, dict] = {}
    for signal in signals:
        layer = _layer_of(signal)
        bucket = layers.setdefault(layer, {"contribution": 0, "signals": 0})
        bucket["contribution"] += signal.weight
        bucket["signals"] += 1
    for name in ("L1_LOCAL_RULES", "L2_URL", "L3_THREAT_INTEL", "L4_CORRELATION", "L5_AI"):
        layers.setdefault(name, {"contribution": 0, "signals": 0})
    layers["L6_AGGREGATION"] = {"contribution": score, "signals": len(signals)}
    if result.ai_used:
        layers["L5_AI"]["ai_score"] = result.extracted.get("ai", {}).get("score")

    evidence = [f"{s.label}" + (f" ({s.evidence})" if s.evidence else "")
                for s in sorted(signals, key=lambda x: -x.weight) if s.weight > 0][:6]

    return RiskAssessment(
        score=score,
        level=level,
        confidence=_confidence(result, traits, len(correlation)),
        summary=result.summary,
        signals=[s.dict() for s in signals],
        evidence=evidence,
        scam_dna=[t.dict() for t in traits],
        layers=layers,
        sources=result.sources,
        technical={k: v for k, v in result.extracted.items() if k not in {"visible_text_excerpt"}},
        recommendation=RECOMMENDATIONS[level],
        ai_used=result.ai_used,
        timestamp=datetime.now(timezone.utc).isoformat(),
    )
