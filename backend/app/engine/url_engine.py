from __future__ import annotations

import ipaddress
import re
import socket
import ssl
from html.parser import HTMLParser
from datetime import datetime, timezone
from urllib.parse import unquote, urljoin, urlparse

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

ENGINE_VERSION = "2.0.1"
MAX_PAGE_BYTES = 512 * 1024
MAX_TEXT_CHARS = 12000
_settings = get_settings()

URL_RE = re.compile(r"""(?:(?:https?|ftp)://|www\.)[^\s<>"'`\]\[{}]+""", re.IGNORECASE)
MULTI_TLD = {"co.uk", "com.br", "co.za", "com.au", "co.jp", "org.uk", "gov.uk", "ac.uk", "com.ng"}

# Domaines officiels connus pour les marques les plus usurpees. Cette table n'est
# jamais utilisee seule pour declarer une fraude : elle sert a corroborer un titre/formulaire
# qui imite une marque depuis un autre domaine.
OFFICIAL_BRAND_DOMAINS = {
    "paypal": {"paypal.com"}, "google": {"google.com"}, "gmail": {"google.com", "gmail.com"},
    "microsoft": {"microsoft.com"}, "outlook": {"microsoft.com", "outlook.com"},
    "apple": {"apple.com"}, "icloud": {"icloud.com", "apple.com"},
    "facebook": {"facebook.com"}, "instagram": {"instagram.com"}, "whatsapp": {"whatsapp.com"},
    "amazon": {"amazon.com", "amazon.fr"}, "netflix": {"netflix.com"},
    "dhl": {"dhl.com"}, "fedex": {"fedex.com"}, "orange": {"orange.com", "orange.fr"},
    "orangemoney": {"orange.com", "orange.fr"}, "wave": {"wave.com", "wave.com"},
    "mtn": {"mtn.com"}, "mtnmomo": {"mtn.com"}, "moovmoney": {"moov-africa.com"},
    "ecobank": {"ecobank.com"}, "uba": {"ubagroup.com"}, "boa": {"bankofafrica.net"},
    "westernunion": {"westernunion.com"}, "moneygram": {"moneygram.com"},
}


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
        add(Signal("many_subdomains", f"Le domaine empile {len(labels)} niveaux de sous-domaines, ce qui merite une verification supplementaire.", 6, host, "domaine"))

    if len(url) > 120:
        add(Signal("long_url", f"Le lien est anormalement long ({len(url)} caracteres).", 8, category="forme"))

    if domain in SHORTENERS:
        add(Signal("shortener", f"Le lien est raccourci via {domain} : la destination reelle est masquee tant qu'on ne l'ouvre pas.", 22, domain, "obfuscation"))

    if tld in HIGH_RISK_TLDS:
        add(Signal("risky_tld", f"L'extension .{tld} est a surveiller, mais cette caracteristique seule ne prouve pas une fraude.", 8, tld, "domaine"))

    for free in FREE_HOSTING:
        if host == free or host.endswith("." + free):
            add(Signal("free_hosting", f"La page est hebergee sur un service partage ou gratuit ({free}) : cela ne prouve pas une fraude, mais reduit la valeur de confiance du domaine.", 6, free, "hebergement"))
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
        sensitive_weight = 10 + 4 * min(len(hits), 3)
        add(Signal("sensitive_path", f"L'adresse contient des mots lies aux comptes ou aux paiements ({', '.join(hits[:4])}).", sensitive_weight, ", ".join(hits[:6]), "contenu"))
        if parsed.scheme != "https":
            add(Signal(
                "http_sensitive_page",
                "La page semble concerner une connexion ou un paiement mais utilise HTTP non chiffre : ne saisis aucune donnee.",
                15,
                ", ".join(hits[:6]),
                "transport",
            ))

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



