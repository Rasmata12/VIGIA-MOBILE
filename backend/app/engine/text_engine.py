from __future__ import annotations

import re

from app.engine.common import EngineResult, Signal, clamp_score, level_from_score
from app.engine.lists import BRANDS
from app.engine.url_engine import analyse_url, extract_urls

ENGINE_VERSION = "1.0.0"

# (code, libelle, poids, categorie, motifs) - FR + EN + variantes africaines
PATTERNS: list[tuple[str, str, int, str, list[str]]] = [
    ("urgence", "Le message cree un sentiment d'urgence pour t'empecher de reflechir.", 16, "manipulation",
     [r"\burgent\b", r"imm[ée]diatement", r"sous \d+\s*(h|heures|minutes)", r"derni[eè]re? (chance|avertissement|rappel)",
      r"avant (ce soir|demain|minuit|\d{1,2}h)", r"\bexpire\b", r"d[ée]lai d[ée]pass", r"agir maintenant",
      r"\bimmediately\b", r"\bwithin 24 hours\b", r"\basap\b"]),
    ("menace", "Le message menace de suspendre, bloquer ou supprimer quelque chose.", 20, "manipulation",
     [r"suspend(u|re|ue|ed)", r"bloqu[ée]", r"d[ée]sactiv[ée]", r"supprim[ée]", r"restrein", r"poursuite",
      r"amende", r"sanction", r"votre compte sera", r"account will be (closed|suspended|locked)", r"\blocked\b"]),
    ("identifiants", "Le message demande des identifiants ou un mot de passe.", 34, "vol_de_donnees",
     [r"mot de passe", r"identifiant", r"vos identifiants", r"\bpassword\b", r"\blogin\b", r"code (secret|pin|confidentiel)",
      r"\bpin\b", r"code de v[ée]rification", r"code re[çc]u par sms", r"\botp\b", r"\bcvv\b", r"\bcvc\b",
      r"num[ée]ro de carte", r"code a 6 chiffres", r"phrase de r[ée]cup[ée]ration", r"seed phrase", r"cl[ée] priv[ée]"]),
    ("donnees_perso", "Le message reclame des donnees personnelles sensibles.", 18, "vol_de_donnees",
     [r"num[ée]ro de (s[ée]curit[ée] sociale|piece|cni|passeport)", r"date de naissance", r"copie de votre (cni|piece)",
      r"selfie avec", r"\bibanc?\b", r"\biban\b", r"\brib\b", r"social security number"]),
    ("paiement", "Le message demande un paiement, un transfert ou des frais.", 24, "arnaque_financiere",
     [r"payer? des frais", r"frais de (dossier|livraison|douane|activation|deblocage)", r"virement",
      r"transf[ée]rez?", r"envoyez? (\d|de l'argent|la somme)", r"mobile money", r"orange money", r"moov money",
      r"\bmtn momo\b", r"\bwave\b", r"recharge", r"carte pr[ée]pay[ée]e", r"coupon", r"gift ?card", r"bitcoin",
      r"\busdt\b", r"crypto", r"western union", r"moneygram"]),
    ("recompense", "Le message promet un gain, un cadeau ou un remboursement inattendu.", 22, "appat",
     [r"f[ée]licitations", r"vous avez gagn", r"\bgagnant\b", r"\blot\b", r"tirage", r"loterie", r"cadeau",
      r"remboursement", r"vous avez re[çc]u \d", r"prime", r"bonus exceptionnel", r"you (have )?won", r"\bprize\b",
      r"\bclaim your\b", r"h[ée]ritage", r"investissement garanti", r"rendement de \d+ ?%"]),
    ("lien_action", "Le message pousse a cliquer sur un lien pour resoudre un probleme.", 12, "manipulation",
     [r"cliquez? (ici|sur ce lien)", r"suivez? ce lien", r"click here", r"acc[ée]dez? a votre compte",
      r"confirmez? (votre|vos)", r"v[ée]rifiez? (votre|vos)", r"mettre a jour vos (informations|coordonn[ée]es)"]),
    ("secret", "Le message demande de garder le secret ou de ne pas verifier ailleurs.", 26, "manipulation",
     [r"ne (dites|parlez|en parlez) (rien|a personne)", r"restez? discret", r"confidentiel", r"entre nous",
      r"ne contactez pas", r"sans en parler", r"don'?t tell (anyone|anybody)"]),
    ("autorite", "Le message se fait passer pour une autorite ou un service officiel.", 14, "usurpation",
     [r"service (client|fiscal|des imp[oô]ts)", r"police", r"gendarmerie", r"minist[eè]re", r"banque centrale",
      r"\bdouane\b", r"tribunal", r"huissier", r"votre banque", r"support technique", r"service de s[ée]curit[ée]"]),
    ("livraison", "Le message evoque un colis bloque, motif de phishing tres courant.", 16, "appat",
     [r"colis (en attente|bloqu[ée]|non livr[ée])", r"frais de livraison", r"votre (commande|livraison) (est|a [ée]t[ée])",
      r"\bdhl\b", r"\bdouanes?\b", r"package (is )?(pending|waiting)"]),
    ("romance_emploi", "Le message correspond a une arnaque a l'emploi ou sentimentale.", 18, "appat",
     [r"offre d'emploi", r"travail a domicile", r"gagnez? \d+ ?(f|fcfa|€|\$) par (jour|semaine)",
      r"recrutement urgent", r"\bwhatsapp\b.{0,30}\+\d{6,}", r"je t'aime", r"mon ch[ée]ri"]),
    ("frais_avant_embauche",
     "Le message demande un paiement avant l'embauche ou le debut de la formation : "
     "aucun employeur ou centre de formation legitime ne fait jamais payer pour recruter.",
     44, "arnaque_emploi",
     [r"payer.{0,15}avant.{0,15}(embauche|recrut|formation|poste)",
      r"frais.{0,15}(avant|pour).{0,10}(embauche|recrut|formation)",
      r"kit de formation (obligatoire|payant)", r"frais de dossier pour le poste",
      r"d[ée]p[oô]t de garantie pour le poste", r"frais de visa (de travail|professionnel)",
      r"mat[ée]riel a acheter (vous[- ]m[êe]me|vous m[êe]me)", r"avance sur salaire a (verser|payer)",
      r"frais d'inscription non remboursable"]),
    ("embauche_sans_verification",
     "Le message promet une embauche sans entretien ni verification, procede rare pour un vrai recrutement.",
     20, "arnaque_emploi",
     [r"sans entretien", r"aucun entretien (requis|necessaire)", r"embauche imm[ée]diate sans cv",
      r"aucune exp[ée]rience requise.{0,40}(urgent|imm[ée]diat)"]),
    ("annonce_sans_visite",
     "L'annonce ecarte toute visite ou verification physique avant paiement, motif classique des faux logements/objets.",
     36, "arnaque_annonce",
     [r"(je suis|actuellement|d[ée]j[aà]) (a l'etranger|en mission|au front|militaire d[ée]ploy[ée])",
      r"pas (possible|disponible) de visiter avant", r"sans visite pr[ée]alable",
      r"cl[ée]s? (envoy[ée]es?|remises?) apr[ée]s paiement", r"paiement avant la visite",
      r"r[ée]servation par (western union|moneygram|mandat cash)"]),
    ("annonce_prix_trop_bas",
     "Le prix annonce est presente comme exceptionnellement bas pour justifier une urgence a payer.",
     14, "arnaque_annonce",
     [r"prix imbattable", r"vente urgente.{0,20}(depart|deces|divorce)", r"en dessous du prix du march[ée]",
      r"[ée]tat neuf.{0,30}(moiti[ée] prix|tres bas prix)"]),
    ("acompte_reservation",
     "L'annonce demande un acompte ou des arrhes avant toute rencontre ou remise du bien.",
     30, "arnaque_annonce",
     [r"acompte (obligatoire|exig[ée]|pour r[ée]server)", r"arrhes? (obligatoires?|pour bloquer)",
      r"verser (un acompte|des arrhes) avant", r"frais de r[ée]servation non remboursable"]),
    ("contact_hors_canal", "Le message renvoie vers WhatsApp ou Telegram, hors des canaux officiels.", 12, "manipulation",
     [r"contactez?[- ]nous sur whatsapp", r"[ée]crivez? sur telegram", r"\bt\.me/", r"\bwa\.me/", r"whatsapp\s*:\s*\+?\d"]),
]

