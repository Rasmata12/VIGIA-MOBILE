from __future__ import annotations

import ipaddress
import re
import socket
import ssl
from datetime import datetime, timezone
from urllib.parse import unquote, urlparse

import httpx

from app.config import get_settings
from app.engine.common import EngineResult, Signal, clamp_score, level_from_score, levenshtein, shannon_entropy
from app.engine.lists import (
    BRANDS,
    DANGEROUS_EXTENSIONS,
    FREE_HOSTING,
    HIGH_RISK_TLDS,
    SENSITIVE_PATH_WORDS,
    SHORTENERS,
)

ENGINE_VERSION = "1.0.0"
_settings = get_settings()

URL_RE = re.compile(r"""(?:(?:https?|ftp)://|www\.)[^\s<>"'`\]\[{}]+""", re.IGNORECASE)
MULTI_TLD = {"co.uk", "com.br", "co.za", "com.au", "co.jp", "org.uk", "gov.uk", "ac.uk", "com.ng"}


def normalize_url(raw: str) -> str:
    value = raw.strip()
    if not value:
        raise ValueError("URL vide.")
    if not re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*://", value):
        value = "http://" + value
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"}:
        raise ValueError("Seuls les liens http et https peuvent etre analyses.")
    host = parsed.hostname
    if not host:
        raise ValueError("URL invalide : nom de domaine introuvable.")
    if " " in value or "\t" in value:
        raise ValueError("URL invalide : une adresse web ne contient pas d'espace.")
    is_ip = bool(re.match(r"^\d{1,3}(\.\d{1,3}){3}$", host))
    if not is_ip and host != "localhost":
        if "." not in host or not re.match(r"^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$", host, re.IGNORECASE):
            raise ValueError("URL invalide : le nom de domaine n'est pas valide.")
    return value


def registrable_domain(host: str) -> str:
    parts = host.lower().strip(".").split(".")
    if len(parts) <= 2:
        return ".".join(parts)
    last_two = ".".join(parts[-2:])
    if last_two in MULTI_TLD and len(parts) >= 3:
        return ".".join(parts[-3:])
    return last_two


def extract_urls(text: str) -> list[str]:
    found: list[str] = []
    for match in URL_RE.findall(text or ""):
        cleaned = match.rstrip(".,;:!?)»\"'")
        if cleaned not in found:
            found.append(cleaned)
    return found[:10]