class _PageParser(HTMLParser):
    """Extracteur HTML minimal et defensif : pas d'execution de JavaScript."""
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.title_parts: list[str] = []
        self.text_parts: list[str] = []
        self.forms: list[dict] = []
        self.links: list[str] = []
        self.meta_refresh: list[str] = []
        self._in_title = False
        self._current_form: dict | None = None

    def handle_starttag(self, tag, attrs):
        attrs = {str(k).lower(): str(v) for k, v in attrs if k}
        tag = tag.lower()
        if tag == "title":
            self._in_title = True
        elif tag == "form":
            self._current_form = {"action": attrs.get("action", ""), "method": attrs.get("method", "get"), "password": False, "inputs": []}
            self.forms.append(self._current_form)
        elif tag == "input" and self._current_form is not None:
            typ = attrs.get("type", "text").lower()
            name = attrs.get("name", "").lower()
            self._current_form["inputs"].append(typ)
            if typ == "password" or any(x in name for x in ("password", "pass", "pin", "otp", "code", "cvv", "secret")):
                self._current_form["password"] = True
        elif tag == "a" and attrs.get("href"):
            self.links.append(attrs["href"])
        elif tag == "meta" and attrs.get("http-equiv", "").lower() == "refresh":
            if attrs.get("content"):
                self.meta_refresh.append(attrs["content"][:300])

    def handle_endtag(self, tag):
        if tag.lower() == "title":
            self._in_title = False
        elif tag.lower() == "form":
            self._current_form = None

    def handle_data(self, data):
        value = " ".join((data or "").split())
        if not value:
            return
        if self._in_title:
            self.title_parts.append(value)
        self.text_parts.append(value)


def _domain_in_text(text: str, domain: str) -> bool:
    return domain and domain.lower() in text.lower()


