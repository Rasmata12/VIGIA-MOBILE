from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import User
from app.schemas import ListingIn, ListingOut
from app.security import current_user, rate_limit
from app.services.pipeline import run_pipeline

router = APIRouter(prefix="/listing", tags=["listing"])

CHECKLIST = [
    "Ne verse jamais d'acompte, d'arrhes ou de frais de reservation avant d'avoir vu le bien "
    "en personne (ou, pour un objet envoye, avant d'utiliser un vrai service sequestre reconnu).",
    "Mefie-toi d'un vendeur qui refuse tout appel telephonique ou toute visite, ou qui pretend "
    "etre a l'etranger/en mission au moment cle.",
    "Un prix nettement en dessous du marche pour un bien en bon etat est le signe le plus frequent "
    "d'une fausse annonce.",
    "Paye uniquement via un moyen tracable et reversible ; evite mandat cash, Western Union/MoneyGram "
    "ou crypto pour un achat entre particuliers.",
    "Verifie l'historique du vendeur (anciennes annonces, avis) avant tout versement.",
]


@router.post("", response_model=ListingOut, status_code=201)
async def analyse_listing(
    payload: ListingIn,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
) -> ListingOut:
    rate_limit(db, f"listing:{user.id}", limit=60, window_seconds=3600)

    if not payload.content.strip():
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Fournis au moins le texte de l'annonce.")

    red_flags: list[str] = []
    context_parts: list[str] = [f"Categorie d'annonce : {payload.category}"]

    if payload.price_asked.strip():
        context_parts.append(f"Prix demande : {payload.price_asked.strip()}")

    if payload.seller_contact.strip():
        context_parts.append(f"Contact du vendeur : {payload.seller_contact.strip()}")

    if payload.can_visit_in_person is False:
        red_flags.append("Le vendeur indique lui-meme qu'une visite ou une rencontre en personne n'est pas possible.")
        context_parts.append(
            "Le vendeur precise qu'il n'est pas possible de visiter le bien ou de le voir en personne avant paiement."
        )

    if payload.deposit_requested.strip():
        red_flags.append(
            f"Un acompte ou des arrhes sont demandes avant toute visite ou remise du bien "
            f"({payload.deposit_requested.strip()}) : signal classique de fausse annonce."
        )
        context_parts.append(
            "L'utilisateur indique qu'on lui demande de verser un acompte ou des arrhes avant la visite "
            f"ou la remise du bien (montant ou nature indiques : {payload.deposit_requested.strip()})."
        )

    record, assessment, _ = await run_pipeline(
        db, user, "text", payload.content.strip(), module="listing",
        online=payload.online, use_ai=payload.use_ai,
        extra_context=" ; ".join(context_parts),
    )

    decision = {"dangerous": "stop", "suspicious": "verifier", "safe": "prudence"}[assessment.level]
    headline = {
        "stop": "Ne paie rien : cette annonce presente des signaux forts de fraude.",
        "verifier": "Ne t'engage pas tout de suite : verifie d'abord les points ci-dessous.",
        "prudence": "Aucun signal fort detecte, mais VIGIA ne peut pas garantir la legitimite d'une annonce : "
                    "applique quand meme la checklist avant de payer.",
    }[decision]

    return ListingOut(
        analysis_id=record.id, decision=decision, headline=headline,
        checklist=CHECKLIST, red_flags=red_flags, assessment=assessment.dict(),
    )