SENSITIVE_ANSWER = re.compile(r"\b\d{4,8}\b")


def _matches(text: str) -> list[Signal]:
    lowered = text.lower()
    signals: list[Signal] = []
    for code, label, weight, category, regexes in PATTERNS:
        hits = []
        for rx in regexes:
            found = re.search(rx, lowered)
            if found:
                hits.append(found.group(0))
        if hits:
            bonus = min(len(hits) - 1, 2) * 3
            signals.append(Signal(code, label, weight + bonus, ", ".join(dict.fromkeys(hits))[:160], category))
    return signals


def _style_signals(text: str) -> list[Signal]:
    signals: list[Signal] = []
    letters = [c for c in text if c.isalpha()]
    if len(letters) >= 40:
        upper_ratio = sum(1 for c in letters if c.isupper()) / len(letters)
        if upper_ratio > 0.45:
            signals.append(Signal("shouting", "Le message est majoritairement en majuscules, procede typique des messages d'arnaque.", 8, category="style"))
    if text.count("!") >= 4:
        signals.append(Signal("exclamations", "Ponctuation excessive, signe d'un message concu pour faire reagir vite.", 6, category="style"))
    if re.search(r"(cher|bonjour)\s+(client|utilisateur|monsieur/madame|user)", text.lower()):
        signals.append(Signal("generic_greeting", "Le message utilise une formule impersonnelle alors qu'un vrai service connait ton nom.", 10, category="style"))
    brands = sorted({b for b in BRANDS if len(b) >= 5 and re.search(rf"\b{re.escape(b)}\b", text.lower())})
    if brands:
        signals.append(Signal("brand_mention", f"Le message se reclame de : {', '.join(brands[:4])}. Verifie toujours via l'application officielle, jamais via le lien du message.", 8, ", ".join(brands[:6]), "usurpation"))
    phones = re.findall(r"\+\d{8,15}", text)
    if phones:
        signals.append(Signal("phone_contact", "Le message fournit un numero de telephone de contact direct.", 6, ", ".join(phones[:3]), "contact"))
    return signals