def _static_signals(url: str) -> tuple[list[Signal], dict]:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower()
    domain = registrable_domain(host)
    tld = host.rsplit(".", 1)[-1] if "." in host else ""
    path = unquote(parsed.path or "")
    query = unquote(parsed.query or "")
    signals: list[Signal] = []
    add = signals.append

    if parsed.scheme == "https":
        add(Signal("https", "La connexion est chiffree (HTTPS).", -5, category="transport"))
    else:
        add(Signal("no_https", "Le lien n'est pas chiffre (HTTP) : les donnees saisies circulent en clair.", 18, category="transport"))

    is_ip = False
    try:
        ipaddress.ip_address(host)
        is_ip = True
    except ValueError:
        pass
    if is_ip:
        add(Signal("ip_host", "Le lien pointe vers une adresse IP brute au lieu d'un nom de domaine, ce qui est typique des pages frauduleuses temporaires.", 30, host, "domaine"))

    if "@" in (parsed.netloc or ""):
        add(Signal("userinfo", "Le lien contient un caractere '@' : la partie visible avant le '@' est ignoree par le navigateur et sert a masquer la vraie destination.", 35, parsed.netloc, "obfuscation"))

    if parsed.port and parsed.port not in (80, 443):
        add(Signal("odd_port", f"Le lien utilise un port inhabituel ({parsed.port}).", 12, str(parsed.port), "transport"))

    if host.startswith("xn--") or ".xn--" in host:
        add(Signal("punycode", "Le domaine utilise un encodage Punycode (xn--), technique frequente pour imiter visuellement un domaine legitime.", 32, host, "obfuscation"))

    labels = host.split(".")
    if len(labels) >= 5:
        add(Signal("many_subdomains", f"Le domaine empile {len(labels)} niveaux de sous-domaines, souvent pour noyer la vraie destination.", 14, host, "domaine"))

    if len(url) > 120:
        add(Signal("long_url", f"Le lien est anormalement long ({len(url)} caracteres).", 8, category="forme"))

    if domain in SHORTENERS:
        add(Signal("shortener", f"Le lien est raccourci via {domain} : la destination reelle est masquee tant qu'on ne l'ouvre pas.", 22, domain, "obfuscation"))

    if tld in HIGH_RISK_TLDS:
        add(Signal("risky_tld", f"L'extension .{tld} est parmi les plus utilisees pour l'hebergement de pages malveillantes.", 18, tld, "domaine"))

    for free in FREE_HOSTING:
        if host == free or host.endswith("." + free):
            add(Signal("free_hosting", f"La page est hebergee sur un service gratuit ({free}), frequemment utilise pour des pages ephemeres de phishing.", 16, free, "hebergement"))
            break

    core = domain.split(".")[0] if domain else ""
    for brand in BRANDS:
        if brand == core:
            break
        distance = levenshtein(core, brand)
        if 0 < distance <= 2 and len(core) >= 5 and abs(len(core) - len(brand)) <= 2:
            add(Signal("typosquat", f"Le domaine '{core}' ressemble fortement a la marque '{brand}' a {distance} caractere(s) pres : typosquatting probable.", 40, domain, "usurpation"))
            break
        if brand in host and brand not in core and len(brand) >= 5:
            add(Signal("brand_in_subdomain", f"Le nom '{brand}' apparait dans le sous-domaine mais le domaine reel est '{domain}' : la marque est imitee, pas utilisee.", 38, host, "usurpation"))
            break

    lowered = (path + "?" + query).lower()
    hits = sorted({w for w in SENSITIVE_PATH_WORDS if w in lowered})
    if hits:
        add(Signal("sensitive_path", f"L'adresse contient des mots lies aux comptes ou aux paiements ({', '.join(hits[:4])}).", 10 + 4 * min(len(hits), 3), ", ".join(hits[:6]), "contenu"))

    for ext in DANGEROUS_EXTENSIONS:
        if path.lower().endswith(ext):
            add(Signal("dangerous_file", f"Le lien telecharge directement un fichier {ext} : ne jamais l'installer depuis une source non verifiee.", 45, ext, "charge"))
            break

    if core and shannon_entropy(core) > 3.6 and len(core) >= 12:
        add(Signal("random_domain", "Le nom de domaine ressemble a une suite de caracteres generee aleatoirement.", 16, core, "domaine"))

    if core.count("-") >= 3:
        add(Signal("many_hyphens", "Le domaine accumule les tirets, motif courant des faux sites.", 10, core, "domaine"))

    if re.search(r"%[0-9a-fA-F]{2}", url) and len(re.findall(r"%[0-9a-fA-F]{2}", url)) >= 6:
        add(Signal("encoded", "L'adresse contient de nombreux caracteres encodes, technique d'obfuscation.", 12, category="obfuscation"))

    meta = {"host": host, "domain": domain, "tld": tld, "scheme": parsed.scheme, "path": path[:200]}
    return signals, meta


