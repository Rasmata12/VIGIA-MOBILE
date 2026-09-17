from __future__ import annotations

import re

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import User
from app.schemas import BeforePayIn, BeforePayOut
from app.security import current_user, rate_limit
from app.services.pipeline import run_pipeline

router = APIRouter(prefix="/before-pay", tags=["before-pay"])

# VIGIA ne demande JAMAIS ces elements. Si l'utilisateur en colle un, on refuse l'analyse
# et on l'avertit plutot que de stocker une donnee bancaire.
FORBIDDEN_PATTERNS = [
    (re.compile(r"\b\d{13,19}\b"), "un numero de carte bancaire"),
    (re.compile(r"\bcvv\s*[:=]?\s*\d{3,4}\b", re.I), "un cryptogramme de carte"),
    (re.compile(r"\b(mon|le)\s+(code\s+)?pin\s*[:=]?\s*\d{4,8}\b", re.I), "un code PIN"),
]

CHECKLIST = [
    "Appelle le beneficiaire sur un numero que TU connais deja, pas celui du message.",
    "Verifie le nom exact affiche par l'operateur avant de valider le transfert.",
    "Aucune administration, banque ou operateur ne demande un code recu par SMS.",
    "Un paiement urgent ou secret est presque toujours une arnaque : prends 10 minutes.",
    "En cas de doute, fais relire le message a une autre personne avant de payer.",
]


@router.post("", response_model=BeforePayOut, status_code=201)
async def before_pay(
    payload: BeforePayIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> BeforePayOut:
    rate_limit(db, f"beforepay:{user.id}", limit=60, window_seconds=3600)
    blob = " ".join(filter(None, [payload.message, payload.url, payload.beneficiary, payload.context]))

    for pattern, what in FORBIDDEN_PATTERNS:
        if pattern.search(blob):
            raise HTTPException(
                status.HTTP_400_BAD_REQUEST,
                f"Ta saisie semble contenir {what}. VIGIA ne demande jamais ces informations et ne les enregistre pas. "
                "Retire cette donnee et relance la verification.",
            )

    if not payload.message.strip() and not payload.url.strip():
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            "Fournis au moins le message recu ou le lien concerne.",
        )

    kind = "url" if (payload.url.strip() and not payload.message.strip()) else "text"
    content = payload.url.strip() if kind == "url" else payload.message.strip()
    context_parts = []
    if payload.beneficiary:
        context_parts.append(f"Beneficiaire indique : {payload.beneficiary}")
    if payload.amount:
        context_parts.append(f"Montant demande : {payload.amount}")
    if payload.context:
        context_parts.append(payload.context)
    if kind == "text" and payload.url.strip():
        content = f"{content}\n{payload.url.strip()}"

    record, assessment, _ = await run_pipeline(
        db, user, kind, content, module="before_pay",
        online=payload.online, use_ai=payload.use_ai,
        extra_context=" ; ".join(context_parts),
    )

    notes: list[str] = []
    if payload.beneficiary:
        beneficiary = payload.beneficiary.strip()
        if re.search(r"\+?\d[\d\s]{6,}", beneficiary):
            notes.append("Le beneficiaire est identifie par un numero de telephone : verifie le nom affiche par l'operateur avant de valider.")
        if len(beneficiary) < 4:
            notes.append("Le nom du beneficiaire est tres court ou incomplet.")
    if not payload.beneficiary:
        notes.append("Aucun beneficiaire renseigne : impossible de verifier la coherence du destinataire.")

    decision = {"dangerous": "stop", "suspicious": "verifier", "safe": "prudence"}[assessment.level]
    headline = {
        "stop": "Ne paie pas. Ce que tu as soumis presente des signaux forts de fraude.",
        "verifier": "Ne paie pas tout de suite. Verifie d'abord les points ci-dessous.",
        "prudence": "Aucun signal fort detecte. VIGIA ne peut pas garantir qu'un paiement est sur : applique quand meme la checklist.",
    }[decision]

    return BeforePayOut(
        analysis_id=record.id,
        decision=decision,
        headline=headline,
        checklist=CHECKLIST,
        assessment=assessment.dict(),
        beneficiary_notes=notes,
    )