async def _page_content_signals(url: str) -> tuple[list[Signal], list[dict], dict]:
    """Analyse le contenu HTML reel de la destination finale sans executer le code.

    Cette couche ne declare jamais un site frauduleux sur la seule presence d'un mot.
    Elle cherche des combinaisons : formulaire de mot de passe + domaine non officiel,
    collecte de secrets, action cross-domain, faux titre de marque, meta-refresh, etc.
    """
    signals: list[Signal] = []
    sources: list[dict] = []
    meta: dict = {}
    parsed = urlparse(url)
    host = parsed.hostname or ""
    domain = registrable_domain(host)
    try:
        ips = await _resolve(host)
        if ips and any(not _is_public_ip(ip) for ip in ips):
            return [Signal("page_internal_target", "La destination finale resout vers une adresse interne : contenu non sonde.", 40, host, "reseau")], [{"name": "Page HTML", "status": "blocked", "detail": "cible interne"}], meta
        timeout = max(_settings.http_timeout, 8.0)
        async with httpx.AsyncClient(follow_redirects=False, timeout=timeout, headers={"User-Agent": "VigiaAI-Scanner/2.0"}, verify=True) as client:
            async with client.stream("GET", url) as response:
                ctype = response.headers.get("content-type", "").lower()
                meta["content_type"] = ctype
                meta["content_length"] = response.headers.get("content-length", "")
                if "text/html" not in ctype and "application/xhtml+xml" not in ctype:
                    sources.append({"name": "Page HTML", "status": "skipped", "detail": f"type {ctype or 'inconnu'}"})
                    return signals, sources, meta
                chunks: list[bytes] = []
                total = 0
                async for chunk in response.aiter_bytes():
                    if not chunk:
                        continue
                    remaining = MAX_PAGE_BYTES - total
                    if remaining <= 0:
                        break
                    part = chunk[:remaining]
                    chunks.append(part)
                    total += len(part)
                    if total >= MAX_PAGE_BYTES:
                        break
                raw = b"".join(chunks)
                charset = "utf-8"
                match = re.search(r"charset=([\w-]+)", ctype, re.I)
                if match:
                    charset = match.group(1)
                try:
                    html = raw.decode(charset, errors="replace")
                except LookupError:
                    html = raw.decode("utf-8", errors="replace")
    except Exception as exc:
        return signals, [{"name": "Page HTML", "status": "error", "detail": type(exc).__name__}], meta

    parser = _PageParser()
    try:
        parser.feed(html)
    except Exception:
        pass
    title = " ".join(parser.title_parts).strip()
    visible = " ".join(parser.text_parts)
    lowered = (title + " " + visible).lower()[:MAX_TEXT_CHARS]
    meta["title"] = title[:240]
    meta["forms"] = len(parser.forms)
    meta["password_forms"] = sum(1 for f in parser.forms if f.get("password"))

    if parser.forms:
        signals.append(Signal("html_form", f"La page contient {len(parser.forms)} formulaire(s) HTML.", 4, str(len(parser.forms)), "contenu"))
    secret_forms = [f for f in parser.forms if f.get("password")]
    if secret_forms:
        signals.append(Signal("credential_form", "La page contient un champ pouvant collecter un mot de passe, PIN, OTP ou autre secret.", 18, "formulaire sensible", "vol_de_donnees"))
        for form in secret_forms:
            action = form.get("action", "").strip()
            if action:
                action_url = urljoin(url, action)
                action_domain = registrable_domain(urlparse(action_url).hostname or "")
                if action_domain and action_domain != domain:
                    signals.append(Signal("cross_domain_form", f"Un formulaire sensible envoie les donnees vers un autre domaine ({action_domain}).", 32, action_domain, "vol_de_donnees"))

    payment_words = {"paiement", "payment", "transfer", "transfert", "wallet", "mobile money", "momo", "wave", "orangemoney", "moov", "mtn"}
    credential_words = {"mot de passe", "password", "code pin", "pin", "otp", "verification code", "code de verification", "recovery phrase", "seed phrase"}
    payment_hits = sorted(w for w in payment_words if w in lowered)
    credential_hits = sorted(w for w in credential_words if w in lowered)
    if payment_hits:
        signals.append(Signal("payment_language", f"La page contient un vocabulaire de paiement ({', '.join(payment_hits[:5])}).", 8, ", ".join(payment_hits[:5]), "arnaque_financiere"))
    if credential_hits:
        signals.append(Signal("credential_language", f"La page demande ou mentionne des secrets d'acces ({', '.join(credential_hits[:5])}).", 12, ", ".join(credential_hits[:5]), "vol_de_donnees"))

    title_lower = title.lower()
    for brand, official_domains in OFFICIAL_BRAND_DOMAINS.items():
        if brand in title_lower or any(brand in x for x in credential_hits + payment_hits):
            if domain not in official_domains and not any(domain.endswith("." + d) for d in official_domains):
                signals.append(Signal("brand_page_domain_mismatch", f"La page semble se presenter comme '{brand}' mais le domaine reel est '{domain}'.", 34, f"marque={brand}, domaine={domain}", "usurpation"))
                break

    if parser.meta_refresh:
        signals.append(Signal("meta_refresh", "La page utilise une redirection HTML automatique.", 8, parser.meta_refresh[0][:120], "reseau"))
    if re.search(r"(?:window\.location|location\.href|document\.location)\s*=", html, re.I):
        signals.append(Signal("js_redirect", "La page contient une redirection JavaScript automatique.", 10, "script de redirection", "reseau"))

    external_domains: set[str] = set()
    for href in parser.links[:80]:
        try:
            absolute = urljoin(url, href)
            h = urlparse(absolute).hostname or ""
            d = registrable_domain(h)
            if d and d != domain:
                external_domains.add(d)
        except Exception:
            continue
    if len(external_domains) >= 8:
        signals.append(Signal("many_external_domains", f"La page charge ou reference de nombreux domaines externes ({len(external_domains)}).", 5, ", ".join(sorted(external_domains)[:6]), "contenu"))

    sources.append({"name": "Page HTML", "status": "ok", "detail": f"{len(raw)} octets inspectes, titre={title[:80] or 'sans titre'}, formulaires={len(parser.forms)}"})
    return signals, sources, meta