async def _network_signals(url: str) -> tuple[list[Signal], list[dict], dict]:
    signals: list[Signal] = []
    sources: list[dict] = []
    meta: dict = {}
    parsed = urlparse(url)
    host = parsed.hostname or ""

    blocked_internal = False
    try:
        infos = await _resolve(host)
        meta["ips"] = infos
        if not infos:
            signals.append(Signal("dns_fail", "Le nom de domaine ne resout vers aucune adresse IP : le site n'existe pas ou n'existe plus.", 25, host, "reseau"))
        elif any(not _is_public_ip(ip) for ip in infos):
            # Le domaine resout vers une adresse interne/privee : on ne sonde jamais cette cible
            # (protection anti-SSRF), et c'est en soi un signal tres suspect pour un lien public.
            blocked_internal = True
            signals.append(Signal(
                "internal_target",
                "Le domaine resout vers une adresse reseau interne/privee : lien anormal, sonde reseau bloquee par securite.",
                40, host, "reseau",
            ))
            sources.append({"name": "DNS", "status": "blocked", "detail": "cible interne, sondes reseau non executees"})
        sources.append({"name": "DNS", "status": "ok" if infos else "unresolved", "detail": ", ".join(infos[:3])})
    except Exception as exc:  # pragma: no cover - depend du reseau
        sources.append({"name": "DNS", "status": "error", "detail": str(exc)[:120]})

    if blocked_internal:
        return signals, sources, meta

    if parsed.scheme == "https":
        cert = await _tls_certificate(host, parsed.port or 443)
        if cert.get("error"):
            signals.append(Signal("tls_error", f"Le certificat HTTPS n'a pas pu etre valide ({cert['error']}).", 28, category="transport"))
            sources.append({"name": "TLS", "status": "error", "detail": cert["error"][:120]})
        elif cert.get("age_days") is not None:
            meta["cert_age_days"] = cert["age_days"]
            meta["cert_issuer"] = cert.get("issuer", "")
            sources.append({"name": "TLS", "status": "ok", "detail": f"certificat emis il y a {cert['age_days']} j par {cert.get('issuer','')}"})
            if cert["age_days"] <= 7:
                signals.append(Signal("fresh_cert", f"Le certificat du site a ete emis il y a {cert['age_days']} jour(s) : domaine tres recent, signal frequent de campagne de phishing.", 22, category="transport"))
            elif cert["age_days"] <= 30:
                signals.append(Signal("recent_cert", f"Le certificat a moins d'un mois ({cert['age_days']} jours).", 10, category="transport"))

    chain, final_url, status_code, error = await _redirect_chain(url)
    if error:
        sources.append({"name": "HTTP", "status": "error", "detail": error[:120]})
    else:
        meta["final_url"] = final_url
        meta["status_code"] = status_code
        meta["redirects"] = chain
        sources.append({"name": "HTTP", "status": "ok", "detail": f"{status_code} apres {len(chain)} redirection(s)"})
        if len(chain) >= 3:
            signals.append(Signal("redirect_chain", f"Le lien enchaine {len(chain)} redirections avant d'arriver a destination.", 14, " -> ".join(chain[:3]), "reseau"))
        final_host = urlparse(final_url).hostname or ""
        if final_host and registrable_domain(final_host) != registrable_domain(host):
            signals.append(Signal("cross_domain_redirect", f"Le lien redirige vers un autre domaine : {registrable_domain(final_host)}.", 20, final_url[:120], "reseau"))
        if final_url != url:
            extra, _ = _static_signals(final_url)
            for sig in extra:
                if sig.weight > 15 and all(s.code != f"final_{sig.code}" for s in signals):
                    signals.append(Signal(f"final_{sig.code}", f"Destination finale : {sig.label}", sig.weight, sig.evidence, sig.category))
    return signals, sources, meta


def _is_public_ip(ip: str) -> bool:
    """Refuse toute adresse privee, loopback, link-local, reservee ou multicast.

    Sans ce filtre, un attaquant pourrait soumettre une URL comme http://169.254.169.254/
    ou http://localhost/ et forcer le serveur VIGIA a sonder son propre reseau interne
    (SSRF) : le probleme meme que VIGIA est cense proteger les utilisateurs contre.
    """
    try:
        addr = ipaddress.ip_address(ip)
    except ValueError:
        return False
    return not (
        addr.is_private
        or addr.is_loopback
        or addr.is_link_local
        or addr.is_reserved
        or addr.is_multicast
        or addr.is_unspecified
    )


async def _resolve(host: str) -> list[str]:
    import asyncio

    loop = asyncio.get_running_loop()

    def _lookup() -> list[str]:
        try:
            infos = socket.getaddrinfo(host, None)
        except socket.gaierror:
            return []
        return sorted({info[4][0] for info in infos})

    return await asyncio.wait_for(loop.run_in_executor(None, _lookup), timeout=_settings.http_timeout)