async def analyse_text(text: str, online: bool = True) -> EngineResult:
    content = (text or "").strip()
    if len(content) < 5:
        raise ValueError("Le message est trop court pour etre analyse.")
    if len(content) > 20000:
        raise ValueError("Le message depasse la taille maximale (20 000 caracteres).")

    signals = _matches(content) + _style_signals(content)
    sources = [{"name": "Heuristiques VIGIA (texte)", "status": "ok", "detail": f"v{ENGINE_VERSION}"}]
    urls = extract_urls(content)
    url_reports: list[dict] = []

    for url in urls[:3]:
        try:
            report = await analyse_url(url, online=online)
        except ValueError:
            continue
        url_reports.append({"url": url, "score": report.score, "level": report.level, "summary": report.summary})
        sources.extend(report.sources)
        weight = {"dangerous": 40, "suspicious": 20, "safe": 0}[report.level]
        if weight:
            signals.append(Signal(f"url_{report.level}", f"Le lien {url} a ete analyse et classe {report.level}. {report.summary}", weight, url[:150], "lien"))
        else:
            signals.append(Signal("url_ok", f"Le lien {url} n'a revele aucun signal majeur.", 0, url[:150], "lien"))

    if urls and not any(s.code.startswith("url_") for s in signals):
        signals.append(Signal("url_present", "Le message contient un lien qui n'a pas pu etre analyse.", 8, category="lien"))

    combos = {s.category for s in signals}
    if {"vol_de_donnees", "manipulation"} <= combos:
        signals.append(Signal("combo_phishing", "Combinaison caracteristique d'hameconnage : pression psychologique + demande d'identifiants.", 18, category="synthese"))
    if {"arnaque_financiere", "appat"} <= combos:
        signals.append(Signal("combo_advance_fee", "Combinaison caracteristique de l'arnaque a l'avance de frais : gain promis + paiement demande.", 18, category="synthese"))
    if {"arnaque_annonce"} & combos and "manipulation" in combos:
        signals.append(Signal("combo_fake_listing", "Combinaison caracteristique d'une fausse annonce : impossibilite de verifier le bien + pression pour payer vite.", 16, category="synthese"))

    score = clamp_score(sum(s.weight for s in signals))
    result = EngineResult(score=score, level=level_from_score(score), signals=signals, sources=sources,
                          extracted={"urls": urls, "url_reports": url_reports, "length": len(content)})
    result.summary = _summary(result)
    return result


def _summary(result: EngineResult) -> str:
    top = sorted([s for s in result.signals if s.weight > 0], key=lambda s: -s.weight)[:3]
    if result.level == "dangerous":
        head = "Ce message presente les caracteristiques d'une tentative d'arnaque. Ne reponds pas, ne clique pas, ne paie rien et ne communique aucun code."
    elif result.level == "suspicious":
        head = "Ce message contient des signaux suspects. Verifie l'expediteur par un canal officiel avant d'agir."
    else:
        head = "Aucun signal d'arnaque marquant n'a ete detecte dans ce message. Reste prudent si on te demande de l'argent ou un code."
    if top:
        head += " Motifs releves : " + " ".join(s.label for s in top)
    return head
