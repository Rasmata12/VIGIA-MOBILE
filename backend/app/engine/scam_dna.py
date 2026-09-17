"""SCAM DNA — traduction des signaux bruts en categories de fraude explicables.

Chaque trait n'est produit que si un signal a REELLEMENT ete observe dans le
contenu soumis. Aucun trait n'est ajoute pour "remplir" un profil.
"""
from __future__ import annotations

from dataclasses import asdict, dataclass

from app.engine.common import Signal

# categorie -> (libelle humain, codes de signaux declencheurs)
CATEGORIES: dict[str, tuple[str, set[str]]] = {
    "URGENCY": ("Langage d'urgence destine a empecher la reflexion", {"urgence", "exclamations", "shouting"}),
    "THREAT": ("Menace de blocage, de sanction ou de perte", {"menace"}),
    "PAYMENT": ("Demande de paiement ou de transfert d'argent", {"paiement"}),
    "CREDENTIAL_REQUEST": ("Demande d'identifiants, de code ou de mot de passe", {"identifiants"}),
    "ACCOUNT_TAKEOVER": ("Tentative de prise de controle d'un compte", {"identifiants", "menace", "sensitive_path", "final_sensitive_path"}),
    "IMPERSONATION": ("Usurpation d'une marque ou d'une autorite", {"autorite", "brand_mention", "typosquat", "brand_in_subdomain", "punycode", "homoglyph"}),
    "REWARD": ("Promesse de gain, cadeau ou remboursement inattendu", {"recompense", "romance_emploi"}),
    "SOCIAL_ENGINEERING": ("Manipulation relationnelle (secret, isolement, confiance)", {"secret", "generic_greeting", "romance_emploi"}),
    "SUSPICIOUS_LINK": ("Lien presentant des caracteristiques suspectes", {
        "url_dangerous", "url_suspicious", "shortener", "ip_host", "userinfo", "risky_tld",
        "free_hosting", "random_domain", "dangerous_file", "cross_domain_redirect", "redirect_chain",
        "gsb_match", "vt_malicious", "vt_suspicious", "encoded", "many_subdomains",
    }),
    "FINANCIAL_PRESSURE": ("Pression financiere (frais, delai, penalite)", {"paiement", "livraison", "menace"}),
    "CHANNEL_SWITCH": ("Invitation a quitter le canal officiel", {"contact_hors_canal", "phone_contact", "hors_canal"}),
    "DATA_HARVESTING": ("Collecte de donnees personnelles sensibles", {"donnees_perso"}),
    "FRAUDULENT_OFFER": ("Offre d'emploi, de formation ou d'annonce presentant des signaux de fraude", {
        "frais_avant_embauche", "embauche_sans_verification", "annonce_sans_visite",
        "annonce_prix_trop_bas", "acompte_reservation",
    }),
    "COMMUNITY_REPORTED": ("Deja signale par d'autres utilisateurs de la communaute VIGIA", {"community_flagged"}),
}

# Les categories composites n'apparaissent que si PLUSIEURS de leurs declencheurs sont presents.
COMPOSITE = {"ACCOUNT_TAKEOVER": 2, "FINANCIAL_PRESSURE": 2}


@dataclass
class Trait:
    category: str
    label: str
    evidence: list[str]
    strength: int  # 0-100, derive du poids des signaux reellement observes

    def dict(self) -> dict:
        return asdict(self)


def profile(signals: list[Signal]) -> list[Trait]:
    codes = {s.code: s for s in signals}
    traits: list[Trait] = []
    for category, (label, triggers) in CATEGORIES.items():
        matched = [codes[c] for c in triggers if c in codes]
        if not matched:
            continue
        if len(matched) < COMPOSITE.get(category, 1):
            continue
        weight = sum(max(s.weight, 0) for s in matched)
        traits.append(Trait(
            category=category,
            label=label,
            evidence=[s.label for s in matched][:4],
            strength=min(100, weight),
        ))
    return sorted(traits, key=lambda t: -t.strength)