async def _tls_certificate(host: str, port: int) -> dict:
    import asyncio

    loop = asyncio.get_running_loop()

    def _fetch() -> dict:
        context = ssl.create_default_context()
        try:
            with socket.create_connection((host, port), timeout=_settings.http_timeout) as sock:
                with context.wrap_socket(sock, server_hostname=host) as tls:
                    cert = tls.getpeercert()
        except Exception as exc:
            return {"error": type(exc).__name__}
        not_before = cert.get("notBefore")
        issuer = ""
        for part in cert.get("issuer", ()):  # type: ignore[assignment]
            for key, value in part:
                if key == "organizationName":
                    issuer = value
        age = None
        if not_before:
            try:
                dt = datetime.strptime(not_before, "%b %d %H:%M:%S %Y %Z").replace(tzinfo=timezone.utc)
                age = (datetime.now(timezone.utc) - dt).days
            except ValueError:
                age = None
        return {"age_days": age, "issuer": issuer}

    try:
        return await asyncio.wait_for(loop.run_in_executor(None, _fetch), timeout=_settings.http_timeout + 2)
    except Exception as exc:
        return {"error": type(exc).__name__}


async def _redirect_chain(url: str) -> tuple[list[str], str, int, str]:
    """Suit les redirections manuellement pour pouvoir verifier CHAQUE saut contre le SSRF :
    une redirection peut pointer vers une adresse interne meme si l'URL de depart etait publique."""
    chain: list[str] = []
    current = url
    try:
        async with httpx.AsyncClient(
            follow_redirects=False,
            timeout=_settings.http_timeout,
            headers={"User-Agent": "VigiaAI-Scanner/1.0 (+security-check)"},
            verify=True,
        ) as client:
            for _ in range(6):
                hop_host = urlparse(current).hostname or ""
                try:
                    hop_ips = await _resolve(hop_host)
                except Exception:
                    hop_ips = []
                if hop_ips and any(not _is_public_ip(ip) for ip in hop_ips):
                    return chain, current, 0, "redirection vers une cible interne bloquee"
                response = await client.get(current)
                if response.is_redirect and response.headers.get("location"):
                    nxt = str(response.next_request.url) if response.next_request else response.headers["location"]
                    chain.append(nxt)
                    current = nxt
                    continue
                return chain, current, response.status_code, ""
        return chain, current, 0, "trop de redirections"
    except Exception as exc:
        return chain, current, 0, f"{type(exc).__name__}"


async def analyse_url(url: str, online: bool = True) -> EngineResult:
    normalized = normalize_url(url)
    signals, meta = _static_signals(normalized)
    sources: list[dict] = [{"name": "Heuristiques VIGIA", "status": "ok", "detail": f"v{ENGINE_VERSION}, hors ligne"}]

    if online and _settings.allow_network_probes:
        import asyncio

        from app.engine.reputation import google_safebrowsing, virustotal

        net, gsb, vt = await asyncio.gather(
            _network_signals(normalized),
            google_safebrowsing(normalized),
            virustotal(normalized),
            return_exceptions=True,
        )
        if not isinstance(net, Exception):
            net_signals, net_sources, net_meta = net
            signals.extend(net_signals)
            sources.extend(net_sources)
            meta.update(net_meta)
        for reputation in (gsb, vt):
            if isinstance(reputation, Exception):
                continue
            rep_signals, rep_source = reputation
            signals.extend(rep_signals)
            sources.append(rep_source)

    score = clamp_score(sum(s.weight for s in signals))
    result = EngineResult(score=score, level=level_from_score(score), signals=signals, sources=sources, extracted=meta)
    result.summary = _summary(result, meta)
    return result


def _summary(result: EngineResult, meta: dict) -> str:
    risky = [s for s in result.signals if s.weight >= 15]
    domain = meta.get("domain", "ce lien")
    if result.level == "dangerous":
        head = f"Ce lien presente un risque eleve. Ne l'ouvre pas et ne saisis aucune information sur {domain}."
    elif result.level == "suspicious":
        head = f"Ce lien presente des signaux suspects. Verifie l'expediteur avant d'ouvrir {domain}."
    else:
        head = f"Aucun signal de danger significatif detecte sur {domain}. Reste attentif si on te demande des informations personnelles."
    if risky:
        head += " Principaux motifs : " + " ".join(s.label for s in risky[:3])
    return head
