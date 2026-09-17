"""VIGIA MOMENT — correlation d'evenements REELS.

Ne relie que des evenements effectivement enregistres pour cet utilisateur,
dans une fenetre de temps courte. Aucun evenement n'est fabrique.
"""
from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone

from sqlalchemy.orm import Session

from app.engine.common import Signal
from app.models import Event

WINDOW = timedelta(hours=6)          # fenetre de correlation
EVENT_TTL_DAYS = 14                  # expiration des evenements

# Combinaisons de categories SCAM DNA qui, observees sur des evenements distincts,
# elevent reellement le risque.
COMBOS: list[tuple[set[str], int, str]] = [
    ({"CREDENTIAL_REQUEST", "SUSPICIOUS_LINK"}, 18,
     "Tu as recu, dans un court intervalle, une demande d'identifiants ET un lien suspect : "
     "sequence typique d'une campagne d'hameconnage ciblee."),
    ({"PAYMENT", "URGENCY"}, 15,
     "Une demande de paiement et un message d'urgence ont ete detectes coup sur coup."),
    ({"IMPERSONATION", "PAYMENT"}, 20,
     "Une usurpation de marque puis une demande de paiement ont ete detectees : "
     "scenario classique de fraude au faux service."),
    ({"REWARD", "PAYMENT"}, 18,
     "Promesse de gain suivie d'une demande de frais : arnaque a l'avance de frais."),
    ({"CHANNEL_SWITCH", "CREDENTIAL_REQUEST"}, 14,
     "Invitation a changer de canal puis demande d'informations sensibles."),
]


def recent_events(db: Session, user_id: str, exclude_id: str | None = None) -> list[Event]:
    since = datetime.now(timezone.utc) - WINDOW
    query = db.query(Event).filter(Event.user_id == user_id, Event.created_at >= since)
    if exclude_id:
        query = query.filter(Event.id != exclude_id)
    return query.order_by(Event.created_at.desc()).limit(25).all()


def correlate(db: Session, user_id: str, current_dna: list[str]) -> tuple[list[Signal], list[str]]:
    """Retourne (signaux de correlation, ids des evenements relies)."""
    others = recent_events(db, user_id)
    if not others:
        return [], []

    previous: dict[str, str] = {}   # categorie -> event id
    for event in others:
        for trait in json.loads(event.dna_json or "[]"):
            previous.setdefault(trait.get("category", ""), event.id)

    signals: list[Signal] = []
    related: set[str] = set()
    current = set(current_dna)

    for combo, weight, explanation in COMBOS:
        in_current = combo & current
        in_previous = {c for c in combo if c in previous}
        # la combinaison doit etre repartie sur au moins deux evenements distincts
        if in_current and (combo - in_current) & in_previous:
            signals.append(Signal(
                code="moment_" + "_".join(sorted(combo)).lower(),
                label=explanation,
                weight=weight,
                evidence=f"{len(others)} evenement(s) recent(s) analyses",
                category="correlation",
            ))
            related.update(previous[c] for c in (combo - in_current) if c in previous)

    return signals, sorted(related)


def purge_expired(db: Session) -> int:
    now = datetime.now(timezone.utc)
    cutoff = now - timedelta(days=EVENT_TTL_DAYS)
    deleted = db.query(Event).filter(Event.created_at < cutoff).delete(synchronize_session=False)
    db.commit()
    return int(deleted or 0)
