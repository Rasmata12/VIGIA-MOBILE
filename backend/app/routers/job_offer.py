from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.engine.lists import FREE_EMAIL_DOMAINS
from app.models import User
from app.schemas import JobOfferIn, JobOfferOut
from app.security import current_user, rate_limit
from app.services.pipeline import run_pipeline

router = APIRouter(prefix="/job-offer", tags=["job-offer"])

# Regle universelle du recrutement legitime, rappelee dans tous les cas.
CHECKLIST = [
    "Un employeur ou un centre de formation legitime ne fait JAMAIS payer pour recruter, "
    "que ce soit sous forme de frais de dossier, de kit, de materiel ou de depot de garantie.",
    "Verifie l'entreprise independamment : site officiel, existence legale (registre du commerce) si elle en a un, "
    "coordonnees publiques et traces d'activite coherentes. Ne te fie pas uniquement a la page qui t'a contacte.",
    "Une adresse professionnelle peut aider a verifier l'identite d'un employeur, mais Gmail, Outlook ou WhatsApp ne prouvent pas a eux seuls une fraude.",
    "Ne transmets jamais de copie de piece d'identite ou de coordonnees bancaires avant la signature "
    "d'un contrat officiel, sur un canal verifie.",
    "Mefie-toi des postes tres bien payes sans qualification ni entretien : c'est rarement legitime.",
]


@router.post("", response_model=JobOfferOut, status_code=201)
async def analyse_job_offer(
    payload: JobOfferIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> JobOfferOut:
    rate_limit(db, f"joboffer:{user.id}", limit=60, window_seconds=3600)

    if not payload.content.strip():
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            "Fournis le texte de l'offre reçue. Les autres champs sont facultatifs et servent seulement de contexte.",
        )

    red_flags: list[str] = []
    context_parts: list[str] = []

    if payload.company_name.strip():
        context_parts.append(f"Entreprise ou organisme indique : {payload.company_name.strip()}")

    if payload.contact_email.strip():
        email = payload.contact_email.strip().lower()
        domain = email.rsplit("@", 1)[-1] if "@" in email else ""
        context_parts.append(f"Email de contact du recruteur : {email}")
        if domain in FREE_EMAIL_DOMAINS:
            # Une adresse Gmail/Outlook n'est pas une preuve d'arnaque : de
            # petites entreprises, associations et recruteurs indépendants
            # peuvent légitimement l'utiliser. On la conserve comme point de
            # vérification, sans la présenter comme un signal majeur.
            context_parts.append(
                f"Point de vérification contextuel : le contact utilise une messagerie grand public ({domain}). "
                "Cela ne prouve pas une fraude et doit être corroboré par l'identité réelle de l'employeur."
            )

    if payload.salary_promised.strip():
        context_parts.append(f"Remuneration promise : {payload.salary_promised.strip()}")

    if payload.fee_requested.strip():
        red_flags.append(
            f"Un paiement est demande avant l'embauche ou la formation ({payload.fee_requested.strip()}) : "
            "c'est le signal le plus fiable d'une arnaque a l'emploi."
        )
        # Phrase construite pour etre detectee de maniere fiable par le moteur de texte,
        # quel que soit le contenu exact saisi par l'utilisateur dans fee_requested.
        context_parts.append(
            "L'utilisateur indique qu'on lui demande de payer avant d'etre embauche ou de commencer "
            f"la formation (montant ou nature indiques : {payload.fee_requested.strip()})."
        )

    content = payload.content.strip() or (payload.company_name.strip() or "Offre sans texte fourni.")

    record, assessment, _ = await run_pipeline(
        db, user, "text", content, module="job_offer",
        online=payload.online, use_ai=payload.use_ai,
        extra_context=" ; ".join(context_parts),
    )

    decision = {"dangerous": "stop", "suspicious": "verifier", "safe": "prudence"}[assessment.level]
    headline = {
        "stop": "Ne donne aucune information ni argent : cette offre presente des signaux forts d'arnaque.",
        "verifier": "Ne t'engage pas tout de suite : verifie d'abord les points ci-dessous.",
        "prudence": "Aucun signal fort detecte, mais VIGIA ne peut pas garantir la legitimite d'une offre : "
                    "verifie quand meme l'entreprise avant de t'engager.",
    }[decision]

    return JobOfferOut(
        analysis_id=record.id,
        decision=decision,
        headline=headline,
        checklist=CHECKLIST,
        red_flags=red_flags,
        assessment=assessment.dict(),
    )