async def _rdap_signals(domain: str) -> tuple[list[Signal], dict]:
    if not domain or domain in {"localhost"}:
        return [], {"name": "RDAP", "status": "skipped", "detail": "domaine non public"}
    try:
        async with httpx.AsyncClient(follow_redirects=True, timeout=_settings.http_timeout, headers={"User-Agent": "VigiaAI-Scanner/2.0"}) as client:
            response = await client.get(f"https://rdap.org/domain/{domain}")
            if response.status_code == 404:
                return [], {"name": "RDAP", "status": "unknown", "detail": "aucune donnee d'enregistrement"}
            response.raise_for_status()
            data = response.json()
    except Exception as exc:
        return [], {"name": "RDAP", "status": "error", "detail": type(exc).__name__}
    registration = None
    for event in data.get("events", []) or []:
        if event.get("eventAction") in {"registration", "registered"}:
            registration = event.get("eventDate")
            break
    if not registration:
        return [], {"name": "RDAP", "status": "ok", "detail": "enregistrement trouve, date non exposee"}
    try:
        dt = datetime.fromisoformat(registration.replace("Z", "+00:00"))
        age = max(0, (datetime.now(timezone.utc) - dt).days)
    except Exception:
        return [], {"name": "RDAP", "status": "ok", "detail": "date d'enregistrement illisible"}
    signals: list[Signal] = []
    if age <= 7:
        signals.append(Signal("new_domain", f"Le domaine a ete enregistre il y a {age} jour(s) : indice contextuel, pas une preuve de fraude.", 10, f"age={age}j", "domaine"))
    elif age <= 30:
        signals.append(Signal("recent_domain", f"Le domaine a ete enregistre il y a {age} jours : indice contextuel, pas une preuve de fraude.", 5, f"age={age}j", "domaine"))
    return signals, {"name": "RDAP", "status": "ok", "detail": f"domaine enregistre il y a {age} jour(s)"}

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
                signals.append(Signal("fresh_cert", f"Le certificat du site a ete emis il y a {cert['age_days']} jour(s) : le domaine merite une verification supplementaire.", 8, category="transport"))
            elif cert["age_days"] <= 30:
                signals.append(Signal("recent_cert", f"Le certificat a moins d'un mois ({cert['age_days']} jours) : indice contextuel, pas une preuve de fraude.", 4, category="transport"))

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
    sources: list[dict] = [{"name": "Heuristiques VIGIA", "status": "ok", "detail": f"v{ENGINE_VERSION}, heuristiques + contenu HTML + reseau"}]

    if online and _settings.allow_network_probes:
        import asyncio

        from app.engine.reputation import google_safebrowsing, virustotal

        net, gsb, vt, rdap = await asyncio.gather(
            _network_signals(normalized),
            google_safebrowsing(normalized),
            virustotal(normalized),
            _rdap_signals(registrable_domain(urlparse(normalized).hostname or "")),
            return_exceptions=True,
        )
        if not isinstance(net, Exception):
            net_signals, net_sources, net_meta = net
            signals.extend(net_signals)
            sources.extend(net_sources)
            meta.update(net_meta)
        for reputation in (gsb, vt):
            if isinstance(reputation, Exception):
                sources.append({"name": "Threat intelligence", "status": "error", "detail": type(reputation).__name__})
                continue
            rep_signals, rep_source = reputation
            signals.extend(rep_signals)
            sources.append(rep_source)
        if isinstance(rdap, Exception):
            sources.append({"name": "RDAP", "status": "error", "detail": type(rdap).__name__})
        else:
            rdap_signals, rdap_source = rdap
            signals.extend(rdap_signals)
            sources.append(rdap_source)
        # Inspection du contenu reel de la destination finale : c'est volontairement
        # separee des redirections afin de ne jamais telecharger un fichier arbitraire
        # a chaque hop. On ne lit que le HTML et seulement les premiers 512 Ko.
        final_url = meta.get("final_url") or normalized
        page_signals, page_sources, page_meta = await _page_content_signals(final_url)
        signals.extend(page_signals)
        sources.extend(page_sources)
        meta.update(page_meta)

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
