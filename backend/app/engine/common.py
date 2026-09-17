from __future__ import annotations

import math
from dataclasses import dataclass, field, asdict


@dataclass
class Signal:
    code: str
    label: str          # explication lisible en francais
    weight: int         # contribution au score de risque (negatif = rassurant)
    evidence: str = ""
    category: str = "general"

    def dict(self) -> dict:
        return asdict(self)


@dataclass
class EngineResult:
    score: int = 0
    level: str = "safe"
    summary: str = ""
    signals: list[Signal] = field(default_factory=list)
    sources: list[dict] = field(default_factory=list)
    ai_used: bool = False
    extracted: dict = field(default_factory=dict)


def level_from_score(score: int) -> str:
    if score >= 70:
        return "dangerous"
    if score >= 35:
        return "suspicious"
    return "safe"


def clamp_score(raw: int) -> int:
    return max(0, min(100, raw))


def shannon_entropy(value: str) -> float:
    if not value:
        return 0.0
    counts: dict[str, int] = {}
    for char in value:
        counts[char] = counts.get(char, 0) + 1
    total = len(value)
    return -sum((c / total) * math.log2(c / total) for c in counts.values())


def levenshtein(a: str, b: str) -> int:
    if a == b:
        return 0
    if not a:
        return len(b)
    if not b:
        return len(a)
    previous = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        current = [i]
        for j, cb in enumerate(b, 1):
            current.append(min(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + (ca != cb)))
        previous = current
    return previous[-1]
