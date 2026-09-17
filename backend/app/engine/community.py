"""Espace communautaire : signalements crees par les utilisateurs, agreges pour
avertir les autres. Aucune donnee personnelle n'est partagee entre utilisateurs :
seule une cible normalisee (domaine ou numero) et un compteur de signalants
DISTINCTS sont exposes."""
from __future__ import annotations

import re
from urllib.parse import urlparse

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.engine.url_engine import extract_urls, normalize_url, registrable_domain
from app.models import CommunityReport

PHONE_RE = re.compile(r"\+\d{7,15}")

VALID_TARGET_TYPES = {"domain", "phone"}
VALID_CATEGORIES = {"emploi", "annonce", "paiement", "phishing", "autre"}


DOMAIN_LIKE_RE = re.compile(r"^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$", re.I)


def normalize_target(target_type: str, raw: str) -> str | None:
    raw = (raw or "").strip()
    if not raw:
        return None
    if target_type == "domain":
        if any(c.isspace() for c in raw):
            return None  # un domaine ne contient jamais d'espace : rejette les textes libres
        try:
            normalized = normalize_url(raw) if "://" not in raw and "." in raw else raw
            host = urlparse(normalized if "://" in normalized else "http://" + normalized).hostname or ""
        except ValueError:
            return None
        if not host or not DOMAIN_LIKE_RE.match(host):
            return None
        return registrable_domain(host) or None
    if target_type == "phone":
        digits = re.sub(r"[^\d+]", "", raw)
        return digits or None
    return None


def detect_target(raw: str) -> tuple[str, str] | None:
    """Devine si une saisie libre est un domaine/URL ou un numero de telephone."""
    raw = (raw or "").strip()
    if not raw:
        return None
    if PHONE_RE.fullmatch(raw) or re.fullmatch(r"[\d\s.+-]{7,20}", raw):
        key = normalize_target("phone", raw)
        return ("phone", key) if key else None
    key = normalize_target("domain", raw)
    return ("domain", key) if key else None


def extract_targets_from_content(kind: str, content: str, extracted: dict) -> list[tuple[str, str]]:
    """Deduit les cibles verifiables (domaines, numeros) presentes dans une analyse deja
    effectuee, pour verifier si la communaute les a deja signalees."""
    targets: list[tuple[str, str]] = []
    urls: list[str] = []
    if kind == "url":
        domain = extracted.get("domain")
        if domain:
            targets.append(("domain", domain))
    else:
        urls = extracted.get("urls") or extract_urls(content)
        for phone in PHONE_RE.findall(content):
            key = normalize_target("phone", phone)
            if key:
                targets.append(("phone", key))
    for url in urls:
        try:
            host = urlparse(normalize_url(url)).hostname or ""
        except ValueError:
            continue
        domain = registrable_domain(host)
        if domain:
            targets.append(("domain", domain))
    # deduplique en gardant l'ordre
    return list(dict.fromkeys(targets))


def report_counts(db: Session, targets: list[tuple[str, str]]) -> dict[tuple[str, str], int]:
    """Nombre d'utilisateurs DISTINCTS ayant signale chaque cible."""
    if not targets:
        return {}
    counts: dict[tuple[str, str], int] = {}
    for target_type, target_key in targets:
        n = (
            db.query(func.count(func.distinct(CommunityReport.user_id)))
            .filter(CommunityReport.target_type == target_type, CommunityReport.target_key == target_key)
            .scalar()
        ) or 0
        if n:
            counts[(target_type, target_key)] = n
    return counts


def weight_for_reports(n: int) -> int:
    """Poids ajoute au score de risque selon le nombre de signalants distincts.
    Un seul signalement pese peu (peut etre errone) ; plusieurs signalements
    independants sont un signal tres fort."""
    if n <= 0:
        return 0
    if n == 1:
        return 10
    if n == 2:
        return 24
    if n <= 4:
        return 38
    return 55


def record_report(
    db: Session, user_id: str, target_type: str, target_key: str,
    category: str, description: str, analysis_id: str | None,
) -> tuple[CommunityReport, bool]:
    """Enregistre un signalement. Idempotent par (utilisateur, cible) : si l'utilisateur
    a deja signale cette cible, on renvoie le signalement existant sans le dupliquer."""
    existing = (
        db.query(CommunityReport)
        .filter(
            CommunityReport.user_id == user_id,
            CommunityReport.target_type == target_type,
            CommunityReport.target_key == target_key,
        )
        .first()
    )
    if existing:
        return existing, False
    report = CommunityReport(
        user_id=user_id, target_type=target_type, target_key=target_key,
        category=category, description=description[:400], analysis_id=analysis_id,
    )
    db.add(report)
    db.commit()
    db.refresh(report)
    return report, True


def community_summary(db: Session, target_type: str, target_key: str) -> dict:
    rows = (
        db.query(CommunityReport.category, func.count(func.distinct(CommunityReport.user_id)))
        .filter(CommunityReport.target_type == target_type, CommunityReport.target_key == target_key)
        .group_by(CommunityReport.category)
        .all()
    )
    by_category = {category: count for category, count in rows}
    total = sum(by_category.values())
    return {
        "target_type": target_type,
        "target_key": target_key,
        "reporters": total,
        "by_category": by_category,
        "weight": weight_for_reports(total),
    }
